package com.khanghv.campusexpense.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "payments")
public class Payment {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;
    private int categoryId;
    private String name;
    private double amount;
    private String note;
    private long date; // date-only millis (at 00:00)
    private int timeMinutes; // minutes of day (0..1439)
    private String status; // "Pending" or "Paid"
    private Integer linkedExpenseId; // nullable
    private long createdAt;

    public Payment() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
    public int getTimeMinutes() { return timeMinutes; }
    public void setTimeMinutes(int timeMinutes) { this.timeMinutes = timeMinutes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getLinkedExpenseId() { return linkedExpenseId; }
    public void setLinkedExpenseId(Integer linkedExpenseId) { this.linkedExpenseId = linkedExpenseId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
