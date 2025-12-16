package com.khanghv.campusexpense.base;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.khanghv.campusexpense.util.LocaleManager;

/**
 * Bảo đảm mọi Activity luôn khởi tạo với Locale đã chọn.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocaleManager.applyAppLocale(this);
    }
}

