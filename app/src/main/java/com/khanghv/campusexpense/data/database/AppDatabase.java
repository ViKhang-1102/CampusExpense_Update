package com.khanghv.campusexpense.data.database;


import android.content.Context;

import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.khanghv.campusexpense.data.model.Budget;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.data.model.Expense;
import com.khanghv.campusexpense.data.model.User;
import com.khanghv.campusexpense.data.model.MonthlyBudget;


@Database(entities = {User.class, Category.class, Budget.class, Expense.class, MonthlyBudget.class}, version =5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
private static AppDatabase instance;
public static final String DATABASE_NAME = "app_database";


public abstract UserDao userDao ();
public abstract CategoryDao categoryDao();
public abstract BudgetDao budgetDao();
public abstract ExpenseDao expenseDao();
public abstract MonthlyBudgetDao monthlyBudgetDao();


public static synchronized AppDatabase getInstance(Context context){
    if (instance == null) {
        Migration MIGRATION_4_5 = new Migration(4, 5) {
            @Override
            public void migrate(SupportSQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS category_pairs (oldCategoryId INTEGER, userId INTEGER, name TEXT)");
                db.execSQL("INSERT INTO category_pairs(oldCategoryId,userId,name) SELECT c.id, b.userId, c.name FROM categories c JOIN budgets b ON b.categoryId = c.id");
                db.execSQL("INSERT INTO category_pairs(oldCategoryId,userId,name) SELECT c.id, e.userId, c.name FROM categories c JOIN expenses e ON e.categoryId = c.id");
                db.execSQL("CREATE TABLE IF NOT EXISTS categories_user (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, userId INTEGER NOT NULL)");
                db.execSQL("INSERT INTO categories_user(name,userId) SELECT DISTINCT name,userId FROM category_pairs");
                db.execSQL("CREATE TABLE IF NOT EXISTS cat_map (oldCategoryId INTEGER, userId INTEGER, newCategoryId INTEGER)");
                db.execSQL("INSERT INTO cat_map(oldCategoryId,userId,newCategoryId) SELECT DISTINCT p.oldCategoryId, p.userId, cu.id FROM category_pairs p JOIN categories_user cu ON cu.name = p.name AND cu.userId = p.userId");
                db.execSQL("UPDATE budgets SET categoryId = (SELECT newCategoryId FROM cat_map WHERE oldCategoryId = budgets.categoryId AND userId = budgets.userId) WHERE EXISTS (SELECT 1 FROM cat_map WHERE oldCategoryId = budgets.categoryId AND userId = budgets.userId)");
                db.execSQL("UPDATE expenses SET categoryId = (SELECT newCategoryId FROM cat_map WHERE oldCategoryId = expenses.categoryId AND userId = expenses.userId) WHERE EXISTS (SELECT 1 FROM cat_map WHERE oldCategoryId = expenses.categoryId AND userId = expenses.userId)");
                db.execSQL("DROP TABLE categories");
                db.execSQL("ALTER TABLE categories_user RENAME TO categories");
                db.execSQL("DROP TABLE category_pairs");
                db.execSQL("DROP TABLE cat_map");
                // Add monthly_budgets table to support per-month budgets and add budgetId column to expenses
                db.execSQL("CREATE TABLE IF NOT EXISTS monthly_budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, categoryId INTEGER NOT NULL, month INTEGER NOT NULL, year INTEGER NOT NULL, totalBudget REAL NOT NULL, remainingBudget REAL NOT NULL, createdAt INTEGER NOT NULL)");
                // Add budgetId column to expenses for linking transactions to monthly budgets
                try {
                    db.execSQL("ALTER TABLE expenses ADD COLUMN budgetId INTEGER");
                } catch (Exception ignored) {}
                // Migrate existing budgets -> monthly_budgets using createdAt month/year; remainingBudget set = amount
                db.execSQL("INSERT INTO monthly_budgets(userId, categoryId, month, year, totalBudget, remainingBudget, createdAt) SELECT userId, categoryId, CAST(strftime('%m', datetime(createdAt/1000, 'unixepoch')) AS INTEGER), CAST(strftime('%Y', datetime(createdAt/1000, 'unixepoch')) AS INTEGER), amount, amount, createdAt FROM budgets");
            }
        };
        instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_4_5)
                .build();
    }
    return instance;
}
}


