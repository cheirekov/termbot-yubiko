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

public final class AwsCredentials {
	private final String accessKeyId;
	private final String secretAccessKey;
	private final String sessionToken;

	public AwsCredentials(@NonNull String accessKeyId, @NonNull String secretAccessKey,
			@Nullable String sessionToken) {
		this.accessKeyId = accessKeyId;
		this.secretAccessKey = secretAccessKey;
		this.sessionToken = sessionToken;
	}

	@NonNull
	public String getAccessKeyId() {
		return accessKeyId;
	}

	@NonNull
	public String getSecretAccessKey() {
		return secretAccessKey;
	}

	@Nullable
	public String getSessionToken() {
		return sessionToken;
	}

	public boolean hasSessionToken() {
		return sessionToken != null && !sessionToken.isEmpty();
	}
}
