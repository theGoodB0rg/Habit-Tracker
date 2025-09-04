package com.habittracker.analytics.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_retention")
data class StreakRetentionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val streakCount: Int,
    val lastUpdated: Long // Timestamp for the last update
)
