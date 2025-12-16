package com.khanghv.campusexpense.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {
    private static final String PREFS = "settings_prefs";
    private static final String KEY_THEME = "app_theme";

    public enum ThemeMode { LIGHT, DARK }

    private ThemeManager() {}

    public static ThemeMode getTheme(Context context) {
        return ThemeMode.LIGHT;
    }

    public static void setTheme(Context context, ThemeMode mode) {
        applyTheme(ThemeMode.LIGHT);
    }

    public static void toggle(Context context) {
        applyTheme(ThemeMode.LIGHT);
    }

    public static void applySavedTheme(Context context) {
        applyTheme(ThemeMode.LIGHT);
    }

    private static void applyTheme(ThemeMode mode) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
