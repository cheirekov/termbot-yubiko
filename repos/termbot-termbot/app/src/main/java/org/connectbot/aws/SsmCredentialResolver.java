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
import java.util.Locale;
import org.connectbot.bean.HostBean;
import org.connectbot.util.SavedPasswordStore;

/**
 * Resolves and persists SSM credentials without exposing secret values in call-sites.
 */
public final class SsmCredentialResolver {
	public static final String CREDENTIAL_MODE_LONG_LIVED_KEY = "long_lived_key";
	public static final String CREDENTIAL_MODE_SESSION_TOKEN = "session_token";
	public static final String CREDENTIAL_ENHANCEMENT_NONE = "none";
	public static final String CREDENTIAL_ENHANCEMENT_GET_SESSION_TOKEN = "get_session_token";
	public static final String CREDENTIAL_ENHANCEMENT_ASSUME_ROLE = "assume_role";

	public static final class MissingSessionTokenException extends IOException {
		private static final long serialVersionUID = 1L;

		public MissingSessionTokenException() {
			super("session_token_required");
		}
	}

	public static final class MissingMfaCodeException extends IOException {
		private static final long serialVersionUID = 1L;

		public MissingMfaCodeException() {
			super("mfa_code_required");
		}
	}

	public interface PromptDelegate {
		@Nullable
		String requestSecretAccessKey();

		@Nullable
		String requestSessionToken();

		@Nullable
		String requestMfaCode(@Nullable String hint);
	}

	public interface SessionCredentialEnhancer {
		@NonNull
		String getEnhancementMode(@NonNull AwsCredentials baseCredentials);

		@Nullable
		String getMfaPromptHint(@NonNull AwsCredentials baseCredentials);

		boolean requiresMfaCode(@NonNull AwsCredentials baseCredentials);

		@Nullable
		AwsCredentials enhance(@NonNull AwsCredentials baseCredentials,
				@Nullable String mfaCode) throws IOException;
	}

	public static final class Resolution {
		private final AwsCredentials runtimeCredentials;
		private final AwsCredentials persistedCredentials;
		private final String credentialMode;
		private final String secretSource;
		private final String sessionTokenSource;
		private final String enhancementMode;
		private final boolean mfaPrompted;
		private final boolean credentialEnhanced;

		private Resolution(@NonNull AwsCredentials runtimeCredentials,
				@NonNull AwsCredentials persistedCredentials,
				@NonNull String credentialMode,
				@NonNull String secretSource,
				@NonNull String sessionTokenSource,
				@NonNull String enhancementMode,
				boolean mfaPrompted,
				boolean credentialEnhanced) {
			this.runtimeCredentials = runtimeCredentials;
			this.persistedCredentials = persistedCredentials;
			this.credentialMode = credentialMode;
			this.secretSource = secretSource;
			this.sessionTokenSource = sessionTokenSource;
			this.enhancementMode = enhancementMode;
			this.mfaPrompted = mfaPrompted;
			this.credentialEnhanced = credentialEnhanced;
		}

		@NonNull
		public AwsCredentials getRuntimeCredentials() {
			return runtimeCredentials;
		}

		@NonNull
		public AwsCredentials getPersistedCredentials() {
			return persistedCredentials;
		}

		@NonNull
		public String getCredentialMode() {
			return credentialMode;
		}

		@NonNull
		public String getSecretSource() {
			return secretSource;
		}

		@NonNull
		public String getSessionTokenSource() {
			return sessionTokenSource;
		}

		@NonNull
		public String getEnhancementMode() {
			return enhancementMode;
		}

		public boolean isMfaPrompted() {
			return mfaPrompted;
		}

		public boolean isCredentialEnhanced() {
			return credentialEnhanced;
		}
	}

	private final SavedPasswordStore savedPasswordStore;
	@Nullable
	private final SessionCredentialEnhancer sessionCredentialEnhancer;

	public SsmCredentialResolver(@NonNull SavedPasswordStore savedPasswordStore,
			@Nullable SessionCredentialEnhancer sessionCredentialEnhancer) {
		this.savedPasswordStore = savedPasswordStore;
		this.sessionCredentialEnhancer = sessionCredentialEnhancer;
	}

