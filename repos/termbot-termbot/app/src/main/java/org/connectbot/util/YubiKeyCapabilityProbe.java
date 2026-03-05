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

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

public final class YubiKeyCapabilityProbe {
	public static final String MARKER_PROBE_START = "YUBIKEY_CAPABILITY_PROBE_START";
	public static final String MARKER_PROBE_RESULT = "YUBIKEY_CAPABILITY_PROBE_RESULT";
	public static final String MARKER_PIV_STATUS = "PIV_PROTOTYPE_STATUS";
	public static final String MARKER_FIDO2_STATUS = "FIDO2_PROTOTYPE_STATUS";
	public static final String MARKER_SSHLIB_SK_ECDSA = "SSHLIB_SK_ECDSA_SUPPORTED";
	public static final String MARKER_SSHLIB_SK_ED25519 = "SSHLIB_SK_ED25519_SUPPORTED";

	private static final String TAG = "CB.YubiKeyProbe";

	private YubiKeyCapabilityProbe() {
	}

	public static final class ProbeResult {
		public final boolean nfcSupported;
		public final boolean usbHostSupported;
		public final boolean sshlibSkEcdsaSupported;
		public final boolean sshlibSkEd25519Supported;
		public final String pivStatus;
		public final String fido2Status;

		ProbeResult(
				boolean nfcSupported,
				boolean usbHostSupported,
				boolean sshlibSkEcdsaSupported,
				boolean sshlibSkEd25519Supported,
				String pivStatus,
				String fido2Status) {
			this.nfcSupported = nfcSupported;
			this.usbHostSupported = usbHostSupported;
			this.sshlibSkEcdsaSupported = sshlibSkEcdsaSupported;
			this.sshlibSkEd25519Supported = sshlibSkEd25519Supported;
			this.pivStatus = pivStatus;
			this.fido2Status = fido2Status;
		}
	}

	@NonNull
	public static ProbeResult run(@NonNull Context context) {
		Context appContext = context.getApplicationContext();
		PackageManager pm = appContext.getPackageManager();
		boolean nfcSupported = pm.hasSystemFeature(PackageManager.FEATURE_NFC);
		boolean usbHostSupported = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST);

		SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_PROBE_START);

		// Newer sshlib exposes SK support through a generic SkPublicKey interface.
		boolean skInterfaceSupported = hasClass("com.trilead.ssh2.signature.SkPublicKey");
		boolean skEcdsaSupported = skInterfaceSupported;
		boolean skEd25519Supported = skInterfaceSupported;

		String pivStatus = "prototype_planned_sdk_selection";
		if (!nfcSupported && !usbHostSupported) {
			pivStatus = "blocked_no_nfc_or_usb_host";
		}

		String fido2Status;
		if (!nfcSupported && !usbHostSupported) {
			fido2Status = "blocked_no_nfc_or_usb_host";
		} else if (!skInterfaceSupported) {
			fido2Status = "enabled_ctap2_two_step_signing";
		} else {
			fido2Status = "enabled_with_sshlib_sk_interface";
		}

		SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_SSHLIB_SK_ECDSA, Boolean.toString(skEcdsaSupported));
		SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_SSHLIB_SK_ED25519, Boolean.toString(skEd25519Supported));
		SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_PIV_STATUS, pivStatus);
		SecurityKeyDebugLog.logFlow(appContext, TAG, MARKER_FIDO2_STATUS, fido2Status);
		SecurityKeyDebugLog.logFlow(
				appContext,
				TAG,
				MARKER_PROBE_RESULT,
				"nfc_supported=" + nfcSupported
						+ " usb_host_supported=" + usbHostSupported
						+ " sk_ecdsa_supported=" + skEcdsaSupported
						+ " sk_ed25519_supported=" + skEd25519Supported);

		return new ProbeResult(
				nfcSupported,
				usbHostSupported,
				skEcdsaSupported,
				skEd25519Supported,
				pivStatus,
				fido2Status);
	}

	private static boolean hasClass(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException ignored) {
			return false;
		}
	}
}
