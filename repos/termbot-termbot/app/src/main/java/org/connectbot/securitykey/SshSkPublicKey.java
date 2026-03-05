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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.trilead.ssh2.crypto.Base64;
import com.trilead.ssh2.packets.TypesReader;
import com.trilead.ssh2.packets.TypesWriter;
import com.trilead.ssh2.crypto.keys.Ed25519PublicKey;
import com.trilead.ssh2.signature.ECDSASHA2Verify;
import com.trilead.ssh2.signature.Ed25519Verify;
import java.io.IOException;
import java.io.Serializable;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * Minimal SSH security-key public key representation for sk-* key blobs.
 */
public final class SshSkPublicKey implements PublicKey, Serializable {
	public static final String KEY_TYPE_SK_ECDSA = "sk-ecdsa-sha2-nistp256@openssh.com";
	public static final String KEY_TYPE_SK_ED25519 = "sk-ssh-ed25519@openssh.com";
	public static final String APPLICATION_DEFAULT = "ssh:";

	private static final long serialVersionUID = 1L;

	private final String sshKeyType;
	private final String application;
	private final byte[] keyData;
	private final byte[] encoded;

	private SshSkPublicKey(String sshKeyType, String application, byte[] keyData, byte[] encoded) {
		this.sshKeyType = sshKeyType;
		this.application = application;
		this.keyData = keyData;
		this.encoded = encoded;
	}

	public static final class ParsedAuthorizedKeyLine {
		public final SshSkPublicKey publicKey;
		public final String comment;

		ParsedAuthorizedKeyLine(SshSkPublicKey publicKey, String comment) {
			this.publicKey = publicKey;
			this.comment = comment;
		}
	}

	@NonNull
	public static ParsedAuthorizedKeyLine parseAuthorizedKeyLine(@Nullable String line) throws IOException {
		if (line == null) {
			throw new IOException("Missing OpenSSH public key line");
		}
		String trimmed = line.trim();
		if (trimmed.isEmpty()) {
			throw new IOException("Missing OpenSSH public key line");
		}

		String[] parts = trimmed.split("\\s+", 3);
		if (parts.length < 2) {
			throw new IOException("Invalid OpenSSH public key line");
		}

		String keyType = parts[0].trim();
		byte[] keyBlob = Base64.decode(parts[1].trim().toCharArray());
		SshSkPublicKey publicKey = fromBlob(keyBlob, keyType);
		String comment = (parts.length >= 3) ? parts[2].trim() : "";

		return new ParsedAuthorizedKeyLine(publicKey, comment);
	}

	@NonNull
	public static SshSkPublicKey fromBlob(byte[] keyBlob, @Nullable String expectedKeyType) throws IOException {
		if (keyBlob == null || keyBlob.length == 0) {
			throw new IOException("Missing security key public key blob");
		}
		TypesReader reader = new TypesReader(keyBlob);
		String parsedKeyType = reader.readString("US-ASCII");
		if (!isSupportedKeyType(parsedKeyType)) {
			throw new IOException("Unsupported security key type: " + parsedKeyType);
		}
		if (expectedKeyType != null && !expectedKeyType.isEmpty() && !expectedKeyType.equals(parsedKeyType)) {
			throw new IOException("Public key type mismatch: expected " + expectedKeyType + " but blob has " + parsedKeyType);
		}

		byte[] parsedKeyData;
		String parsedApplication;
		if (KEY_TYPE_SK_ECDSA.equals(parsedKeyType)) {
			reader.readString("US-ASCII"); // curve name (nistp256)
			parsedKeyData = reader.readByteString();
			parsedApplication = sanitizeApplication(reader.readString("UTF-8"));
		} else if (KEY_TYPE_SK_ED25519.equals(parsedKeyType)) {
			parsedKeyData = reader.readByteString();
			parsedApplication = sanitizeApplication(reader.readString("UTF-8"));
		} else {
			throw new IOException("Unsupported security key type: " + parsedKeyType);
		}

		return new SshSkPublicKey(
				parsedKeyType,
				parsedApplication,
				parsedKeyData,
				Arrays.copyOf(keyBlob, keyBlob.length));
	}

