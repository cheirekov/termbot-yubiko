/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2019 Dominik Schürmann <dominik@cotech.de>
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

package org.connectbot;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.connectbot.securitykey.SecurityKeyProviderProfile;
import org.connectbot.securitykey.SecurityKeyAuthenticatorBridge;
import org.connectbot.securitykey.SshSkPublicKey;
import org.connectbot.service.SecurityKeyService;
import org.connectbot.util.SecurityKeyDebugLog;

import com.trilead.ssh2.auth.SignatureProxy;
import com.trilead.ssh2.crypto.keys.Ed25519PublicKey;
import com.trilead.ssh2.packets.TypesReader;
import com.trilead.ssh2.signature.ECDSASHA2Verify;
import com.trilead.ssh2.signature.Ed25519Verify;
import com.trilead.ssh2.signature.RSASHA1Verify;
import com.trilead.ssh2.signature.RSASHA256Verify;
import com.trilead.ssh2.signature.RSASHA512Verify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.WorkerThread;


public class SecurityKeySignatureProxy extends SignatureProxy {
	private static final String TAG = "CB.SKSignatureProxy";
	private static final long SECURITY_KEY_WAIT_LOG_INTERVAL_SECONDS = 30L;
	private static final String MARKER_PROXY_CREATE = "SK_PROXY_CREATE";
	private static final String MARKER_SERVICE_CONNECTED = "SK_PROXY_SERVICE_CONNECTED";
	private static final String MARKER_SERVICE_DISCONNECTED = "SK_PROXY_SERVICE_DISCONNECTED";
	private static final String MARKER_BIND_FAILED = "SK_BIND_FAILED";
	private static final String MARKER_WAITING_FOR_KEY = "SK_WAITING_FOR_KEY";
	private static final String MARKER_SIGN_REQUESTED = "SIGN_REQUESTED";
	private static final String MARKER_SIGN_END = "SK_SIGN_END";
	private static final String MARKER_SIGN_TIMEOUT = "SK_SIGN_TIMEOUT";
	private static final String MARKER_SIGN_RETRY = "SK_SIGN_RETRY";
	private static final String MARKER_SIGN_FAILED = "SK_SIGN_FAILED";
	private static final String MARKER_SET_AUTHENTICATOR = "SK_PROXY_SET_AUTHENTICATOR";
	private static final String MARKER_CANCELLED = "SK_PROXY_CANCELLED";
	private static final String MARKER_WAIT_INTERRUPTED = "SK_WAIT_INTERRUPTED";
	private static final String MARKER_AUTHENTICATOR_MISSING = "SK_AUTHENTICATOR_MISSING";
	private static final String MARKER_SIGNATURE_META = "SK_SIGNATURE_META";

	private CountDownLatch mResultReadyLatch;

	private SecurityKeyAuthenticatorBridge mSecurityKeyAuthenticator;
	private final Context mAppContext;
	private final String mPubkeyNickname;
	private final SecurityKeyProviderProfile mSecurityKeyProviderProfile;

	private SecurityKeyService mSecurityKeyService = null;
	private volatile boolean cancelled;
	private volatile boolean signRequested;
	private volatile boolean signTimeoutObserved;

	public SecurityKeySignatureProxy(PublicKey publicKey, String pubkeyNickname, Context appContext) {
		this(publicKey, pubkeyNickname, appContext, SecurityKeyProviderProfile.DEFAULT_OPENPGP_AUTH);
	}

	public SecurityKeySignatureProxy(PublicKey publicKey, String pubkeyNickname, Context appContext,
			SecurityKeyProviderProfile securityKeyProviderProfile) {
		super(publicKey);
		mAppContext = appContext.getApplicationContext();
		mPubkeyNickname = pubkeyNickname;
		mSecurityKeyProviderProfile = securityKeyProviderProfile == null
				? SecurityKeyProviderProfile.DEFAULT_OPENPGP_AUTH
				: securityKeyProviderProfile;
		SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_PROXY_CREATE);

		mResultReadyLatch = new CountDownLatch(1);

