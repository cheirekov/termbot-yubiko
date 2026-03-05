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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Security-key authenticator abstraction to decouple SSH flow from a specific SDK.
 */
public interface SecurityKeyAuthenticatorBridge {
	byte[] authenticateWithDigest(byte[] challenge, String hashAlgorithm) throws IOException, NoSuchAlgorithmException;

	void dismissDialog();

	void postError(IOException e);
}
