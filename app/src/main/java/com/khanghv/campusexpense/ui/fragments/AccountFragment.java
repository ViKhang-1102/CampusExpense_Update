package com.khanghv.campusexpense.ui.fragments;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.Observer;

import com.khanghv.campusexpense.MainActivity;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.FavoriteDao;
import com.khanghv.campusexpense.data.database.CategoryDao;
import com.khanghv.campusexpense.data.database.ExpenseDao;
import com.khanghv.campusexpense.data.database.MonthlyBudgetDao;
import com.khanghv.campusexpense.data.model.Expense;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.ui.auth.LoginActivity;
import com.khanghv.campusexpense.ui.expense.ExpenseRecyclerAdapter;
import com.khanghv.campusexpense.ui.home.BudgetBreakdownAdapter;
import com.khanghv.campusexpense.ui.home.YearSummaryAdapter;
import com.khanghv.campusexpense.util.CurrencyManager;
import com.khanghv.campusexpense.util.LocaleManager;

public class AccountFragment extends Fragment {

    private TextView welcomeText;
    private TextView usernameText;
    private TextView avatarText;
    private ImageView avatarImage;
    private Button categoryButton;
    private Button logoutButton;
    private Button languageButton;
    private Button themeButton;
    private android.widget.ImageButton btnEditAvatar;
    private Button btnToggleFavorites;
    private Button btnToggleYearSummary;
    private Button btnFilterYear;
    private RecyclerView favoritesRecyclerView;
    private RecyclerView yearSummaryRecyclerView;
    private TextView tvYearTotalSpent;
    private TextView tvYearCaption;
    private SharedPreferences sharedPreferences;
    private FavoriteDao favoriteDao;
    private CategoryDao categoryDao;
    private ExpenseDao expenseDao;
    private MonthlyBudgetDao monthlyBudgetDao;
    private int currentUserId;
    private ExpenseRecyclerAdapter favoritesAdapter;
    private YearSummaryAdapter yearSummaryAdapter;
    private int selectedYear;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_account, container, false);

        welcomeText = view.findViewById(R.id.welcomeText);
        usernameText = view.findViewById(R.id.usernameText);
        avatarText = view.findViewById(R.id.avatarText);
        avatarImage = view.findViewById(R.id.avatarImage);
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
        categoryButton = view.findViewById(R.id.categoryButton);
        languageButton = view.findViewById(R.id.languageButton);
        themeButton = view.findViewById(R.id.themeButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        btnToggleFavorites = view.findViewById(R.id.btnToggleFavorites);
        favoritesRecyclerView = view.findViewById(R.id.favoritesRecyclerView);
        btnToggleYearSummary = view.findViewById(R.id.btnToggleYearSummary);
        btnFilterYear = view.findViewById(R.id.btnFilterYear);
        yearSummaryRecyclerView = view.findViewById(R.id.yearSummaryRecyclerView);
        tvYearTotalSpent = view.findViewById(R.id.tvYearTotalSpent);
        tvYearCaption = view.findViewById(R.id.tvYearCaption);
        sharedPreferences = getActivity().getSharedPreferences("user_prefs", 0);
        AppDatabase db = AppDatabase.getInstance(requireContext());
        favoriteDao = db.favoriteDao();
        categoryDao = db.categoryDao();
        expenseDao = db.expenseDao();
        monthlyBudgetDao = db.monthlyBudgetDao();
        currentUserId = sharedPreferences.getInt("userId", -1);

        String username = sharedPreferences.getString("username", "");
        welcomeText.setText(R.string.welcome);
        usernameText.setText(username);

        if (!username.isEmpty()) {
            char firstLetter = username.charAt(0);
            avatarText.setText(String.valueOf(firstLetter));
        }
        String avatarUriStr = sharedPreferences.getString("avatar_uri", null);
        if (avatarUriStr != null) {
            Uri uri = Uri.parse(avatarUriStr);
            try {
                avatarImage.setImageURI(uri);
                avatarImage.setVisibility(View.VISIBLE);
                avatarText.setVisibility(View.GONE);
            } catch (Exception e) {
                avatarImage.setVisibility(View.GONE);
                avatarText.setVisibility(View.VISIBLE);
            }
        } else {
            avatarImage.setVisibility(View.GONE);
            avatarText.setVisibility(View.VISIBLE);
        }
        btnEditAvatar.setOnClickListener(v -> openImagePicker());

        categoryButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToCategoriesFragment();
            }
        });

        logoutButton.setOnClickListener(v -> logout());
        setupLanguageButton();
        if (themeButton != null) {
            themeButton.setVisibility(View.GONE);
        }
        setupResetDbButton(view);
        setupFavoritesSection();
        setupYearSummarySection();
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
                    if (!isAdded()) return;
                    updateLanguageButtonText();
                    restartApp();
                }

                @Override
                public void onError(Exception exception) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), R.string.currency_rate_failed, Toast.LENGTH_SHORT).show();
                    updateLanguageButtonText();
                    restartApp();
                }
            });
        });
    }

    private void restartApp() {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, 1001);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && data != null && data.getData() != null) {
            if (!isAdded()) return;
            Uri uri = data.getData();
            try {
                requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            sharedPreferences.edit().putString("avatar_uri", uri.toString()).apply();
            View view = getView();
            if (view != null) {
                avatarImage.setImageURI(uri);
                avatarImage.setVisibility(View.VISIBLE);
                avatarText.setVisibility(View.GONE);
            }
        }
    }

    private void setupFavoritesSection() {
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        favoritesAdapter = new ExpenseRecyclerAdapter(new java.util.ArrayList<>(), new java.util.ArrayList<>(),
                expense -> {},
                expense -> {
                    if (favoriteDao != null && currentUserId != -1) {
                        try {
                            favoriteDao.deleteByUserAndExpense(currentUserId, expense.getId());
                        } catch (Exception ignored) {}
                    }
                });
        favoritesAdapter.setContext(requireContext());
        favoritesAdapter.setCurrentUserId(currentUserId);
        java.util.List<Category> categories = new java.util.ArrayList<>();
        try {
            if (categoryDao != null && currentUserId != -1) {
                categories = categoryDao.getAllByUser(currentUserId);
            }
        } catch (Exception ignored) {}
        favoritesAdapter.setCategoryList(categories);
        favoritesRecyclerView.setAdapter(favoritesAdapter);
        btnToggleFavorites.setOnClickListener(v -> {
            boolean visible = favoritesRecyclerView.getVisibility() == View.VISIBLE;
            favoritesRecyclerView.setVisibility(visible ? View.GONE : View.VISIBLE);
            if (btnToggleFavorites instanceof com.google.android.material.button.MaterialButton) {
                ((com.google.android.material.button.MaterialButton) btnToggleFavorites)
                        .setIconResource(visible ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
            }
            try {
                sharedPreferences.edit().putBoolean("account_favorites_collapsed", visible).apply();
            } catch (Exception ignored) {}
        });
        boolean favoritesCollapsed = false;
        try {
            favoritesCollapsed = sharedPreferences.getBoolean("account_favorites_collapsed", false);
        } catch (Exception ignored) {}
        favoritesRecyclerView.setVisibility(favoritesCollapsed ? View.GONE : View.VISIBLE);
        if (btnToggleFavorites instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) btnToggleFavorites)
                    .setIconResource(favoritesCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
        }
        if (favoriteDao != null && currentUserId != -1) {
            try {
                favoriteDao.getFavoriteExpensesLive(currentUserId).observe(getViewLifecycleOwner(), new Observer<java.util.List<Expense>>() {
                    @Override
                    public void onChanged(java.util.List<Expense> expenses) {
                        favoritesAdapter.updateExpenses(expenses != null ? expenses : new java.util.ArrayList<>());
                    }
                });
            } catch (Exception ignored) {}
        }
    }

    private void setupYearSummarySection() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        selectedYear = cal.get(java.util.Calendar.YEAR);
        yearSummaryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        yearSummaryAdapter = new YearSummaryAdapter();
        yearSummaryRecyclerView.setAdapter(yearSummaryAdapter);
        btnFilterYear.setOnClickListener(v -> showYearPickerDialog());
        btnToggleYearSummary.setOnClickListener(v -> {
            boolean visible = yearSummaryRecyclerView.getVisibility() == View.VISIBLE;
            yearSummaryRecyclerView.setVisibility(visible ? View.GONE : View.VISIBLE);
            if (btnToggleYearSummary instanceof com.google.android.material.button.MaterialButton) {
                ((com.google.android.material.button.MaterialButton) btnToggleYearSummary)
                        .setIconResource(visible ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
            }
            try {
                sharedPreferences.edit().putBoolean("account_year_summary_collapsed", visible).apply();
            } catch (Exception ignored) {}
        });
        boolean summaryCollapsed = false;
        try {
            summaryCollapsed = sharedPreferences.getBoolean("account_year_summary_collapsed", false);
        } catch (Exception ignored) {}
        yearSummaryRecyclerView.setVisibility(summaryCollapsed ? View.GONE : View.VISIBLE);
        if (btnToggleYearSummary instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) btnToggleYearSummary)
                    .setIconResource(summaryCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
        }
        updateYearSummary();
    }

    private void showYearPickerDialog() {
        if (!isAdded()) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View pickerView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_year_picker, null);
        if (pickerView instanceof android.view.ViewGroup) {
            View firstChild = ((android.view.ViewGroup) pickerView).getChildAt(0);
            if (firstChild instanceof android.widget.TextView) {
                ((android.widget.TextView) firstChild).setText(getString(R.string.select_year));
            }
        }
        android.widget.NumberPicker monthPicker = pickerView.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = pickerView.findViewById(R.id.yearPicker);
        if (monthPicker != null) monthPicker.setVisibility(View.GONE);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        yearPicker.setMinValue(year - 9);
        yearPicker.setMaxValue(year + 1);
        yearPicker.setValue(year);
        builder.setView(pickerView);
        builder.setPositiveButton(getString(R.string.apply_label), (d, which) -> {
            selectedYear = yearPicker.getValue();
            if (btnFilterYear instanceof android.widget.Button) {
                ((android.widget.Button) btnFilterYear).setText(String.valueOf(selectedYear));
            }
            updateYearSummary();
        });
        builder.setNegativeButton(getString(R.string.cancel_label), null);
        builder.show();
    }

    private void updateYearSummary() {
        if (!isAdded()) return;
        double yearTotal = 0;
        java.util.List<YearSummaryAdapter.Item> items = new java.util.ArrayList<>();
        double maxSpent = 0;
        for (int m = 1; m <= 12; m++) {
            long[] range = getMonthRange(selectedYear, m);
            Double total = null;
            try {
                if (expenseDao != null && currentUserId != -1) {
                    total = expenseDao.getTotalExpensesByDateRange(currentUserId, range[0], range[1]);
                }
            } catch (Exception ignored) {}
            double spent = total != null ? total : 0;
            yearTotal += spent;
            if (spent > maxSpent) maxSpent = spent;
            items.add(new YearSummaryAdapter.Item(selectedYear, m, spent));
        }
        yearSummaryAdapter.setMaxSpent(maxSpent);
        yearSummaryAdapter.setItems(items);
        if (tvYearTotalSpent != null) {
            try {
                tvYearTotalSpent.setText(CurrencyManager.formatDisplayCurrency(requireContext(), yearTotal));
            } catch (Exception ignored) {}
        }
        if (tvYearCaption != null && isAdded()) {
            try {
                tvYearCaption.setText(getString(R.string.year_total_spent_title) + " · " + selectedYear);
            } catch (Exception ignored) {}
        }
    }

    private long[] getMonthRange(int year, int month) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.YEAR, year);
        cal.set(java.util.Calendar.MONTH, month - 1);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(java.util.Calendar.MONTH, 1);
        cal.add(java.util.Calendar.MILLISECOND, -1);
        long end = cal.getTimeInMillis();
        return new long[]{start, end};
    }
    
    private void setupThemeButton() {}

    private void updateLanguageButtonText() {
        android.content.Context ctx = getContext();
        if (ctx == null || languageButton == null) return;
        LocaleManager.Language language = LocaleManager.getLanguage(ctx);
        int textRes = language == LocaleManager.Language.VI ?
                R.string.switch_to_english :
                R.string.switch_to_vietnamese;
        languageButton.setText(textRes);
    }

    private void updateThemeButtonText() {}
    
    private void setupResetDbButton(View view) {
        android.widget.Button resetButton = view.findViewById(R.id.resetDbButton);
        if (resetButton == null) return;
        resetButton.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.reset_db))
                    .setMessage(getString(R.string.confirm_reset_db))
                    .setPositiveButton(getString(R.string.delete), (d, w) -> {
                        new Thread(() -> {
                            com.khanghv.campusexpense.data.database.AppDatabase db = com.khanghv.campusexpense.data.database.AppDatabase.getInstance(requireContext());
                            db.clearAllTables();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.clear();
                            editor.apply();
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(), getString(R.string.database_reset), Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                });
                            }
                        }).start();
                    })
                    .setNegativeButton(getString(R.string.cancel_label), null)
                    .show();
        });
    }
}
