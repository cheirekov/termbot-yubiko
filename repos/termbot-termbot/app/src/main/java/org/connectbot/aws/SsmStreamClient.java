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
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Minimal Session Manager stream bridge for shell sessions.
 *
 * Scope of this implementation:
 * - Establish authenticated websocket channel using StartSession stream URL.
 * - Send open-data-channel token handshake.
 * - Relay output payloads and accept input payloads.
 * - Handle acknowledgement and basic handshake request/response payloads.
 */
public final class SsmStreamClient {
	private static final String SERVICE_NAME = "ssmmessages";
	private static final String MESSAGE_SCHEMA_VERSION = "1.0";
	private static final String CLIENT_VERSION = "1.0.0-termbot";
	private static final int CONNECT_TIMEOUT_SECONDS = 20;
	private static final int INPUT_CHUNK_SIZE = 1024;

	private static final String MESSAGE_TYPE_INPUT_STREAM = "input_stream_data";
	private static final String MESSAGE_TYPE_OUTPUT_STREAM = "output_stream_data";
	private static final String MESSAGE_TYPE_ACKNOWLEDGE = "acknowledge";
	private static final String MESSAGE_TYPE_CHANNEL_CLOSED = "channel_closed";
	private static final String MESSAGE_TYPE_START_PUBLICATION = "start_publication";
	private static final String MESSAGE_TYPE_PAUSE_PUBLICATION = "pause_publication";

	private static final int PAYLOAD_OUTPUT = 1;
	private static final int PAYLOAD_SIZE = 3;
	private static final int PAYLOAD_HANDSHAKE_REQUEST = 5;
	private static final int PAYLOAD_HANDSHAKE_RESPONSE = 6;
	private static final int PAYLOAD_HANDSHAKE_COMPLETE = 7;
	private static final int PAYLOAD_FLAG = 10;
	private static final int PAYLOAD_STDERR = 11;
	private static final int PAYLOAD_EXIT_CODE = 12;

	private static final int ACTION_STATUS_SUCCESS = 1;
	private static final int ACTION_STATUS_FAILED = 2;
	private static final int ACTION_STATUS_UNSUPPORTED = 3;

	private static final OkHttpClient SHARED_WEBSOCKET_CLIENT = new OkHttpClient.Builder()
			.pingInterval(5, TimeUnit.MINUTES)
			.build();

	public interface Callback {
		void onStdout(@NonNull byte[] payload);

		void onStderr(@NonNull byte[] payload);

		void onFlag(@NonNull byte[] payload);

		void onInfo(@NonNull String message);

		void onError(@NonNull IOException error);

		void onClosed(@Nullable String reason);
	}

	private final String streamUrl;
	private final String tokenValue;
	private final String region;
	private final AwsCredentials credentials;
	private final Callback callback;
	private final String clientId = UUID.randomUUID().toString();
	private final AtomicLong streamDataSequenceNumber = new AtomicLong(0);
	private final AtomicBoolean open = new AtomicBoolean(false);
	private final AtomicReference<IOException> startupFailure = new AtomicReference<>();
	private final CountDownLatch handshakeCompleteLatch = new CountDownLatch(1);
	private final Object sendLock = new Object();

	private volatile WebSocket webSocket;
	private volatile boolean closedByClient;
	private volatile String agentVersion;
	private volatile String sessionType;
	private volatile String sessionPropertiesType;

	public SsmStreamClient(
			@NonNull String streamUrl,
			@NonNull String tokenValue,
			@NonNull String region,
			@NonNull AwsCredentials credentials,
			@NonNull Callback callback
	) {
		this.streamUrl = streamUrl;
		this.tokenValue = tokenValue;
		this.region = region;
		this.credentials = credentials;
		this.callback = callback;
	}

