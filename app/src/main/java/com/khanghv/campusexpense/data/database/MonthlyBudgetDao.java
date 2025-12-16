package com.khanghv.campusexpense.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.khanghv.campusexpense.data.model.MonthlyBudget;

import java.util.List;

@Dao
public interface MonthlyBudgetDao {
    @Insert
    long insert(MonthlyBudget budget);

    @Update
    void update(MonthlyBudget budget);

    @Delete
    void delete(MonthlyBudget budget);

    @Query("SELECT * FROM monthly_budgets WHERE userId = :userId AND month = :month AND year = :year ORDER BY createdAt DESC")
    List<MonthlyBudget> getBudgetsByUserAndMonth(int userId, int month, int year);

    @Query("SELECT * FROM monthly_budgets WHERE userId = :userId AND month = :month AND year = :year ORDER BY createdAt DESC")
    LiveData<List<MonthlyBudget>> getBudgetsByUserAndMonthLiveData(int userId, int month, int year);

    @Query("SELECT * FROM monthly_budgets WHERE userId = :userId ORDER BY year DESC, month DESC")
    List<MonthlyBudget> getAllBudgetsByUser(int userId);

    @Query("SELECT * FROM monthly_budgets WHERE userId = :userId AND categoryId = :categoryId AND month = :month AND year = :year LIMIT 1")
    MonthlyBudget getBudgetByCategoryUserMonth(int userId, int categoryId, int month, int year);

    @Query("SELECT * FROM monthly_budgets WHERE id = :id LIMIT 1")
    MonthlyBudget getById(int id);

    @Query("DELETE FROM monthly_budgets WHERE categoryId = :categoryId")
    void deleteBudgetsByCategoryId(int categoryId);

    @Query("DELETE FROM monthly_budgets WHERE id = :id")
    void deleteById(int id);
}
