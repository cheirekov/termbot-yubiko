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

import com.trilead.ssh2.crypto.keys.Ed25519PublicKey;
import com.trilead.ssh2.packets.TypesReader;
import com.trilead.ssh2.packets.TypesWriter;
import com.trilead.ssh2.signature.ECDSASHA2Verify;
import com.trilead.ssh2.signature.Ed25519Verify;
import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.CommandException;
import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.fido.ctap.ClientPin;
import com.yubico.yubikit.fido.ctap.Ctap2Session;
import com.yubico.yubikit.fido.ctap.PinUvAuthProtocolV1;
import com.yubico.yubikit.core.fido.FidoConnection;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.List;

/**
 * Security-key authenticator bridge for YubiKey FIDO2 signing.
 */
public class Fido2SecurityKeyAuthenticatorBridge implements SecurityKeyAuthenticatorBridge {
	public interface Callbacks {
		void onDismissRequested();

		void onError(IOException e);
	}

	private static final int AUTH_DATA_FLAGS_OFFSET = 32;
	private static final int AUTH_DATA_COUNTER_OFFSET = 33;
	private static final int AUTH_DATA_MIN_LENGTH = 37;

	private final YubiKeyDevice yubiKeyDevice;
	private final SshSkPublicKey sshSkPublicKey;
	private final String slotReference;
	private final Callbacks callbacks;
	private char[] pinChars;

	public Fido2SecurityKeyAuthenticatorBridge(
			YubiKeyDevice yubiKeyDevice,
			SshSkPublicKey sshSkPublicKey,
			String slotReference,
			char[] pinChars,
			Callbacks callbacks) {
		this.yubiKeyDevice = yubiKeyDevice;
		this.sshSkPublicKey = sshSkPublicKey;
		this.slotReference = slotReference;
		this.pinChars = pinChars == null ? null : Arrays.copyOf(pinChars, pinChars.length);
		this.callbacks = callbacks;
	}

	@Override
	public byte[] authenticateWithDigest(byte[] challenge, String hashAlgorithm)
			throws IOException, NoSuchAlgorithmException {
		if (challenge == null || challenge.length == 0) {
			throw new IOException("FIDO2 challenge is empty");
		}

		String keyType = sshSkPublicKey.getSshKeyType();
		if (!SshSkPublicKey.isSupportedKeyType(keyType)) {
			throw new IOException("Unsupported FIDO2 SSH key type: " + keyType);
		}

		String applicationId = SshSkPublicKey.normalizeApplication(sshSkPublicKey.getApplication());
		byte[] clientDataHash = sha256(challenge);

		try (Ctap2Session ctap2Session = openCtap2Session()) {
			byte[] pinUvAuthParam = null;
			Integer pinUvAuthProtocol = null;
			if (pinChars != null && pinChars.length > 0) {
				PinUvAuthProtocolV1 pinUvAuthProtocolV1 = new PinUvAuthProtocolV1();
				ClientPin clientPin = new ClientPin(ctap2Session, pinUvAuthProtocolV1);
				byte[] pinToken = clientPin.getPinToken(pinChars, ClientPin.PIN_PERMISSION_GA, applicationId);
				pinUvAuthParam = pinUvAuthProtocolV1.authenticate(pinToken, clientDataHash);
				pinUvAuthProtocol = pinUvAuthProtocolV1.getVersion();
			}

				List<Ctap2Session.AssertionData> assertions = ctap2Session.getAssertions(
						applicationId,
						clientDataHash,
						null,
					null,
					null,
					pinUvAuthParam,
					pinUvAuthProtocol,
					null);
				if (assertions == null || assertions.isEmpty()) {
					throw new IOException("No FIDO2 assertions available for application " + applicationId);
				}

				for (Ctap2Session.AssertionData assertion : assertions) {
					byte[] authData = assertion.getAuthenticatorData();
					if (authData == null || authData.length < AUTH_DATA_MIN_LENGTH) {
						continue;
					}

					byte[] signedData = concat(authData, clientDataHash);
					int flags = authData[AUTH_DATA_FLAGS_OFFSET] & 0xff;
					int counter = ((authData[AUTH_DATA_COUNTER_OFFSET] & 0xff) << 24)
							| ((authData[AUTH_DATA_COUNTER_OFFSET + 1] & 0xff) << 16)
							| ((authData[AUTH_DATA_COUNTER_OFFSET + 2] & 0xff) << 8)
							| (authData[AUTH_DATA_COUNTER_OFFSET + 3] & 0xff);

					byte[] assertionSignature = assertion.getSignature();
					byte[] sshWireSignature = toSshWireSecurityKeySignature(assertionSignature, keyType);
					byte[] encodedSignature = encodeSecurityKeySignature(keyType, sshWireSignature, flags, counter);
					if (!matchesImportedPublicKey(signedData, assertionSignature, keyType)) {
						continue;
					}
					return encodedSignature;
				}

				throw new IOException("No FIDO2 assertion matched the imported public key");
		} catch (ApplicationNotAvailableException e) {
			throw new IOException("FIDO2 application is not available on this YubiKey", e);
		} catch (CommandException e) {
			throw new IOException("FIDO2 command failed during sign operation", e);
		}
	}

