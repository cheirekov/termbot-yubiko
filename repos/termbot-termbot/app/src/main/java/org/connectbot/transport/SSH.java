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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.trilead.ssh2.crypto.keys.Ed25519PrivateKey;
import com.trilead.ssh2.crypto.keys.Ed25519PublicKey;
import com.trilead.ssh2.crypto.keys.Ed25519Provider;

import org.connectbot.SecurityKeySignatureProxy;
import org.connectbot.R;
import org.connectbot.bean.HostBean;
import org.connectbot.bean.PortForwardBean;
import org.connectbot.bean.PubkeyBean;
import org.connectbot.securitykey.SecurityKeyProviderProfile;
import org.connectbot.securitykey.SshSkPublicKey;
import org.connectbot.service.TerminalBridge;
import org.connectbot.service.TerminalManager;
import org.connectbot.service.TerminalManager.KeyHolder;
import org.connectbot.util.HostDatabase;
import org.connectbot.util.PubkeyDatabase;
import org.connectbot.util.PubkeyUtils;
import org.connectbot.util.SavedPasswordStore;
import org.connectbot.util.SecurityKeyDebugLog;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.Nullable;

import com.trilead.ssh2.AuthAgentCallback;
import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ConnectionInfo;
import com.trilead.ssh2.ConnectionMonitor;
import com.trilead.ssh2.DynamicPortForwarder;
import com.trilead.ssh2.ExtensionInfo;
import com.trilead.ssh2.ExtendedServerHostKeyVerifier;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.LocalPortForwarder;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.auth.SignatureProxy;
import com.trilead.ssh2.crypto.Base64;
import com.trilead.ssh2.crypto.PEMDecoder;
import com.trilead.ssh2.packets.PacketUserauthRequestPublicKey;
import com.trilead.ssh2.packets.Packets;
import com.trilead.ssh2.packets.TypesReader;
import com.trilead.ssh2.packets.TypesWriter;
import com.trilead.ssh2.signature.DSASHA1Verify;
import com.trilead.ssh2.signature.ECDSASHA2Verify;
import com.trilead.ssh2.signature.Ed25519Verify;
import com.trilead.ssh2.signature.RSASHA1Verify;
import com.trilead.ssh2.transport.TransportManager;

/**
 * @author Kenny Root
 *
 */
public class SSH extends AbsTransport implements ConnectionMonitor, InteractiveCallback, AuthAgentCallback {
	static {
		// Since this class deals with Ed25519 keys, we need to make sure this is available.
		Ed25519Provider.insertIfNeeded();
	}

	public SSH() {
		super();
	}

	/**
	 * @param host
	 * @param bridge
	 * @param manager
	 */
	public SSH(HostBean host, TerminalBridge bridge, TerminalManager manager) {
		super(host, bridge, manager);
	}

	private static final String PROTOCOL = "ssh";
	private static final String TAG = "CB.SSH";
	private static final String MARKER_SECURITY_KEY_AUTH_FAIL_FAST = "SK_AUTH_FAIL_FAST";
	private static final String MARKER_AUTH_KEY_OFFER = "SSH_AUTH_KEY_OFFER";
	private static final String MARKER_AUTH_KEY_RESULT = "SSH_AUTH_KEY_RESULT";
	private static final String MARKER_AUTH_PK_STATUS = "SSH_AUTH_PK_STATUS";
	private static final String MARKER_AUTH_END_REASON = "SSH_AUTH_END_REASON";
	private static final String MARKER_SIGN_REQUESTED = "SIGN_REQUESTED";
	private static final String MARKER_SIGN_NOT_CALLED = "SIGN_NOT_CALLED";
	private static final String MARKER_OFFERED_KEY_OPENSSH = "OFFERED_KEY_OPENSSH";
	private static final String MARKER_OFFERED_KEY_FP = "OFFERED_KEY_FP";
	private static final String MARKER_SECURITY_KEY_PROVIDER_USED = "SECURITY_KEY_PROVIDER_USED";
	private static final String MARKER_OPENPGP_SLOT_USED = "OPENPGP_SLOT_USED";
	private static final String MARKER_PIV_SLOT_USED = "PIV_SLOT_USED";
	private static final String MARKER_SERVER_DISCONNECT_DETAIL = "SSH_SERVER_DISCONNECT_DETAIL";
	private static final String MARKER_SESSION_OPEN_ATTEMPT = "SSH_SESSION_OPEN_ATTEMPT";
	private static final String MARKER_SESSION_OPEN_DEFERRED = "SSH_SESSION_OPEN_DEFERRED";
	private static final String MARKER_SESSION_OPEN_SUCCESS = "SSH_SESSION_OPEN_SUCCESS";
	private static final String MARKER_SESSION_OPEN_FAILED = "SSH_SESSION_OPEN_FAILED";
	private static final String MARKER_PASSWORD_SOURCE = "PASSWORD_SOURCE";
	private static final String MARKER_JUMP_HOST_USED = "JUMP_HOST_USED";
	private static final String MARKER_JUMP_STAGE = "JUMP_STAGE";
	private static final String MARKER_AUTH_CONTEXT = "SSH_AUTH_CONTEXT";
	private static final int SSH_MSG_USERAUTH_PK_OK = 60;
	private static final int DEFAULT_PORT = 22;

	private static final String AUTH_PUBLICKEY = "publickey",
		AUTH_PASSWORD = "password",
		AUTH_KEYBOARDINTERACTIVE = "keyboard-interactive";

	private final static int AUTH_TRIES = 20;

	private static final Pattern hostmask = Pattern.compile(
			"^(.+)@(([0-9a-z.-]+)|(\\[[a-f:0-9]+\\]))(:(\\d+))?$", Pattern.CASE_INSENSITIVE);

	private boolean compression = false;
	private volatile boolean authenticated = false;
	private volatile boolean connected = false;
	private volatile boolean sessionOpen = false;

	private boolean pubkeysExhausted = false;
	private boolean interactiveCanContinue = true;

	private Connection connection;
	private Connection jumpConnection;
	private Session session;
	private LocalPortForwarder jumpLocalPortForwarder;
	private String hostVerifierOverride;
	private int portVerifierOverride = -1;

	private OutputStream stdin;
	private InputStream stdout;
	private InputStream stderr;

	private static final int conditions = ChannelCondition.STDOUT_DATA
		| ChannelCondition.STDERR_DATA
		| ChannelCondition.CLOSED
		| ChannelCondition.EOF;

	private List<PortForwardBean> portForwards = new ArrayList<>();

	private int columns;
	private int rows;

	private int width;
	private int height;

	private String useAuthAgent = HostDatabase.AUTHAGENT_NO;
	private String agentLockPassphrase;

	public class HostKeyVerifier extends ExtendedServerHostKeyVerifier {
		@Override
		public boolean verifyServerHostKey(String hostname, int port,
				String serverHostKeyAlgorithm, byte[] serverHostKey) throws IOException {
			String verificationHost = getHostForVerification(hostname);
			int verificationPort = getPortForVerification(port);

			// read in all known hosts from hostdb
			KnownHosts hosts = manager.hostdb.getKnownHosts();
			Boolean result;

			String matchName = String.format(Locale.US, "%s:%d", verificationHost, verificationPort);

			String fingerprint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey);

			String algorithmName;
			if ("ssh-rsa".equals(serverHostKeyAlgorithm))
				algorithmName = "RSA";
			else if ("ssh-dss".equals(serverHostKeyAlgorithm))
				algorithmName = "DSA";
			else if (serverHostKeyAlgorithm.startsWith("ecdsa-"))
				algorithmName = "EC";
			else if ("ssh-ed25519".equals(serverHostKeyAlgorithm))
				algorithmName = "Ed25519";
			else
				algorithmName = serverHostKeyAlgorithm;

			switch (hosts.verifyHostkey(matchName, serverHostKeyAlgorithm, serverHostKey)) {
			case KnownHosts.HOSTKEY_IS_OK:
				bridge.outputLine(manager.res.getString(R.string.terminal_sucess, algorithmName, fingerprint));
				return true;

			case KnownHosts.HOSTKEY_IS_NEW:
				// prompt user
				bridge.outputLine(manager.res.getString(R.string.host_authenticity_warning, verificationHost));
				bridge.outputLine(manager.res.getString(R.string.host_fingerprint, algorithmName, fingerprint));

				result = bridge.promptHelper.requestBooleanPrompt(null, manager.res.getString(R.string.prompt_continue_connecting));
				if (result == null) {
					return false;
				}
				if (result) {
					// save this key in known database
					manager.hostdb.saveKnownHost(verificationHost, verificationPort, serverHostKeyAlgorithm, serverHostKey);
				}
				return result;

			case KnownHosts.HOSTKEY_HAS_CHANGED:
				String header = String.format("@   %s   @",
						manager.res.getString(R.string.host_verification_failure_warning_header));

				char[] atsigns = new char[header.length()];
				Arrays.fill(atsigns, '@');
				String border = new String(atsigns);

				bridge.outputLine(border);
				bridge.outputLine(header);
				bridge.outputLine(border);

				bridge.outputLine(manager.res.getString(R.string.host_verification_failure_warning));

				bridge.outputLine(String.format(manager.res.getString(R.string.host_fingerprint),
						algorithmName, fingerprint));

				// Users have no way to delete keys, so we'll prompt them for now.
				result = bridge.promptHelper.requestBooleanPrompt(null, manager.res.getString(R.string.prompt_continue_connecting));
				if (result != null && result) {
					// save this key in known database
					manager.hostdb.saveKnownHost(verificationHost, verificationPort, serverHostKeyAlgorithm, serverHostKey);
					return true;
				} else {
					return false;
				}

			default:
				bridge.outputLine(manager.res.getString(R.string.terminal_failed));
				return false;
			}
		}

