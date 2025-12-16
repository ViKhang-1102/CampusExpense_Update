package com.khanghv.campusexpense;

import android.app.Application;
import android.content.Context;

import com.khanghv.campusexpense.util.CurrencyManager;
import com.khanghv.campusexpense.util.LocaleManager;

public class CampusExpenseApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrap(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocaleManager.applyAppLocale(this);
        com.khanghv.campusexpense.util.ThemeManager.applySavedTheme(this);
        CurrencyManager.refreshRateIfNeeded(this, false, null);
    }
}

