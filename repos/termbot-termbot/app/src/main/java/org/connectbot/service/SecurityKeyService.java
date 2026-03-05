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

package org.connectbot.service;

import org.connectbot.SecurityKeySignatureProxy;
import org.connectbot.SecurityKeyActivity;
import org.connectbot.securitykey.SecurityKeyAuthenticatorBridge;
import org.connectbot.util.SecurityKeyDebugLog;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

/**
 * This service is used to share data between SecurityKeySignatureProxy and SecurityKeyActivity
 */
public class SecurityKeyService extends Service {
	private static final String TAG = "CB.SKService";
	private static final String MARKER_SET_SIGNATURE_PROXY = "SK_SERVICE_SET_SIGNATURE_PROXY";
	private static final String MARKER_START_ACTIVITY = "SK_SERVICE_START_ACTIVITY";
	private static final String MARKER_SET_AUTHENTICATOR = "SK_SERVICE_SET_AUTHENTICATOR";
	private static final String MARKER_CANCEL = "SK_SERVICE_CANCEL";
	private static final String MARKER_PROXY_MISSING_SET_AUTH = "SK_SERVICE_PROXY_MISSING_SET_AUTH";
	private static final String MARKER_PROXY_MISSING_CANCEL = "SK_SERVICE_PROXY_MISSING_CANCEL";

	SecurityKeySignatureProxy mSignatureProxy;
	private SecurityKeyAuthenticatorBridge mPendingAuthenticator;
	private boolean mPendingCancel;

	public class SecurityKeyServiceBinder extends Binder {
		public SecurityKeyService getService() {
			return SecurityKeyService.this;
		}
	}

	private final IBinder mSecurityKeyServiceBinder = new SecurityKeyServiceBinder();

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return mSecurityKeyServiceBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		/*
		 * The lifecycle of this service is bound to the lifecycle of TerminalManager, since
		 * authentication might need to occur in the background if connectivity is temporarily
		 * lost, so this service needs to run as long as there are TerminalBridges active in
		 * TerminalManager
		 */
		return START_STICKY;
	}

	public void startActivity(String pubKeyNickname, String provider, String slotReference,
			String publicKeyAlgorithm, byte[] publicKeyEncoded) {
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_START_ACTIVITY);
		Intent intent = new Intent(this, SecurityKeyActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(SecurityKeyActivity.EXTRA_PUBKEY_NICKNAME, pubKeyNickname);
		intent.putExtra(SecurityKeyActivity.EXTRA_SECURITY_KEY_PROVIDER, provider);
		intent.putExtra(SecurityKeyActivity.EXTRA_SECURITY_KEY_SLOT_REFERENCE, slotReference);
		intent.putExtra(SecurityKeyActivity.EXTRA_SECURITY_KEY_PUBLIC_KEY_ALGORITHM, publicKeyAlgorithm);
		intent.putExtra(SecurityKeyActivity.EXTRA_SECURITY_KEY_PUBLIC_KEY_BYTES, publicKeyEncoded);
		startActivity(intent);
	}

	public void setSignatureProxy(SecurityKeySignatureProxy signatureProxy) {
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_SET_SIGNATURE_PROXY);
		mSignatureProxy = signatureProxy;
		deliverPendingActions();
	}

	public synchronized void setAuthenticator(SecurityKeyAuthenticatorBridge securityKeyAuthenticator) {
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_SET_AUTHENTICATOR);
		if (mSignatureProxy == null) {
			Log.w(TAG, MARKER_PROXY_MISSING_SET_AUTH + ": signature proxy not ready, queuing authenticator");
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PROXY_MISSING_SET_AUTH, "queue authenticator");
			mPendingCancel = false;
			mPendingAuthenticator = securityKeyAuthenticator;
			return;
		}
		mSignatureProxy.setAuthenticator(securityKeyAuthenticator);
	}

	public synchronized void cancel() {
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_CANCEL);
		if (mSignatureProxy == null) {
			Log.w(TAG, MARKER_PROXY_MISSING_CANCEL + ": cancel requested before signature proxy was attached, queuing cancellation");
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PROXY_MISSING_CANCEL, "queue cancel");
			mPendingCancel = true;
			mPendingAuthenticator = null;
			return;
		}
		mSignatureProxy.cancel();
	}

	private synchronized void deliverPendingActions() {
		if (mSignatureProxy == null) {
			return;
		}
		if (mPendingCancel) {
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PROXY_MISSING_CANCEL, "deliver queued cancel");
			mSignatureProxy.cancel();
			mPendingCancel = false;
			mPendingAuthenticator = null;
			return;
		}
		if (mPendingAuthenticator != null) {
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PROXY_MISSING_SET_AUTH, "deliver queued authenticator");
			mSignatureProxy.setAuthenticator(mPendingAuthenticator);
			mPendingAuthenticator = null;
		}
	}

}
