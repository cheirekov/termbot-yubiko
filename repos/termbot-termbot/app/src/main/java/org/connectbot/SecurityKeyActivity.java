/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2019 Dominik Schürmann <dominik@cotech.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot;

import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.connectbot.securitykey.Fido2SecurityKeyAuthenticatorBridge;
import org.connectbot.securitykey.PivSecurityKeyAuthenticatorBridge;
import org.connectbot.securitykey.SecurityKeyAuthenticatorBridge;
import org.connectbot.securitykey.SecurityKeyProviderProfile;
import org.connectbot.securitykey.SshSkPublicKey;
import org.connectbot.securitykey.YubiKitOpenPgpAuthenticatorBridge;
import org.connectbot.service.SecurityKeyService;
import org.connectbot.util.SecurityKeyDebugLog;
import org.connectbot.util.SecurityKeyDebugReportExporter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration;
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.core.YubiKeyDevice;

public class SecurityKeyActivity extends AppCompatActivity {
    private static final String TAG = "CB.SKActivity";
    private static final String MARKER_CREATE_START = "SK_ACTIVITY_CREATE_START";
    private static final String MARKER_BIND_OK = "SK_ACTIVITY_BIND_OK";
    private static final String MARKER_BIND_FAILED = "SK_ACTIVITY_BIND_FAILED";
    private static final String MARKER_SERVICE_CONNECTED = "SK_ACTIVITY_SERVICE_CONNECTED";
    private static final String MARKER_SERVICE_DISCONNECTED = "SK_ACTIVITY_SERVICE_DISCONNECTED";
    private static final String MARKER_PENDING_AUTH = "SK_ACTIVITY_PENDING_AUTH";
    private static final String MARKER_PENDING_CANCEL = "SK_ACTIVITY_PENDING_CANCEL";
    private static final String MARKER_DESTROY = "SK_ACTIVITY_DESTROY";
    private static final String MARKER_EXPORT_START = "SK_ACTIVITY_EXPORT_REPORT_START";
    private static final String MARKER_EXPORT_SUCCESS = "SK_ACTIVITY_EXPORT_REPORT_SUCCESS";
    private static final String MARKER_EXPORT_FAILED = "SK_ACTIVITY_EXPORT_REPORT_FAILED";
    private static final String MARKER_PROVIDER_SELECTED = "SK_ACTIVITY_PROVIDER_SELECTED";
    private static final String MARKER_PIV_DISCOVERY_START = "SK_ACTIVITY_PIV_DISCOVERY_START";
    private static final String MARKER_PIV_DISCOVERED = "SK_ACTIVITY_PIV_DISCOVERED";
    private static final String MARKER_PIV_DISCOVERY_FAILED = "SK_ACTIVITY_PIV_DISCOVERY_FAILED";
    private static final String MARKER_PIV_PIN_PROMPT = "SK_ACTIVITY_PIV_PIN_PROMPT";
    private static final String MARKER_PIV_PIN_CANCELLED = "SK_ACTIVITY_PIV_PIN_CANCELLED";
    private static final String MARKER_PIV_AUTH_DELIVERED = "SK_ACTIVITY_PIV_AUTH_DELIVERED";
    private static final String MARKER_PIV_PUBLIC_KEY_DECODE_FAILED = "SK_ACTIVITY_PIV_PUBLIC_KEY_DECODE_FAILED";
    private static final String MARKER_FIDO2_DISCOVERY_START = "SK_ACTIVITY_FIDO2_DISCOVERY_START";
    private static final String MARKER_FIDO2_DISCOVERED = "SK_ACTIVITY_FIDO2_DISCOVERED";
    private static final String MARKER_FIDO2_DISCOVERY_FAILED = "SK_ACTIVITY_FIDO2_DISCOVERY_FAILED";
    private static final String MARKER_FIDO2_PIN_PROMPT = "SK_ACTIVITY_FIDO2_PIN_PROMPT";
    private static final String MARKER_FIDO2_PIN_CANCELLED = "SK_ACTIVITY_FIDO2_PIN_CANCELLED";
    private static final String MARKER_FIDO2_AUTH_DELIVERED = "SK_ACTIVITY_FIDO2_AUTH_DELIVERED";
    private static final String MARKER_FIDO2_AUTH_ERROR = "SK_ACTIVITY_FIDO2_AUTH_ERROR";
    private static final String MARKER_FIDO2_PUBLIC_KEY_DECODE_FAILED = "SK_ACTIVITY_FIDO2_PUBLIC_KEY_DECODE_FAILED";
    private static final String MARKER_OPENPGP_DISCOVERY_START = "SK_ACTIVITY_OPENPGP_DISCOVERY_START";
    private static final String MARKER_OPENPGP_DISCOVERED = "SK_ACTIVITY_OPENPGP_DISCOVERED";
    private static final String MARKER_OPENPGP_PIN_PROMPT = "SK_ACTIVITY_OPENPGP_PIN_PROMPT";
    private static final String MARKER_OPENPGP_PIN_CANCELLED = "SK_ACTIVITY_OPENPGP_PIN_CANCELLED";
    private static final String MARKER_OPENPGP_AUTH_DELIVERED = "SK_ACTIVITY_OPENPGP_AUTH_DELIVERED";
    private static final String MARKER_OPENPGP_AUTH_ERROR = "SK_ACTIVITY_OPENPGP_AUTH_ERROR";
    private static final String MARKER_WAIT_UI_SHOW = "SK_ACTIVITY_WAIT_UI_SHOW";
    private static final String MARKER_WAIT_UI_HIDE = "SK_ACTIVITY_WAIT_UI_HIDE";
    private static final String MARKER_WAIT_UI_CANCELLED = "SK_ACTIVITY_WAIT_UI_CANCELLED";
    private static final String MARKER_PIN_LOCAL_FORMAT_REJECTED = "SK_ACTIVITY_PIN_LOCAL_FORMAT_REJECTED";
    private static final String MARKER_USB_DISCOVERY_START = "SK_ACTIVITY_USB_DISCOVERY_START";
    private static final String MARKER_USB_DISCOVERY_STOP = "SK_ACTIVITY_USB_DISCOVERY_STOP";
    private static final String MARKER_USB_DISCOVERY_FAILED = "SK_ACTIVITY_USB_DISCOVERY_FAILED";
    private static final String MARKER_NFC_DISCOVERY_START = "SK_ACTIVITY_NFC_DISCOVERY_START";
    private static final String MARKER_NFC_DISCOVERY_STOP = "SK_ACTIVITY_NFC_DISCOVERY_STOP";
    private static final String MARKER_NFC_DISCOVERY_FAILED = "SK_ACTIVITY_NFC_DISCOVERY_FAILED";
    private static final int OPENPGP_PIN_MIN_LENGTH = 6;
    private static final int PIV_PIN_MIN_LENGTH = 6;
    private static final int PIV_PIN_MAX_LENGTH = 8;
    private static final int FIDO2_PIN_MIN_LENGTH = 4;

