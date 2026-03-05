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

package org.connectbot;

import android.app.Application;
import org.connectbot.util.AppThemeManager;
import org.connectbot.util.SecurityKeyDebugLog;

/**
 * Application entry point.
 *
 * Initialises the security-key debug ring buffer. The YubiKit YubiKitManager is
 * instantiated per-activity (SecurityKeyActivity) — it requires an Activity context
 * and no global singleton setup.
 */
public class ConnectBotApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppThemeManager.applyFromPreferences(this);
        SecurityKeyDebugLog.initialize(this);
        SecurityKeyDebugLog.logFlow(this, "ConnectBotApplication", "APP_CREATED");
    }
}
