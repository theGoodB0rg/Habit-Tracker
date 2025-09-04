package com.habittracker.analytics.utils

object AnalyticsConstants {
    const val HABIT_COMPLETION_RATE = "habit_completion_rate"
    const val SCREEN_VISITS = "screen_visits"
    const val STREAK_RETENTION = "streak_retention"

    const val ANALYTICS_DATABASE_NAME = "analytics_database"
    const val ANALYTICS_PREFERENCES_NAME = "analytics_preferences"

    const val EXPORT_FORMAT_JSON = "application/json"
    const val EXPORT_FORMAT_CSV = "text/csv"

    const val ANALYTICS_EVENT_TYPE_COMPLETION = "completion"
    const val ANALYTICS_EVENT_TYPE_SCREEN_VISIT = "screen_visit"
    const val ANALYTICS_EVENT_TYPE_STREAK_RETENTION = "streak_retention"

    const val ANALYTICS_EVENT_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
}