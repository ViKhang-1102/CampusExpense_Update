package com.khanghv.campusexpense.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.khanghv.campusexpense.data.model.Expense;
import com.khanghv.campusexpense.data.model.Favorite;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Favorite favorite);

    @Query("DELETE FROM favorites WHERE userId = :userId AND expenseId = :expenseId")
    void deleteByUserAndExpense(int userId, int expenseId);

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND expenseId = :expenseId)")
    boolean isFavorite(int userId, int expenseId);

    @Query("SELECT e.* FROM expenses e JOIN favorites f ON e.id = f.expenseId WHERE f.userId = :userId ORDER BY e.date DESC, e.createdAt DESC")
    List<Expense> getFavoriteExpenses(int userId);

    @Query("SELECT e.* FROM expenses e JOIN favorites f ON e.id = f.expenseId WHERE f.userId = :userId ORDER BY e.date DESC, e.createdAt DESC")
    LiveData<List<Expense>> getFavoriteExpensesLive(int userId);
}

