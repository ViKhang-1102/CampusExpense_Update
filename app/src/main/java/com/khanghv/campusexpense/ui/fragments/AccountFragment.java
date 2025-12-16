package com.khanghv.campusexpense.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.khanghv.campusexpense.MainActivity;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.ui.auth.LoginActivity;
import com.khanghv.campusexpense.util.CurrencyManager;
import com.khanghv.campusexpense.util.LocaleManager;

public class AccountFragment extends Fragment {

    private TextView welcomeText;
    private TextView usernameText;
    private TextView avatarText;
    private Button categoryButton;
    private Button logoutButton;
    private Button languageButton;
    private Button themeButton;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_account, container, false);

        welcomeText = view.findViewById(R.id.welcomeText);
        usernameText = view.findViewById(R.id.usernameText);
        avatarText = view.findViewById(R.id.avatarText);
        categoryButton = view.findViewById(R.id.categoryButton);
        languageButton = view.findViewById(R.id.languageButton);
        themeButton = view.findViewById(R.id.themeButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        sharedPreferences = getActivity().getSharedPreferences("user_prefs", 0);

        String username = sharedPreferences.getString("username", "");
        welcomeText.setText(R.string.welcome);
        usernameText.setText(username);

        if (!username.isEmpty()) {
            char firstLetter = username.charAt(0);
            avatarText.setText(String.valueOf(firstLetter));
        }

        categoryButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToCategoriesFragment();
            }
        });

        logoutButton.setOnClickListener(v -> logout());
        setupLanguageButton();
        setupThemeButton();
        return view;
    }

    private void logout() {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.clear();
    editor.apply();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void setupLanguageButton() {
        if (languageButton == null) {
            return;
        }
        updateLanguageButtonText();
        languageButton.setOnClickListener(v -> {
            LocaleManager.Language nextLanguage = LocaleManager.toggleLanguage(requireContext());
            CurrencyManager.CurrencyType nextCurrency =
                    nextLanguage == LocaleManager.Language.EN ?
                            CurrencyManager.CurrencyType.USD :
                            CurrencyManager.CurrencyType.VND;
            CurrencyManager.setDisplayCurrency(requireContext(), nextCurrency);
            // Đánh dấu để MainActivity biết cần reset về Home sau khi recreate
            SharedPreferences settingsPrefs = requireContext().getSharedPreferences("settings_prefs", 0);
            settingsPrefs.edit().putBoolean("reset_to_home_after_recreate", true).apply();
            Toast.makeText(requireContext(), R.string.currency_rate_updating, Toast.LENGTH_SHORT).show();
            CurrencyManager.refreshRateIfNeeded(requireContext(), true, new CurrencyManager.RateUpdateListener() {
                @Override
                public void onSuccess(double rate) {
                    updateLanguageButtonText();
                    requireActivity().recreate();
                }

                @Override
                public void onError(Exception exception) {
                    Toast.makeText(requireContext(), R.string.currency_rate_failed, Toast.LENGTH_SHORT).show();
                    updateLanguageButtonText();
                    requireActivity().recreate();
                }
            });
        });
    }

    private void setupThemeButton() {
        if (themeButton == null) {
            return;
        }
        updateThemeButtonText();
        themeButton.setOnClickListener(v -> {
            com.khanghv.campusexpense.util.ThemeManager.toggle(requireContext());
            SharedPreferences settingsPrefs = requireContext().getSharedPreferences("settings_prefs", 0);
            settingsPrefs.edit().putBoolean("reset_to_home_after_recreate", true).apply();
            updateThemeButtonText();
            requireActivity().recreate();
        });
    }

    private void updateLanguageButtonText() {
        LocaleManager.Language language = LocaleManager.getLanguage(requireContext());
        int textRes = language == LocaleManager.Language.VI ?
                R.string.switch_to_english :
                R.string.switch_to_vietnamese;
        languageButton.setText(textRes);
    }

    private void updateThemeButtonText() {
        com.khanghv.campusexpense.util.ThemeManager.ThemeMode mode =
                com.khanghv.campusexpense.util.ThemeManager.getTheme(requireContext());
        int textRes = mode == com.khanghv.campusexpense.util.ThemeManager.ThemeMode.DARK ?
                R.string.switch_to_light :
                R.string.switch_to_dark;
        if (themeButton != null) {
            themeButton.setText(textRes);
        }
    }
}