	public void connect() throws IOException {
		final URI uri;
		try {
			uri = URI.create(streamUrl);
		} catch (Exception e) {
			throw new IOException("Invalid SSM stream URL", e);
		}

		final AwsV4Signer.SigningResult signingResult;
		try {
			signingResult = AwsV4Signer.signGet(SERVICE_NAME, region, uri, credentials, null);
		} catch (Exception e) {
			throw new IOException("Unable to sign SSM websocket request", e);
		}

		Request.Builder requestBuilder = new Request.Builder()
				.url(streamUrl)
				.addHeader("Authorization", signingResult.getAuthorizationHeader())
				.addHeader("X-Amz-Date", signingResult.getAmzDate());
		if (signingResult.getSessionToken() != null && !signingResult.getSessionToken().isEmpty()) {
			requestBuilder.addHeader("X-Amz-Security-Token", signingResult.getSessionToken());
		}

		final CountDownLatch openLatch = new CountDownLatch(1);
		startupFailure.set(null);
		closedByClient = false;
		webSocket = SHARED_WEBSOCKET_CLIENT.newWebSocket(
				requestBuilder.build(),
				new StreamWebSocketListener(openLatch));

		try {
			boolean completed = openLatch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!completed) {
				close();
				throw new IOException("Timed out opening SSM stream websocket");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			close();
			throw new IOException("Interrupted while opening SSM stream websocket", e);
		}

		IOException error = startupFailure.get();
		if (error != null) {
			close();
			throw error;
		}

		if (!open.get()) {
			throw new IOException("SSM stream websocket did not open");
		}
	}

	public void sendInput(@NonNull byte[] payload) throws IOException {
		sendInputInternal(payload, true);
	}

	public void sendPortInput(@NonNull byte[] payload) throws IOException {
		sendInputInternal(payload, false);
	}

	private void sendInputInternal(@NonNull byte[] payload, boolean normalizeTerminalNewlines)
			throws IOException {
		if (payload.length == 0) {
			return;
		}

		for (int offset = 0; offset < payload.length; offset += INPUT_CHUNK_SIZE) {
			int chunkSize = Math.min(INPUT_CHUNK_SIZE, payload.length - offset);
			byte[] chunk = Arrays.copyOfRange(payload, offset, offset + chunkSize);
			if (normalizeTerminalNewlines && chunk.length == 1 && chunk[0] == 10) {
				chunk = new byte[] { 13 };
			}
			sendInputPayload(PAYLOAD_OUTPUT, chunk);
		}
	}

	public void sendTerminalSize(int columns, int rows) throws IOException {
		JSONObject payload = new JSONObject();
		try {
			payload.put("cols", columns);
			payload.put("rows", rows);
		} catch (Exception e) {
			throw new IOException("Unable to encode SSM terminal size payload", e);
		}
		sendInputPayload(PAYLOAD_SIZE, payload.toString().getBytes(StandardCharsets.UTF_8));
	}

	public boolean isOpen() {
		return open.get();
	}

	public boolean awaitHandshakeComplete(long timeout, @NonNull TimeUnit unit)
			throws InterruptedException {
		return handshakeCompleteLatch.await(timeout, unit);
	}

	@Nullable
	public String getAgentVersion() {
		return agentVersion;
	}

	@Nullable
	public String getSessionType() {
		return sessionType;
	}

	@Nullable
	public String getSessionPropertiesType() {
		return sessionPropertiesType;
	}

	public void close() {
		closedByClient = true;
		open.set(false);
		WebSocket socket = webSocket;
		if (socket != null) {
			socket.close(1000, "client closing");
		}
	}

	private void handleBinaryMessage(@NonNull byte[] rawMessage) throws IOException {
		SsmClientMessage incoming = SsmClientMessage.deserialize(rawMessage);
		switch (incoming.messageType) {
			case MESSAGE_TYPE_OUTPUT_STREAM:
				handleOutputStreamDataMessage(incoming);
				break;
			case MESSAGE_TYPE_ACKNOWLEDGE:
				// ACKs for outbound data are currently observed but not buffered/retransmitted.
				break;
			case MESSAGE_TYPE_CHANNEL_CLOSED:
				handleChannelClosedMessage(incoming);
				break;
			case MESSAGE_TYPE_START_PUBLICATION:
			case MESSAGE_TYPE_PAUSE_PUBLICATION:
				// Accepted as control messages. No extra action needed in this transport stage.
				break;
			default:
				break;
		}
	}

