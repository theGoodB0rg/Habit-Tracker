package com.habittracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.Date

/**
 * Habit entity representing a habit stored in the Room database.
 * This is the core data model for habit tracking functionality.
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    
    val description: String,
    
    val iconId: Int,
    
    val frequency: HabitFrequency,
    
    val createdDate: Date,
    
    val streakCount: Int = 0,
    
    val longestStreak: Int = 0,
    
    val lastCompletedDate: LocalDate? = null,
    
    val isActive: Boolean = true,
    // Phase UIX-1 additions (timer foundations)
    // Whether per-habit timer functionality is enabled for this habit (feature gated in UI)
    // Default is true - users can disable per-habit if they prefer no timer
    val timerEnabled: Boolean = true,
    // Optional custom duration override in minutes (null = use global default)
    val customDurationMinutes: Int? = null,
    // Optional reference to an alert profile id
    val alertProfileId: String? = null
)

/**
 * Enum representing different habit frequencies
 */
enum class HabitFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}
