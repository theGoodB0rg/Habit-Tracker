package com.habittracker.nudges.model

import java.time.LocalDateTime

/**
 * Represents different types of nudges that can be shown to users
 */
enum class NudgeType {
    STREAK_BREAK_WARNING,
    MOTIVATIONAL_QUOTE,
    EASIER_GOAL_SUGGESTION,
    CELEBRATION,
    REMINDER,
    TIP_OF_THE_DAY
}

/**
 * Priority levels for nudges
 */
enum class NudgePriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Represents a nudge that can be shown to the user
 */
data class Nudge(
    val id: String,
    val type: NudgeType,
    val priority: NudgePriority,
    val title: String,
    val message: String,
    val actionText: String? = null,
    val habitId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val expiresAt: LocalDateTime? = null,
    val isDismissed: Boolean = false,
    val isActionTaken: Boolean = false
)

/**
 * Context information for generating nudges
 */
data class NudgeContext(
    val habitId: Long,
    val habitName: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val daysSinceLastCompletion: Int,
    val completionRate: Double,
    val consecutiveMisses: Int,
    val isActiveToday: Boolean,
    val difficulty: HabitDifficulty = HabitDifficulty.MEDIUM
)

/**
 * Represents the perceived difficulty of a habit
 */
enum class HabitDifficulty {
    VERY_EASY,
    EASY,
    MEDIUM,
    HARD,
    VERY_HARD
}

/**
 * Configuration for nudge generation
 */
data class NudgeConfig(
    val enableStreakWarnings: Boolean = true,
    val enableMotivationalQuotes: Boolean = true,
    val enableGoalSuggestions: Boolean = true,
    val enableCelebrations: Boolean = true,
    val maxNudgesPerDay: Int = 3,
    val streakWarningThreshold: Int = 3, // Show warning if streak >= 3 and about to break
    val failureThreshold: Int = 3, // Suggest easier goals after 3 consecutive misses
    val motivationalQuoteFrequency: Int = 2 // Show quotes every 2 days
)
