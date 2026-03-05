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

import java.util.Locale;

/**
 * Encapsulates device support policy for hardware security keys.
 */
public final class SecurityKeySupportPolicy {
	private SecurityKeySupportPolicy() {
	}

	public static boolean isYubiKeyDevice(String securityKeyName) {
		if (securityKeyName == null || securityKeyName.isEmpty()) {
			return false;
		}
		String normalizedName = securityKeyName.toLowerCase(Locale.US);
		return normalizedName.contains("yubikey") || normalizedName.contains("yubico");
	}
}
