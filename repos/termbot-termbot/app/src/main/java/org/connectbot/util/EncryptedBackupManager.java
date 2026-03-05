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
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.connectbot.BuildConfig;
import org.connectbot.R;
import org.connectbot.bean.HostBean;
import org.connectbot.bean.HostGroupBean;
import org.connectbot.bean.PubkeyBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class EncryptedBackupManager {
	private static final String TAG = "CB.EncryptedBackup";

	private static final String BACKUP_DIR = "backup";
	private static final String BACKUP_FILENAME_PREFIX = "termbot-backup-";
	private static final String BACKUP_FILENAME_SUFFIX = ".tbbak";
	private static final String UTF_8 = "UTF-8";

	private static final String BACKUP_FORMAT = "termbot-backup-v1";
	private static final String CRYPTO_FORMAT = "termbot-backup-crypto-v1";
	private static final int PBKDF2_DEFAULT_ITERATIONS = 210000;
	private static final int KEY_SIZE_BITS = 256;
	private static final int SALT_SIZE = 16;
	private static final int NONCE_SIZE = 12;

	private static final String KDF_PBKDF2_SHA256 = "PBKDF2WithHmacSHA256";
	private static final String KDF_PBKDF2_SHA1 = "PBKDF2WithHmacSHA1";
	private static final String CIPHER_AES_GCM = "AES/GCM/NoPadding";
	private static final int GCM_TAG_BITS = 128;

	private static final String MARKER_BACKUP_EXPORT = "BACKUP_EXPORT";
	private static final String MARKER_BACKUP_IMPORT = "BACKUP_IMPORT";

	private EncryptedBackupManager() {
	}

	public static final class ImportResult {
		public final int hostsImported;
		public final int pubkeysImported;
		public final int passwordsImported;

		ImportResult(int hostsImported, int pubkeysImported, int passwordsImported) {
			this.hostsImported = hostsImported;
			this.pubkeysImported = pubkeysImported;
			this.passwordsImported = passwordsImported;
		}
	}

	@NonNull
	public static File exportBackup(@NonNull Context context, @NonNull String password) throws IOException {
		validatePassword(password);
		Context appContext = context.getApplicationContext();
		SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_BACKUP_EXPORT, "start");

		try {
			JSONObject plainBackup = buildPlainBackupJson(appContext);
			int groupsCount = getJsonArrayLength(plainBackup.optJSONArray("groups"));
			int hostsCount = getJsonArrayLength(plainBackup.optJSONArray("hosts"));
			int pubkeysCount = getJsonArrayLength(plainBackup.optJSONArray("pubkeys"));
			int passwordsCount = getJsonArrayLength(plainBackup.optJSONArray("saved_passwords"));
			SecurityKeyDebugLog.logFlow(
					appContext,
					TAG,
					MARKER_BACKUP_EXPORT,
					"counts groups=" + groupsCount
							+ " hosts=" + hostsCount
							+ " pubkeys=" + pubkeysCount
							+ " passwords=" + passwordsCount);
			byte[] plaintext = plainBackup.toString().getBytes(UTF_8);
			String encryptedPayload = encryptPayload(plaintext, password);

			File backupDir = appContext.getExternalFilesDir(BACKUP_DIR);
			if (backupDir == null) {
				throw new IOException("Backup directory is unavailable");
			}
			if (!backupDir.exists() && !backupDir.mkdirs()) {
				throw new IOException("Unable to create backup directory");
			}

			String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
			File backupFile = new File(backupDir, BACKUP_FILENAME_PREFIX + timestamp + BACKUP_FILENAME_SUFFIX);

			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(backupFile, false), UTF_8))) {
				writer.write(encryptedPayload);
			}

			SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_BACKUP_EXPORT, "success");
			return backupFile;
		} catch (IOException e) {
			SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_BACKUP_EXPORT, "failed_io=" + e.getClass().getSimpleName());
			throw e;
		} catch (Exception e) {
			SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_BACKUP_EXPORT, "failed=" + e.getClass().getSimpleName());
			throw new IOException("Unable to export encrypted backup", e);
		}
	}

	@NonNull
	public static Intent createShareIntent(@NonNull Context context, @NonNull File backupFile) {
		Uri backupUri = FileProvider.getUriForFile(
				context,
				BuildConfig.APPLICATION_ID + ".fileprovider",
				backupFile);

		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("application/octet-stream");
		shareIntent.putExtra(Intent.EXTRA_STREAM, backupUri);
		shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		return Intent.createChooser(shareIntent, context.getString(R.string.backup_share_title));
	}

	@NonNull
	public static ImportResult importBackup(@NonNull Context context, @NonNull Uri backupUri, @NonNull String password)
			throws IOException {
		validatePassword(password);
		Context appContext = context.getApplicationContext();
		SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_BACKUP_IMPORT, "start");

		try {
			byte[] encryptedPayload = readAllBytes(appContext, backupUri);
			byte[] plainPayload = decryptPayload(new String(encryptedPayload, UTF_8), password);
			JSONObject backupJson = new JSONObject(new String(plainPayload, UTF_8));

			if (!BACKUP_FORMAT.equals(backupJson.optString("format"))) {
				throw new IOException("Unsupported backup format");
			}

			PubkeyDatabase pubkeyDatabase = PubkeyDatabase.get(appContext);
			HostDatabase hostDatabase = HostDatabase.get(appContext);
			SavedPasswordStore savedPasswordStore = SavedPasswordStore.get(appContext);

			GroupImportState groupImportState = importGroups(backupJson.optJSONArray("groups"), hostDatabase);
			Map<Long, Long> pubkeyIdMap = importPubkeys(backupJson.optJSONArray("pubkeys"), pubkeyDatabase);
			HostImportState hostImportState = importHosts(
					backupJson.optJSONArray("hosts"),
					hostDatabase,
					pubkeyIdMap,
					groupImportState.oldToNewGroupIdMap);
			int importedPasswords = importSavedPasswords(
					backupJson.optJSONArray("saved_passwords"),
					hostImportState.oldToNewHostIdMap,
					savedPasswordStore);

			SecurityKeyDebugLog.logFlow(
					appContext,
					TAG,
					MARKER_BACKUP_IMPORT,
					"success groups=" + groupImportState.importedGroupCount
							+ " mappedGroups=" + groupImportState.oldToNewGroupIdMap.size()
							+ " hosts=" + hostImportState.importedHostCount
							+ " pubkeys=" + pubkeyIdMap.size()
							+ " passwords=" + importedPasswords);

			return new ImportResult(hostImportState.importedHostCount, pubkeyIdMap.size(), importedPasswords);
		} catch (IOException e) {
			SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_BACKUP_IMPORT, "failed_io=" + e.getClass().getSimpleName());
			throw e;
		} catch (GeneralSecurityException e) {
			SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_BACKUP_IMPORT, "failed_crypto=" + e.getClass().getSimpleName());
			throw new IOException("Incorrect password or invalid encrypted backup", e);
		} catch (JSONException e) {
			SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_BACKUP_IMPORT, "failed_json=" + e.getClass().getSimpleName());
			throw new IOException("Backup file is not valid JSON", e);
		} catch (Exception e) {
			SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_BACKUP_IMPORT, "failed=" + e.getClass().getSimpleName());
			throw new IOException("Unable to import backup", e);
		}
	}

	@NonNull
	private static JSONObject buildPlainBackupJson(@NonNull Context context) throws JSONException {
		HostDatabase hostDatabase = HostDatabase.get(context);
		PubkeyDatabase pubkeyDatabase = PubkeyDatabase.get(context);
		SavedPasswordStore savedPasswordStore = SavedPasswordStore.get(context);

		List<HostBean> hosts = hostDatabase.getHosts(false);
		List<HostGroupBean> groups = hostDatabase.getHostGroups();
		List<PubkeyBean> pubkeys = pubkeyDatabase.allPubkeys();

		ArrayList<Long> hostIds = new ArrayList<Long>(hosts.size());
		for (HostBean host : hosts) {
			hostIds.add(host.getId());
		}
		Map<Long, String> savedPasswords = savedPasswordStore.exportPlaintextPasswords(hostIds);

		JSONObject backup = new JSONObject();
		backup.put("format", BACKUP_FORMAT);
		backup.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US).format(new Date()));
		backup.put("app_version", BuildConfig.VERSION_NAME);
		backup.put("app_flavor", BuildConfig.FLAVOR);

		JSONArray groupsArray = new JSONArray();
		for (HostGroupBean group : groups) {
			groupsArray.put(groupToJson(group));
		}
		backup.put("groups", groupsArray);

		JSONArray hostsArray = new JSONArray();
		for (HostBean host : hosts) {
			hostsArray.put(hostToJson(host));
		}
		backup.put("hosts", hostsArray);

		JSONArray pubkeysArray = new JSONArray();
		for (PubkeyBean pubkey : pubkeys) {
			pubkeysArray.put(pubkeyToJson(pubkey));
		}
		backup.put("pubkeys", pubkeysArray);

		JSONArray passwordsArray = new JSONArray();
		for (Map.Entry<Long, String> entry : savedPasswords.entrySet()) {
			JSONObject passwordEntry = new JSONObject();
			passwordEntry.put("host_id", entry.getKey());
			passwordEntry.put("password", entry.getValue());
			passwordsArray.put(passwordEntry);
		}
		backup.put("saved_passwords", passwordsArray);

		return backup;
	}

	@NonNull
	private static JSONObject hostToJson(@NonNull HostBean host) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("id", host.getId());
		obj.put("nickname", nullToEmpty(host.getNickname()));
		obj.put("protocol", nullToEmpty(host.getProtocol()));
		obj.put("username", nullToEmpty(host.getUsername()));
		obj.put("hostname", nullToEmpty(host.getHostname()));
		obj.put("port", host.getPort());
		obj.put("last_connect", host.getLastConnect());
		obj.put("color", nullToEmpty(host.getColor()));
		obj.put("use_keys", host.getUseKeys());
		obj.put("use_auth_agent", nullToEmpty(host.getUseAuthAgent()));
		obj.put("post_login", nullToEmpty(host.getPostLogin()));
		obj.put("pubkey_id", host.getPubkeyId());
		obj.put("want_session", host.getWantSession());
		obj.put("del_key", nullToEmpty(host.getDelKey()));
		obj.put("font_size", host.getFontSize());
		obj.put("compression", host.getCompression());
		obj.put("encoding", nullToEmpty(host.getEncoding()));
		obj.put("stay_connected", host.getStayConnected());
		obj.put("quick_disconnect", host.getQuickDisconnect());
		obj.put("remember_password", host.getRememberPassword());
		obj.put("jump_host_id", host.getJumpHostId());
		obj.put("group_id", host.getGroupId());
		return obj;
	}

	@NonNull
	private static HostBean hostFromJson(@NonNull JSONObject obj) {
		HostBean host = new HostBean();
		host.setId(-1);
		host.setNickname(emptyToNull(obj.optString("nickname")));
		String protocol = emptyToNull(obj.optString("protocol"));
		host.setProtocol(protocol != null ? protocol : "ssh");
		host.setUsername(emptyToNull(obj.optString("username")));
		host.setHostname(emptyToNull(obj.optString("hostname")));
		host.setPort(obj.optInt("port", 22));
		host.setLastConnect(obj.optLong("last_connect", -1));
		host.setColor(emptyToNull(obj.optString("color")));
		host.setUseKeys(obj.optBoolean("use_keys", true));
		String useAuthAgent = emptyToNull(obj.optString("use_auth_agent"));
		host.setUseAuthAgent(useAuthAgent != null ? useAuthAgent : HostDatabase.AUTHAGENT_NO);
		host.setPostLogin(emptyToNull(obj.optString("post_login")));
		host.setPubkeyId(obj.optLong("pubkey_id", HostDatabase.PUBKEYID_ANY));
		host.setWantSession(obj.optBoolean("want_session", true));
		String delKey = emptyToNull(obj.optString("del_key"));
		host.setDelKey(delKey != null ? delKey : HostDatabase.DELKEY_DEL);
		host.setFontSize(obj.optInt("font_size", HostBean.DEFAULT_FONT_SIZE));
		host.setCompression(obj.optBoolean("compression", false));
		String encoding = emptyToNull(obj.optString("encoding"));
		host.setEncoding(encoding != null ? encoding : HostDatabase.ENCODING_DEFAULT);
		host.setStayConnected(obj.optBoolean("stay_connected", false));
		host.setQuickDisconnect(obj.optBoolean("quick_disconnect", false));
		host.setRememberPassword(obj.optBoolean("remember_password", false));
		host.setJumpHostId(obj.optLong("jump_host_id", -1));
		host.setGroupId(obj.optLong("group_id", HostDatabase.HOST_GROUP_NONE));
		return host;
	}

	@NonNull
	private static JSONObject groupToJson(@NonNull HostGroupBean group) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("id", group.getId());
		obj.put("name", nullToEmpty(group.getName()));
		return obj;
	}

	@Nullable
	private static HostGroupBean groupFromJson(@NonNull JSONObject obj) {
		String name = emptyToNull(obj.optString("name"));
		if (name == null) {
			return null;
		}

		HostGroupBean group = new HostGroupBean();
		group.setId(-1);
		group.setName(name);
		return group;
	}

	@NonNull
	private static JSONObject pubkeyToJson(@NonNull PubkeyBean pubkey) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("id", pubkey.getId());
		obj.put("nickname", nullToEmpty(pubkey.getNickname()));
		obj.put("type", nullToEmpty(pubkey.getType()));
		obj.put("private_key", encodeBase64(pubkey.getPrivateKey()));
		obj.put("public_key", encodeBase64(pubkey.getPublicKey()));
		obj.put("encrypted", pubkey.isEncrypted());
		obj.put("startup", pubkey.isStartup());
		obj.put("confirm_use", pubkey.isConfirmUse());
		obj.put("lifetime", pubkey.getLifetime());
		obj.put("security_key", pubkey.isSecurityKey());
		return obj;
	}

	@NonNull
	private static PubkeyBean pubkeyFromJson(@NonNull JSONObject obj) {
		PubkeyBean pubkey = new PubkeyBean();
		pubkey.setId(-1);
		pubkey.setNickname(emptyToNull(obj.optString("nickname")));
		pubkey.setType(emptyToNull(obj.optString("type")));
		pubkey.setPrivateKey(decodeBase64(obj.optString("private_key")));
		pubkey.setPublicKey(decodeBase64(obj.optString("public_key")));
		pubkey.setEncrypted(obj.optBoolean("encrypted", false));
		pubkey.setStartup(obj.optBoolean("startup", false));
		pubkey.setConfirmUse(obj.optBoolean("confirm_use", false));
		pubkey.setLifetime(obj.optInt("lifetime", 0));
		pubkey.setSecurityKey(obj.optBoolean("security_key", false));
		return pubkey;
	}

	@NonNull
	private static Map<Long, Long> importPubkeys(@Nullable JSONArray pubkeysArray, @NonNull PubkeyDatabase pubkeyDatabase) {
		HashMap<Long, Long> oldToNew = new HashMap<Long, Long>();
		if (pubkeysArray == null) {
			return oldToNew;
		}

		Map<String, PubkeyBean> existingByPublicKey = new HashMap<String, PubkeyBean>();
		for (PubkeyBean existingPubkey : pubkeyDatabase.allPubkeys()) {
			existingByPublicKey.put(buildPubkeyIdentity(existingPubkey), existingPubkey);
		}

		for (int i = 0; i < pubkeysArray.length(); i++) {
			JSONObject pubkeyJson = pubkeysArray.optJSONObject(i);
			if (pubkeyJson == null) {
				continue;
			}

			long oldId = pubkeyJson.optLong("id", -1);
			PubkeyBean importedPubkey = pubkeyFromJson(pubkeyJson);
			if (importedPubkey.getType() == null || importedPubkey.getPublicKey() == null) {
				continue;
			}

			String identity = buildPubkeyIdentity(importedPubkey);
			PubkeyBean existing = existingByPublicKey.get(identity);
			if (existing != null) {
				importedPubkey.setId(existing.getId());
			}
			PubkeyBean savedPubkey = pubkeyDatabase.savePubkey(importedPubkey);
			existingByPublicKey.put(identity, savedPubkey);

			if (oldId > 0 && savedPubkey.getId() > 0) {
				oldToNew.put(oldId, savedPubkey.getId());
			}
		}
		return oldToNew;
	}

	@NonNull
	private static String buildPubkeyIdentity(@NonNull PubkeyBean pubkey) {
		return nullToEmpty(pubkey.getType()) + ":" + encodeBase64(pubkey.getPublicKey());
	}

	@NonNull
	private static GroupImportState importGroups(
			@Nullable JSONArray groupsArray,
			@NonNull HostDatabase hostDatabase) {
		GroupImportState state = new GroupImportState();
		if (groupsArray == null) {
			return state;
		}

		for (int i = 0; i < groupsArray.length(); i++) {
			JSONObject groupJson = groupsArray.optJSONObject(i);
			if (groupJson == null) {
				continue;
			}

			long oldId = groupJson.optLong("id", HostDatabase.HOST_GROUP_NONE);
			HostGroupBean importedGroup = groupFromJson(groupJson);
			if (importedGroup == null) {
				continue;
			}

			HostGroupBean existing = hostDatabase.findHostGroupByName(importedGroup.getName());
			HostGroupBean savedGroup = existing != null ? existing : hostDatabase.saveHostGroup(importedGroup);
			if (savedGroup == null || savedGroup.getId() <= 0) {
				continue;
			}

			state.importedGroupCount++;
			if (oldId > 0) {
				state.oldToNewGroupIdMap.put(oldId, savedGroup.getId());
			}
		}

		return state;
	}

	@NonNull
	private static HostImportState importHosts(
			@Nullable JSONArray hostsArray,
			@NonNull HostDatabase hostDatabase,
			@NonNull Map<Long, Long> pubkeyIdMap,
			@NonNull Map<Long, Long> groupIdMap) {
		HostImportState state = new HostImportState();
		if (hostsArray == null) {
			return state;
		}

		ArrayList<HostJumpBinding> jumpBindings = new ArrayList<HostJumpBinding>();
		for (int i = 0; i < hostsArray.length(); i++) {
			JSONObject hostJson = hostsArray.optJSONObject(i);
			if (hostJson == null) {
				continue;
			}

			long oldId = hostJson.optLong("id", -1);
			long oldJumpHostId = hostJson.optLong("jump_host_id", -1);
			HostBean importedHost = hostFromJson(hostJson);
			if (importedHost.getProtocol() == null || importedHost.getHostname() == null || importedHost.getPort() <= 0) {
				continue;
			}

			long importedPubkeyId = importedHost.getPubkeyId();
			if (importedPubkeyId > 0) {
				Long remappedPubkeyId = pubkeyIdMap.get(importedPubkeyId);
				importedHost.setPubkeyId(remappedPubkeyId != null ? remappedPubkeyId : HostDatabase.PUBKEYID_ANY);
			}
			long importedGroupId = importedHost.getGroupId();
			if (importedGroupId > 0) {
				Long remappedGroupId = groupIdMap.get(importedGroupId);
				importedHost.setGroupId(remappedGroupId != null ? remappedGroupId : HostDatabase.HOST_GROUP_NONE);
			} else {
				importedHost.setGroupId(HostDatabase.HOST_GROUP_NONE);
			}

			HostBean existingHost = findExistingHost(hostDatabase, importedHost);
			importedHost.setId(existingHost != null ? existingHost.getId() : -1);
			importedHost.setJumpHostId(-1);
			HostBean savedHost = hostDatabase.saveHost(importedHost);
			state.importedHostCount++;

			if (oldId > 0 && savedHost.getId() > 0) {
				state.oldToNewHostIdMap.put(oldId, savedHost.getId());
			}
			jumpBindings.add(new HostJumpBinding(savedHost, oldJumpHostId));
		}

		for (HostJumpBinding binding : jumpBindings) {
			if (binding.oldJumpHostId <= 0) {
				continue;
			}
			Long mappedJumpHostId = state.oldToNewHostIdMap.get(binding.oldJumpHostId);
			if (mappedJumpHostId == null || mappedJumpHostId <= 0 || mappedJumpHostId == binding.host.getId()) {
				continue;
			}

			binding.host.setJumpHostId(mappedJumpHostId);
			hostDatabase.saveHost(binding.host);
		}
		return state;
	}

	@Nullable
	private static HostBean findExistingHost(@NonNull HostDatabase hostDatabase, @NonNull HostBean host) {
		HashMap<String, String> selection = new HashMap<String, String>();
		selection.put(HostDatabase.FIELD_HOST_PROTOCOL, host.getProtocol());
		selection.put(HostDatabase.FIELD_HOST_HOSTNAME, host.getHostname());
		selection.put(HostDatabase.FIELD_HOST_PORT, String.valueOf(host.getPort()));
		selection.put(HostDatabase.FIELD_HOST_USERNAME, host.getUsername());
		return hostDatabase.findHost(selection);
	}

	private static int importSavedPasswords(
			@Nullable JSONArray passwordsArray,
			@NonNull Map<Long, Long> oldToNewHostIdMap,
			@NonNull SavedPasswordStore savedPasswordStore) {
		if (passwordsArray == null) {
			return 0;
		}

		int importedPasswords = 0;
		for (int i = 0; i < passwordsArray.length(); i++) {
			JSONObject passwordJson = passwordsArray.optJSONObject(i);
			if (passwordJson == null) {
				continue;
			}

			long oldHostId = passwordJson.optLong("host_id", -1);
			String password = passwordJson.optString("password", null);
			if (oldHostId <= 0 || password == null || password.isEmpty()) {
				continue;
			}

			Long newHostId = oldToNewHostIdMap.get(oldHostId);
			if (newHostId == null || newHostId <= 0) {
				continue;
			}

			savedPasswordStore.savePassword(newHostId, password);
			importedPasswords++;
		}

		return importedPasswords;
	}

	@NonNull
	private static String encryptPayload(byte[] plaintext, @NonNull String password)
			throws GeneralSecurityException, JSONException, IOException {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			throw new IOException("Encrypted backup requires Android 5.0 or newer");
		}

		SecureRandom secureRandom = new SecureRandom();
		byte[] salt = new byte[SALT_SIZE];
		byte[] nonce = new byte[NONCE_SIZE];
		secureRandom.nextBytes(salt);
		secureRandom.nextBytes(nonce);

		DerivedKey derivedKey = deriveKey(password, salt, PBKDF2_DEFAULT_ITERATIONS, null);
		byte[] ciphertext = Api21AesGcm.encrypt(plaintext, derivedKey.keyBytes, nonce);

		JSONObject cryptoContainer = new JSONObject();
		cryptoContainer.put("format", CRYPTO_FORMAT);
		cryptoContainer.put("kdf", derivedKey.kdfAlgorithm);
		cryptoContainer.put("iterations", PBKDF2_DEFAULT_ITERATIONS);
		cryptoContainer.put("salt", encodeBase64(salt));
		cryptoContainer.put("nonce", encodeBase64(nonce));
		cryptoContainer.put("ciphertext", encodeBase64(ciphertext));
		return cryptoContainer.toString();
	}

	@NonNull
	private static byte[] decryptPayload(@NonNull String encryptedPayload, @NonNull String password)
			throws GeneralSecurityException, JSONException, IOException {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			throw new IOException("Encrypted backup requires Android 5.0 or newer");
		}

		JSONObject cryptoContainer = new JSONObject(encryptedPayload);
		if (!CRYPTO_FORMAT.equals(cryptoContainer.optString("format"))) {
			throw new IOException("Unsupported encrypted backup format");
		}

		String kdf = cryptoContainer.optString("kdf", KDF_PBKDF2_SHA256);
		int iterations = cryptoContainer.optInt("iterations", 0);
		if (iterations < 10000) {
			throw new IOException("Encrypted backup KDF iterations are invalid");
		}

		byte[] salt = decodeBase64(cryptoContainer.optString("salt"));
		byte[] nonce = decodeBase64(cryptoContainer.optString("nonce"));
		byte[] ciphertext = decodeBase64(cryptoContainer.optString("ciphertext"));
		if (salt == null || nonce == null || ciphertext == null) {
			throw new IOException("Encrypted backup payload is invalid");
		}

		DerivedKey derivedKey = deriveKey(password, salt, iterations, kdf);
		return Api21AesGcm.decrypt(ciphertext, derivedKey.keyBytes, nonce);
	}

	@NonNull
	private static DerivedKey deriveKey(
			@NonNull String password,
			byte[] salt,
			int iterations,
			@Nullable String preferredKdf) throws GeneralSecurityException {
		String[] candidates;
		if (preferredKdf != null && !preferredKdf.isEmpty()) {
			candidates = new String[] {preferredKdf, KDF_PBKDF2_SHA256, KDF_PBKDF2_SHA1};
		} else {
			candidates = new String[] {KDF_PBKDF2_SHA256, KDF_PBKDF2_SHA1};
		}

		char[] passwordChars = password.toCharArray();
		try {
			for (String candidate : candidates) {
				if (candidate == null || candidate.isEmpty()) {
					continue;
				}
				try {
					SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(candidate);
					KeySpec keySpec = new PBEKeySpec(passwordChars, salt, iterations, KEY_SIZE_BITS);
					byte[] keyBytes = secretKeyFactory.generateSecret(keySpec).getEncoded();
					return new DerivedKey(candidate, keyBytes);
				} catch (NoSuchAlgorithmException ignored) {
				}
			}
		} finally {
			Arrays.fill(passwordChars, '\0');
		}

		throw new GeneralSecurityException("No supported PBKDF2 algorithm is available");
	}

	private static void validatePassword(@NonNull String password) throws IOException {
		if (password.trim().isEmpty()) {
			throw new IOException("Backup password cannot be empty");
		}
	}

	@NonNull
	private static byte[] readAllBytes(@NonNull Context context, @NonNull Uri uri) throws IOException {
		try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
			if (inputStream == null) {
				throw new IOException("Unable to open backup file");
			}
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, read);
			}
			return outputStream.toByteArray();
		}
	}

	@NonNull
	public static String readDisplayName(@NonNull Uri uri) {
		String path = uri.getLastPathSegment();
		return path == null || path.isEmpty() ? "backup file" : path;
	}

	@Nullable
	private static byte[] decodeBase64(@Nullable String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		try {
			return Base64.decode(value, Base64.NO_WRAP);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@NonNull
	private static String encodeBase64(@Nullable byte[] value) {
		if (value == null || value.length == 0) {
			return "";
		}
		return Base64.encodeToString(value, Base64.NO_WRAP);
	}

	@NonNull
	private static String nullToEmpty(@Nullable String value) {
		return value == null ? "" : value;
	}

	@Nullable
	private static String emptyToNull(@Nullable String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		return value;
	}

	private static int getJsonArrayLength(@Nullable JSONArray array) {
		return array == null ? 0 : array.length();
	}

	private static final class HostJumpBinding {
		private final HostBean host;
		private final long oldJumpHostId;

		HostJumpBinding(HostBean host, long oldJumpHostId) {
			this.host = host;
			this.oldJumpHostId = oldJumpHostId;
		}
	}

	private static final class HostImportState {
		private final HashMap<Long, Long> oldToNewHostIdMap = new HashMap<Long, Long>();
		private int importedHostCount = 0;
	}

	private static final class GroupImportState {
		private final HashMap<Long, Long> oldToNewGroupIdMap = new HashMap<Long, Long>();
		private int importedGroupCount = 0;
	}

	private static final class DerivedKey {
		private final String kdfAlgorithm;
		private final byte[] keyBytes;

		DerivedKey(String kdfAlgorithm, byte[] keyBytes) {
			this.kdfAlgorithm = kdfAlgorithm;
			this.keyBytes = keyBytes;
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static final class Api21AesGcm {
		private static byte[] encrypt(byte[] plaintext, byte[] keyBytes, byte[] nonce) throws GeneralSecurityException {
			Cipher cipher = Cipher.getInstance(CIPHER_AES_GCM);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
			return cipher.doFinal(plaintext);
		}

		private static byte[] decrypt(byte[] ciphertext, byte[] keyBytes, byte[] nonce) throws GeneralSecurityException {
			Cipher cipher = Cipher.getInstance(CIPHER_AES_GCM);
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
			return cipher.doFinal(ciphertext);
		}
	}
}
