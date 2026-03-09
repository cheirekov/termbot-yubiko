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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;

public final class StsApiClient {
	private static final String SERVICE = "sts";
	private static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";
	private static final String VERSION = "2011-06-15";

	private StsApiClient() {
	}

	@NonNull
	public static AwsCredentials assumeRole(
			@NonNull String region,
			@NonNull String roleArn,
			@NonNull String roleSessionName,
			@NonNull AwsCredentials sourceCredentials
	) throws IOException {
		return assumeRole(region, roleArn, roleSessionName, sourceCredentials, null, null);
	}

	@NonNull
	public static AwsCredentials assumeRole(
			@NonNull String region,
			@NonNull String roleArn,
			@NonNull String roleSessionName,
			@NonNull AwsCredentials sourceCredentials,
			@Nullable String mfaSerialNumber,
			@Nullable String mfaTokenCode
	) throws IOException {
		String payload = "Action=AssumeRole"
				+ "&Version=" + encode(VERSION)
				+ "&RoleArn=" + encode(roleArn)
				+ "&RoleSessionName=" + encode(roleSessionName);
		if (!isEmpty(mfaSerialNumber) && !isEmpty(mfaTokenCode)) {
			payload += "&SerialNumber=" + encode(mfaSerialNumber)
					+ "&TokenCode=" + encode(mfaTokenCode);
		}
		return executeCredentialRequest(region, payload, sourceCredentials, "AssumeRole");
	}

	@NonNull
	public static AwsCredentials getSessionToken(
			@NonNull String region,
			@NonNull AwsCredentials sourceCredentials,
			@NonNull String mfaSerialNumber,
			@NonNull String mfaTokenCode
	) throws IOException {
		String payload = "Action=GetSessionToken"
				+ "&Version=" + encode(VERSION)
				+ "&SerialNumber=" + encode(mfaSerialNumber)
				+ "&TokenCode=" + encode(mfaTokenCode);
		return executeCredentialRequest(region, payload, sourceCredentials, "GetSessionToken");
	}

	@NonNull
	private static AwsCredentials executeCredentialRequest(
			@NonNull String region,
			@NonNull String payload,
			@NonNull AwsCredentials sourceCredentials,
			@NonNull String actionName
	) throws IOException {
		String host = "sts." + region + ".amazonaws.com";
		URL url = new URL("https://" + host + "/");

		AwsV4Signer.SigningResult signingResult;
		try {
			signingResult = AwsV4Signer.signFormPost(
					SERVICE,
					region,
					host,
					payload,
					sourceCredentials,
					null);
		} catch (Exception e) {
			throw new IOException("Unable to sign STS " + actionName + " request", e);
		}

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(20000);
		connection.setRequestProperty("Content-Type", CONTENT_TYPE);
		connection.setRequestProperty("X-Amz-Date", signingResult.getAmzDate());
		connection.setRequestProperty("Authorization", signingResult.getAuthorizationHeader());
		if (signingResult.getSessionToken() != null && !signingResult.getSessionToken().isEmpty()) {
			connection.setRequestProperty("X-Amz-Security-Token", signingResult.getSessionToken());
		}

		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
		connection.setFixedLengthStreamingMode(payloadBytes.length);
		try (OutputStream outputStream = connection.getOutputStream()) {
			outputStream.write(payloadBytes);
		}

		int statusCode = connection.getResponseCode();
		String responseBody = readBody(
				statusCode >= 200 && statusCode < 300 ? connection.getInputStream()
						: connection.getErrorStream());

		if (statusCode < 200 || statusCode >= 300) {
			throw new IOException("STS " + actionName + " failed (HTTP " + statusCode + "): "
					+ simplifyError(responseBody));
		}

		return parseCredentials(responseBody);
	}

	@NonNull
	private static AwsCredentials parseCredentials(@NonNull String xmlBody) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(xmlBody)));
			String accessKeyId = getFirstTag(document, "AccessKeyId");
			String secretAccessKey = getFirstTag(document, "SecretAccessKey");
			String sessionToken = getFirstTag(document, "SessionToken");
			if (isEmpty(accessKeyId) || isEmpty(secretAccessKey) || isEmpty(sessionToken)) {
				throw new IOException("STS response missing required credential fields");
			}
			return new AwsCredentials(accessKeyId, secretAccessKey, sessionToken);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Unable to parse STS credential response", e);
		}
	}

	@NonNull
	private static String getFirstTag(@NonNull Document document, @NonNull String name) {
		NodeList nodes = document.getElementsByTagName(name);
		if (nodes == null || nodes.getLength() <= 0 || nodes.item(0) == null
				|| nodes.item(0).getTextContent() == null) {
			return "";
		}
		return nodes.item(0).getTextContent();
	}

	@NonNull
	private static String simplifyError(@NonNull String body) {
		if (body.isEmpty()) {
			return "empty error response";
		}

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(body)));
			String code = getFirstTag(document, "Code");
			String message = getFirstTag(document, "Message");
			if (!isEmpty(code) && !isEmpty(message)) {
				return code + " - " + message;
			}
			if (!isEmpty(message)) {
				return message;
			}
		} catch (Exception ignored) {
			// Fall back to generic body handling.
		}

		String sanitized = body.replace('\n', ' ').replace('\r', ' ').trim();
		if (sanitized.length() > 240) {
			return sanitized.substring(0, 240) + "...";
		}
		return sanitized;
	}

	@NonNull
	private static String readBody(InputStream stream) throws IOException {
		if (stream == null) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
		}
		return result.toString();
	}

	@NonNull
	private static String encode(@NonNull String value) throws IOException {
		return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
	}

	private static boolean isEmpty(String value) {
		return value == null || value.isEmpty();
	}
}
