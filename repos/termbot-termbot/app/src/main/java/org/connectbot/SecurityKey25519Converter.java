package org.connectbot;

import java.security.PublicKey;

/**
 * Previously converted Hwsecurity25519PublicKey wrappers to ConnectBot's Ed25519PublicKey.
 * With YubiKit the key material is returned as standard java.security.PublicKey directly,
 * so this converter is now a passthrough retained for call-site compatibility.
 */
class SecurityKey25519Converter {

    static PublicKey hwsecurityToConnectbot(PublicKey publicKey) {
        return publicKey;
    }
}