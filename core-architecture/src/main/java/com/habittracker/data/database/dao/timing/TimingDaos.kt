package com.habittracker.data.database.dao.timing

import androidx.room.*
import com.habittracker.data.database.entity.timing.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for habit timing operations
 * Part of Phase 1 - Smart Timing Enhancement
 */
@Dao
interface HabitTimingDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM habit_timing WHERE habit_id = :habitId")
    suspend fun getTimingByHabitId(habitId: Long): HabitTimingEntity?
    
    @Query("SELECT * FROM habit_timing WHERE habit_id = :habitId")
    fun getTimingByHabitIdFlow(habitId: Long): Flow<HabitTimingEntity?>
    
    @Query("SELECT * FROM habit_timing")
    suspend fun getAllTimings(): List<HabitTimingEntity>
    
    @Query("SELECT * FROM habit_timing WHERE timer_enabled = 1")
    suspend fun getTimerEnabledHabits(): List<HabitTimingEntity>
    
    @Query("SELECT * FROM habit_timing WHERE is_scheduling_enabled = 1")
    suspend fun getSchedulingEnabledHabits(): List<HabitTimingEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiming(timing: HabitTimingEntity): Long
    
    @Update
    suspend fun updateTiming(timing: HabitTimingEntity)
    
    @Delete
    suspend fun deleteTiming(timing: HabitTimingEntity)
    
    @Query("DELETE FROM habit_timing WHERE habit_id = :habitId")
    suspend fun deleteTimingByHabitId(habitId: Long)
    
    // Convenience update methods
    @Query("UPDATE habit_timing SET timer_enabled = :enabled, updated_at = :timestamp WHERE habit_id = :habitId")
    suspend fun updateTimerEnabled(habitId: Long, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE habit_timing SET preferred_time = :preferredTime, is_scheduling_enabled = :enabled, updated_at = :timestamp WHERE habit_id = :habitId")
    suspend fun updatePreferredTime(habitId: Long, preferredTime: String?, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE habit_timing SET estimated_duration_minutes = :durationMinutes, updated_at = :timestamp WHERE habit_id = :habitId")
    suspend fun updateEstimatedDuration(habitId: Long, durationMinutes: Int?, timestamp: Long = System.currentTimeMillis())
}

/**
 * DAO for timer session operations
 */
@Dao
interface TimerSessionDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM timer_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): TimerSessionEntity?
    
    @Query("SELECT * FROM timer_sessions WHERE habit_id = :habitId ORDER BY created_at DESC")
    suspend fun getSessionsByHabitId(habitId: Long): List<TimerSessionEntity>
    
    @Query("SELECT * FROM timer_sessions WHERE habit_id = :habitId AND is_running = 1 LIMIT 1")
    suspend fun getActiveSessionByHabitId(habitId: Long): TimerSessionEntity?
    
    @Query("SELECT * FROM timer_sessions WHERE is_running = 1")
    suspend fun getAllActiveSessions(): List<TimerSessionEntity>
    
    @Query("SELECT * FROM timer_sessions WHERE is_running = 1")
    fun getAllActiveSessionsFlow(): Flow<List<TimerSessionEntity>>
    
    @Query("SELECT * FROM timer_sessions WHERE habit_id = :habitId AND end_time IS NOT NULL ORDER BY created_at DESC LIMIT :limit")
    suspend fun getCompletedSessions(habitId: Long, limit: Int = 10): List<TimerSessionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimerSessionEntity): Long
    
    @Update
    suspend fun updateSession(session: TimerSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: TimerSessionEntity)
    
    @Query("DELETE FROM timer_sessions WHERE habit_id = :habitId")
    suspend fun deleteSessionsByHabitId(habitId: Long)
    
    // State management methods
    @Query("UPDATE timer_sessions SET is_running = :isRunning, start_time = :startTime WHERE id = :sessionId")
    suspend fun updateSessionRunningState(sessionId: Long, isRunning: Boolean, startTime: Long?)
    
    @Query("UPDATE timer_sessions SET is_paused = :isPaused, paused_time = :pausedTime WHERE id = :sessionId")
    suspend fun updateSessionPausedState(sessionId: Long, isPaused: Boolean, pausedTime: Long?)

    @Query("UPDATE timer_sessions SET start_time = :newStartTime, is_paused = 0, paused_time = NULL WHERE id = :sessionId")
    suspend fun resumeSessionWithTimeShift(sessionId: Long, newStartTime: Long)
    
    @Query("UPDATE timer_sessions SET end_time = :endTime, actual_duration_minutes = :actualDurationMinutes, is_running = 0 WHERE id = :sessionId")
    suspend fun completeSession(sessionId: Long, endTime: Long, actualDurationMinutes: Int)
    
    @Query("UPDATE timer_sessions SET interruptions = interruptions + 1 WHERE id = :sessionId")
    suspend fun incrementInterruptions(sessionId: Long)
    
    // Analytics queries
    @Query("""
        SELECT AVG(actual_duration_minutes) 
        FROM timer_sessions 
        WHERE habit_id = :habitId AND end_time IS NOT NULL AND actual_duration_minutes > 0
    """)
    suspend fun getAverageSessionDuration(habitId: Long): Float?
    
    @Query("""
        SELECT COUNT(*) 
        FROM timer_sessions 
        WHERE habit_id = :habitId AND end_time IS NOT NULL
    """)
    suspend fun getCompletedSessionCount(habitId: Long): Int
}

