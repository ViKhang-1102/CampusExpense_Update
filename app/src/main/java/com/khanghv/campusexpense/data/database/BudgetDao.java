package com.khanghv.campusexpense.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.khanghv.campusexpense.data.model.Budget;
import java.util.List;

@Dao
public interface BudgetDao {

    @Insert
    long insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY createdAt DESC")
    List<Budget> getAllBudgetsByUser(int userId);

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<Budget>> getAllBudgetsByUserLiveData(int userId);

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId ORDER BY createdAt DESC")
    Budget getBudgetByCategoryAndUser(int userId, int categoryId);

    @Query("SELECT * FROM budgets WHERE id = :id")
    Budget getBudgetById(int id);

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    void deleteBudgetsByCategoryId(int categoryId);

}
