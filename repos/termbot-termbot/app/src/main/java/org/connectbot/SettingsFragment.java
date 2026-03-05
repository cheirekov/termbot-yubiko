/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2017 Kenny Root, Jeffrey Sharkey
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

import org.connectbot.util.VolumePreference;
import org.connectbot.util.VolumePreferenceFragment;
import org.connectbot.util.AppThemeManager;
import org.connectbot.util.PreferenceConstants;

import com.takisoft.preferencex.PreferenceFragmentCompat;

import android.os.Bundle;
import android.content.SharedPreferences;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;

/**
 * Created by kenny on 2/20/17.
 */

public class SettingsFragment extends PreferenceFragmentCompat
		implements SharedPreferences.OnSharedPreferenceChangeListener {
	public SettingsFragment() {
	}

	/**
	 * Called when a preference in the tree requests to display a dialog. Subclasses should
	 * override this method to display custom dialogs or to handle dialogs for custom preference
	 * classes.
	 *
	 * @param preference The Preference object requesting the dialog.
	 */
	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (preference instanceof VolumePreference) {
			DialogFragment fragment = VolumePreferenceFragment.newInstance(preference);
			fragment.setTargetFragment(this, 0);
			fragment.show(getFragmentManager(),
					"android.support.v7.preference.PreferenceFragment.DIALOG");
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onCreatePreferencesFix(Bundle bundle, String rootKey) {
		setPreferencesFromResource(R.xml.preferences, rootKey);
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
		if (preferences != null) {
			preferences.registerOnSharedPreferenceChangeListener(this);
		}
	}

	@Override
	public void onPause() {
		SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
		if (preferences != null) {
			preferences.unregisterOnSharedPreferenceChangeListener(this);
		}
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (!PreferenceConstants.APP_THEME.equals(key)) {
			return;
		}

		AppThemeManager.applyFromPreferences(requireContext());
		if (getActivity() != null) {
			getActivity().recreate();
		}
	}
}
