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

import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.piv.KeyType;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Shared PIV helper functions for slot/key/signature resolution.
 */
public final class PivSupport {
	private PivSupport() {
	}

	public static Slot resolveSlot(String slotReference) {
		if (slotReference == null) {
			return Slot.AUTHENTICATION;
		}
		String normalized = slotReference.toLowerCase();
		if (normalized.contains("9a") || normalized.contains("auth")) {
			return Slot.AUTHENTICATION;
		}
		if (normalized.contains("9c") || normalized.contains("sign")) {
			return Slot.SIGNATURE;
		}
		if (normalized.contains("9d") || normalized.contains("manage")) {
			return Slot.KEY_MANAGEMENT;
		}
		if (normalized.contains("9e") || normalized.contains("card")) {
			return Slot.CARD_AUTH;
		}
		return Slot.AUTHENTICATION;
	}

	public static String describeSlot(Slot slot) {
		if (slot == null) {
			return "AUTHENTICATION (9A)";
		}
		switch (slot) {
		case AUTHENTICATION:
			return "AUTHENTICATION (9A)";
		case SIGNATURE:
			return "SIGNATURE (9C)";
		case KEY_MANAGEMENT:
			return "KEY_MANAGEMENT (9D)";
		case CARD_AUTH:
			return "CARD_AUTH (9E)";
		default:
			return slot.name() + " (" + slot.getStringAlias().toUpperCase() + ")";
		}
	}

	public static KeyType resolveKeyType(PublicKey publicKey) throws IOException {
		if (publicKey instanceof RSAPublicKey) {
			int bits = ((RSAPublicKey) publicKey).getModulus().bitLength();
			return bits <= 1024 ? KeyType.RSA1024 : KeyType.RSA2048;
		}
		if (publicKey instanceof ECPublicKey) {
			int bits = ((ECPublicKey) publicKey).getParams().getCurve().getField().getFieldSize();
			return bits <= 256 ? KeyType.ECCP256 : KeyType.ECCP384;
		}
		throw new IOException("Unsupported PIV public key algorithm: " + publicKey.getAlgorithm());
	}

	public static Signature createSignature(PublicKey publicKey, String hashAlgorithm)
			throws IOException, NoSuchAlgorithmException {
		String normalizedHash = normalizeHash(hashAlgorithm);
		if (publicKey instanceof RSAPublicKey) {
			return Signature.getInstance(normalizedHash + "withRSA");
		}
		if (publicKey instanceof ECPublicKey) {
			return Signature.getInstance(normalizedHash + "withECDSA");
		}
		throw new IOException("Unsupported PIV signature algorithm for key type " + publicKey.getAlgorithm());
	}

	public static PublicKey readPublicKey(PivSession pivSession, Slot slot) throws IOException {
		try {
			return pivSession.getSlotMetadata(slot).getPublicKey();
		} catch (Exception ignored) {
			// Fall through to certificate retrieval for older devices/firmware.
		}

		try {
			X509Certificate certificate = pivSession.getCertificate(slot);
			if (certificate != null) {
				return certificate.getPublicKey();
			}
		} catch (ApduException | BadResponseException e) {
			throw new IOException("Unable to read PIV certificate for slot " + describeSlot(slot), e);
		} catch (Exception e) {
			throw new IOException("Unable to parse PIV certificate for slot " + describeSlot(slot), e);
		}

		throw new IOException("No public key available in PIV slot " + describeSlot(slot));
	}

	public static String normalizeHash(String hashAlgorithm) {
		if (hashAlgorithm == null || hashAlgorithm.trim().isEmpty()) {
			return "SHA256";
		}
		return hashAlgorithm.replace("-", "").toUpperCase();
	}
}
