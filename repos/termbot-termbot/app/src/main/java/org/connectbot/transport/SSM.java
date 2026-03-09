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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import org.connectbot.bean.PortForwardBean;
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
	private static final int PORT_FORWARD_BUFFER_SIZE = 8192;
	private static final String DOCUMENT_PORT_FORWARDING = "AWS-StartPortForwardingSession";
	private static final String DOCUMENT_REMOTE_HOST_PORT_FORWARDING =
			"AWS-StartPortForwardingSessionToRemoteHost";
	private static final long PORT_STREAM_HANDSHAKE_TIMEOUT_MILLIS = 5000L;
	private static final String TCP_MULTIPLEXING_SUPPORTED_AFTER_AGENT_VERSION = "3.0.196.0";
	private static final String PORT_SESSION_TYPE_LOCAL_PORT_FORWARDING = "LocalPortForwarding";
	private static final int SSM_FLAG_CONNECT_TO_PORT_ERROR = 3;
	private static final int SMUX_VERSION = 1;
	private static final int SMUX_VERSION_V2 = 2;
	private static final int SMUX_HEADER_SIZE = 8;
	private static final int SMUX_MAX_FRAME_SIZE = 32768;
	private static final int SMUX_STREAM_ID = 3;
	private static final int SMUX_CMD_SYN = 0;
	private static final int SMUX_CMD_FIN = 1;
	private static final int SMUX_CMD_PSH = 2;
	private static final int SMUX_CMD_NOP = 3;
	private static final int SMUX_CMD_UPD = 4;

	private volatile boolean connected;
	private volatile boolean sessionOpen;
	private volatile SsmStreamClient streamClient;
	private final LinkedBlockingQueue<byte[]> inboundPayloadQueue = new LinkedBlockingQueue<>();
	private final AtomicBoolean disconnectDispatched = new AtomicBoolean(false);
	private final Object portForwardLock = new Object();
	private final List<PortForwardBean> portForwards = new ArrayList<>();
	private final Map<PortForwardBean, SsmPortForwardHandle> activePortForwards = new HashMap<>();
	private final List<SsmPortForwardHandle> activeRouteTunnels = new ArrayList<>();
	private byte[] pendingInboundChunk;
	private int pendingInboundOffset;
	private volatile AwsCredentials activeSessionCredentials;
	private volatile String activeRegion;
	private volatile String activeTarget;

	public interface RouteTunnel {
		int getLocalPort();
		void close();
	}

	public static String getProtocolName() {
		return PROTOCOL;
	}

	private SessionContext resolveSessionContext()
			throws IOException, SsmCredentialResolver.MissingSessionTokenException {
		if (activeSessionCredentials != null && activeRegion != null && activeTarget != null) {
			return new SessionContext(
					activeRegion,
					activeTarget,
					activeSessionCredentials,
					null,
					null);
		}

		if (bridge == null || manager == null || host == null) {
			throw new IOException("SSM host context is unavailable");
		}

		String region = safeTrim(host.getHostname());
		String target = safeTrim(host.getPostLogin());
		if (region == null || target == null) {
			throw new IOException(manager.res.getString(R.string.ssm_missing_configuration));
		}

		String accessKeyId = safeTrim(host.getUsername());
		if (accessKeyId == null) {
			trace("credential_resolution_failed reason=missing_access_key");
			throw new IOException(manager.res.getString(
					R.string.ssm_credential_resolution_failed,
					"AWS access key ID is required"));
		}

		String roleArn = safeTrim(host.getSsmRoleArn());
		String mfaSerial = safeTrim(host.getSsmMfaSerial());
		SsmCredentialResolver credentialResolver = createCredentialResolver(region, roleArn, mfaSerial);
		SsmCredentialResolver.Resolution credentialResolution;
		try {
			credentialResolution = credentialResolver.resolve(host, createPromptDelegate());
		} catch (SsmCredentialResolver.MissingSessionTokenException e) {
			trace("credential_resolution_failed reason=session_token_required"
					+ " assume_role_configured=" + (roleArn != null));
			throw e;
		} catch (SsmCredentialResolver.MissingMfaCodeException e) {
			trace("credential_resolution_failed reason=mfa_code_required"
					+ " assume_role_configured=" + (roleArn != null)
					+ " mfa_configured=" + (mfaSerial != null));
			throw e;
		} catch (IOException e) {
			trace("credential_resolution_failed reason=" + summarizeError(e)
					+ " assume_role_configured=" + (roleArn != null));
			throw new IOException(manager.res.getString(
					R.string.ssm_credential_resolution_failed,
					summarizeError(e)), e);
		}

		if (credentialResolution == null) {
			throw new IOException(manager.res.getString(R.string.ssm_secret_key_required));
		}

		trace("credential_resolved mode=" + credentialResolution.getCredentialMode()
				+ " secret_source=" + credentialResolution.getSecretSource()
				+ " session_token_source=" + credentialResolution.getSessionTokenSource()
				+ " enhancement_mode=" + credentialResolution.getEnhancementMode()
				+ " mfa_prompted=" + credentialResolution.isMfaPrompted()
				+ " credential_enhanced=" + credentialResolution.isCredentialEnhanced()
				+ " assume_role_configured=" + (roleArn != null)
				+ " mfa_configured=" + (mfaSerial != null));

		if (!host.getRememberPassword()) {
			credentialResolver.clear(host.getId());
		}

		activeSessionCredentials = credentialResolution.getRuntimeCredentials();
		activeRegion = region;
		activeTarget = target;
		return new SessionContext(
				region,
				target,
				activeSessionCredentials,
				credentialResolution.getPersistedCredentials(),
				credentialResolver);
	}

	private SsmCredentialResolver createCredentialResolver(
			final String region,
			final String roleArn,
			final String mfaSerial
	) {
		return new SsmCredentialResolver(
				SavedPasswordStore.get(manager.getApplicationContext()),
				roleArn == null && mfaSerial == null ? null : new SsmCredentialResolver.SessionCredentialEnhancer() {
					@Override
					public String getEnhancementMode(AwsCredentials baseCredentials) {
						if (roleArn != null) {
							return SsmCredentialResolver.CREDENTIAL_ENHANCEMENT_ASSUME_ROLE;
						}
						if (mfaSerial != null && !baseCredentials.hasSessionToken()) {
							return SsmCredentialResolver.CREDENTIAL_ENHANCEMENT_GET_SESSION_TOKEN;
						}
						return SsmCredentialResolver.CREDENTIAL_ENHANCEMENT_NONE;
					}

					@Override
					public String getMfaPromptHint(AwsCredentials baseCredentials) {
						if (mfaSerial != null) {
							return manager.res.getString(R.string.prompt_aws_mfa_code, mfaSerial);
						}
						return manager.res.getString(R.string.prompt_aws_mfa_code_generic);
					}

					@Override
					public boolean requiresMfaCode(AwsCredentials baseCredentials) {
						return !SsmCredentialResolver.CREDENTIAL_ENHANCEMENT_NONE.equals(
								getEnhancementMode(baseCredentials)) && mfaSerial != null;
					}

					@Override
					public AwsCredentials enhance(AwsCredentials baseCredentials, String mfaCode)
							throws IOException {
						String enhancementMode = getEnhancementMode(baseCredentials);
						if (SsmCredentialResolver.CREDENTIAL_ENHANCEMENT_ASSUME_ROLE.equals(
								enhancementMode)) {
							return StsApiClient.assumeRole(
									region,
									roleArn,
									buildRoleSessionName(),
									baseCredentials,
									mfaSerial,
									mfaCode);
						}
						if (SsmCredentialResolver.CREDENTIAL_ENHANCEMENT_GET_SESSION_TOKEN.equals(
								enhancementMode)) {
							return StsApiClient.getSessionToken(
									region,
									baseCredentials,
									mfaSerial,
									mfaCode);
						}
						return null;
					}
				});
	}

	private SsmCredentialResolver.PromptDelegate createPromptDelegate() {
		return new SsmCredentialResolver.PromptDelegate() {
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
		};
	}

	@Override
	public void connect() {
		connected = false;
		sessionOpen = false;
		pendingInboundChunk = null;
		pendingInboundOffset = 0;
		inboundPayloadQueue.clear();
		disconnectDispatched.set(false);
		clearActiveSessionState();
		disableAllPortForwards();

		if (bridge == null || manager == null || host == null) {
			return;
		}

		SessionContext sessionContext;
		try {
			sessionContext = resolveSessionContext();
			} catch (SsmCredentialResolver.MissingSessionTokenException e) {
				bridge.outputLine(manager.res.getString(R.string.ssm_session_token_required));
				bridge.dispatchDisconnect(false);
				return;
			} catch (SsmCredentialResolver.MissingMfaCodeException e) {
				bridge.outputLine(manager.res.getString(R.string.ssm_mfa_code_required));
				bridge.dispatchDisconnect(false);
				return;
			} catch (IOException e) {
				bridge.outputLine(e.getMessage());
				bridge.dispatchDisconnect(false);
			return;
		}

		trace("start_session_request region=" + sessionContext.region
				+ " target=" + sessionContext.target);
		bridge.outputLine(manager.res.getString(
				R.string.ssm_connecting,
				sessionContext.target,
				sessionContext.region));

		try {
			SsmSessionStartResult result = SsmApiClient.startSession(
					sessionContext.region,
					sessionContext.target,
					sessionContext.credentials);
			persistResolvedCredentials(sessionContext);
			trace("start_session_ok session=" + summarizeSessionId(result.getSessionId())
					+ " stream=url");
			bridge.outputLine(manager.res.getString(
					R.string.ssm_start_session_ok_stub,
					summarizeSessionId(result.getSessionId())));

			SsmStreamClient client = new SsmStreamClient(
					result.getStreamUrl(),
					result.getTokenValue(),
					sessionContext.region,
					sessionContext.credentials,
					new SsmStreamClient.Callback() {
						@Override
						public void onStdout(byte[] payload) {
							if (payload != null && payload.length > 0) {
								inboundPayloadQueue.offer(payload);
							}
						}

						@Override
						public void onStderr(byte[] payload) {
							if (payload != null && payload.length > 0) {
								inboundPayloadQueue.offer(payload);
							}
						}

						@Override
						public void onFlag(byte[] payload) {
							// Shell sessions do not currently consume port-forward control flags.
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
		trace("transport_close connected=" + connected + " session_open=" + sessionOpen);
		disableAllPortForwards();
		disableAllRouteTunnels();
		clearActiveSessionState();
		connected = false;
		sessionOpen = false;
		SsmStreamClient client = streamClient;
		streamClient = null;
		if (client != null) {
			client.close();
		}
	}

	public RouteTunnel openRouteTunnel(String destinationHost, int destinationPort)
			throws IOException {
		SessionContext sessionContext;
			try {
				sessionContext = resolveSessionContext();
			} catch (SsmCredentialResolver.MissingSessionTokenException e) {
				throw new IOException(manager.res.getString(R.string.ssm_session_token_required), e);
			} catch (SsmCredentialResolver.MissingMfaCodeException e) {
				throw new IOException(manager.res.getString(R.string.ssm_mfa_code_required), e);
			}

		final SsmPortForwardConfig config =
				SsmPortForwardConfig.fromDestination(destinationHost, destinationPort);
		final SsmPortForwardHandle handle = new SsmPortForwardHandle(
				null,
				0,
				config,
				sessionContext.region,
				sessionContext.target,
				sessionContext.credentials,
				sessionContext);
		handle.start();
		synchronized (portForwardLock) {
			activeRouteTunnels.add(handle);
		}
		trace("route_tunnel_ready kind=" + config.getKindMarker()
				+ " local_port=" + handle.getLocalPort());
		return new RouteTunnel() {
			@Override
			public int getLocalPort() {
				return handle.getLocalPort();
			}

			@Override
			public void close() {
				synchronized (portForwardLock) {
					activeRouteTunnels.remove(handle);
				}
				handle.close();
			}
		};
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
	public boolean canForwardPorts() {
		return true;
	}

	@Override
	public List<PortForwardBean> getPortForwards() {
		synchronized (portForwardLock) {
			return new ArrayList<>(portForwards);
		}
	}

	@Override
	public boolean addPortForward(PortForwardBean portForward) {
		if (portForward == null) {
			return false;
		}

		synchronized (portForwardLock) {
			if (portForwards.contains(portForward)) {
				return true;
			}
			return portForwards.add(portForward);
		}
	}

	@Override
	public boolean removePortForward(PortForwardBean portForward) {
		if (portForward == null) {
			return false;
		}

		disablePortForward(portForward);

		synchronized (portForwardLock) {
			return portForwards.remove(portForward);
		}
	}

	@Override
	public boolean enablePortForward(PortForwardBean portForward) {
		if (portForward == null) {
			return false;
		}

		if (!HostDatabase.PORTFORWARD_LOCAL.equals(portForward.getType())) {
			trace("tunnel_listener_enable_failed reason=unsupported_type");
			if (bridge != null && manager != null) {
				bridge.outputLine(manager.res.getString(R.string.ssm_port_forward_local_only));
			}
			return false;
		}

		final SsmPortForwardConfig config;
		try {
			config = SsmPortForwardConfig.fromPortForward(portForward);
		} catch (IOException e) {
			trace("tunnel_listener_enable_failed reason=" + summarizeError(e));
			return false;
		}

		final AwsCredentials credentials = activeSessionCredentials;
		final String region = activeRegion;
		final String target = activeTarget;
		if (!connected || credentials == null || region == null || target == null) {
			trace("tunnel_listener_enable_failed reason=missing_runtime_context");
			return false;
		}

		synchronized (portForwardLock) {
			if (!portForwards.contains(portForward)) {
				return false;
			}

			if (portForward.isEnabled()) {
				return true;
			}

			SsmPortForwardHandle handle = new SsmPortForwardHandle(
					portForward,
					portForward.getSourcePort(),
					config,
					region,
					target,
					credentials,
					null);
			try {
				handle.start();
			} catch (IOException e) {
				trace("tunnel_listener_enable_failed kind=" + config.getKindMarker()
						+ " local_port=" + portForward.getSourcePort()
						+ " reason=" + summarizeError(e));
				return false;
			}

			activePortForwards.put(portForward, handle);
			portForward.setIdentifier(handle);
			portForward.setEnabled(true);
			trace("tunnel_listener_enabled kind=" + config.getKindMarker()
					+ " local_port=" + portForward.getSourcePort());
			return true;
		}
	}

	@Override
	public boolean disablePortForward(PortForwardBean portForward) {
		if (portForward == null) {
			return false;
		}

		SsmPortForwardHandle handle;
		synchronized (portForwardLock) {
			if (!portForwards.contains(portForward)) {
				return false;
			}
			handle = activePortForwards.remove(portForward);
			portForward.setEnabled(false);
			portForward.setIdentifier(null);
		}

		if (handle == null) {
			return false;
		}

		handle.close();
		trace("tunnel_listener_disabled local_port=" + portForward.getSourcePort());
		return true;
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

	private void clearActiveSessionState() {
		activeSessionCredentials = null;
		activeRegion = null;
		activeTarget = null;
	}

	private void disableAllPortForwards() {
		List<SsmPortForwardHandle> handles;
		synchronized (portForwardLock) {
			handles = new ArrayList<>(activePortForwards.values());
			activePortForwards.clear();
			for (PortForwardBean portForward : portForwards) {
				portForward.setEnabled(false);
				portForward.setIdentifier(null);
			}
		}

		for (SsmPortForwardHandle handle : handles) {
			handle.close();
		}
	}

	private void disableAllRouteTunnels() {
		List<SsmPortForwardHandle> handles;
		synchronized (portForwardLock) {
			handles = new ArrayList<>(activeRouteTunnels);
			activeRouteTunnels.clear();
		}
		for (SsmPortForwardHandle handle : handles) {
			handle.close();
		}
	}

	private void persistResolvedCredentials(SessionContext sessionContext) throws IOException {
		if (sessionContext == null || sessionContext.credentialResolver == null) {
			return;
		}
		sessionContext.credentialResolver.persistIfEnabled(host, sessionContext.persistedCredentials);
	}

	private void onTunnelConnectionFailed(
			SsmPortForwardConfig config,
			int localPort,
			IOException error
	) {
		String reason = summarizeError(error);
		trace("tunnel_connection_failed kind=" + config.getKindMarker()
				+ " local_port=" + localPort
				+ " reason=" + reason);
		if (bridge != null && manager != null) {
			bridge.outputLine(manager.res.getString(R.string.ssm_port_forward_failed, reason));
		}
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
		Log.w(TAG, "SSM operation failed: " + message);
		return message;
	}

	private void disconnectFromRemote() {
		trace("disconnect_from_remote");
		close();
		dispatchDisconnectOnce();
	}

	private void dispatchDisconnectOnce() {
		if (bridge == null) {
			return;
		}
		if (disconnectDispatched.compareAndSet(false, true)) {
			trace("dispatch_disconnect");
			bridge.dispatchDisconnect(false);
		} else {
			trace("dispatch_disconnect_skipped already_dispatched=true");
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

	private static boolean agentSupportsTcpMultiplexing(String agentVersion) {
		return compareAgentVersions(agentVersion,
				TCP_MULTIPLEXING_SUPPORTED_AFTER_AGENT_VERSION) > 0;
	}

	private static int compareAgentVersions(String left, String right) {
		if (left == null || right == null) {
			return Integer.MIN_VALUE;
		}

		String[] leftParts = left.trim().split("\\.");
		String[] rightParts = right.trim().split("\\.");
		int count = Math.max(leftParts.length, rightParts.length);
		for (int i = 0; i < count; i++) {
			int leftValue = i < leftParts.length ? parseVersionPart(leftParts[i]) : 0;
			int rightValue = i < rightParts.length ? parseVersionPart(rightParts[i]) : 0;
			if (leftValue != rightValue) {
				return leftValue < rightValue ? -1 : 1;
			}
		}
		return 0;
	}

	private static int parseVersionPart(String value) {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return Integer.MIN_VALUE;
		}
	}

	private static int parseBigEndianFlag(byte[] payload) {
		if (payload == null || payload.length < 4) {
			return -1;
		}
		return ByteBuffer.wrap(payload, 0, 4).order(ByteOrder.BIG_ENDIAN).getInt();
	}

	private static int parseLittleEndianUnsignedShort(byte[] payload, int offset) {
		return (payload[offset] & 0xff) | ((payload[offset + 1] & 0xff) << 8);
	}

	private static int parseLittleEndianInt(byte[] payload, int offset) {
		return (payload[offset] & 0xff)
				| ((payload[offset + 1] & 0xff) << 8)
				| ((payload[offset + 2] & 0xff) << 16)
				| ((payload[offset + 3] & 0xff) << 24);
	}

	private static String summarizeAgentVersion(String agentVersion) {
		return agentVersion == null ? "unknown" : agentVersion;
	}

	private static boolean isPrintableAscii(byte value) {
		int unsigned = value & 0xff;
		return unsigned == 9 || unsigned == 10 || unsigned == 13
				|| (unsigned >= 32 && unsigned <= 126);
	}

	private static final class SessionContext {
		private final String region;
		private final String target;
		private final AwsCredentials credentials;
		private final AwsCredentials persistedCredentials;
		private final SsmCredentialResolver credentialResolver;

		private SessionContext(
				String region,
				String target,
				AwsCredentials credentials,
				AwsCredentials persistedCredentials,
				SsmCredentialResolver credentialResolver
		) {
			this.region = region;
			this.target = target;
			this.credentials = credentials;
			this.persistedCredentials = persistedCredentials;
			this.credentialResolver = credentialResolver;
		}
	}

	private static final class SsmPortForwardConfig {
		private final boolean managedNodeLocalhost;
		private final String destinationHost;
		private final int destinationPort;
		private final String kindMarker;

		private SsmPortForwardConfig(
				boolean managedNodeLocalhost,
				String destinationHost,
				int destinationPort,
				String kindMarker
		) {
			this.managedNodeLocalhost = managedNodeLocalhost;
			this.destinationHost = destinationHost;
			this.destinationPort = destinationPort;
			this.kindMarker = kindMarker;
		}

		public String getDocumentName() {
			return managedNodeLocalhost
					? DOCUMENT_PORT_FORWARDING
					: DOCUMENT_REMOTE_HOST_PORT_FORWARDING;
		}

		public Map<String, String> buildParameters(int localPort) {
			Map<String, String> parameters = new HashMap<>();
			parameters.put("portNumber", Integer.toString(destinationPort));
			parameters.put("localPortNumber", Integer.toString(localPort));
			if (!managedNodeLocalhost) {
				parameters.put("host", destinationHost);
			}
			return parameters;
		}

		public String getKindMarker() {
			return kindMarker;
		}

		public static SsmPortForwardConfig fromPortForward(PortForwardBean portForward)
				throws IOException {
			if (portForward.getSourcePort() <= 0) {
				throw new IOException("Local tunnel port must be greater than zero");
			}
			return fromDestination(portForward.getDestAddr(), portForward.getDestPort());
		}

		public static SsmPortForwardConfig fromDestination(
				String destinationHost,
				int destinationPort
		) throws IOException {
			if (destinationPort <= 0) {
				throw new IOException("Tunnel destination port must be greater than zero");
			}

			String normalizedDestinationHost = safeStaticTrim(destinationHost);
			if (normalizedDestinationHost == null) {
				throw new IOException("Tunnel destination host is required");
			}

			if (isManagedNodeLocalhost(normalizedDestinationHost)) {
				return new SsmPortForwardConfig(
						true,
						normalizedDestinationHost,
						destinationPort,
						"managed_node");
			}

			return new SsmPortForwardConfig(
					false,
					normalizedDestinationHost,
					destinationPort,
					"remote_host");
		}

		private static boolean isManagedNodeLocalhost(String destinationHost) {
			String normalized = destinationHost.trim().toLowerCase(Locale.US);
			return "localhost".equals(normalized) || "127.0.0.1".equals(normalized);
		}

		private static String safeStaticTrim(String value) {
			if (value == null) {
				return null;
			}
			String trimmed = value.trim();
			return trimmed.isEmpty() ? null : trimmed;
		}
	}

	private final class SsmPortForwardHandle {
		private final PortForwardBean portForward;
		private final int requestedLocalPort;
		private final SsmPortForwardConfig config;
		private final String region;
		private final String target;
		private final AwsCredentials credentials;
		private final SessionContext resolvedSessionContext;
		private final AtomicBoolean closed = new AtomicBoolean(false);
		private final List<SsmPortForwardConnection> activeConnections = new ArrayList<>();

		private ServerSocket serverSocket;
		private Thread acceptThread;
		private volatile int localPort = -1;

		private SsmPortForwardHandle(
				PortForwardBean portForward,
				int requestedLocalPort,
				SsmPortForwardConfig config,
				String region,
				String target,
				AwsCredentials credentials,
				SessionContext resolvedSessionContext
		) {
			this.portForward = portForward;
			this.requestedLocalPort = requestedLocalPort;
			this.config = config;
			this.region = region;
			this.target = target;
			this.credentials = credentials;
			this.resolvedSessionContext = resolvedSessionContext;
		}

		public void start() throws IOException {
			serverSocket = new ServerSocket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(
					InetAddress.getByName("127.0.0.1"),
					requestedLocalPort));
			localPort = serverSocket.getLocalPort();

			acceptThread = new Thread(new Runnable() {
				@Override
				public void run() {
					runAcceptLoop();
				}
			}, "SsmPortForwardAccept-" + getLocalPort());
			acceptThread.setDaemon(true);
			acceptThread.start();
		}

		public int getLocalPort() {
			return localPort;
		}

		public void close() {
			if (!closed.compareAndSet(false, true)) {
				return;
			}

			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException ignored) {
					// Best effort close.
				}
			}

			List<SsmPortForwardConnection> connections;
			synchronized (activeConnections) {
				connections = new ArrayList<>(activeConnections);
				activeConnections.clear();
			}
			for (SsmPortForwardConnection connection : connections) {
				connection.close();
			}
		}

		private void runAcceptLoop() {
			while (!closed.get()) {
				final Socket localSocket;
				try {
					localSocket = serverSocket.accept();
				} catch (IOException e) {
					if (!closed.get()) {
						onTunnelConnectionFailed(config, getLocalPort(), e);
					}
					return;
				}

				SsmPortForwardConnection connection =
						new SsmPortForwardConnection(localSocket, getLocalPort());
				synchronized (activeConnections) {
					if (closed.get()) {
						connection.close();
						return;
					}
					activeConnections.add(connection);
				}
				connection.start();
			}
		}

		private void onConnectionClosed(SsmPortForwardConnection connection) {
			synchronized (activeConnections) {
				activeConnections.remove(connection);
			}
		}

		private final class SsmPortForwardConnection {
			private final Socket localSocket;
			private final int localPort;
			private final AtomicBoolean connectionClosed = new AtomicBoolean(false);
			private final Object outputLock = new Object();

			private SsmStreamClient portStreamClient;
			private OutputStream outputStream;
			private PortTunnelProtocolBridge portProtocolBridge;

			private SsmPortForwardConnection(Socket localSocket, int localPort) {
				this.localSocket = localSocket;
				this.localPort = localPort;
			}

			public void start() {
				Thread connectionThread = new Thread(new Runnable() {
					@Override
					public void run() {
						runPortSession();
					}
				}, "SsmPortForward-" + localPort);
				connectionThread.setDaemon(true);
				connectionThread.start();
			}

			public void close() {
				if (!connectionClosed.compareAndSet(false, true)) {
					return;
				}

				if (portStreamClient != null) {
					portStreamClient.close();
				}
				if (localSocket != null) {
					try {
						localSocket.close();
					} catch (IOException ignored) {
						// Best effort close.
					}
				}
				onConnectionClosed(this);
			}

			private void runPortSession() {
				try {
					outputStream = localSocket.getOutputStream();
					SsmSessionStartResult result = SsmApiClient.startSession(
							region,
							target,
							config.getDocumentName(),
							config.buildParameters(localPort),
							credentials);
					persistResolvedCredentials(resolvedSessionContext);
					trace("tunnel_connection_started kind=" + config.getKindMarker()
							+ " local_port=" + localPort
							+ " session=" + summarizeSessionId(result.getSessionId()));

					portStreamClient = new SsmStreamClient(
							result.getStreamUrl(),
							result.getTokenValue(),
							region,
							credentials,
							new SsmStreamClient.Callback() {
								@Override
								public void onStdout(byte[] payload) {
									if (payload == null || payload.length == 0) {
										return;
									}
									try {
										if (portProtocolBridge != null) {
											portProtocolBridge.onRemoteOutput(payload);
										}
									} catch (IOException e) {
										onError(e);
									}
								}

								@Override
								public void onStderr(byte[] payload) {
									trace("tunnel_connection_stderr kind=" + config.getKindMarker()
											+ " local_port=" + localPort);
								}

								@Override
								public void onFlag(byte[] payload) {
									if (payload == null || payload.length == 0) {
										return;
									}
									try {
										if (portProtocolBridge != null) {
											portProtocolBridge.onRemoteFlag(payload);
										}
									} catch (IOException e) {
										onError(e);
									}
								}

								@Override
								public void onInfo(String message) {
									trace("tunnel_connection_info kind=" + config.getKindMarker()
											+ " local_port=" + localPort);
								}

								@Override
								public void onError(IOException error) {
									if (connectionClosed.get()) {
										return;
									}
									onTunnelConnectionFailed(config, localPort, error);
									close();
								}

								@Override
								public void onClosed(String reason) {
									trace("tunnel_connection_closed kind=" + config.getKindMarker()
											+ " local_port=" + localPort);
									close();
								}
							});
					portStreamClient.connect();
					awaitTunnelHandshake();
					portProtocolBridge = createProtocolBridge(
							portStreamClient.getAgentVersion(),
							portStreamClient.getSessionPropertiesType());
					trace("tunnel_protocol_selected kind=" + config.getKindMarker()
							+ " local_port=" + localPort
							+ " protocol=" + portProtocolBridge.getProtocolMarker()
							+ " agent_version=" + summarizeAgentVersion(
									portStreamClient.getAgentVersion())
							+ " session_type=" + summarizeAgentVersion(
									portStreamClient.getSessionType())
							+ " port_session_type=" + summarizeAgentVersion(
									portStreamClient.getSessionPropertiesType()));
					portProtocolBridge.start();
					pumpLocalSocket();
				} catch (IOException e) {
					if (!connectionClosed.get()) {
						onTunnelConnectionFailed(config, localPort, e);
					}
				} finally {
					close();
				}
			}

			private void awaitTunnelHandshake() throws IOException {
				try {
					if (!portStreamClient.awaitHandshakeComplete(
							PORT_STREAM_HANDSHAKE_TIMEOUT_MILLIS,
							TimeUnit.MILLISECONDS)) {
						trace("tunnel_handshake_timeout kind=" + config.getKindMarker()
								+ " local_port=" + localPort);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted while waiting for SSM tunnel handshake", e);
				}
			}

				private PortTunnelProtocolBridge createProtocolBridge(
						String agentVersion,
						String sessionPropertiesType
				) {
					if (PORT_SESSION_TYPE_LOCAL_PORT_FORWARDING.equals(sessionPropertiesType)
							&& agentSupportsTcpMultiplexing(agentVersion)) {
						return new SmuxPortTunnelBridge();
					}
					return new RawPortTunnelBridge();
				}

			private void pumpLocalSocket() throws IOException {
				byte[] buffer = new byte[PORT_FORWARD_BUFFER_SIZE];
				try (InputStream inputStream = localSocket.getInputStream()) {
					int bytesRead;
					while (!connectionClosed.get()
							&& (bytesRead = inputStream.read(buffer)) >= 0) {
						if (bytesRead == 0) {
							continue;
						}
						if (portStreamClient == null || !portStreamClient.isOpen()) {
							throw new IOException("SSM tunnel stream is not open");
						}

						byte[] payload = new byte[bytesRead];
						System.arraycopy(buffer, 0, payload, 0, bytesRead);
						portProtocolBridge.sendLocalInput(payload);
					}
				} finally {
					if (portProtocolBridge != null) {
						portProtocolBridge.onLocalEof();
					}
				}
			}

			private void writeToLocalSocket(byte[] payload) throws IOException {
				synchronized (outputLock) {
					if (outputStream == null) {
						throw new IOException("Local tunnel socket output is not available");
					}
					outputStream.write(payload);
					outputStream.flush();
				}
			}

			private abstract class PortTunnelProtocolBridge {
				public abstract String getProtocolMarker();

				public abstract void start() throws IOException;

				public abstract void onRemoteOutput(byte[] payload) throws IOException;

				public abstract void onRemoteFlag(byte[] payload) throws IOException;

				public abstract void sendLocalInput(byte[] payload) throws IOException;

				public abstract void onLocalEof() throws IOException;
			}

			private final class RawPortTunnelBridge extends PortTunnelProtocolBridge {
				@Override
				public String getProtocolMarker() {
					return "raw";
				}

				@Override
				public void start() {
					// No protocol bootstrap required for old-agent raw forwarding.
				}

				@Override
				public void onRemoteOutput(byte[] payload) throws IOException {
					writeToLocalSocket(payload);
				}

				@Override
				public void onRemoteFlag(byte[] payload) throws IOException {
					handleFlagPayload(payload);
				}

				@Override
				public void sendLocalInput(byte[] payload) throws IOException {
					portStreamClient.sendPortInput(payload);
				}

				@Override
				public void onLocalEof() {
					// Best-effort only; raw port mode uses connection close to end the session.
				}
			}

			private final class SmuxPortTunnelBridge extends PortTunnelProtocolBridge {
				private byte[] pendingRemoteFrames = new byte[0];
				private boolean awaitingFirstFrame = true;
				private boolean streamOpened;
				private boolean finSent;

				@Override
				public String getProtocolMarker() {
					return "smux_v1";
				}

				@Override
				public void start() throws IOException {
					ensureStreamOpen();
				}

				@Override
				public void onRemoteOutput(byte[] payload) throws IOException {
					appendRemoteFrames(payload);
					drainRemoteFrames();
				}

				@Override
				public void onRemoteFlag(byte[] payload) throws IOException {
					handleFlagPayload(payload);
				}

				@Override
				public void sendLocalInput(byte[] payload) throws IOException {
					ensureStreamOpen();
					for (int offset = 0; offset < payload.length; offset += SMUX_MAX_FRAME_SIZE) {
						int chunkSize = Math.min(SMUX_MAX_FRAME_SIZE, payload.length - offset);
						byte[] chunk = Arrays.copyOfRange(payload, offset, offset + chunkSize);
						sendFrame(SMUX_CMD_PSH, chunk);
					}
				}

				@Override
				public void onLocalEof() throws IOException {
					if (finSent) {
						return;
					}
					finSent = true;
					sendFrame(SMUX_CMD_FIN, new byte[0]);
				}

				private void ensureStreamOpen() throws IOException {
					if (streamOpened) {
						return;
					}
					streamOpened = true;
					sendFrame(SMUX_CMD_SYN, new byte[0]);
				}

				private void appendRemoteFrames(byte[] payload) {
					byte[] merged = new byte[pendingRemoteFrames.length + payload.length];
					System.arraycopy(pendingRemoteFrames, 0, merged, 0, pendingRemoteFrames.length);
					System.arraycopy(payload, 0, merged, pendingRemoteFrames.length, payload.length);
					pendingRemoteFrames = merged;
				}

				private void drainRemoteFrames() throws IOException {
					int offset = 0;
					while (pendingRemoteFrames.length - offset >= SMUX_HEADER_SIZE) {
						int version = pendingRemoteFrames[offset] & 0xff;
						if (awaitingFirstFrame && version != SMUX_VERSION && version != SMUX_VERSION_V2) {
							int skipped = skipPrintablePreamble(offset);
							if (skipped < 0) {
								break;
							}
							offset += skipped;
							if (pendingRemoteFrames.length - offset < SMUX_HEADER_SIZE) {
								break;
							}
							version = pendingRemoteFrames[offset] & 0xff;
						}
						int command = pendingRemoteFrames[offset + 1] & 0xff;
						int length = parseLittleEndianUnsignedShort(pendingRemoteFrames, offset + 2);
						int streamId = parseLittleEndianInt(pendingRemoteFrames, offset + 4);

						if (version != SMUX_VERSION && version != SMUX_VERSION_V2) {
							throw new IOException("Unexpected SSM smux version " + version);
						}
						if (!isSupportedSmuxCommand(command)) {
							throw new IOException("Unexpected SSM smux command " + command);
						}

						int frameLength = SMUX_HEADER_SIZE + length;
						if (pendingRemoteFrames.length - offset < frameLength) {
							break;
						}

						byte[] framePayload = Arrays.copyOfRange(
								pendingRemoteFrames,
								offset + SMUX_HEADER_SIZE,
								offset + frameLength);
						offset += frameLength;

						if (streamId != SMUX_STREAM_ID) {
							continue;
						}
						awaitingFirstFrame = false;

						switch (command) {
							case SMUX_CMD_SYN:
							case SMUX_CMD_NOP:
							case SMUX_CMD_UPD:
								break;
							case SMUX_CMD_PSH:
								if (framePayload.length > 0) {
									writeToLocalSocket(framePayload);
								}
								break;
							case SMUX_CMD_FIN:
								close();
								return;
						}
					}

					if (offset > 0) {
						pendingRemoteFrames = Arrays.copyOfRange(
								pendingRemoteFrames,
								offset,
								pendingRemoteFrames.length);
					}
				}

				private void sendFrame(int command, byte[] framePayload) throws IOException {
					ByteBuffer frame = ByteBuffer
							.allocate(SMUX_HEADER_SIZE + framePayload.length)
							.order(ByteOrder.LITTLE_ENDIAN);
					frame.put((byte) SMUX_VERSION);
					frame.put((byte) command);
					frame.putShort((short) framePayload.length);
					frame.putInt(SMUX_STREAM_ID);
					frame.put(framePayload);
					portStreamClient.sendPortInput(frame.array());
				}

				private boolean isSupportedSmuxCommand(int command) {
					return command == SMUX_CMD_SYN
							|| command == SMUX_CMD_FIN
							|| command == SMUX_CMD_PSH
							|| command == SMUX_CMD_NOP
							|| command == SMUX_CMD_UPD;
				}

				private int skipPrintablePreamble(int offset) throws IOException {
					int newlineIndex = -1;
					for (int i = offset; i < pendingRemoteFrames.length; i++) {
						byte value = pendingRemoteFrames[i];
						if (!isPrintableAscii(value)) {
							throw new IOException("Unexpected SSM smux version " + (value & 0xff));
						}
						if (value == '\n') {
							newlineIndex = i;
							break;
						}
					}

					if (newlineIndex < 0) {
						if (pendingRemoteFrames.length - offset > 256) {
							throw new IOException("Unexpected SSM smux version "
									+ (pendingRemoteFrames[offset] & 0xff));
						}
						return -1;
					}

					trace("tunnel_protocol_preamble_skipped kind=" + config.getKindMarker()
							+ " local_port=" + localPort
							+ " bytes=" + (newlineIndex - offset + 1));
					return newlineIndex - offset + 1;
				}
			}

			private void handleFlagPayload(byte[] payload) throws IOException {
				int flag = parseBigEndianFlag(payload);
				if (flag == SSM_FLAG_CONNECT_TO_PORT_ERROR) {
					throw new IOException("Connection to destination port failed");
				}
			}
		}
	}
}
