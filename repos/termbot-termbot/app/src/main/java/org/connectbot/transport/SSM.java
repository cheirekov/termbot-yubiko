/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2007 Kenny Root, Jeffrey Sharkey
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

package org.connectbot.transport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.connectbot.R;
import org.connectbot.aws.AwsCredentials;
import org.connectbot.aws.SsmApiClient;
import org.connectbot.aws.SsmCredentialResolver;
import org.connectbot.aws.SsmStreamClient;
import org.connectbot.aws.SsmSessionStartResult;
import org.connectbot.aws.StsApiClient;
import org.connectbot.bean.HostBean;
import org.connectbot.util.HostDatabase;
import org.connectbot.util.SavedPasswordStore;
import org.connectbot.util.SecurityKeyDebugLog;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * SSM transport backed by AWS Session Manager StartSession + websocket stream.
 */
public class SSM extends AbsTransport {
	private static final String TAG = "CB.SSM";
	private static final String PROTOCOL = "ssm";
	private static final int DEFAULT_PORT = 443;
	private static final String FLOW_MARKER = "SSM_FLOW";
	private static final int READ_WAIT_MILLIS = 500;

	private volatile boolean connected;
	private volatile boolean sessionOpen;
	private volatile SsmStreamClient streamClient;
	private final LinkedBlockingQueue<byte[]> inboundPayloadQueue = new LinkedBlockingQueue<>();
	private final AtomicBoolean disconnectDispatched = new AtomicBoolean(false);
	private byte[] pendingInboundChunk;
	private int pendingInboundOffset;

	public static String getProtocolName() {
		return PROTOCOL;
	}

