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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.connectbot.BuildConfig;
import org.connectbot.R;

public final class SecurityKeyDebugReportExporter {
	private static final String UTF_8 = "UTF-8";
	private static final int REPORT_LINE_COUNT = 300;
	private static final int SUMMARY_SCAN_LINE_COUNT = 600;
	private static final String REPORT_FILENAME_PREFIX = "termbot-report-";
	private static final String REPORT_FILENAME_SUFFIX = ".txt";
	private static final String MARKER_OFFERED_KEY_OPENSSH = "OFFERED_KEY_OPENSSH";
	private static final String MARKER_OFFERED_KEY_FP = "OFFERED_KEY_FP";
	private static final String MARKER_SECURITY_KEY_PROVIDER_USED = "SECURITY_KEY_PROVIDER_USED";
	private static final String MARKER_OPENPGP_SLOT_USED = "OPENPGP_SLOT_USED";
	private static final String MARKER_PIV_SLOT_USED = "PIV_SLOT_USED";
	private static final String MARKER_SERVER_DISCONNECT_DETAIL = "SSH_SERVER_DISCONNECT_DETAIL";
	private static final String MARKER_JUMP_HOST_USED = "JUMP_HOST_USED";
	private static final String MARKER_JUMP_STAGE = "JUMP_STAGE";

	private SecurityKeyDebugReportExporter() {
	}

	@NonNull
	public static File exportReport(@NonNull Context context) throws IOException {
		SecurityKeyDebugLog.initialize(context);

		File debugDir = context.getExternalFilesDir("debug");
		if (debugDir == null) {
			throw new IOException("External debug directory is unavailable");
		}
		if (!debugDir.exists() && !debugDir.mkdirs()) {
			throw new IOException("Unable to create debug directory at " + debugDir.getAbsolutePath());
		}

		String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
		File reportFile = new File(debugDir, REPORT_FILENAME_PREFIX + timestamp + REPORT_FILENAME_SUFFIX);

		writeReport(context, reportFile);
		return reportFile;
	}

