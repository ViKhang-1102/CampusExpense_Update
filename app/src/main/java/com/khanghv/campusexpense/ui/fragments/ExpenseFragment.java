package com.khanghv.campusexpense.ui.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.BudgetDao;
import com.khanghv.campusexpense.data.database.CategoryDao;
import com.khanghv.campusexpense.data.database.ExpenseDao;
import com.khanghv.campusexpense.data.model.Budget;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.data.model.Expense;
import com.khanghv.campusexpense.ui.expense.CategoryExpenseAdapter;
import com.khanghv.campusexpense.ui.expense.ExpenseRecyclerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.khanghv.campusexpense.util.CurrencyManager;

public class ExpenseFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private TabLayout tabLayout;
    private Spinner monthSpinner;
    private Spinner categoryFilterSpinner;
    private TextView totalExpenseText;
    private TextView expenseCountText;
    private TextView emptyView;

    private ExpenseDao expenseDao;
    private CategoryDao categoryDao;
    private BudgetDao budgetDao;
    private SharedPreferences sharedPreferences;
    private int currentUserId;

    private CategoryExpenseAdapter categoryAdapter;
    private ExpenseRecyclerAdapter expenseAdapter;

    private List<Category> categoryList;
    private List<CategoryExpenseAdapter.CategoryExpenseItem> categoryExpenseList;
    private List<Expense> expenseList;

    private int currentMonth;
    private int currentYear;
    private int selectedCategoryId = -1;
    private int currentTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        tabLayout = view.findViewById(R.id.tabLayout);
        monthSpinner = view.findViewById(R.id.monthSpinner);
        categoryFilterSpinner = view.findViewById(R.id.categoryFilterSpinner);
        totalExpenseText = view.findViewById(R.id.totalExpenseText);
        expenseCountText = view.findViewById(R.id.expenseCountText);
        emptyView = view.findViewById(R.id.emptyView);

        sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt("userId", -1);

        AppDatabase database = AppDatabase.getInstance(requireContext());
        expenseDao = database.expenseDao();
        categoryDao = database.categoryDao();
        budgetDao = database.budgetDao();

        categoryList = new ArrayList<>();
        categoryExpenseList = new ArrayList<>();
        expenseList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);

        setupTabs();
        setupSpinners();
        setupRecyclerView();

        fabAdd.setOnClickListener(v -> showAddDialog());
        CurrencyManager.refreshRateIfNeeded(requireContext(), false, null);
        refreshData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data khi quay lại fragment
        if (currentUserId != -1) {
            refreshData();
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_by_category));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_by_date));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSpinners() {
        Calendar calendar = Calendar.getInstance();
        List<String> months = new ArrayList<>();
        int currentMonthIndex = calendar.get(Calendar.MONTH);
        int currentYearValue = calendar.get(Calendar.YEAR);

        for (int i = -6; i <= 6; i++) {
            calendar.set(currentYearValue, currentMonthIndex + i, 1);
            months.add(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime()));
        }

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
        monthSpinner.setSelection(6);
        monthSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                calendar.set(currentYearValue, currentMonthIndex + (position - 6), 1);
                currentMonth = calendar.get(Calendar.MONTH);
                currentYear = calendar.get(Calendar.YEAR);
                refreshData();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        List<String> categoryNames = new ArrayList<>();
        categoryNames.add(getString(R.string.all_categories));
        categoryList.clear();
        categoryList.addAll(categoryDao.getAllByUser(currentUserId));
        for (Category cat : categoryList) {
            categoryNames.add(cat.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(categoryAdapter);
        categoryFilterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedCategoryId = -1;
                } else {
                    selectedCategoryId = categoryList.get(position - 1).getId();
                }
                refreshData();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        categoryAdapter = new CategoryExpenseAdapter(categoryExpenseList, (categoryId, categoryName) -> {
            showCategoryExpensesDialog(categoryId, categoryName);
        });

        expenseAdapter = new ExpenseRecyclerAdapter(expenseList, categoryList,
                expense -> showEditDialog(expense),
                expense -> showDeleteDialog(expense));
        expenseAdapter.setContext(requireContext());
    }

    private void refreshData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startDate = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        long endDate = calendar.getTimeInMillis();

        if (currentTab == 0) {
            refreshCategoryData(startDate, endDate);
        } else {
            refreshDateData(startDate, endDate);
        }
    }

    private void refreshCategoryData(long startDate, long endDate) {
        categoryList.clear();
        categoryList.addAll(categoryDao.getAllByUser(currentUserId));

        categoryExpenseList.clear();

        List<Category> categories = new ArrayList<>();
        if (selectedCategoryId == -1) {
            categories.addAll(categoryList);
        } else {
            for (Category cat : categoryList) {
                if (cat.getId() == selectedCategoryId) {
                    categories.add(cat);
                    break;
                }
            }
        }

        for (Category category : categories) {
            List<Expense> expenses = expenseDao.getExpensesByCategoryAndDateRange(currentUserId, category.getId(), startDate, endDate);

            Double total = expenseDao.getTotalExpensesByCategoryAndDateRange(currentUserId, category.getId(), startDate, endDate);
            double totalExpense = total != null ? total : 0.0;

            if (totalExpense > 0 || selectedCategoryId != -1) {
                // Map monthly budget (if any) to legacy Budget object for display
                com.khanghv.campusexpense.data.model.MonthlyBudget mb = null;
                try {
                    mb = AppDatabase.getInstance(requireContext()).monthlyBudgetDao().getBudgetByCategoryUserMonth(currentUserId, category.getId(), currentMonth + 1, currentYear);
                } catch (Exception ignored) {}
                Budget budget = null;
                if (mb != null) {
                    budget = new Budget();
                    budget.setId(mb.getId());
                    budget.setUserId(mb.getUserId());
                    budget.setCategoryId(mb.getCategoryId());
                    budget.setAmount(mb.getTotalBudget());
                    budget.setPeriod("Monthly");
                    budget.setCreatedAt(mb.getCreatedAt());
                }
                categoryExpenseList.add(new CategoryExpenseAdapter.CategoryExpenseItem(
                        category.getId(),
                        category.getName(),
                        totalExpense,
                        expenses.size(),
                        budget
                ));
            }
        }

        recyclerView.setAdapter(categoryAdapter);
        categoryAdapter.notifyDataSetChanged();

        // Cập nhật statistics cho tab By Category
        updateStatistics(startDate, endDate);
        updateEmptyView();
    }

    private void refreshDateData(long startDate, long endDate) {
        if (selectedCategoryId == -1) {
            expenseList.clear();
            expenseList.addAll(expenseDao.getExpensesByDateRange(currentUserId, startDate, endDate));
        } else {
            expenseList.clear();
            expenseList.addAll(expenseDao.getExpensesByCategoryAndDateRange(currentUserId, selectedCategoryId, startDate, endDate));
        }

        categoryList.clear();
        categoryList.addAll(categoryDao.getAllByUser(currentUserId));
        expenseAdapter.setCategoryList(categoryList);

        recyclerView.setAdapter(expenseAdapter);
        expenseAdapter.updateExpenses(expenseList);

        updateStatistics(startDate, endDate);
        updateEmptyView();
    }

    private void updateStatistics(long startDate, long endDate) {
        Double total = selectedCategoryId == -1 ?
                expenseDao.getTotalExpensesByDateRange(currentUserId, startDate, endDate) :
                expenseDao.getTotalExpensesByCategoryAndDateRange(currentUserId, selectedCategoryId, startDate, endDate);

        double totalExpense = total != null ? total : 0.0;
        int count;
        if (currentTab == 0) {
            // Tab By Category - đếm tổng số transactions từ categoryExpenseList
            count = 0;
            for (CategoryExpenseAdapter.CategoryExpenseItem item : categoryExpenseList) {
                count += item.expenseCount;
            }
        } else {
            // Tab By Date - đếm từ expenseList
            count = expenseList.size();
        }

        totalExpenseText.setText(CurrencyManager.formatDisplayCurrency(requireContext(), totalExpense));
        expenseCountText.setText(String.valueOf(count));
    }

    private void updateEmptyView() {
        boolean isEmpty = (currentTab == 0 && categoryExpenseList.isEmpty()) ||
                (currentTab == 1 && expenseList.isEmpty());

        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddDialog() {
        categoryList.clear();
        categoryList.addAll(categoryDao.getAllByUser(currentUserId));

        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "Please add categories first", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputLayout amountLayout = dialogView.findViewById(R.id.amountLayout);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        if (amountLayout != null) {
            String symbol = CurrencyManager.getCurrencySymbol(requireContext());
            amountLayout.setHint(getString(R.string.amount_with_currency, symbol));
        }

        Button dateButton = dialogView.findViewById(R.id.dateButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        List<String> categoryNames = new ArrayList<>();
        for (Category cat : categoryList) {
            categoryNames.add(cat.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateButton.setText(getString(R.string.select_date));
        long[] selectedDate = {calendar.getTimeInMillis()};

        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate[0] = calendar.getTimeInMillis();
                        dateButton.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            int categoryPosition = categorySpinner.getSelectedItemPosition();
            String amountStr = amountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
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

            Category selectedCategory = categoryList.get(categoryPosition);
            Expense expense = new Expense(currentUserId, selectedCategory.getId(), amount, description, selectedDate[0]);
            // Link to monthly budget if exists for the expense's month/year
            Calendar calForExp = Calendar.getInstance();
            calForExp.setTimeInMillis(selectedDate[0]);
            int expMonth = calForExp.get(Calendar.MONTH) + 1;
            int expYear = calForExp.get(Calendar.YEAR);
            try {
                com.khanghv.campusexpense.data.model.MonthlyBudget mb = AppDatabase.getInstance(requireContext()).monthlyBudgetDao().getBudgetByCategoryUserMonth(currentUserId, selectedCategory.getId(), expMonth, expYear);
                if (mb != null) {
                    expense.setBudgetId(mb.getId());
                    double newRemaining = mb.getRemainingBudget() - amount;
                    if (newRemaining < 0) newRemaining = 0;
                    mb.setRemainingBudget(newRemaining);
                    AppDatabase.getInstance(requireContext()).monthlyBudgetDao().update(mb);
                }
            } catch (Exception ignored) {}

            expenseDao.insert(expense);

            refreshData();
            dialog.dismiss();
            Toast.makeText(requireContext(), R.string.expense_added, Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEditDialog(Expense expense) {
        categoryList.clear();
        categoryList.addAll(categoryDao.getAllByUser(currentUserId));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputLayout amountLayout = dialogView.findViewById(R.id.amountLayout);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        if (amountLayout != null) {
            String symbol = CurrencyManager.getCurrencySymbol(requireContext());
            amountLayout.setHint(getString(R.string.amount_with_currency, symbol));
        }

        Button dateButton = dialogView.findViewById(R.id.dateButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        List<String> categoryNames = new ArrayList<>();
        for (Category cat : categoryList) {
            categoryNames.add(cat.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        int categoryIndex = -1;
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).getId() == expense.getCategoryId()) {
                categoryIndex = i;
                break;
            }
        }
        if (categoryIndex >= 0) {
            categorySpinner.setSelection(categoryIndex);
        }
        categorySpinner.setEnabled(false);

        amountInput.setText(CurrencyManager.formatEditableValue(requireContext(), expense.getAmount()));
        descriptionInput.setText(expense.getDescription());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(expense.getDate());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateButton.setText(dateFormat.format(calendar.getTime()));
        long[] selectedDate = {expense.getDate()};

        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate[0] = calendar.getTimeInMillis();
                        dateButton.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
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

            // adjust monthly budget remaining if linked
            Integer linkedBudgetId = expense.getBudgetId();
            double oldAmount = expense.getAmount();
            expense.setAmount(amount);
            expense.setDescription(description);
            expense.setDate(selectedDate[0]);

            if (linkedBudgetId != null) {
                try {
                    com.khanghv.campusexpense.data.model.MonthlyBudget mb = AppDatabase.getInstance(requireContext()).monthlyBudgetDao().getById(linkedBudgetId);
                    if (mb != null) {
                        double remaining = mb.getRemainingBudget();
                        // revert old amount then deduct new amount: remaining += oldAmount - amount
                        double newRemaining = remaining + oldAmount - amount;
                        if (newRemaining < 0) newRemaining = 0;
                        mb.setRemainingBudget(newRemaining);
                        AppDatabase.getInstance(requireContext()).monthlyBudgetDao().update(mb);
                    }
                } catch (Exception ignored) {}
            }

            expenseDao.update(expense);

            refreshData();
            dialog.dismiss();
            Toast.makeText(requireContext(), R.string.expense_updated, Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteDialog(Expense expense) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_expense)
                .setMessage(R.string.confirm_delete_expense)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    // adjust monthly budget remaining if linked
                    Integer linkedBudgetId = expense.getBudgetId();
                    if (linkedBudgetId != null) {
                        try {
                            com.khanghv.campusexpense.data.model.MonthlyBudget mb = AppDatabase.getInstance(requireContext()).monthlyBudgetDao().getById(linkedBudgetId);
                            if (mb != null) {
                                double remaining = mb.getRemainingBudget();
                                double newRemaining = remaining + expense.getAmount();
                                mb.setRemainingBudget(newRemaining);
                                AppDatabase.getInstance(requireContext()).monthlyBudgetDao().update(mb);
                            }
                        } catch (Exception ignored) {}
                    }
                    expenseDao.delete(expense);
                    refreshData();
                    Toast.makeText(requireContext(), R.string.expense_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCategoryExpensesDialog(int categoryId, String categoryName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.expense_title, categoryName));

        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startDate = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        long endDate = calendar.getTimeInMillis();

        List<Expense> expenses = expenseDao.getExpensesByCategoryAndDateRange(currentUserId, categoryId, startDate, endDate);

        if (expenses.isEmpty()) {
            builder.setMessage(R.string.no_transactions);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
            return;
        }

        StringBuilder message = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        for (Expense expense : expenses) {
            message.append(dateFormat.format(new Date(expense.getDate())));
            message.append(" - ");
            message.append(CurrencyManager.formatDisplayCurrency(requireContext(), expense.getAmount()));
            if (expense.getDescription() != null && !expense.getDescription().trim().isEmpty()) {
                message.append("\n");
                message.append(expense.getDescription());
            }
            message.append("\n\n");
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }
}

