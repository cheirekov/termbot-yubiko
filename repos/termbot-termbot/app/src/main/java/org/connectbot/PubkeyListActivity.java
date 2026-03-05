/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2007 Kenny Root, Jeffrey Sharkey
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
import java.util.Map;

import org.connectbot.bean.PubkeyBean;
import org.connectbot.service.TerminalManager;
import org.connectbot.securitykey.PivSupport;
import org.connectbot.securitykey.SecurityKeyProviderProfile;
import org.connectbot.securitykey.SshSkPublicKey;
import org.connectbot.util.PubkeyDatabase;
import org.connectbot.util.PubkeyUtils;
import org.connectbot.util.SecurityKeyDebugLog;
import org.connectbot.util.SecurityKeySupportPolicy;
import org.openintents.intents.FileManagerIntents;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.trilead.ssh2.crypto.Base64;
import com.trilead.ssh2.crypto.PEMDecoder;
import com.trilead.ssh2.crypto.PEMStructure;
import com.trilead.ssh2.crypto.keys.Ed25519PublicKey;
import com.trilead.ssh2.signature.Ed25519Verify;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.InputType;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yubico.yubikit.fido.Cose;
import com.yubico.yubikit.fido.ctap.ClientPin;
import com.yubico.yubikit.fido.ctap.CredentialManagement;
import com.yubico.yubikit.fido.ctap.Ctap2Session;
import com.yubico.yubikit.fido.ctap.PinUvAuthProtocolV1;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration;
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.core.fido.FidoConnection;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import org.connectbot.securitykey.OpenPgpKeyAlgorithmConfig;

/**
 * List public keys in database by nickname and describe their properties. Allow users to import,
 * generate, rename, and delete key pairs.
 *
 * @author Kenny Root
 */
public class PubkeyListActivity extends AppCompatListActivity implements EventListener, PubkeyAddBottomSheetDialog.PubkeyAddBottomSheetListener {
	public final static String TAG = "CB.PubkeyListActivity";
	private static final int DESKTOP_ADD_KEY_MIN_WIDTH_DP = 840;

	private static final int MAX_KEYFILE_SIZE = 32768;
	private static final int REQUEST_CODE_PICK_FILE = 1;
	private static final String MARKER_UNSUPPORTED_DEVICE = "PUBKEY_UNSUPPORTED_SECURITY_KEY_DEVICE";
	private static final String MARKER_PUBLIC_KEY_EXPORT = "PUBKEY_PUBLIC_KEY_EXPORT";
	private static final String MARKER_PIV_IMPORT_START = "PUBKEY_PIV_IMPORT_START";
	private static final String MARKER_PIV_IMPORT_DISCOVERED = "PUBKEY_PIV_IMPORT_DISCOVERED";
	private static final String MARKER_PIV_IMPORT_SUCCESS = "PUBKEY_PIV_IMPORT_SUCCESS";
	private static final String MARKER_PIV_IMPORT_FAILED = "PUBKEY_PIV_IMPORT_FAILED";
	private static final String MARKER_FIDO2_IMPORT_START = "PUBKEY_FIDO2_IMPORT_START";
	private static final String MARKER_FIDO2_IMPORT_DISCOVERED = "PUBKEY_FIDO2_IMPORT_DISCOVERED";
	private static final String MARKER_FIDO2_IMPORT_SUCCESS = "PUBKEY_FIDO2_IMPORT_SUCCESS";
	private static final String MARKER_FIDO2_IMPORT_FAILED = "PUBKEY_FIDO2_IMPORT_FAILED";
	private static final String MARKER_FIDO2_IMPORT_PIN_REQUIRED = "PUBKEY_FIDO2_IMPORT_PIN_REQUIRED";
	private static final String MARKER_FIDO2_IMPORT_RP_NOT_FOUND = "PUBKEY_FIDO2_IMPORT_RP_NOT_FOUND";
	private static final String MARKER_FIDO2_IMPORT_CREDENTIALS_EMPTY = "PUBKEY_FIDO2_IMPORT_CREDENTIALS_EMPTY";
	private static final String MARKER_OPENPGP_IMPORT_START = "PUBKEY_OPENPGP_IMPORT_START";
	private static final String MARKER_OPENPGP_IMPORT_DISCOVERED = "PUBKEY_OPENPGP_IMPORT_DISCOVERED";
	private static final String MARKER_OPENPGP_IMPORT_SUCCESS = "PUBKEY_OPENPGP_IMPORT_SUCCESS";
	private static final String MARKER_OPENPGP_IMPORT_FAILED = "PUBKEY_OPENPGP_IMPORT_FAILED";
	private static final String MARKER_IMPORT_WAIT_UI_SHOW = "PUBKEY_IMPORT_WAIT_UI_SHOW";
	private static final String MARKER_IMPORT_WAIT_UI_HIDE = "PUBKEY_IMPORT_WAIT_UI_HIDE";
	private static final String MARKER_IMPORT_WAIT_UI_CANCELLED = "PUBKEY_IMPORT_WAIT_UI_CANCELLED";
	private static final String IMPORT_PROVIDER_OPENPGP = "openpgp";
	private static final String IMPORT_PROVIDER_PIV = "piv";
	private static final String IMPORT_PROVIDER_FIDO2 = "fido2";
	private static final String YUBICO_INFO_URL = "https://www.yubico.com/products/yubikey-5-overview/";
	private String mLastPublicKeyExportFailure = "unknown";

	// Constants for AndExplorer's file picking intent
	private static final String ANDEXPLORER_TITLE = "explorer_title";
	private static final String MIME_TYPE_ANDEXPLORER_FILE = "vnd.android.cursor.dir/lysesoft.andexplorer.file";

	protected ClipboardManager clipboard;

	private TerminalManager bound = null;

	private WebView webView;
	private BottomSheetBehavior bottomSheetBehavior;
	private volatile boolean mPivImportActive;
	private volatile boolean mPivImportHandled;
	private volatile boolean mFido2ImportActive;
	private volatile boolean mFido2ImportHandled;
	private volatile boolean mOpenPgpImportActive;
	private volatile boolean mOpenPgpImportHandled;
	private String mPendingFido2Application;
	private char[] mPendingFido2Pin;
	private AlertDialog mImportWaitDialog;
	private String mImportWaitDialogProvider;
	private YubiKitManager mImportYubiKitManager;
	private YubiKitManager mOpenPgpImportYubiKitManager;

	private void startYubiKitImportDiscovery(boolean forPiv) {
		if (mImportYubiKitManager == null) {
			mImportYubiKitManager = new YubiKitManager(getApplicationContext());
		}
		stopYubiKitImportDiscovery();
		try {
			mImportYubiKitManager.startNfcDiscovery(new NfcConfiguration(), this,
					device -> importSecurityKeyFromDevice(device, forPiv));
		} catch (NfcNotAvailable e) {
			// NFC not available; USB discovery via UsbConfiguration will handle it.
		}
		mImportYubiKitManager.startUsbDiscovery(new UsbConfiguration(),
				device -> importSecurityKeyFromDevice(device, forPiv));
	}

	private void startOpenPgpImportDiscovery(@Nullable OpenPgpKeyAlgorithmConfig algorithmConfig) {
		if (mOpenPgpImportYubiKitManager == null) {
			mOpenPgpImportYubiKitManager = new YubiKitManager(getApplicationContext());
		}
		stopOpenPgpImportDiscovery();
		try {
			mOpenPgpImportYubiKitManager.startNfcDiscovery(new NfcConfiguration(), this,
					device -> importOpenPgpFromDevice(device, algorithmConfig));
		} catch (NfcNotAvailable ignored) {
			// NFC not available; USB discovery via UsbConfiguration will handle it.
		}
		mOpenPgpImportYubiKitManager.startUsbDiscovery(new UsbConfiguration(),
				device -> importOpenPgpFromDevice(device, algorithmConfig));
	}

