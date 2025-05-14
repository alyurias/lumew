package com.example.lumew.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class PrefsManager {
    private static final String PREF_DARK_MODE = "dark_mode";
    private static final String PREF_NOTIFICATIONS = "notifications_enabled";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(PREF_DARK_MODE, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_DARK_MODE, enabled).apply();
    }

    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(PREF_NOTIFICATIONS, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_NOTIFICATIONS, enabled).apply();
    }
}