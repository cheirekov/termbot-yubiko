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

/**
 * Replaces OpenPgpSecurityKey.AlgorithmConfig from hwsecurity-openpgp.
 * Maps the spinner selection in PubkeyAddBottomSheetDialog to a key algorithm
 * that the YubiKit OpenPGP session can apply.
 */
public enum OpenPgpKeyAlgorithmConfig {
    CURVE25519_GENERATE_ON_HARDWARE,
    NIST_P256_GENERATE_ON_HARDWARE,
    NIST_P384_GENERATE_ON_HARDWARE,
    NIST_P521_GENERATE_ON_HARDWARE,
    RSA_2048_UPLOAD
}