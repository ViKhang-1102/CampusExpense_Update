package com.khanghv.campusexpense.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.khanghv.campusexpense.data.model.Payment;

import java.util.List;

@Dao
public interface PaymentDao {
    @Insert
    long insert(Payment payment);

    @Update
    void update(Payment payment);

    @Delete
    void delete(Payment payment);

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY date ASC, timeMinutes ASC")
    List<Payment> getAllByUser(int userId);

    @Query("SELECT COUNT(*) FROM payments WHERE userId = :userId AND status = 'Pending'")
    int getPendingCount(int userId);

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    Payment getById(int id);

    @Query("SELECT COUNT(*) FROM payments WHERE userId = :userId AND status = 'Pending' AND (date + timeMinutes*60000) <= :now")
    int getOverdueCount(int userId, long now);
}
