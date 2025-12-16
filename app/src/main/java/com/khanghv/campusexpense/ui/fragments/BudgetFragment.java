package com.khanghv.campusexpense.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.BudgetDao;
import com.khanghv.campusexpense.data.database.CategoryDao;
import com.khanghv.campusexpense.data.model.Budget;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.ui.budget.BudgetRecyclerAdapter;
import com.khanghv.campusexpense.util.CurrencyManager;

import java.util.ArrayList;
import java.util.List;

public class BudgetFragment extends Fragment {
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private BudgetRecyclerAdapter adapter;
    private List<Budget> budgetList;
    private List<Category> categoryList;
    private List<String> categoryNames;
    private BudgetDao budgetDao;
    private CategoryDao categoryDao;
    private com.khanghv.campusexpense.data.database.ExpenseDao expenseDao;
    private TextView emptyView;
    private SharedPreferences sharedPreferences;
    private int currentUserId;
    private com.khanghv.campusexpense.data.database.MonthlyBudgetDao monthlyBudgetDao;
    private int currentMonth;
    private int currentYear;
    private com.khanghv.campusexpense.data.ExpenseRepository repository;
    private int selectedCategoryId = -1;
    private String selectedCategoryName;


    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        emptyView = view.findViewById(R.id.emptyView);
        sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt("userId", -1);
        AppDatabase db = AppDatabase.getInstance(requireContext());
        budgetDao = db.budgetDao();
        expenseDao = db.expenseDao();
        monthlyBudgetDao = db.monthlyBudgetDao();
        categoryDao = db.categoryDao();
        budgetList = new ArrayList<>();
        categoryList = new ArrayList<>();
        categoryNames = new ArrayList<>();
        adapter = new BudgetRecyclerAdapter(budgetList, categoryNames, this::showEditDialog, this::showDeleteDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        fabAdd.setOnClickListener(v -> showAddBudgetDialog());
        // Tap emptyView to open month/year filter dialog
        emptyView.setOnClickListener(v -> showMonthYearPickerDialog());
        View btnSelectMonth = view.findViewById(R.id.btnSelectMonth);
        if (btnSelectMonth != null) {
            btnSelectMonth.setOnClickListener(v -> showMonthYearPickerDialog());
        }
        View btnFilterCategory = view.findViewById(R.id.btnFilterCategory);
        if (btnFilterCategory != null) {
            btnFilterCategory.setOnClickListener(v -> showCategoryFilterDialog());
        }
        CurrencyManager.refreshRateIfNeeded(requireContext(), false, null);
        repository = new com.khanghv.campusexpense.data.ExpenseRepository((android.app.Application) requireContext().getApplicationContext());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        currentMonth = cal.get(java.util.Calendar.MONTH) + 1; // 1-12
        currentYear = cal.get(java.util.Calendar.YEAR);
        selectedCategoryName = getString(R.string.all_categories);
        refreshBudgetList();
        return view;
    }

