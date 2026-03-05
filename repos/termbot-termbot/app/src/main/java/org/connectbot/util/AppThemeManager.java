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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

public final class AppThemeManager {
	private AppThemeManager() {
	}

	public static void applyFromPreferences(Context context) {
		SharedPreferences preferences =
				PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		String selectedTheme = preferences.getString(
				PreferenceConstants.APP_THEME, PreferenceConstants.APP_THEME_SYSTEM);
		AppCompatDelegate.setDefaultNightMode(resolveNightMode(selectedTheme));
	}

	private static int resolveNightMode(String selectedTheme) {
		if (PreferenceConstants.APP_THEME_LIGHT.equals(selectedTheme)) {
			return AppCompatDelegate.MODE_NIGHT_NO;
		}
		if (PreferenceConstants.APP_THEME_DARK.equals(selectedTheme)) {
			return AppCompatDelegate.MODE_NIGHT_YES;
		}
		return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
	}
}