	@Nullable
	public Resolution resolve(@NonNull HostBean host, @NonNull PromptDelegate promptDelegate)
			throws IOException {
		String accessKeyId = safeTrim(host.getUsername());
		if (accessKeyId == null) {
			return null;
		}

		long hostId = host.getId();
		boolean canReadPersisted = host.getRememberPassword() && hostId > 0;
		String secretSource = "none";
		String sessionTokenSource = "none";
		String secretAccessKey = null;
		String sessionToken = null;
		boolean sessionTokenExpected = looksLikeSessionCredentials(accessKeyId);

		if (canReadPersisted) {
			secretAccessKey = safeTrim(savedPasswordStore.loadScopedSecret(
					SavedPasswordStore.SCOPE_SSM_SECRET_ACCESS_KEY, hostId));
			if (secretAccessKey != null) {
				secretSource = "saved_scoped";
			}

			if (secretAccessKey == null) {
				secretAccessKey = safeTrim(savedPasswordStore.loadPassword(hostId));
				if (secretAccessKey != null) {
					secretSource = "saved_legacy";
				}
			}

			sessionToken = safeTrim(savedPasswordStore.loadScopedSecret(
					SavedPasswordStore.SCOPE_SSM_SESSION_TOKEN, hostId));
			if (sessionToken != null) {
				sessionTokenSource = "saved_scoped";
			}
		}

		if (secretAccessKey == null) {
			secretAccessKey = safeTrim(promptDelegate.requestSecretAccessKey());
			if (secretAccessKey == null) {
				return null;
			}
			secretSource = "prompt";
		}

		if (sessionToken == null && sessionTokenExpected) {
			sessionToken = safeTrim(promptDelegate.requestSessionToken());
			if (sessionToken != null) {
				sessionTokenSource = "prompt";
			} else {
				throw new MissingSessionTokenException();
			}
		}

		AwsCredentials persistedCredentials = new AwsCredentials(accessKeyId, secretAccessKey,
				sessionToken);
		AwsCredentials runtimeCredentials = persistedCredentials;
		String enhancementMode = CREDENTIAL_ENHANCEMENT_NONE;
		boolean mfaPrompted = false;
		boolean credentialEnhanced = false;
		SessionCredentialEnhancer enhancer = sessionCredentialEnhancer;
		if (enhancer != null) {
			enhancementMode = safeTrim(enhancer.getEnhancementMode(persistedCredentials));
			if (enhancementMode == null) {
				enhancementMode = CREDENTIAL_ENHANCEMENT_NONE;
			}
			String mfaCode = null;
			if (!CREDENTIAL_ENHANCEMENT_NONE.equals(enhancementMode)
					&& enhancer.requiresMfaCode(persistedCredentials)) {
				mfaPrompted = true;
				mfaCode = safeTrim(promptDelegate.requestMfaCode(
						enhancer.getMfaPromptHint(persistedCredentials)));
				if (mfaCode == null) {
					throw new MissingMfaCodeException();
				}
			}
			AwsCredentials enhanced = CREDENTIAL_ENHANCEMENT_NONE.equals(enhancementMode)
					? null
					: enhancer.enhance(persistedCredentials, mfaCode);
			if (enhanced != null) {
				runtimeCredentials = enhanced;
				credentialEnhanced = true;
			}
		}

		String credentialMode = runtimeCredentials.hasSessionToken() || sessionTokenExpected
				? CREDENTIAL_MODE_SESSION_TOKEN
				: CREDENTIAL_MODE_LONG_LIVED_KEY;

		return new Resolution(runtimeCredentials, persistedCredentials, credentialMode, secretSource,
				sessionTokenSource, enhancementMode, mfaPrompted, credentialEnhanced);
	}

	public void persistIfEnabled(@NonNull HostBean host, @NonNull AwsCredentials credentials) {
		long hostId = host.getId();
		if (hostId <= 0) {
			return;
		}

		if (!host.getRememberPassword()) {
			clear(hostId);
			return;
		}

		savedPasswordStore.saveScopedSecret(
				SavedPasswordStore.SCOPE_SSM_SECRET_ACCESS_KEY,
				hostId,
				credentials.getSecretAccessKey());
		savedPasswordStore.clearPassword(hostId);

		if (credentials.hasSessionToken()) {
			savedPasswordStore.saveScopedSecret(
					SavedPasswordStore.SCOPE_SSM_SESSION_TOKEN,
					hostId,
					credentials.getSessionToken());
		} else {
			savedPasswordStore.clearScopedSecret(
					SavedPasswordStore.SCOPE_SSM_SESSION_TOKEN,
					hostId);
		}
	}

	public void clear(long hostId) {
		if (hostId <= 0) {
			return;
		}
		savedPasswordStore.clearScopedSecret(SavedPasswordStore.SCOPE_SSM_SECRET_ACCESS_KEY, hostId);
		savedPasswordStore.clearScopedSecret(SavedPasswordStore.SCOPE_SSM_SESSION_TOKEN, hostId);
		savedPasswordStore.clearPassword(hostId);
	}

	private static String safeTrim(@Nullable String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static boolean looksLikeSessionCredentials(@Nullable String accessKeyId) {
		if (accessKeyId == null) {
			return false;
		}
		return accessKeyId.trim().toUpperCase(Locale.US).startsWith("ASIA");
	}
}