	@NonNull
	public static SshSkPublicKey fromPublicKey(@NonNull PublicKey publicKey, @Nullable String application)
			throws IOException {
		String normalizedApplication = normalizeApplication(application);
		TypesWriter writer = new TypesWriter();
		if (publicKey instanceof ECPublicKey) {
			byte[] encodedEcKey = ECDSASHA2Verify.encodeSSHECDSAPublicKey((ECPublicKey) publicKey);
			TypesReader reader = new TypesReader(encodedEcKey);
			String ecdsaType = reader.readString("US-ASCII");
			if (!"ecdsa-sha2-nistp256".equals(ecdsaType)) {
				throw new IOException("Only nistp256 is supported for sk-ecdsa keys");
			}
			String curveName = reader.readString("US-ASCII");
			byte[] keyPoint = reader.readByteString();

			writer.writeString(KEY_TYPE_SK_ECDSA);
			writer.writeString(curveName);
			writer.writeString(keyPoint, 0, keyPoint.length);
			writer.writeString(normalizedApplication);
			return fromBlob(writer.getBytes(), KEY_TYPE_SK_ECDSA);
		}

		if (isEd25519Algorithm(publicKey.getAlgorithm())) {
			byte[] edKeyData = extractEd25519KeyData(publicKey);
			writer.writeString(KEY_TYPE_SK_ED25519);
			writer.writeString(edKeyData, 0, edKeyData.length);
			writer.writeString(normalizedApplication);
			return fromBlob(writer.getBytes(), KEY_TYPE_SK_ED25519);
		}

		throw new IOException("Unsupported FIDO2 public key algorithm: " + publicKey.getAlgorithm());
	}

	public static boolean isSupportedKeyType(@Nullable String keyType) {
		return KEY_TYPE_SK_ECDSA.equals(keyType) || KEY_TYPE_SK_ED25519.equals(keyType);
	}

	@NonNull
	public static String normalizeApplication(@Nullable String application) {
		if (application == null) {
			return APPLICATION_DEFAULT;
		}
		String normalized = application.trim();
		return normalized.isEmpty() ? APPLICATION_DEFAULT : normalized;
	}

	private static String sanitizeApplication(@Nullable String application) {
		return normalizeApplication(application);
	}

	private static boolean isEd25519Algorithm(@Nullable String algorithm) {
		return "Ed25519".equalsIgnoreCase(String.valueOf(algorithm))
				|| "EdDSA".equalsIgnoreCase(String.valueOf(algorithm))
				|| "1.3.101.112".equals(algorithm);
	}

	private static byte[] extractEd25519KeyData(PublicKey publicKey) throws IOException {
		try {
			Ed25519PublicKey trileadPublicKey;
			if (publicKey instanceof Ed25519PublicKey) {
				trileadPublicKey = (Ed25519PublicKey) publicKey;
			} else {
				trileadPublicKey = new Ed25519PublicKey(new X509EncodedKeySpec(publicKey.getEncoded()));
			}
			byte[] encoded = Ed25519Verify.encodeSSHEd25519PublicKey(trileadPublicKey);
			TypesReader reader = new TypesReader(encoded);
			reader.readString("US-ASCII");
			return reader.readByteString();
		} catch (InvalidKeySpecException e) {
			throw new IOException("Unable to convert Ed25519 key data", e);
		}
	}

	public String getSshKeyType() {
		return sshKeyType;
	}

	public String getApplication() {
		return application;
	}

	public byte[] getKeyData() {
		return Arrays.copyOf(keyData, keyData.length);
	}

	@Override
	public String getAlgorithm() {
		if (KEY_TYPE_SK_ED25519.equals(sshKeyType)) {
			return "Ed25519";
		}
		if (KEY_TYPE_SK_ECDSA.equals(sshKeyType)) {
			return "EC";
		}
		return "SK";
	}

	@Override
	public String getFormat() {
		return "SSH";
	}

	@Override
	public byte[] getEncoded() {
		return Arrays.copyOf(encoded, encoded.length);
	}

	@NonNull
	public String toOpenSshPublicKeyLine(@Nullable String comment) {
		String openSsh = sshKeyType + " " + String.valueOf(Base64.encode(encoded));
		if (comment == null || comment.trim().isEmpty()) {
			return openSsh;
		}
		return openSsh + " " + comment.trim();
	}
}