	private void handleOutputStreamDataMessage(@NonNull SsmClientMessage incoming) throws IOException {
		sendAcknowledge(incoming);

		switch (incoming.payloadType) {
			case PAYLOAD_HANDSHAKE_REQUEST:
				handleHandshakeRequest(incoming.payload);
				return;
			case PAYLOAD_HANDSHAKE_COMPLETE:
				handleHandshakeComplete(incoming.payload);
				return;
			case PAYLOAD_OUTPUT:
				if (incoming.payload.length > 0) {
					callback.onStdout(incoming.payload);
				}
				return;
			case PAYLOAD_STDERR:
				if (incoming.payload.length > 0) {
					callback.onStderr(incoming.payload);
				}
				return;
			case PAYLOAD_FLAG:
				if (incoming.payload.length > 0) {
					callback.onFlag(incoming.payload);
				}
				return;
			case PAYLOAD_EXIT_CODE:
				handleExitCode(incoming.payload);
				return;
			default:
				return;
		}
	}

	private void handleChannelClosedMessage(@NonNull SsmClientMessage incoming) {
		String output = null;
		if (incoming.payload.length > 0) {
			try {
				JSONObject payload = new JSONObject(new String(incoming.payload, StandardCharsets.UTF_8));
				String parsedOutput = payload.optString("Output", "").trim();
				if (!parsedOutput.isEmpty()) {
					output = parsedOutput;
				}
			} catch (Exception ignored) {
				// Best-effort parse only.
			}
		}
		callback.onClosed(output);
	}