	private Ctap2Session openCtap2Session() throws IOException, CommandException {
		Exception fidoConnectionFailure = null;
		try {
			FidoConnection fidoConnection = yubiKeyDevice.openConnection(FidoConnection.class);
			return new Ctap2Session(fidoConnection);
		} catch (Exception e) {
			fidoConnectionFailure = e;
		}

		try {
			SmartCardConnection smartCardConnection = yubiKeyDevice.openConnection(SmartCardConnection.class);
			return new Ctap2Session(smartCardConnection);
		} catch (IOException | CommandException e) {
			if (fidoConnectionFailure != null) {
				e.addSuppressed(fidoConnectionFailure);
			}
			throw e;
		}
	}

	@Override
	public void dismissDialog() {
		clearPin();
		if (callbacks != null) {
			callbacks.onDismissRequested();
		}
	}

	@Override
	public void postError(IOException e) {
		clearPin();
		if (callbacks != null) {
			callbacks.onError(e);
		}
	}

	private boolean matchesImportedPublicKey(byte[] signedData, byte[] assertionSignature, String keyType)
			throws IOException {
		if (SshSkPublicKey.KEY_TYPE_SK_ED25519.equals(keyType)) {
			Ed25519PublicKey publicKey = decodeEd25519PublicKey();
			return Ed25519Verify.verifySignature(signedData, assertionSignature, publicKey);
		}

		ECPublicKey publicKey = decodeEcdsaPublicKey();
		return ECDSASHA2Verify.verifySignature(signedData, assertionSignature, publicKey);
	}

	private byte[] toSshWireSecurityKeySignature(byte[] assertionSignature, String keyType) throws IOException {
		if (assertionSignature == null || assertionSignature.length == 0) {
			throw new IOException("Missing FIDO2 assertion signature");
		}
		if (SshSkPublicKey.KEY_TYPE_SK_ED25519.equals(keyType)) {
			return Arrays.copyOf(assertionSignature, assertionSignature.length);
		}

		ECPublicKey ecPublicKey = decodeEcdsaPublicKey();
		byte[] sshEcdsaSignature = ECDSASHA2Verify.encodeSSHECDSASignature(assertionSignature, ecPublicKey.getParams());
		TypesReader typesReader = new TypesReader(sshEcdsaSignature);
		typesReader.readString("US-ASCII");
		byte[] rs = typesReader.readByteString();
		if (typesReader.remain() != 0) {
			throw new IOException("Unexpected trailing bytes in ECDSA signature encoding");
		}
		return rs;
	}

	private ECPublicKey decodeEcdsaPublicKey() throws IOException {
		TypesWriter writer = new TypesWriter();
		writer.writeString("ecdsa-sha2-nistp256");
		writer.writeString("nistp256");
		byte[] keyData = sshSkPublicKey.getKeyData();
		writer.writeString(keyData, 0, keyData.length);
		return ECDSASHA2Verify.decodeSSHECDSAPublicKey(writer.getBytes());
	}

	private Ed25519PublicKey decodeEd25519PublicKey() throws IOException {
		TypesWriter writer = new TypesWriter();
		writer.writeString("ssh-ed25519");
		byte[] keyData = sshSkPublicKey.getKeyData();
		writer.writeString(keyData, 0, keyData.length);
		return Ed25519Verify.decodeSSHEd25519PublicKey(writer.getBytes());
	}

	private byte[] encodeSecurityKeySignature(String keyType, byte[] sshRawSignature, int flags, int counter) {
		TypesWriter outer = new TypesWriter();
		outer.writeString(keyType);
		outer.writeString(sshRawSignature, 0, sshRawSignature.length);
		outer.writeByte(flags);
		outer.writeUINT32(counter);
		return outer.getBytes();
	}

	private static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		return digest.digest(data);
	}

	private static byte[] concat(byte[] a, byte[] b) {
		byte[] out = Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, out, a.length, b.length);
		return out;
	}

	private void clearPin() {
		if (pinChars != null) {
			Arrays.fill(pinChars, '\0');
			pinChars = null;
		}
	}
}
