package com.habittracker.analytics.data.repository

import com.habittracker.analytics.data.database.AnalyticsDao
import com.habittracker.analytics.data.database.entities.*
import com.habittracker.analytics.domain.models.*
import com.habittracker.analytics.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced analytics repository interface with comprehensive tracking capabilities
 */
interface AnalyticsRepository {
    suspend fun trackHabitCompletion(
        habitId: String,
        habitName: String,
        isCompleted: Boolean,
        timeSpentMinutes: Int = 0,
        difficultyLevel: DifficultyLevel = DifficultyLevel.MODERATE
    )
    
    suspend fun trackScreenVisit(screenName: String, fromScreen: String? = null)
    suspend fun endScreenVisit(interactionCount: Int = 0, bounced: Boolean = false)
    
    suspend fun startAppSession()
    suspend fun endAppSession()
    
    suspend fun getHabitCompletionRate(habitId: String, timeFrame: TimeFrame): Flow<CompletionRate?>
    suspend fun getScreenVisitData(timeFrame: TimeFrame): Flow<List<ScreenVisit>>
    suspend fun getStreakRetentionData(timeFrame: TimeFrame): Flow<List<StreakRetention>>
    suspend fun getComprehensiveAnalytics(timeFrame: TimeFrame): Flow<AnalyticsData>
    
    suspend fun getUserEngagementMode(timeFrame: TimeFrame): Flow<UserEngagementMode>
    
    suspend fun exportAnalyticsData(format: ExportFormat): String
    suspend fun cleanupOldData(retentionDays: Int = 365)
    suspend fun clearAllData()

    // Generic timer event tracking
    suspend fun trackTimerEvent(
        eventType: String,
        habitId: Long? = null,
        sessionId: Long? = null,
        source: String? = null,
        extra: Map<String, Any?> = emptyMap()
    )
}

/**
 * Comprehensive analytics repository implementation with race condition protection
 */
