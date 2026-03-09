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

package org.connectbot.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stores per-host passwords in private app storage as encrypted payloads.
 * Uses Android Keystore-backed encryption on API 23+ and a non-plaintext fallback otherwise.
 */
public final class SavedPasswordStore {
	private static final String TAG = "CB.SavedPasswordStore";

	private static final String PREFS_NAME = "saved_host_passwords";
	private static final String PREFS_KEY_PREFIX = "host_";
	private static final String PREFS_SCOPED_KEY_PREFIX = "scoped_";

	public static final String SCOPE_SSM_SECRET_ACCESS_KEY = "ssm_secret_access_key";
	public static final String SCOPE_SSM_SESSION_TOKEN = "ssm_session_token";

	private static final String FORMAT_KEYSTORE = "ks1";
	private static final String FORMAT_FALLBACK = "fb1";

	private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
	private static final String KEY_ALIAS = "org.connectbot.saved_password_key";
	private static final String AES_MODE = "AES/GCM/NoPadding";
	private static final int GCM_TAG_BITS = 128;

	private static final int FALLBACK_SALT_BYTES = 16;
	private static final int FALLBACK_ITERATIONS = 7000;

	private static final Object sInstanceLock = new Object();
	private static SavedPasswordStore sInstance;

	private final Context mContext;
	private final SharedPreferences mPreferences;
	private final SecureRandom mSecureRandom = new SecureRandom();

	public static SavedPasswordStore get(@NonNull Context context) {
		synchronized (sInstanceLock) {
			if (sInstance == null) {
				sInstance = new SavedPasswordStore(context.getApplicationContext());
			}
			return sInstance;
		}
	}

	private SavedPasswordStore(@NonNull Context context) {
		mContext = context;
		mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	public synchronized void savePassword(long hostId, @Nullable String password) {
		if (hostId <= 0 || password == null) {
			clearPassword(hostId);
			return;
		}

		String encrypted = encrypt(password);
		if (encrypted == null) {
			Log.w(TAG, "Failed to encrypt saved password for host " + hostId);
			return;
		}

		mPreferences.edit().putString(getHostKey(hostId), encrypted).apply();
	}

	@Nullable
	public synchronized String loadPassword(long hostId) {
		if (hostId <= 0) {
			return null;
		}

		String encrypted = mPreferences.getString(getHostKey(hostId), null);
		if (encrypted == null || encrypted.isEmpty()) {
			return null;
		}

		String decrypted = decrypt(encrypted);
		if (decrypted == null) {
			// Clear corrupted/invalid entries to avoid repeated failures.
			clearPassword(hostId);
		}
		return decrypted;
	}

	public synchronized boolean hasSavedPassword(long hostId) {
		if (hostId <= 0) {
			return false;
		}
		return mPreferences.contains(getHostKey(hostId));
	}

	public synchronized void clearPassword(long hostId) {
		if (hostId <= 0) {
			return;
		}
		mPreferences.edit().remove(getHostKey(hostId)).apply();
	}

	public synchronized void saveScopedSecret(@NonNull String scope, long hostId,
			@Nullable String secret) {
		if (hostId <= 0 || scope.trim().isEmpty() || secret == null) {
			clearScopedSecret(scope, hostId);
			return;
		}

		String encrypted = encrypt(secret);
		if (encrypted == null) {
			Log.w(TAG, "Failed to encrypt scoped secret for " + scope + " host " + hostId);
			return;
		}

		mPreferences.edit().putString(getScopedKey(scope, hostId), encrypted).apply();
	}

	@Nullable
	public synchronized String loadScopedSecret(@NonNull String scope, long hostId) {
		if (hostId <= 0 || scope.trim().isEmpty()) {
			return null;
		}

		String encrypted = mPreferences.getString(getScopedKey(scope, hostId), null);
		if (encrypted == null || encrypted.isEmpty()) {
			return null;
		}

		String decrypted = decrypt(encrypted);
		if (decrypted == null) {
			clearScopedSecret(scope, hostId);
		}
		return decrypted;
	}

	public synchronized boolean hasScopedSecret(@NonNull String scope, long hostId) {
		if (hostId <= 0 || scope.trim().isEmpty()) {
			return false;
		}
		return mPreferences.contains(getScopedKey(scope, hostId));
	}

	public synchronized void clearScopedSecret(@NonNull String scope, long hostId) {
		if (hostId <= 0 || scope.trim().isEmpty()) {
			return;
		}
		mPreferences.edit().remove(getScopedKey(scope, hostId)).apply();
	}

	@NonNull
	public synchronized Map<Long, String> exportPlaintextPasswords(@NonNull List<Long> hostIds) {
		HashMap<Long, String> exported = new HashMap<Long, String>();
		for (Long hostIdObject : hostIds) {
			if (hostIdObject == null) {
				continue;
			}
			long hostId = hostIdObject;
			if (hostId <= 0) {
				continue;
			}
			String password = loadPassword(hostId);
			if (password != null && !password.isEmpty()) {
				exported.put(hostId, password);
			}
		}
		return exported;
	}

	private String getHostKey(long hostId) {
		return PREFS_KEY_PREFIX + hostId;
	}

	private String getScopedKey(@NonNull String scope, long hostId) {
		return PREFS_SCOPED_KEY_PREFIX + sanitizeScope(scope) + "_" + hostId;
	}

	@NonNull
	private static String sanitizeScope(@NonNull String scope) {
		return scope.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9_\\-]", "_");
	}