		ServiceConnection mSecurityKeyServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName className, IBinder service) {
				mSecurityKeyService = ((SecurityKeyService.SecurityKeyServiceBinder) service).getService();
				SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_SERVICE_CONNECTED);
				mSecurityKeyService.setSignatureProxy(SecurityKeySignatureProxy.this);
				PublicKey key = getPublicKey();
				mSecurityKeyService.startActivity(
						mPubkeyNickname,
						mSecurityKeyProviderProfile.getProvider(),
						mSecurityKeyProviderProfile.getSlotReference(),
						key == null ? null : key.getAlgorithm(),
						key == null ? null : key.getEncoded());
			}

			@Override
			public void onServiceDisconnected(ComponentName className) {
				mSecurityKeyService = null;
				SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_SERVICE_DISCONNECTED);
			}
		};
		boolean serviceBound = appContext.bindService(
				new Intent(appContext, SecurityKeyService.class),
				mSecurityKeyServiceConnection,
				Context.BIND_AUTO_CREATE);
		if (!serviceBound) {
			Log.e(TAG, MARKER_BIND_FAILED + ": unable to bind SecurityKeyService");
			SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_BIND_FAILED, "unable to bind service");
			cancel();
		}
	}

	public void setAuthenticator(SecurityKeyAuthenticatorBridge securityKeyAuthenticator) {
		SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_SET_AUTHENTICATOR);
		this.mSecurityKeyAuthenticator = securityKeyAuthenticator;
		mResultReadyLatch.countDown();
	}

	public void cancel() {
		SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_CANCELLED);
		cancelled = true;
		mResultReadyLatch.countDown();
	}

	@Override
	public byte[] sign(final byte[] challenge, final String hashAlgorithm) throws IOException {
		signRequested = true;
		SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_SIGN_REQUESTED, hashAlgorithm);
		while (true) {
			waitForSecurityKey();
			if (cancelled) {
				SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_CANCELLED, "sign aborted");
				throw new IOException("Cancelled!");
			}
			if (mSecurityKeyAuthenticator == null) {
				Log.e(TAG, MARKER_AUTHENTICATOR_MISSING + ": authenticator is null after wait");
				SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_AUTHENTICATOR_MISSING);
				throw new IOException("Authenticator unavailable for hardware security key authentication");
			}
				byte[] signature = tryAuthOperation(challenge, hashAlgorithm);
				if (signature != null) {
					logSignatureMeta(signature);
					SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_SIGN_END, hashAlgorithm);
					return signature;
				}
			}
	}

	@WorkerThread
	private byte[] tryAuthOperation(byte[] challenge, String hashAlgorithm) throws IOException {
		try {
			byte[] ds = mSecurityKeyAuthenticator.authenticateWithDigest(challenge, hashAlgorithm);
			byte[] encodedSignature = ds;
			if (!(getPublicKey() instanceof SshSkPublicKey)) {
				encodedSignature = encodeSignature(ds, hashAlgorithm);
			}

			mSecurityKeyAuthenticator.dismissDialog();
			return encodedSignature;
		} catch (IOException e) {
			String detail = e.getClass().getSimpleName();
			if (e.getMessage() != null && !e.getMessage().isEmpty()) {
				detail = detail + "(" + e.getMessage() + ")";
			}
			SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_SIGN_RETRY, detail);
			if (mSecurityKeyAuthenticator != null) {
				mSecurityKeyAuthenticator.postError(e);
			}
			if (!isRetryableAuthError(e)) {
				SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_SIGN_FAILED, detail);
				throw e;
			}
			return null;

		} catch (NoSuchAlgorithmException e) {
			throw new IOException("NoSuchAlgorithmException");
		}
	}

	private boolean isRetryableAuthError(IOException e) {
		if (SecurityKeyProviderProfile.PROVIDER_OPENPGP.equals(mSecurityKeyProviderProfile.getProvider())) {
			return false;
		}
		String message = e.getMessage();
		if (message == null) {
			return true;
		}
		String normalized = message.toLowerCase(Locale.US);
		if (normalized.contains("invalid pin/puk")
				|| normalized.contains("invalid pin")
				|| normalized.contains("wrong pin")) {
			return false;
		}
		return true;
	}

	private void waitForSecurityKey() throws IOException {
		try {
			while (!mResultReadyLatch.await(SECURITY_KEY_WAIT_LOG_INTERVAL_SECONDS, TimeUnit.SECONDS)) {
				if (cancelled) {
					return;
				}
				signTimeoutObserved = true;
				Log.w(TAG, MARKER_WAITING_FOR_KEY + ": still waiting for security key callback");
				SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_SIGN_TIMEOUT, "waiting for security key callback");
			}
			mResultReadyLatch = new CountDownLatch(1);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Log.e(TAG, MARKER_WAIT_INTERRUPTED + ": interrupted while waiting for security key callback");
			SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_WAIT_INTERRUPTED);
			throw new IOException("Interrupted while waiting for security key callback", e);
		}
	}

	/**
	 * Based on methods from AuthenticationManager in sshlib
	 */
	private byte[] encodeSignature(byte[] ds, String hashAlgorithm) throws IOException {
		PublicKey publicKey = getPublicKey();
		if (publicKey instanceof RSAPublicKey) {
			switch (hashAlgorithm) {
			case SHA512: {
				return RSASHA512Verify.encodeRSASHA512Signature(ds);
			}
			case SHA256: {
				return RSASHA256Verify.encodeRSASHA256Signature(ds);
			}
			case SHA1: {
				return RSASHA1Verify.encodeSSHRSASignature(ds);
			}
			default:
				throw new IOException("Unsupported algorithm in SecurityKeySignatureProxy!");
			}
		} else if (publicKey instanceof ECPublicKey) {
			ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
			return ECDSASHA2Verify.encodeSSHECDSASignature(ds, ecPublicKey.getParams());
		} else if (publicKey instanceof Ed25519PublicKey || isEd25519PublicKey(publicKey)) {
			return Ed25519Verify.encodeSSHEd25519Signature(ds);
		} else {
			throw new IOException("Unsupported algorithm in SecurityKeySignatureProxy!");
		}
	}

	private boolean isEd25519PublicKey(PublicKey publicKey) {
		if (publicKey == null) {
			return false;
		}
		String algorithm = publicKey.getAlgorithm();
		return "Ed25519".equalsIgnoreCase(algorithm)
				|| "EdDSA".equalsIgnoreCase(algorithm)
				|| "1.3.101.112".equals(algorithm);
	}

	private void logSignatureMeta(byte[] signature) {
		PublicKey publicKey = getPublicKey();
		if (!(publicKey instanceof SshSkPublicKey) || signature == null) {
			return;
		}
		try {
			TypesReader outerReader = new TypesReader(signature);
			String outerType = outerReader.readString("US-ASCII");
			byte[] signatureField = outerReader.readByteString();
			int flags;
			long counter;
			if (outerReader.remain() >= 5) {
				flags = outerReader.readByte() & 0xff;
				counter = outerReader.readUINT32() & 0xffffffffL;
			} else {
				// Backward compatibility for older nested format while debugging transitions.
				TypesReader nestedReader = new TypesReader(signatureField);
				signatureField = nestedReader.readByteString();
				flags = nestedReader.readByte() & 0xff;
				counter = nestedReader.readUINT32() & 0xffffffffL;
			}
			String meta = "outer=" + outerType
					+ " sig_len=" + signatureField.length
					+ " flags=0x" + Integer.toHexString(flags)
					+ " counter=" + counter;
			SecurityKeyDebugLog.logFlow(mAppContext, TAG, MARKER_SIGNATURE_META, meta);
		} catch (IOException e) {
			SecurityKeyDebugLog.logFlow(
					mAppContext,
					TAG,
					MARKER_SIGNATURE_META,
					"parse_failed=" + e.getClass().getSimpleName());
		}
	}

	public boolean wasSignRequested() {
		return signRequested;
	}

	public boolean wasCancelled() {
		return cancelled;
	}

	public boolean hadSignTimeout() {
		return signTimeoutObserved;
	}
}
