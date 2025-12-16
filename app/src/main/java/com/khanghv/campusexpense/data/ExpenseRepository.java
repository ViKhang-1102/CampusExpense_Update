package com.khanghv.campusexpense.data;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.BudgetDao;
import com.khanghv.campusexpense.data.database.CategoryDao;
import com.khanghv.campusexpense.data.database.ExpenseDao;
import com.khanghv.campusexpense.data.database.UserDao;
import com.khanghv.campusexpense.data.model.Budget;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.data.model.User;
import com.khanghv.campusexpense.data.model.Expense;  // Adjust package nếu cần

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpenseRepository {
    private ExpenseDao expenseDao;
    private BudgetDao budgetDao;
    private UserDao userDao;
    private CategoryDao categoryDao;
    private com.khanghv.campusexpense.data.database.MonthlyBudgetDao monthlyBudgetDao;
    private AppDatabase db;

    public ExpenseRepository(Application application) {
        this.db = AppDatabase.getInstance(application);
        expenseDao = this.db.expenseDao();
        budgetDao = this.db.budgetDao();
        userDao = this.db.userDao();
        categoryDao = this.db.categoryDao();
        try {
            monthlyBudgetDao = this.db.monthlyBudgetDao();
        } catch (Exception e) {
            monthlyBudgetDao = null;
        }
    }

    // User methods (cho Greeting)
    public LiveData<User> getUserById(int userId) {
        return userDao.getUserById(userId);
    }

    // Expense methods (cho total/count tháng)
    public LiveData<Double> getTotalSpentForMonth(String monthYear, int userId) {
        long[] dateRange = getMonthDateRange(monthYear);
        return expenseDao.getTotalSpentForMonth(dateRange[0], dateRange[1], userId);
    }

    public LiveData<Double> getTotalSpentForMonth(String monthYear, int userId, int categoryId) {
        if (categoryId == -1) return getTotalSpentForMonth(monthYear, userId);
        long[] dateRange = getMonthDateRange(monthYear);
        return expenseDao.getTotalSpentForMonthByCategory(dateRange[0], dateRange[1], userId, categoryId);
    }

    public LiveData<Integer> getTransactionCountForMonth(String monthYear, int userId) {
        long[] dateRange = getMonthDateRange(monthYear);
        return expenseDao.getTransactionCountForMonth(dateRange[0], dateRange[1], userId);
    }

    public LiveData<Integer> getTransactionCountForMonth(String monthYear, int userId, int categoryId) {
        if (categoryId == -1) return getTransactionCountForMonth(monthYear, userId);
        long[] dateRange = getMonthDateRange(monthYear);
        return expenseDao.getTransactionCountForMonthByCategory(dateRange[0], dateRange[1], userId, categoryId);
    }

    private long[] getMonthDateRange(String monthYear) {
        String[] parts = monthYear.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1; // Calendar.MONTH là 0-based

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startDate = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endDate = cal.getTimeInMillis();

        return new long[]{startDate, endDate};
    }

    // Budget methods
    public LiveData<Double> getCurrentBudget(int userId) {
        LiveData<List<Budget>> budgetsLiveData = budgetDao.getAllBudgetsByUserLiveData(userId);
        return Transformations.map(budgetsLiveData, budgets -> {
            if (budgets == null || budgets.isEmpty()) {
                return 0.0;
            }
            double total = 0.0;
            for (Budget budget : budgets) {
                total += budget.getAmount();
            }
            return total;
        });
    }

    // Async insert expense
    public void insertExpense(Expense expense) {
        new Thread(() -> expenseDao.insert(expense)).start();
    }

    // Budget breakdown by category
    public static class BudgetBreakdownItem {
        public int categoryId;
        public String categoryName;
        public double budgetAmount;
        public double spentAmount;
        public int percentage;

        public BudgetBreakdownItem(int categoryId, String categoryName, double budgetAmount, double spentAmount) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.budgetAmount = budgetAmount;
            this.spentAmount = spentAmount;
            this.percentage = budgetAmount > 0 ? (int) ((spentAmount / budgetAmount) * 100) : 0;
        }
    }

    public LiveData<List<BudgetBreakdownItem>> getBudgetBreakdown(String monthYear, int userId) {
        long[] dateRange = getMonthDateRange(monthYear);
        if (monthlyBudgetDao == null) {
            MutableLiveData<List<BudgetBreakdownItem>> empty = new MutableLiveData<>();
            empty.setValue(new ArrayList<>());
            return empty;
        }
        LiveData<java.util.List<com.khanghv.campusexpense.data.model.MonthlyBudget>> budgetsLiveData = monthlyBudgetDao.getBudgetsByUserAndMonthLiveData(userId, Integer.parseInt(monthYear.split("-")[1]), Integer.parseInt(monthYear.split("-")[0]));

        return Transformations.switchMap(budgetsLiveData, budgets -> {
            MutableLiveData<List<BudgetBreakdownItem>> result = new MutableLiveData<>();
            
            new Thread(() -> {
                List<BudgetBreakdownItem> breakdownList = new ArrayList<>();
                if (budgets == null || budgets.isEmpty()) {
                    result.postValue(breakdownList);
                    return;
                }
                
                // Lấy tất cả categories một lần
                List<Category> allCategories = categoryDao.getAllByUser(userId);
                java.util.Map<Integer, String> categoryMap = new java.util.HashMap<>();
                for (Category cat : allCategories) {
                    categoryMap.put(cat.getId(), cat.getName());
                }

                // Tạo breakdown items from monthly budgets
                for (com.khanghv.campusexpense.data.model.MonthlyBudget mb : budgets) {
                    String categoryName = categoryMap.get(mb.getCategoryId());
                    if (categoryName == null) continue;

                    Double totalSpent = expenseDao.getTotalExpensesByCategoryAndDateRange(
                        userId, mb.getCategoryId(), dateRange[0], dateRange[1]);
                    double spent = totalSpent != null ? totalSpent : 0.0;

                    breakdownList.add(new BudgetBreakdownItem(
                        mb.getCategoryId(),
                        categoryName,
                        mb.getTotalBudget(),
                        spent
                    ));
                }
                
                result.postValue(breakdownList);
            }).start();
            
            return result;
        });
    }

    

    // New methods for monthly budgets
    public LiveData<java.util.List<com.khanghv.campusexpense.data.model.MonthlyBudget>> getMonthlyBudgetsForMonth(String monthYear, int userId) {
        if (monthlyBudgetDao == null) {
            MutableLiveData<java.util.List<com.khanghv.campusexpense.data.model.MonthlyBudget>> empty = new MutableLiveData<>();
            empty.setValue(new ArrayList<>());
            return empty;
        }
        String[] parts = monthYear.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        return monthlyBudgetDao.getBudgetsByUserAndMonthLiveData(userId, month, year);
    }

    public LiveData<Double> getTotalBudgetForMonth(String monthYear, int userId) {
        return getTotalBudgetForMonth(monthYear, userId, -1);
    }

    public LiveData<Double> getTotalBudgetForMonth(String monthYear, int userId, int categoryId) {
        MutableLiveData<Double> result = new MutableLiveData<>();
        if (monthlyBudgetDao == null) {
            result.setValue(0.0);
            return result;
        }
        String[] parts = monthYear.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        LiveData<java.util.List<com.khanghv.campusexpense.data.model.MonthlyBudget>> live = monthlyBudgetDao.getBudgetsByUserAndMonthLiveData(userId, month, year);
        return Transformations.map(live, mbs -> {
            if (mbs == null) return 0.0;
            double sum = 0;
            for (com.khanghv.campusexpense.data.model.MonthlyBudget mb : mbs) {
                if (categoryId == -1 || mb.getCategoryId() == categoryId) {
                    sum += mb.getTotalBudget();
                }
            }
            return sum;
        });
    }

    // Ensure budgets exist for given month: if missing, create from previous month by carrying over remaining
    public void ensureBudgetsForMonth(String monthYear, int userId) {
        new Thread(() -> {
            String[] parts = monthYear.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            int prevMonth = month - 1;
            int prevYear = year;
            if (prevMonth <= 0) { prevMonth = 12; prevYear = year - 1; }

            if (monthlyBudgetDao == null) return;
            com.khanghv.campusexpense.data.database.MonthlyBudgetDao mbDao = monthlyBudgetDao;
            java.util.List<Category> categories = categoryDao.getAllByUser(userId);

            long[] prevRange = getMonthDateRange(String.format(Locale.getDefault(), "%04d-%02d", prevYear, prevMonth));

            for (Category cat : categories) {
                com.khanghv.campusexpense.data.model.MonthlyBudget existing = mbDao.getBudgetByCategoryUserMonth(userId, cat.getId(), month, year);
                if (existing != null) continue;

                com.khanghv.campusexpense.data.model.MonthlyBudget prev = mbDao.getBudgetByCategoryUserMonth(userId, cat.getId(), prevMonth, prevYear);
                if (prev == null) continue;

                Double spentPrev = expenseDao.getTotalExpensesByCategoryAndDateRange(userId, cat.getId(), prevRange[0], prevRange[1]);
                double spent = spentPrev != null ? spentPrev : 0.0;
                double newTotal = prev.getTotalBudget() - spent;
                if (newTotal < 0) newTotal = 0.0;
                com.khanghv.campusexpense.data.model.MonthlyBudget newBud = new com.khanghv.campusexpense.data.model.MonthlyBudget(userId, cat.getId(), month, year, newTotal, newTotal);
                mbDao.insert(newBud);
            }
        }).start();
    }
}