	@NonNull
	public static Intent createShareIntent(@NonNull Context context, @NonNull File reportFile) {
		Uri reportUri = FileProvider.getUriForFile(
				context,
				BuildConfig.APPLICATION_ID + ".fileprovider",
				reportFile);

		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.security_key_export_debug_report_title));
		shareIntent.putExtra(Intent.EXTRA_STREAM, reportUri);
		shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		return Intent.createChooser(shareIntent, context.getString(R.string.security_key_export_debug_report_share_title));
	}

	private static void writeReport(Context context, File reportFile) throws IOException {
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
		String nfcState = (nfcAdapter == null) ? "unsupported" : Boolean.toString(nfcAdapter.isEnabled());
		boolean usbHostSupported = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
		String appFlavor = (BuildConfig.FLAVOR == null || BuildConfig.FLAVOR.isEmpty()) ? "default" : BuildConfig.FLAVOR;
		String deviceModel = SecurityKeyDebugLog.sanitizeForDebugReport(Build.MANUFACTURER + " " + Build.MODEL);
		String androidVersion = SecurityKeyDebugLog.sanitizeForDebugReport(Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
		String appVersionFlavor =
				SecurityKeyDebugLog.sanitizeForDebugReport(BuildConfig.VERSION_NAME + " / " + appFlavor + " / " + BuildConfig.BUILD_TYPE);
                String hwsecurityVersion = SecurityKeyDebugLog.sanitizeForDebugReport("yubikit-2.4.0");
		List<String> summaryLines = SecurityKeyDebugLog.getLastLines(context, SUMMARY_SCAN_LINE_COUNT);
		List<String> logLines = SecurityKeyDebugLog.getLastLines(context, REPORT_LINE_COUNT);
		String offeredKeyOpenSsh = findLatestMarkerValue(summaryLines, MARKER_OFFERED_KEY_OPENSSH);
		String offeredKeyFingerprint = findLatestMarkerValue(summaryLines, MARKER_OFFERED_KEY_FP);
		String securityKeyProviderUsed = findLatestMarkerValue(summaryLines, MARKER_SECURITY_KEY_PROVIDER_USED);
		String openPgpSlotUsed = findLatestMarkerValue(summaryLines, MARKER_OPENPGP_SLOT_USED);
		String pivSlotUsed = findLatestMarkerValue(summaryLines, MARKER_PIV_SLOT_USED);
		String sshServerDisconnectDetail = findLatestMarkerValue(summaryLines, MARKER_SERVER_DISCONNECT_DETAIL);
		String jumpHostUsed = findLatestMarkerValue(summaryLines, MARKER_JUMP_HOST_USED);
		String jumpStage = findLatestMarkerValue(summaryLines, MARKER_JUMP_STAGE);
		String pivPrototypeStatus = findLatestMarkerValue(summaryLines, YubiKeyCapabilityProbe.MARKER_PIV_STATUS);
		String fido2PrototypeStatus = findLatestMarkerValue(summaryLines, YubiKeyCapabilityProbe.MARKER_FIDO2_STATUS);
		String sshlibSkEcdsaSupported = findLatestMarkerValue(summaryLines, YubiKeyCapabilityProbe.MARKER_SSHLIB_SK_ECDSA);
		String sshlibSkEd25519Supported = findLatestMarkerValue(summaryLines, YubiKeyCapabilityProbe.MARKER_SSHLIB_SK_ED25519);

		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(reportFile, false), UTF_8))) {
			writer.write("Termbot Debug Report");
			writer.newLine();
			writer.write("generated_at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US).format(new Date()));
			writer.newLine();
			writer.write("device_model: " + deviceModel);
			writer.newLine();
			writer.write("android_version: " + androidVersion);
			writer.newLine();
			writer.write("app_version_flavor: " + appVersionFlavor);
			writer.newLine();
			writer.write("hwsecurity_version: " + hwsecurityVersion);
			writer.newLine();
			writer.write("nfc_enabled: " + nfcState);
			writer.newLine();
			writer.write("usb_host_supported: " + usbHostSupported);
			writer.newLine();
			writer.write("PIV_PROTOTYPE_STATUS=" + pivPrototypeStatus);
			writer.newLine();
			writer.write("FIDO2_PROTOTYPE_STATUS=" + fido2PrototypeStatus);
			writer.newLine();
			writer.write("SSHLIB_SK_ECDSA_SUPPORTED=" + sshlibSkEcdsaSupported);
			writer.newLine();
			writer.write("SSHLIB_SK_ED25519_SUPPORTED=" + sshlibSkEd25519Supported);
			writer.newLine();
			writer.write("OFFERED_KEY_OPENSSH=" + offeredKeyOpenSsh);
			writer.newLine();
			writer.write("OFFERED_KEY_FP=" + offeredKeyFingerprint);
			writer.newLine();
			writer.write("SECURITY_KEY_PROVIDER_USED=" + securityKeyProviderUsed);
			writer.newLine();
			writer.write("OPENPGP_SLOT_USED=" + openPgpSlotUsed);
			writer.newLine();
			writer.write("PIV_SLOT_USED=" + pivSlotUsed);
			writer.newLine();
			writer.write("SSH_SERVER_DISCONNECT_DETAIL=" + sshServerDisconnectDetail);
			writer.newLine();
			writer.write("JUMP_HOST_USED=" + jumpHostUsed);
			writer.newLine();
			writer.write("JUMP_STAGE_LAST=" + jumpStage);
			writer.newLine();
			writer.newLine();
			writer.write("last_300_log_lines:");
			writer.newLine();

			if (logLines.isEmpty()) {
				writer.write("(no debug logs captured yet)");
				writer.newLine();
			} else {
				for (String line : logLines) {
					writer.write(line);
					writer.newLine();
				}
			}
		}
	}

	private static String findLatestMarkerValue(List<String> lines, String marker) {
		if (lines == null || lines.isEmpty()) {
			return "unavailable";
		}
		String prefix = marker + ":";
		for (int i = lines.size() - 1; i >= 0; i--) {
			String line = lines.get(i);
			int markerStart = line.indexOf(prefix);
			if (markerStart < 0) {
				continue;
			}
			String value = line.substring(markerStart + prefix.length()).trim();
			if (!value.isEmpty()) {
				return value;
			}
		}
		return "unavailable";
	}
}
