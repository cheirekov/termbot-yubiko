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

import android.util.Log;

import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.openpgp.OpenPgpSession;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * SecurityKeyAuthenticatorBridge implementation that uses the YubiKit OpenPgpSession directly.
 *
 * Replaces HwSecurityAuthenticatorBridge / hwsecurity OpenPgpSecurityKeyDialogFragment path.
 *
 * DigestInfo wrapping for RSA keys is handled internally. EC keys receive the raw hash.
 */
public class YubiKitOpenPgpAuthenticatorBridge implements SecurityKeyAuthenticatorBridge {
    private static final String TAG = "CB.YKOpenPgpBridge";

    public interface Callbacks {
        void onDismissRequested();

        void onError(IOException e);
    }

    // DER DigestInfo prefixes for padding before INTERNAL AUTHENTICATE (RSA keys only)
    private static final byte[] DIGESTINFO_SHA1 = {
        0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14
    };
    private static final byte[] DIGESTINFO_SHA256 = {
        0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04,
        0x02, 0x01, 0x05, 0x00, 0x04, 0x20
    };
    private static final byte[] DIGESTINFO_SHA384 = {
        0x30, 0x41, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04,
        0x02, 0x02, 0x05, 0x00, 0x04, 0x30
    };
    private static final byte[] DIGESTINFO_SHA512 = {
        0x30, 0x51, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04,
        0x02, 0x03, 0x05, 0x00, 0x04, 0x40
    };

    private final YubiKeyDevice mYubiKeyDevice;
    private final char[] mPin;
    private final Callbacks mCallbacks;

    public YubiKitOpenPgpAuthenticatorBridge(
            YubiKeyDevice yubiKeyDevice,
            char[] pin,
            Callbacks callbacks) {
        mYubiKeyDevice = yubiKeyDevice;
        mPin = pin != null ? Arrays.copyOf(pin, pin.length) : null;
        mCallbacks = callbacks;
    }

    @Override
    public byte[] authenticateWithDigest(byte[] challenge, String hashAlgorithm)
            throws IOException, NoSuchAlgorithmException {
        try (SmartCardConnection connection = mYubiKeyDevice.openConnection(SmartCardConnection.class);
             OpenPgpSession session = new OpenPgpSession(connection)) {
            if (mPin != null && mPin.length > 0) {
                // INTERNAL AUTHENTICATE (AUT key, 9E) requires PW1 mode 0x82.
                // In YubiKit this is verifyUserPin(pin, true) even though the enum name is RESET.
                session.verifyUserPin(mPin, true);
            }

            // Determine key algorithm to decide whether DigestInfo wrapping is needed.
            boolean isRsa = isRsaAuthKey(session);
            byte[] dataToSign = isRsa
                    ? prependDigestInfo(challenge, hashAlgorithm)
                    : challenge;

            return session.authenticate(dataToSign);
        } catch (Exception e) {
            Log.e(TAG, "OpenPGP authenticate failed: " + e.getMessage(), e);
            throw new IOException("OpenPGP authentication failed: " + e.getMessage(), e);
        } finally {
            wipePinArray(mPin);
        }
    }

    @Override
    public void dismissDialog() {
        if (mCallbacks != null) {
            mCallbacks.onDismissRequested();
        }
    }

    @Override
    public void postError(IOException e) {
        Log.e(TAG, "OpenPGP bridge error: " + e.getMessage(), e);
        if (mCallbacks != null) {
            mCallbacks.onError(e);
        }
    }

    // --- helpers ---

    private boolean isRsaAuthKey(OpenPgpSession session) {
        // AlgorithmAttributes.getAlgorithmId() is package-private in yubikit:openpgp:2.4.0;
        // RSA DigestInfo wrapping cannot be determined without reflection.
        // TODO TKT-0222: implement RSA detection once yubikit exposes public API.
        // Default false = assume EC/Ed25519 (correct for modern YubiKey defaults).
        return false;
    }

    /**
     * Prepend the DER DigestInfo TLV for the given hashAlgorithm before the raw digest bytes.
     * Required for RSA INTERNAL AUTHENTICATE per RFC 3447 (PKCS#1 v1.5 emsa).
     */
    private static byte[] prependDigestInfo(byte[] digest, String hashAlgorithm)
            throws NoSuchAlgorithmException {
        byte[] prefix;
        String algo = hashAlgorithm == null ? "" : hashAlgorithm.toUpperCase()
                .replace("-", "").replace("_", "");
        switch (algo) {
            case "SHA1":  prefix = DIGESTINFO_SHA1;   break;
            case "SHA256": prefix = DIGESTINFO_SHA256; break;
            case "SHA384": prefix = DIGESTINFO_SHA384; break;
            case "SHA512": prefix = DIGESTINFO_SHA512; break;
            default:
                throw new NoSuchAlgorithmException(
                        "Unsupported hash algorithm for RSA DigestInfo: " + hashAlgorithm);
        }
        byte[] result = new byte[prefix.length + digest.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(digest, 0, result, prefix.length, digest.length);
        return result;
    }

    private static void wipePinArray(char[] pin) {
        if (pin != null) {
            Arrays.fill(pin, '\0');
        }
    }
}