	@Override
	public void connect() {
		connected = false;
		sessionOpen = false;
		pendingInboundChunk = null;
		pendingInboundOffset = 0;
		inboundPayloadQueue.clear();
		disconnectDispatched.set(false);

		if (bridge == null || manager == null || host == null) {
			return;
		}

		String accessKeyId = safeTrim(host.getUsername());
		String region = safeTrim(host.getHostname());
		String target = safeTrim(host.getPostLogin());
		final String roleArn = safeTrim(host.getSsmRoleArn());
		if (accessKeyId == null || region == null || target == null) {
			bridge.outputLine(manager.res.getString(R.string.ssm_missing_configuration));
			bridge.dispatchDisconnect(false);
			return;
		}

		SsmCredentialResolver credentialResolver = new SsmCredentialResolver(
				SavedPasswordStore.get(manager.getApplicationContext()),
				roleArn == null ? null : new SsmCredentialResolver.SessionCredentialEnhancer() {
					@Override
					public boolean requiresMfaCode() {
						return false;
					}

					@Override
					public String getMfaPromptHint() {
						return null;
					}

					@Override
					public AwsCredentials enhance(AwsCredentials baseCredentials, String mfaCode)
							throws IOException {
						return StsApiClient.assumeRole(
								region,
								roleArn,
								buildRoleSessionName(),
								baseCredentials);
					}
				});

		SsmCredentialResolver.Resolution credentialResolution;
		try {
			credentialResolution = credentialResolver.resolve(host,
					new SsmCredentialResolver.PromptDelegate() {
						@Override
						public String requestSecretAccessKey() {
							return bridge.getPromptHelper().requestStringPrompt(null,
									manager.res.getString(R.string.prompt_aws_secret_access_key));
						}

						@Override
						public String requestMfaCode(String hint) {
							return bridge.getPromptHelper().requestStringPrompt(null, hint);
						}

						@Override
						public String requestSessionToken() {
							return bridge.getPromptHelper().requestStringPrompt(null,
									manager.res.getString(R.string.prompt_aws_session_token));
						}
					});
		} catch (SsmCredentialResolver.MissingSessionTokenException e) {
			trace("credential_resolution_failed reason=session_token_required"
					+ " assume_role_configured=" + (roleArn != null));
			bridge.outputLine(manager.res.getString(R.string.ssm_session_token_required));
			bridge.dispatchDisconnect(false);
			return;
		} catch (IOException e) {
			trace("credential_resolution_failed reason=" + summarizeError(e)
					+ " assume_role_configured=" + (roleArn != null));
			bridge.outputLine(manager.res.getString(
					R.string.ssm_credential_resolution_failed,
					summarizeError(e)));
			bridge.dispatchDisconnect(false);
			return;
		}

		if (credentialResolution == null) {
			bridge.outputLine(manager.res.getString(R.string.ssm_secret_key_required));
			bridge.dispatchDisconnect(false);
			return;
		}
		AwsCredentials sessionCredentials = credentialResolution.getRuntimeCredentials();
		AwsCredentials persistedCredentials = credentialResolution.getPersistedCredentials();

		trace("credential_resolved mode=" + credentialResolution.getCredentialMode()
				+ " secret_source=" + credentialResolution.getSecretSource()
				+ " session_token_source=" + credentialResolution.getSessionTokenSource()
				+ " mfa_prompted=" + credentialResolution.isMfaPrompted()
				+ " credential_enhanced=" + credentialResolution.isCredentialEnhanced()
				+ " assume_role_configured=" + (roleArn != null));

		if (!host.getRememberPassword()) {
			credentialResolver.clear(host.getId());
		}

		trace("start_session_request region=" + region + " target=" + target);
		bridge.outputLine(manager.res.getString(R.string.ssm_connecting, target, region));

		try {
			SsmSessionStartResult result = SsmApiClient.startSession(region, target,
					sessionCredentials);
			credentialResolver.persistIfEnabled(host, persistedCredentials);
			trace("start_session_ok session=" + summarizeSessionId(result.getSessionId())
					+ " stream=url");
			bridge.outputLine(manager.res.getString(
					R.string.ssm_start_session_ok_stub,
					summarizeSessionId(result.getSessionId())));

			SsmStreamClient client = new SsmStreamClient(
					result.getStreamUrl(),
					result.getTokenValue(),
					region,
					sessionCredentials,
					new SsmStreamClient.Callback() {
						@Override
						public void onStdout(byte[] payload) {
							if (payload != null && payload.length > 0) {
								inboundPayloadQueue.offer(payload);
							}
						}

						@Override
						public void onInfo(String message) {
							if (message == null) {
								return;
							}
							String trimmed = message.trim();
							if (trimmed.isEmpty()) {
								return;
							}
							inboundPayloadQueue.offer(("\r\n" + trimmed + "\r\n")
									.getBytes(StandardCharsets.UTF_8));
						}

						@Override
						public void onError(IOException error) {
							String summary = summarizeError(error);
							trace("stream_error reason=" + summary);
							inboundPayloadQueue.offer(("\r\nSSM stream error: " + summary
									+ "\r\n").getBytes(StandardCharsets.UTF_8));
							disconnectFromRemote();
						}

						@Override
						public void onClosed(String reason) {
							if (reason != null && !reason.trim().isEmpty()) {
								inboundPayloadQueue.offer(("\r\n" + reason.trim() + "\r\n")
										.getBytes(StandardCharsets.UTF_8));
							}
							trace("stream_closed reason=" + (reason == null ? "none" : reason));
							disconnectFromRemote();
						}
					});
			client.connect();
			streamClient = client;
			connected = true;
			sessionOpen = true;
			bridge.onConnected();
		} catch (IOException e) {
			trace("start_session_failed reason=" + summarizeError(e));
			bridge.outputLine(manager.res.getString(
					R.string.ssm_start_session_failed,
					summarizeError(e)));
			close();
			dispatchDisconnectOnce();
		}
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (length <= 0) {
			return 0;
		}

		while (true) {
			if (pendingInboundChunk != null) {
				int available = pendingInboundChunk.length - pendingInboundOffset;
				int toCopy = Math.min(length, available);
				System.arraycopy(pendingInboundChunk, pendingInboundOffset, buffer, offset, toCopy);
				pendingInboundOffset += toCopy;
				if (pendingInboundOffset >= pendingInboundChunk.length) {
					pendingInboundChunk = null;
					pendingInboundOffset = 0;
				}
				return toCopy;
			}

			if (!connected && inboundPayloadQueue.isEmpty()) {
				throw new IOException("SSM stream is closed");
			}

			try {
				byte[] chunk = inboundPayloadQueue.poll(READ_WAIT_MILLIS, TimeUnit.MILLISECONDS);
				if (chunk == null || chunk.length == 0) {
					if (!connected) {
						throw new IOException("SSM stream is closed");
					}
					continue;
				}
				pendingInboundChunk = chunk;
				pendingInboundOffset = 0;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while reading SSM stream", e);
			}
		}
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		if (!connected) {
			return;
		}
		SsmStreamClient client = streamClient;
		if (client == null || !client.isOpen()) {
			throw new IOException("SSM stream is not open");
		}
		client.sendInput(buffer);
	}

