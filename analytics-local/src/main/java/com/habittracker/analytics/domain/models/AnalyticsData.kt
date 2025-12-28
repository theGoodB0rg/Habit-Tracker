package com.habittracker.analytics.domain.models

import java.time.LocalDate

/**
 * Comprehensive analytics data structure containing all tracked metrics
 */
data class AnalyticsData(
    val habitCompletionRates: List<CompletionRate>,
    val screenVisits: List<ScreenVisit>,
    val streakRetentions: List<StreakRetention>,
    val userEngagement: UserEngagement,
    val timeRangeStats: TimeRangeStats,
    val exportMetadata: ExportMetadata
)

/**
 * Habit completion rate metrics with detailed breakdown
 */
data class CompletionRate(
    val habitId: String,
    val habitName: String,
    val totalDays: Int,
    val completedDays: Int,
    val completionPercentage: Double,
    val currentStreak: Int,
    val longestStreak: Int,
    val weeklyAverage: Double,
    val monthlyAverage: Double,
    val timeFrame: TimeFrame,
    val lastUpdated: LocalDate
) {
    val isSuccessful: Boolean
        get() = completionPercentage >= 70.0
        
    val needsAttention: Boolean
        get() = completionPercentage < 50.0
}

/**
 * Screen visit tracking with user engagement metrics
 */
data class ScreenVisit(
    val screenName: String,
    val visitCount: Int,
    val totalTimeSpent: Long, // in milliseconds
    val averageSessionTime: Long,
    val lastVisited: LocalDate,
    val engagementScore: Double, // calculated based on time and interactions
    val bounceRate: Double, // percentage of quick exits
    val timeFrame: TimeFrame
)

/**
 * Streak retention analysis for user motivation insights
 */
data class StreakRetention(
    val habitId: String,
    val habitName: String,
    val streakLength: Int,
    val longestStreak: Int,
    val streakStartDate: LocalDate,
    val streakEndDate: LocalDate?,
    val isActive: Boolean,
    val retentionProbability: Double, // AI-calculated likelihood of continuation
    val difficultyLevel: DifficultyLevel,
    val timeFrame: TimeFrame
)

/**
 * Overall user engagement metrics
 */
data class UserEngagement(
    val dailyActiveUsage: Boolean,
    val weeklyActiveUsage: Boolean,
    val monthlyActiveUsage: Boolean,
    val totalSessions: Int,
    val averageSessionLength: Long,
    val totalAppUsageTime: Long,
    val engagementTrend: EngagementTrend,
    val lastActiveDate: LocalDate
)

/**
 * Time-based statistics for trend analysis
 */
data class TimeRangeStats(
    val timeFrame: TimeFrame,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalHabits: Int,
    val activeHabits: Int,
    val completedSessions: Int,
    val missedSessions: Int,
    val averageStreakLength: Double,
    val improvementRate: Double // percentage improvement over time
)

/**
 * Export metadata for data portability
 */
data class ExportMetadata(
    val exportDate: LocalDate,
    val dataVersion: String,
    val totalRecords: Int,
    val anonymized: Boolean,
    val format: ExportFormat
)

enum class TimeFrame {
    DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, ALL_TIME
}

enum class DifficultyLevel {
    EASY, MODERATE, HARD, EXPERT
}

enum class EngagementTrend {
    INCREASING, STABLE, DECREASING, FLUCTUATING
}

enum class ExportFormat(val extension: String) {
    JSON("json"), CSV("csv"), PDF("pdf")
}