@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val dateUtils: DateUtils
) : AnalyticsRepository {
    
    // Mutexes for thread-safe operations
    private val habitCompletionMutex = Mutex()
    private val screenVisitMutex = Mutex()
    private val streakRetentionMutex = Mutex()
    private val sessionMutex = Mutex()
    
    // Active session tracking
    private var currentSessionId: String? = null
    private var currentScreenVisitId: String? = null
    private var lastScreenStartTime: Long? = null
    private var sessionStartTime: Long? = null
    private val timerEventMutex = Mutex()
    
    override suspend fun trackHabitCompletion(
        habitId: String,
        habitName: String,
        isCompleted: Boolean,
        timeSpentMinutes: Int,
        difficultyLevel: DifficultyLevel
    ) = habitCompletionMutex.withLock {
        val currentTime = System.currentTimeMillis()
        val today = dateUtils.getCurrentDateString()
        
        // Calculate current streak
        val currentStreak = if (isCompleted) {
            calculateCurrentStreak(habitId) + 1
        } else {
            0
        }
        
        val completion = HabitCompletionAnalyticsEntity(
            habitId = habitId,
            habitName = habitName,
            isCompleted = isCompleted,
            completionTimestamp = currentTime,
            date = today,
            streakCount = currentStreak,
            difficultyLevel = difficultyLevel.name,
            timeSpentMinutes = timeSpentMinutes,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        
        analyticsDao.insertHabitCompletionAnalytics(completion)
        updateStreakRetention(habitId, habitName, currentStreak, difficultyLevel)
    }
    
    private suspend fun calculateCurrentStreak(habitId: String): Int {
        val today = dateUtils.getCurrentDateString()
        val recentCompletions = analyticsDao.getHabitCompletions(
            habitId = habitId,
            startDate = dateUtils.getDateString(LocalDate.now().minusDays(30)),
            endDate = today
        ).first()
        
        var streak = 0
        var checkDate = LocalDate.now().minusDays(1)
        
        for (i in 0 until 30) {
            val dateString = dateUtils.getDateString(checkDate)
            val hasCompletion = recentCompletions.any { 
                it.date == dateString && it.isCompleted 
            }
            
            if (hasCompletion) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }
        
        return streak
    }
    
    override suspend fun trackScreenVisit(screenName: String, fromScreen: String?) = screenVisitMutex.withLock {
        val currentTime = System.currentTimeMillis()
        // Generate unique ID for this specific visit to ensure accurate duration tracking
        val visitId = UUID.randomUUID().toString()
        currentScreenVisitId = visitId
        val today = dateUtils.getCurrentDateString()
        
        val screenVisit = ScreenVisitAnalyticsEntity(
            screenName = screenName,
            sessionId = visitId,
            entryTimestamp = currentTime,
            date = today,
            fromScreen = fromScreen,
            createdAt = currentTime
        )
        
        analyticsDao.insertScreenVisitAnalytics(screenVisit)
        lastScreenStartTime = currentTime
    }
    
    override suspend fun endScreenVisit(interactionCount: Int, bounced: Boolean) = screenVisitMutex.withLock {
        val sessionId = currentScreenVisitId ?: return@withLock
        val currentTime = System.currentTimeMillis()
        
        analyticsDao.closeScreenVisit(
            sessionId = sessionId,
            exitTime = currentTime,
            duration = currentTime - (lastScreenStartTime ?: currentTime)
        )
        currentScreenVisitId = null
    }
    
    override suspend fun startAppSession() = sessionMutex.withLock {
        val sessionId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        val today = dateUtils.getCurrentDateString()
        
        val session = AppSessionEntity(
            sessionId = sessionId,
            startTimestamp = currentTime,
            startDate = today,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        
        analyticsDao.insertAppSession(session)
        currentSessionId = sessionId
    }
    
    override suspend fun endAppSession() = sessionMutex.withLock {
        val sessionId = currentSessionId ?: return@withLock
        val currentTime = System.currentTimeMillis()
        val today = dateUtils.getCurrentDateString()
        
        analyticsDao.endAppSession(
            sessionId = sessionId,
            endTime = currentTime,
            endDate = today,
            duration = currentTime - (sessionStartTime ?: currentTime)
        )
        currentSessionId = null
    }
    
    override suspend fun getHabitCompletionRate(habitId: String, timeFrame: TimeFrame): Flow<CompletionRate?> = flow {
        val (startDate, endDate) = dateUtils.getDateRange(timeFrame)
        val summary = analyticsDao.getHabitAnalyticsSummary(
            habitId = habitId,
            startDate = startDate,
            endDate = endDate
        )
        
        val completionRate = summary?.let {
            CompletionRate(
                habitId = it.habitId,
                habitName = it.habitName,
                totalDays = it.totalDays,
                completedDays = it.completedDays,
                completionPercentage = it.completionRate,
                currentStreak = it.currentStreak,
                longestStreak = it.longestStreak,
                weeklyAverage = it.completionRate / 7.0,
                monthlyAverage = it.completionRate / 30.0,
                timeFrame = timeFrame,
                lastUpdated = LocalDate.now()
            )
        }
        
        emit(completionRate)
    }
    
    override suspend fun getScreenVisitData(timeFrame: TimeFrame): Flow<List<ScreenVisit>> = flow {
        val (startDate, endDate) = dateUtils.getDateRange(timeFrame)
        val screenAnalytics = analyticsDao.getScreenAnalyticsSummary(
            startDate = startDate,
            endDate = endDate
        )
        
        emit(screenAnalytics.map {
            ScreenVisit(
                screenName = it.screenName,
                visitCount = it.totalVisits,
                totalTimeSpent = it.totalTimeSpent,
                averageSessionTime = it.averageTimeSpent,
                lastVisited = LocalDate.now(),
                engagementScore = it.engagementScore,
                bounceRate = it.bounceRate,
                timeFrame = timeFrame
            )
        })
    }
    
    override suspend fun getStreakRetentionData(timeFrame: TimeFrame): Flow<List<StreakRetention>> = flow {
        val (startDate, endDate) = dateUtils.getDateRange(timeFrame)
        
        // Get comprehensive habit analytics which includes streak information
        val habitAnalytics = analyticsDao.getComprehensiveHabitAnalytics(startDate, endDate)
        
        emit(habitAnalytics.map { habit ->
            StreakRetention(
                habitId = habit.habitId,
                habitName = habit.habitName,
                streakLength = habit.currentStreak,
                longestStreak = habit.longestStreak,
                streakStartDate = dateUtils.parseDate(startDate), // Approximation
                streakEndDate = if (habit.currentStreak > 0) null else dateUtils.parseDate(endDate),
                isActive = habit.currentStreak > 0,
                retentionProbability = 0.7, // Default probability
                difficultyLevel = DifficultyLevel.MODERATE, // Fixed: Use enum directly
                timeFrame = timeFrame
            )
        })
    }
    
    override suspend fun getComprehensiveAnalytics(timeFrame: TimeFrame): Flow<AnalyticsData> {
        val (startDate, endDate) = dateUtils.getDateRange(timeFrame)
        
        return flow {
            val habitSummaries = analyticsDao.getComprehensiveHabitAnalytics(startDate, endDate)
            val screenSummaries = analyticsDao.getScreenAnalyticsSummary(startDate, endDate)
            
            val completionRates = habitSummaries.map { h ->
                CompletionRate(
                    habitId = h.habitId,
                    habitName = h.habitName,
                    totalDays = h.totalDays,
                    completedDays = h.completedDays,
                    completionPercentage = h.completionRate,
                    currentStreak = h.currentStreak,
                    longestStreak = h.longestStreak,
                    weeklyAverage = 0.0,
                    monthlyAverage = 0.0,
                    timeFrame = timeFrame,
                    lastUpdated = LocalDate.now()
                )
            }
            
            val screenVisits = screenSummaries.map { s ->
                ScreenVisit(
                    screenName = s.screenName,
                    visitCount = s.totalVisits,
                    totalTimeSpent = s.totalTimeSpent,
                    averageSessionTime = s.averageTimeSpent,
                    lastVisited = LocalDate.now(),
                    engagementScore = s.engagementScore,
                    bounceRate = s.bounceRate,
                    timeFrame = timeFrame
                )
            }
            
            val streakRetentions = habitSummaries.map { habit ->
                StreakRetention(
                    habitId = habit.habitId,
                    habitName = habit.habitName,
                    streakLength = habit.currentStreak,
                    longestStreak = habit.longestStreak,
                    streakStartDate = dateUtils.parseDate(startDate), // Approximation
                    streakEndDate = if (habit.currentStreak > 0) null else dateUtils.parseDate(endDate),
                    isActive = habit.currentStreak > 0,
                    retentionProbability = 0.7, // Default probability
                    difficultyLevel = DifficultyLevel.MODERATE,
                    timeFrame = timeFrame
                )
            }

            val analyticsData = AnalyticsData(
                habitCompletionRates = completionRates,
                screenVisits = screenVisits,
                streakRetentions = streakRetentions,
                userEngagement = createUserEngagement(),
                timeRangeStats = createTimeRangeStats(timeFrame, startDate, endDate),
                exportMetadata = createExportMetadata()
            )
            
            emit(analyticsData)
        }
    }

    override suspend fun getUserEngagementMode(timeFrame: TimeFrame): Flow<UserEngagementMode> = flow {
        val (startDate, endDate) = dateUtils.getDateRange(timeFrame)
        val screenAnalytics = analyticsDao.getScreenAnalyticsSummary(startDate, endDate)
        
        if (screenAnalytics.isEmpty()) {
            emit(UserEngagementMode.UNKNOWN)
            return@flow
        }
        
        val totalVisits = screenAnalytics.sumOf { it.totalVisits }
        val totalTime = screenAnalytics.sumOf { it.totalTimeSpent }
        
        if (totalVisits == 0) {
            emit(UserEngagementMode.UNKNOWN)
            return@flow
        }
        
        val avgDuration = totalTime / totalVisits
        
        val mode = when {
            avgDuration < 30_000 -> UserEngagementMode.SPEED_RUNNER
            avgDuration in 30_000..120_000 -> UserEngagementMode.BALANCED
            else -> UserEngagementMode.PLANNER
        }
        emit(mode)
    }
    
    override suspend fun exportAnalyticsData(format: ExportFormat): String {
        val timeFrame = TimeFrame.MONTHLY // Fixed: Use proper enum value
        val analyticsData = getComprehensiveAnalytics(timeFrame).first()
        
        return when (format) {
            ExportFormat.JSON -> exportAsJson(analyticsData)
            ExportFormat.CSV -> exportAsCsv(analyticsData)
            ExportFormat.PDF -> exportAsPdf(analyticsData)
            ExportFormat.IMAGE -> "Image export not supported in repository directly"
        }
    }
    
    override suspend fun cleanupOldData(retentionDays: Int) {
        val cutoffDate = dateUtils.getDateString(LocalDate.now().minusDays(retentionDays.toLong()))
        analyticsDao.cleanupOldHabitCompletions(cutoffDate)
        analyticsDao.cleanupOldScreenVisits(cutoffDate)
        analyticsDao.cleanupOldEngagementData(cutoffDate)
        analyticsDao.cleanupOldSessions(cutoffDate)
        analyticsDao.cleanupOldPerformanceMetrics(cutoffDate)
    }

    override suspend fun clearAllData() {
        analyticsDao.deleteAllHabitCompletions()
        analyticsDao.deleteAllScreenVisits()
        analyticsDao.deleteAllUserEngagement()
        analyticsDao.deleteAllAppSessions()
        analyticsDao.deleteAllPerformanceMetrics()
    }
    
    private suspend fun updateStreakRetention(habitId: String, habitName: String, currentStreak: Int, difficultyLevel: DifficultyLevel) {
        streakRetentionMutex.withLock {
            val today = dateUtils.getCurrentDateString()
            
            if (currentStreak > 0) {
                // Start or continue a streak
                val activeStreak = analyticsDao.getActiveStreak(habitId)
                if (activeStreak == null) {
                    // Start new streak
                    val newStreak = StreakRetentionAnalyticsEntity(
                        habitId = habitId,
                        habitName = habitName,
                        streakLength = currentStreak,
                        startDate = today,
                        isActive = true,
                        maxStreakLength = currentStreak,
                        difficultyLevel = difficultyLevel.name,
                        retentionProbability = calculateRetentionProbability(currentStreak, difficultyLevel),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    analyticsDao.insertStreakRetentionAnalytics(newStreak)
                } else {
                    // Update existing streak
                    val updatedStreak = activeStreak.copy(
                        streakLength = currentStreak,
                        maxStreakLength = maxOf(activeStreak.maxStreakLength, currentStreak),
                        retentionProbability = calculateRetentionProbability(currentStreak, difficultyLevel),
                        updatedAt = System.currentTimeMillis()
                    )
                    analyticsDao.updateStreakRetentionAnalytics(updatedStreak)
                }
            } else {
                // End active streak
                analyticsDao.endActiveStreak(habitId, today)
            }
        }
    }
    
    private fun calculateRetentionProbability(streakLength: Int, difficultyLevel: DifficultyLevel): Double {
        val baseRetention = when (difficultyLevel) {
            DifficultyLevel.EASY -> 0.9
            DifficultyLevel.MODERATE -> 0.7
            DifficultyLevel.HARD -> 0.5
            DifficultyLevel.EXPERT -> 0.3
        }
        
        // Retention probability decreases slightly with longer streaks due to increased pressure
        val streakFactor = 1.0 - (streakLength * 0.01)
        return (baseRetention * streakFactor).coerceIn(0.1, 0.95)
    }
    
    private suspend fun createUserEngagement(): UserEngagement {
        val today = dateUtils.getCurrentDateString()
        val engagement = analyticsDao.getUserEngagementForDate(today)
        
        return if (engagement != null) {
            UserEngagement(
                dailyActiveUsage = engagement.habitsCompleted > 0,
                weeklyActiveUsage = true, // Default for now
                monthlyActiveUsage = true, // Default for now
                totalSessions = engagement.sessionCount,
                averageSessionLength = engagement.averageSessionMs,
                totalAppUsageTime = engagement.totalTimeSpentMs,
                engagementTrend = if (engagement.deepEngagement) EngagementTrend.INCREASING else EngagementTrend.STABLE,
                lastActiveDate = LocalDate.now()
            )
        } else {
            UserEngagement(
                dailyActiveUsage = false,
                weeklyActiveUsage = false,
                monthlyActiveUsage = false,
                totalSessions = 0,
                averageSessionLength = 0L,
                totalAppUsageTime = 0L,
                engagementTrend = EngagementTrend.STABLE,
                lastActiveDate = LocalDate.now()
            )
        }
    }
    
    private suspend fun createTimeRangeStats(timeFrame: TimeFrame, startDate: String, endDate: String): TimeRangeStats {
        val activeDays = analyticsDao.getActiveDaysCount(startDate, endDate)
        val uniqueHabits = analyticsDao.getUniqueHabitsCount(startDate, endDate)
        val overallCompletionRate = analyticsDao.getOverallCompletionRate(startDate, endDate) ?: 0.0
        val averageSessionDuration = analyticsDao.getAverageSessionDuration(startDate, endDate) ?: 0L
        
        return TimeRangeStats(
            timeFrame = timeFrame,
            startDate = dateUtils.parseDate(startDate),
            endDate = dateUtils.parseDate(endDate),
            totalHabits = uniqueHabits,
            activeHabits = uniqueHabits, // Default assumption
            completedSessions = 0, // Could be calculated if needed
            missedSessions = 0, // Could be calculated if needed
            averageStreakLength = 0.0, // Could be calculated if needed
            improvementRate = overallCompletionRate / 100.0 // Convert percentage to rate
        )
    }
    
    private fun createExportMetadata(): ExportMetadata {
        return ExportMetadata(
            exportDate = LocalDate.now(),
            dataVersion = "2.0",
            totalRecords = 0, // Will be calculated during export
            anonymized = true, // Fixed parameter name
            format = ExportFormat.JSON
        )
    }
    
    private fun exportAsJson(analyticsData: AnalyticsData): String {
        // Use Gson or similar to convert to JSON
        return "{\"exported\": true, \"format\": \"json\"}"
    }
    
    private fun exportAsCsv(analyticsData: AnalyticsData): String {
        val csv = StringBuilder()
        csv.append("Type,Name,Value,Date\n")
        
        analyticsData.habitCompletionRates.forEach { completion ->
            csv.append("Completion,${completion.habitName},${completion.completionPercentage}%,${completion.lastUpdated}\n")
        }
        
        analyticsData.screenVisits.forEach { screen ->
            csv.append("Screen,${screen.screenName},${screen.visitCount} visits,${screen.lastVisited}\n")
        }
        
        return csv.toString()
    }
    
    private fun exportAsPdf(analyticsData: AnalyticsData): String {
        // For now, return a placeholder - would need PDF generation library
        return "PDF export not implemented yet"
    }

    override suspend fun trackTimerEvent(
        eventType: String,
        habitId: Long?,
        sessionId: Long?,
        source: String?,
        extra: Map<String, Any?>
    ) = timerEventMutex.withLock {
        val now = System.currentTimeMillis()
        val payload = mutableMapOf<String, Any?>()
        if (habitId != null) payload["habitId"] = habitId
        if (sessionId != null) payload["sessionId"] = sessionId
        if (source != null) payload["source"] = source
        if (extra.isNotEmpty()) payload.putAll(extra)

        val json = payload.takeIf { it.isNotEmpty() }?.let { map ->
            // Very small JSON builder to avoid adding dependencies here
            map.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
                val value = when (v) {
                    null -> "null"
                    is Number, is Boolean -> v.toString()
                    else -> "\"" + v.toString().replace("\"", "\\\"") + "\""
                }
                "\"$k\":$value"
            }
        }

        analyticsDao.insertAnalyticsEvent(
            AnalyticsEventEntity(
                eventType = eventType,
                timestamp = java.util.Date(now),
                additionalData = json
            )
        )
    }
}