	@Override
	public void write(int c) throws IOException {
		if (!connected) {
			return;
		}
		write(new byte[] { (byte) c });
	}

	@Override
	public void flush() throws IOException {
		// No-op: websocket sends each frame immediately.
	}

	@Override
	public void close() {
		connected = false;
		sessionOpen = false;
		SsmStreamClient client = streamClient;
		streamClient = null;
		if (client != null) {
			client.close();
		}
	}

	@Override
	public void setDimensions(int columns, int rows, int width, int height) {
		SsmStreamClient client = streamClient;
		if (!connected || client == null || !client.isOpen()) {
			return;
		}
		try {
			client.sendTerminalSize(columns, rows);
		} catch (IOException e) {
			trace("set_dimensions_failed reason=" + summarizeError(e));
		}
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public boolean isSessionOpen() {
		return sessionOpen;
	}

	@Override
	public int getDefaultPort() {
		return DEFAULT_PORT;
	}

	@Override
	public String getDefaultNickname(String username, String hostname, int port) {
		String safeUser = username == null ? "access-key" : username;
		String safeHost = hostname == null ? "region" : hostname;
		if (port == DEFAULT_PORT) {
			return String.format(Locale.US, "%s@%s", safeUser, safeHost);
		}
		return String.format(Locale.US, "%s@%s:%d", safeUser, safeHost, port);
	}

	public static Uri getUri(String input) {
		if (input == null) {
			return null;
		}

		String rawValue = input.trim();
		if (rawValue.isEmpty()) {
			return null;
		}

		String value = rawValue;
		if (value.startsWith(PROTOCOL + "://")) {
			value = value.substring((PROTOCOL + "://").length());
		}

		String target = null;
		int slashIndex = value.indexOf('/');
		if (slashIndex >= 0) {
			target = value.substring(slashIndex + 1).trim();
			value = value.substring(0, slashIndex);
		}

		String user = null;
		int atIndex = value.indexOf('@');
		if (atIndex >= 0) {
			user = value.substring(0, atIndex).trim();
			value = value.substring(atIndex + 1);
		}

		String host = value.trim();
		int port = DEFAULT_PORT;
		int colonIndex = host.lastIndexOf(':');
		if (colonIndex > 0 && colonIndex < host.length() - 1) {
			String parsedPort = host.substring(colonIndex + 1);
			try {
				port = Integer.parseInt(parsedPort);
				host = host.substring(0, colonIndex);
			} catch (NumberFormatException ignored) {
				port = DEFAULT_PORT;
			}
		}

		if (host.isEmpty()) {
			return null;
		}

		StringBuilder uriBuilder = new StringBuilder();
		uriBuilder.append(PROTOCOL).append("://");
		if (user != null && !user.isEmpty()) {
			uriBuilder.append(Uri.encode(user)).append('@');
		}
		uriBuilder.append(Uri.encode(host));
		if (port != DEFAULT_PORT) {
			uriBuilder.append(':').append(port);
		}
		uriBuilder.append('/');
		if (target != null && !target.isEmpty()) {
			uriBuilder.append(Uri.encode(target));
		}
		uriBuilder.append("/#").append(Uri.encode(rawValue));

		return Uri.parse(uriBuilder.toString());
	}

	@Override
	public HostBean createHost(Uri uri) {
		HostBean host = new HostBean();
		host.setProtocol(PROTOCOL);
		String parsedHostname = uri.getHost();
		host.setHostname(parsedHostname);

		int port = uri.getPort();
		if (port < 0) {
			port = DEFAULT_PORT;
		}
		host.setPort(port);

		String parsedUser = uri.getUserInfo();
		String rawInput = uri.getFragment();
		if ((parsedUser == null || parsedUser.isEmpty())
				&& isLikelyAccessKeyOnlyInput(rawInput, parsedHostname)) {
			// Support pasting only an AWS access key ID in the quick-connect field.
			host.setUsername(parsedHostname);
			host.setHostname(null);
		} else {
			host.setUsername(parsedUser);
		}

		String target = null;
		if (!uri.getPathSegments().isEmpty()) {
			target = uri.getPathSegments().get(0);
		}
		host.setPostLogin(target);

		String nickname = uri.getFragment();
		if (nickname == null || nickname.length() == 0) {
			host.setNickname(getDefaultNickname(host.getUsername(), host.getHostname(), host.getPort()));
		} else {
			host.setNickname(nickname);
		}

		return host;
	}

	@Override
	public void getSelectionArgs(Uri uri, Map<String, String> selection) {
		selection.put(HostDatabase.FIELD_HOST_PROTOCOL, PROTOCOL);
		selection.put(HostDatabase.FIELD_HOST_NICKNAME, uri.getFragment());
		selection.put(HostDatabase.FIELD_HOST_HOSTNAME, uri.getHost());
		selection.put(HostDatabase.FIELD_HOST_PORT,
				Integer.toString(uri.getPort() < 0 ? DEFAULT_PORT : uri.getPort()));
		selection.put(HostDatabase.FIELD_HOST_USERNAME, uri.getUserInfo());
		String target = null;
		if (!uri.getPathSegments().isEmpty()) {
			target = uri.getPathSegments().get(0);
		}
		selection.put(HostDatabase.FIELD_HOST_POSTLOGIN, target);
	}

	public static String getFormatHint(Context context) {
		return String.format("%s@%s/%s",
				context.getString(R.string.aws_access_key_id_hint),
				context.getString(R.string.aws_region_hint),
				context.getString(R.string.ssm_target_hint));
	}

	@Override
	public Map<String, String> getOptions() {
		return new HashMap<String, String>();
	}

	@Override
	public boolean usesNetwork() {
		return true;
	}

	private void trace(String details) {
		if (manager == null) {
			return;
		}
		SecurityKeyDebugLog.logFlow(manager.getApplicationContext(), TAG, FLOW_MARKER, details);
	}

	private String summarizeSessionId(String sessionId) {
		if (sessionId == null || sessionId.isEmpty()) {
			return "unknown";
		}
		if (sessionId.length() <= 12) {
			return sessionId;
		}
		return sessionId.substring(0, 12) + "...";
	}

	private String summarizeError(Exception error) {
		String message = error.getMessage();
		if (message == null || message.trim().isEmpty()) {
			message = error.getClass().getSimpleName();
		}
		message = message.replace('\n', ' ').replace('\r', ' ').trim();
		if (message.length() > 160) {
			message = message.substring(0, 160) + "...";
		}
		Log.w(TAG, "SSM start session failed: " + message);
		return message;
	}

	private void disconnectFromRemote() {
		close();
		dispatchDisconnectOnce();
	}

	private void dispatchDisconnectOnce() {
		if (bridge == null) {
			return;
		}
		if (disconnectDispatched.compareAndSet(false, true)) {
			bridge.dispatchDisconnect(false);
		}
	}

	private String safeTrim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String buildRoleSessionName() {
		long hostId = host == null ? -1 : host.getId();
		String suffix = hostId > 0 ? Long.toString(hostId)
				: Long.toString(System.currentTimeMillis() / 1000L);
		return "termbot-ssm-" + suffix;
	}

	private static boolean isLikelyAccessKeyOnlyInput(String rawInput, String parsedHost) {
		if (rawInput == null || parsedHost == null || parsedHost.isEmpty()) {
			return false;
		}

		String value = rawInput.trim();
		if (value.startsWith(PROTOCOL + "://")) {
			value = value.substring((PROTOCOL + "://").length());
		}

		if (value.isEmpty()
				|| value.contains("@")
				|| value.contains("/")
				|| value.contains(":")) {
			return false;
		}

		if (!value.equals(parsedHost)) {
			return false;
		}

		if (!value.matches("^[A-Za-z0-9]{8,}$")) {
			return false;
		}

		String upper = value.toUpperCase(Locale.US);
		return upper.startsWith("AKI") || upper.startsWith("ASI");
	}
}
