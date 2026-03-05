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

import android.app.ProgressDialog;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.text.InputType;
import android.text.format.DateUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.connectbot.bean.HostBean;
import org.connectbot.bean.HostGroupBean;
import org.connectbot.service.OnHostStatusChangedListener;
import org.connectbot.service.TerminalBridge;
import org.connectbot.service.TerminalManager;
import org.connectbot.util.EncryptedBackupManager;
import org.connectbot.transport.TransportFactory;
import org.connectbot.util.HostDatabase;
import org.connectbot.util.PreferenceConstants;
import org.connectbot.util.SecurityKeyDebugLog;
import org.connectbot.util.SecurityKeyDebugReportExporter;
import org.connectbot.util.YubiKeyCapabilityProbe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import androidx.annotation.Nullable;

public class HostListActivity extends AppCompatListActivity implements OnHostStatusChangedListener {
	public final static String TAG = "CB.HostListActivity";
	public static final String DISCONNECT_ACTION = "org.connectbot.action.DISCONNECT";
	private static final String MARKER_BACKUP_PICKER = "BACKUP_IMPORT_PICKER";
	private static final String MARKER_BACKUP_OPERATION = "BACKUP_OPERATION";
	private static final long HOST_GROUP_ALL = -3L;

	public final static int REQUEST_EDIT = 1;
	private static final int REQUEST_IMPORT_BACKUP = 2;

	protected TerminalManager bound = null;

	private HostDatabase hostdb;
	private List<HostBean> hosts;
	protected LayoutInflater inflater = null;

	protected boolean sortedByColor = false;
	private final Map<Long, Boolean> mGroupExpansionStates = new LinkedHashMap<Long, Boolean>();

	private MenuItem sortcolor;

	private MenuItem sortlast;

	private MenuItem disconnectall;
	private MenuItem renameGroup;
	private MenuItem deleteGroup;

	private SharedPreferences prefs = null;

	protected boolean makingShortcut = false;

	private boolean waitingForDisconnectAll = false;
	private boolean mBackupOperationInProgress = false;
	private ProgressDialog mBackupProgressDialog;

	/**
	 * Whether to close the activity when disconnectAll is called. True if this activity was
	 * only brought to the foreground via the notification button to disconnect all hosts.
	 */
	private boolean closeOnDisconnectAll = true;

	private ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			bound = ((TerminalManager.TerminalBinder) service).getService();

			// update our listview binder to find the service
			HostListActivity.this.updateList();

			bound.registerOnHostStatusChangedListener(HostListActivity.this);

