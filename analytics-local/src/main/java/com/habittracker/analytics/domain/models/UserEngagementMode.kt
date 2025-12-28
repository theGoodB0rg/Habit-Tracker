package com.habittracker.analytics.domain.models

enum class UserEngagementMode(val title: String, val description: String) {
    SPEED_RUNNER("Speed Runner", "Quick check-ins, high efficiency."),
    PLANNER("Planner", "Deep dives and thoughtful review."),
    LOST("Lost", "Spending time but not taking action."),
    BALANCED("Balanced", "Good mix of action and reflection."),
    UNKNOWN("Unknown", "Not enough data yet.")
}
