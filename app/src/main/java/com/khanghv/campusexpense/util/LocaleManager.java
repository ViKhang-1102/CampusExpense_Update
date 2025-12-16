package com.khanghv.campusexpense.util;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

/**
 * Quản lý ngôn ngữ ứng dụng và cung cấp context đã được cấu hình Locale phù hợp.
 */
public final class LocaleManager {

    private static final String PREF_NAME = "settings_prefs";
    private static final String KEY_LANGUAGE = "app_language";
    private static SharedPreferences sharedPreferences;
    private LocaleManager() {}

    public enum Language {
        EN("en"),
        VI("vi");

        private final String code;

        Language(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static Language fromCode(String code) {
            for (Language language : values()) {
                if (language.code.equalsIgnoreCase(code)) {
                    return language;
                }
            }
            return VI;
        }
    }

    private static SharedPreferences getPrefs(Context context) {

        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    public static Language getLanguage(Context context) {
        sharedPreferences = context.getSharedPreferences("settings_prefs",MODE_PRIVATE);
        String stored =
                sharedPreferences.getString("app_language",Locale.getDefault().getLanguage());

        return Language.fromCode(stored);
    }

    public static void setLanguage(Context context, Language language) {
        getPrefs(context).edit()
                .putString(KEY_LANGUAGE, language.getCode())
                .apply();
    }

    public static Language toggleLanguage(Context context) {
        Language current = getLanguage(context);
        Language next = current == Language.VI ? Language.EN : Language.VI;
        setLanguage(context, next);
        return next;
    }

    public static Context wrap(Context context) {
        return updateResources(context, getLanguage(context).getCode());
    }

    public static void applyAppLocale(Context context) {
        updateResources(context, getLanguage(context).getCode());
    }

    private static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(configuration);
        } else {
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
}

