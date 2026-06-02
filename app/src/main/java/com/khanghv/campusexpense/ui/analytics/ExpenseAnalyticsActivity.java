package com.khanghv.campusexpense.ui.analytics;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.base.BaseActivity;
import com.khanghv.campusexpense.data.ExpenseRepository;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.model.CategorySpendingStat;
import com.khanghv.campusexpense.data.model.MonthlyBudget;
import com.khanghv.campusexpense.util.CurrencyManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseAnalyticsActivity extends BaseActivity {
    private PieChart pieChart;
    private TextView tvAnalyticsTitle;
    private TextView tvEmpty;
    private TextView tvGoalSummary;
    private TextView tvGoalStatus;
    private MaterialButtonToggleGroup togglePeriod;
    private AnalyticsDetailAdapter detailAdapter;
    private ExpenseRepository repository;
    private int currentUserId = -1;
    private int selectedMonth;
    private int selectedYear;
    private int selectedWeek = 1;
    private double latestTotalSpent = 0.0;
    private int latestOverCategoryCount = 0;
    private int latestGoalCategoryCount = 0;
    private static final String PREFS_ANALYTICS = "analytics_prefs";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_analytics);

        repository = new ExpenseRepository(getApplication());
        currentUserId = getSharedPreferences("user_prefs", MODE_PRIVATE).getInt("userId", -1);
        Calendar now = Calendar.getInstance();
        selectedMonth = now.get(Calendar.MONTH) + 1;
        selectedYear = now.get(Calendar.YEAR);

        pieChart = findViewById(R.id.pieChart);
        tvAnalyticsTitle = findViewById(R.id.tvAnalyticsTitle);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvGoalSummary = findViewById(R.id.tvGoalSummary);
        tvGoalStatus = findViewById(R.id.tvGoalStatus);
        togglePeriod = findViewById(R.id.togglePeriod);
        RecyclerView rvDetail = findViewById(R.id.recyclerViewAnalyticsDetail);
        rvDetail.setLayoutManager(new LinearLayoutManager(this));
        detailAdapter = new AnalyticsDetailAdapter(this);
        rvDetail.setAdapter(detailAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFilterMonth).setOnClickListener(v -> showMonthYearPickerDialog());
        findViewById(R.id.btnFilterWeek).setOnClickListener(v -> showWeekPickerDialog());
        findViewById(R.id.btnSetGoal).setOnClickListener(v -> showSetGoalDialog());
        configureChart();
        togglePeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) loadAnalytics(checkedId == R.id.btnPeriodWeek);
        });
        loadAnalytics(false);
    }

    private void configureChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(54f);
        pieChart.setDrawCenterText(false);
        pieChart.setEntryLabelTextSize(11f);
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
    }

    private void loadAnalytics(boolean isWeekly) {
        if (currentUserId == -1) return;
        findViewById(R.id.btnFilterWeek).setVisibility(isWeekly ? View.VISIBLE : View.GONE);
        new Thread(() -> {
            long[] range = isWeekly ? getSelectedWeekRange() : getSelectedMonthRange();
            List<CategorySpendingStat> stats = repository.getSpendingStatsByRange(currentUserId, range[0], range[1]);
            Map<Integer, Double> budgetLimitMap = getBudgetLimitByCategory(false, selectedMonth, selectedYear);
            Map<Integer, Double> customGoalMap = getGoalMap(isWeekly, selectedMonth, selectedYear, selectedWeek);
            runOnUiThread(() -> renderAnalytics(stats, customGoalMap, isWeekly, selectedMonth, selectedYear, selectedWeek));
        }).start();
    }

    private Map<Integer, Double> getBudgetLimitByCategory(boolean isWeekly, int month, int year) {
        Map<Integer, Double> result = new HashMap<>();
        List<MonthlyBudget> budgets = AppDatabase.getInstance(this).monthlyBudgetDao()
                .getBudgetsByUserAndMonth(currentUserId, month, year);
        for (MonthlyBudget budget : budgets) {
            double limit = budget.getTotalBudget();
            if (isWeekly) {
                limit = limit / 4.0;
            }
            result.put(budget.getCategoryId(), limit);
        }
        return result;
    }

    private void renderAnalytics(List<CategorySpendingStat> stats, Map<Integer, Double> budgetLimitMap, boolean isWeekly, int month, int year, int week) {
        if (isWeekly) {
            tvAnalyticsTitle.setText(getString(R.string.analytics_title_week_selected, week, month, year));
        } else {
            tvAnalyticsTitle.setText(getString(R.string.analytics_title_month_selected, month, year));
        }
        if (stats == null || stats.isEmpty()) {
            pieChart.clear();
            detailAdapter.submitItems(new ArrayList<>());
            tvEmpty.setText(R.string.analytics_empty);
            tvEmpty.setVisibility(TextView.VISIBLE);
            latestTotalSpent = 0.0;
            latestGoalCategoryCount = 0;
            latestOverCategoryCount = 0;
            updateGoalSection(isWeekly);
            return;
        }

        tvEmpty.setVisibility(TextView.GONE);
        List<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        List<AnalyticsDetailAdapter.AnalyticsItem> details = new ArrayList<>();

        double total = 0.0;
        for (CategorySpendingStat stat : stats) {
            total += stat.getTotalSpent();
        }
        if (total <= 0) total = 1;
        latestTotalSpent = total;

        int[] palette = {
                0xFF3F51B5, 0xFF4CAF50, 0xFFFF9800, 0xFFE91E63, 0xFF00BCD4, 0xFF9C27B0, 0xFFF44336, 0xFF8BC34A
        };
        int colorIdx = 0;
        latestGoalCategoryCount = 0;
        latestOverCategoryCount = 0;
        for (CategorySpendingStat stat : stats) {
            float pct = (float) ((stat.getTotalSpent() / total) * 100f);
            entries.add(new PieEntry((float) stat.getTotalSpent(), stat.getCategoryName()));
            colors.add(palette[colorIdx % palette.length]);
            colorIdx++;

            AnalyticsDetailAdapter.AnalyticsItem item = new AnalyticsDetailAdapter.AnalyticsItem();
            item.categoryName = stat.getCategoryName();
            item.totalSpent = stat.getTotalSpent();
            item.percentage = pct;
            item.limitBudget = budgetLimitMap.get(stat.getCategoryId());
            item.isOverLimit = item.limitBudget != null && stat.getTotalSpent() > item.limitBudget;
            if (item.limitBudget != null && item.limitBudget > 0) {
                latestGoalCategoryCount++;
                if (item.isOverLimit) latestOverCategoryCount++;
            }
            details.add(item);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(8f);
        dataSet.setColors(colors);
        PieData pieData = new PieData(dataSet);
        pieData.setValueTextSize(12f);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(pieData);
        pieChart.invalidate();
        detailAdapter.submitItems(details);
        updateGoalSection(isWeekly);
    }

    private long[] getSelectedMonthRange() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, selectedYear);
        cal.set(Calendar.MONTH, selectedMonth - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        return new long[]{start, cal.getTimeInMillis()};
    }

    private long[] getSelectedWeekRange() {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.set(Calendar.YEAR, selectedYear);
        cal.set(Calendar.MONTH, selectedMonth - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int dayOffset = Math.max(0, (selectedWeek - 1) * 7);
        cal.add(Calendar.DAY_OF_MONTH, dayOffset);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, 6);
        Calendar endOfMonth = Calendar.getInstance();
        endOfMonth.set(Calendar.YEAR, selectedYear);
        endOfMonth.set(Calendar.MONTH, selectedMonth - 1);
        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
        endOfMonth.set(Calendar.MINUTE, 59);
        endOfMonth.set(Calendar.SECOND, 59);
        endOfMonth.set(Calendar.MILLISECOND, 999);
        long end = Math.min(cal.getTimeInMillis(), endOfMonth.getTimeInMillis());
        return new long[]{start, end};
    }

    private void showMonthYearPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View pickerView = LayoutInflater.from(this).inflate(R.layout.dialog_month_year_picker, null);
        android.widget.NumberPicker monthPicker = pickerView.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = pickerView.findViewById(R.id.yearPicker);
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        String[] months = getResources().getStringArray(R.array.months_numbers);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(selectedMonth - 1);
        yearPicker.setMinValue(year - 5);
        yearPicker.setMaxValue(year + 1);
        yearPicker.setValue(selectedYear);
        builder.setView(pickerView);
        builder.setPositiveButton(getString(R.string.apply_label), (d, which) -> {
            selectedMonth = monthPicker.getValue() + 1;
            selectedYear = yearPicker.getValue();
            selectedWeek = 1;
            reloadWithCurrentMode();
        });
        builder.setNegativeButton(getString(R.string.cancel_label), null);
        builder.show();
    }

    private void showWeekPickerDialog() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, selectedYear);
        cal.set(Calendar.MONTH, selectedMonth - 1);
        int totalWeeks = (int) Math.ceil(cal.getActualMaximum(Calendar.DAY_OF_MONTH) / 7.0);
        String[] weeks = new String[totalWeeks];
        for (int i = 0; i < totalWeeks; i++) {
            weeks[i] = getString(R.string.week_n, i + 1);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.analytics_filter_week)
                .setSingleChoiceItems(weeks, Math.max(0, selectedWeek - 1), (dialog, which) -> selectedWeek = which + 1)
                .setPositiveButton(R.string.apply_label, (dialog, which) -> reloadWithCurrentMode())
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private void showSetGoalDialog() {
        boolean isWeekly = togglePeriod.getCheckedButtonId() == R.id.btnPeriodWeek;
        Map<Integer, Double> currentBudgets = getBudgetLimitByCategory(false, selectedMonth, selectedYear);
        if (currentBudgets.isEmpty()) {
            Toast.makeText(this, R.string.analytics_no_budget_for_goal, Toast.LENGTH_SHORT).show();
            return;
        }
        Map<Integer, Double> stored = getGoalMap(isWeekly, selectedMonth, selectedYear, selectedWeek);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_analytics_goal, null);
        LinearLayout layoutGoalInputs = view.findViewById(R.id.layoutGoalInputs);
        TextView tvSubtitle = view.findViewById(R.id.tvGoalDialogSubtitle);
        tvSubtitle.setText(isWeekly
                ? getString(R.string.analytics_goal_dialog_week_subtitle, selectedWeek, selectedMonth, selectedYear)
                : getString(R.string.analytics_goal_dialog_month_subtitle, selectedMonth, selectedYear));

        AppDatabase db = AppDatabase.getInstance(this);
        List<MonthlyBudget> budgets = db.monthlyBudgetDao().getBudgetsByUserAndMonth(currentUserId, selectedMonth, selectedYear);
        Map<Integer, TextInputEditText> inputMap = new HashMap<>();
        for (MonthlyBudget budget : budgets) {
            com.khanghv.campusexpense.data.model.Category category = db.categoryDao().getById(budget.getCategoryId());
            if (category == null) continue;

            View row = LayoutInflater.from(this).inflate(R.layout.item_analytics_goal_target, layoutGoalInputs, false);
            TextView tvCategoryName = row.findViewById(R.id.tvCategoryName);
            TextView tvCurrentBudget = row.findViewById(R.id.tvCurrentBudget);
            TextInputLayout targetLayout = row.findViewById(R.id.targetInputLayout);
            TextInputEditText editText = row.findViewById(R.id.etTargetAmount);

            tvCategoryName.setText(category.getName());
            tvCurrentBudget.setText(getString(
                    R.string.analytics_current_budget_format,
                    CurrencyManager.formatDisplayCurrency(this, budget.getTotalBudget())
            ));
            targetLayout.setHint(getString(R.string.analytics_target_for_category_hint, category.getName()));

            double value = stored.containsKey(budget.getCategoryId()) ? stored.get(budget.getCategoryId()) : 0.0;
            if (value > 0) {
                editText.setText(CurrencyManager.formatEditableValue(this, value));
            }
            CurrencyManager.attachInputFormatter(editText);
            layoutGoalInputs.addView(row);
            inputMap.put(budget.getCategoryId(), editText);
        }

        builder.setView(view);
        builder.setTitle(R.string.analytics_set_goal);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            try {
                Map<Integer, Double> saveMap = new HashMap<>();
                for (Map.Entry<Integer, TextInputEditText> entry : inputMap.entrySet()) {
                    String raw = entry.getValue().getText() == null ? "" : entry.getValue().getText().toString().trim();
                    if (raw.isEmpty()) continue;
                    double parsed = CurrencyManager.toBaseCurrency(this, CurrencyManager.parseDisplayAmount(this, raw));
                    saveMap.put(entry.getKey(), Math.max(0, parsed));
                }
                saveGoalMap(isWeekly, selectedMonth, selectedYear, selectedWeek, saveMap);
                reloadWithCurrentMode();
            } catch (Exception e) {
                Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.show();
    }

    private void updateGoalSection(boolean isWeekly) {
        if (latestGoalCategoryCount <= 0) {
            tvGoalSummary.setText(getString(R.string.analytics_goal_empty));
            tvGoalStatus.setText(getString(R.string.analytics_goal_set_hint_specific));
            tvGoalStatus.setTextColor(getColor(android.R.color.darker_gray));
            return;
        }
        tvGoalSummary.setText(getString(
                isWeekly ? R.string.analytics_goal_week_count_format : R.string.analytics_goal_month_count_format,
                latestGoalCategoryCount
        ));
        if (latestOverCategoryCount > 0) {
            tvGoalStatus.setText(getString(R.string.analytics_goal_over_count, latestOverCategoryCount));
            tvGoalStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        } else {
            tvGoalStatus.setText(getString(R.string.analytics_goal_all_good));
            tvGoalStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        }
    }

    private SharedPreferences getAnalyticsPrefs() {
        return getSharedPreferences(PREFS_ANALYTICS, MODE_PRIVATE);
    }

    private String getGoalKey(boolean isWeekly, int month, int year, int week) {
        return (isWeekly ? "weekly_" + week : "monthly") + "_" + year + "_" + month;
    }

    private Map<Integer, Double> getGoalMap(boolean isWeekly, int month, int year, int week) {
        String encoded = getAnalyticsPrefs().getString(getGoalKey(isWeekly, month, year, week), "");
        Map<Integer, Double> map = new HashMap<>();
        if (encoded == null || encoded.trim().isEmpty()) return map;
        String[] pairs = encoded.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length != 2) continue;
            try {
                map.put(Integer.parseInt(parts[0]), Double.parseDouble(parts[1]));
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private void saveGoalMap(boolean isWeekly, int month, int year, int week, Map<Integer, Double> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Double> entry : map.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        getAnalyticsPrefs().edit().putString(getGoalKey(isWeekly, month, year, week), sb.toString()).apply();
    }

    private void reloadWithCurrentMode() {
        int checkedId = togglePeriod.getCheckedButtonId();
        loadAnalytics(checkedId == R.id.btnPeriodWeek);
    }
}
