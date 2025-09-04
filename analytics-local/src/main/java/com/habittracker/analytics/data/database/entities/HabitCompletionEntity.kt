package com.habittracker.analytics.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_completion")
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val isCompleted: Boolean,
    val timestamp: Long
) {
    // Additional logic can be added here if needed
}
