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

package org.connectbot.securitykey;

import java.nio.charset.StandardCharsets;
import org.connectbot.bean.PubkeyBean;

/**
 * Compact metadata profile for hardware-backed pubkeys without DB schema migration.
 *
 * Stored in PubkeyBean.privateKey for security-key entries only. This does not
 * contain secrets, private keys, or PINs.
 */
public final class SecurityKeyProviderProfile {
	private static final String STORAGE_PREFIX = "SKPROFILE:v1;";
	private static final String KEY_PROVIDER = "provider";
	private static final String KEY_SLOT = "slot";

	public static final String PROVIDER_OPENPGP = "openpgp";
	public static final String PROVIDER_PIV = "piv";
	public static final String PROVIDER_FIDO2 = "fido2";
	public static final String SLOT_OPENPGP_AUTH = "AUTH (9E)";
	public static final String SLOT_PIV_AUTHENTICATION = "AUTHENTICATION (9A)";
	public static final String SLOT_PIV_SIGNATURE = "SIGNATURE (9C)";
	public static final String SLOT_FIDO2_APPLICATION = "ssh:";

	public static final SecurityKeyProviderProfile DEFAULT_OPENPGP_AUTH =
			new SecurityKeyProviderProfile(PROVIDER_OPENPGP, SLOT_OPENPGP_AUTH);
	public static final SecurityKeyProviderProfile DEFAULT_PIV_AUTH =
			new SecurityKeyProviderProfile(PROVIDER_PIV, SLOT_PIV_AUTHENTICATION);
	public static final SecurityKeyProviderProfile DEFAULT_FIDO2 =
			new SecurityKeyProviderProfile(PROVIDER_FIDO2, SLOT_FIDO2_APPLICATION);

	private final String provider;
	private final String slotReference;

	public SecurityKeyProviderProfile(String provider, String slotReference) {
		this.provider = provider == null ? PROVIDER_OPENPGP : provider.trim().toLowerCase();
		this.slotReference = slotReference == null ? "unknown" : slotReference.trim();
	}

	public String getProvider() {
		return provider;
	}

	public String getSlotReference() {
		return slotReference;
	}

	public boolean isOpenPgp() {
		return PROVIDER_OPENPGP.equals(provider);
	}

	public boolean isPiv() {
		return PROVIDER_PIV.equals(provider);
	}

	public boolean isFido2() {
		return PROVIDER_FIDO2.equals(provider);
	}

	public byte[] toStorageBlob() {
		String serialized = STORAGE_PREFIX
				+ KEY_PROVIDER + "=" + provider + ";"
				+ KEY_SLOT + "=" + slotReference + ";";
		return serialized.getBytes(StandardCharsets.UTF_8);
	}

	public static SecurityKeyProviderProfile fromPubkey(PubkeyBean pubkey) {
		if (pubkey == null || !pubkey.isSecurityKey()) {
			return DEFAULT_OPENPGP_AUTH;
		}
		return parse(pubkey.getPrivateKey());
	}

	public static SecurityKeyProviderProfile parse(byte[] blob) {
		if (blob == null || blob.length == 0) {
			return DEFAULT_OPENPGP_AUTH;
		}

		String raw = new String(blob, StandardCharsets.UTF_8);
		if (!raw.startsWith(STORAGE_PREFIX)) {
			return DEFAULT_OPENPGP_AUTH;
		}

		String payload = raw.substring(STORAGE_PREFIX.length());
		String provider = PROVIDER_OPENPGP;
		String slot = SLOT_OPENPGP_AUTH;
		String[] fields = payload.split(";");
		for (String field : fields) {
			if (field == null || field.isEmpty()) {
				continue;
			}
			int separator = field.indexOf('=');
			if (separator <= 0 || separator >= field.length() - 1) {
				continue;
			}
			String key = field.substring(0, separator).trim();
			String value = field.substring(separator + 1).trim();
			if (KEY_PROVIDER.equals(key) && !value.isEmpty()) {
				provider = value.toLowerCase();
			} else if (KEY_SLOT.equals(key) && !value.isEmpty()) {
				slot = value;
			}
		}
		return new SecurityKeyProviderProfile(provider, slot);
	}
}