    public static final String EXTRA_PUBKEY_NICKNAME = "pubkey_nickname";
    public static final String EXTRA_SECURITY_KEY_PROVIDER = "security_key_provider";
    public static final String EXTRA_SECURITY_KEY_SLOT_REFERENCE = "security_key_slot_reference";
    public static final String EXTRA_SECURITY_KEY_PUBLIC_KEY_ALGORITHM = "security_key_public_key_algorithm";
    public static final String EXTRA_SECURITY_KEY_PUBLIC_KEY_BYTES = "security_key_public_key_bytes";

    private YubiKitManager mYubiKitManager;
    private SecurityKeyService mSecurityKeyService = null;
    private boolean mSecurityKeyServiceBound;
    private SecurityKeyAuthenticatorBridge mPendingAuthenticator;
    private boolean mPendingCancel;
    private String mProvider = SecurityKeyProviderProfile.PROVIDER_OPENPGP;
    private String mSlotReference = SecurityKeyProviderProfile.SLOT_OPENPGP_AUTH;
    private String mPublicKeyAlgorithm;
    private byte[] mPublicKeyBytes;
    private final AtomicBoolean mPivDiscoveryHandled = new AtomicBoolean(false);
    private final AtomicBoolean mFido2DiscoveryHandled = new AtomicBoolean(false);
    private final AtomicBoolean mOpenPgpDiscoveryHandled = new AtomicBoolean(false);
    private AlertDialog mWaitForKeyDialog;
    private boolean mUsbDiscoveryActive;
    private boolean mNfcDiscoveryActive;
    private boolean mIsResumed;
    // Two-tap NFC flow: PIN collected before key tap so the YubiKey can be tapped
    // stays live during the actual sign/verifyUserPin call.
    private volatile char[] mPendingOpenPgpPin;

