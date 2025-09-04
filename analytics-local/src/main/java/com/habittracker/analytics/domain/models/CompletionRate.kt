package com.habittracker.analytics.domain.models

import java.time.LocalDate

/**
 * Legacy model - moved to AnalyticsData.kt for consolidated structure
 * This file is maintained for backward compatibility
 */
@Deprecated("Use CompletionRate from AnalyticsData.kt", ReplaceWith("com.habittracker.analytics.domain.models.CompletionRate"))
data class LegacyCompletionRate(
    val habitId: String,
    val completionCount: Int,
    val totalCount: Int,
    val date: LocalDate = LocalDate.now()
) {
    val rate: Double
        get() = if (totalCount > 0) {
            (completionCount.toDouble() / totalCount) * 100
        } else {
            0.0
        }
}