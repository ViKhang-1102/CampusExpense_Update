package com.khanghv.campusexpense.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budget_adjustments")
public class BudgetAdjustment {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;
    private int monthlyBudgetId;
    private int categoryId;
    private int month;
    private int year;
    private double amount;
    private String note;
    private long createdAt;

    public BudgetAdjustment() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getMonthlyBudgetId() { return monthlyBudgetId; }
    public void setMonthlyBudgetId(int monthlyBudgetId) { this.monthlyBudgetId = monthlyBudgetId; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