	private void handleHandshakeRequest(@NonNull byte[] payloadBytes) throws IOException {
		JSONObject handshakeRequest;
		try {
			handshakeRequest = new JSONObject(new String(payloadBytes, StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new IOException("Unable to parse SSM handshake request", e);
		}
		agentVersion = safeTrim(handshakeRequest.optString("AgentVersion", null));

		JSONArray processedActions = new JSONArray();
		JSONArray errors = new JSONArray();
			JSONArray requestedActions = handshakeRequest.optJSONArray("RequestedClientActions");
			if (requestedActions != null) {
				for (int i = 0; i < requestedActions.length(); i++) {
					JSONObject action = requestedActions.optJSONObject(i);
					if (action == null) {
						continue;
					}
					captureSessionTypeMetadata(action);
					processedActions.put(processAction(action, errors));
				}
			}

		JSONObject response = new JSONObject();
		try {
			response.put("ClientVersion", CLIENT_VERSION);
			response.put("ProcessedClientActions", processedActions);
			response.put("Errors", errors);
		} catch (Exception e) {
			throw new IOException("Unable to encode SSM handshake response", e);
		}
		sendInputPayload(PAYLOAD_HANDSHAKE_RESPONSE,
				response.toString().getBytes(StandardCharsets.UTF_8));
	}

	private JSONObject processAction(@NonNull JSONObject action, @NonNull JSONArray errors) {
		JSONObject processed = new JSONObject();
		String actionType = action.optString("ActionType", "");
		String actionError;
		try {
			processed.put("ActionType", actionType);
			if ("SessionType".equals(actionType)) {
				JSONObject parameters = action.optJSONObject("ActionParameters");
				String sessionType = parameters != null
						? parameters.optString("SessionType", "")
						: "";
				if (isSupportedSessionType(sessionType)) {
					processed.put("ActionStatus", ACTION_STATUS_SUCCESS);
				} else {
					actionError = "Unsupported SessionType: " + sessionType;
					processed.put("ActionStatus", ACTION_STATUS_FAILED);
					processed.put("Error", actionError);
					errors.put(actionError);
				}
			} else if ("KMSEncryption".equals(actionType)) {
				actionError = "KMSEncryption handshake is not supported in this build";
				processed.put("ActionStatus", ACTION_STATUS_UNSUPPORTED);
				processed.put("ActionResult", "Unsupported");
				processed.put("Error", actionError);
				errors.put(actionError);
			} else {
				actionError = "Unsupported action: " + actionType;
				processed.put("ActionStatus", ACTION_STATUS_UNSUPPORTED);
				processed.put("ActionResult", "Unsupported");
				processed.put("Error", actionError);
				errors.put(actionError);
			}
		} catch (Exception ignored) {
			// Best-effort fill; malformed action response should not crash the transport.
		}
		return processed;
	}

	private boolean isSupportedSessionType(@NonNull String sessionType) {
		return "Standard_Stream".equals(sessionType)
				|| "InteractiveCommands".equals(sessionType)
				|| "NonInteractiveCommands".equals(sessionType)
				|| "Port".equals(sessionType);
	}

	private void handleHandshakeComplete(@NonNull byte[] payload) {
		try {
			if (payload.length > 0) {
				JSONObject json = new JSONObject(new String(payload, StandardCharsets.UTF_8));
				String customerMessage = json.optString("CustomerMessage", "").trim();
				if (!customerMessage.isEmpty()) {
					callback.onInfo(customerMessage);
				}
			}
		} catch (Exception ignored) {
			// Optional payload.
		} finally {
			handshakeCompleteLatch.countDown();
		}
	}

	@Nullable
	private static String safeTrim(@Nullable String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private void captureSessionTypeMetadata(@NonNull JSONObject action) {
		if (!"SessionType".equals(action.optString("ActionType", ""))) {
			return;
		}

		JSONObject parameters = action.optJSONObject("ActionParameters");
		if (parameters == null) {
			return;
		}

		sessionType = safeTrim(parameters.optString("SessionType", null));
		JSONObject properties = parameters.optJSONObject("Properties");
		if (properties != null) {
			sessionPropertiesType = safeTrim(properties.optString("Type", null));
		}
	}

	private void handleExitCode(@NonNull byte[] payload) {
		if (payload.length == 0) {
			return;
		}
		String exit = new String(payload, StandardCharsets.UTF_8).trim();
		if (!exit.isEmpty()) {
			callback.onInfo("SSM exit code: " + exit);
		}
	}

	private void sendAcknowledge(@NonNull SsmClientMessage incoming) throws IOException {
		JSONObject ackContent = new JSONObject();
		try {
			ackContent.put("AcknowledgedMessageType", incoming.messageType);
			ackContent.put("AcknowledgedMessageId", incoming.messageId.toString());
			ackContent.put("AcknowledgedMessageSequenceNumber", incoming.sequenceNumber);
			ackContent.put("IsSequentialMessage", true);
		} catch (Exception e) {
			throw new IOException("Unable to encode SSM acknowledge payload", e);
		}

		byte[] serialized = SsmClientMessage.serializeAcknowledge(
				ackContent.toString().getBytes(StandardCharsets.UTF_8));
		sendBinary(serialized);
	}

	private void sendInputPayload(int payloadType, @NonNull byte[] payload) throws IOException {
		long sequence = streamDataSequenceNumber.getAndIncrement();
		byte[] serialized = SsmClientMessage.serializeInput(payloadType, sequence, payload);
		sendBinary(serialized);
	}

	private void sendBinary(@NonNull byte[] payload) throws IOException {
		synchronized (sendLock) {
			if (!open.get() || webSocket == null) {
				throw new IOException("SSM stream websocket is not open");
			}
			boolean sent = webSocket.send(ByteString.of(payload));
			if (!sent) {
				throw new IOException("Failed to send SSM stream message");
			}
		}
	}

	private void sendOpenDataChannelToken(@NonNull WebSocket ws) throws IOException {
		JSONObject openDataChannel = new JSONObject();
		try {
			openDataChannel.put("MessageSchemaVersion", MESSAGE_SCHEMA_VERSION);
			openDataChannel.put("RequestId", UUID.randomUUID().toString());
			openDataChannel.put("TokenValue", tokenValue);
			openDataChannel.put("ClientId", clientId);
			openDataChannel.put("ClientVersion", CLIENT_VERSION);
		} catch (Exception e) {
			throw new IOException("Unable to encode SSM open-data-channel payload", e);
		}

		if (!ws.send(openDataChannel.toString())) {
			throw new IOException("Failed to send SSM open-data-channel payload");
		}
	}

	private final class StreamWebSocketListener extends WebSocketListener {
		private final CountDownLatch openLatch;

		private StreamWebSocketListener(CountDownLatch openLatch) {
			this.openLatch = openLatch;
		}

		@Override
		public void onOpen(WebSocket webSocket, Response response) {
			open.set(true);
			try {
				sendOpenDataChannelToken(webSocket);
			} catch (IOException e) {
				startupFailure.compareAndSet(null, e);
				open.set(false);
				webSocket.close(1011, "handshake failed");
			}
			openLatch.countDown();
		}

		@Override
		public void onMessage(WebSocket webSocket, String text) {
			// Service payloads are expected to arrive as binary client messages.
		}

		@Override
		public void onMessage(WebSocket webSocket, ByteString bytes) {
			try {
				handleBinaryMessage(bytes.toByteArray());
			} catch (IOException e) {
				callback.onError(e);
				webSocket.close(1011, "protocol error");
			}
		}

		@Override
		public void onClosing(WebSocket webSocket, int code, String reason) {
			open.set(false);
			webSocket.close(code, reason);
		}

		@Override
		public void onClosed(WebSocket webSocket, int code, String reason) {
			open.set(false);
			openLatch.countDown();
			if (!closedByClient) {
				callback.onClosed(sanitizeCloseReason(reason));
			}
		}

		@Override
		public void onFailure(WebSocket webSocket, Throwable t, Response response) {
			open.set(false);
			String message = "SSM stream websocket failure";
			if (response != null) {
				message += " (HTTP " + response.code() + ")";
			}
			IOException failure = new IOException(message, t);
			startupFailure.compareAndSet(null, failure);
			openLatch.countDown();
			if (!closedByClient) {
				callback.onError(failure);
			}
		}
	}

	@Nullable
	private String sanitizeCloseReason(@Nullable String reason) {
		if (reason == null) {
			return null;
		}
		String trimmed = reason.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static final class SsmClientMessage {
		private static final int MESSAGE_TYPE_LENGTH = 32;
		private static final int PAYLOAD_DIGEST_LENGTH = 32;
		private static final int HEADER_LENGTH = 116;
		private static final int PAYLOAD_LENGTH_FIELD_SIZE = 4;
		private static final int PAYLOAD_OFFSET = HEADER_LENGTH + PAYLOAD_LENGTH_FIELD_SIZE;

		private static final int OFFSET_MESSAGE_TYPE = 4;
		private static final int OFFSET_SEQUENCE_NUMBER = 48;
		private static final int OFFSET_MESSAGE_ID = 64;
		private static final int OFFSET_PAYLOAD_DIGEST = 80;
		private static final int OFFSET_PAYLOAD_TYPE = 112;
		private static final int OFFSET_PAYLOAD_LENGTH = 116;

		private final String messageType;
		private final long sequenceNumber;
		private final UUID messageId;
		private final int payloadType;
		private final byte[] payload;

		private SsmClientMessage(
				String messageType,
				long sequenceNumber,
				UUID messageId,
				int payloadType,
				byte[] payload
		) {
			this.messageType = messageType;
			this.sequenceNumber = sequenceNumber;
			this.messageId = messageId;
			this.payloadType = payloadType;
			this.payload = payload;
		}

		private static byte[] serializeInput(int payloadType, long sequenceNumber, byte[] payload)
				throws IOException {
			return serialize(
					MESSAGE_TYPE_INPUT_STREAM,
					sequenceNumber,
					0L,
					UUID.randomUUID(),
					payloadType,
					payload);
		}

		private static byte[] serializeAcknowledge(byte[] acknowledgePayload) throws IOException {
			return serialize(
					MESSAGE_TYPE_ACKNOWLEDGE,
					0L,
					3L,
					UUID.randomUUID(),
					0,
					acknowledgePayload);
		}

		private static byte[] serialize(
				String messageType,
				long sequenceNumber,
				long flags,
				UUID messageId,
				int payloadType,
				byte[] payload
		) throws IOException {
			int payloadLength = payload.length;
			ByteBuffer buffer = ByteBuffer
					.allocate(HEADER_LENGTH + PAYLOAD_LENGTH_FIELD_SIZE + payloadLength)
					.order(ByteOrder.BIG_ENDIAN);

			buffer.putInt(HEADER_LENGTH);
			putFixedString(buffer, messageType, MESSAGE_TYPE_LENGTH);
			buffer.putInt(1);
			buffer.putLong(System.currentTimeMillis());
			buffer.putLong(sequenceNumber);
			buffer.putLong(flags);
			putUuid(buffer, messageId);
			buffer.put(sha256(payload));
			buffer.putInt(payloadType);
			buffer.putInt(payloadLength);
			buffer.put(payload);
			return buffer.array();
		}

		private static SsmClientMessage deserialize(byte[] raw) throws IOException {
			if (raw.length < PAYLOAD_OFFSET) {
				throw new IOException("SSM stream message is too short");
			}

			String messageType = readFixedString(raw, OFFSET_MESSAGE_TYPE, MESSAGE_TYPE_LENGTH).trim();
			int headerLength = readInt(raw, 0);
			if (headerLength < HEADER_LENGTH) {
				throw new IOException("Invalid SSM message header length: " + headerLength);
			}

			int payloadLength = readInt(raw, OFFSET_PAYLOAD_LENGTH);
			if (payloadLength < 0) {
				throw new IOException("Invalid SSM payload length: " + payloadLength);
			}
			int payloadStart = headerLength + PAYLOAD_LENGTH_FIELD_SIZE;
			if (payloadStart < 0 || payloadStart > raw.length) {
				throw new IOException("Invalid SSM payload offset");
			}
			if (payloadStart + payloadLength > raw.length) {
				throw new IOException("SSM payload length exceeds frame size");
			}

			UUID messageId = readUuid(raw, OFFSET_MESSAGE_ID);
			long sequenceNumber = readLong(raw, OFFSET_SEQUENCE_NUMBER);
			int payloadType = readInt(raw, OFFSET_PAYLOAD_TYPE);
			byte[] payload = Arrays.copyOfRange(raw, payloadStart, payloadStart + payloadLength);

			if (!MESSAGE_TYPE_START_PUBLICATION.equals(messageType)
					&& !MESSAGE_TYPE_PAUSE_PUBLICATION.equals(messageType)
					&& payloadLength > 0) {
				byte[] expectedDigest = Arrays.copyOfRange(
						raw,
						OFFSET_PAYLOAD_DIGEST,
						OFFSET_PAYLOAD_DIGEST + PAYLOAD_DIGEST_LENGTH);
				byte[] calculatedDigest = sha256(payload);
				if (!Arrays.equals(expectedDigest, calculatedDigest)) {
					throw new IOException("SSM payload digest mismatch");
				}
			}

			return new SsmClientMessage(messageType, sequenceNumber, messageId, payloadType, payload);
		}

		private static int readInt(byte[] raw, int offset) {
			return ByteBuffer.wrap(raw, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
		}

		private static long readLong(byte[] raw, int offset) {
			return ByteBuffer.wrap(raw, offset, 8).order(ByteOrder.BIG_ENDIAN).getLong();
		}

		private static UUID readUuid(byte[] raw, int offset) {
			ByteBuffer uuidBuffer = ByteBuffer.wrap(raw, offset, 16).order(ByteOrder.BIG_ENDIAN);
			long leastSignificantBits = uuidBuffer.getLong();
			long mostSignificantBits = uuidBuffer.getLong();
			return new UUID(mostSignificantBits, leastSignificantBits);
		}

		private static String readFixedString(byte[] raw, int offset, int length) {
			return new String(raw, offset, length, StandardCharsets.UTF_8).replace("\u0000", "");
		}

		private static void putFixedString(ByteBuffer buffer, String value, int fixedLength) {
			byte[] data = value.getBytes(StandardCharsets.UTF_8);
			byte[] padded = new byte[fixedLength];
			Arrays.fill(padded, (byte) ' ');
			System.arraycopy(data, 0, padded, 0, Math.min(data.length, fixedLength));
			buffer.put(padded);
		}

		private static void putUuid(ByteBuffer buffer, UUID uuid) {
			buffer.putLong(uuid.getLeastSignificantBits());
			buffer.putLong(uuid.getMostSignificantBits());
		}

		private static byte[] sha256(byte[] payload) throws IOException {
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				return digest.digest(payload);
			} catch (Exception e) {
				throw new IOException("Unable to hash SSM payload", e);
			}
		}
	}
}
