package com.khanghv.campusexpense.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;

import com.khanghv.campusexpense.data.model.BudgetAdjustment;

import java.util.List;

@Dao
public interface BudgetAdjustmentDao {
    @Insert
    long insert(BudgetAdjustment adj);

    @Query("SELECT * FROM budget_adjustments WHERE monthlyBudgetId = :monthlyBudgetId ORDER BY createdAt DESC")
    List<BudgetAdjustment> getByMonthlyBudget(int monthlyBudgetId);

    @Query("DELETE FROM budget_adjustments WHERE monthlyBudgetId = :monthlyBudgetId")
    void deleteByMonthlyBudget(int monthlyBudgetId);

    @Update
    void update(BudgetAdjustment adj);

    @Query("DELETE FROM budget_adjustments WHERE id = :id")
    void deleteById(int id);
}