/**
 * DAO for partial session operations
 */
@Dao
interface PartialSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(partial: com.habittracker.data.database.entity.timing.PartialSessionEntity): Long

    @Query("SELECT * FROM partial_sessions WHERE habit_id = :habitId ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentByHabit(habitId: Long, limit: Int = 20): List<com.habittracker.data.database.entity.timing.PartialSessionEntity>

    @Query("DELETE FROM partial_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/**
 * DAO for smart suggestion operations
 */
@Dao
interface SmartSuggestionDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM smart_suggestions WHERE id = :suggestionId")
    suspend fun getSuggestionById(suggestionId: Long): SmartSuggestionEntity?
    
    @Query("SELECT * FROM smart_suggestions WHERE habit_id = :habitId AND (valid_until IS NULL OR valid_until > :currentTime) ORDER BY confidence DESC")
    suspend fun getValidSuggestionsByHabitId(habitId: Long, currentTime: Long = System.currentTimeMillis()): List<SmartSuggestionEntity>
    
    @Query("SELECT * FROM smart_suggestions WHERE habit_id = :habitId ORDER BY created_at DESC")
    suspend fun getAllSuggestionsByHabitId(habitId: Long): List<SmartSuggestionEntity>
    
    @Query("""
        SELECT * FROM smart_suggestions 
        WHERE habit_id = :habitId 
        AND suggestion_type = :type 
        AND (valid_until IS NULL OR valid_until > :currentTime)
        ORDER BY confidence DESC
    """)
    suspend fun getSuggestionsByType(habitId: Long, type: String, currentTime: Long = System.currentTimeMillis()): List<SmartSuggestionEntity>
    
    @Query("SELECT * FROM smart_suggestions WHERE priority = :priority AND (valid_until IS NULL OR valid_until > :currentTime)")
    suspend fun getSuggestionsByPriority(priority: String, currentTime: Long = System.currentTimeMillis()): List<SmartSuggestionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: SmartSuggestionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestions(suggestions: List<SmartSuggestionEntity>): List<Long>
    
    @Update
    suspend fun updateSuggestion(suggestion: SmartSuggestionEntity)
    
    @Delete
    suspend fun deleteSuggestion(suggestion: SmartSuggestionEntity)
    
    @Query("DELETE FROM smart_suggestions WHERE habit_id = :habitId")
    suspend fun deleteSuggestionsByHabitId(habitId: Long)
    
    @Query("DELETE FROM smart_suggestions WHERE valid_until IS NOT NULL AND valid_until <= :currentTime")
    suspend fun deleteExpiredSuggestions(currentTime: Long = System.currentTimeMillis())
    
    // User interaction tracking
    @Query("UPDATE smart_suggestions SET accepted = :accepted WHERE id = :suggestionId")
    suspend fun recordSuggestionResponse(suggestionId: Long, accepted: Boolean)
    
    @Query("""
        SELECT 
            suggestion_type,
            AVG(CASE WHEN accepted = 1 THEN 1.0 ELSE 0.0 END) as acceptance_rate,
            COUNT(*) as total_suggestions
        FROM smart_suggestions 
        WHERE habit_id = :habitId AND accepted IS NOT NULL
        GROUP BY suggestion_type
    """)
    suspend fun getSuggestionAcceptanceRates(habitId: Long): List<SuggestionAcceptanceRate>
    
    // Cleanup operations
    @Query("DELETE FROM smart_suggestions WHERE created_at < :cutoffTime")
    suspend fun deleteOldSuggestions(cutoffTime: Long)
}

/**
 * DAO for completion metrics operations
 */
@Dao
interface CompletionMetricsDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM completion_metrics WHERE habit_id = :habitId ORDER BY completion_date DESC")
    suspend fun getMetricsByHabitId(habitId: Long): List<CompletionMetricsEntity>
    
    @Query("SELECT * FROM completion_metrics WHERE habit_id = :habitId AND completion_date >= :fromDate ORDER BY completion_date DESC")
    suspend fun getMetricsByHabitIdAndDateRange(habitId: Long, fromDate: Long): List<CompletionMetricsEntity>
    
    @Query("SELECT * FROM completion_metrics WHERE habit_id = :habitId ORDER BY completion_date DESC LIMIT :limit")
    suspend fun getRecentMetrics(habitId: Long, limit: Int = 30): List<CompletionMetricsEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: CompletionMetricsEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: List<CompletionMetricsEntity>): List<Long>
    
    @Update
    suspend fun updateMetrics(metrics: CompletionMetricsEntity)
    
    @Delete
    suspend fun deleteMetrics(metrics: CompletionMetricsEntity)
    
    @Query("DELETE FROM completion_metrics WHERE habit_id = :habitId")
    suspend fun deleteMetricsByHabitId(habitId: Long)
    
    // Analytics queries
    @Query("""
        SELECT completion_time, COUNT(*) as frequency
        FROM completion_metrics 
        WHERE habit_id = :habitId 
        GROUP BY completion_time 
        ORDER BY frequency DESC
    """)
    suspend fun getCompletionTimeFrequency(habitId: Long): List<TimeFrequency>
    
    @Query("""
        SELECT AVG(efficiency_score) 
        FROM completion_metrics 
        WHERE habit_id = :habitId AND efficiency_score IS NOT NULL
    """)
    suspend fun getAverageEfficiencyScore(habitId: Long): Float?
    
    @Query("""
        SELECT completion_time, AVG(efficiency_score) as avg_efficiency
        FROM completion_metrics 
        WHERE habit_id = :habitId AND efficiency_score IS NOT NULL
        GROUP BY completion_time
        ORDER BY avg_efficiency DESC
    """)
    suspend fun getOptimalTimesWithEfficiency(habitId: Long): List<TimeEfficiency>
}

