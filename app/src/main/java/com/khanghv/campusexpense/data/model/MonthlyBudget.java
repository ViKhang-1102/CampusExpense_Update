package com.khanghv.campusexpense.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "monthly_budgets")
public class MonthlyBudget {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;
    private int categoryId;
    private int month; // 1-12
    private int year;
    private double totalBudget;
    private double remainingBudget;
    private long createdAt;

    public MonthlyBudget() {}

    public MonthlyBudget(int userId, int categoryId, int month, int year, double totalBudget, double remainingBudget) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.month = month;
        this.year = year;
        this.totalBudget = totalBudget;
        this.remainingBudget = remainingBudget;
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }
    public double getRemainingBudget() { return remainingBudget; }
    public void setRemainingBudget(double remainingBudget) { this.remainingBudget = remainingBudget; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
