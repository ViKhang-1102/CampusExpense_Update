package com.khanghv.campusexpense.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat; // Đảm bảo đã import
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.ExpenseRepository;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.CategoryDao;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.data.model.User;
import com.khanghv.campusexpense.ui.home.BudgetBreakdownAdapter;
import com.khanghv.campusexpense.util.CurrencyManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final String PREFS_HOME = "home_filter_prefs";
    private static final String KEY_MONTH_YEAR = "selected_month_year";
    private TextView tvGreeting, tvTotalSpent, tvTransactionCount, tvAvgPerDay, tvBudget,
            tvTotalBudget, tvSpent, tvRemaining;
    private RecyclerView recyclerViewBreakdown;
    private BudgetBreakdownAdapter breakdownAdapter;
    private androidx.recyclerview.widget.RecyclerView recyclerViewWeekly;
    private com.khanghv.campusexpense.ui.home.WeekSummaryAdapter weeklyAdapter;
    private ExpenseRepository repository;
    private CategoryDao categoryDao;
    private int currentUserId;
    private String currentMonthYear;
    private int selectedCategoryId = -1;
    private String selectedCategoryName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Init views
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        tvAvgPerDay = view.findViewById(R.id.tvAvgPerDay);
        tvBudget = view.findViewById(R.id.tvBudget);
        recyclerViewBreakdown = view.findViewById(R.id.recyclerViewBreakdown);
        recyclerViewWeekly = view.findViewById(R.id.recyclerViewWeekly);
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        tvSpent = view.findViewById(R.id.tvSpent);
        tvRemaining = view.findViewById(R.id.tvRemaining);

        // Setup breakdown RecyclerView
        breakdownAdapter = new BudgetBreakdownAdapter(new ArrayList<>());
        recyclerViewBreakdown.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewBreakdown.setAdapter(breakdownAdapter);
        weeklyAdapter = new com.khanghv.campusexpense.ui.home.WeekSummaryAdapter();
        recyclerViewWeekly.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewWeekly.setAdapter(weeklyAdapter);

        // Init repo
        repository = new ExpenseRepository((android.app.Application) requireContext().getApplicationContext());
        categoryDao = AppDatabase.getInstance(requireContext()).categoryDao();
        selectedCategoryName = getString(R.string.all_categories);

        View btnSelectMonth = view.findViewById(R.id.btnSelectMonth);
        if (btnSelectMonth != null) {
            btnSelectMonth.setOnClickListener(v -> showMonthYearPickerDialog());
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại toàn bộ dữ liệu để đảm bảo tính nhất quán
        refreshData();
    }

    private void refreshData() {
        // Lấy thông tin mới nhất mỗi khi refresh
        currentUserId = getCurrentUserId();
        currentMonthYear = getSelectedMonthYear();

        // Cập nhật text cho nút Select Month
        View btnSelectMonth = getView() != null ? getView().findViewById(R.id.btnSelectMonth) : null;
        if (btnSelectMonth instanceof android.widget.Button) {
            String[] parts = currentMonthYear.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, 1);
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            ((android.widget.Button) btnSelectMonth).setText(sdf.format(cal.getTime()));
        }

        // Làm tươi tỷ giá nếu cần (không ép buộc, dùng cache nếu còn hạn)
        CurrencyManager.refreshRateIfNeeded(requireContext(), false, null);

        // Kiểm tra userId hợp lệ
        if (currentUserId == -1) {
            tvGreeting.setText(getString(R.string.greeting, "User"));
            return;
        }

        // Load user cho greeting
        repository.getUserById(currentUserId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                tvGreeting.setText(String.format(getString(R.string.greeting), user.getUsername()));
            } else {
                tvGreeting.setText(getString(R.string.greeting, "User")); // Giá trị mặc định
            }
        });

        // Load dữ liệu tài chính
        loadDataForMonth(currentMonthYear, currentUserId);
    }


    private void loadDataForMonth(String monthYear, int userId) {
        final Double[] totalSpentValue = {0.0};
        final Double[] budgetValue = {0.0};

        // Observe total spent
        repository.getTotalSpentForMonth(monthYear, userId).observe(getViewLifecycleOwner(), totalSpent -> {
            if (totalSpent == null) totalSpent = 0.0;
            totalSpentValue[0] = totalSpent;
            String formatted = CurrencyManager.formatDisplayCurrency(requireContext(), totalSpent);
            tvTotalSpent.setText(formatted);
            tvSpent.setText(formatted);
            // Cập nhật với budget hiện tại
            updateSummaryAndBreakdown(budgetValue[0], totalSpent);
            updateAvgPerDay(totalSpent);
        });

        // Observe count
        repository.getTransactionCountForMonth(monthYear, userId).observe(getViewLifecycleOwner(), count -> {
            if (count == null) count = 0;
            tvTransactionCount.setText(String.valueOf(count));
        });

        // Observe total budget for the month
        repository.getTotalBudgetForMonth(monthYear, userId).observe(getViewLifecycleOwner(), totalBudget -> {
            if (totalBudget == null) {
                totalBudget = 0.0;
            }
            budgetValue[0] = totalBudget;
            String formatted = CurrencyManager.formatDisplayCurrency(requireContext(), totalBudget);
            tvBudget.setText(formatted);
            tvTotalBudget.setText(formatted);
            // Cập nhật với spent hiện tại
            updateSummaryAndBreakdown(totalBudget, totalSpentValue[0]);
        });

        // Observe budget breakdown
        repository.getBudgetBreakdown(monthYear, userId).observe(getViewLifecycleOwner(), breakdownList -> {
            if (breakdownList != null) {
                breakdownAdapter.updateBreakdownList(breakdownList);
            }
        });

        updateWeeklySummary(monthYear, userId);
    }

    private void updateSummaryAndBreakdown(double budget, double spent) {
        double remaining = budget - spent;
        String remainingText = CurrencyManager.formatDisplayCurrency(requireContext(), remaining);
        tvRemaining.setText(remainingText);

        int colorRes = remaining < 0 ? android.R.color.holo_red_dark : android.R.color.holo_green_dark;
        tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    private void updateAvgPerDay(double totalSpent) {
        int daysInMonth = getDaysInMonth(currentMonthYear);
        if (daysInMonth > 0) {
            double avgDaily = totalSpent / daysInMonth;
            tvAvgPerDay.setText(CurrencyManager.formatDisplayCurrency(requireContext(), avgDaily));
        }
    }

    private String getCurrentMonthYear() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    private String getSelectedMonthYear() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_HOME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_MONTH_YEAR, null);
        if (saved != null && saved.matches("\\d{4}-\\d{2}")) {
            return saved;
        }
        return getCurrentMonthYear();
    }

    private int getDaysInMonth(String monthYear) {
        String[] parts = monthYear.split("-");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, Integer.parseInt(parts[0]));
        cal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private int getCurrentUserId() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return prefs.getInt("userId", -1);
    }

    private void showMonthYearPickerDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View pickerView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_year_picker, null);
        android.widget.NumberPicker monthPicker = pickerView.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = pickerView.findViewById(R.id.yearPicker);
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        String[] parts = getCurrentMonthYear().split("-");
        int currentMonth = Integer.parseInt(parts[1]);
        int currentYear = Integer.parseInt(parts[0]);
        String[] months = getResources().getStringArray(R.array.months_numbers);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(currentMonth - 1);
        yearPicker.setMinValue(year - 5);
        yearPicker.setMaxValue(year + 1);
        yearPicker.setValue(currentYear);
        builder.setView(pickerView);
        builder.setPositiveButton(getString(R.string.apply_label), (d, which) -> {
            int selMonth = monthPicker.getValue() + 1;
            int selYear = yearPicker.getValue();
            currentMonthYear = String.format(Locale.getDefault(), "%04d-%02d", selYear, selMonth);
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_HOME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_MONTH_YEAR, currentMonthYear).apply();
            refreshData();
        });
        builder.setNegativeButton(getString(R.string.cancel_label), null);
        builder.show();
    }

    private void updateWeeklySummary(String monthYear, int userId) {
        String[] parts = monthYear.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startMonth = cal.getTimeInMillis();
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        java.util.List<com.khanghv.campusexpense.ui.home.WeekSummaryAdapter.WeekSummaryItem> items = new java.util.ArrayList<>();
        long cursor = startMonth;
        int weekIndex = 1;
        Calendar todayCal = Calendar.getInstance();
        boolean isSelectedCurrentMonth = (todayCal.get(Calendar.YEAR) == year) && (todayCal.get(Calendar.MONTH) == month);
        long todayMillis = todayCal.getTimeInMillis();
        com.khanghv.campusexpense.data.database.ExpenseDao expenseDao = com.khanghv.campusexpense.data.database.AppDatabase.getInstance(requireContext()).expenseDao();
        while (cursor < startMonth + (long) daysInMonth * 24L * 60L * 60L * 1000L) {
            long weekStart = cursor;
            long weekEnd = Math.min(weekStart + 7L * 24L * 60L * 60L * 1000L - 1, startMonth + (long) daysInMonth * 24L * 60L * 60L * 1000L - 1);
            Double total = expenseDao.getTotalExpensesByDateRange(userId, weekStart, weekEnd);
            int count = expenseDao.getExpenseCountByDateRange(userId, weekStart, weekEnd);
            double spent = total != null ? total : 0.0;
            String label = getString(R.string.week_n, weekIndex);
            boolean highlight = isSelectedCurrentMonth && todayMillis >= weekStart && todayMillis <= weekEnd;
            items.add(new com.khanghv.campusexpense.ui.home.WeekSummaryAdapter.WeekSummaryItem(label, spent, count, highlight));
            cursor = weekEnd + 1;
            weekIndex++;
        }
        weeklyAdapter.setItems(items);
    }
}
