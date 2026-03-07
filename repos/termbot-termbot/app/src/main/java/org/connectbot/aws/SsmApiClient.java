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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class SsmApiClient {
	private static final String SERVICE = "ssm";
	private static final String START_SESSION_TARGET = "AmazonSSM.StartSession";
	private static final String CONTENT_TYPE = "application/x-amz-json-1.1";

	private SsmApiClient() {
	}

	@NonNull
	public static SsmSessionStartResult startSession(
			@NonNull String region,
			@NonNull String target,
			@NonNull AwsCredentials credentials
	) throws IOException {
		String host = "ssm." + region + ".amazonaws.com";
		URL url = new URL("https://" + host + "/");
		JSONObject payloadJson = new JSONObject();
		try {
			payloadJson.put("Target", target);
		} catch (Exception e) {
			throw new IOException("Unable to build SSM StartSession payload", e);
		}
		String payload = payloadJson.toString();

		AwsV4Signer.SigningResult signingResult;
		try {
			signingResult = AwsV4Signer.signJsonPost(
					SERVICE,
					region,
					host,
					START_SESSION_TARGET,
					payload,
					credentials,
					null);
		} catch (Exception e) {
			throw new IOException("Unable to sign SSM request", e);
		}

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(20000);
		connection.setRequestProperty("Content-Type", CONTENT_TYPE);
		connection.setRequestProperty("X-Amz-Target", START_SESSION_TARGET);
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
			throw new IOException("SSM StartSession failed (HTTP " + statusCode + "): "
					+ simplifyAwsError(responseBody));
		}

		try {
			JSONObject response = new JSONObject(responseBody);
			String sessionId = response.optString("SessionId");
			String streamUrl = response.optString("StreamUrl");
			String tokenValue = response.optString("TokenValue");
			if (sessionId.isEmpty() || streamUrl.isEmpty() || tokenValue.isEmpty()) {
				throw new IOException("SSM StartSession response missing required fields");
			}
			return new SsmSessionStartResult(sessionId, streamUrl, tokenValue);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Unable to parse SSM StartSession response", e);
		}
	}

	@NonNull
	private static String simplifyAwsError(@NonNull String body) {
		if (body.isEmpty()) {
			return "empty error response";
		}

		try {
			JSONObject json = new JSONObject(body);
			String code = json.optString("__type");
			String message = json.optString("message");
			if (message.isEmpty()) {
				message = json.optString("Message");
			}
			if (!code.isEmpty() && !message.isEmpty()) {
				return code + " - " + message;
			}
			if (!message.isEmpty()) {
				return message;
			}
		} catch (Exception ignored) {
			// Fall back to generic response handling.
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
}
