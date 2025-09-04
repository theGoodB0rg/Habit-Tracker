package com.habittracker.analytics.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Enhanced database entities for comprehensive analytics tracking
 * Designed with proper indexing for performance and race condition protection
 */

@Entity(
    tableName = "habit_completion_analytics",
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index(value = ["habitId"]),
        Index(value = ["date"])
    ]
)
data class HabitCompletionAnalyticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: String,
    val habitName: String,
    val isCompleted: Boolean,
    val completionTimestamp: Long, // UTC timestamp
    val date: String, // ISO date string for easy querying
    val streakCount: Int,
    val difficultyLevel: String, // EASY, MODERATE, HARD, EXPERT
    val timeSpentMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "screen_visit_analytics",
    indices = [
        Index(value = ["screenName", "date"]),
        Index(value = ["sessionId"]),
        Index(value = ["date"])
    ]
)
data class ScreenVisitAnalyticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screenName: String,
    val sessionId: String, // Unique session identifier
    val entryTimestamp: Long,
    val exitTimestamp: Long? = null,
    val timeSpentMs: Long = 0,
    val date: String, // ISO date string
    val interactionCount: Int = 0, // Number of user interactions
    val bounced: Boolean = false, // Quick exit without interaction
    val fromScreen: String? = null, // Previous screen for navigation flow
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "streak_retention_analytics",
    indices = [
        Index(value = ["habitId", "startDate"], unique = true),
        Index(value = ["habitId"]),
        Index(value = ["isActive"])
    ]
)
data class StreakRetentionAnalyticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: String,
    val habitName: String,
    val streakLength: Int,
    val startDate: String, // ISO date string
    val endDate: String? = null, // null if streak is active
    val isActive: Boolean,
    val maxStreakLength: Int, // Historical maximum for this habit
    val difficultyLevel: String,
    val retentionProbability: Double = 0.0, // AI-calculated retention likelihood
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_engagement_analytics",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class UserEngagementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO date string
    val sessionCount: Int,
    val totalTimeSpentMs: Long,
    val averageSessionMs: Long,
    val screenTransitions: Int,
    val habitsInteracted: Int,
    val habitsCompleted: Int,
    val deepEngagement: Boolean, // More than 5 minutes of usage
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "app_session_analytics",
    indices = [
        Index(value = ["sessionId"], unique = true),
        Index(value = ["startDate"]),
        Index(value = ["endDate"])
    ]
)
data class AppSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val startDate: String, // ISO date string
    val endDate: String? = null,
    val durationMs: Long = 0,
    val screenVisits: Int = 0,
    val habitInteractions: Int = 0,
    val backgroundedCount: Int = 0, // How many times app went to background
    val crashed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "performance_metrics",
    indices = [
        Index(value = ["date"]),
        Index(value = ["metricType"])
    ]
)
data class PerformanceMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO date string
    val metricType: String, // LOAD_TIME, MEMORY_USAGE, BATTERY_IMPACT, etc.
    val value: Double,
    val unit: String, // ms, mb, percentage, etc.
    val context: String? = null, // Additional context about the metric
    val deviceInfo: String? = null, // Anonymous device characteristics
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Data transfer objects for complex analytics queries
 */
data class HabitAnalyticsSummary(
    val habitId: String,
    val habitName: String,
    val totalDays: Int,
    val completedDays: Int,
    val completionRate: Double,
    val currentStreak: Int,
    val longestStreak: Int,
    val averageTimeSpent: Double
)

data class ScreenAnalyticsSummary(
    val screenName: String,
    val totalVisits: Int,
    val totalTimeSpent: Long,
    val averageTimeSpent: Long,
    val bounceRate: Double,
    val engagementScore: Double
)

data class EngagementTrendData(
    val date: String,
    val sessionCount: Int,
    val totalTime: Long,
    val habitInteractions: Int,
    val engagementScore: Double
)