		@Override
		public List<String> getKnownKeyAlgorithmsForHost(String host, int port) {
			return manager.hostdb.getHostKeyAlgorithmsForHost(getHostForVerification(host), getPortForVerification(port));
		}

		@Override
		public void removeServerHostKey(String host, int port, String algorithm, byte[] hostKey) {
			manager.hostdb.removeKnownHost(getHostForVerification(host), getPortForVerification(port), algorithm, hostKey);
		}

		@Override
		public void addServerHostKey(String host, int port, String algorithm, byte[] hostKey) {
			manager.hostdb.saveKnownHost(getHostForVerification(host), getPortForVerification(port), algorithm, hostKey);
		}
	}

	private void authenticate() {
		authenticate(true);
	}

	private void authenticate(boolean openSessionAfterAuth) {
		try {
			if (connection.authenticateWithNone(host.getUsername())) {
				traceAuthEnd("SUCCESS_NONE", null);
				authenticated = true;
				if (openSessionAfterAuth) {
					finishConnection();
				}
				return;
			}
		} catch (Exception e) {
			Log.d(TAG, "Host does not support 'none' authentication.");
		}

		bridge.outputLine(manager.res.getString(R.string.terminal_auth));

		try {
			long pubkeyId = host.getPubkeyId();
			boolean publickeyAvailable = connection.isAuthMethodAvailable(host.getUsername(), AUTH_PUBLICKEY);
			boolean keyboardInteractiveAvailable = connection.isAuthMethodAvailable(host.getUsername(), AUTH_KEYBOARDINTERACTIVE);
			boolean passwordAvailable = connection.isAuthMethodAvailable(host.getUsername(), AUTH_PASSWORD);
			traceFlow(
					MARKER_AUTH_CONTEXT,
					"host=" + String.valueOf(host.getNickname())
							+ " user=" + String.valueOf(host.getUsername())
							+ " pubkeyId=" + pubkeyId
							+ " methods=publickey:" + publickeyAvailable
							+ ",keyboard-interactive:" + keyboardInteractiveAvailable
							+ ",password:" + passwordAvailable);

				boolean forcePublicKeyAttemptForJumpHost = host.getJumpHostId() > 0;
				if (!pubkeysExhausted &&
						pubkeyId != HostDatabase.PUBKEYID_NEVER &&
						(publickeyAvailable
								|| pubkeyId > HostDatabase.PUBKEYID_ANY
								|| forcePublicKeyAttemptForJumpHost)) {

				// if explicit pubkey defined for this host, then prompt for password as needed
				// otherwise just try all in-memory keys held in terminalmanager

				if (pubkeyId == HostDatabase.PUBKEYID_ANY) {
					// try each of the in-memory keys
					bridge.outputLine(manager.res
							.getString(R.string.terminal_auth_pubkey_any));
					boolean attemptedAnyKey = false;
					for (Entry<String, KeyHolder> entry : manager.loadedKeypairs.entrySet()) {
						attemptedAnyKey = true;
						if (entry.getValue().bean.isConfirmUse()
								&& !promptForPubkeyUse(entry.getKey()))
							continue;

						if (this.tryPublicKey(host.getUsername(), entry.getKey(),
								entry.getValue().pair)) {
							authenticated = true;
							if (openSessionAfterAuth) {
								finishConnection();
							}
							break;
						}
					}

					if (!authenticated) {
						for (PubkeyBean candidatePubkey : manager.pubkeydb.allPubkeys()) {
							if (candidatePubkey == null || !candidatePubkey.isSecurityKey()) {
								continue;
							}
							attemptedAnyKey = true;
							if (candidatePubkey.isConfirmUse()
									&& !promptForPubkeyUse(candidatePubkey.getNickname())) {
								continue;
							}

							if (this.trySecurityKey(candidatePubkey)) {
								authenticated = true;
								if (openSessionAfterAuth) {
									finishConnection();
								}
								break;
							}
						}
					}

					if (!attemptedAnyKey) {
						traceAuthEnd("PUBKEY_ANY_NO_KEYS_AVAILABLE", null);
					}
				} else {
					// use a specific key for this host, as requested
					PubkeyBean pubkey = manager.pubkeydb.findPubkeyById(pubkeyId);

					if (pubkey == null) {
						bridge.outputLine(manager.res.getString(R.string.terminal_auth_pubkey_invalid));
						traceAuthEnd("PUBKEY_INVALID", null);
					} else if (pubkey.isSecurityKey()) {
						bridge.outputLine(manager.getString(R.string.terminal_auth_pubkey_security_key));
						if (trySecurityKey(pubkey)) {
							authenticated = true;
							if (openSessionAfterAuth) {
								finishConnection();
							}
						}
					} else {
						bridge.outputLine(manager.res.getString(R.string.terminal_auth_pubkey_specific));
						if (tryPublicKey(pubkey)) {
							authenticated = true;
							if (openSessionAfterAuth) {
								finishConnection();
							}
						}
					}
				}

				pubkeysExhausted = true;
			} else if (interactiveCanContinue && keyboardInteractiveAvailable) {
				// this auth method will talk with us using InteractiveCallback interface
				// it blocks until authentication finishes
				bridge.outputLine(manager.res.getString(R.string.terminal_auth_ki));
				interactiveCanContinue = false;
				if (connection.authenticateWithKeyboardInteractive(host.getUsername(), this)) {
					traceAuthEnd("SUCCESS_KEYBOARD_INTERACTIVE", null);
					authenticated = true;
					if (openSessionAfterAuth) {
						finishConnection();
					}
				} else {
					bridge.outputLine(manager.res.getString(R.string.terminal_auth_ki_fail));
					traceAuthEnd("KEYBOARD_INTERACTIVE_FAILED", null);
				}
			} else if (passwordAvailable) {
				bridge.outputLine(manager.res.getString(R.string.terminal_auth_pass));
				if (!host.getRememberPassword()) {
					clearSavedHostPassword();
				}

				String password = null;
				boolean usedSavedPassword = false;

				if (host.getRememberPassword()) {
					password = getSavedHostPassword();
					if (password != null) {
						usedSavedPassword = true;
						traceFlow(MARKER_PASSWORD_SOURCE + "=saved");
						if (connection.authenticateWithPassword(host.getUsername(), password)) {
							traceAuthEnd("SUCCESS_PASSWORD", "saved");
							finishConnection();
							return;
						}
						clearSavedHostPassword();
						password = null;
						usedSavedPassword = false;
					}
				}

				traceFlow(MARKER_PASSWORD_SOURCE + "=prompt");
				password = bridge.getPromptHelper().requestStringPrompt(null,
						manager.res.getString(R.string.prompt_password));
				if (password != null
						&& connection.authenticateWithPassword(host.getUsername(), password)) {
					saveHostPasswordIfEnabled(password);
					traceAuthEnd("SUCCESS_PASSWORD", null);
					authenticated = true;
					if (openSessionAfterAuth) {
						finishConnection();
					}
				} else {
					bridge.outputLine(manager.res.getString(R.string.terminal_auth_pass_fail));
					if (password == null) {
						traceAuthEnd("PASSWORD_CANCELLED", null);
					} else {
						traceAuthEnd("PASSWORD_FAILED", usedSavedPassword ? "saved" : "prompt");
					}
				}
			} else {
				bridge.outputLine(manager.res.getString(R.string.terminal_auth_fail));
				traceAuthEnd("NO_SUPPORTED_AUTH_METHOD", null);
			}
		} catch (IllegalStateException e) {
			Log.e(TAG, "Connection went away while we were trying to authenticate", e);
			traceDisconnectDetails("authenticate_illegal_state", e);
			traceAuthEnd("DISCONNECT_DURING_AUTH", describeThrowable(e));
		} catch (Exception e) {
			Log.e(TAG, "Problem during handleAuthentication()", e);
			traceDisconnectDetails("authenticate_exception", e);
			traceAuthEnd("AUTH_EXCEPTION", describeThrowable(e));
		}
	}

	private boolean trySecurityKey(PubkeyBean pubkey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		SecurityKeyProviderProfile securityKeyProfile = SecurityKeyProviderProfile.fromPubkey(pubkey);
		PublicKey pubKey = resolveSecurityKeyPublicKey(pubkey, securityKeyProfile);
		String nickname = pubkey.getNickname();
		byte[] sshPublicKey = resolveSshPublicKey(pubKey, pubkey);
		String keyType = getSshPublicKeyType(pubKey, sshPublicKey);
		String keyFingerprint = getPublicKeyFingerprint(sshPublicKey);
		String keyIdentity = "type=" + keyType + " fp=" + keyFingerprint;
		traceAuthKeyOffer("security_key", nickname, keyIdentity);
		traceFlow(MARKER_OFFERED_KEY_FP, keyFingerprint);
		traceFlow(MARKER_OFFERED_KEY_OPENSSH, getOpenSshPublicKeyLine(keyType, sshPublicKey));
		traceFlow(MARKER_SECURITY_KEY_PROVIDER_USED,
				"provider=" + securityKeyProfile.getProvider() + " slot=" + securityKeyProfile.getSlotReference());
		if (securityKeyProfile.isPiv()) {
			traceFlow(MARKER_PIV_SLOT_USED, securityKeyProfile.getSlotReference());
		} else if (securityKeyProfile.isOpenPgp()) {
			traceFlow(MARKER_OPENPGP_SLOT_USED, securityKeyProfile.getSlotReference());
		}
		if (securityKeyProfile.isFido2()) {
			return tryFido2SecurityKey(nickname, pubKey, keyType, sshPublicKey, securityKeyProfile);
		}

		SecurityKeySignatureProxy securityKeySignatureProxy = new SecurityKeySignatureProxy(
				pubKey,
				nickname,
				manager.getApplicationContext(),
				securityKeyProfile);
		boolean success;
		try {
			success = authenticateWithTwoStepPublicKey(host.getUsername(), pubKey, keyType, sshPublicKey, securityKeySignatureProxy);
		} catch (IOException e) {
			traceDisconnectDetails("security_key_exception", e);
			if (!securityKeySignatureProxy.wasSignRequested()) {
				tracePkStatus("security_key", "KEY_REJECTED_BEFORE_SIGN");
				traceAuthResult("security_key", nickname, "FAILED", MARKER_SIGN_NOT_CALLED);
			} else {
				tracePkStatus("security_key", "KEY_REJECTED");
				traceAuthResult("security_key", nickname, "FAILED", "SIGN_REQUESTED");
			}
			if (securityKeySignatureProxy.wasCancelled()) {
				traceAuthEnd("USER_CANCEL", "security_key");
			} else if (securityKeySignatureProxy.hadSignTimeout()) {
				traceAuthEnd("TIMEOUT", "security_key");
			} else {
				traceAuthEnd("AUTH_IO_EXCEPTION", describeThrowable(e));
			}
			Log.e(TAG, MARKER_SECURITY_KEY_AUTH_FAIL_FAST + ": security key authentication failed fast", e);
			bridge.outputLine(manager.res.getString(R.string.terminal_auth_pubkey_fail_security_key_fail_fast));
			bridge.outputLine(manager.res.getString(R.string.terminal_auth_pubkey_fail_security_key));
			return false;
		}

		if (success) {
			tracePkStatus("security_key", "KEY_ACCEPTED");
			traceAuthResult("security_key", nickname, "SUCCESS", null);
			traceAuthEnd("SUCCESS_PUBLICKEY_SECURITY_KEY", null);
		} else {
			if (securityKeySignatureProxy.wasCancelled()) {
				if (!securityKeySignatureProxy.wasSignRequested()) {
					tracePkStatus("security_key", "KEY_REJECTED_BEFORE_SIGN");
					traceAuthResult("security_key", nickname, "FAILED", MARKER_SIGN_NOT_CALLED);
				} else {
					tracePkStatus("security_key", "KEY_REJECTED");
					traceAuthResult("security_key", nickname, "FAILED", "SIGN_REQUESTED");
				}
				traceAuthEnd("USER_CANCEL", "security_key");
			} else if (!securityKeySignatureProxy.wasSignRequested()) {
				tracePkStatus("security_key", "KEY_REJECTED_BEFORE_SIGN");
				traceAuthResult("security_key", nickname, "FAILED", MARKER_SIGN_NOT_CALLED);
				traceAuthEnd("SERVER_REJECTED_KEY", "security_key_before_sign");
			} else {
				tracePkStatus("security_key", "KEY_REJECTED");
				traceAuthResult("security_key", nickname, "FAILED", "SIGN_REQUESTED");
				traceAuthEnd("SERVER_REJECTED_KEY", "security_key");
			}
			bridge.outputLine(manager.res.getString(R.string.terminal_auth_pubkey_fail_security_key));
		}
		return success;
	}

	private PublicKey resolveSecurityKeyPublicKey(PubkeyBean pubkey, SecurityKeyProviderProfile securityKeyProfile)
			throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		if (securityKeyProfile.isFido2()) {
			return SshSkPublicKey.fromBlob(pubkey.getPublicKey(), pubkey.getType());
		}
		return PubkeyUtils.decodePublic(pubkey.getPublicKey(), pubkey.getType());
	}

	private boolean tryFido2SecurityKey(String nickname, PublicKey publicKey, String keyType, byte[] sshPublicKey,
			SecurityKeyProviderProfile securityKeyProfile)
			throws IOException {
		SecurityKeySignatureProxy securityKeySignatureProxy = new SecurityKeySignatureProxy(
				publicKey,
				nickname,
				manager.getApplicationContext(),
				securityKeyProfile);

		try {
			boolean success = authenticateWithTwoStepPublicKey(
					host.getUsername(),
					publicKey,
					keyType,
					sshPublicKey,
					securityKeySignatureProxy);
			if (success) {
				tracePkStatus("security_key", "KEY_ACCEPTED");
				traceAuthResult("security_key", nickname, "SUCCESS", null);
				traceAuthEnd("SUCCESS_PUBLICKEY_SECURITY_KEY", "fido2");
				return true;
			}

			if (securityKeySignatureProxy.wasCancelled()) {
				if (!securityKeySignatureProxy.wasSignRequested()) {
					tracePkStatus("security_key", "KEY_REJECTED_BEFORE_SIGN");
					traceAuthResult("security_key", nickname, "FAILED", MARKER_SIGN_NOT_CALLED);
				} else {
					tracePkStatus("security_key", "KEY_REJECTED");
					traceAuthResult("security_key", nickname, "FAILED", "SIGN_REQUESTED");
				}
				traceAuthEnd("USER_CANCEL", "fido2");
			} else if (!securityKeySignatureProxy.wasSignRequested()) {
				tracePkStatus("security_key", "KEY_REJECTED_BEFORE_SIGN");
				traceAuthResult("security_key", nickname, "FAILED", MARKER_SIGN_NOT_CALLED);
				traceAuthEnd("SERVER_REJECTED_KEY", "fido2_before_sign");
			} else {
				tracePkStatus("security_key", "KEY_REJECTED");
				traceAuthResult("security_key", nickname, "FAILED", "SIGN_REQUESTED");
				traceAuthEnd("SERVER_REJECTED_KEY", "fido2");
			}
			bridge.outputLine(manager.res.getString(R.string.terminal_auth_pubkey_fail_security_key));
			return false;
		} catch (IOException e) {
			traceDisconnectDetails("security_key_fido2_exception", e);
			if (!securityKeySignatureProxy.wasSignRequested()) {
				tracePkStatus("security_key", "KEY_REJECTED_BEFORE_SIGN");
				traceAuthResult("security_key", nickname, "FAILED", MARKER_SIGN_NOT_CALLED);
			} else {
				tracePkStatus("security_key", "KEY_REJECTED");
				traceAuthResult("security_key", nickname, "FAILED", "SIGN_REQUESTED");
			}
			if (securityKeySignatureProxy.wasCancelled()) {
				traceAuthEnd("USER_CANCEL", "fido2");
			} else if (securityKeySignatureProxy.hadSignTimeout()) {
				traceAuthEnd("TIMEOUT", "fido2");
			} else {
				traceAuthEnd("AUTH_IO_EXCEPTION", describeThrowable(e));
			}
			bridge.outputLine(manager.res.getString(R.string.terminal_auth_pubkey_fail_security_key_fail_fast));
			bridge.outputLine(manager.res.getString(R.string.terminal_auth_pubkey_fail_security_key));
			return false;
		}
	}

	private boolean authenticateWithTwoStepPublicKey(String username, PublicKey publicKey, String keyType,
			byte[] sshPublicKey, SignatureProxy signatureProxy) throws IOException {
		if (sshPublicKey == null || sshPublicKey.length == 0) {
			throw new IOException("Public key is unavailable for two-step publickey authentication");
		}

		Object authManager = getAuthenticationManager(username);
		TransportManager transportManager = getAuthenticationTransportManager(authManager);
		sendPublicKeyOffer(transportManager, username, keyType, sshPublicKey);
		tracePkStatus("security_key", "KEY_OFFER_SENT");

		byte[] offerResponse = invokeGetNextMessage(authManager);
		if (isMessageType(offerResponse, SSH_MSG_USERAUTH_PK_OK)) {
			tracePkStatus("security_key", "PK_OK");

			String authAlgorithm = selectPublicKeyAuthAlgorithm(publicKey, keyType, transportManager);
			String digestAlgorithm = selectDigestAlgorithmForKey(publicKey, authAlgorithm);
			byte[] challenge = invokeGenerateSignedPublicKeyRequest(authManager, username, authAlgorithm, sshPublicKey);
			byte[] signature = signatureProxy.sign(challenge, digestAlgorithm);
			PacketUserauthRequestPublicKey signedRequest = new PacketUserauthRequestPublicKey(
					"ssh-connection",
					username,
					authAlgorithm,
					sshPublicKey,
					signature);
			transportManager.sendMessage(signedRequest.getPayload());

			byte[] signedResponse = invokeGetNextMessage(authManager);
			if (isMessageType(signedResponse, Packets.SSH_MSG_DISCONNECT)) {
				traceDisconnectPacketDetails("after_sign", signedResponse);
			}
			return invokeIsAuthenticationSuccessful(authManager, signedResponse);
		}

		if (isMessageType(offerResponse, Packets.SSH_MSG_USERAUTH_FAILURE)) {
			tracePkStatus("security_key", "KEY_REJECTED_BEFORE_SIGN");
			return invokeIsAuthenticationSuccessful(authManager, offerResponse);
		}

		if (isMessageType(offerResponse, Packets.SSH_MSG_DISCONNECT)) {
			traceDisconnectPacketDetails("key_offer", offerResponse);
			throw new IOException("Server disconnected during publickey key offer");
		}

		if (isMessageType(offerResponse, Packets.SSH_MSG_USERAUTH_SUCCESS)) {
			tracePkStatus("security_key", "KEY_ACCEPTED_WITHOUT_SIGN");
			return invokeIsAuthenticationSuccessful(authManager, offerResponse);
		}

		throw new IOException("Unexpected SSH message during publickey offer (type " + getSshMessageType(offerResponse) + ")");
	}

	private void sendPublicKeyOffer(TransportManager transportManager, String username, String keyType,
			byte[] sshPublicKey) throws IOException {
		TypesWriter typesWriter = new TypesWriter();
		typesWriter.writeByte(Packets.SSH_MSG_USERAUTH_REQUEST);
		typesWriter.writeString(username);
		typesWriter.writeString("ssh-connection");
		typesWriter.writeString(AUTH_PUBLICKEY);
		typesWriter.writeBoolean(false);
		typesWriter.writeString(keyType);
		typesWriter.writeString(sshPublicKey, 0, sshPublicKey.length);
		transportManager.sendMessage(typesWriter.getBytes());
	}

	private String selectPublicKeyAuthAlgorithm(PublicKey publicKey, String keyType, TransportManager transportManager) {
		if (!(publicKey instanceof RSAPublicKey)) {
			return keyType;
		}

		ExtensionInfo extensionInfo = transportManager.getExtensionInfo();
		if (extensionInfo == null) {
			return "ssh-rsa";
		}
		Set<String> acceptedAlgorithms = extensionInfo.getSignatureAlgorithmsAccepted();
		if (acceptedAlgorithms == null) {
			return "ssh-rsa";
		}
		if (acceptedAlgorithms.contains("rsa-sha2-512")) {
			return "rsa-sha2-512";
		}
		if (acceptedAlgorithms.contains("rsa-sha2-256")) {
			return "rsa-sha2-256";
		}
		return "ssh-rsa";
	}

	private String selectDigestAlgorithmForKey(PublicKey publicKey, String authAlgorithm) throws IOException {
		if (SshSkPublicKey.KEY_TYPE_SK_ED25519.equals(authAlgorithm)) {
			return "SHA-512";
		}
		if (SshSkPublicKey.KEY_TYPE_SK_ECDSA.equals(authAlgorithm)) {
			return "SHA-256";
		}
		if ("rsa-sha2-512".equals(authAlgorithm)) {
			return "SHA-512";
		}
		if ("rsa-sha2-256".equals(authAlgorithm)) {
			return "SHA-256";
		}
		if ("ssh-rsa".equals(authAlgorithm) || "ssh-dss".equals(authAlgorithm)) {
			return "SHA-1";
		}
		if ("ssh-ed25519".equals(authAlgorithm)) {
			return "SHA-512";
		}
		if (authAlgorithm != null && authAlgorithm.startsWith("ecdsa-sha2-")) {
			if (publicKey instanceof ECPublicKey) {
				try {
					return ECDSASHA2Verify.getDigestAlgorithmForParams(((ECPublicKey) publicKey).getParams());
				} catch (Exception e) {
					throw new IOException("Unsupported EC parameters for security key", e);
				}
			}
			if ("ecdsa-sha2-nistp256".equals(authAlgorithm)) {
				return "SHA-256";
			}
			if ("ecdsa-sha2-nistp384".equals(authAlgorithm)) {
				return "SHA-384";
			}
			if ("ecdsa-sha2-nistp521".equals(authAlgorithm)) {
				return "SHA-512";
			}
		}

		if (publicKey instanceof RSAPublicKey || publicKey instanceof DSAPublicKey) {
			return "SHA-1";
		}
		if (publicKey instanceof ECPublicKey) {
			try {
				return ECDSASHA2Verify.getDigestAlgorithmForParams(((ECPublicKey) publicKey).getParams());
			} catch (Exception e) {
				throw new IOException("Unsupported EC parameters for security key", e);
			}
		}
		if (publicKey instanceof Ed25519PublicKey) {
			return "SHA-512";
		}
		throw new IOException("Unknown public key type for security key authentication (authAlgorithm=" + authAlgorithm + ")");
	}

	private Object getAuthenticationManager(String username) throws IOException {
		try {
			Field authManagerField = Connection.class.getDeclaredField("am");
			authManagerField.setAccessible(true);
			Object authManager = authManagerField.get(connection);
			if (authManager == null) {
				connection.isAuthMethodAvailable(username, AUTH_PUBLICKEY);
				authManager = authManagerField.get(connection);
			}
			if (authManager == null) {
				throw new IOException("sshlib AuthenticationManager is unavailable");
			}
			return authManager;
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IOException("Unable to access sshlib AuthenticationManager", e);
		}
	}

	private TransportManager getAuthenticationTransportManager(Object authManager) throws IOException {
		try {
			Field transportManagerField = authManager.getClass().getDeclaredField("tm");
			transportManagerField.setAccessible(true);
			TransportManager transportManager = (TransportManager) transportManagerField.get(authManager);
			if (transportManager == null) {
				throw new IOException("sshlib TransportManager is unavailable");
			}
			return transportManager;
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IOException("Unable to access sshlib TransportManager", e);
		}
	}

	private byte[] invokeGetNextMessage(Object authManager) throws IOException {
		try {
			Method getNextMessage = authManager.getClass().getDeclaredMethod("getNextMessage");
			getNextMessage.setAccessible(true);
			return (byte[]) getNextMessage.invoke(authManager);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IOException("Unable to invoke sshlib getNextMessage()", e);
		} catch (InvocationTargetException e) {
			throw unwrapInvocationTargetException("sshlib getNextMessage() failed", e);
		}
	}

	private boolean invokeIsAuthenticationSuccessful(Object authManager, byte[] packet) throws IOException {
		try {
			Method isAuthenticationSuccessful =
					authManager.getClass().getDeclaredMethod("isAuthenticationSuccessful", byte[].class);
			isAuthenticationSuccessful.setAccessible(true);
			Object result = isAuthenticationSuccessful.invoke(authManager, packet);
			return Boolean.TRUE.equals(result);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IOException("Unable to invoke sshlib isAuthenticationSuccessful()", e);
		} catch (InvocationTargetException e) {
			throw unwrapInvocationTargetException("sshlib isAuthenticationSuccessful() failed", e);
		}
	}

	private byte[] invokeGenerateSignedPublicKeyRequest(Object authManager, String username, String authAlgorithm,
			byte[] sshPublicKey) throws IOException {
		try {
			Method generateRequest = authManager.getClass().getDeclaredMethod(
					"generatePublicKeyUserAuthenticationRequest",
					String.class,
					String.class,
					byte[].class);
			generateRequest.setAccessible(true);
			return (byte[]) generateRequest.invoke(authManager, username, authAlgorithm, sshPublicKey);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IOException("Unable to invoke sshlib signed publickey request generator", e);
		} catch (InvocationTargetException e) {
			throw unwrapInvocationTargetException("sshlib request generator failed", e);
		}
	}

	private IOException unwrapInvocationTargetException(String fallbackMessage, InvocationTargetException exception) {
		Throwable cause = exception.getCause();
		if (cause instanceof IOException) {
			return (IOException) cause;
		}
		return new IOException(fallbackMessage, cause != null ? cause : exception);
	}

	private boolean isMessageType(byte[] packet, int messageType) throws IOException {
		return getSshMessageType(packet) == messageType;
	}

	private int getSshMessageType(byte[] packet) throws IOException {
		if (packet == null || packet.length == 0) {
			throw new IOException("Received empty SSH packet");
		}
		return packet[0] & 0xff;
	}

	private void traceDisconnectPacketDetails(String stage, byte[] packet) {
		traceFlow(MARKER_SERVER_DISCONNECT_DETAIL, "stage=" + stage + " " + parseDisconnectPacket(packet));
	}

	private String parseDisconnectPacket(byte[] packet) {
		if (packet == null || packet.length == 0) {
			return "disconnect_packet=empty";
		}
		try {
			TypesReader reader = new TypesReader(packet);
			int messageType = reader.readByte();
			if (messageType != Packets.SSH_MSG_DISCONNECT) {
				return "message_type=" + messageType;
			}
			int reasonCode = reader.readUINT32();
			String description = reader.readString("UTF-8");
			String languageTag = reader.readString("UTF-8");
			return "reason_code=" + reasonCode + " description=" + description + " language=" + languageTag;
		} catch (Exception e) {
			return "disconnect_parse_error=" + e.getClass().getSimpleName();
		}
	}

	private void traceDisconnectDetails(String stage, Throwable throwable) {
		String details = "stage=" + stage + " exception=" + describeThrowable(throwable);
		Throwable transportCause = getTransportCloseCause();
		if (transportCause != null && transportCause != throwable) {
			details += " transport=" + describeThrowable(transportCause);
		}
		traceFlow(MARKER_SERVER_DISCONNECT_DETAIL, details);
	}

	private Throwable getTransportCloseCause() {
		if (connection == null) {
			return null;
		}
		try {
			Field transportManagerField = Connection.class.getDeclaredField("tm");
			transportManagerField.setAccessible(true);
			TransportManager transportManager = (TransportManager) transportManagerField.get(connection);
			if (transportManager == null) {
				return null;
			}
			return transportManager.getReasonClosedCause();
		} catch (Exception ignored) {
			return null;
		}
	}

	private String describeThrowable(Throwable throwable) {
		if (throwable == null) {
			return "unknown";
		}
		StringBuilder details = new StringBuilder(throwable.getClass().getSimpleName());
		String message = throwable.getMessage();
		if (message != null && !message.isEmpty()) {
			details.append("(").append(message).append(")");
		}
		Throwable cause = throwable.getCause();
		if (cause != null && cause != throwable) {
			details.append(" cause=").append(cause.getClass().getSimpleName());
			String causeMessage = cause.getMessage();
			if (causeMessage != null && !causeMessage.isEmpty()) {
				details.append("(").append(causeMessage).append(")");
			}
		}
		return details.toString();
	}

	/**
	 * Attempt connection with given {@code pubkey}.
	 * @return {@code true} for successful authentication
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws IOException
	 */
	private boolean tryPublicKey(PubkeyBean pubkey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		KeyPair pair = null;

		if (manager.isKeyLoaded(pubkey.getNickname())) {
			// load this key from memory if its already there
			Log.d(TAG, String.format("Found unlocked key '%s' already in-memory", pubkey.getNickname()));

			if (pubkey.isConfirmUse()) {
				if (!promptForPubkeyUse(pubkey.getNickname()))
					return false;
			}

			pair = manager.getKey(pubkey.getNickname());
		} else {
			// otherwise load key from database and prompt for password as needed
			String password = null;
			if (pubkey.isEncrypted()) {
				password = bridge.getPromptHelper().requestStringPrompt(null,
						manager.res.getString(R.string.prompt_pubkey_password, pubkey.getNickname()));

				// Something must have interrupted the prompt.
				if (password == null)
					return false;
			}

			if (PubkeyDatabase.KEY_TYPE_IMPORTED.equals(pubkey.getType())) {
				// load specific key using pem format
				pair = PEMDecoder.decode(new String(pubkey.getPrivateKey(), "UTF-8").toCharArray(), password);
			} else {
				// load using internal generated format
				PrivateKey privKey;
				try {
					privKey = PubkeyUtils.decodePrivate(pubkey.getPrivateKey(),
							pubkey.getType(), password);
				} catch (Exception e) {
					String message = String.format("Bad password for key '%s'. Authentication failed.", pubkey.getNickname());
					Log.e(TAG, message, e);
					bridge.outputLine(message);
					return false;
				}

				PublicKey pubKey = PubkeyUtils.decodePublic(pubkey.getPublicKey(), pubkey.getType());

				// convert key to trilead format
				pair = new KeyPair(pubKey, privKey);
				Log.d(TAG, "Unlocked key " + PubkeyUtils.formatKey(pubKey));
			}

			Log.d(TAG, String.format("Unlocked key '%s'", pubkey.getNickname()));

			// save this key in memory
			manager.addKey(pubkey, pair);
		}

		return tryPublicKey(host.getUsername(), pubkey.getNickname(), pair);
	}

	private boolean tryPublicKey(String username, String keyNickname, KeyPair pair) throws IOException {
		byte[] sshPublicKey = resolveSshPublicKey(pair.getPublic(), null);
		String keyType = getSshPublicKeyType(pair.getPublic(), sshPublicKey);
		String keyFingerprint = getPublicKeyFingerprint(sshPublicKey);
		String keyIdentity = "type=" + keyType + " fp=" + keyFingerprint;
		traceAuthKeyOffer("file_key", keyNickname, keyIdentity);
		traceFlow(MARKER_OFFERED_KEY_FP, keyFingerprint);
		traceFlow(MARKER_OFFERED_KEY_OPENSSH, getOpenSshPublicKeyLine(keyType, sshPublicKey));
		tracePkStatus("file_key", "PK_OK_CHECK_UNAVAILABLE_SINGLE_STEP_LIB");
		boolean success = connection.authenticateWithPublicKey(username, pair);
		if (!success) {
			tracePkStatus("file_key", "KEY_REJECTED");
			traceAuthResult("file_key", keyNickname, "FAILED", null);
			traceAuthEnd("SERVER_REJECTED_KEY", "file_key");
			bridge.outputLine(manager.res.getString(R.string.terminal_auth_pubkey_fail, keyNickname));
		} else {
			tracePkStatus("file_key", "KEY_ACCEPTED");
			traceAuthResult("file_key", keyNickname, "SUCCESS", null);
			traceAuthEnd("SUCCESS_PUBLICKEY_FILE_KEY", null);
		}
		return success;
	}

	private void traceAuthKeyOffer(String keySource, String keyNickname, String keyIdentity) {
		traceFlow(MARKER_AUTH_KEY_OFFER, "source=" + keySource + " nickname=" + keyNickname + " " + keyIdentity);
	}

	private void traceAuthResult(String keySource, String keyNickname, String resultCode, String extraCode) {
		String details = "source=" + keySource + " nickname=" + keyNickname + " result=" + resultCode;
		if (extraCode != null && !extraCode.isEmpty()) {
			details += " code=" + extraCode;
		}
		traceFlow(MARKER_AUTH_KEY_RESULT, details);
	}

	private void tracePkStatus(String keySource, String statusCode) {
		traceFlow(MARKER_AUTH_PK_STATUS, "source=" + keySource + " status=" + statusCode);
	}

	private void traceAuthEnd(String reasonCode, String details) {
		if (details == null || details.isEmpty()) {
			traceFlow(MARKER_AUTH_END_REASON, "reason=" + reasonCode);
		} else {
			traceFlow(MARKER_AUTH_END_REASON, "reason=" + reasonCode + " detail=" + details);
		}
	}

	private void traceFlow(String marker, String details) {
		if (manager == null) {
			return;
		}
		SecurityKeyDebugLog.logFlow(manager.getApplicationContext(), TAG, marker, details);
	}

	private void traceFlow(String marker) {
		if (manager == null) {
			return;
		}
		SecurityKeyDebugLog.logFlow(manager.getApplicationContext(), TAG, marker);
	}

	@Nullable
	private String getSavedHostPassword() {
		if (host == null || host.getId() <= 0 || manager == null) {
			return null;
		}
		return SavedPasswordStore.get(manager.getApplicationContext()).loadPassword(host.getId());
	}

	private void saveHostPasswordIfEnabled(String password) {
		if (host == null || host.getId() <= 0 || manager == null || password == null) {
			return;
		}
		SavedPasswordStore store = SavedPasswordStore.get(manager.getApplicationContext());
		if (host.getRememberPassword()) {
			store.savePassword(host.getId(), password);
		} else {
			store.clearPassword(host.getId());
		}
	}

	private void clearSavedHostPassword() {
		if (host == null || host.getId() <= 0 || manager == null) {
			return;
		}
		SavedPasswordStore.get(manager.getApplicationContext()).clearPassword(host.getId());
	}

	private String getHostForVerification(String fallbackHost) {
		return hostVerifierOverride == null ? fallbackHost : hostVerifierOverride;
	}

	private int getPortForVerification(int fallbackPort) {
		return portVerifierOverride > 0 ? portVerifierOverride : fallbackPort;
	}

	private void clearHostVerifierOverride() {
		hostVerifierOverride = null;
		portVerifierOverride = -1;
	}

	private String getPublicKeyType(PublicKey publicKey) {
		if (publicKey instanceof RSAPublicKey) {
			return "ssh-rsa";
		} else if (publicKey instanceof DSAPublicKey) {
			return "ssh-dss";
		} else if (publicKey instanceof ECPublicKey) {
			try {
				return ECDSASHA2Verify.getSshKeyType(((ECPublicKey) publicKey).getParams());
			} catch (Exception e) {
				return "ecdsa";
			}
		} else if (publicKey instanceof Ed25519PublicKey) {
			return Ed25519Verify.ED25519_ID;
		}
		return publicKey.getAlgorithm();
	}

	private String getPublicKeyFingerprint(byte[] sshPublicKey) {
		try {
			if (sshPublicKey == null || sshPublicKey.length == 0) {
				return "SHA256:unavailable";
			}
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(sshPublicKey);
			String fingerprint = String.valueOf(Base64.encode(hash)).replace("=", "");
			return "SHA256:" + fingerprint;
		} catch (Exception e) {
			return "SHA256:unavailable";
		}
	}

	private byte[] resolveSshPublicKey(PublicKey publicKey, PubkeyBean pubkey) {
		try {
			byte[] encoded = encodeSshPublicKey(publicKey);
			if (looksLikeSshPublicKey(encoded)) {
				return encoded;
			}
		} catch (IOException e) {
			// ignore and try fallback
		}

		if (pubkey != null) {
			byte[] fallback = encodeSshPublicKeyFromBean(pubkey);
			if (looksLikeSshPublicKey(fallback)) {
				return fallback;
			}
		}

		return null;
	}

	private byte[] encodeSshPublicKeyFromBean(PubkeyBean pubkey) {
		if (pubkey == null) {
			return null;
		}
		try {
			String type = pubkey.getType();
			if (SshSkPublicKey.isSupportedKeyType(type)) {
				return pubkey.getPublicKey();
			}
			if (PubkeyDatabase.KEY_TYPE_ED25519.equals(type)) {
				return encodeSshEd25519PublicKey(pubkey.getPublicKey());
			}

			PublicKey decodedPublicKey = PubkeyUtils.decodePublic(pubkey.getPublicKey(), type);
			return encodeSshPublicKey(decodedPublicKey);
		} catch (Exception e) {
			return null;
		}
	}

	private String getSshPublicKeyType(PublicKey publicKey, byte[] sshPublicKey) {
		String sshType = parseSshPublicKeyType(sshPublicKey);
		if (sshType != null) {
			return sshType;
		}
		return getPublicKeyType(publicKey);
	}

	private String parseSshPublicKeyType(byte[] sshPublicKey) {
		if (!looksLikeSshPublicKey(sshPublicKey)) {
			return null;
		}
		try {
			int keyTypeLength = readInt(sshPublicKey, 0);
			return new String(sshPublicKey, 4, keyTypeLength, "US-ASCII");
		} catch (Exception e) {
			return null;
		}
	}

	private String getOpenSshPublicKeyLine(String keyType, byte[] sshPublicKey) {
		if (keyType == null || keyType.isEmpty() || sshPublicKey == null || sshPublicKey.length == 0) {
			return "unavailable";
		}
		return keyType + " " + String.valueOf(Base64.encode(sshPublicKey));
	}

	private boolean looksLikeSshPublicKey(byte[] encoded) {
		if (encoded == null || encoded.length < 8) {
			return false;
		}

		int keyTypeLength = readInt(encoded, 0);
		if (keyTypeLength <= 0 || keyTypeLength > 64 || encoded.length < keyTypeLength + 4) {
			return false;
		}

		for (int i = 4; i < 4 + keyTypeLength; i++) {
			byte value = encoded[i];
			boolean valid = (value >= 'a' && value <= 'z')
					|| (value >= 'A' && value <= 'Z')
					|| (value >= '0' && value <= '9')
					|| value == '-'
					|| value == '_'
					|| value == '@'
					|| value == '.';
			if (!valid) {
				return false;
			}
		}
		return true;
	}

	private int readInt(byte[] data, int offset) {
		return ((data[offset] & 0xff) << 24)
				| ((data[offset + 1] & 0xff) << 16)
				| ((data[offset + 2] & 0xff) << 8)
				| (data[offset + 3] & 0xff);
	}

	private byte[] encodeSshPublicKey(PublicKey publicKey) throws IOException {
		if (publicKey instanceof RSAPublicKey) {
			return RSASHA1Verify.encodeSSHRSAPublicKey((RSAPublicKey) publicKey);
		} else if (publicKey instanceof DSAPublicKey) {
			return DSASHA1Verify.encodeSSHDSAPublicKey((DSAPublicKey) publicKey);
		} else if (publicKey instanceof ECPublicKey) {
			return ECDSASHA2Verify.encodeSSHECDSAPublicKey((ECPublicKey) publicKey);
		} else if (publicKey instanceof Ed25519PublicKey) {
			return Ed25519Verify.encodeSSHEd25519PublicKey((Ed25519PublicKey) publicKey);
		} else if (isEd25519Algorithm(publicKey.getAlgorithm())) {
			try {
				return encodeSshEd25519PublicKey(publicKey.getEncoded());
			} catch (InvalidKeySpecException e) {
				throw new IOException("Failed to encode Ed25519 public key", e);
			}
		}
		return publicKey.getEncoded();
	}

	private boolean isEd25519Algorithm(String algorithm) {
		return "Ed25519".equalsIgnoreCase(algorithm) || "EdDSA".equalsIgnoreCase(algorithm);
	}

	private byte[] encodeSshEd25519PublicKey(byte[] encodedKey) throws IOException, InvalidKeySpecException {
		if (encodedKey == null || encodedKey.length == 0) {
			throw new InvalidKeySpecException("Ed25519 public key is empty");
		}

		if (encodedKey.length == 32) {
			return Ed25519Verify.encodeSSHEd25519PublicKey(new Ed25519PublicKey(encodedKey));
		}

		Ed25519PublicKey ed25519PublicKey = new Ed25519PublicKey(new X509EncodedKeySpec(encodedKey));
		return Ed25519Verify.encodeSSHEd25519PublicKey(ed25519PublicKey);
	}

	/**
	 * Internal method to request actual PTY terminal once we've finished
	 * authentication. If called before authenticated, it will just fail.
	 */
	private void finishConnection() {
		if (sessionOpen) {
			return;
		}

		if (connection == null) {
			traceFlow(MARKER_SESSION_OPEN_FAILED, "connection_unavailable");
			return;
		}

		if (!connection.isAuthenticationComplete()) {
			traceFlow(MARKER_SESSION_OPEN_DEFERRED, "connection_not_authenticated");
			return;
		}

		authenticated = true;
		traceFlow(MARKER_SESSION_OPEN_ATTEMPT, "authenticated=true");

		for (PortForwardBean portForward : portForwards) {
			try {
				enablePortForward(portForward);
				bridge.outputLine(manager.res.getString(R.string.terminal_enable_portfoward, portForward.getDescription()));
			} catch (Exception e) {
				Log.e(TAG, "Error setting up port forward during connect", e);
			}
		}

		if (!host.getWantSession()) {
			bridge.outputLine(manager.res.getString(R.string.terminal_no_session));
			bridge.onConnected();
			return;
		}

		try {
			session = connection.openSession();

			if (!useAuthAgent.equals(HostDatabase.AUTHAGENT_NO))
				session.requestAuthAgentForwarding(this);

			session.requestPTY(getEmulation(), columns, rows, width, height, null);
			session.startShell();

			stdin = session.getStdin();
			stdout = session.getStdout();
			stderr = session.getStderr();

			sessionOpen = true;
			traceFlow(MARKER_SESSION_OPEN_SUCCESS, null);

			bridge.onConnected();
		} catch (IOException | IllegalStateException e1) {
			traceFlow(MARKER_SESSION_OPEN_FAILED, describeThrowable(e1));
			Log.e(TAG, "Problem while trying to create PTY in finishConnection()", e1);
		}

	}

	@Override
	public void connect() {
		clearHostVerifierOverride();
		pubkeysExhausted = false;
		interactiveCanContinue = true;
		if (!setupConnectionWithOptionalJumpHost()) {
			close();
			onDisconnect();
			return;
		}

		connection.addConnectionMonitor(this);

		try {
			// enter a loop to keep trying until authentication
			int tries = 0;
			while (connected && connection != null && !connection.isAuthenticationComplete() && tries++ < AUTH_TRIES) {
				authenticate();

				// sleep to make sure we dont kill system
				Thread.sleep(1000);
			}
			if (connected && connection != null && connection.isAuthenticationComplete()) {
				finishConnection();
			}
			if (connection == null) {
				traceAuthEnd("DISCONNECT", "connection_unavailable");
			} else if (!connection.isAuthenticationComplete()) {
				if (!connected) {
					traceAuthEnd("DISCONNECT", "before auth complete");
				} else if (tries >= AUTH_TRIES) {
					traceAuthEnd("TIMEOUT", "auth retries exhausted");
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Problem in SSH connection thread during authentication", e);
			traceDisconnectDetails("auth_loop", e);
			traceAuthEnd("AUTH_LOOP_EXCEPTION", describeThrowable(e));
		}
	}

	private boolean setupConnectionWithOptionalJumpHost() {
		HostBean targetHost = host;
		HostBean configuredJumpHost = resolveJumpHost(targetHost);

		if (configuredJumpHost != null) {
			if (!connectJumpHost(configuredJumpHost, targetHost)) {
				return false;
			}
			try {
				connection = new Connection("127.0.0.1", reserveAndCreateJumpForward(targetHost));
				hostVerifierOverride = targetHost.getHostname();
				portVerifierOverride = targetHost.getPort();
			} catch (Exception e) {
				Log.e(TAG, "Unable to create target connection via jump host", e);
				traceFlow(MARKER_JUMP_STAGE, "target_connection_failed=" + describeThrowable(e));
				return false;
			}
		} else {
			connection = new Connection(targetHost.getHostname(), targetHost.getPort());
			clearHostVerifierOverride();
		}

		try {
			connection.setCompression(compression);
		} catch (IOException e) {
			Log.e(TAG, "Could not enable compression!", e);
		}

		try {
			ConnectionInfo connectionInfo = connection.connect(new HostKeyVerifier());
			connected = true;

			bridge.outputLine(manager.res.getString(R.string.terminal_kex_algorithm,
					connectionInfo.keyExchangeAlgorithm));
			if (connectionInfo.clientToServerCryptoAlgorithm
					.equals(connectionInfo.serverToClientCryptoAlgorithm)
					&& connectionInfo.clientToServerMACAlgorithm
					.equals(connectionInfo.serverToClientMACAlgorithm)) {
				bridge.outputLine(manager.res.getString(R.string.terminal_using_algorithm,
						connectionInfo.clientToServerCryptoAlgorithm,
						connectionInfo.clientToServerMACAlgorithm));
			} else {
				bridge.outputLine(manager.res.getString(
						R.string.terminal_using_c2s_algorithm,
						connectionInfo.clientToServerCryptoAlgorithm,
						connectionInfo.clientToServerMACAlgorithm));

				bridge.outputLine(manager.res.getString(
						R.string.terminal_using_s2c_algorithm,
						connectionInfo.serverToClientCryptoAlgorithm,
						connectionInfo.serverToClientMACAlgorithm));
			}
			return true;
		} catch (IOException e) {
			Log.e(TAG, "Problem in SSH connection thread during authentication", e);
			traceDisconnectDetails("connect", e);
			traceAuthEnd("DISCONNECT_CONNECT_IO", describeThrowable(e));

			Throwable t = e.getCause();
			do {
				if (t != null && t.getMessage() != null) {
					bridge.outputLine(t.getMessage());
				}
				t = (t == null) ? null : t.getCause();
			} while (t != null);
			return false;
		}
	}

	@Nullable
	private HostBean resolveJumpHost(HostBean targetHost) {
		if (targetHost == null || targetHost.getJumpHostId() <= 0 || manager == null) {
			return null;
		}

		HostBean jumpHost = manager.hostdb.findHostById(targetHost.getJumpHostId());
		if (jumpHost == null) {
			traceFlow(MARKER_JUMP_STAGE, "jump_host_missing");
			return null;
		}
		if (!PROTOCOL.equals(jumpHost.getProtocol())) {
			traceFlow(MARKER_JUMP_STAGE, "jump_host_invalid_protocol=" + jumpHost.getProtocol());
			return null;
		}
		if (targetHost.getId() > 0 && jumpHost.getId() == targetHost.getId()) {
			traceFlow(MARKER_JUMP_STAGE, "jump_host_self_reference");
			return null;
		}
		return jumpHost;
	}

	private boolean connectJumpHost(HostBean jumpHost, HostBean targetHost) {
		try {
			traceFlow(MARKER_JUMP_HOST_USED, jumpHost.getNickname());
			traceFlow(MARKER_JUMP_STAGE, "jump_connect_start");
			jumpConnection = new Connection(jumpHost.getHostname(), jumpHost.getPort());
			try {
				jumpConnection.setCompression(jumpHost.getCompression());
			} catch (IOException ignored) {
			}
			clearHostVerifierOverride();
			jumpConnection.connect(new HostKeyVerifier());
			traceFlow(MARKER_JUMP_STAGE, "jump_connect_success");

			if (!authenticateJumpHost(jumpHost)) {
				traceFlow(MARKER_JUMP_STAGE, "jump_auth_failed");
				return false;
			}

			traceFlow(MARKER_JUMP_STAGE, "jump_auth_success");
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Jump host connection failed", e);
			traceFlow(MARKER_JUMP_STAGE, "jump_connect_failed=" + describeThrowable(e));
			traceDisconnectDetails("jump_connect", e);
			traceAuthEnd("JUMP_CONNECT_FAILED", describeThrowable(e));
			return false;
		}
	}

	private boolean authenticateJumpHost(HostBean jumpHost) {
		Connection previousConnection = connection;
		HostBean previousHost = host;
		boolean previousConnected = connected;
		boolean previousAuthenticated = authenticated;
		boolean previousSessionOpen = sessionOpen;
		boolean previousPubkeysExhausted = pubkeysExhausted;
		boolean previousInteractiveCanContinue = interactiveCanContinue;
		try {
			connection = jumpConnection;
			host = jumpHost;
			connected = true;
			authenticated = false;
			sessionOpen = false;
			pubkeysExhausted = false;
			interactiveCanContinue = true;

			int tries = 0;
			while (connection != null && !connection.isAuthenticationComplete() && tries++ < AUTH_TRIES) {
				authenticate(false);
				Thread.sleep(1000);
			}
			return connection != null && connection.isAuthenticationComplete();
		} catch (Exception e) {
			Log.e(TAG, "Jump host authentication failed", e);
			traceFlow(MARKER_JUMP_STAGE, "jump_auth_exception=" + describeThrowable(e));
			return false;
		} finally {
			connection = previousConnection;
			host = previousHost;
			connected = previousConnected;
			authenticated = previousAuthenticated;
			sessionOpen = previousSessionOpen;
			pubkeysExhausted = previousPubkeysExhausted;
			interactiveCanContinue = previousInteractiveCanContinue;
		}
	}

	private int reserveAndCreateJumpForward(HostBean targetHost) throws Exception {
		if (jumpConnection == null || targetHost == null) {
			throw new IOException("Jump host is not connected");
		}

		int localPort;
		try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
			localPort = serverSocket.getLocalPort();
		}

		jumpLocalPortForwarder = jumpConnection.createLocalPortForwarder(
				new InetSocketAddress(InetAddress.getByName("127.0.0.1"), localPort),
				targetHost.getHostname(),
				targetHost.getPort());
		traceFlow(MARKER_JUMP_STAGE, "forward_ready=127.0.0.1:" + localPort
				+ "->" + targetHost.getHostname() + ":" + targetHost.getPort());
		return localPort;
	}

	@Override
	public void close() {
		connected = false;
		clearHostVerifierOverride();

		if (session != null) {
			session.close();
			session = null;
		}

		if (connection != null) {
			connection.close();
			connection = null;
		}

		if (jumpLocalPortForwarder != null) {
			jumpLocalPortForwarder.close();
			jumpLocalPortForwarder = null;
		}

		if (jumpConnection != null) {
			jumpConnection.close();
			jumpConnection = null;
		}
	}

	private void onDisconnect() {
		bridge.dispatchDisconnect(false);
	}

	@Override
	public void flush() throws IOException {
		if (stdin != null)
			stdin.flush();
	}

	@Override
	public int read(byte[] buffer, int start, int len) throws IOException {
		int bytesRead = 0;

		if (session == null)
			return 0;

		int newConditions = session.waitForCondition(conditions, 0);

		if ((newConditions & ChannelCondition.STDOUT_DATA) != 0) {
			bytesRead = stdout.read(buffer, start, len);
		}

		if ((newConditions & ChannelCondition.STDERR_DATA) != 0) {
			byte discard[] = new byte[256];
			while (stderr.available() > 0) {
				stderr.read(discard);
			}
		}

		if ((newConditions & ChannelCondition.EOF) != 0) {
			close();
			onDisconnect();
			throw new IOException("Remote end closed connection");
		}

		return bytesRead;
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		if (stdin != null)
			stdin.write(buffer);
	}

	@Override
	public void write(int c) throws IOException {
		if (stdin != null)
			stdin.write(c);
	}

	@Override
	public Map<String, String> getOptions() {
		Map<String, String> options = new HashMap<>();

		options.put("compression", Boolean.toString(compression));

		return options;
	}

	@Override
	public void setOptions(Map<String, String> options) {
		if (options.containsKey("compression"))
			compression = Boolean.parseBoolean(options.get("compression"));
	}

	public static String getProtocolName() {
		return PROTOCOL;
	}

	@Override
	public boolean isSessionOpen() {
		return sessionOpen;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void connectionLost(Throwable reason) {
		if (reason != null) {
			traceDisconnectDetails("connection_lost", reason);
			traceAuthEnd("DISCONNECT", describeThrowable(reason));
		} else {
			traceAuthEnd("DISCONNECT", "unknown");
		}
		onDisconnect();
	}

	@Override
	public boolean canForwardPorts() {
		return true;
	}

	@Override
	public List<PortForwardBean> getPortForwards() {
		return portForwards;
	}

	@Override
	public boolean addPortForward(PortForwardBean portForward) {
		return portForwards.add(portForward);
	}

	@Override
	public boolean removePortForward(PortForwardBean portForward) {
		// Make sure we don't have a phantom forwarder.
		disablePortForward(portForward);

		return portForwards.remove(portForward);
	}

	@Override
	public boolean enablePortForward(PortForwardBean portForward) {
		if (!portForwards.contains(portForward)) {
			Log.e(TAG, "Attempt to enable port forward not in list");
			return false;
		}

		if (!authenticated)
			return false;

		if (HostDatabase.PORTFORWARD_LOCAL.equals(portForward.getType())) {
			LocalPortForwarder lpf = null;
			try {
				lpf = connection.createLocalPortForwarder(
						new InetSocketAddress(InetAddress.getLocalHost(), portForward.getSourcePort()),
						portForward.getDestAddr(), portForward.getDestPort());
			} catch (Exception e) {
				Log.e(TAG, "Could not create local port forward", e);
				return false;
			}

			if (lpf == null) {
				Log.e(TAG, "returned LocalPortForwarder object is null");
				return false;
			}

			portForward.setIdentifier(lpf);
			portForward.setEnabled(true);
			return true;
		} else if (HostDatabase.PORTFORWARD_REMOTE.equals(portForward.getType())) {
			try {
				connection.requestRemotePortForwarding("", portForward.getSourcePort(), portForward.getDestAddr(), portForward.getDestPort());
			} catch (Exception e) {
				Log.e(TAG, "Could not create remote port forward", e);
				return false;
			}

			portForward.setEnabled(true);
			return true;
		} else if (HostDatabase.PORTFORWARD_DYNAMIC5.equals(portForward.getType())) {
			DynamicPortForwarder dpf = null;

			try {
				dpf = connection.createDynamicPortForwarder(
						new InetSocketAddress(InetAddress.getLocalHost(), portForward.getSourcePort()));
			} catch (Exception e) {
				Log.e(TAG, "Could not create dynamic port forward", e);
				return false;
			}

			portForward.setIdentifier(dpf);
			portForward.setEnabled(true);
			return true;
		} else {
			// Unsupported type
			Log.e(TAG, String.format("attempt to forward unknown type %s", portForward.getType()));
			return false;
		}
	}

	@Override
	public boolean disablePortForward(PortForwardBean portForward) {
		if (!portForwards.contains(portForward)) {
			Log.e(TAG, "Attempt to disable port forward not in list");
			return false;
		}

		if (!authenticated)
			return false;

		if (HostDatabase.PORTFORWARD_LOCAL.equals(portForward.getType())) {
			LocalPortForwarder lpf = null;
			lpf = (LocalPortForwarder) portForward.getIdentifier();

			if (!portForward.isEnabled() || lpf == null) {
				Log.d(TAG, String.format("Could not disable %s; it appears to be not enabled or have no handler", portForward.getNickname()));
				return false;
			}

			portForward.setEnabled(false);

			lpf.close();

			return true;
		} else if (HostDatabase.PORTFORWARD_REMOTE.equals(portForward.getType())) {
			portForward.setEnabled(false);

			try {
				connection.cancelRemotePortForwarding(portForward.getSourcePort());
			} catch (IOException e) {
				Log.e(TAG, "Could not stop remote port forwarding, setting enabled to false", e);
				return false;
			}

			return true;
		} else if (HostDatabase.PORTFORWARD_DYNAMIC5.equals(portForward.getType())) {
			DynamicPortForwarder dpf = null;
			dpf = (DynamicPortForwarder) portForward.getIdentifier();

			if (!portForward.isEnabled() || dpf == null) {
				Log.d(TAG, String.format("Could not disable %s; it appears to be not enabled or have no handler", portForward.getNickname()));
				return false;
			}

			portForward.setEnabled(false);

			dpf.close();

			return true;
		} else {
			// Unsupported type
			Log.e(TAG, String.format("attempt to forward unknown type %s", portForward.getType()));
			return false;
		}
	}

	@Override
	public void setDimensions(int columns, int rows, int width, int height) {
		this.columns = columns;
		this.rows = rows;

		if (sessionOpen) {
			try {
				session.resizePTY(columns, rows, width, height);
			} catch (IOException e) {
				Log.e(TAG, "Couldn't send resize PTY packet", e);
			}
		}
	}

	@Override
	public int getDefaultPort() {
		return DEFAULT_PORT;
	}

	@Override
	public String getDefaultNickname(String username, String hostname, int port) {
		if (port == DEFAULT_PORT) {
			return String.format(Locale.US, "%s@%s", username, hostname);
		} else {
			return String.format(Locale.US, "%s@%s:%d", username, hostname, port);
		}
	}

	public static Uri getUri(String input) {
		Matcher matcher = hostmask.matcher(input);

		if (!matcher.matches())
			return null;

		StringBuilder sb = new StringBuilder();

		sb.append(PROTOCOL)
			.append("://")
			.append(Uri.encode(matcher.group(1)))
			.append('@')
			.append(Uri.encode(matcher.group(2)));

		String portString = matcher.group(6);
		int port = DEFAULT_PORT;
		if (portString != null) {
			try {
				port = Integer.parseInt(portString);
				if (port < 1 || port > 65535) {
					port = DEFAULT_PORT;
				}
			} catch (NumberFormatException nfe) {
				// Keep the default port
			}
		}

		if (port != DEFAULT_PORT) {
			sb.append(':')
				.append(port);
		}

		sb.append("/#")
			.append(Uri.encode(input));

		Uri uri = Uri.parse(sb.toString());

		return uri;
	}

	/**
	 * Handle challenges from keyboard-interactive authentication mode.
	 */
	@Override
	public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) {
		interactiveCanContinue = true;
		String[] responses = new String[numPrompts];
		for (int i = 0; i < numPrompts; i++) {
			// request response from user for each prompt
			responses[i] = bridge.promptHelper.requestStringPrompt(instruction, prompt[i]);
		}
		return responses;
	}

	@Override
	public HostBean createHost(Uri uri) {
		HostBean host = new HostBean();

		host.setProtocol(PROTOCOL);

		host.setHostname(uri.getHost());

		int port = uri.getPort();
		if (port < 0)
			port = DEFAULT_PORT;
		host.setPort(port);

		host.setUsername(uri.getUserInfo());

		String nickname = uri.getFragment();
		if (nickname == null || nickname.length() == 0) {
			host.setNickname(getDefaultNickname(host.getUsername(),
					host.getHostname(), host.getPort()));
		} else {
			host.setNickname(uri.getFragment());
		}

		return host;
	}

	@Override
	public void getSelectionArgs(Uri uri, Map<String, String> selection) {
		selection.put(HostDatabase.FIELD_HOST_PROTOCOL, PROTOCOL);
		selection.put(HostDatabase.FIELD_HOST_NICKNAME, uri.getFragment());
		selection.put(HostDatabase.FIELD_HOST_HOSTNAME, uri.getHost());

		int port = uri.getPort();
		if (port < 0)
			port = DEFAULT_PORT;
		selection.put(HostDatabase.FIELD_HOST_PORT, Integer.toString(port));
		selection.put(HostDatabase.FIELD_HOST_USERNAME, uri.getUserInfo());
	}

	@Override
	public void setCompression(boolean compression) {
		this.compression = compression;
	}

	public static String getFormatHint(Context context) {
		return String.format("%s@%s:%s",
				context.getString(R.string.format_username),
				context.getString(R.string.format_hostname),
				context.getString(R.string.format_port));
	}

	@Override
	public void setUseAuthAgent(String useAuthAgent) {
		this.useAuthAgent = useAuthAgent;
	}

	@Override
	public Map<String, byte[]> retrieveIdentities() {
		Map<String, byte[]> pubKeys = new HashMap<>(manager.loadedKeypairs.size());

		for (Entry<String, KeyHolder> entry : manager.loadedKeypairs.entrySet()) {
			KeyPair pair = entry.getValue().pair;

			try {
				PrivateKey privKey = pair.getPrivate();
				if (privKey instanceof RSAPrivateKey) {
					RSAPublicKey pubkey = (RSAPublicKey) pair.getPublic();
					pubKeys.put(entry.getKey(), RSASHA1Verify.encodeSSHRSAPublicKey(pubkey));
				} else if (privKey instanceof DSAPrivateKey) {
					DSAPublicKey pubkey = (DSAPublicKey) pair.getPublic();
					pubKeys.put(entry.getKey(), DSASHA1Verify.encodeSSHDSAPublicKey(pubkey));
				} else if (privKey instanceof ECPrivateKey) {
					ECPublicKey pubkey = (ECPublicKey) pair.getPublic();
					pubKeys.put(entry.getKey(), ECDSASHA2Verify.encodeSSHECDSAPublicKey(pubkey));
				} else if (privKey instanceof Ed25519PrivateKey) {
					Ed25519PublicKey pubkey = (Ed25519PublicKey) pair.getPublic();
					pubKeys.put(entry.getKey(), Ed25519Verify.encodeSSHEd25519PublicKey(pubkey));
				} else
					continue;
			} catch (IOException e) {
				continue;
			}
		}

		return pubKeys;
	}

	@Override
	public KeyPair getKeyPair(byte[] publicKey) {
		String nickname = manager.getKeyNickname(publicKey);

		if (nickname == null)
			return null;

		if (useAuthAgent.equals(HostDatabase.AUTHAGENT_NO)) {
			Log.e(TAG, "");
			return null;
		} else if (useAuthAgent.equals(HostDatabase.AUTHAGENT_CONFIRM) ||
				manager.loadedKeypairs.get(nickname).bean.isConfirmUse()) {
			if (!promptForPubkeyUse(nickname))
				return null;
		}
		return manager.getKey(nickname);
	}

	private boolean promptForPubkeyUse(String nickname) {
		Boolean result = bridge.promptHelper.requestBooleanPrompt(null,
				manager.res.getString(R.string.prompt_allow_agent_to_use_key,
						nickname));
		return result;
	}

	@Override
	public boolean addIdentity(KeyPair pair, String comment, boolean confirmUse, int lifetime) {
		PubkeyBean pubkey = new PubkeyBean();
//		pubkey.setType(PubkeyDatabase.KEY_TYPE_IMPORTED);
		pubkey.setNickname(comment);
		pubkey.setConfirmUse(confirmUse);
		pubkey.setLifetime(lifetime);
		manager.addKey(pubkey, pair);
		return true;
	}

	@Override
	public boolean removeAllIdentities() {
		manager.loadedKeypairs.clear();
		return true;
	}

	@Override
	public boolean removeIdentity(byte[] publicKey) {
		return manager.removeKey(publicKey);
	}

	@Override
	public boolean isAgentLocked() {
		return agentLockPassphrase != null;
	}

	@Override
	public boolean requestAgentUnlock(String unlockPassphrase) {
		if (agentLockPassphrase == null)
			return false;

		if (agentLockPassphrase.equals(unlockPassphrase))
			agentLockPassphrase = null;

		return agentLockPassphrase == null;
	}

	@Override
	public boolean setAgentLock(String lockPassphrase) {
		if (agentLockPassphrase != null)
			return false;

		agentLockPassphrase = lockPassphrase;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.connectbot.transport.AbsTransport#usesNetwork()
	 */
	@Override
	public boolean usesNetwork() {
		return true;
	}
}
