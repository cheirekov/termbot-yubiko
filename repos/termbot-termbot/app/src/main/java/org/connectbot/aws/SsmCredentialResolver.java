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

	public static final class MissingSessionTokenException extends IOException {
		private static final long serialVersionUID = 1L;

		public MissingSessionTokenException() {
			super("session_token_required");
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
		boolean requiresMfaCode();

		@Nullable
		String getMfaPromptHint();

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
		private final boolean mfaPrompted;
		private final boolean credentialEnhanced;

		private Resolution(@NonNull AwsCredentials runtimeCredentials,
				@NonNull AwsCredentials persistedCredentials,
				@NonNull String credentialMode,
				@NonNull String secretSource,
				@NonNull String sessionTokenSource,
				boolean mfaPrompted,
				boolean credentialEnhanced) {
			this.runtimeCredentials = runtimeCredentials;
			this.persistedCredentials = persistedCredentials;
			this.credentialMode = credentialMode;
			this.secretSource = secretSource;
			this.sessionTokenSource = sessionTokenSource;
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
		boolean mfaPrompted = false;
		boolean credentialEnhanced = false;
		SessionCredentialEnhancer enhancer = sessionCredentialEnhancer;
		if (enhancer != null) {
			String mfaCode = null;
			if (enhancer.requiresMfaCode()) {
				mfaPrompted = true;
				mfaCode = safeTrim(promptDelegate.requestMfaCode(enhancer.getMfaPromptHint()));
			}
			AwsCredentials enhanced = enhancer.enhance(persistedCredentials, mfaCode);
			if (enhanced != null) {
				runtimeCredentials = enhanced;
				credentialEnhanced = true;
			}
		}

		String credentialMode = runtimeCredentials.hasSessionToken() || sessionTokenExpected
				? CREDENTIAL_MODE_SESSION_TOKEN
				: CREDENTIAL_MODE_LONG_LIVED_KEY;

		return new Resolution(runtimeCredentials, persistedCredentials, credentialMode, secretSource,
				sessionTokenSource, mfaPrompted, credentialEnhanced);
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
