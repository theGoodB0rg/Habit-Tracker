package com.habittracker.analytics.domain.models

import java.time.LocalDate

/**
 * Legacy model - moved to AnalyticsData.kt for consolidated structure
 * This file is maintained for backward compatibility
 */
@Deprecated("Use StreakRetention from AnalyticsData.kt", ReplaceWith("com.habittracker.analytics.domain.models.StreakRetention"))
data class LegacyStreakRetention(
    val userId: String,
    val streakCount: Int,
    val lastStreakDate: Long,
    val date: LocalDate = LocalDate.now()
) {
    fun isStreakActive(currentDate: Long): Boolean {
        // Logic to determine if the streak is still active based on the last streak date
        val oneDayInMillis = 24 * 60 * 60 * 1000
        return (currentDate - lastStreakDate) <= oneDayInMillis
    }

    fun incrementStreak(): LegacyStreakRetention {
        // Logic to increment the streak count
        return copy(streakCount = streakCount + 1)
    }

    fun resetStreak(): LegacyStreakRetention {
        // Logic to reset the streak count
        return copy(streakCount = 0)
    }
}