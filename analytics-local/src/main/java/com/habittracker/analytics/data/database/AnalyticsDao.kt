package com.habittracker.analytics.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.habittracker.analytics.data.database.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * Comprehensive DAO for analytics data with proper race condition protection
 * and performance optimizations
 */
@Dao
interface AnalyticsDao {

    // ==================== HABIT COMPLETION ANALYTICS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitCompletion(habitCompletion: HabitCompletionEntity)
    
    @Update
    suspend fun updateHabitCompletion(habitCompletion: HabitCompletionEntity)

    @Query("""
        SELECT * FROM habit_completion_analytics 
        WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    fun getHabitCompletions(habitId: String, startDate: String, endDate: String): Flow<List<HabitCompletionAnalyticsEntity>>

    // Analytics operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitCompletionAnalytics(habitCompletion: HabitCompletionAnalyticsEntity)

    @Query("""
        SELECT 
            habitId,
            habitName,
            COUNT(*) as totalDays,
            SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completedDays,
            CAST(SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) AS REAL) * 100.0 / COUNT(*) as completionRate,
            MAX(streakCount) as currentStreak,
            MAX(streakCount) as longestStreak,
            AVG(timeSpentMinutes) as averageTimeSpent
        FROM habit_completion_analytics 
        WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate
        GROUP BY habitId
    """)
    suspend fun getHabitAnalyticsSummary(habitId: String, startDate: String, endDate: String): HabitAnalyticsSummary?

    @Query("""
        SELECT AVG(completion_rate) as averageCompletionRate
        FROM (
            SELECT 
                habitId,
                CAST(SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) AS REAL) * 100.0 / COUNT(*) as completion_rate
            FROM habit_completion_analytics 
            WHERE date BETWEEN :startDate AND :endDate
            GROUP BY habitId
        )
    """)
    suspend fun getOverallCompletionRate(startDate: String, endDate: String): Double?

    // ==================== SCREEN VISIT ANALYTICS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenVisit(screenVisit: ScreenVisitEntity)
    
    @Update
    suspend fun updateScreenVisit(screenVisit: ScreenVisitEntity)

    // Analytics operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenVisitAnalytics(screenVisit: ScreenVisitAnalyticsEntity)
    
    @Update
    suspend fun updateScreenVisitAnalytics(screenVisit: ScreenVisitAnalyticsEntity)

    @Query("UPDATE screen_visit_analytics SET exitTimestamp = :exitTime, timeSpentMs = :duration WHERE sessionId = :sessionId AND exitTimestamp IS NULL")
    suspend fun closeScreenVisit(sessionId: String, exitTime: Long, duration: Long)

    @Query("""
        SELECT 
            screenName,
            COUNT(*) as totalVisits,
            SUM(timeSpentMs) as totalTimeSpent,
            AVG(timeSpentMs) as averageTimeSpent,
            CAST(SUM(CASE WHEN bounced = 1 THEN 1 ELSE 0 END) AS REAL) * 100.0 / COUNT(*) as bounceRate,
            (AVG(timeSpentMs) / 1000.0 + AVG(interactionCount) * 10.0) as engagementScore
        FROM screen_visit_analytics 
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY screenName
        ORDER BY totalVisits DESC
    """)
    suspend fun getScreenAnalyticsSummary(startDate: String, endDate: String): List<ScreenAnalyticsSummary>

    @Query("SELECT COUNT(*) FROM screen_visit_analytics WHERE screenName = :screenName AND date BETWEEN :startDate AND :endDate")
    suspend fun getScreenVisitCount(screenName: String, startDate: String, endDate: String): Int

    // ==================== STREAK RETENTION ANALYTICS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreakRetention(streakRetention: StreakRetentionEntity)
    
    @Update
    suspend fun updateStreakRetention(streakRetention: StreakRetentionEntity)

    // Analytics operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreakRetentionAnalytics(streakRetention: StreakRetentionAnalyticsEntity)
    
    @Update
    suspend fun updateStreakRetentionAnalytics(streakRetention: StreakRetentionAnalyticsEntity)

    @Query("UPDATE streak_retention_analytics SET isActive = 0, endDate = :endDate WHERE habitId = :habitId AND isActive = 1")
    suspend fun endActiveStreak(habitId: String, endDate: String)

    @Query("SELECT * FROM streak_retention_analytics WHERE habitId = :habitId AND isActive = 1")
    suspend fun getActiveStreak(habitId: String): StreakRetentionAnalyticsEntity?

    @Query("SELECT MAX(streakLength) FROM streak_retention_analytics WHERE habitId = :habitId")
    suspend fun getLongestStreak(habitId: String): Int?

    @Query("SELECT AVG(streakLength) FROM streak_retention_analytics WHERE habitId = :habitId AND isActive = 0")
    suspend fun getAverageStreakLength(habitId: String): Double?

    // ==================== USER ENGAGEMENT ANALYTICS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserEngagement(engagement: UserEngagementEntity)
    
    @Update
    suspend fun updateUserEngagement(engagement: UserEngagementEntity)

    @Query("SELECT * FROM user_engagement_analytics WHERE date = :date")
    suspend fun getUserEngagementForDate(date: String): UserEngagementEntity?

    @Query("""
        SELECT 
            date,
            sessionCount,
            totalTimeSpentMs as totalTime,
            habitsInteracted as habitInteractions,
            (totalTimeSpentMs / 1000.0 / 60.0 + habitsCompleted * 20.0 + CASE WHEN deepEngagement = 1 THEN 50.0 ELSE 0.0 END) as engagementScore
        FROM user_engagement_analytics 
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    suspend fun getEngagementTrend(startDate: String, endDate: String): List<EngagementTrendData>

    // ==================== APP SESSION ANALYTICS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSession(session: AppSessionEntity)
    
    @Update
    suspend fun updateAppSession(session: AppSessionEntity)

    @Query("UPDATE app_session_analytics SET endTimestamp = :endTime, endDate = :endDate, durationMs = :duration WHERE sessionId = :sessionId")
    suspend fun endAppSession(sessionId: String, endTime: Long, endDate: String, duration: Long)

    @Query("SELECT * FROM app_session_analytics WHERE endTimestamp IS NULL ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getActiveSession(): AppSessionEntity?

    @Query("SELECT AVG(durationMs) FROM app_session_analytics WHERE startDate BETWEEN :startDate AND :endDate")
    suspend fun getAverageSessionDuration(startDate: String, endDate: String): Long?

    // ==================== PERFORMANCE METRICS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformanceMetric(metric: PerformanceMetricEntity)

    @Query("SELECT AVG(value) FROM performance_metrics WHERE metricType = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getAveragePerformanceMetric(type: String, startDate: String, endDate: String): Double?

    // ==================== CLEANUP AND MAINTENANCE ====================
    
    @Query("DELETE FROM habit_completion_analytics WHERE date < :cutoffDate")
    suspend fun cleanupOldHabitCompletions(cutoffDate: String)

    @Query("DELETE FROM screen_visit_analytics WHERE date < :cutoffDate")
    suspend fun cleanupOldScreenVisits(cutoffDate: String)

    @Query("DELETE FROM user_engagement_analytics WHERE date < :cutoffDate")
    suspend fun cleanupOldEngagementData(cutoffDate: String)

    @Query("DELETE FROM app_session_analytics WHERE startDate < :cutoffDate")
    suspend fun cleanupOldSessions(cutoffDate: String)

    @Query("DELETE FROM performance_metrics WHERE date < :cutoffDate")
    suspend fun cleanupOldPerformanceMetrics(cutoffDate: String)

    // ==================== COMPREHENSIVE ANALYTICS ====================
    
    @Transaction
    @Query("""
        SELECT 
            h.habitId,
            h.habitName,
            COUNT(h.id) as totalDays,
            SUM(CASE WHEN h.isCompleted = 1 THEN 1 ELSE 0 END) as completedDays,
            AVG(CASE WHEN h.isCompleted = 1 THEN 100.0 ELSE 0.0 END) as completionRate,
            COALESCE(s.streakLength, 0) as currentStreak,
            COALESCE(MAX(s.maxStreakLength), 0) as longestStreak,
            AVG(h.timeSpentMinutes) as averageTimeSpent
        FROM habit_completion_analytics h
        LEFT JOIN streak_retention_analytics s ON h.habitId = s.habitId AND s.isActive = 1
        WHERE h.date BETWEEN :startDate AND :endDate
        GROUP BY h.habitId
        ORDER BY completionRate DESC
    """)
    suspend fun getComprehensiveHabitAnalytics(startDate: String, endDate: String): List<HabitAnalyticsSummary>

    @Query("SELECT COUNT(DISTINCT date) FROM user_engagement_analytics WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getActiveDaysCount(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(DISTINCT habitId) FROM habit_completion_analytics WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getUniqueHabitsCount(startDate: String, endDate: String): Int

    // ==================== GENERIC ANALYTICS EVENTS ====================
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnalyticsEvent(event: AnalyticsEventEntity)
}