    private void showMonthYearPickerDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_year_picker, null);
        android.widget.NumberPicker monthPicker = view.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = view.findViewById(R.id.yearPicker);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        String[] months = getResources().getStringArray(R.array.months_numbers);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(currentMonth - 1);
        yearPicker.setMinValue(year - 5);
        yearPicker.setMaxValue(year + 1);
        yearPicker.setValue(currentYear);
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.apply_label), (d, which) -> {
            currentMonth = monthPicker.getValue() + 1;
            currentYear = yearPicker.getValue();
            // ensure budgets exist for this month by carrying over previous month
            repository.ensureBudgetsForMonth(String.format(java.util.Locale.getDefault(), "%04d-%02d", currentYear, currentMonth), currentUserId);
            refreshBudgetList();
        });
        builder.setNegativeButton(getString(R.string.cancel_label), null);
        builder.show();
    }

    private void refreshBudgetList() {
        // Cập nhật text cho nút Select Month
        View btnSelectMonth = getView() != null ? getView().findViewById(R.id.btnSelectMonth) : null;
        if (btnSelectMonth instanceof android.widget.Button) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(currentYear, currentMonth - 1, 1);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault());
            ((android.widget.Button) btnSelectMonth).setText(sdf.format(cal.getTime()));
        }

        budgetList.clear();
        categoryNames.clear();
        java.util.List<com.khanghv.campusexpense.data.model.MonthlyBudget> mbs = monthlyBudgetDao.getBudgetsByUserAndMonth(currentUserId, currentMonth, currentYear);
        for (com.khanghv.campusexpense.data.model.MonthlyBudget mb : mbs) {
            Budget b = new Budget();
            b.setId(mb.getId());
            b.setUserId(mb.getUserId());
            b.setCategoryId(mb.getCategoryId());
            b.setAmount(mb.getTotalBudget());
            b.setPeriod("Monthly");
            b.setCreatedAt(mb.getCreatedAt());
            budgetList.add(b);
            Category category = categoryDao.getById(b.getCategoryId());
            categoryNames.add(category == null ? "Unknown Category" : category.getName());
        }
        adapter.notifyDataSetChanged();
        if (budgetList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showCategoryFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_choose_category, null);
        RecyclerView rv = dialogView.findViewById(R.id.recyclerViewCategories);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        List<Category> cats = new ArrayList<>();
        Category allCat = new Category();
        allCat.setId(-1);
        allCat.setName(getString(R.string.all_categories));
        cats.add(allCat);
        cats.addAll(categoryDao.getAllByUser(currentUserId));

        final android.app.AlertDialog[] dialogPtr = new android.app.AlertDialog[1];
        com.khanghv.campusexpense.ui.adapters.CategorySelectionAdapter adapter = new com.khanghv.campusexpense.ui.adapters.CategorySelectionAdapter(cats, category -> {
            selectedCategoryId = category.getId();
            selectedCategoryName = category.getName();
            View btnFilterCategory = getView() != null ? getView().findViewById(R.id.btnFilterCategory) : null;
            if (btnFilterCategory instanceof android.widget.Button) {
                ((android.widget.Button) btnFilterCategory).setText(selectedCategoryName);
            }
            refreshBudgetList();
            if (dialogPtr[0] != null) dialogPtr[0].dismiss();
        });
        
        adapter.setSelectedCategoryId(selectedCategoryId);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        builder.setView(dialogView);
        dialogPtr[0] = builder.create();
        
        btnCancel.setOnClickListener(v -> dialogPtr[0].dismiss());
        if (dialogPtr[0].getWindow() != null) {
            dialogPtr[0].getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialogPtr[0].show();
    }

    private void showDeleteDialog(Budget budget) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_budget))
                .setMessage(getString(R.string.confirm_delete_budget))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                        // delete related transactions and monthly budget record
                        expenseDao.deleteExpensesByBudgetId(budget.getId());
                        com.khanghv.campusexpense.data.model.MonthlyBudget mbToDelete = monthlyBudgetDao.getBudgetByCategoryUserMonth(currentUserId, budget.getCategoryId(), currentMonth, currentYear);
                        if (mbToDelete != null) {
                            monthlyBudgetDao.delete(mbToDelete);
                        }
                        refreshBudgetList();
                        Toast.makeText(requireContext(), getString(R.string.budget_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showAddBudgetDialog() {
        categoryList.clear();
        categoryList.addAll(categoryDao.getAllByUser(currentUserId));

        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "Please add categories first", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget, null);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputLayout amountLayout = dialogView.findViewById(R.id.amountLayout);
        Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        if (amountLayout != null) {
            String symbol = CurrencyManager.getCurrencySymbol(requireContext());
            amountLayout.setHint(getString(R.string.amount_with_currency, symbol));
        }
        CurrencyManager.attachInputFormatter(amountInput);

        List<String> categoryNameList = new ArrayList<>();
        for (Category cat : categoryList) {
            categoryNameList.add(cat.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNameList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        String[] periods = {getString(R.string.period_monthly), getString(R.string.period_weekly)};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        saveButton.setOnClickListener(v -> {
            int categoryPosition = categorySpinner.getSelectedItemPosition();
            String amountStr = amountInput.getText().toString().trim();
            int periodPosition = periodSpinner.getSelectedItemPosition();
            double displayAmount = CurrencyManager.parseDisplayAmount(requireContext(), amountStr);
            double amount = CurrencyManager.toBaseCurrency(requireContext(), displayAmount);
            Category selectedCategory = categoryList.get(categoryPosition);
            // For monthly budgets, ensure uniqueness per month/year
            com.khanghv.campusexpense.data.model.MonthlyBudget existingMb = monthlyBudgetDao.getBudgetByCategoryUserMonth(currentUserId, selectedCategory.getId(), currentMonth, currentYear);
            if (existingMb != null) {
                Toast.makeText(requireContext(), "Budget for this category already exists for selected month", Toast.LENGTH_SHORT).show();
                return;
            }
            com.khanghv.campusexpense.data.model.MonthlyBudget mb = new com.khanghv.campusexpense.data.model.MonthlyBudget(currentUserId, selectedCategory.getId(), currentMonth, currentYear, amount, amount);
            monthlyBudgetDao.insert(mb);
            refreshBudgetList();
            dialog.dismiss();
            Toast.makeText(requireContext(), "Budget added successfully", Toast.LENGTH_SHORT).show();
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEditDialog(Budget budget) {
        categoryList.clear();
        categoryList.addAll(categoryDao.getAllByUser(currentUserId));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputLayout amountLayout = dialogView.findViewById(R.id.amountLayout);
        Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        if (amountLayout != null) {
            String symbol = CurrencyManager.getCurrencySymbol(requireContext());
            amountLayout.setHint(getString(R.string.amount_with_currency, symbol));
        }
        CurrencyManager.attachInputFormatter(amountInput);

        List<String> categoryNameList = new ArrayList<>();
        for (Category cat : categoryList) {
            categoryNameList.add(cat.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNameList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        Category category = categoryDao.getById(budget.getCategoryId());
        if (category != null) {
            int categoryIndex = -1;
            for (int i = 0; i < categoryList.size(); i++) {
                if (categoryList.get(i).getId() == budget.getCategoryId()) {
                    categoryIndex = i;
                    break;
                }
            }
            if (categoryIndex >= 0) {
                categorySpinner.setSelection(categoryIndex);
            }
        }
        categorySpinner.setEnabled(false);

        amountInput.setText(CurrencyManager.formatEditableValue(requireContext(), budget.getAmount()));

        String[] periods = {getString(R.string.period_monthly), getString(R.string.period_weekly)};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);

        int periodIndex = -1;
        for (int i = 0; i < periods.length; i++) {
            if (periods[i].equals(budget.getPeriod())) {
                periodIndex = i;
                break;
            }
        }
        if (periodIndex >= 0) {
            periodSpinner.setSelection(periodIndex);
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            int periodPosition = periodSpinner.getSelectedItemPosition();

            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            if (periodPosition < 0 || periodPosition >= periods.length) {
                Toast.makeText(requireContext(), "Please select period", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                double displayAmount = CurrencyManager.parseDisplayAmount(requireContext(), amountStr);
                amount = CurrencyManager.toBaseCurrency(requireContext(), displayAmount);
                if (amount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update corresponding monthly budget record for current month/year
            com.khanghv.campusexpense.data.model.MonthlyBudget mb = monthlyBudgetDao.getBudgetByCategoryUserMonth(currentUserId, budget.getCategoryId(), currentMonth, currentYear);
            if (mb != null) {
                mb.setTotalBudget(amount);
                mb.setRemainingBudget(amount);
                monthlyBudgetDao.update(mb);
            }
            refreshBudgetList();
            dialog.dismiss();
            Toast.makeText(requireContext(), "Budget updated successfully", Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}


