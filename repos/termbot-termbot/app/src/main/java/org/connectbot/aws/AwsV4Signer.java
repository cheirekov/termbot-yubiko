/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.connectbot.aws;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AwsV4Signer {
	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final String SHA256 = "SHA-256";
	private static final String ALGORITHM = "AWS4-HMAC-SHA256";

	private AwsV4Signer() {
	}

	@NonNull
	public static SigningResult signJsonPost(
			@NonNull String service,
			@NonNull String region,
			@NonNull String host,
			@NonNull String amzTarget,
			@NonNull String payload,
			@NonNull AwsCredentials credentials,
			@Nullable Date now
	) throws Exception {
		return signPost(service, region, host, payload, "application/x-amz-json-1.1", amzTarget,
				credentials, now);
	}

	@NonNull
	public static SigningResult signFormPost(
			@NonNull String service,
			@NonNull String region,
			@NonNull String host,
			@NonNull String payload,
			@NonNull AwsCredentials credentials,
			@Nullable Date now
	) throws Exception {
		return signPost(service, region, host, payload,
				"application/x-www-form-urlencoded; charset=utf-8", null, credentials, now);
	}

	@NonNull
	private static SigningResult signPost(
			@NonNull String service,
			@NonNull String region,
			@NonNull String host,
			@NonNull String payload,
			@NonNull String contentType,
			@Nullable String amzTarget,
			@NonNull AwsCredentials credentials,
			@Nullable Date now
	) throws Exception {
		Date date = now == null ? new Date() : now;
		String amzDate = formatAmzDate(date);
		String dateStamp = formatDateStamp(date);
		String payloadHash = sha256Hex(payload);

		String canonicalHeaders = buildPostCanonicalHeaders(host, contentType, amzTarget, amzDate,
				credentials.getSessionToken());
		String signedHeaders = buildPostSignedHeaders(credentials.hasSessionToken(),
				amzTarget != null);
		String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n"
				+ payloadHash;
		String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";
		String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n"
				+ sha256Hex(canonicalRequest);

		byte[] signingKey = getSignatureKey(credentials.getSecretAccessKey(), dateStamp, region, service);
		String signature = toHex(hmacSha256(signingKey, stringToSign));

		String authorization = ALGORITHM
				+ " Credential=" + credentials.getAccessKeyId() + "/" + credentialScope
				+ ", SignedHeaders=" + signedHeaders
				+ ", Signature=" + signature;

		return new SigningResult(authorization, amzDate, credentials.getSessionToken());
	}

	@NonNull
	public static SigningResult signGet(
			@NonNull String service,
			@NonNull String region,
			@NonNull URI uri,
			@NonNull AwsCredentials credentials,
			@Nullable Date now
	) throws Exception {
		if (uri.getHost() == null || uri.getHost().isEmpty()) {
			throw new IllegalArgumentException("URI host is required for SigV4 signing");
		}

		Date date = now == null ? new Date() : now;
		String amzDate = formatAmzDate(date);
		String dateStamp = formatDateStamp(date);
		String canonicalUri = canonicalizePath(uri.getRawPath());
		String canonicalQuery = canonicalizeQuery(uri.getRawQuery());
		String hostHeader = uri.getPort() > 0 ? (uri.getHost() + ":" + uri.getPort()) : uri.getHost();
		String canonicalHeaders = buildGetCanonicalHeaders(hostHeader, amzDate,
				credentials.getSessionToken());
		String signedHeaders = buildGetSignedHeaders(credentials.hasSessionToken());
		String canonicalRequest = "GET\n"
				+ canonicalUri + "\n"
				+ canonicalQuery + "\n"
				+ canonicalHeaders + "\n"
				+ signedHeaders + "\n"
				+ sha256Hex("");
		String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";
		String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n"
				+ sha256Hex(canonicalRequest);

		byte[] signingKey = getSignatureKey(credentials.getSecretAccessKey(), dateStamp, region, service);
		String signature = toHex(hmacSha256(signingKey, stringToSign));

		String authorization = ALGORITHM
				+ " Credential=" + credentials.getAccessKeyId() + "/" + credentialScope
				+ ", SignedHeaders=" + signedHeaders
				+ ", Signature=" + signature;
		return new SigningResult(authorization, amzDate, credentials.getSessionToken());
	}

	private static String buildPostCanonicalHeaders(String host, String contentType,
			@Nullable String amzTarget, String amzDate, @Nullable String sessionToken) {
		StringBuilder headers = new StringBuilder();
		headers.append("content-type:").append(contentType).append("\n");
		headers.append("host:").append(host).append("\n");
		headers.append("x-amz-date:").append(amzDate).append("\n");
		if (sessionToken != null && !sessionToken.isEmpty()) {
			headers.append("x-amz-security-token:").append(sessionToken).append("\n");
		}
		if (amzTarget != null && !amzTarget.isEmpty()) {
			headers.append("x-amz-target:").append(amzTarget).append("\n");
		}
		return headers.toString();
	}

	private static String buildPostSignedHeaders(boolean includeSessionToken,
			boolean includeAmzTarget) {
		if (includeSessionToken && includeAmzTarget) {
			return "content-type;host;x-amz-date;x-amz-security-token;x-amz-target";
		}
		if (includeSessionToken) {
			return "content-type;host;x-amz-date;x-amz-security-token";
		}
		if (includeAmzTarget) {
			return "content-type;host;x-amz-date;x-amz-target";
		}
		return "content-type;host;x-amz-date";
	}

	private static String buildGetCanonicalHeaders(
			String host,
			String amzDate,
			@Nullable String sessionToken
	) {
		StringBuilder headers = new StringBuilder();
		headers.append("host:").append(host).append("\n");
		headers.append("x-amz-date:").append(amzDate).append("\n");
		if (sessionToken != null && !sessionToken.isEmpty()) {
			headers.append("x-amz-security-token:").append(sessionToken).append("\n");
		}
		return headers.toString();
	}

	private static String buildGetSignedHeaders(boolean includeSessionToken) {
		if (includeSessionToken) {
			return "host;x-amz-date;x-amz-security-token";
		}
		return "host;x-amz-date";
	}

	private static String canonicalizePath(@Nullable String rawPath) {
		if (rawPath == null || rawPath.isEmpty()) {
			return "/";
		}
		String[] segments = rawPath.split("/", -1);
		StringBuilder canonicalPath = new StringBuilder();
		for (int i = 0; i < segments.length; i++) {
			if (i > 0) {
				canonicalPath.append('/');
			}
			canonicalPath.append(encodeRfc3986PreservingPercent(segments[i]));
		}
		if (rawPath.startsWith("/") && (canonicalPath.length() == 0 || canonicalPath.charAt(0) != '/')) {
			canonicalPath.insert(0, '/');
		}
		return canonicalPath.toString();
	}

	private static String canonicalizeQuery(@Nullable String rawQuery) {
		if (rawQuery == null || rawQuery.isEmpty()) {
			return "";
		}

		List<String[]> pairs = new ArrayList<>();
		for (String parameter : rawQuery.split("&", -1)) {
			int separatorIndex = parameter.indexOf('=');
			String name;
			String value;
			if (separatorIndex < 0) {
				name = parameter;
				value = "";
			} else {
				name = parameter.substring(0, separatorIndex);
				value = parameter.substring(separatorIndex + 1);
			}
			pairs.add(new String[] {
					encodeRfc3986PreservingPercent(name),
					encodeRfc3986PreservingPercent(value)
			});
		}

		Collections.sort(pairs, new Comparator<String[]>() {
			@Override
			public int compare(String[] left, String[] right) {
				int byName = left[0].compareTo(right[0]);
				if (byName != 0) {
					return byName;
				}
				return left[1].compareTo(right[1]);
			}
		});

		StringBuilder canonicalQuery = new StringBuilder();
		for (int i = 0; i < pairs.size(); i++) {
			if (i > 0) {
				canonicalQuery.append('&');
			}
			canonicalQuery.append(pairs.get(i)[0]).append('=').append(pairs.get(i)[1]);
		}
		return canonicalQuery.toString();
	}

	private static String encodeRfc3986PreservingPercent(@NonNull String value) {
		StringBuilder result = new StringBuilder(value.length() * 2);
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (isUnreserved(c)) {
				result.append(c);
				continue;
			}
			if (c == '%' && i + 2 < value.length()
					&& isHex(value.charAt(i + 1))
					&& isHex(value.charAt(i + 2))) {
				result.append('%')
						.append(Character.toUpperCase(value.charAt(i + 1)))
						.append(Character.toUpperCase(value.charAt(i + 2)));
				i += 2;
				continue;
			}

			byte[] utf8 = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
			for (byte b : utf8) {
				result.append('%');
				int unsigned = b & 0xFF;
				char upper = Character.toUpperCase(Character.forDigit((unsigned >> 4) & 0x0F, 16));
				char lower = Character.toUpperCase(Character.forDigit(unsigned & 0x0F, 16));
				result.append(upper).append(lower);
			}
		}
		return result.toString();
	}

	private static boolean isUnreserved(char c) {
		return (c >= 'A' && c <= 'Z')
				|| (c >= 'a' && c <= 'z')
				|| (c >= '0' && c <= '9')
				|| c == '-'
				|| c == '_'
				|| c == '.'
				|| c == '~';
	}

	private static boolean isHex(char c) {
		return (c >= '0' && c <= '9')
				|| (c >= 'a' && c <= 'f')
				|| (c >= 'A' && c <= 'F');
	}

	private static byte[] getSignatureKey(String secretKey, String dateStamp, String regionName,
			String serviceName) throws Exception {
		byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
		byte[] kDate = hmacSha256(kSecret, dateStamp);
		byte[] kRegion = hmacSha256(kDate, regionName);
		byte[] kService = hmacSha256(kRegion, serviceName);
		return hmacSha256(kService, "aws4_request");
	}

	private static byte[] hmacSha256(byte[] key, String data) throws Exception {
		Mac mac = Mac.getInstance(HMAC_SHA256);
		mac.init(new SecretKeySpec(key, HMAC_SHA256));
		return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
	}

	private static String sha256Hex(String text) throws Exception {
		MessageDigest md = MessageDigest.getInstance(SHA256);
		byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
		return toHex(digest);
	}

	private static String toHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		final char[] hexArray = "0123456789abcdef".toCharArray();
		for (int i = 0; i < bytes.length; i++) {
			int value = bytes[i] & 0xFF;
			hexChars[i * 2] = hexArray[value >>> 4];
			hexChars[i * 2 + 1] = hexArray[value & 0x0F];
		}
		return new String(hexChars);
	}

	private static String formatAmzDate(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(date);
	}

	private static String formatDateStamp(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(date);
	}

	public static final class SigningResult {
		private final String authorizationHeader;
		private final String amzDate;
		private final String sessionToken;

		private SigningResult(String authorizationHeader, String amzDate,
				@Nullable String sessionToken) {
			this.authorizationHeader = authorizationHeader;
			this.amzDate = amzDate;
			this.sessionToken = sessionToken;
		}

		@NonNull
		public String getAuthorizationHeader() {
			return authorizationHeader;
		}

		@NonNull
		public String getAmzDate() {
			return amzDate;
		}

		@Nullable
		public String getSessionToken() {
			return sessionToken;
		}
	}
}