			if (waitingForDisconnectAll) {
				disconnectAll();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			bound.unregisterOnHostStatusChangedListener(HostListActivity.this);

			bound = null;
			HostListActivity.this.updateList();
		}
	};

	@Override
	public void onStart() {
		super.onStart();

		// start the terminal manager service
		this.bindService(new Intent(this, TerminalManager.class), connection, Context.BIND_AUTO_CREATE);

		hostdb = HostDatabase.get(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		this.unbindService(connection);

		dismissBackupProgressDialog();

		hostdb = null;

		closeOnDisconnectAll = true;
	}

	@Override
	public void onResume() {
		super.onResume();

		// Must disconnectAll before setting closeOnDisconnectAll to know whether to keep the
		// activity open after disconnecting.
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0 &&
				DISCONNECT_ACTION.equals(getIntent().getAction())) {
			Log.d(TAG, "Got disconnect all request");
			disconnectAll();
		}

		// Still close on disconnect if waiting for a disconnect.
		closeOnDisconnectAll = waitingForDisconnectAll && closeOnDisconnectAll;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_EDIT) {
			this.updateList();
			return;
		}

		if (requestCode == REQUEST_IMPORT_BACKUP) {
			final Uri backupUri = extractBackupUri(data);
			SecurityKeyDebugLog.logFlow(
					getApplicationContext(),
					TAG,
					MARKER_BACKUP_PICKER,
					"resultCode=" + resultCode
							+ " hasData=" + (data != null && data.getData() != null)
							+ " hasClip=" + (data != null && data.getClipData() != null)
							+ " hasUri=" + (backupUri != null));

			if (backupUri == null) {
				boolean explicitCancel = resultCode == RESULT_CANCELED && data == null;
				if (!explicitCancel) {
					Toast.makeText(this, R.string.backup_import_file_missing, Toast.LENGTH_LONG).show();
				}
				SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_PICKER, "uri_missing");
				Log.w(TAG, "Import backup result did not contain a file URI");
				return;
			}

			// Some OEM file pickers return non-standard result codes even when a file URI exists.
			if (resultCode != RESULT_OK) {
				Log.w(TAG, "Import backup returned non-standard result code " + resultCode + ", proceeding with selected URI");
				SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_PICKER, "non_standard_result_code=" + resultCode);
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
					&& "content".equalsIgnoreCase(backupUri.getScheme())) {
				final int takeFlags = (data != null ? data.getFlags() : 0)
						& (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
				try {
					getContentResolver().takePersistableUriPermission(backupUri, takeFlags);
				} catch (SecurityException ignored) {
				} catch (RuntimeException ignored) {
				}
			}

			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_PICKER, "uri_selected");
			promptForImportPassword(backupUri);
		}
	}

	@Nullable
	private Uri extractBackupUri(@Nullable Intent data) {
		if (data == null) {
			return null;
		}
		Uri directUri = data.getData();
		if (directUri != null) {
			return directUri;
		}
		if (data.getClipData() != null && data.getClipData().getItemCount() > 0) {
			Uri clipUri = data.getClipData().getItemAt(0).getUri();
			if (clipUri != null) {
				return clipUri;
			}
		}
		// Some file managers return the selected URI through EXTRA_STREAM.
		Object streamExtra = data.getParcelableExtra(Intent.EXTRA_STREAM);
		if (streamExtra instanceof Uri) {
			return (Uri) streamExtra;
		}
		return null;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_PICKER, "host_list_created");
		setContentView(R.layout.act_hostlist);
		setTitle(R.string.title_hosts_list);

		mListView = findViewById(R.id.list);
		mListView.setHasFixedSize(true);
		mListView.setLayoutManager(new LinearLayoutManager(this));
		mListView.addItemDecoration(new ListItemDecoration(this));

		mEmptyView = findViewById(R.id.empty);

		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// detect HTC Dream and apply special preferences
		if (Build.MANUFACTURER.equals("HTC") && Build.DEVICE.equals("dream")) {
			SharedPreferences.Editor editor = prefs.edit();
			boolean doCommit = false;
			if (!prefs.contains(PreferenceConstants.SHIFT_FKEYS) &&
					!prefs.contains(PreferenceConstants.CTRL_FKEYS)) {
				editor.putBoolean(PreferenceConstants.SHIFT_FKEYS, true);
				editor.putBoolean(PreferenceConstants.CTRL_FKEYS, true);
				doCommit = true;
			}
			if (!prefs.contains(PreferenceConstants.STICKY_MODIFIERS)) {
				editor.putString(PreferenceConstants.STICKY_MODIFIERS, PreferenceConstants.YES);
				doCommit = true;
			}
			if (!prefs.contains(PreferenceConstants.KEYMODE)) {
				editor.putString(PreferenceConstants.KEYMODE, PreferenceConstants.KEYMODE_RIGHT);
				doCommit = true;
			}
			if (doCommit) {
				editor.apply();
			}
		}

		this.makingShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())
								|| Intent.ACTION_PICK.equals(getIntent().getAction());

		// connect with hosts database and populate list
		this.hostdb = HostDatabase.get(this);

		this.sortedByColor = prefs.getBoolean(PreferenceConstants.SORT_BY_COLOR, false);

		this.registerForContextMenu(mListView);

		View addHostButtonContainer = findViewById(R.id.add_host_button_container);
		addHostButtonContainer.setVisibility(makingShortcut ? View.GONE : View.VISIBLE);

		FloatingActionButton addHostButton = findViewById(R.id.add_host_button);
		addHostButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = EditHostActivity.createIntentForNewHost(HostListActivity.this);
				startActivityForResult(intent, REQUEST_EDIT);
			}

			public void onNothingSelected(AdapterView<?> arg0) {}
		});

		this.inflater = LayoutInflater.from(this);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// don't offer menus when creating shortcut
		if (makingShortcut) return true;

		sortcolor.setVisible(!sortedByColor);
		sortlast.setVisible(sortedByColor);
		disconnectall.setEnabled(bound != null && bound.getBridges().size() > 0);
		boolean hasGroups = hostdb != null && !hostdb.getHostGroups().isEmpty();
		if (renameGroup != null) {
			renameGroup.setEnabled(hasGroups);
		}
		if (deleteGroup != null) {
			deleteGroup.setEnabled(hasGroups);
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// don't offer menus when creating shortcut
		if (makingShortcut) return true;

		// add host, ssh keys, about
		sortcolor = menu.add(R.string.list_menu_sortcolor);
		sortcolor.setIcon(android.R.drawable.ic_menu_share);
		sortcolor.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				sortedByColor = true;
				updateList();
				return true;
			}
		});

		sortlast = menu.add(R.string.list_menu_sortname);
		sortlast.setIcon(android.R.drawable.ic_menu_share);
		sortlast.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				sortedByColor = false;
				updateList();
				return true;
			}
		});

		MenuItem keys = menu.add(R.string.list_menu_pubkeys);
		keys.setIcon(android.R.drawable.ic_lock_lock);
		keys.setIntent(new Intent(HostListActivity.this, PubkeyListActivity.class));

		MenuItem colors = menu.add(R.string.title_colors);
		colors.setIcon(android.R.drawable.ic_menu_slideshow);
		colors.setIntent(new Intent(HostListActivity.this, ColorsActivity.class));

		disconnectall = menu.add(R.string.list_menu_disconnect);
		disconnectall.setIcon(android.R.drawable.ic_menu_delete);
		disconnectall.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				disconnectAll();
				return false;
			}
		});

		MenuItem settings = menu.add(R.string.list_menu_settings);
		settings.setIcon(android.R.drawable.ic_menu_preferences);
		settings.setIntent(new Intent(HostListActivity.this, SettingsActivity.class));

		MenuItem createGroup = menu.add(R.string.list_menu_group_create);
		createGroup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				promptCreateGroup();
				return true;
			}
		});

		renameGroup = menu.add(R.string.list_menu_group_rename);
		renameGroup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				promptRenameGroup();
				return true;
			}
		});

		deleteGroup = menu.add(R.string.list_menu_group_delete);
		deleteGroup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				promptDeleteGroup();
				return true;
			}
		});

		MenuItem exportDebugReport = menu.add(R.string.security_key_export_debug_report_title);
		exportDebugReport.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				exportDebugReport();
				return true;
			}
		});

		MenuItem exportEncryptedBackup = menu.add(R.string.backup_export_title);
		exportEncryptedBackup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				promptForExportPassword();
				return true;
			}
		});

		MenuItem importEncryptedBackup = menu.add(R.string.backup_import_title);
		importEncryptedBackup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				pickBackupFileForImport();
				return true;
			}
		});

		MenuItem runYubiKeyCapabilityProbe = menu.add(R.string.yubikey_capability_probe_title);
		runYubiKeyCapabilityProbe.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				runYubiKeyCapabilityProbe();
				return true;
			}
		});

		MenuItem importLatestBackup = menu.add(R.string.backup_import_latest_title);
		importLatestBackup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				importLatestLocalBackup();
				return true;
			}
		});

		MenuItem help = menu.add(R.string.title_help);
		help.setIcon(android.R.drawable.ic_menu_help);
		help.setIntent(new Intent(HostListActivity.this, HelpActivity.class));

		return true;

	}

	private void promptCreateGroup() {
		showGroupNameDialog(R.string.host_group_create_title, null, new GroupNameHandler() {
			@Override
			public void onGroupNameEntered(String groupName) {
				if (hostdb.findHostGroupByName(groupName) != null) {
					Toast.makeText(HostListActivity.this, R.string.host_group_name_exists, Toast.LENGTH_LONG).show();
					return;
				}

				HostGroupBean group = new HostGroupBean();
				group.setName(groupName);
				try {
					hostdb.saveHostGroup(group);
					updateList();
					Toast.makeText(HostListActivity.this, R.string.host_group_create_success, Toast.LENGTH_LONG).show();
				} catch (RuntimeException e) {
					Log.e(TAG, "Failed to create host group", e);
					Toast.makeText(HostListActivity.this, R.string.host_group_operation_failed, Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	private void promptRenameGroup() {
		final List<HostGroupBean> groups = hostdb.getHostGroups();
		if (groups.isEmpty()) {
			Toast.makeText(this, R.string.host_group_none_available, Toast.LENGTH_LONG).show();
			return;
		}

		CharSequence[] groupNames = new CharSequence[groups.size()];
		for (int i = 0; i < groups.size(); i++) {
			groupNames[i] = groups.get(i).getName();
		}

		new AlertDialog.Builder(this, R.style.AlertDialogTheme)
				.setTitle(R.string.host_group_rename_pick_title)
				.setItems(groupNames, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final HostGroupBean selectedGroup = groups.get(which);
						showGroupNameDialog(
								R.string.host_group_rename_title,
								selectedGroup.getName(),
								new GroupNameHandler() {
									@Override
									public void onGroupNameEntered(String groupName) {
										HostGroupBean existing = hostdb.findHostGroupByName(groupName);
										if (existing != null && existing.getId() != selectedGroup.getId()) {
											Toast.makeText(
													HostListActivity.this,
													R.string.host_group_name_exists,
													Toast.LENGTH_LONG).show();
											return;
										}

										selectedGroup.setName(groupName);
										try {
											hostdb.saveHostGroup(selectedGroup);
											updateList();
											Toast.makeText(
													HostListActivity.this,
													R.string.host_group_rename_success,
													Toast.LENGTH_LONG).show();
										} catch (SQLiteException e) {
											Log.e(TAG, "Failed to rename host group", e);
											Toast.makeText(
													HostListActivity.this,
													R.string.host_group_operation_failed,
													Toast.LENGTH_LONG).show();
										}
									}
								});
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void promptDeleteGroup() {
		final List<HostGroupBean> groups = hostdb.getHostGroups();
		if (groups.isEmpty()) {
			Toast.makeText(this, R.string.host_group_none_available, Toast.LENGTH_LONG).show();
			return;
		}

		CharSequence[] groupNames = new CharSequence[groups.size()];
		for (int i = 0; i < groups.size(); i++) {
			groupNames[i] = groups.get(i).getName();
		}

		new AlertDialog.Builder(this, R.style.AlertDialogTheme)
				.setTitle(R.string.host_group_delete_pick_title)
				.setItems(groupNames, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final HostGroupBean selectedGroup = groups.get(which);
						new AlertDialog.Builder(HostListActivity.this, R.style.AlertDialogTheme)
								.setMessage(getString(R.string.host_group_delete_message, selectedGroup.getName()))
								.setPositiveButton(R.string.host_group_delete_confirm, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface d, int w) {
										try {
											hostdb.deleteHostGroup(selectedGroup.getId());
											updateList();
											Toast.makeText(
													HostListActivity.this,
													R.string.host_group_delete_success,
													Toast.LENGTH_LONG).show();
										} catch (RuntimeException e) {
											Log.e(TAG, "Failed to delete host group", e);
											Toast.makeText(
													HostListActivity.this,
													R.string.host_group_operation_failed,
													Toast.LENGTH_LONG).show();
										}
									}
								})
								.setNegativeButton(android.R.string.cancel, null)
								.show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showGroupNameDialog(
			int titleResId,
			@Nullable String initialName,
			final GroupNameHandler handler) {
		final EditText input = new EditText(this);
		int padding = (int) (16 * getResources().getDisplayMetrics().density);
		input.setPadding(padding, padding, padding, padding);
		input.setHint(R.string.host_group_name_hint);
		if (initialName != null) {
			input.setText(initialName);
			input.setSelection(initialName.length());
		}

		final AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
				.setTitle(titleResId)
				.setView(input)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, null)
				.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface d) {
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String groupName = input.getText().toString().trim();
						if (groupName.isEmpty()) {
							input.setError(getString(R.string.host_group_name_invalid));
							input.requestFocus();
							return;
						}
						dialog.dismiss();
						handler.onGroupNameEntered(groupName);
					}
				});
			}
		});
		dialog.show();
	}

	private interface GroupNameHandler {
		void onGroupNameEntered(String groupName);
	}

	private void exportDebugReport() {
		try {
			File reportFile = SecurityKeyDebugReportExporter.exportReport(this);
			startActivity(SecurityKeyDebugReportExporter.createShareIntent(this, reportFile));
			Toast.makeText(this, getString(R.string.security_key_export_debug_report_success, reportFile.getAbsolutePath()),
					Toast.LENGTH_LONG).show();
		} catch (IOException | RuntimeException e) {
			Log.e(TAG, "SK_HOSTLIST_EXPORT_REPORT_FAILED: unable to export debug report", e);
			Toast.makeText(this, R.string.security_key_export_debug_report_failed, Toast.LENGTH_LONG).show();
		}
	}

	private void runYubiKeyCapabilityProbe() {
		YubiKeyCapabilityProbe.ProbeResult result = YubiKeyCapabilityProbe.run(getApplicationContext());
		Toast.makeText(
				this,
				getString(
						R.string.yubikey_capability_probe_done,
						result.pivStatus,
						result.fido2Status,
						Boolean.toString(result.sshlibSkEcdsaSupported),
						Boolean.toString(result.sshlibSkEd25519Supported)),
				Toast.LENGTH_LONG).show();
	}

	private void promptForExportPassword() {
		if (mBackupOperationInProgress) {
			Toast.makeText(this, R.string.backup_operation_in_progress, Toast.LENGTH_LONG).show();
			return;
		}
		showPasswordDialog(
				R.string.backup_export_title,
				R.string.backup_password_prompt_export,
				true,
				new PasswordHandler() {
					@Override
					public void onPasswordEntered(String password) {
						exportEncryptedBackup(password);
					}
				});
	}

	private void promptForImportPassword(final Uri backupUri) {
		if (mBackupOperationInProgress) {
			Toast.makeText(this, R.string.backup_operation_in_progress, Toast.LENGTH_LONG).show();
			return;
		}
		SecurityKeyDebugLog.logFlow(
				getApplicationContext(),
				TAG,
				MARKER_BACKUP_PICKER,
				"password_prompt_for_scheme=" + String.valueOf(backupUri.getScheme()));
		String fileName = EncryptedBackupManager.readDisplayName(backupUri);
		showPasswordDialog(
				R.string.backup_import_title,
				getString(R.string.backup_password_prompt_import, fileName),
				false,
				new PasswordHandler() {
					@Override
					public void onPasswordEntered(String password) {
						importEncryptedBackup(backupUri, password);
					}
				});
	}

	private void exportEncryptedBackup(final String password) {
		if (!beginBackupOperation(MARKER_BACKUP_OPERATION, "export_started", R.string.backup_export_in_progress)) {
			Toast.makeText(this, R.string.backup_operation_in_progress, Toast.LENGTH_LONG).show();
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final File backupFile = EncryptedBackupManager.exportBackup(HostListActivity.this, password);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							startActivity(EncryptedBackupManager.createShareIntent(HostListActivity.this, backupFile));
							Toast.makeText(
									HostListActivity.this,
									getString(R.string.backup_export_success, backupFile.getAbsolutePath()),
									Toast.LENGTH_LONG).show();
						}
					});
					SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_OPERATION, "export_success");
				} catch (final IOException e) {
					Log.e(TAG, "Failed to export encrypted backup", e);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(
									HostListActivity.this,
									getString(R.string.backup_operation_failed, e.getMessage()),
									Toast.LENGTH_LONG).show();
						}
					});
					SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_OPERATION, "export_failed");
				} finally {
					finishBackupOperation(MARKER_BACKUP_OPERATION, "export_finished");
				}
			}
		}).start();
	}

	private void importEncryptedBackup(final Uri backupUri, final String password) {
		if (!beginBackupOperation(MARKER_BACKUP_OPERATION, "import_started", R.string.backup_import_in_progress)) {
			Toast.makeText(this, R.string.backup_operation_in_progress, Toast.LENGTH_LONG).show();
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final EncryptedBackupManager.ImportResult importResult =
							EncryptedBackupManager.importBackup(HostListActivity.this, backupUri, password);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							updateList();
							Toast.makeText(
									HostListActivity.this,
									getString(
											R.string.backup_import_success,
											importResult.hostsImported,
											importResult.pubkeysImported,
											importResult.passwordsImported),
									Toast.LENGTH_LONG).show();
						}
					});
					SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_OPERATION, "import_success");
				} catch (final IOException e) {
					Log.e(TAG, "Failed to import encrypted backup", e);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(
									HostListActivity.this,
									getString(R.string.backup_operation_failed, e.getMessage()),
									Toast.LENGTH_LONG).show();
						}
					});
					SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_OPERATION, "import_failed");
				} finally {
					finishBackupOperation(MARKER_BACKUP_OPERATION, "import_finished");
				}
			}
		}).start();
	}

	private synchronized boolean beginBackupOperation(String marker, String detail, int progressMessageResId) {
		if (mBackupOperationInProgress) {
			return false;
		}
		mBackupOperationInProgress = true;
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, marker, detail);
		showBackupProgressDialog(progressMessageResId);
		return true;
	}

	private void finishBackupOperation(String marker, String detail) {
		synchronized (this) {
			mBackupOperationInProgress = false;
		}
		dismissBackupProgressDialog();
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, marker, detail);
	}

	private void showBackupProgressDialog(final int messageResId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (isFinishing()) {
					return;
				}
				if (mBackupProgressDialog == null) {
					mBackupProgressDialog = new ProgressDialog(HostListActivity.this);
					mBackupProgressDialog.setIndeterminate(true);
					mBackupProgressDialog.setCancelable(false);
				}
				mBackupProgressDialog.setMessage(getString(messageResId));
				if (!mBackupProgressDialog.isShowing()) {
					mBackupProgressDialog.show();
				}
			}
		});
	}

	private void dismissBackupProgressDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mBackupProgressDialog == null) {
					return;
				}
				if (mBackupProgressDialog.isShowing()) {
					mBackupProgressDialog.dismiss();
				}
			}
		});
	}

	private void pickBackupFileForImport() {
		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_PICKER, "picker_open");
		Intent intent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		} else {
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
		intent.setType("*/*");
		startActivityForResult(Intent.createChooser(intent, getString(R.string.backup_import_file_picker_title)), REQUEST_IMPORT_BACKUP);
	}

	private void importLatestLocalBackup() {
		File backupDir = getExternalFilesDir("backup");
		if (backupDir == null || !backupDir.exists() || !backupDir.isDirectory()) {
			Toast.makeText(this, R.string.backup_import_no_local_file, Toast.LENGTH_LONG).show();
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_PICKER, "latest_local_missing_dir");
			return;
		}

		File[] files = backupDir.listFiles();
		if (files == null || files.length == 0) {
			Toast.makeText(this, R.string.backup_import_no_local_file, Toast.LENGTH_LONG).show();
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_PICKER, "latest_local_empty");
			return;
		}

		File newestBackup = null;
		for (File file : files) {
			if (file == null || !file.isFile()) {
				continue;
			}
			if (!file.getName().endsWith(".tbbak")) {
				continue;
			}
			if (newestBackup == null || file.lastModified() > newestBackup.lastModified()) {
				newestBackup = file;
			}
		}

		if (newestBackup == null) {
			Toast.makeText(this, R.string.backup_import_no_local_file, Toast.LENGTH_LONG).show();
			SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_PICKER, "latest_local_no_tbbak");
			return;
		}

		SecurityKeyDebugLog.logFlow(getApplicationContext(), TAG, MARKER_BACKUP_PICKER,
				"latest_local_selected=" + newestBackup.getName());
		promptForImportPassword(Uri.fromFile(newestBackup));
	}

	private void showPasswordDialog(
			int titleResId,
			CharSequence message,
			final boolean requireConfirmation,
			final PasswordHandler passwordHandler) {
		final LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);

		int padding = (int) (16 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, padding, padding, 0);

		final EditText passwordInput = new EditText(this);
		passwordInput.setHint(R.string.backup_password_hint);
		passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
		container.addView(passwordInput);

		final EditText confirmInput;
		if (requireConfirmation) {
			confirmInput = new EditText(this);
			confirmInput.setHint(R.string.backup_password_confirm_hint);
			confirmInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			confirmInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
			container.addView(confirmInput);
		} else {
			confirmInput = null;
		}

		final AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
				.setTitle(titleResId)
				.setMessage(message)
				.setView(container)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, null)
				.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialogInterface) {
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String password = passwordInput.getText().toString();
						passwordInput.setError(null);
						if (confirmInput != null) {
							confirmInput.setError(null);
						}
						if (password.trim().isEmpty()) {
							passwordInput.setError(getString(R.string.backup_password_empty));
							passwordInput.requestFocus();
							return;
						}
						if (requireConfirmation && confirmInput != null) {
							String confirmation = confirmInput.getText().toString();
							if (!password.equals(confirmation)) {
								confirmInput.setError(getString(R.string.backup_password_mismatch));
								confirmInput.requestFocus();
								return;
							}
						}
						dialog.dismiss();
						passwordHandler.onPasswordEntered(password);
					}
				});
			}
		});
		dialog.show();
	}

	private void showPasswordDialog(
			int titleResId,
			int messageResId,
			boolean requireConfirmation,
			PasswordHandler passwordHandler) {
		showPasswordDialog(titleResId, getString(messageResId), requireConfirmation, passwordHandler);
	}

	private interface PasswordHandler {
		void onPasswordEntered(String password);
	}

	/**
	 * Disconnects all active connections and closes the activity if appropriate.
	 */
	private void disconnectAll() {
		if (bound == null) {
			waitingForDisconnectAll = true;
			return;
		}

		new androidx.appcompat.app.AlertDialog.Builder(
				HostListActivity.this, R.style.AlertDialogTheme)
			.setMessage(getString(R.string.disconnect_all_message))
			.setPositiveButton(R.string.disconnect_all_pos, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					bound.disconnectAll(true, false);
					waitingForDisconnectAll = false;

					// Clear the intent so that the activity can be relaunched without closing.
					// TODO(jlklein): Find a better way to do this.
					setIntent(new Intent());

					if (closeOnDisconnectAll) {
						finish();
					}
				}
			})
			.setNegativeButton(R.string.disconnect_all_neg, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					waitingForDisconnectAll = false;
					// Clear the intent so that the activity can be relaunched without closing.
					// TODO(jlklein): Find a better way to do this.
					setIntent(new Intent());
				}
			}).create().show();
	}

	/**
	 * @return
	 */
	private boolean startConsoleActivity(Uri uri) {
		HostBean host = TransportFactory.findHost(hostdb, uri);
		if (host == null) {
			host = TransportFactory.getTransport(uri.getScheme()).createHost(uri);
			host.setColor(HostDatabase.COLOR_GRAY);
			host.setPubkeyId(HostDatabase.PUBKEYID_ANY);
			hostdb.saveHost(host);
		}

		Intent intent = new Intent(HostListActivity.this, ConsoleActivity.class);
		intent.setData(uri);
		startActivity(intent);

		return true;
	}

	protected void updateList() {
		if (prefs.getBoolean(PreferenceConstants.SORT_BY_COLOR, false) != sortedByColor) {
			Editor edit = prefs.edit();
			edit.putBoolean(PreferenceConstants.SORT_BY_COLOR, sortedByColor);
			edit.apply();
		}

		if (hostdb == null)
			hostdb = HostDatabase.get(this);

		hosts = hostdb.getHosts(sortedByColor);

		// Don't lose hosts that are connected via shortcuts but not in the database.
		if (bound != null) {
			for (TerminalBridge bridge : bound.getBridges()) {
				if (!hosts.contains(bridge.host))
					hosts.add(0, bridge.host);
			}
		}

		mAdapter = new HostAdapter(this, buildGroupedRows(hosts), bound);
		mListView.setAdapter(mAdapter);
		adjustViewVisibility();
	}

	private List<HostListRow> buildGroupedRows(List<HostBean> allHosts) {
		LinkedHashMap<Long, List<HostBean>> groupedHosts = new LinkedHashMap<Long, List<HostBean>>();
		LinkedHashMap<Long, String> groupNamesById = new LinkedHashMap<Long, String>();
		ArrayList<HostBean> allAvailableHosts = new ArrayList<HostBean>();

		groupedHosts.put(HostDatabase.HOST_GROUP_NONE, new ArrayList<HostBean>());
		groupNamesById.put(HostDatabase.HOST_GROUP_NONE, getString(R.string.host_group_ungrouped_header));

		List<HostGroupBean> groups = hostdb.getHostGroups();
		for (HostGroupBean group : groups) {
			if (group == null || group.getId() <= 0 || group.getName() == null) {
				continue;
			}
			groupedHosts.put(group.getId(), new ArrayList<HostBean>());
			groupNamesById.put(group.getId(), group.getName());
		}

		for (HostBean host : allHosts) {
			if (host == null) {
				continue;
			}
			allAvailableHosts.add(host);
			long groupId = host.getGroupId();
			List<HostBean> hostsForGroup = groupedHosts.get(groupId);
			if (hostsForGroup == null) {
				hostsForGroup = groupedHosts.get(HostDatabase.HOST_GROUP_NONE);
			}
			if (hostsForGroup != null) {
				hostsForGroup.add(host);
			}
		}

		ArrayList<HostListRow> rows = new ArrayList<HostListRow>();
		ArrayList<Long> visibleGroupIds = new ArrayList<Long>();

		for (HostGroupBean group : groups) {
			if (group == null || group.getId() <= 0 || group.getName() == null) {
				continue;
			}
			appendGroupRows(
					rows,
					visibleGroupIds,
					group.getId(),
					group.getName(),
					groupedHosts.get(group.getId()),
					true);
		}

		List<HostBean> ungroupedHosts = groupedHosts.get(HostDatabase.HOST_GROUP_NONE);
		boolean showUngrouped = !groups.isEmpty() || (ungroupedHosts != null && !ungroupedHosts.isEmpty());
		if (showUngrouped) {
			appendGroupRows(
					rows,
					visibleGroupIds,
					HostDatabase.HOST_GROUP_NONE,
					groupNamesById.get(HostDatabase.HOST_GROUP_NONE),
					ungroupedHosts,
					true);
		}

		if (!allAvailableHosts.isEmpty()) {
			appendGroupRows(
					rows,
					visibleGroupIds,
					HOST_GROUP_ALL,
					getString(R.string.host_group_all_header),
					allAvailableHosts,
					false);
		}

		pruneGroupExpansionState(visibleGroupIds);
		return rows;
	}

	private void appendGroupRows(
			List<HostListRow> rows,
			List<Long> visibleGroupIds,
			long groupId,
			@Nullable String title,
			@Nullable List<HostBean> groupHosts,
			boolean showWhenEmpty) {
		int hostCount = groupHosts == null ? 0 : groupHosts.size();
		if (!showWhenEmpty && hostCount == 0) {
			return;
		}

		boolean expanded = isGroupExpanded(groupId);
		visibleGroupIds.add(groupId);
		rows.add(new GroupHeaderRow(groupId, title, hostCount, expanded));

		if (!expanded || groupHosts == null) {
			return;
		}

		for (HostBean host : groupHosts) {
			rows.add(new HostRow(host));
		}
	}

	private boolean isGroupExpanded(long groupId) {
		Boolean expanded = mGroupExpansionStates.get(groupId);
		if (expanded != null) {
			return expanded;
		}

		boolean defaultExpanded = groupId != HOST_GROUP_ALL;
		mGroupExpansionStates.put(groupId, defaultExpanded);
		return defaultExpanded;
	}

	private void toggleGroupExpanded(long groupId) {
		mGroupExpansionStates.put(groupId, !isGroupExpanded(groupId));
		updateList();
	}

	private void pruneGroupExpansionState(List<Long> visibleGroupIds) {
		if (mGroupExpansionStates.isEmpty()) {
			return;
		}

		ArrayList<Long> staleGroupIds = new ArrayList<Long>();
		for (Long groupId : mGroupExpansionStates.keySet()) {
			if (!visibleGroupIds.contains(groupId)) {
				staleGroupIds.add(groupId);
			}
		}
		for (Long staleGroupId : staleGroupIds) {
			mGroupExpansionStates.remove(staleGroupId);
		}
	}

	@Override
	public void onHostStatusChanged() {
		updateList();
	}

	@VisibleForTesting
	public class HostViewHolder extends ItemViewHolder {
		public final ImageView icon;
		public final TextView nickname;
		public final TextView caption;

		public HostBean host;

		public HostViewHolder(View v) {
			super(v);

			icon = v.findViewById(android.R.id.icon);
			nickname = v.findViewById(android.R.id.text1);
			caption = v.findViewById(android.R.id.text2);
		}

		@Override
		public void onClick(View v) {
			// launch off to console details
			Uri uri = host.getUri();

			Intent contents = new Intent(Intent.ACTION_VIEW, uri);
			contents.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			if (makingShortcut) {
				// create shortcut if requested
				ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(
						HostListActivity.this, R.mipmap.icon);

				Intent intent = new Intent();
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, contents);
				intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, host.getNickname());
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

				setResult(RESULT_OK, intent);
				finish();

			} else {
				// otherwise just launch activity to show this host
				contents.setClass(HostListActivity.this, ConsoleActivity.class);
				HostListActivity.this.startActivity(contents);
			}
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			menu.setHeaderTitle(host.getNickname());

			// edit, disconnect, delete
			MenuItem connect = menu.add(R.string.list_host_disconnect);
			final TerminalBridge bridge = (bound == null) ? null : bound.getConnectedBridge(host);
			connect.setEnabled(bridge != null);
			connect.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					bridge.dispatchDisconnect(true);
					return true;
				}
			});

			MenuItem edit = menu.add(R.string.list_host_edit);
			edit.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					Intent intent = EditHostActivity.createIntentForExistingHost(
							HostListActivity.this, host.getId());
					HostListActivity.this.startActivityForResult(intent, REQUEST_EDIT);
					return true;
				}
			});

			MenuItem portForwards = menu.add(R.string.list_host_portforwards);
			portForwards.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					Intent intent = new Intent(HostListActivity.this, PortForwardListActivity.class);
					intent.putExtra(Intent.EXTRA_TITLE, host.getId());
					HostListActivity.this.startActivityForResult(intent, REQUEST_EDIT);
					return true;
				}
			});
			if (!TransportFactory.canForwardPorts(host.getProtocol()))
				portForwards.setEnabled(false);

			MenuItem delete = menu.add(R.string.list_host_delete);
			delete.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					// prompt user to make sure they really want this
					new androidx.appcompat.app.AlertDialog.Builder(
									HostListActivity.this, R.style.AlertDialogTheme)
							.setMessage(getString(R.string.delete_message, host.getNickname()))
							.setPositiveButton(R.string.delete_pos, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// make sure we disconnect
									if (bridge != null)
										bridge.dispatchDisconnect(true);

									hostdb.deleteHost(host);
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
	public class GroupHeaderViewHolder extends ItemViewHolder {
		public final TextView title;
		@Nullable private GroupHeaderRow row;

		public GroupHeaderViewHolder(View view) {
			super(view);
			title = view.findViewById(android.R.id.text1);
		}

		@Override
		public void onClick(View v) {
			if (row != null) {
				toggleGroupExpanded(row.groupId);
			}
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			// Headers do not have context actions.
		}
	}

	private interface HostListRow {
	}

	private static final class GroupHeaderRow implements HostListRow {
		private final long groupId;
		private final String title;
		private final int hostCount;
		private final boolean expanded;

		GroupHeaderRow(long groupId, @Nullable String title, int hostCount, boolean expanded) {
			this.groupId = groupId;
			this.title = title == null ? "" : title;
			this.hostCount = hostCount;
			this.expanded = expanded;
		}
	}

	private static final class HostRow implements HostListRow {
		private final HostBean host;

		HostRow(HostBean host) {
			this.host = host;
		}
	}

	@VisibleForTesting
	private class HostAdapter extends ItemAdapter {
		private static final int VIEW_TYPE_GROUP_HEADER = 1;
		private static final int VIEW_TYPE_HOST = 2;

		private final List<HostListRow> rows;
		private final TerminalManager manager;

		public final static int STATE_UNKNOWN = 1, STATE_CONNECTED = 2, STATE_DISCONNECTED = 3;

		public HostAdapter(Context context, List<HostListRow> rows, TerminalManager manager) {
			super(context);

			this.rows = rows;
			this.manager = manager;
		}

		/**
		 * Check if we're connected to a terminal with the given host.
		 */
		private int getConnectedState(HostBean host) {
			// always disconnected if we don't have backend service
			if (this.manager == null || host == null) {
				return STATE_UNKNOWN;
			}

			if (manager.getConnectedBridge(host) != null) {
				return STATE_CONNECTED;
			}

			if (manager.disconnected.contains(host)) {
				return STATE_DISCONNECTED;
			}

			return STATE_UNKNOWN;
		}

		@Override
		public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == VIEW_TYPE_GROUP_HEADER) {
				View v = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_host_group_header, parent, false);
				return new GroupHeaderViewHolder(v);
			}
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_host, parent, false);
			return new HostViewHolder(v);
		}

		@Override
		public int getItemViewType(int position) {
			HostListRow row = rows.get(position);
			if (row instanceof GroupHeaderRow) {
				return VIEW_TYPE_GROUP_HEADER;
			}
			return VIEW_TYPE_HOST;
		}

		@TargetApi(16)
		private void hideFromAccessibility(View view, boolean hide) {
			view.setImportantForAccessibility(hide ?
					View.IMPORTANT_FOR_ACCESSIBILITY_NO : View.IMPORTANT_FOR_ACCESSIBILITY_YES);
		}

		@Override
		public void onBindViewHolder(ItemViewHolder holder, int position) {
			HostListRow row = rows.get(position);
			if (row instanceof GroupHeaderRow) {
				GroupHeaderRow header = (GroupHeaderRow) row;
				GroupHeaderViewHolder headerHolder = (GroupHeaderViewHolder) holder;
				headerHolder.row = header;
				String expandIndicator = header.expanded ? "\u25BE" : "\u25B8";
				headerHolder.title.setText(
						expandIndicator + " " + header.title + " (" + header.hostCount + ")");
				return;
			}

			HostViewHolder hostHolder = (HostViewHolder) holder;
			HostBean host = ((HostRow) row).host;
			hostHolder.host = host;
			if (host == null) {
				// Well, something bad happened. We can't continue.
				Log.e("HostAdapter", "Host bean is null!");
				hostHolder.nickname.setText("Error during lookup");
			} else {
				hostHolder.nickname.setText(host.getNickname());
			}

			switch (this.getConnectedState(host)) {
			case STATE_UNKNOWN:
				hostHolder.icon.setImageState(new int[] { }, true);
				hostHolder.icon.setContentDescription(null);
				if (Build.VERSION.SDK_INT >= 16) {
					hideFromAccessibility(hostHolder.icon, true);
				}
				break;
			case STATE_CONNECTED:
				hostHolder.icon.setImageState(new int[] { android.R.attr.state_checked }, true);
				hostHolder.icon.setContentDescription(getString(R.string.image_description_connected));
				if (Build.VERSION.SDK_INT >= 16) {
					hideFromAccessibility(hostHolder.icon, false);
				}
				break;
			case STATE_DISCONNECTED:
				hostHolder.icon.setImageState(new int[] { android.R.attr.state_expanded }, true);
				hostHolder.icon.setContentDescription(getString(R.string.image_description_disconnected));
				if (Build.VERSION.SDK_INT >= 16) {
					hideFromAccessibility(hostHolder.icon, false);
				}
				break;
			default:
				Log.e("HostAdapter", "Unknown host state encountered: " + getConnectedState(host));
			}

			@StyleRes final int chosenStyleFirstLine;
			@StyleRes final int chosenStyleSecondLine;
			if (HostDatabase.COLOR_RED.equals(host.getColor())) {
				chosenStyleFirstLine = R.style.ListItemFirstLineText_Red;
				chosenStyleSecondLine = R.style.ListItemSecondLineText_Red;
			} else if (HostDatabase.COLOR_GREEN.equals(host.getColor())) {
				chosenStyleFirstLine = R.style.ListItemFirstLineText_Green;
				chosenStyleSecondLine = R.style.ListItemSecondLineText_Green;
			} else if (HostDatabase.COLOR_BLUE.equals(host.getColor())) {
				chosenStyleFirstLine = R.style.ListItemFirstLineText_Blue;
				chosenStyleSecondLine = R.style.ListItemSecondLineText_Blue;
			} else {
				chosenStyleFirstLine = R.style.ListItemFirstLineText;
				chosenStyleSecondLine = R.style.ListItemSecondLineText;
			}

			hostHolder.nickname.setTextAppearance(context, chosenStyleFirstLine);
			hostHolder.caption.setTextAppearance(context, chosenStyleSecondLine);

			CharSequence nice = context.getString(R.string.bind_never);
			if (host.getLastConnect() > 0) {
				nice = DateUtils.getRelativeTimeSpanString(host.getLastConnect() * 1000);
			}

			hostHolder.caption.setText(nice);
		}

		@Override
		public long getItemId(int position) {
			HostListRow row = rows.get(position);
			if (row instanceof HostRow) {
				return ((HostRow) row).host.getId();
			}
			return Long.MIN_VALUE + ((GroupHeaderRow) row).groupId;
		}

		@Override
		public int getItemCount() {
			return rows.size();
		}
	}
}
