package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_goals")
data class MonthlyGoalEntity(
    @PrimaryKey
    val monthKey: String, // Format: "yyyy-MM" or "DEFAULT"
    val targetAmount: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
