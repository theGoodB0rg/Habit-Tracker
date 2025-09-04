package com.habittracker.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain model for habit completion tracking
 * Represents a single habit completion event
 */
data class HabitCompletion(
    val id: Long = 0,
    val habitId: Long,
    val completedDate: LocalDate,
    val completedAt: LocalDateTime = LocalDateTime.now(),
    val note: String? = null
)

/**
 * Domain model for habit streak information
 */
data class HabitStreak(
    val habitId: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastCompletedDate: LocalDate?,
    val isActiveToday: Boolean = false
)

/**
 * Domain model for habit statistics
 */
data class HabitStats(
    val habitId: Long,
    val totalCompletions: Int,
    val completionRate: Double, // Percentage (0.0 - 1.0)
    val averageStreakLength: Double,
    val longestStreak: Int,
    val currentStreak: Int,
    val completionsThisWeek: Int,
    val completionsThisMonth: Int
)
