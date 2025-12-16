package com.khanghv.campusexpense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.base.BaseActivity;
import com.khanghv.campusexpense.ui.auth.LoginActivity;
import com.khanghv.campusexpense.ui.fragments.AccountFragment;
import com.khanghv.campusexpense.ui.fragments.AddFragment;
import com.khanghv.campusexpense.ui.fragments.BudgetFragment;
import com.khanghv.campusexpense.ui.fragments.CategoryFragment;
import com.khanghv.campusexpense.ui.fragments.HomeFragment;
import com.khanghv.campusexpense.ui.fragments.TransactionFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import  com.khanghv.campusexpense.ui.fragments.ExpenseFragment;

public class MainActivity extends BaseActivity {

    private SharedPreferences sharedPreferences;
    private BottomNavigationView bottomNavigation;
    private static final String SETTINGS_PREFS = "settings_prefs";
    private static final String KEY_RESET_HOME_AFTER_RECREATE = "reset_to_home_after_recreate";
    private boolean isLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }
    private void goToLoginAcitivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        if (!isLoggedIn()) {
            goToLoginAcitivity();
            return;
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_expense) {
                selectedFragment = new ExpenseFragment();
            }  else if (itemId == R.id.nav_budget) {
                selectedFragment = new BudgetFragment();
            } else if (itemId == R.id.nav_account) {
                selectedFragment = new AccountFragment();
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Luôn đặt tab Home làm mặc định sau khi Activity khởi tạo / recreate
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Nếu vừa đổi ngôn ngữ từ AccountFragment, ép bottom nav về Home
        SharedPreferences settingsPrefs = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        boolean shouldResetHome = settingsPrefs.getBoolean(KEY_RESET_HOME_AFTER_RECREATE, false);
        if (shouldResetHome && bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            settingsPrefs.edit().putBoolean(KEY_RESET_HOME_AFTER_RECREATE, false).apply();
        }
    }

    public void navigateToCategoriesFragment() {
        CategoryFragment categoryFragment = new CategoryFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, categoryFragment)
                .addToBackStack(null)
                .commit();
    }

}