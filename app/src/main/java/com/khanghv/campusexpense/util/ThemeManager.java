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
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_THEME, ThemeMode.LIGHT.name());
        try {
            return ThemeMode.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return ThemeMode.LIGHT;
        }
    }

    public static void setTheme(Context context, ThemeMode mode) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, mode.name()).apply();
        applyTheme(mode);
    }

    public static void toggle(Context context) {
        ThemeMode current = getTheme(context);
        setTheme(context, current == ThemeMode.LIGHT ? ThemeMode.DARK : ThemeMode.LIGHT);
    }

    public static void applySavedTheme(Context context) {
        applyTheme(getTheme(context));
    }

    private static void applyTheme(ThemeMode mode) {
        if (mode == ThemeMode.DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
