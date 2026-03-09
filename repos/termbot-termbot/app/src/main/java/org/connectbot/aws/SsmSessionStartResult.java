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

public final class SsmSessionStartResult {
	private final String sessionId;
	private final String streamUrl;
	private final String tokenValue;

	public SsmSessionStartResult(@NonNull String sessionId, @NonNull String streamUrl,
			@NonNull String tokenValue) {
		this.sessionId = sessionId;
		this.streamUrl = streamUrl;
		this.tokenValue = tokenValue;
	}

	@NonNull
	public String getSessionId() {
		return sessionId;
	}

	@NonNull
	public String getStreamUrl() {
		return streamUrl;
	}

	@NonNull
	public String getTokenValue() {
		return tokenValue;
	}
}