/**
 * DAO for habit analytics operations
 */
@Dao
interface HabitAnalyticsDao {
    
    @Query("SELECT * FROM habit_analytics WHERE habit_id = :habitId")
    suspend fun getAnalyticsByHabitId(habitId: Long): HabitAnalyticsEntity?
    
    @Query("SELECT * FROM habit_analytics WHERE habit_id = :habitId")
    fun getAnalyticsByHabitIdFlow(habitId: Long): Flow<HabitAnalyticsEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: HabitAnalyticsEntity): Long
    
    @Update
    suspend fun updateAnalytics(analytics: HabitAnalyticsEntity)
    
    @Delete
    suspend fun deleteAnalytics(analytics: HabitAnalyticsEntity)
    
    @Query("DELETE FROM habit_analytics WHERE habit_id = :habitId")
    suspend fun deleteAnalyticsByHabitId(habitId: Long)
    
    @Query("UPDATE habit_analytics SET last_calculated = :timestamp, updated_at = :timestamp WHERE habit_id = :habitId")
    suspend fun updateLastCalculated(habitId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM habit_analytics WHERE last_calculated < :cutoffTime")
    suspend fun getStaleAnalytics(cutoffTime: Long): List<HabitAnalyticsEntity>
}

// Data classes for query results
data class SuggestionAcceptanceRate(
    val suggestion_type: String,
    val acceptance_rate: Float,
    val total_suggestions: Int
)

data class TimeFrequency(
    val completion_time: String,
    val frequency: Int
)

data class TimeEfficiency(
    val completion_time: String,
    val avg_efficiency: Float
)
