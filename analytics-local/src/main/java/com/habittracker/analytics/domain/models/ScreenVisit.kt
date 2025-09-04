package com.habittracker.analytics.domain.models

import java.time.LocalDateTime

/**
 * Legacy model - moved to AnalyticsData.kt for consolidated structure
 * This file is maintained for backward compatibility
 */
@Deprecated("Use ScreenVisit from AnalyticsData.kt", ReplaceWith("com.habittracker.analytics.domain.models.ScreenVisit"))
data class LegacyScreenVisit(
    val screenName: String,
    val visitTimestamp: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(screenName.isNotBlank()) { "Screen name cannot be blank" }
    }
}