    private final ServiceConnection mSecurityKeyServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mSecurityKeyService = ((SecurityKeyService.SecurityKeyServiceBinder) service).getService();
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_SERVICE_CONNECTED);
            deliverPendingActions();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mSecurityKeyService = null;
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_SERVICE_DISCONNECTED);
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_CREATE_START);

        mYubiKitManager = new YubiKitManager(this);

        String pubkeyNickname = getIntent().getStringExtra(EXTRA_PUBKEY_NICKNAME);
        mProvider = normalizeProvider(getIntent().getStringExtra(EXTRA_SECURITY_KEY_PROVIDER));
        mSlotReference = normalizeSlotReference(getIntent().getStringExtra(EXTRA_SECURITY_KEY_SLOT_REFERENCE));
        mPublicKeyAlgorithm = getIntent().getStringExtra(EXTRA_SECURITY_KEY_PUBLIC_KEY_ALGORITHM);
        mPublicKeyBytes = getIntent().getByteArrayExtra(EXTRA_SECURITY_KEY_PUBLIC_KEY_BYTES);
        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PROVIDER_SELECTED,
                "provider=" + mProvider + " slot=" + mSlotReference);

        mSecurityKeyServiceBound = getApplicationContext().bindService(
                new Intent(getApplicationContext(), SecurityKeyService.class),
                mSecurityKeyServiceConnection,
                Context.BIND_AUTO_CREATE);
        if (!mSecurityKeyServiceBound) {
            Log.e(TAG, MARKER_BIND_FAILED + ": unable to bind SecurityKeyService");
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BIND_FAILED, "unable to bind service");
            finish();
            return;
        }
        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BIND_OK);

        // Show prompt appropriate to provider
        switch (mProvider) {
            case SecurityKeyProviderProfile.PROVIDER_PIV:
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PIV_DISCOVERY_START, mSlotReference);
                showWaitForKeyDialog(getString(R.string.security_key_piv_wait_for_key));
                break;
            case SecurityKeyProviderProfile.PROVIDER_FIDO2:
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_DISCOVERY_START, mSlotReference);
                showWaitForKeyDialog(getString(R.string.security_key_fido2_wait_for_key));
                break;
            default:
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_DISCOVERY_START);
                // Collect PIN immediately (before any key tap) so the NFC connection
                // obtained on the subsequent tap remains live during authentication.
                promptForOpenPgpPin(pinChars -> {
                    if (pinChars == null) {
                        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_PIN_CANCELLED);
                        cancelAndFinish();
                        return;
                    }
                    SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_PIN_PROMPT);
                    mPendingOpenPgpPin = pinChars;
                    showWaitForKeyDialog(getString(R.string.security_key_openpgp_tap_now));
                    maybeStartYubiKitDiscovery();
                });
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResumed = true;
        maybeStartYubiKitDiscovery();
    }

    @Override
    protected void onPause() {
        mIsResumed = false;
        stopYubiKitDiscovery();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_DESTROY);
        dismissWaitForKeyDialog();
        if (mPendingOpenPgpPin != null) {
            Arrays.fill(mPendingOpenPgpPin, '\0');
            mPendingOpenPgpPin = null;
        }
        stopYubiKitDiscovery();
        if (mSecurityKeyServiceBound) {
            getApplicationContext().unbindService(mSecurityKeyServiceConnection);
            mSecurityKeyServiceBound = false;
        }
        super.onDestroy();
    }

    // --- YubiKey device dispatch ---

    private void handleYubiKeyDevice(YubiKeyDevice yubiKeyDevice, @NonNull String source) {
        switch (mProvider) {
            case SecurityKeyProviderProfile.PROVIDER_PIV:
                if (!mPivDiscoveryHandled.compareAndSet(false, true)) return;
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PIV_DISCOVERED,
                        "source=" + source);
                dismissWaitForKeyDialog();
                handlePivDiscovery(yubiKeyDevice);
                break;
            case SecurityKeyProviderProfile.PROVIDER_FIDO2:
                if (!mFido2DiscoveryHandled.compareAndSet(false, true)) return;
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_DISCOVERED,
                        "source=" + source);
                dismissWaitForKeyDialog();
                handleFido2Discovery(yubiKeyDevice);
                break;
            default:
                if (!mOpenPgpDiscoveryHandled.compareAndSet(false, true)) return;
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_DISCOVERED,
                        "source=" + source);
                dismissWaitForKeyDialog();
                handleOpenPgpDiscovery(yubiKeyDevice);
                break;
        }
    }

    private void maybeStartYubiKitDiscovery() {
        if (mYubiKitManager == null || !mIsResumed) {
            return;
        }
        if (SecurityKeyProviderProfile.PROVIDER_OPENPGP.equals(mProvider)
                && (mPendingOpenPgpPin == null || mPendingOpenPgpPin.length == 0)) {
            // OpenPGP two-tap flow: wait until PIN is collected, then start NFC/USB discovery.
            return;
        }

        if (!mUsbDiscoveryActive) {
            try {
                mYubiKitManager.startUsbDiscovery(new UsbConfiguration(),
                        device -> handleYubiKeyDevice(device, "usb"));
                mUsbDiscoveryActive = true;
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_USB_DISCOVERY_START);
            } catch (Exception e) {
                Log.w(TAG, "USB discovery unavailable: " + e.getMessage());
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_USB_DISCOVERY_FAILED,
                        e.getClass().getSimpleName());
            }
        }

        if (!mNfcDiscoveryActive) {
            try {
                mYubiKitManager.startNfcDiscovery(new NfcConfiguration(), this,
                        device -> handleYubiKeyDevice(device, "nfc"));
                mNfcDiscoveryActive = true;
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_NFC_DISCOVERY_START);
            } catch (NfcNotAvailable e) {
                Log.w(TAG, "NFC not available: " + e.getMessage());
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_NFC_DISCOVERY_FAILED,
                        "not_available");
            } catch (RuntimeException e) {
                Log.w(TAG, "NFC discovery start failed: " + e.getMessage());
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_NFC_DISCOVERY_FAILED,
                        e.getClass().getSimpleName());
            }
        }
    }

    private void stopYubiKitDiscovery() {
        if (mYubiKitManager == null) {
            return;
        }
        if (mNfcDiscoveryActive) {
            try {
                mYubiKitManager.stopNfcDiscovery(this);
            } catch (RuntimeException ignored) {
            }
            mNfcDiscoveryActive = false;
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_NFC_DISCOVERY_STOP);
        }
        if (mUsbDiscoveryActive) {
            try {
                mYubiKitManager.stopUsbDiscovery();
            } catch (RuntimeException ignored) {
            }
            mUsbDiscoveryActive = false;
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_USB_DISCOVERY_STOP);
        }
    }

    private void handleDiscoveryError(Exception e) {
        Log.e(TAG, "Key discovery failed: " + e.getMessage(), e);
        String msg;
        switch (mProvider) {
            case SecurityKeyProviderProfile.PROVIDER_PIV:
                mPivDiscoveryHandled.set(false);
                msg = getString(R.string.security_key_piv_auth_failed);
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PIV_DISCOVERY_FAILED,
                        e.getClass().getSimpleName());
                break;
            case SecurityKeyProviderProfile.PROVIDER_FIDO2:
                mFido2DiscoveryHandled.set(false);
                msg = getString(R.string.security_key_fido2_auth_failed);
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_DISCOVERY_FAILED,
                        e.getClass().getSimpleName());
                break;
            default:
                mOpenPgpDiscoveryHandled.set(false);
                msg = getString(R.string.terminal_auth_pubkey_fail_security_key);
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_AUTH_ERROR,
                        e.getClass().getSimpleName());
                break;
        }
        final String toastMsg = msg;
        runOnUiThread(() -> {
            Toast.makeText(SecurityKeyActivity.this, toastMsg, Toast.LENGTH_LONG).show();
            cancelAndFinish();
        });
    }

    // --- OpenPGP flow ---

    private void handleOpenPgpDiscovery(YubiKeyDevice yubiKeyDevice) {
        char[] pin = mPendingOpenPgpPin;
        mPendingOpenPgpPin = null;
        if (pin != null && pin.length > 0) {
            // PIN pre-collected: create bridge immediately while NFC connection is live.
            YubiKitOpenPgpAuthenticatorBridge bridge = createOpenPgpAuthenticatorBridge(yubiKeyDevice, pin);
            deliverAuthenticator(bridge);
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_AUTH_DELIVERED);
            return;
        }
        // Fallback (e.g. USB): show PIN dialog after discovery.
        runOnUiThread(() -> promptForOpenPgpPin(pinChars -> {
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_PIN_PROMPT);
            if (pinChars == null) {
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_PIN_CANCELLED);
                cancelAndFinish();
                return;
            }
            YubiKitOpenPgpAuthenticatorBridge bridge = createOpenPgpAuthenticatorBridge(yubiKeyDevice, pinChars);
            deliverAuthenticator(bridge);
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_AUTH_DELIVERED);
        }));
    }

    private YubiKitOpenPgpAuthenticatorBridge createOpenPgpAuthenticatorBridge(
            YubiKeyDevice yubiKeyDevice,
            char[] pinChars) {
        return new YubiKitOpenPgpAuthenticatorBridge(
                yubiKeyDevice,
                pinChars,
                new YubiKitOpenPgpAuthenticatorBridge.Callbacks() {
                    @Override
                    public void onDismissRequested() {
                        runOnUiThread(SecurityKeyActivity.this::finish);
                    }

                    @Override
                    public void onError(IOException e) {
                        String detail = e.getClass().getSimpleName();
                        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                            detail += "(" + e.getMessage() + ")";
                        }
                        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_AUTH_ERROR, detail);
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    SecurityKeyActivity.this,
                                    resolveOpenPgpErrorMessage(e),
                                    Toast.LENGTH_LONG).show();
                            cancelAndFinish();
                        });
                    }
                });
    }

    private String resolveOpenPgpErrorMessage(IOException e) {
        String msg = e.getMessage();
        if (msg == null) {
            return getString(R.string.terminal_auth_pubkey_fail_security_key);
        }
        String normalized = msg.toLowerCase(Locale.US);
        if (normalized.contains("invalid pin/puk") || normalized.contains("invalid pin")) {
            return getString(R.string.security_key_openpgp_invalid_pin);
        }
        return getString(R.string.terminal_auth_pubkey_fail_security_key);
    }

    // --- PIV flow ---

    private void handlePivDiscovery(YubiKeyDevice yubiKeyDevice) {
        PublicKey publicKey = decodePublicKeyForPiv();
        if (publicKey == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.security_key_piv_auth_failed, Toast.LENGTH_LONG).show();
                cancelAndFinish();
            });
            return;
        }

        runOnUiThread(() -> promptForPivPin(pinChars -> {
            if (pinChars == null || pinChars.length == 0) {
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PIV_PIN_CANCELLED);
                cancelAndFinish();
                return;
            }
            showWaitForKeyDialog(getString(R.string.security_key_piv_wait_for_key));

            PivSecurityKeyAuthenticatorBridge bridge = new PivSecurityKeyAuthenticatorBridge(
                    yubiKeyDevice,
                    publicKey,
                    mSlotReference,
                    pinChars,
                    new PivSecurityKeyAuthenticatorBridge.Callbacks() {
                        @Override
                        public void onDismissRequested() {
                            runOnUiThread(SecurityKeyActivity.this::finish);
                        }

                        @Override
                        public void onError(IOException e) {
                            runOnUiThread(() -> {
                                Toast.makeText(SecurityKeyActivity.this,
                                        R.string.security_key_piv_auth_failed, Toast.LENGTH_LONG).show();
                                cancelAndFinish();
                            });
                        }
                    });

            deliverAuthenticator(bridge);
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PIV_AUTH_DELIVERED);
        }));
    }

    // --- FIDO2 flow ---

    private void handleFido2Discovery(YubiKeyDevice yubiKeyDevice) {
        SshSkPublicKey sshSkPublicKey = decodePublicKeyForFido2();
        if (sshSkPublicKey == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.security_key_fido2_auth_failed, Toast.LENGTH_LONG).show();
                cancelAndFinish();
            });
            return;
        }

        runOnUiThread(() -> promptForFido2Pin(pinChars -> {
            if (pinChars == null) {
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_PIN_CANCELLED);
                cancelAndFinish();
                return;
            }
            showWaitForKeyDialog(getString(R.string.security_key_fido2_wait_for_key));

            Fido2SecurityKeyAuthenticatorBridge bridge = new Fido2SecurityKeyAuthenticatorBridge(
                    yubiKeyDevice,
                    sshSkPublicKey,
                    mSlotReference,
                    pinChars,
                    new Fido2SecurityKeyAuthenticatorBridge.Callbacks() {
                        @Override
                        public void onDismissRequested() {
                            runOnUiThread(SecurityKeyActivity.this::finish);
                        }

                        @Override
                        public void onError(IOException e) {
                            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_AUTH_ERROR,
                                    e.getClass().getSimpleName());
                            showWaitForKeyDialog(getString(R.string.security_key_fido2_wait_for_key));
                            // Allow retry
                            mFido2DiscoveryHandled.set(false);
                        }
                    });

            deliverAuthenticator(bridge);
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_AUTH_DELIVERED);
        }));
    }

    // --- Authenticator delivery ---

    private void showWaitForKeyDialog(@NonNull String message) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }

            if (mWaitForKeyDialog != null && mWaitForKeyDialog.isShowing()) {
                TextView messageView = mWaitForKeyDialog.findViewById(R.id.security_key_wait_message);
                if (messageView != null) {
                    messageView.setText(message);
                }
                return;
            }

            View dialogView = getLayoutInflater().inflate(R.layout.dia_security_key_wait_for_touch, null);
            TextView messageView = dialogView.findViewById(R.id.security_key_wait_message);
            messageView.setText(message);

            AlertDialog dialog = new AlertDialog.Builder(SecurityKeyActivity.this)
                    .setTitle(R.string.security_key_wait_dialog_title)
                    .setView(dialogView)
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> {
                        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_WAIT_UI_CANCELLED);
                        cancelAndFinish();
                    })
                    .create();
            dialog.setOnCancelListener(dialogInterface -> {
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_WAIT_UI_CANCELLED);
                cancelAndFinish();
            });
            dialog.show();
            mWaitForKeyDialog = dialog;
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_WAIT_UI_SHOW, message);
        });
    }

    private void dismissWaitForKeyDialog() {
        runOnUiThread(() -> {
            if (mWaitForKeyDialog == null) {
                return;
            }
            if (mWaitForKeyDialog.isShowing()) {
                mWaitForKeyDialog.dismiss();
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_WAIT_UI_HIDE);
            }
            mWaitForKeyDialog = null;
        });
    }

    private void deliverAuthenticator(SecurityKeyAuthenticatorBridge bridge) {
        synchronized (this) {
            if (mSecurityKeyService != null) {
                mSecurityKeyService.setAuthenticator(bridge);
            } else {
                mPendingAuthenticator = bridge;
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PENDING_AUTH,
                        "service not connected");
            }
        }
    }

    private void cancelAndFinish() {
        dismissWaitForKeyDialog();
        synchronized (this) {
            if (mSecurityKeyService != null) {
                mSecurityKeyService.cancel();
            } else {
                mPendingCancel = true;
                mPendingAuthenticator = null;
            }
        }
        finish();
    }

    private void deliverPendingActions() {
        synchronized (this) {
            if (mSecurityKeyService == null) return;
            if (mPendingCancel) {
                mSecurityKeyService.cancel();
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PENDING_CANCEL,
                        "delivered queued cancel");
                mPendingCancel = false;
                mPendingAuthenticator = null;
                return;
            }
            if (mPendingAuthenticator != null) {
                mSecurityKeyService.setAuthenticator(mPendingAuthenticator);
                SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PENDING_AUTH,
                        "delivered queued authenticator");
                mPendingAuthenticator = null;
            }
        }
    }

    // --- PIN prompts ---

    private interface OpenPgpPinCallback { void onResult(@Nullable char[] pinChars); }
    private interface PivPinCallback { void onResult(@Nullable char[] pinChars); }
    private interface Fido2PinCallback { void onResult(@Nullable char[] pinChars); }
    private interface PinResultCallback { void onResult(@Nullable char[] pinChars); }
    private interface PinPolicyValidator {
        @Nullable
        PinPolicyError validate(@NonNull String pinValue);
    }

    private static final class PinPolicyError {
        private final int messageResId;
        private final String reasonCode;

        private PinPolicyError(int messageResId, @NonNull String reasonCode) {
            this.messageResId = messageResId;
            this.reasonCode = reasonCode;
        }
    }

    private void promptForOpenPgpPin(@NonNull OpenPgpPinCallback callback) {
        showPinPrompt(
                R.string.security_key_openpgp_pin_prompt_title,
                getString(R.string.security_key_openpgp_pin_prompt_message),
                R.string.security_key_openpgp_pin_hint,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD,
                SecurityKeyProviderProfile.PROVIDER_OPENPGP,
                pinValue -> {
                    if (pinValue.isEmpty()) {
                        return new PinPolicyError(R.string.security_key_openpgp_pin_required, "empty");
                    }
                    if (!pinValue.matches("\\d+")) {
                        return new PinPolicyError(R.string.security_key_openpgp_pin_format_invalid, "non_numeric");
                    }
                    if (pinValue.length() < OPENPGP_PIN_MIN_LENGTH) {
                        return new PinPolicyError(R.string.security_key_openpgp_pin_min_length, "length_lt_6");
                    }
                    return null;
                },
                callback::onResult);
    }

    private void promptForPivPin(@NonNull PivPinCallback callback) {
        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PIV_PIN_PROMPT);
        showPinPrompt(
                R.string.security_key_piv_pin_prompt_title,
                getString(R.string.security_key_piv_pin_prompt_message),
                R.string.security_key_piv_pin_hint,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                SecurityKeyProviderProfile.PROVIDER_PIV,
                pinValue -> {
                    if (pinValue.isEmpty()) {
                        return new PinPolicyError(R.string.security_key_piv_pin_required, "empty");
                    }
                    if (pinValue.length() < PIV_PIN_MIN_LENGTH || pinValue.length() > PIV_PIN_MAX_LENGTH) {
                        return new PinPolicyError(R.string.security_key_piv_pin_length_invalid, "length_6_8_required");
                    }
                    return null;
                },
                callback::onResult);
    }

    private void promptForFido2Pin(@NonNull Fido2PinCallback callback) {
        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_PIN_PROMPT);
        showPinPrompt(
                R.string.security_key_fido2_pin_prompt_title,
                getString(R.string.security_key_fido2_pin_prompt_message, mSlotReference),
                R.string.security_key_fido2_pin_hint,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                SecurityKeyProviderProfile.PROVIDER_FIDO2,
                pinValue -> {
                    if (!pinValue.isEmpty() && pinValue.length() < FIDO2_PIN_MIN_LENGTH) {
                        return new PinPolicyError(R.string.security_key_fido2_pin_min_length, "length_lt_4");
                    }
                    return null;
                },
                callback::onResult);
    }

    private void showPinPrompt(
            int titleResId,
            @NonNull String message,
            int hintResId,
            int inputType,
            @NonNull String providerForLogs,
            @NonNull PinPolicyValidator validator,
            @NonNull PinResultCallback callback) {
        EditText pinInput = new EditText(this);
        pinInput.setInputType(inputType);
        pinInput.setHint(hintResId);
        pinInput.setSingleLine(true);
        pinInput.setImeOptions(EditorInfo.IME_ACTION_DONE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setMessage(message)
                .setView(pinInput)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (d, w) -> callback.onResult(null))
                .create();

        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ESCAPE) {
                callback.onResult(null);
                dialog.dismiss();
                return true;
            }
            return false;
        });

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String pinValue = pinInput.getText() == null ? "" : pinInput.getText().toString();
                    PinPolicyError validationError = validator.validate(pinValue);
                    if (validationError != null) {
                        SecurityKeyDebugLog.logFlow(
                                getApplicationContext(),
                                TAG,
                                MARKER_PIN_LOCAL_FORMAT_REJECTED,
                                "provider=" + providerForLogs + " reason=" + validationError.reasonCode);
                        Toast.makeText(this, validationError.messageResId, Toast.LENGTH_LONG).show();
                        return;
                    }
                    callback.onResult(pinValue.toCharArray());
                    dialog.dismiss();
                }));

        pinInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnterKey = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER);
            boolean isImeAction = actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_NULL;
            if (isEnterKey || isImeAction) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });

        dialog.show();
    }

    // --- Public key decode helpers ---

    @Nullable
    private PublicKey decodePublicKeyForPiv() {
        if (mPublicKeyBytes == null || mPublicKeyBytes.length == 0 || mPublicKeyAlgorithm == null) {
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG,
                    MARKER_PIV_PUBLIC_KEY_DECODE_FAILED, "missing extras");
            return null;
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(mPublicKeyAlgorithm);
            return keyFactory.generatePublic(new X509EncodedKeySpec(mPublicKeyBytes));
        } catch (Exception e) {
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG,
                    MARKER_PIV_PUBLIC_KEY_DECODE_FAILED, e.getClass().getSimpleName());
            return null;
        }
    }

    @Nullable
    private SshSkPublicKey decodePublicKeyForFido2() {
        if (mPublicKeyBytes == null || mPublicKeyBytes.length == 0) {
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG,
                    MARKER_FIDO2_PUBLIC_KEY_DECODE_FAILED, "missing extras");
            return null;
        }
        try {
            return SshSkPublicKey.fromBlob(mPublicKeyBytes, null);
        } catch (IOException e) {
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG,
                    MARKER_FIDO2_PUBLIC_KEY_DECODE_FAILED, e.getClass().getSimpleName());
            return null;
        }
    }

    // --- Normalise helpers ---

    private String normalizeProvider(@Nullable String provider) {
        if (provider == null || provider.isEmpty()) return SecurityKeyProviderProfile.PROVIDER_OPENPGP;
        return provider.trim().toLowerCase();
    }

    private String normalizeSlotReference(@Nullable String slotReference) {
        if (slotReference == null || slotReference.isEmpty()) return SecurityKeyProviderProfile.SLOT_OPENPGP_AUTH;
        return slotReference;
    }

    // --- Options menu / debug report ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.security_key_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_export_debug_report) {
            exportDebugReport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportDebugReport() {
        SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_EXPORT_START);
        try {
            File reportFile = SecurityKeyDebugReportExporter.exportReport(this);
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_EXPORT_SUCCESS,
                    reportFile.getName());
            startActivity(SecurityKeyDebugReportExporter.createShareIntent(this, reportFile));
            Toast.makeText(this,
                    getString(R.string.security_key_export_debug_report_success,
                            reportFile.getAbsolutePath()),
                    Toast.LENGTH_LONG).show();
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, MARKER_EXPORT_FAILED + ": unable to export debug report", e);
            SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_EXPORT_FAILED,
                    e.getClass().getSimpleName());
            Toast.makeText(this, R.string.security_key_export_debug_report_failed,
                    Toast.LENGTH_LONG).show();
        }
    }
}
