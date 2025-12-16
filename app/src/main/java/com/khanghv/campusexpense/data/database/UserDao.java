package com.khanghv.campusexpense.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.khanghv.campusexpense.data.model.User;

@Dao
public interface UserDao {
    @Insert
    long insertUser(User user);

    @Query("SELECT * FROM user_table WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);

    @Query("SELECT * FROM user_table WHERE id = :userId LIMIT 1")
    LiveData<User> getUserById(int userId);

    @Query("SELECT * FROM user_table WHERE username = :username AND password = :password LIMIT 1")
    User login(String username, String password);

    @Query("SELECT COUNT(*) FROM user_table WHERE username = :username")
    int checkUsernameExists(String username);
}

