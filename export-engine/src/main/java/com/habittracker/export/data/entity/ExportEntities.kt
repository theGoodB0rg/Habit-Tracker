package com.habittracker.export.data.entity

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

/**
 * Copy of HabitEntity for export module
 * This avoids circular dependency with the main app module
 */
data class HabitEntity(
    val id: Long = 0,
    val name: String,
    val description: String,
    val iconId: Int,
    val frequency: HabitFrequency,
    val createdDate: Date,
    val streakCount: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: LocalDate? = null,
    val isActive: Boolean = true
)

/**
 * Copy of HabitCompletionEntity for export module
 */
data class HabitCompletionEntity(
    val id: Long = 0,
    val habitId: Long,
    val completedDate: LocalDate,
    val completedAt: LocalDateTime,
    val note: String? = null
)

/**
 * Copy of HabitFrequency enum for export module
 */
enum class HabitFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}

/**
 * Copy of TimerSessionEntity for export module
 */
data class TimerSessionEntity(
    val id: Long = 0,
    val habitId: Long,
    val timerType: String = "SIMPLE",
    val targetDurationMinutes: Int,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val startTime: LocalDateTime? = null,
    val pausedTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val actualDurationMinutes: Int = 0,
    val interruptions: Int = 0,
    val createdAt: LocalDateTime? = null
)

/**
 * Copy of PartialSessionEntity for export module
 */
data class PartialSessionEntity(
    val id: Long = 0,
    val habitId: Long,
    val durationMinutes: Int,
    val note: String? = null,
    val createdAt: LocalDateTime
)