	private void importSecurityKeyFromDevice(
			@NonNull com.yubico.yubikit.core.YubiKeyDevice device,
			boolean forPiv) {
		if (forPiv) {
			try (SmartCardConnection conn = device.openConnection(SmartCardConnection.class)) {
				if (!mPivImportActive || mPivImportHandled) {
					return;
				}
				mPivImportHandled = true;
				SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PIV_IMPORT_DISCOVERED);
				importPivSecurityKeyFromConnection(conn);
				return;
			} catch (Exception e) {
				String detail = e.getClass().getSimpleName();
				if (e.getMessage() != null && !e.getMessage().isEmpty()) {
					detail += "(" + e.getMessage() + ")";
				}
				if (!mPivImportActive || mPivImportHandled) {
					return;
				}
				mPivImportHandled = true;
				mPivImportActive = false;
				stopYubiKitImportDiscovery();
				dismissImportWaitDialog();
				SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PIV_IMPORT_FAILED, detail);
				runOnUiThread(() -> Toast.makeText(
						PubkeyListActivity.this,
						R.string.pubkey_add_piv_failed,
						Toast.LENGTH_LONG).show());
				return;
			}
		}

		try {
			if (!mFido2ImportActive || mFido2ImportHandled) {
				return;
			}
			mFido2ImportHandled = true;
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_IMPORT_DISCOVERED);
			importFido2SecurityKeyFromDevice(device);
		} catch (Exception e) {
			String detail = e.getClass().getSimpleName();
			if (e.getMessage() != null && !e.getMessage().isEmpty()) {
				detail += "(" + e.getMessage() + ")";
			}
			if (!mFido2ImportActive || mFido2ImportHandled) {
				return;
			}
			mFido2ImportHandled = true;
			mFido2ImportActive = false;
			stopYubiKitImportDiscovery();
			dismissImportWaitDialog();
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_IMPORT_FAILED, detail);
			clearPendingFido2Pin();
			mPendingFido2Application = null;
			runOnUiThread(() -> Toast.makeText(
					PubkeyListActivity.this,
					R.string.pubkey_add_fido2_failed,
					Toast.LENGTH_LONG).show());
		}
	}

	private void importOpenPgpFromDevice(
			@NonNull com.yubico.yubikit.core.YubiKeyDevice device,
			@Nullable OpenPgpKeyAlgorithmConfig algorithmConfig) {
		boolean started = false;
		try (SmartCardConnection conn = device.openConnection(SmartCardConnection.class)) {
			if (!mOpenPgpImportActive || mOpenPgpImportHandled) {
				return;
			}
			mOpenPgpImportHandled = true;
			started = true;
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_IMPORT_DISCOVERED);
			importOpenPgpFromConnection(conn, algorithmConfig);
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_IMPORT_SUCCESS);
		} catch (Exception e) {
			if (!started && !mOpenPgpImportActive) {
				return;
			}
			if (!started) {
				mOpenPgpImportHandled = true;
				started = true;
			}
			String detail = e.getClass().getSimpleName();
			if (e.getMessage() != null && !e.getMessage().isEmpty()) {
				detail += "(" + e.getMessage() + ")";
			}
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_OPENPGP_IMPORT_FAILED, detail);
			runOnUiThread(() -> Toast.makeText(
					PubkeyListActivity.this,
					getString(R.string.pubkey_add_openpgp_failed, e.getMessage()),
					Toast.LENGTH_LONG).show());
		} finally {
			if (started) {
				mOpenPgpImportActive = false;
				mOpenPgpImportHandled = false;
				stopOpenPgpImportDiscovery();
				dismissImportWaitDialog();
			}
		}
	}

	private void stopYubiKitImportDiscovery() {
		if (mImportYubiKitManager != null) {
			mImportYubiKitManager.stopNfcDiscovery(this);
			mImportYubiKitManager.stopUsbDiscovery();
		}
	}

	private void stopOpenPgpImportDiscovery() {
		if (mOpenPgpImportYubiKitManager != null) {
			mOpenPgpImportYubiKitManager.stopNfcDiscovery(this);
			mOpenPgpImportYubiKitManager.stopUsbDiscovery();
		}
	}

	private void showImportWaitDialog(@NonNull String provider, @NonNull String message) {
		runOnUiThread(() -> {
			if (isFinishing()) {
				return;
			}

			if (mImportWaitDialog != null && mImportWaitDialog.isShowing()) {
				TextView messageView = mImportWaitDialog.findViewById(R.id.security_key_wait_message);
				if (messageView != null) {
					messageView.setText(message);
				}
				mImportWaitDialogProvider = provider;
				return;
			}

			View dialogView = getLayoutInflater().inflate(R.layout.dia_security_key_wait_for_touch, null);
			TextView messageView = dialogView.findViewById(R.id.security_key_wait_message);
			messageView.setText(message);

			AlertDialog dialog = new AlertDialog.Builder(PubkeyListActivity.this, R.style.AlertDialogTheme)
					.setTitle(R.string.security_key_wait_dialog_title)
					.setView(dialogView)
					.setCancelable(true)
					.setNegativeButton(android.R.string.cancel, (dialogInterface, which) ->
							onImportWaitDialogCancelled(provider))
					.create();
			dialog.setOnCancelListener(dialogInterface -> onImportWaitDialogCancelled(provider));
			dialog.show();

			mImportWaitDialog = dialog;
			mImportWaitDialogProvider = provider;
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_IMPORT_WAIT_UI_SHOW,
					"provider=" + provider);
		});
	}

	private void dismissImportWaitDialog() {
		runOnUiThread(() -> {
			if (mImportWaitDialog == null) {
				return;
			}
			String provider = mImportWaitDialogProvider;
			if (mImportWaitDialog.isShowing()) {
				mImportWaitDialog.dismiss();
			}
			mImportWaitDialog = null;
			mImportWaitDialogProvider = null;
			if (provider != null) {
				SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_IMPORT_WAIT_UI_HIDE,
						"provider=" + provider);
			}
		});
	}

	private void onImportWaitDialogCancelled(@NonNull String provider) {
		boolean wasActive = cancelSecurityKeyImport(provider);
		if (wasActive) {
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_IMPORT_WAIT_UI_CANCELLED,
					"provider=" + provider);
		}
		dismissImportWaitDialog();
	}

	private boolean cancelSecurityKeyImport(@NonNull String provider) {
		if (IMPORT_PROVIDER_PIV.equals(provider)) {
			if (!mPivImportActive) {
				return false;
			}
			mPivImportActive = false;
			mPivImportHandled = false;
			stopYubiKitImportDiscovery();
			return true;
		}
		if (IMPORT_PROVIDER_FIDO2.equals(provider)) {
			if (!mFido2ImportActive) {
				return false;
			}
			mFido2ImportActive = false;
			mFido2ImportHandled = false;
			clearPendingFido2Pin();
			mPendingFido2Application = null;
			stopYubiKitImportDiscovery();
			return true;
		}
		if (!mOpenPgpImportActive) {
			return false;
		}
		mOpenPgpImportActive = false;
		mOpenPgpImportHandled = false;
		stopOpenPgpImportDiscovery();
		return true;
	}


	private ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			bound = ((TerminalManager.TerminalBinder) service).getService();

			// update our listview binder to find the service
			updateList();
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			bound = null;
			updateList();
		}
	};

	@Override
	public void onStart() {
		super.onStart();

		bindService(new Intent(this, TerminalManager.class), connection, Context.BIND_AUTO_CREATE);

		updateList();
	}

	@Override
	public void onStop() {
		super.onStop();

		unbindService(connection);
		mPivImportActive = false;
		mPivImportHandled = false;
		mFido2ImportActive = false;
		mFido2ImportHandled = false;
		mOpenPgpImportActive = false;
		mOpenPgpImportHandled = false;
		stopYubiKitImportDiscovery();
		stopOpenPgpImportDiscovery();
		dismissImportWaitDialog();
		clearPendingFido2Pin();
		mPendingFido2Application = null;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.act_pubkeylist_shop);

		mListView = findViewById(R.id.list);
		mListView.setHasFixedSize(true);
		mListView.setLayoutManager(new LinearLayoutManager(this));
		mListView.addItemDecoration(new ListItemDecoration(this));

		mEmptyView = findViewById(R.id.empty);

		registerForContextMenu(mListView);

		clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

		setupShop();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		if (mPivImportActive || mFido2ImportActive || mOpenPgpImportActive) {
			dispatchSecurityKeyIntent(intent);
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void setupShop() {
		ConstraintLayout bottomSheet = findViewById(R.id.shopBottomSheet);
		bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

		boolean showShop = getSharedPreferences("termbot_preference", MODE_PRIVATE).getBoolean("termbot_showshop", true);
		if (!showShop) {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
			return;
		}

		webView = findViewById(R.id.webView);
		webView.setWebViewClient(new WebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl(YUBICO_INFO_URL);

		ImageView shopImageCard = findViewById(R.id.webImageCard);

		ImageButton shopMenuButton = findViewById(R.id.webMenuButton);
		shopMenuButton.setOnClickListener(this::showShopMenu);

		bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View view, int state) {
			}

			@Override
			public void onSlide(@NonNull View view, float slideOffset) {
				shopImageCard.setAlpha(1 - slideOffset);
			}
		});

		View shopViewClick = findViewById(R.id.webViewClick);
		shopViewClick.setOnClickListener(v -> {
			if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
				bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			} else {
				bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (webView.canGoBack()) {
					webView.goBack();
				} else {
					finish();
				}
				return true;
			}

		}
		return super.onKeyDown(keyCode, event);
	}

	public void showShopMenu(View v) {
		PopupMenu popup = new PopupMenu(this, v);
		popup.setOnMenuItemClickListener(item -> {
			switch (item.getItemId()) {
			case R.id.webOpenInBrowser:
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(YUBICO_INFO_URL));
				startActivity(i);
				return true;
			case R.id.webShare:
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, "YubiKey information: " + YUBICO_INFO_URL);
				sendIntent.setType("text/plain");
				startActivity(sendIntent);
				return true;
			case R.id.webHide:
				bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
				getSharedPreferences("termbot_preference", MODE_PRIVATE)
						.edit()
						.putBoolean("termbot_showshop", false)
						.apply();
				return true;
			default:
				return false;
			}
		});
		MenuInflater inflater = popup.getMenuInflater();
		inflater.inflate(R.menu.web_menu, popup.getMenu());
		popup.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.pubkey_list_activity_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_new_key_icon:
			showAddKeyActions();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showAddKeyActions() {
		if (!shouldUseBottomSheetForAddKeyActions()) {
			showDesktopAddKeyActionsDialog();
			return;
		}

		try {
			PubkeyAddBottomSheetDialog existing = (PubkeyAddBottomSheetDialog)
					getSupportFragmentManager().findFragmentByTag(PubkeyAddBottomSheetDialog.TAG);
			if (existing != null) {
				return;
			}
			PubkeyAddBottomSheetDialog addDialog = PubkeyAddBottomSheetDialog.newInstance();
			addDialog.show(getSupportFragmentManager(), PubkeyAddBottomSheetDialog.TAG);
		} catch (IllegalStateException e) {
			// Desktop/freeform window transitions can reject fragment transactions.
			showDesktopAddKeyActionsDialog();
		}
	}

	private boolean shouldUseBottomSheetForAddKeyActions() {
		return getResources().getConfiguration().screenWidthDp < DESKTOP_ADD_KEY_MIN_WIDTH_DP;
	}

	private void showDesktopAddKeyActionsDialog() {
		final CharSequence[] actions = new CharSequence[] {
				getString(R.string.pubkey_add_new),
				getString(R.string.pubkey_import_existing),
				getString(R.string.pubkey_add_security_key),
				getString(R.string.pubkey_add_piv_security_key),
				getString(R.string.pubkey_add_fido2_security_key),
				getString(R.string.pubkey_setup_security_key)
		};

		new AlertDialog.Builder(this, R.style.AlertDialogTheme)
				.setTitle(R.string.pubkey_add_new)
				.setItems(actions, (dialog, which) -> {
					switch (which) {
					case 0:
						startActivity(new Intent(this, GeneratePubkeyActivity.class));
						break;
					case 1:
						importExistingKey();
						break;
					case 2:
						addSecurityKey();
						break;
					case 3:
						addPivSecurityKey();
						break;
					case 4:
						addFido2SecurityKey();
						break;
					case 5:
						showDesktopOpenPgpSetupDialog();
						break;
					default:
						break;
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showDesktopOpenPgpSetupDialog() {
		final CharSequence[] algorithms = getResources().getTextArray(R.array.securitykey_setup_spinner);
		final int[] selected = new int[] { 0 };
		new AlertDialog.Builder(this, R.style.AlertDialogTheme)
				.setTitle(R.string.pubkey_setup_security_key)
				.setSingleChoiceItems(algorithms, selected[0], (dialog, which) -> selected[0] = which)
				.setPositiveButton(android.R.string.ok, (dialog, which) ->
						setupSecurityKey(getOpenPgpSetupAlgorithm(selected[0])))
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private OpenPgpKeyAlgorithmConfig getOpenPgpSetupAlgorithm(int selectedPosition) {
		switch (selectedPosition) {
		case 0:
			return OpenPgpKeyAlgorithmConfig.CURVE25519_GENERATE_ON_HARDWARE;
		case 1:
			return OpenPgpKeyAlgorithmConfig.NIST_P256_GENERATE_ON_HARDWARE;
		case 2:
			return OpenPgpKeyAlgorithmConfig.NIST_P384_GENERATE_ON_HARDWARE;
		case 3:
			return OpenPgpKeyAlgorithmConfig.NIST_P521_GENERATE_ON_HARDWARE;
		case 4:
			return OpenPgpKeyAlgorithmConfig.RSA_2048_UPLOAD;
		default:
			return OpenPgpKeyAlgorithmConfig.CURVE25519_GENERATE_ON_HARDWARE;
		}
	}

	@Override
	public void onBottomSheetAddKey() {
		startActivity(new Intent(this, GeneratePubkeyActivity.class));
	}

	@Override
	public void onBottomSheetImportKey() {
		importExistingKey();
	}

	@Override
	public void onBottomSheetAddSecurityKey() {
		addSecurityKey();
	}

	@Override
	public void onBottomSheetAddPivSecurityKey() {
		addPivSecurityKey();
	}

	@Override
	public void onBottomSheetAddFido2SecurityKey() {
		addFido2SecurityKey();
	}

	@Override
	public void onBottomSheetSetupSecurityKey(OpenPgpKeyAlgorithmConfig algorithmConfig) {
		setupSecurityKey(algorithmConfig);
	}

	private boolean importExistingKey() {
		Uri sdcard = Uri.fromFile(Environment.getExternalStorageDirectory());
		String pickerTitle = getString(R.string.pubkey_list_pick);

		if (Build.VERSION.SDK_INT >= 19 && importExistingKeyKitKat()) {
			return true;
		} else {
			return importExistingKeyOpenIntents(sdcard, pickerTitle)
					|| importExistingKeyAndExplorer(sdcard, pickerTitle) || pickFileSimple();
		}
	}

	private void addPivSecurityKey() {
		if (mPivImportActive) {
			Toast.makeText(this, R.string.pubkey_add_piv_in_progress, Toast.LENGTH_LONG).show();
			return;
		}
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_PIV_IMPORT_START);
		mPivImportActive = true;
		mPivImportHandled = false;
		showImportWaitDialog(IMPORT_PROVIDER_PIV, getString(R.string.pubkey_add_piv_wait_for_key));
		startYubiKitImportDiscovery(true);
	}

	private void addFido2SecurityKey() {
		final LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		int padding = Math.round(getResources().getDisplayMetrics().density * 16f);
		container.setPadding(padding, padding / 2, padding, 0);

		final EditText applicationInput = new EditText(this);
		applicationInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		applicationInput.setHint(getString(R.string.pubkey_add_fido2_application_hint));
		applicationInput.setText(SshSkPublicKey.APPLICATION_DEFAULT);
		container.addView(applicationInput);

		final EditText pinInput = new EditText(this);
		pinInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		pinInput.setHint(getString(R.string.pubkey_add_fido2_pin_hint));
		container.addView(pinInput);

		final EditText keyLineInput = new EditText(this);
		keyLineInput.setInputType(
				InputType.TYPE_CLASS_TEXT
						| InputType.TYPE_TEXT_FLAG_MULTI_LINE
						| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		keyLineInput.setMinLines(2);
		keyLineInput.setHint(getString(R.string.pubkey_add_fido2_keyline_hint));
		container.addView(keyLineInput);

		new androidx.appcompat.app.AlertDialog.Builder(this, R.style.AlertDialogTheme)
				.setTitle(R.string.pubkey_add_fido2_security_key)
				.setMessage(R.string.pubkey_add_fido2_prompt_message)
				.setView(container)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String keyLine = keyLineInput.getText().toString().trim();
						if (keyLine.startsWith("sk-") && keyLine.contains(" ")) {
							importFido2SecurityKeyFromLine(keyLine);
							return;
						}

						String applicationId = SshSkPublicKey.normalizeApplication(applicationInput.getText().toString());
						char[] pinChars = pinInput.getText().toString().toCharArray();
						if (pinChars.length == 0) {
							SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_IMPORT_PIN_REQUIRED);
							Toast.makeText(PubkeyListActivity.this, R.string.pubkey_add_fido2_pin_required, Toast.LENGTH_LONG).show();
							return;
						}
						startFido2SecurityKeyImport(applicationId, pinChars);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create()
				.show();
	}

	private void startFido2SecurityKeyImport(String applicationId, char[] pinChars) {
		if (mFido2ImportActive) {
			Toast.makeText(this, R.string.pubkey_add_fido2_in_progress, Toast.LENGTH_LONG).show();
			Arrays.fill(pinChars, '\0');
			return;
		}
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_IMPORT_START, "application=" + applicationId);

		mFido2ImportActive = true;
		mFido2ImportHandled = false;
		mPendingFido2Application = applicationId;
		clearPendingFido2Pin();
		mPendingFido2Pin = Arrays.copyOf(pinChars, pinChars.length);
		Arrays.fill(pinChars, '\0');

		showImportWaitDialog(
				IMPORT_PROVIDER_FIDO2,
				getString(R.string.pubkey_add_fido2_wait_for_key, applicationId));
		startYubiKitImportDiscovery(false);
	}

	private void dispatchSecurityKeyIntent(@Nullable Intent intent) {
		// YubiKit handles NFC/USB dispatch via lifecycle — no manual dispatch needed.
	}

	private void addSecurityKey() {
		startOpenPgpImport(null);
	}

	private void setupSecurityKey(OpenPgpKeyAlgorithmConfig algorithmConfig) {
		startOpenPgpImport(algorithmConfig);
	}

	private void startOpenPgpImport(@Nullable OpenPgpKeyAlgorithmConfig algorithmConfig) {
		if (mOpenPgpImportActive) {
			Toast.makeText(this, R.string.pubkey_add_openpgp_in_progress, Toast.LENGTH_LONG).show();
			return;
		}
		mOpenPgpImportActive = true;
		mOpenPgpImportHandled = false;
		SecurityKeyDebugLog.logFlow(
				getApplicationContext(),
				TAG,
				MARKER_OPENPGP_IMPORT_START,
				algorithmConfig == null ? "mode=import" : "mode=setup");
		showImportWaitDialog(IMPORT_PROVIDER_OPENPGP, getString(R.string.pubkey_add_openpgp_tap_key));
		startOpenPgpImportDiscovery(algorithmConfig);
	}

    /** @param algorithmConfig null → import existing auth key; non-null → generate/upload key first */
    private void importOpenPgpFromConnection(SmartCardConnection conn,
            @Nullable OpenPgpKeyAlgorithmConfig algorithmConfig) throws Exception {
        try (com.yubico.yubikit.openpgp.OpenPgpSession session =
                new com.yubico.yubikit.openpgp.OpenPgpSession(conn)) {
            com.yubico.yubikit.openpgp.ApplicationRelatedData data =
                    session.getApplicationRelatedData();
            String serial = String.valueOf(data.getAid().getSerial());
            String nickname = serial != null && !serial.isEmpty() && !"0".equals(serial)
                    ? "YubiKey OpenPGP (" + serial + ")"
                    : "YubiKey OpenPGP";

            // Read the authentication sub-key public key from the card.
            com.yubico.yubikit.core.keys.PublicKeyValues pkv =
                    session.getPublicKey(com.yubico.yubikit.openpgp.KeyRef.AUT);
            java.security.PublicKey publicKey = pkv.toPublicKey();

            final String finalNickname = nickname;
            final java.security.PublicKey finalPublicKey = publicKey;
            runOnUiThread(() -> {
                addSecurityKey(finalPublicKey, finalNickname);
                Toast.makeText(PubkeyListActivity.this,
                        getString(R.string.pubkey_add_openpgp_success, finalNickname),
                        Toast.LENGTH_LONG).show();
            });
        }
    }

	private String normalizeSecurityKeyName(String securityKeyName) {
		if (securityKeyName == null || securityKeyName.isEmpty()) {
			return getString(R.string.security_key_unknown_device_name);
		}
		return securityKeyName;
	}

	private void addSecurityKey(PublicKey publicKey, String nickname) {
		PubkeyBean pubkey = new PubkeyBean();
		pubkey.setSecurityKey(true);
		pubkey.setPublicKey(publicKey.getEncoded());
		pubkey.setPrivateKey(SecurityKeyProviderProfile.DEFAULT_OPENPGP_AUTH.toStorageBlob());
		pubkey.setNickname(nickname);
		String algorithm = convertAlgorithmName(publicKey.getAlgorithm());
		pubkey.setType(algorithm);

		PubkeyDatabase pubkeyDb = PubkeyDatabase.get(this);
		pubkeyDb.savePubkey(pubkey);

		updateList();
	}

	private void importFido2SecurityKeyFromLine(String openSshPublicKeyLine) {
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_IMPORT_START, "manual_key_line");
		try {
			SshSkPublicKey.ParsedAuthorizedKeyLine parsed =
					SshSkPublicKey.parseAuthorizedKeyLine(openSshPublicKeyLine);
			String nickname = parsed.comment;
			if (nickname == null || nickname.trim().isEmpty()) {
				nickname = "YubiKey FIDO2 (" + parsed.publicKey.getSshKeyType() + ")";
			}
			saveFido2SecurityKey(parsed.publicKey, nickname);
			SecurityKeyDebugLog.logFlow(
					getApplicationContext(),
					TAG,
					MARKER_FIDO2_IMPORT_SUCCESS,
					"type=" + parsed.publicKey.getSshKeyType() + " app=" + parsed.publicKey.getApplication());
			Toast.makeText(
					this,
					getString(R.string.pubkey_add_fido2_success, parsed.publicKey.getSshKeyType()),
					Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			SecurityKeyDebugLog.logFlow(
					getApplicationContext(),
					TAG,
					MARKER_FIDO2_IMPORT_FAILED,
					e.getClass().getSimpleName());
			Toast.makeText(this, R.string.pubkey_add_fido2_failed, Toast.LENGTH_LONG).show();
		}
	}

	private void importFido2SecurityKeyFromDevice(@NonNull com.yubico.yubikit.core.YubiKeyDevice device) {
		String applicationId = SshSkPublicKey.normalizeApplication(mPendingFido2Application);
		char[] pinChars = mPendingFido2Pin == null
				? new char[0]
				: Arrays.copyOf(mPendingFido2Pin, mPendingFido2Pin.length);
		int importedCount = 0;

		try {
			if (pinChars.length == 0) {
				SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_FIDO2_IMPORT_PIN_REQUIRED);
				throw new IOException("FIDO2 PIN is required for credential management");
			}

			try (Ctap2Session ctap2Session = openFido2Session(device)) {
				Ctap2Session.InfoData info = ctap2Session.getInfo();
				if (!CredentialManagement.isSupported(info)) {
					throw new IOException("Credential management is not supported by this FIDO2 token");
				}

				PinUvAuthProtocolV1 pinUvAuthProtocol = new PinUvAuthProtocolV1();
				ClientPin clientPin = new ClientPin(ctap2Session, pinUvAuthProtocol);
				byte[] pinToken = clientPin.getPinToken(pinChars, ClientPin.PIN_PERMISSION_CM, applicationId);

				CredentialManagement credentialManagement =
						new CredentialManagement(ctap2Session, pinUvAuthProtocol, pinToken);
				List<CredentialManagement.CredentialData> credentials =
						findFido2CredentialsForApplication(credentialManagement, applicationId);
				if (credentials.isEmpty()) {
					SecurityKeyDebugLog.logFlow(
							getApplicationContext(),
							TAG,
							MARKER_FIDO2_IMPORT_CREDENTIALS_EMPTY,
							"application=" + applicationId);
					throw new IOException("No credentials found for application " + applicationId);
				}

				int index = 1;
				for (CredentialManagement.CredentialData credential : credentials) {
					PublicKey credentialPublicKey = decodeFido2CredentialPublicKey(credential);
					SshSkPublicKey sshSkPublicKey = SshSkPublicKey.fromPublicKey(credentialPublicKey, applicationId);
					String nickname = credentials.size() == 1
							? "YubiKey FIDO2 (" + sshSkPublicKey.getSshKeyType() + ", " + applicationId + ")"
							: "YubiKey FIDO2 (" + sshSkPublicKey.getSshKeyType() + ", " + applicationId + ", #" + index + ")";
					saveFido2SecurityKey(sshSkPublicKey, nickname);
					importedCount++;
					index++;
				}
			}

			final int finalImportedCount = importedCount;
			SecurityKeyDebugLog.logFlow(
					getApplicationContext(),
					TAG,
					MARKER_FIDO2_IMPORT_SUCCESS,
					"application=" + applicationId + " imported=" + finalImportedCount);
			runOnUiThread(() -> Toast.makeText(
					PubkeyListActivity.this,
					getString(R.string.pubkey_add_fido2_success_count, finalImportedCount, applicationId),
					Toast.LENGTH_LONG).show());
		} catch (Exception e) {
			SecurityKeyDebugLog.logFlow(
					getApplicationContext(),
					TAG,
					MARKER_FIDO2_IMPORT_FAILED,
					e.getClass().getSimpleName());
			runOnUiThread(() -> Toast.makeText(
					PubkeyListActivity.this,
					getString(R.string.pubkey_add_fido2_failed_with_app, applicationId),
					Toast.LENGTH_LONG).show());
		} finally {
			Arrays.fill(pinChars, '\0');
			clearPendingFido2Pin();
			mPendingFido2Application = null;
			mFido2ImportActive = false;
			mFido2ImportHandled = false;
			stopYubiKitImportDiscovery();
			dismissImportWaitDialog();
		}
	}

	private List<CredentialManagement.CredentialData> findFido2CredentialsForApplication(
			CredentialManagement credentialManagement,
			String applicationId) throws Exception {
		List<CredentialManagement.RpData> rpDataList = credentialManagement.enumerateRps();
		if (rpDataList == null || rpDataList.isEmpty()) {
			return Collections.emptyList();
		}

		byte[] requestedRpHash = computeRpIdHash(applicationId);
		byte[] selectedRpHash = null;
		for (CredentialManagement.RpData rpData : rpDataList) {
			byte[] rpHash = rpData.getRpIdHash();
			if (rpHash != null && Arrays.equals(rpHash, requestedRpHash)) {
				selectedRpHash = rpHash;
				break;
			}
		}
		if (selectedRpHash == null) {
			for (CredentialManagement.RpData rpData : rpDataList) {
				Map<String, ?> rpMap = rpData.getRp();
				if (rpMap == null) {
					continue;
				}
				Object rpId = rpMap.get("id");
				if (applicationId.equals(String.valueOf(rpId))) {
					selectedRpHash = rpData.getRpIdHash();
					break;
				}
			}
		}
		if (selectedRpHash == null) {
			SecurityKeyDebugLog.logFlow(
					getApplicationContext(),
					TAG,
					MARKER_FIDO2_IMPORT_RP_NOT_FOUND,
					"application=" + applicationId);
			return Collections.emptyList();
		}

		List<CredentialManagement.CredentialData> credentials =
				credentialManagement.enumerateCredentials(selectedRpHash);
		return credentials == null ? Collections.emptyList() : credentials;
	}

	private Ctap2Session openFido2Session(@NonNull com.yubico.yubikit.core.YubiKeyDevice device) throws Exception {
		Exception fidoConnectionFailure = null;
		try {
			FidoConnection fidoConnection = device.openConnection(FidoConnection.class);
			return new Ctap2Session(fidoConnection);
		} catch (Exception e) {
			fidoConnectionFailure = e;
		}

		try {
			SmartCardConnection smartCardConnection = device.openConnection(SmartCardConnection.class);
			return new Ctap2Session(smartCardConnection);
		} catch (Exception e) {
			if (fidoConnectionFailure != null) {
				e.addSuppressed(fidoConnectionFailure);
			}
			throw e;
		}
	}

	private byte[] computeRpIdHash(String applicationId) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		return digest.digest(applicationId.getBytes(StandardCharsets.UTF_8));
	}

	@SuppressWarnings("unchecked")
	private PublicKey decodeFido2CredentialPublicKey(CredentialManagement.CredentialData credentialData)
			throws Exception {
		Map<?, ?> publicKeyMap = credentialData.getPublicKey();
		if (publicKeyMap == null || publicKeyMap.isEmpty()) {
			throw new IOException("FIDO2 credential does not contain public key data");
		}
		return Cose.getPublicKey((Map<Integer, ?>) (Map<?, ?>) publicKeyMap);
	}

	private void saveFido2SecurityKey(SshSkPublicKey publicKey, String nickname) {
		PubkeyBean pubkey = new PubkeyBean();
		pubkey.setSecurityKey(true);
		pubkey.setPublicKey(publicKey.getEncoded());
		pubkey.setPrivateKey(new SecurityKeyProviderProfile(
				SecurityKeyProviderProfile.PROVIDER_FIDO2,
				publicKey.getApplication()).toStorageBlob());
		pubkey.setNickname(nickname);
		pubkey.setType(publicKey.getSshKeyType());

		PubkeyDatabase pubkeyDb = PubkeyDatabase.get(this);
		pubkeyDb.savePubkey(pubkey);
		runOnUiThread(this::updateList);
	}

	private void clearPendingFido2Pin() {
		if (mPendingFido2Pin == null) {
			return;
		}
		Arrays.fill(mPendingFido2Pin, '\0');
		mPendingFido2Pin = null;
	}

	private void importPivSecurityKeyFromConnection(SmartCardConnection rawConnection) {
		PublicKey publicKey;
		String slotReference = SecurityKeyProviderProfile.SLOT_PIV_AUTHENTICATION;
		String nickname;
		try {
			try (PivSession pivSession = new PivSession(rawConnection)) {
				Slot slot = Slot.AUTHENTICATION;
				slotReference = PivSupport.describeSlot(slot);
				publicKey = PivSupport.readPublicKey(pivSession, slot);
				int serial = -1;
				try {
					serial = pivSession.getSerialNumber();
				} catch (Exception ignored) {
					// Serial isn't available on all firmware.
				}
				nickname = serial > 0 ? "YubiKey PIV (" + serial + ")" : "YubiKey PIV";
			}

			savePivSecurityKey(publicKey, nickname, slotReference);
			SecurityKeyDebugLog.logFlow(
					getApplicationContext(),
					TAG,
					MARKER_PIV_IMPORT_SUCCESS,
					"slot=" + slotReference + " type=" + publicKey.getAlgorithm());
			final String importedSlot = slotReference;
			runOnUiThread(() -> Toast.makeText(
					PubkeyListActivity.this,
					getString(R.string.pubkey_add_piv_success, importedSlot),
					Toast.LENGTH_LONG).show());
		} catch (Exception e) {
			SecurityKeyDebugLog.logFlow(
					getApplicationContext(),
					TAG,
					MARKER_PIV_IMPORT_FAILED,
					e.getClass().getSimpleName());
			runOnUiThread(() -> Toast.makeText(
					PubkeyListActivity.this,
					R.string.pubkey_add_piv_failed,
					Toast.LENGTH_LONG).show());
		} finally {
			mPivImportActive = false;
			mPivImportHandled = false;
			stopYubiKitImportDiscovery();
			dismissImportWaitDialog();
		}
	}

	private void savePivSecurityKey(PublicKey publicKey, String nickname, String slotReference) {
		PubkeyBean pubkey = new PubkeyBean();
		pubkey.setSecurityKey(true);
		pubkey.setPublicKey(publicKey.getEncoded());
		pubkey.setPrivateKey(new SecurityKeyProviderProfile(
				SecurityKeyProviderProfile.PROVIDER_PIV,
				slotReference).toStorageBlob());
		pubkey.setNickname(nickname);
		pubkey.setType(convertAlgorithmName(publicKey.getAlgorithm()));

		PubkeyDatabase pubkeyDb = PubkeyDatabase.get(this);
		pubkeyDb.savePubkey(pubkey);
		runOnUiThread(this::updateList);
	}

	/**
	 * Fires an intent to spin up the "file chooser" UI and select a private key.
	 */
	@TargetApi(19)
	public boolean importExistingKeyKitKat() {
		// ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
		// browser.
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

		// Filter to only show results that can be "opened", such as a
		// file (as opposed to a list of contacts or timezones)
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		// PKCS#8 MIME types aren't widely supported, so we'll try */* fro now.
		intent.setType("*/*");

		try {
			startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
			return true;
		} catch (ActivityNotFoundException e) {
			return false;
		}
	}

	/**
	 * Imports an existing key using the OpenIntents-style request.
	 */
	private boolean importExistingKeyOpenIntents(Uri sdcard, String pickerTitle) {
		// Try to use OpenIntent's file browser to pick a file
		Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
		intent.setData(sdcard);
		intent.putExtra(FileManagerIntents.EXTRA_TITLE, pickerTitle);
		intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getString(android.R.string.ok));

		try {
			startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
			return true;
		} catch (ActivityNotFoundException e) {
			return false;
		}
	}

	private boolean importExistingKeyAndExplorer(Uri sdcard, String pickerTitle) {
		Intent intent;
		intent = new Intent(Intent.ACTION_PICK);
		intent.setDataAndType(sdcard, MIME_TYPE_ANDEXPLORER_FILE);
		intent.putExtra(ANDEXPLORER_TITLE, pickerTitle);

		try {
			startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
			return true;
		} catch (ActivityNotFoundException e) {
			return false;
		}
	}

	/**
	 * Builds a simple list of files to pick from.
	 */
	private boolean pickFileSimple() {
		// build list of all files in sdcard root
		final File sdcard = Environment.getExternalStorageDirectory();
		Log.d(TAG, sdcard.toString());

		// Don't show a dialog if the SD card is completely absent.
		final String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)
				&& !Environment.MEDIA_MOUNTED.equals(state)) {
			new androidx.appcompat.app.AlertDialog.Builder(
					PubkeyListActivity.this, R.style.AlertDialogTheme)
					.setMessage(R.string.alert_sdcard_absent)
					.setNegativeButton(android.R.string.cancel, null).create().show();
			return true;
		}

		List<String> names = new ArrayList<>();
		{
			File[] files = sdcard.listFiles();
			if (files != null) {
				for (File file : sdcard.listFiles()) {
					if (file.isDirectory()) continue;
					names.add(file.getName());
				}
			}
		}
		Collections.sort(names);

		final String[] namesList = names.toArray(new String[] {});
		Log.d(TAG, names.toString());

		// prompt user to select any file from the sdcard root
		new androidx.appcompat.app.AlertDialog.Builder(
				PubkeyListActivity.this, R.style.AlertDialogTheme)
				.setTitle(R.string.pubkey_list_pick)
				.setItems(namesList, new OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						String name = namesList[arg1];

						readKeyFromFile(Uri.fromFile(new File(sdcard, name)));
					}
				})
				.setNegativeButton(android.R.string.cancel, null).create().show();

		return true;
	}

	protected void handleAddKey(final PubkeyBean pubkey) {
		if (pubkey.isEncrypted()) {
			final View view = View.inflate(this, R.layout.dia_password, null);
			final EditText passwordField = view.findViewById(android.R.id.text1);

			new androidx.appcompat.app.AlertDialog.Builder(
					PubkeyListActivity.this, R.style.AlertDialogTheme)
					.setView(view)
					.setPositiveButton(R.string.pubkey_unlock, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							handleAddKey(pubkey, passwordField.getText().toString());
						}
					})
					.setNegativeButton(android.R.string.cancel, null).create().show();
		} else {
			handleAddKey(pubkey, null);
		}
	}

	protected void handleAddKey(PubkeyBean keybean, String password) {
		KeyPair pair = null;
		try {
			pair = PubkeyUtils.convertToKeyPair(keybean, password);
		} catch (PubkeyUtils.BadPasswordException e) {
			String message = getResources().getString(R.string.pubkey_failed_add, keybean.getNickname());
			Toast.makeText(PubkeyListActivity.this, message, Toast.LENGTH_LONG).show();
		}

		if (pair == null) {
			return;
		}

		Log.d(TAG, String.format("Unlocked key '%s'", keybean.getNickname()));

		// save this key in memory
		bound.addKey(keybean, pair, true);

		updateList();
	}

	protected void updateList() {
		PubkeyDatabase pubkeyDb = PubkeyDatabase.get(PubkeyListActivity.this);

		mAdapter = new PubkeyAdapter(this, pubkeyDb.allPubkeys());
		mListView.setAdapter(mAdapter);
		adjustViewVisibility();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		super.onActivityResult(requestCode, resultCode, resultData);

		switch (requestCode) {
		case REQUEST_CODE_PICK_FILE:
			if (resultCode == RESULT_OK && resultData != null) {
				Uri uri = resultData.getData();
				try {
					if (uri != null) {
						readKeyFromFile(uri);
					} else {
						String filename = resultData.getDataString();
						if (filename != null) {
							readKeyFromFile(Uri.parse(filename));
						}
					}
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "Couldn't read from picked file", e);
				}
			}
			break;
		}
	}

	public static byte[] getBytesFromInputStream(InputStream is, int maxSize) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[0xFFFF];

		for (int len; (len = is.read(buffer)) != -1 && os.size() < maxSize; ) {
			os.write(buffer, 0, len);
		}

		if (os.size() >= maxSize) {
			throw new IOException("File was too big");
		}

		os.flush();
		return os.toByteArray();
	}

	private KeyPair readPKCS8Key(byte[] keyData) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(keyData)));

		// parse the actual key once to check if its encrypted
		// then save original file contents into our database
		try {
			ByteArrayOutputStream keyBytes = new ByteArrayOutputStream();

			String line;
			boolean inKey = false;
			while ((line = reader.readLine()) != null) {
				if (line.equals(PubkeyUtils.PKCS8_START)) {
					inKey = true;
				} else if (line.equals(PubkeyUtils.PKCS8_END)) {
					break;
				} else if (inKey) {
					keyBytes.write(line.getBytes("US-ASCII"));
				}
			}

			if (keyBytes.size() > 0) {
				byte[] decoded = Base64.decode(keyBytes.toString().toCharArray());

				return PubkeyUtils.recoverKeyPair(decoded);
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	/**
	 * @param uri URI to private key to read.
	 */
	private void readKeyFromFile(Uri uri) {
		PubkeyBean pubkey = new PubkeyBean();

		// find the exact file selected
		pubkey.setNickname(uri.getLastPathSegment());

		byte[] keyData;
		try {
			ContentResolver resolver = getContentResolver();
			keyData = getBytesFromInputStream(resolver.openInputStream(uri), MAX_KEYFILE_SIZE);
		} catch (IOException e) {
			Toast.makeText(PubkeyListActivity.this,
					R.string.pubkey_import_parse_problem,
					Toast.LENGTH_LONG).show();
			return;
		}

		KeyPair kp;
		if ((kp = readPKCS8Key(keyData)) != null) {
			String algorithm = convertAlgorithmName(kp.getPrivate().getAlgorithm());
			pubkey.setType(algorithm);
			pubkey.setPrivateKey(kp.getPrivate().getEncoded());
			pubkey.setPublicKey(kp.getPublic().getEncoded());
		} else {
			try {
				PEMStructure struct = PEMDecoder.parsePEM(new String(keyData).toCharArray());
				boolean encrypted = PEMDecoder.isPEMEncrypted(struct);
				pubkey.setEncrypted(encrypted);
				if (!encrypted) {
					kp = PEMDecoder.decode(struct, null);
					String algorithm = convertAlgorithmName(kp.getPrivate().getAlgorithm());
					pubkey.setType(algorithm);
					pubkey.setPrivateKey(kp.getPrivate().getEncoded());
					pubkey.setPublicKey(kp.getPublic().getEncoded());
				} else {
					pubkey.setType(PubkeyDatabase.KEY_TYPE_IMPORTED);
					pubkey.setPrivateKey(keyData);
				}
			} catch (IOException e) {
				Log.e(TAG, "Problem parsing imported private key", e);
				Toast.makeText(PubkeyListActivity.this, R.string.pubkey_import_parse_problem, Toast.LENGTH_LONG).show();
			}
		}

		// write new value into database
		PubkeyDatabase pubkeyDb = PubkeyDatabase.get(this);
		pubkeyDb.savePubkey(pubkey);

		updateList();
	}

private String convertAlgorithmName(String algorithm) {
    if ("EdDSA".equals(algorithm) || "Ed25519".equals(algorithm) || "1.3.101.112".equals(algorithm)) {
        return PubkeyDatabase.KEY_TYPE_ED25519;
    } else {
        return algorithm;
    }
}

	@Nullable
	private String buildOpenSshPublicKey(PubkeyBean pubkey) {
		if (pubkey != null && pubkey.isSecurityKey()) {
			SecurityKeyProviderProfile profile = SecurityKeyProviderProfile.fromPubkey(pubkey);
			if (profile.isFido2()) {
				try {
					SshSkPublicKey skPublicKey = SshSkPublicKey.fromBlob(pubkey.getPublicKey(), pubkey.getType());
					mLastPublicKeyExportFailure = "none";
					return skPublicKey.toOpenSshPublicKeyLine(pubkey.getNickname());
				} catch (Exception e) {
					mLastPublicKeyExportFailure = e.getClass().getSimpleName();
					SecurityKeyDebugLog.logFlow(
							getApplicationContext(),
							TAG,
							MARKER_PUBLIC_KEY_EXPORT,
							"failed type=" + pubkey.getType() + " reason=" + mLastPublicKeyExportFailure);
					return null;
				}
			}
		}

		try {
			PublicKey pk = PubkeyUtils.decodePublic(pubkey.getPublicKey(), pubkey.getType());
			mLastPublicKeyExportFailure = "none";
			return PubkeyUtils.convertToOpenSSHFormat(pk, pubkey.getNickname());
		} catch (Exception e) {
			mLastPublicKeyExportFailure = e.getClass().getSimpleName();
			if (PubkeyDatabase.KEY_TYPE_ED25519.equals(pubkey.getType())) {
				try {
					Ed25519PublicKey edPub = new Ed25519PublicKey(new X509EncodedKeySpec(pubkey.getPublicKey()));
					String nickname = pubkey.getNickname();
					if (nickname == null || nickname.length() == 0) {
						nickname = "connectbot@android";
					}
					mLastPublicKeyExportFailure = "none";
					return Ed25519Verify.ED25519_ID + " "
							+ String.valueOf(Base64.encode(Ed25519Verify.encodeSSHEd25519PublicKey(edPub)))
							+ " " + nickname;
				} catch (Exception fallbackError) {
					mLastPublicKeyExportFailure =
							e.getClass().getSimpleName() + "->" + fallbackError.getClass().getSimpleName();
				}
			}
			SecurityKeyDebugLog.logFlow(
					getApplicationContext(),
					TAG,
					MARKER_PUBLIC_KEY_EXPORT,
					"failed type=" + pubkey.getType() + " reason=" + mLastPublicKeyExportFailure);
			Log.d(TAG, "Error building OpenSSH public key", e);
			return null;
		}
	}

	private void copyToClipboard(String label, String value) {
		ClipData clip = ClipData.newPlainText(label, value);
		clipboard.setPrimaryClip(clip);
	}

	public class PubkeyViewHolder extends ItemViewHolder {
		public final ImageView icon;
		public final TextView nickname;
		public final TextView caption;

		public PubkeyBean pubkey;

		public PubkeyViewHolder(View v) {
			super(v);

			icon = v.findViewById(android.R.id.icon);
			nickname = v.findViewById(android.R.id.text1);
			caption = v.findViewById(android.R.id.text2);
		}

		@Override
		public void onClick(View v) {
			if (pubkey.isSecurityKey()) {
				return;
			}

			boolean loaded = bound != null && bound.isKeyLoaded(pubkey.getNickname());

			// handle toggling key in-memory on/off
			if (loaded) {
				bound.removeKey(pubkey.getNickname());
				updateList();
			} else {
				handleAddKey(pubkey);
			}
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			// Create menu to handle deleting and editing pubkey
			menu.setHeaderTitle(pubkey.getNickname());

			// TODO: option load/unload key from in-memory list
			// prompt for password as needed for passworded keys

			// cant change password or clipboard imported keys
			final boolean imported = PubkeyDatabase.KEY_TYPE_IMPORTED.equals(pubkey.getType());
			final boolean loaded = bound != null && bound.isKeyLoaded(pubkey.getNickname());

			MenuItem load = menu.add(loaded ? R.string.pubkey_memory_unload : R.string.pubkey_memory_load);
			load.setEnabled(!pubkey.isSecurityKey());
			load.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					if (loaded) {
						bound.removeKey(pubkey.getNickname());
						updateList();
					} else {
						handleAddKey(pubkey);
						//bound.addKey(nickname, trileadKey);
					}
					return true;
				}
			});

			MenuItem onstartToggle = menu.add(R.string.pubkey_load_on_start);
			onstartToggle.setEnabled(!pubkey.isEncrypted() && !pubkey.isSecurityKey());
			onstartToggle.setCheckable(true);
			onstartToggle.setChecked(pubkey.isStartup());
			onstartToggle.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					// toggle onstart status
					pubkey.setStartup(!pubkey.isStartup());
					PubkeyDatabase pubkeyDb = PubkeyDatabase.get(PubkeyListActivity.this);
					pubkeyDb.savePubkey(pubkey);
					updateList();
					return true;
				}
			});

			MenuItem copyPublicToClipboard = menu.add(R.string.pubkey_copy_public);
			copyPublicToClipboard.setEnabled(!imported);
			copyPublicToClipboard.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					String openSshPublic = buildOpenSshPublicKey(pubkey);
					if (openSshPublic == null || openSshPublic.trim().isEmpty()) {
						Toast.makeText(
								PubkeyListActivity.this,
								getString(R.string.pubkey_copy_public_failed_reason, mLastPublicKeyExportFailure),
								Toast.LENGTH_LONG).show();
					} else {
						copyToClipboard(getString(R.string.pubkey_copy_public), openSshPublic);
					}
					return true;
				}
			});

			MenuItem sharePublicKey = menu.add(R.string.pubkey_share_public);
			sharePublicKey.setEnabled(!imported);
			sharePublicKey.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					String openSshPublic = buildOpenSshPublicKey(pubkey);
					if (openSshPublic == null || openSshPublic.trim().isEmpty()) {
						Toast.makeText(
								PubkeyListActivity.this,
								getString(R.string.pubkey_share_public_failed_reason, mLastPublicKeyExportFailure),
								Toast.LENGTH_LONG).show();
					} else {
						Intent shareIntent = new Intent(Intent.ACTION_SEND);
						shareIntent.setType("text/plain");
						shareIntent.putExtra(Intent.EXTRA_TEXT, openSshPublic);
						startActivity(Intent.createChooser(shareIntent, getString(R.string.pubkey_share_public)));
					}
					return true;
				}
			});

			MenuItem copyPrivateToClipboard = menu.add(R.string.pubkey_copy_private);
			copyPrivateToClipboard.setEnabled((!pubkey.isEncrypted() || imported) && !pubkey.isSecurityKey());
			copyPrivateToClipboard.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					try {
						String data = null;

						if (imported)
							data = new String(pubkey.getPrivateKey());
						else {
							PrivateKey pk = PubkeyUtils.decodePrivate(pubkey.getPrivateKey(), pubkey.getType());
							data = PubkeyUtils.exportPEM(pk, null);
						}

						copyToClipboard(getString(R.string.pubkey_copy_private), data);
					} catch (Exception e) {
						Log.d(TAG, "Error copying private key", e);
					}
					return true;
				}
			});

			MenuItem changePassword = menu.add(R.string.pubkey_change_password);
			changePassword.setEnabled(!imported && !pubkey.isSecurityKey());
			changePassword.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					final View changePasswordView =
							View.inflate(PubkeyListActivity.this, R.layout.dia_changepassword, null);
					changePasswordView.findViewById(R.id.old_password_prompt)
							.setVisibility(pubkey.isEncrypted() ? View.VISIBLE : View.GONE);
					new androidx.appcompat.app.AlertDialog.Builder(
							PubkeyListActivity.this, R.style.AlertDialogTheme)
							.setView(changePasswordView)
							.setPositiveButton(R.string.button_change, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									String oldPassword = ((EditText) changePasswordView.findViewById(R.id.old_password)).getText().toString();
									String password1 = ((EditText) changePasswordView.findViewById(R.id.password1)).getText().toString();
									String password2 = ((EditText) changePasswordView.findViewById(R.id.password2)).getText().toString();

									if (!password1.equals(password2)) {
										new androidx.appcompat.app.AlertDialog.Builder(
												PubkeyListActivity.this,
												R.style.AlertDialogTheme)
												.setMessage(R.string.alert_passwords_do_not_match_msg)
												.setPositiveButton(android.R.string.ok, null)
												.create().show();
										return;
									}

									try {
										if (!pubkey.changePassword(oldPassword, password1))
											new androidx.appcompat.app.AlertDialog.Builder(
													PubkeyListActivity.this,
													R.style.AlertDialogTheme)
													.setMessage(R.string.alert_wrong_password_msg)
													.setPositiveButton(android.R.string.ok, null)
													.create().show();
										else {
											PubkeyDatabase pubkeyDb = PubkeyDatabase.get(PubkeyListActivity.this);
											pubkeyDb.savePubkey(pubkey);
											updateList();
										}
									} catch (Exception e) {
										Log.e(TAG, "Could not change private key password", e);
										new androidx.appcompat.app.AlertDialog.Builder(
												PubkeyListActivity.this,
												R.style.AlertDialogTheme)
												.setMessage(R.string.alert_key_corrupted_msg)
												.setPositiveButton(android.R.string.ok, null)
												.create().show();
									}
								}
							})
							.setNegativeButton(android.R.string.cancel, null).create().show();

					return true;
				}
			});

			MenuItem confirmUse = menu.add(R.string.pubkey_confirm_use);
			confirmUse.setCheckable(true);
			confirmUse.setChecked(pubkey.isConfirmUse());
			confirmUse.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					// toggle confirm use
					pubkey.setConfirmUse(!pubkey.isConfirmUse());
					PubkeyDatabase pubkeyDb = PubkeyDatabase.get(PubkeyListActivity.this);
					pubkeyDb.savePubkey(pubkey);
					updateList();
					return true;
				}
			});

			MenuItem rename = menu.add(R.string.pubkey_rename);
			rename.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					final EditText edittext = new EditText(PubkeyListActivity.this);
					edittext.setText(pubkey.getNickname());
					// prompt user to make sure they really want this
					new androidx.appcompat.app.AlertDialog.Builder(
							PubkeyListActivity.this, R.style.AlertDialogTheme)
							.setView(edittext)
							.setMessage(getString(R.string.rename_message, pubkey.getNickname()))
							.setPositiveButton(R.string.rename_pos, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {

									// dont forget to remove from in-memory
									if (loaded) {
										bound.removeKey(pubkey.getNickname());
									}

									// rename in database and update gui
									PubkeyDatabase pubkeyDb = PubkeyDatabase.get(PubkeyListActivity.this);
									pubkeyDb.renamePubkey(pubkey, edittext.getText().toString());
									updateList();
								}
							})
							.setNegativeButton(R.string.rename_neg, null).create().show();

					return true;
				}
			});

			MenuItem delete = menu.add(R.string.pubkey_delete);
			delete.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					// prompt user to make sure they really want this
					new androidx.appcompat.app.AlertDialog.Builder(
							PubkeyListActivity.this, R.style.AlertDialogTheme)
							.setMessage(getString(R.string.delete_message, pubkey.getNickname()))
							.setPositiveButton(R.string.delete_pos, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {

									// dont forget to remove from in-memory
									if (loaded) {
										bound.removeKey(pubkey.getNickname());
									}

									// delete from backend database and update gui
									PubkeyDatabase pubkeyDb = PubkeyDatabase.get(PubkeyListActivity.this);
									pubkeyDb.deletePubkey(pubkey);
									updateList();
								}
							})
							.setNegativeButton(R.string.delete_neg, null).create().show();

					return true;
				}
			});
		}
	}

	@VisibleForTesting
	private class PubkeyAdapter extends ItemAdapter {
		private final List<PubkeyBean> pubkeys;

		public PubkeyAdapter(Context context, List<PubkeyBean> pubkeys) {
			super(context);
			this.pubkeys = pubkeys;
		}

		@Override
		public PubkeyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_pubkey, parent, false);
			return new PubkeyViewHolder(v);
		}

		@Override
		public void onBindViewHolder(ItemViewHolder holder, int position) {
			PubkeyViewHolder pubkeyHolder = (PubkeyViewHolder) holder;

			PubkeyBean pubkey = pubkeys.get(position);
			pubkeyHolder.pubkey = pubkey;
			if (pubkey == null) {
				// Well, something bad happened. We can't continue.
				Log.e("PubkeyAdapter", "Pubkey bean is null!");

				pubkeyHolder.nickname.setText("Error during lookup");
			} else {
				pubkeyHolder.nickname.setText(pubkey.getNickname());
			}

			boolean imported = PubkeyDatabase.KEY_TYPE_IMPORTED.equals(pubkey.getType());

			if (imported) {
				try {
					PEMStructure struct = PEMDecoder.parsePEM(new String(pubkey.getPrivateKey()).toCharArray());
					String type;
					if (struct.pemType == PEMDecoder.PEM_RSA_PRIVATE_KEY) {
						type = "RSA";
					} else if (struct.pemType == PEMDecoder.PEM_DSA_PRIVATE_KEY) {
						type = "DSA";
					} else if (struct.pemType == PEMDecoder.PEM_EC_PRIVATE_KEY) {
						type = "EC";
					} else if (struct.pemType == PEMDecoder.PEM_OPENSSH_PRIVATE_KEY) {
						type = "OpenSSH";
					} else {
						throw new RuntimeException("Unexpected key type: " + struct.pemType);
					}
					pubkeyHolder.caption.setText(String.format("%s unknown-bit", type));
				} catch (IOException e) {
					Log.e(TAG, "Error decoding IMPORTED public key at " + pubkey.getId(), e);
				}
			} else {
				try {
					pubkeyHolder.caption.setText(pubkey.getDescription(getApplicationContext()));
				} catch (Exception e) {
					Log.e(TAG, "Error decoding public key at " + pubkey.getId(), e);
					pubkeyHolder.caption.setText(R.string.pubkey_unknown_format);
				}
			}

			if (bound == null) {
				pubkeyHolder.icon.setVisibility(View.GONE);
			} else {
				pubkeyHolder.icon.setVisibility(View.VISIBLE);

				if (bound.isKeyLoaded(pubkey.getNickname()))
					pubkeyHolder.icon.setImageState(new int[] {android.R.attr.state_checked}, true);
				else
					pubkeyHolder.icon.setImageState(new int[] {}, true);
			}
		}

		@Override
		public int getItemCount() {
			return pubkeys.size();
		}

		@Override
		public long getItemId(int position) {
			return pubkeys.get(position).getId();
		}
	}
}