	@Nullable
	private String encrypt(@NonNull String cleartext) {
		String keystorePayload = encryptWithKeystore(cleartext);
		if (keystorePayload != null) {
			return keystorePayload;
		}
		return encryptWithFallback(cleartext);
	}

	@Nullable
	private String decrypt(@NonNull String payload) {
		if (payload.startsWith(FORMAT_KEYSTORE + ":")) {
			return decryptWithKeystore(payload);
		}
		if (payload.startsWith(FORMAT_FALLBACK + ":")) {
			return decryptWithFallback(payload);
		}
		return null;
	}

	@Nullable
	private String encryptWithKeystore(@NonNull String cleartext) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return null;
		}

		try {
			return Api23Keystore.encrypt(cleartext);
		} catch (Exception e) {
			Log.w(TAG, "Keystore encryption unavailable, falling back", e);
			return null;
		}
	}

	@Nullable
	private String decryptWithKeystore(@NonNull String payload) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return null;
		}

		try {
			String[] parts = payload.split(":", 3);
			if (parts.length != 3) {
				return null;
			}

			byte[] iv = decode(parts[1]);
			byte[] ciphertext = decode(parts[2]);
			return Api23Keystore.decrypt(iv, ciphertext);
		} catch (Exception e) {
			Log.w(TAG, "Keystore decryption failed", e);
			return null;
		}
	}

	@Nullable
	private String encryptWithFallback(@NonNull String cleartext) {
		try {
			byte[] salt = new byte[FALLBACK_SALT_BYTES];
			mSecureRandom.nextBytes(salt);
			byte[] ciphertext = Encryptor.encrypt(salt, FALLBACK_ITERATIONS, getFallbackSecret(), toUtf8(cleartext));
			return FORMAT_FALLBACK + ":" + encode(salt) + ":" + encode(ciphertext);
		} catch (Exception e) {
			Log.w(TAG, "Fallback encryption failed", e);
			return null;
		}
	}

	@Nullable
	private String decryptWithFallback(@NonNull String payload) {
		try {
			String[] parts = payload.split(":", 3);
			if (parts.length != 3) {
				return null;
			}
			byte[] salt = decode(parts[1]);
			byte[] ciphertext = decode(parts[2]);
			byte[] cleartext = Encryptor.decrypt(salt, FALLBACK_ITERATIONS, getFallbackSecret(), ciphertext);
			return fromUtf8(cleartext);
		} catch (Exception e) {
			Log.w(TAG, "Fallback decryption failed", e);
			return null;
		}
	}

	@NonNull
	private String getFallbackSecret() {
		String androidId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
		if (androidId == null || androidId.isEmpty()) {
			androidId = "unknown-device";
		}
		return mContext.getPackageName() + ":" + androidId;
	}

	private static byte[] toUtf8(@NonNull String value) throws Exception {
		return value.getBytes("UTF-8");
	}

	@NonNull
	private static String fromUtf8(byte[] value) throws Exception {
		return new String(value, "UTF-8");
	}

	@NonNull
	private static String encode(byte[] value) {
		return Base64.encodeToString(value, Base64.NO_WRAP);
	}

	private static byte[] decode(@NonNull String value) {
		return Base64.decode(value, Base64.NO_WRAP);
	}

	@TargetApi(Build.VERSION_CODES.M)
	private static final class Api23Keystore {
		private static String encrypt(@NonNull String cleartext) throws Exception {
			Cipher cipher = Cipher.getInstance(AES_MODE);
			cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
			byte[] ciphertext = cipher.doFinal(toUtf8(cleartext));
			return FORMAT_KEYSTORE + ":" + encode(cipher.getIV()) + ":" + encode(ciphertext);
		}

		@NonNull
		private static String decrypt(byte[] iv, byte[] ciphertext) throws Exception {
			Cipher cipher = Cipher.getInstance(AES_MODE);
			cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
			return fromUtf8(cipher.doFinal(ciphertext));
		}

		private static SecretKey getOrCreateKey() throws Exception {
			KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
			keyStore.load(null);

			if (!keyStore.containsAlias(KEY_ALIAS)) {
				KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
				KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
						KEY_ALIAS,
						KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
						.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
						.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
						.setRandomizedEncryptionRequired(true)
						.setUserAuthenticationRequired(false)
						.build();
				keyGenerator.init(spec);
				keyGenerator.generateKey();
			}

			return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
		}
	}
}
