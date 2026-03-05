/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot.util;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SecurityKeyDebugLog {
	private static final String TAG = "CB.SKDebugLog";
	private static final String UTF_8 = "UTF-8";
	private static final String BUFFER_DIRECTORY = "debug";
	private static final String BUFFER_FILENAME = "termbot-debug-ring-buffer.log";
	private static final int MAX_BUFFER_LINES = 600;
	private static final int MAX_LINE_LENGTH = 900;

// Matches hex byte arrays of 4+ bytes (8+ hex chars, optionally space/colon separated).
// Threshold is 4 pairs to catch short APDU command headers and PIN VERIFY payloads
// (e.g. "00c00000" = 4 bytes, "0020008008..." = VERIFY PIN command).
// Short ISO 7816 status words (≤3 pairs like "6d00" or "sw1=90") intentionally stay
// below the threshold — they are public protocol codes, not secrets.
private static final Pattern HEX_PAYLOAD_PATTERN = Pattern.compile("\\b(?:0x)?(?:[0-9A-Fa-f]{2}[\\s:]?){4,}\\b");
private static final Pattern BASE64_PAYLOAD_PATTERN = Pattern.compile("\\b[A-Za-z0-9+/]{32,}={0,2}\\b");
private static final Pattern SHA256_FINGERPRINT_PATTERN = Pattern.compile("SHA256:[A-Za-z0-9+/=]{16,}");
private static final Pattern OPENSSH_PUBLIC_KEY_PATTERN =
Pattern.compile("(?:ssh-(?:rsa|dss|ed25519)|ecdsa-sha2-[A-Za-z0-9-]+|sk-[A-Za-z0-9@._-]+) [A-Za-z0-9+/=]{32,}");
private static final Pattern PIN_VALUE_PATTERN = Pattern.compile("(?i)\\bpin\\b\\s*[:=]\\s*\\S+");
private static final Pattern SECRET_VALUE_PATTERN =
Pattern.compile("(?i)\\b(pass(?:word|phrase)?|secret|private\\s*key(?:\\s*material)?)\\b\\s*[:=]\\s*\\S+");
// Catches "ResponseApdu{data=<hex>" — hwsecurity logs full APDU response bodies here.
// Applied before general hex redaction so the structure tag is preserved.
private static final Pattern RESPONSE_APDU_DATA_PATTERN =
Pattern.compile("(ResponseApdu\\{data=)[0-9A-Fa-f]+");
// Catches NFC tag UIDs logged by hwsecurity as "Discovered NFC tag (<uid>)".
// UIDs are device identifiers and must not appear in exported reports.
private static final Pattern NFC_TAG_UID_PATTERN =
Pattern.compile("(Discovered NFC tag \\()[0-9A-Fa-f]+(\\))");
private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	private static final Object LOCK = new Object();
	private static final ArrayDeque<String> RING_BUFFER = new ArrayDeque<String>(MAX_BUFFER_LINES);

	private static File sBufferFile;
	private static boolean sInitialized;

	private SecurityKeyDebugLog() {
	}

	public static void initialize(@NonNull Context context) {
		synchronized (LOCK) {
			if (sInitialized) {
				return;
			}

			File debugDir = new File(context.getApplicationContext().getFilesDir(), BUFFER_DIRECTORY);
			if (!debugDir.exists() && !debugDir.mkdirs()) {
				Log.w(TAG, "Unable to create debug directory at " + debugDir.getAbsolutePath());
			}

			sBufferFile = new File(debugDir, BUFFER_FILENAME);
			loadBufferFromDiskLocked();
			sInitialized = true;
		}
	}

public static void logFlow(@NonNull Context context, @NonNull String component, @NonNull String marker) {
		logFlow(context, component, marker, null);
	}

	public static void logFlow(@NonNull Context context, @NonNull String component, @NonNull String marker,
			@Nullable String details) {
		initialize(context);
		String message = sanitizeForDebugReport(marker);
		if (details != null && !details.isEmpty()) {
			message = message + ": " + sanitizeForDebugReport(details);
		}
		append("FLOW", component, message);
	}

	@NonNull
	public static List<String> getLastLines(@NonNull Context context, int lineCount) {
		initialize(context);
		int safeLineCount = Math.max(1, lineCount);

		synchronized (LOCK) {
			ArrayList<String> lines = new ArrayList<String>(RING_BUFFER);
			int startIndex = Math.max(0, lines.size() - safeLineCount);
			return new ArrayList<String>(lines.subList(startIndex, lines.size()));
		}
	}

	@NonNull
	public static String sanitizeForDebugReport(@Nullable String value) {
		if (value == null) {
			return "";
		}

		String sanitized = value.replace('\n', ' ').replace('\r', ' ');
		ArrayList<String> preservedFingerprints = new ArrayList<String>();
		ArrayList<String> preservedOpenSshKeys = new ArrayList<String>();
		Matcher fingerprintMatcher = SHA256_FINGERPRINT_PATTERN.matcher(sanitized);
		StringBuffer fingerprintBuffer = new StringBuffer();
		int fingerprintIndex = 0;
		while (fingerprintMatcher.find()) {
			preservedFingerprints.add(fingerprintMatcher.group());
			fingerprintMatcher.appendReplacement(fingerprintBuffer, "__SHA256_FINGERPRINT_" + fingerprintIndex + "__");
			fingerprintIndex++;
		}
		fingerprintMatcher.appendTail(fingerprintBuffer);
		sanitized = fingerprintBuffer.toString();

		Matcher openSshKeyMatcher = OPENSSH_PUBLIC_KEY_PATTERN.matcher(sanitized);
		StringBuffer openSshKeyBuffer = new StringBuffer();
		int openSshKeyIndex = 0;
		while (openSshKeyMatcher.find()) {
			preservedOpenSshKeys.add(openSshKeyMatcher.group());
			openSshKeyMatcher.appendReplacement(openSshKeyBuffer, "__OPENSSH_PUBLIC_KEY_" + openSshKeyIndex + "__");
			openSshKeyIndex++;
		}
		openSshKeyMatcher.appendTail(openSshKeyBuffer);
		sanitized = openSshKeyBuffer.toString();

        // Explicit structural redactions first (preserve log structure labels).
        sanitized = RESPONSE_APDU_DATA_PATTERN.matcher(sanitized).replaceAll("$1<apdu-data-redacted>");
        sanitized = NFC_TAG_UID_PATTERN.matcher(sanitized).replaceAll("$1<tag-uid-redacted>$2");
        // General payload redaction.
        sanitized = HEX_PAYLOAD_PATTERN.matcher(sanitized).replaceAll("<redacted-hex>");
        sanitized = BASE64_PAYLOAD_PATTERN.matcher(sanitized).replaceAll("<redacted-data>");
        sanitized = PIN_VALUE_PATTERN.matcher(sanitized).replaceAll("PIN=<redacted>");
        sanitized = SECRET_VALUE_PATTERN.matcher(sanitized).replaceAll("$1=<redacted>");
		sanitized = WHITESPACE_PATTERN.matcher(sanitized).replaceAll(" ").trim();
		for (int i = 0; i < preservedFingerprints.size(); i++) {
			sanitized = sanitized.replace("__SHA256_FINGERPRINT_" + i + "__", preservedFingerprints.get(i));
		}
		for (int i = 0; i < preservedOpenSshKeys.size(); i++) {
			sanitized = sanitized.replace("__OPENSSH_PUBLIC_KEY_" + i + "__", preservedOpenSshKeys.get(i));
		}

		if (sanitized.length() > MAX_LINE_LENGTH) {
			sanitized = sanitized.substring(0, MAX_LINE_LENGTH) + "...";
		}
		return sanitized;
	}

	private static void append(String channel, String component, String message) {
		String safeChannel = sanitizeLabel(channel, "LOG");
		String safeComponent = sanitizeLabel(component, "Unknown");
		String safeMessage = sanitizeForDebugReport(message);
		String line = timestampNow() + " [" + safeChannel + "] [" + safeComponent + "] " + safeMessage;

		synchronized (LOCK) {
			if (!sInitialized) {
				return;
			}

			boolean trimmed = false;
			if (RING_BUFFER.size() >= MAX_BUFFER_LINES) {
				RING_BUFFER.removeFirst();
				trimmed = true;
			}
			RING_BUFFER.addLast(line);

			if (trimmed) {
				rewriteBufferToDiskLocked();
			} else {
				appendLineToDiskLocked(line);
			}
		}
	}

	private static void loadBufferFromDiskLocked() {
		if (sBufferFile == null || !sBufferFile.exists()) {
			return;
		}

		boolean trimmed = false;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sBufferFile), UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				if (RING_BUFFER.size() >= MAX_BUFFER_LINES) {
					RING_BUFFER.removeFirst();
					trimmed = true;
				}
				if (line.length() > MAX_LINE_LENGTH) {
					RING_BUFFER.addLast(line.substring(0, MAX_LINE_LENGTH) + "...");
				} else {
					RING_BUFFER.addLast(line);
				}
			}
		} catch (IOException e) {
			Log.w(TAG, "Unable to read debug log buffer", e);
		}

		if (trimmed) {
			rewriteBufferToDiskLocked();
		}
	}

	private static void appendLineToDiskLocked(String line) {
		if (sBufferFile == null) {
			return;
		}

		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sBufferFile, true), UTF_8))) {
			writer.write(line);
			writer.newLine();
		} catch (IOException e) {
			Log.w(TAG, "Unable to append debug log buffer", e);
		}
	}

	private static void rewriteBufferToDiskLocked() {
		if (sBufferFile == null) {
			return;
		}

		File parentDir = sBufferFile.getParentFile();
		if (parentDir == null) {
			return;
		}

		File tempFile = new File(parentDir, sBufferFile.getName() + ".tmp");
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile, false), UTF_8))) {
			for (String line : RING_BUFFER) {
				writer.write(line);
				writer.newLine();
			}
		} catch (IOException e) {
			Log.w(TAG, "Unable to rewrite debug log buffer", e);
			if (tempFile.exists() && !tempFile.delete()) {
				Log.w(TAG, "Unable to delete temporary debug buffer file " + tempFile.getAbsolutePath());
			}
			return;
		}

		if (sBufferFile.exists() && !sBufferFile.delete()) {
			Log.w(TAG, "Unable to replace existing debug log buffer");
		}
		if (!tempFile.renameTo(sBufferFile)) {
			Log.w(TAG, "Unable to move rewritten debug buffer into place");
		}
	}

	private static String sanitizeLabel(@Nullable String label, @NonNull String fallback) {
		if (label == null || label.trim().isEmpty()) {
			return fallback;
		}
		String sanitized = label.replace('\n', ' ').replace('\r', ' ').trim();
		if (sanitized.length() > 60) {
			return sanitized.substring(0, 60);
		}
		return sanitized;
	}

	private static int normalizePriority(int priority) {
		switch (priority) {
		case Log.VERBOSE:
		case Log.DEBUG:
		case Log.INFO:
		case Log.WARN:
		case Log.ERROR:
		case Log.ASSERT:
			return priority;
		default:
			return Log.DEBUG;
		}
	}

	private static String timestampNow() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
		return format.format(new Date());
	}
}
