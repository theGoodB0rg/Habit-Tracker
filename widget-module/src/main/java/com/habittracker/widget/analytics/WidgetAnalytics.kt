package com.habittracker.widget.analytics

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicLong

/**
 * Professional analytics system for widget usage tracking and performance monitoring.
 * 
 * Features:
 * - Widget interaction tracking
 * - Performance metrics collection
 * - User behavior analytics
 * - Error rate monitoring
 * - Resource usage tracking
 * 
 * Privacy:
 * - Local-only analytics (no network transmission)
 * - Anonymized data collection
 * - User control over data retention
 * - GDPR compliant data handling
 */
class WidgetAnalytics private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: WidgetAnalytics? = null
        
        fun getInstance(context: Context): WidgetAnalytics {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetAnalytics(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Analytics configuration
        private const val PREFS_NAME = "widget_analytics_prefs"
        private const val KEY_ANALYTICS_DATA = "analytics_data"
        private const val KEY_SESSION_DATA = "session_data"
        private const val MAX_EVENTS_PER_SESSION = 1000
        private const val DATA_RETENTION_DAYS = 30
    }
    
    // Data persistence
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val analyticsmutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // Real-time counters
    private val widgetInteractions = AtomicLong(0)
    private val updateOperations = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private val sessionStartTime = AtomicLong(System.currentTimeMillis())
    
    // Current session data
    private var currentSession = AnalyticsSession(
        sessionId = generateSessionId(),
        startTime = System.currentTimeMillis(),
        events = mutableListOf()
    )
    
    /**
     * Track widget interaction events
     */
    suspend fun trackInteraction(
        interactionType: InteractionType,
        targetElement: String,
        metadata: Map<String, Any> = emptyMap()
    ) = analyticsmutex.withLock {
        try {
            val event = AnalyticsEvent(
                type = EventType.INTERACTION,
                timestamp = System.currentTimeMillis(),
                data = mapOf(
                    "interaction_type" to interactionType.name,
                    "target_element" to targetElement,
                    "metadata" to convertMapToString(metadata).toString()
                )
            )
            
            addEventToSession(event)
            widgetInteractions.incrementAndGet()
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetAnalytics", "Failed to track interaction", e)
        }
    }
    
    /**
     * Track performance metrics with clean syntax
     */
    suspend fun trackPerformance(
        operation: String,
        executionTime: Long,
        success: Boolean,
        metadata: Map<String, Any> = emptyMap()
    ) = analyticsmutex.withLock {
        try {
            val event = AnalyticsEvent(
                type = EventType.PERFORMANCE,
                timestamp = System.currentTimeMillis(),
                data = mapOf(
                    "operation" to operation,
                    "execution_time_ms" to executionTime.toString(),
                    "success" to success.toString(),
                    "metadata" to convertMapToString(metadata).toString()
                )
            )
            
            addEventToSession(event)
            updateOperations.incrementAndGet()
            
            // Track failed operations
            when (success) {
                false -> errorCount.incrementAndGet()
                true -> { /* No error to track */ }
            }
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetAnalytics", "Failed to track performance", e)
        }
    }
    
    /**
     * Track user behavior patterns
     */
    suspend fun trackBehavior(
        behaviorType: BehaviorType,
        context: String,
        value: Any? = null
    ) = analyticsmutex.withLock {
        try {
            val event = AnalyticsEvent(
                type = EventType.BEHAVIOR,
                timestamp = System.currentTimeMillis(),
                data = mapOf(
                    "behavior_type" to behaviorType.name,
                    "context" to context,
                    "value" to (value?.toString() ?: "null"),
                    "session_duration" to getSessionDuration().toString()
                )
            )
            
            addEventToSession(event)
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetAnalytics", "Failed to track behavior", e)
        }
    }
    
    /**
     * Track error occurrences with context
     */
    suspend fun trackError(
        errorType: String,
        errorMessage: String,
        severity: ErrorSeverity,
        context: Map<String, Any> = emptyMap()
    ) = analyticsmutex.withLock {
        try {
            val event = AnalyticsEvent(
                type = EventType.ERROR,
                timestamp = System.currentTimeMillis(),
                data = mapOf(
                    "error_type" to errorType,
                    "error_message" to errorMessage,
                    "severity" to severity.name,
                    "context" to convertMapToString(context).toString()
                )
            )
            
            addEventToSession(event)
            errorCount.incrementAndGet()
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetAnalytics", "Failed to track error", e)
        }
    }
    
    /**
     * Track resource usage metrics
     */
    suspend fun trackResourceUsage(
        resourceType: ResourceType,
        usage: ResourceUsage
    ) = analyticsmutex.withLock {
        try {
            val event = AnalyticsEvent(
                type = EventType.RESOURCE,
                timestamp = System.currentTimeMillis(),
                data = mapOf(
                    "resource_type" to resourceType.name,
                    "memory_mb" to usage.memoryUsageMB.toString(),
                    "cpu_percent" to usage.cpuUsagePercent.toString(),
                    "battery_level" to usage.batteryLevel.toString(),
                    "network_active" to usage.isNetworkActive.toString()
                )
            )
            
            addEventToSession(event)
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetAnalytics", "Failed to track resource usage", e)
        }
    }
    
    /**
     * Add event to current session with overflow protection
     */
    private fun addEventToSession(event: AnalyticsEvent) {
        if (currentSession.events.size >= MAX_EVENTS_PER_SESSION) {
            // Archive current session and start new one
            archiveSession(currentSession)
            startNewSession()
        }
        
        currentSession.events.add(event)
    }
    
    /**
     * Start a new analytics session
     */
    private fun startNewSession() {
        currentSession = AnalyticsSession(
            sessionId = generateSessionId(),
            startTime = System.currentTimeMillis(),
            events = mutableListOf()
        )
        sessionStartTime.set(System.currentTimeMillis())
    }
    
    /**
     * Archive completed session to persistent storage
     */
    private fun archiveSession(session: AnalyticsSession) {
        try {
            val completedSession = session.copy(
                endTime = System.currentTimeMillis(),
                summary = generateSessionSummary(session)
            )
            
            val sessionJson = json.encodeToString(completedSession)
            val sessionKey = "session_${session.sessionId}"
            
            prefs.edit()
                .putString(sessionKey, sessionJson)
                .apply()
                
        } catch (e: Exception) {
            android.util.Log.w("WidgetAnalytics", "Failed to archive session", e)
        }
    }
    
    /**
     * Generate session summary statistics
     */
    private fun generateSessionSummary(session: AnalyticsSession): SessionSummary {
        val interactions = session.events.count { it.type == EventType.INTERACTION }
        val errors = session.events.count { it.type == EventType.ERROR }
        val performanceEvents = session.events.filter { it.type == EventType.PERFORMANCE }
        
        val averageResponseTime = if (performanceEvents.isNotEmpty()) {
            performanceEvents.mapNotNull { 
                (it.data["execution_time_ms"] as? Number)?.toLong() 
            }.average()
        } else {
            0.0
        }
        
        return SessionSummary(
            totalEvents = session.events.size,
            interactions = interactions,
            errors = errors,
            averageResponseTime = averageResponseTime,
            sessionDuration = System.currentTimeMillis() - session.startTime
        )
    }
    
    /**
     * Get comprehensive analytics report
     */
    suspend fun getAnalyticsReport(timeRange: TimeRange = TimeRange.LAST_7_DAYS): AnalyticsReport = analyticsmutex.withLock {
        try {
            val sessions = loadSessionsInRange(timeRange)
            val currentSessionSummary = generateSessionSummary(currentSession)
            
            AnalyticsReport(
                timeRange = timeRange,
                totalSessions = sessions.size + 1, // Include current session
                totalInteractions = sessions.sumOf { (it.summary?.interactions ?: 0L).toLong() } + currentSessionSummary.interactions,
                totalErrors = sessions.sumOf { (it.summary?.errors ?: 0L).toLong() } + currentSessionSummary.errors,
                averageSessionDuration = calculateAverageSessionDuration(sessions),
                errorRate = calculateErrorRate(sessions),
                performanceMetrics = calculatePerformanceMetrics(sessions),
                topInteractions = getTopInteractions(sessions),
                usagePatterns = analyzeUsagePatterns(sessions),
                systemHealth = assessSystemHealth()
            )
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetAnalytics", "Failed to generate analytics report", e)
            AnalyticsReport.empty(timeRange)
        }
    }
    
    /**
     * Load sessions within specified time range
     */
    private fun loadSessionsInRange(timeRange: TimeRange): List<AnalyticsSession> {
        val sessions = mutableListOf<AnalyticsSession>()
        val cutoffTime = System.currentTimeMillis() - timeRange.milliseconds
        
        val allKeys = prefs.all.keys.filter { it.startsWith("session_") }
        
        for (key in allKeys) {
            try {
                val sessionJson = prefs.getString(key, null) ?: continue
                val session = json.decodeFromString<AnalyticsSession>(sessionJson)
                
                if (session.startTime >= cutoffTime) {
                    sessions.add(session)
                }
            } catch (e: Exception) {
                android.util.Log.w("WidgetAnalytics", "Failed to load session: $key", e)
            }
        }
        
        return sessions.sortedBy { it.startTime }
    }
    
    /**
     * Calculate various performance metrics
     */
    private fun calculatePerformanceMetrics(sessions: List<AnalyticsSession>): PerformanceMetrics {
        val allPerformanceEvents = sessions.flatMap { session ->
            session.events.filter { it.type == EventType.PERFORMANCE }
        }
        
        val executionTimes = allPerformanceEvents.mapNotNull { 
            (it.data["execution_time_ms"] as? Number)?.toLong() 
        }
        
        val successfulOperations = allPerformanceEvents.count { 
            it.data["success"] as? Boolean == true 
        }
        
        return PerformanceMetrics(
            averageExecutionTime = executionTimes.average().takeIf { !it.isNaN() } ?: 0.0,
            medianExecutionTime = executionTimes.sorted().let { sorted ->
                if (sorted.isEmpty()) 0.0
                else if (sorted.size % 2 == 0) {
                    (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
                } else {
                    sorted[sorted.size / 2].toDouble()
                }
            },
            successRate = if (allPerformanceEvents.isNotEmpty()) {
                successfulOperations.toDouble() / allPerformanceEvents.size * 100.0
            } else {
                100.0
            },
            totalOperations = allPerformanceEvents.size
        )
    }
    
    /**
     * Analyze usage patterns from session data
     */
    private fun analyzeUsagePatterns(sessions: List<AnalyticsSession>): UsagePatterns {
        val allInteractions = sessions.flatMap { session ->
            session.events.filter { it.type == EventType.INTERACTION }
        }
        
        val hourlyUsage = allInteractions.groupingBy { event ->
            java.util.Calendar.getInstance().apply {
                timeInMillis = event.timestamp
            }.get(java.util.Calendar.HOUR_OF_DAY)
        }.eachCount()
        
        val peakHour = hourlyUsage.maxByOrNull { it.value }?.key ?: 12
        
        return UsagePatterns(
            peakUsageHour = peakHour,
            hourlyDistribution = hourlyUsage,
            averageSessionDuration = calculateAverageSessionDuration(sessions),
            mostActiveDay = getMostActiveDay(sessions)
        )
    }
    
    /**
     * Get top interaction types
     */
    private fun getTopInteractions(sessions: List<AnalyticsSession>): List<InteractionCount> {
        return sessions.flatMap { session ->
            session.events.filter { it.type == EventType.INTERACTION }
        }.groupingBy { event ->
            event.data["interaction_type"] as? String ?: "unknown"
        }.eachCount()
        .toList()
        .sortedByDescending { it.second }
        .take(10)
        .map { InteractionCount(it.first, it.second) }
    }
    
    /**
     * Assess overall system health
     */
    private fun assessSystemHealth(): SystemHealth {
        val currentErrorRate = if (updateOperations.get() > 0) {
            errorCount.get().toDouble() / updateOperations.get().toDouble() * 100.0
        } else {
            0.0
        }
        
        val healthScore = when {
            currentErrorRate < 1.0 -> HealthScore.EXCELLENT
            currentErrorRate < 5.0 -> HealthScore.GOOD
            currentErrorRate < 10.0 -> HealthScore.FAIR
            else -> HealthScore.POOR
        }
        
        return SystemHealth(
            healthScore = healthScore,
            errorRate = currentErrorRate,
            uptime = getSessionDuration(),
            lastError = getLastErrorTime(),
            totalInteractions = widgetInteractions.get()
        )
    }
    
    /**
     * Utility functions
     */
    private fun generateSessionId(): String = "session_${System.currentTimeMillis()}_${(0..999).random()}"
    
    private fun getSessionDuration(): Long = System.currentTimeMillis() - sessionStartTime.get()
    
    private fun calculateAverageSessionDuration(sessions: List<AnalyticsSession>): Double {
        val durations = sessions.mapNotNull { session ->
            session.endTime?.let { end -> end - session.startTime }
        }
        return durations.average().takeIf { !it.isNaN() } ?: 0.0
    }
    
    private fun calculateErrorRate(sessions: List<AnalyticsSession>): Double {
        val totalEvents = sessions.sumOf { it.events.size }
        val totalErrors = sessions.sumOf { session ->
            session.events.count { it.type == EventType.ERROR }
        }
        
        return if (totalEvents > 0) {
            totalErrors.toDouble() / totalEvents.toDouble() * 100.0
        } else {
            0.0
        }
    }
    
    private fun getMostActiveDay(sessions: List<AnalyticsSession>): Int {
        val dailyUsage = sessions.groupingBy { session ->
            java.util.Calendar.getInstance().apply {
                timeInMillis = session.startTime
            }.get(java.util.Calendar.DAY_OF_WEEK)
        }.eachCount()
        
        return dailyUsage.maxByOrNull { it.value }?.key ?: java.util.Calendar.MONDAY
    }
    
    private fun getLastErrorTime(): Long {
        return currentSession.events
            .filter { it.type == EventType.ERROR }
            .maxOfOrNull { it.timestamp } ?: 0L
    }
    
    /**
     * Clear analytics data (for privacy compliance)
     */
    suspend fun clearAnalyticsData() = analyticsmutex.withLock {
        try {
            val allKeys = prefs.all.keys.filter { it.startsWith("session_") }
            val editor = prefs.edit()
            
            for (key in allKeys) {
                editor.remove(key)
            }
            
            editor.apply()
            
            // Reset counters
            widgetInteractions.set(0)
            updateOperations.set(0)
            errorCount.set(0)
            
            // Start fresh session
            startNewSession()
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetAnalytics", "Failed to clear analytics data", e)
        }
    }
    
    /**
     * Export analytics data for user review
     */
    suspend fun exportAnalyticsData(): String? = analyticsmutex.withLock {
        try {
            val report = getAnalyticsReport(TimeRange.ALL_TIME)
            json.encodeToString(report)
        } catch (e: Exception) {
            android.util.Log.e("WidgetAnalytics", "Failed to export analytics data", e)
            null
        }
    }
    
    /**
     * Data classes for analytics
     */
    
    @Serializable
    data class AnalyticsSession(
        val sessionId: String,
        val startTime: Long,
        val endTime: Long? = null,
        val events: MutableList<AnalyticsEvent> = mutableListOf(),
        val summary: SessionSummary? = null
    )
    
    @Serializable
    data class AnalyticsEvent(
        val type: EventType,
        val timestamp: Long,
        val data: Map<String, String>
    )
    
    @Serializable
    data class SessionSummary(
        val totalEvents: Int,
        val interactions: Int,
        val errors: Int,
        val averageResponseTime: Double,
        val sessionDuration: Long
    )
    
    @Serializable
    data class AnalyticsReport(
        val timeRange: TimeRange,
        val totalSessions: Int,
        val totalInteractions: Long,
        val totalErrors: Long,
        val averageSessionDuration: Double,
        val errorRate: Double,
        val performanceMetrics: PerformanceMetrics,
        val topInteractions: List<InteractionCount>,
        val usagePatterns: UsagePatterns,
        val systemHealth: SystemHealth
    ) {
        companion object {
            fun empty(timeRange: TimeRange) = AnalyticsReport(
                timeRange = timeRange,
                totalSessions = 0,
                totalInteractions = 0,
                totalErrors = 0,
                averageSessionDuration = 0.0,
                errorRate = 0.0,
                performanceMetrics = PerformanceMetrics(),
                topInteractions = emptyList(),
                usagePatterns = UsagePatterns(),
                systemHealth = SystemHealth()
            )
        }
    }
    
    @Serializable
    data class PerformanceMetrics(
        val averageExecutionTime: Double = 0.0,
        val medianExecutionTime: Double = 0.0,
        val successRate: Double = 100.0,
        val totalOperations: Int = 0
    )
    
    @Serializable
    data class UsagePatterns(
        val peakUsageHour: Int = 12,
        val hourlyDistribution: Map<Int, Int> = emptyMap(),
        val averageSessionDuration: Double = 0.0,
        val mostActiveDay: Int = java.util.Calendar.MONDAY
    )
    
    @Serializable
    data class SystemHealth(
        val healthScore: HealthScore = HealthScore.EXCELLENT,
        val errorRate: Double = 0.0,
        val uptime: Long = 0L,
        val lastError: Long = 0L,
        val totalInteractions: Long = 0L
    )
    
    @Serializable
    data class InteractionCount(
        val type: String,
        val count: Int
    )
    
    @Serializable
    data class ResourceUsage(
        val memoryUsageMB: Long,
        val cpuUsagePercent: Float,
        val batteryLevel: Int,
        val isNetworkActive: Boolean
    )
    
    /**
     * Enums for analytics categorization
     */
    
    @Serializable
    enum class EventType {
        INTERACTION,
        PERFORMANCE,
        BEHAVIOR,
        ERROR,
        RESOURCE
    }
    
    @Serializable
    enum class InteractionType {
        HABIT_TOGGLE,
        REFRESH_CLICK,
        WIDGET_RESIZE,
        SETTINGS_ACCESS
    }
    
    @Serializable
    enum class BehaviorType {
        FREQUENT_USER,
        CASUAL_USER,
        POWER_USER,
        FIRST_TIME_USER,
        WIDGET_ENABLED,
        WIDGET_DISABLED,
        WIDGET_DELETED
    }
    
    @Serializable
    enum class ErrorSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    @Serializable
    enum class ResourceType {
        MEMORY,
        CPU,
        BATTERY,
        NETWORK
    }
    
    @Serializable
    enum class TimeRange(val milliseconds: Long) {
        LAST_HOUR(3600000L),
        LAST_24_HOURS(86400000L),
        LAST_7_DAYS(604800000L),
        LAST_30_DAYS(2592000000L),
        ALL_TIME(Long.MAX_VALUE)
    }
    
    @Serializable
    enum class HealthScore {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR
    }
    
    /**
     * Helper function to convert Map<String, Any> to Map<String, String>
     */
    private fun convertMapToString(map: Map<String, Any>): Map<String, String> {
        return map.mapValues { (_, value) ->
            when (value) {
                is String -> value
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> value.toString()
            }
        }
    }
}
