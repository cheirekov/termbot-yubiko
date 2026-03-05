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

import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.piv.InvalidPinException;
import com.yubico.yubikit.piv.KeyType;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;

/**
 * Security-key authenticator bridge for YubiKey PIV signing.
 */
public class PivSecurityKeyAuthenticatorBridge implements SecurityKeyAuthenticatorBridge {
	public interface Callbacks {
		void onDismissRequested();

		void onError(IOException e);
	}

	private final YubiKeyDevice yubiKeyDevice;
	private final PublicKey publicKey;
	private final String slotReference;
	private final Callbacks callbacks;
	private char[] pinChars;

	public PivSecurityKeyAuthenticatorBridge(
			YubiKeyDevice yubiKeyDevice,
			PublicKey publicKey,
			String slotReference,
			char[] pinChars,
			Callbacks callbacks) {
		this.yubiKeyDevice = yubiKeyDevice;
		this.publicKey = publicKey;
		this.slotReference = slotReference;
		this.pinChars = pinChars == null ? null : Arrays.copyOf(pinChars, pinChars.length);
		this.callbacks = callbacks;
	}

	@Override
	public byte[] authenticateWithDigest(byte[] challenge, String hashAlgorithm)
			throws IOException, NoSuchAlgorithmException {
		Slot slot = PivSupport.resolveSlot(slotReference);
		KeyType keyType = PivSupport.resolveKeyType(publicKey);
		Signature signature = PivSupport.createSignature(publicKey, hashAlgorithm);

		try (SmartCardConnection smartCardConnection = yubiKeyDevice.openConnection(SmartCardConnection.class);
			 PivSession pivSession = new PivSession(smartCardConnection)) {
			if (pinChars != null && pinChars.length > 0) {
				pivSession.verifyPin(pinChars);
			}
			return pivSession.sign(slot, keyType, challenge, signature);
		} catch (InvalidPinException e) {
			throw new IOException("Invalid PIV PIN", e);
		} catch (ApplicationNotAvailableException e) {
			throw new IOException("PIV application is not available on this YubiKey", e);
		} catch (BadResponseException e) {
			throw new IOException("PIV returned an invalid response", e);
		} catch (ApduException e) {
			throw new IOException("PIV APDU failure during sign operation", e);
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

	private void clearPin() {
		if (pinChars != null) {
			Arrays.fill(pinChars, '\0');
			pinChars = null;
		}
	}
}
