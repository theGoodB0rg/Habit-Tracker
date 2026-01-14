package com.habittracker.data.repository.timing

import com.habittracker.data.database.dao.timing.*
import com.habittracker.data.database.converter.toDomainModel
import com.habittracker.data.database.converter.toEntity
import com.habittracker.data.database.converter.toUiModel
import com.habittracker.ui.models.timing.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import com.habittracker.timing.suggestions.PatternSuggestionEngine
import com.habittracker.data.database.entity.timing.CompletionMetricsEntity

/**
 * Repository for smart timing features
 * Part of Phase 1 - Smart Timing Enhancement
 */
interface TimingRepository {
    
    // Timing features
    suspend fun getHabitTiming(habitId: Long): HabitTiming?
    suspend fun saveHabitTiming(habitId: Long, timing: HabitTiming)
    suspend fun enableTimer(habitId: Long, duration: Duration? = null)
    suspend fun disableTimer(habitId: Long)
    suspend fun setPreferredTime(habitId: Long, time: LocalTime?)
    suspend fun setEstimatedDuration(habitId: Long, duration: Duration?)
    
    // Timer session management
    suspend fun getActiveTimerSessions(): Flow<List<TimerSession>>
    // Snapshot list of currently active sessions (not a Flow)
    suspend fun listActiveTimerSessions(): List<TimerSession>
    suspend fun getActiveTimerSession(habitId: Long): TimerSession?
    suspend fun getTimerSessionById(sessionId: Long): TimerSession?
    suspend fun startTimerSession(habitId: Long, timerType: TimerType = TimerType.SIMPLE, duration: Duration? = null): Long
    suspend fun pauseTimerSession(sessionId: Long)
    suspend fun resumeTimerSession(sessionId: Long)
    suspend fun completeTimerSession(sessionId: Long, actualDuration: Duration)
    suspend fun cancelTimerSession(sessionId: Long)
    suspend fun getRecentCompletedTimerSessions(habitId: Long, limit: Int = 14): List<TimerSession>
    
    // Smart suggestions
    suspend fun getSmartSuggestions(habitId: Long): List<SmartSuggestion>
    suspend fun generateSmartSuggestions(habitId: Long): List<SmartSuggestion>
    suspend fun recordSuggestionInteraction(suggestionId: Long, accepted: Boolean)
    suspend fun clearExpiredSuggestions()
    
    // Analytics and metrics
    suspend fun getCompletionMetrics(habitId: Long): Flow<CompletionMetrics?>
    suspend fun recordCompletionMetrics(habitId: Long, completionTime: LocalTime, duration: Duration? = null, efficiency: Float? = null)
    suspend fun updateHabitAnalytics(habitId: Long)

    // Partial sessions
    suspend fun logPartialSession(habitId: Long, duration: Duration, note: String? = null): Long
    suspend fun getRecentPartialSessions(habitId: Long, limit: Int = 20): List<Pair<Int, String?>>
}

/**
 * Implementation of TimingRepository
 */
@Singleton
class TimingRepositoryImpl @Inject constructor(
    private val habitTimingDao: HabitTimingDao,
    private val timerSessionDao: TimerSessionDao,
    private val smartSuggestionDao: SmartSuggestionDao,
    private val completionMetricsDao: CompletionMetricsDao,
    private val habitAnalyticsDao: HabitAnalyticsDao,
    private val partialSessionDao: PartialSessionDao,
    private val patternSuggestionEngine: PatternSuggestionEngine
) : TimingRepository {
    
    // Timing features implementation
    override suspend fun getHabitTiming(habitId: Long): HabitTiming? {
        return withContext(Dispatchers.IO) {
            habitTimingDao.getTimingByHabitId(habitId)?.toUiModel()
        }
    }
    
    override suspend fun saveHabitTiming(habitId: Long, timing: HabitTiming) {
        return withContext(Dispatchers.IO) {
            val entity = timing.toEntity(habitId)
            habitTimingDao.insertTiming(entity)
        }
    }
    
    override suspend fun enableTimer(habitId: Long, duration: Duration?) {
        withContext(Dispatchers.IO) {
            val existingTiming = getHabitTiming(habitId) ?: HabitTiming.createDefault()
            val updatedTiming = existingTiming.copy(
                timerEnabled = true,
                estimatedDuration = duration ?: existingTiming.estimatedDuration ?: Duration.ofMinutes(25)
            )
            saveHabitTiming(habitId, updatedTiming)
        }
    }
    
    override suspend fun disableTimer(habitId: Long) {
        withContext(Dispatchers.IO) {
            val existingTiming = getHabitTiming(habitId)
            if (existingTiming != null) {
                val updatedTiming = existingTiming.copy(timerEnabled = false)
                saveHabitTiming(habitId, updatedTiming)
            }
        }
    }
    
    override suspend fun setPreferredTime(habitId: Long, time: LocalTime?) {
        withContext(Dispatchers.IO) {
            val existingTiming = getHabitTiming(habitId) ?: HabitTiming.createDefault()
            val updatedTiming = existingTiming.copy(
                preferredTime = time,
                isSchedulingEnabled = time != null
            )
            saveHabitTiming(habitId, updatedTiming)
        }
    }
    
    override suspend fun setEstimatedDuration(habitId: Long, duration: Duration?) {
        withContext(Dispatchers.IO) {
            val existingTiming = getHabitTiming(habitId) ?: HabitTiming.createDefault()
            val updatedTiming = existingTiming.copy(estimatedDuration = duration)
            saveHabitTiming(habitId, updatedTiming)
        }
    }
    
    // Timer session management implementation
    override suspend fun getActiveTimerSessions(): Flow<List<TimerSession>> {
        return timerSessionDao.getAllActiveSessionsFlow()
            .map { entities -> entities.map { it.toUiModel() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun listActiveTimerSessions(): List<TimerSession> {
        return withContext(Dispatchers.IO) {
            timerSessionDao.getAllActiveSessions().map { it.toUiModel() }
        }
    }
    
    override suspend fun getActiveTimerSession(habitId: Long): TimerSession? {
        return withContext(Dispatchers.IO) {
            timerSessionDao.getActiveSessionByHabitId(habitId)?.toUiModel()
        }
    }

    override suspend fun getTimerSessionById(sessionId: Long): TimerSession? {
        return withContext(Dispatchers.IO) {
            timerSessionDao.getSessionById(sessionId)?.toUiModel()
        }
    }
    
    override suspend fun startTimerSession(habitId: Long, timerType: TimerType, duration: Duration?): Long {
        return withContext(Dispatchers.IO) {
            // Single-active-timer coordinator: pause other active sessions
            try {
                val active = timerSessionDao.getAllActiveSessions()
                val now = System.currentTimeMillis()
                active.filter { it.habitId != habitId && it.isRunning }
                    .forEach { session ->
                        timerSessionDao.updateSessionPausedState(session.id, true, now)
                    }
            } catch (_: Exception) { /* best-effort */ }
            // Get habit timing to determine duration if not provided
            val habitTiming = getHabitTiming(habitId)
            val sessionDuration = duration 
                ?: habitTiming?.estimatedDuration 
                ?: when (timerType) {
                    TimerType.POMODORO -> Duration.ofMinutes(25)
                    TimerType.SIMPLE -> Duration.ofMinutes(25)
                    TimerType.INTERVAL -> Duration.ofMinutes(20)
                    TimerType.PROGRESSIVE -> Duration.ofMinutes(15)
                    TimerType.CUSTOM -> Duration.ofMinutes(30)
                    TimerType.FLEXIBLE -> Duration.ofMinutes(30)
                    TimerType.FOCUS_SESSION -> Duration.ofMinutes(45)
                }
            
            // Create timer session based on type
            val session = when (timerType) {
                TimerType.POMODORO -> TimerSession.createPomodoro(habitId)
                TimerType.SIMPLE -> TimerSession.createSimple(habitId)
                else -> TimerSession.createCustom(habitId, sessionDuration)
            }.copy(
                startTime = LocalDateTime.now(),
                isRunning = true
            )
            
            timerSessionDao.insertSession(session.toEntity(habitId))
        }
    }
    
    override suspend fun pauseTimerSession(sessionId: Long) {
        withContext(Dispatchers.IO) {
            timerSessionDao.updateSessionPausedState(
                sessionId = sessionId,
                isPaused = true,
                pausedTime = System.currentTimeMillis()
            )
        }
    }
    
    override suspend fun resumeTimerSession(sessionId: Long) {
        withContext(Dispatchers.IO) {
            val session = timerSessionDao.getSessionById(sessionId)
            if (session != null && session.isPaused && session.pausedTime != null) {
                // Shift start time forward by the duration of the pause
                // This preserves the 'active' duration relative to wall clock time
                val now = System.currentTimeMillis()
                val pauseDuration = now - session.pausedTime!!
                val currentStart = session.startTime ?: now
                val newStart = currentStart + pauseDuration
                timerSessionDao.resumeSessionWithTimeShift(sessionId, newStart)
            } else {
                timerSessionDao.updateSessionPausedState(
                    sessionId = sessionId,
                    isPaused = false,
                    pausedTime = null
                )
            }
        }
    }
    
    override suspend fun completeTimerSession(sessionId: Long, actualDuration: Duration) {
        withContext(Dispatchers.IO) {
            timerSessionDao.completeSession(
                sessionId = sessionId,
                endTime = System.currentTimeMillis(),
                actualDurationMinutes = actualDuration.toMinutes().toInt()
            )
        }
    }
    
    override suspend fun cancelTimerSession(sessionId: Long) {
        withContext(Dispatchers.IO) {
            val session = timerSessionDao.getSessionById(sessionId)
            if (session != null) {
                timerSessionDao.deleteSession(session)
            }
        }
    }

    override suspend fun getRecentCompletedTimerSessions(habitId: Long, limit: Int): List<TimerSession> {
        return withContext(Dispatchers.IO) {
            timerSessionDao.getCompletedSessions(habitId, limit)
                .map { it.toUiModel() }
        }
    }
    
    // Smart suggestions implementation
    override suspend fun getSmartSuggestions(habitId: Long): List<SmartSuggestion> {
        return withContext(Dispatchers.IO) {
            val existing = smartSuggestionDao.getValidSuggestionsByHabitId(habitId).map { it.toUiModel() }
            if (existing.isNotEmpty()) {
                existing
            } else {
                // Auto-generate on first read so users see a suggestion on fresh installs
                generateSmartSuggestions(habitId)
            }
        }
    }
    
    override suspend fun generateSmartSuggestions(habitId: Long): List<SmartSuggestion> {
        return withContext(Dispatchers.IO) {
            val suggestions = mutableListOf<SmartSuggestion>()

    // Analyze completion patterns using lightweight engine
    val metrics = completionMetricsDao.getMetricsByHabitId(habitId)
    if (metrics.isNotEmpty()) {
            val times = metrics.mapNotNull {
                runCatching { LocalTime.parse(it.completionTime, DateTimeFormatter.ISO_LOCAL_TIME) }.getOrNull()
            }
            // Acceptance-informed multiplier: boost if user tends to accept time suggestions, reduce if rejected
            val acceptanceByType = smartSuggestionDao.getSuggestionAcceptanceRates(habitId)
                .associateBy { it.suggestion_type }

            patternSuggestionEngine.bestTimeByFrequency(times)?.let { bestTime ->
                val freq = times.count { t ->
                    // match on 30-min bucket
                    val bt = bestTime
                    val sameHour = t.hour == bt.hour
                    val sameHalf = (t.minute < 30 && bt.minute == 0) || (t.minute >= 30 && bt.minute == 30)
                    sameHour && sameHalf
                }.toFloat()
                var confidence = minOf(0.9f, freq / metrics.size.toFloat())
                acceptanceByType["OPTIMAL_TIME"]?.let { rate ->
                    val multiplier = (0.8f + rate.acceptance_rate * 0.6f).coerceIn(0.6f, 1.4f)
                    confidence = (confidence * multiplier).coerceIn(0.1f, 0.98f)
                }
                if (confidence >= 0.3f) {
                    suggestions.add(
                        SmartSuggestion.createTimeSuggestion(
                            habitId = habitId,
                            suggestedTime = bestTime,
                            confidence = confidence,
                            reason = "You’re more consistent around ${bestTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
                        )
                    )
                }
            }

            val successfulDurations = metrics.mapNotNull { it.sessionDurationMinutes }
            patternSuggestionEngine.optimalDurationMinutes(successfulDurations)?.let { modeMinutes ->
                var confidence = 0.7f.coerceAtMost((successfulDurations.size / 5f))
                acceptanceByType["OPTIMAL_DURATION"]?.let { rate ->
                    val multiplier = (0.8f + rate.acceptance_rate * 0.6f).coerceIn(0.6f, 1.4f)
                    confidence = (confidence * multiplier).coerceIn(0.1f, 0.98f)
                }
                if (confidence >= 0.3f) {
                    suggestions.add(
                        SmartSuggestion.createDurationSuggestion(
                            habitId = habitId,
                            suggestedDuration = Duration.ofMinutes(modeMinutes.toLong()),
                            confidence = confidence,
                            reason = "Typical session length appears to be ~${modeMinutes} min"
                        )
                    )
                }
            }
        } else {
            // Cold-start heuristics: propose something sensible without history
            val timing = getHabitTiming(habitId)

            // If user set an estimated duration, suggest using it
            timing?.estimatedDuration?.let { est ->
                suggestions.add(
                    SmartSuggestion.createDurationSuggestion(
                        habitId = habitId,
                        suggestedDuration = est,
                        confidence = 0.5f,
                        reason = "Quick start with your default ${est.toMinutes()} min"
                    )
                )
            }

            // If a preferred time exists, lightly suggest it
            timing?.preferredTime?.let { pref ->
                suggestions.add(
                    SmartSuggestion.createTimeSuggestion(
                        habitId = habitId,
                        suggestedTime = pref,
                        confidence = 0.45f,
                        reason = "Try your preferred time to build consistency"
                    )
                )
            }

            // If still nothing, propose an easy 10-minute starter as a nudge
            if (suggestions.isEmpty()) {
                suggestions.add(
                    SmartSuggestion.createDurationSuggestion(
                        habitId = habitId,
                        suggestedDuration = Duration.ofMinutes(10),
                        confidence = 0.4f,
                        reason = "Start small: 10 minutes is enough to build momentum"
                    )
                )
            }
        }
        
            // Save suggestions to database
            if (suggestions.isNotEmpty()) {
                val entities = suggestions.map { it.toEntity(habitId) }
                smartSuggestionDao.insertSuggestions(entities)
            }
            
            suggestions
        }
    }
    
    override suspend fun recordSuggestionInteraction(suggestionId: Long, accepted: Boolean) {
        withContext(Dispatchers.IO) {
            smartSuggestionDao.recordSuggestionResponse(suggestionId, accepted)
        }
    }
    
    override suspend fun clearExpiredSuggestions() {
        withContext(Dispatchers.IO) {
            smartSuggestionDao.deleteExpiredSuggestions()
        }
    }
    
    // Analytics and metrics implementation
    override suspend fun getCompletionMetrics(habitId: Long): Flow<CompletionMetrics?> {
    return habitAnalyticsDao.getAnalyticsByHabitIdFlow(habitId).map { entity ->
            entity?.let { 
                // Convert entity to CompletionMetrics domain model
                val total = entity.totalCompletions
                val slots: List<TimeSlot> = parseOptimalSlots(entity.optimalTimeSlotsJson, total)
                CompletionMetrics(
                    habitId = entity.habitId,
                    averageCompletionTime = entity.averageCompletionTime?.let { 
                        LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) 
                    },
                    efficiencyScore = entity.efficiencyScore,
                    consistencyScore = entity.consistencyScore,
                    totalCompletions = total,
                    averageSessionDuration = entity.averageSessionDurationMinutes?.let { 
                        Duration.ofMinutes(it.toLong()) 
                    },
                    bestPerformanceTime = entity.bestPerformanceTime?.let { 
                        LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) 
                    },
                    worstPerformanceTime = entity.worstPerformanceTime?.let { 
                        LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) 
                    },
                    optimalTimeSlots = slots
                )
            }
        }.flowOn(Dispatchers.IO)
    }
    
    override suspend fun recordCompletionMetrics(
        habitId: Long, 
        completionTime: LocalTime, 
        duration: Duration?, 
        efficiency: Float?
    ) {
        withContext(Dispatchers.IO) {
            val entity = com.habittracker.data.database.entity.timing.CompletionMetricsEntity(
                habitId = habitId,
                completionTime = completionTime.format(DateTimeFormatter.ISO_LOCAL_TIME),
                sessionDurationMinutes = duration?.toMinutes()?.toInt(),
                efficiencyScore = efficiency,
                completionDate = System.currentTimeMillis()
            )
            
            completionMetricsDao.insertMetrics(entity)
            
            // Trigger analytics recalculation
            updateHabitAnalytics(habitId)
        }
    }
    
    override suspend fun updateHabitAnalytics(habitId: Long) {
        withContext(Dispatchers.IO) {
            val metrics = completionMetricsDao.getMetricsByHabitId(habitId)
            
            if (metrics.isNotEmpty()) {
            // Calculate analytics
            val completionTimes = metrics.mapNotNull { 
                try { LocalTime.parse(it.completionTime, DateTimeFormatter.ISO_LOCAL_TIME) } catch (e: Exception) { null }
            }
            
            val averageTime = if (completionTimes.isNotEmpty()) {
                val totalMinutes = completionTimes.sumOf { it.hour * 60 + it.minute }
                val avgMinutes = totalMinutes / completionTimes.size
                LocalTime.of(avgMinutes / 60, avgMinutes % 60)
            } else null
            
            val efficiencyScores = metrics.mapNotNull { it.efficiencyScore }
            val avgEfficiency = if (efficiencyScores.isNotEmpty()) {
                efficiencyScores.average().toFloat()
            } else 0f

            // 30-minute bucket analytics
            val buckets = computeTimeBuckets(metrics)
            val bestTime = buckets.firstOrNull()?.start
            val worstTime = buckets.lastOrNull { it.count > 0 }?.start
            val topSlotsJson = buildOptimalSlotsJson(buckets.take(3))
            
                val analyticsEntity = com.habittracker.data.database.entity.timing.HabitAnalyticsEntity(
                habitId = habitId,
                averageCompletionTime = averageTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
                optimalTimeSlotsJson = topSlotsJson,
                efficiencyScore = avgEfficiency,
                consistencyScore = calculateConsistencyScore(metrics),
                totalCompletions = metrics.size,
                averageSessionDurationMinutes = metrics.mapNotNull { it.sessionDurationMinutes }.let { durations ->
                    if (durations.isNotEmpty()) durations.average().toInt() else null
                },
                bestPerformanceTime = bestTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
                worstPerformanceTime = worstTime?.format(DateTimeFormatter.ISO_LOCAL_TIME)
            )
                
                habitAnalyticsDao.insertAnalytics(analyticsEntity)
            }
        }
    }

    override suspend fun logPartialSession(habitId: Long, duration: Duration, note: String?): Long {
        return withContext(Dispatchers.IO) {
            val entity = com.habittracker.data.database.entity.timing.PartialSessionEntity(
                habitId = habitId,
                durationMinutes = duration.toMinutes().toInt(),
                note = note
            )
            partialSessionDao.insert(entity)
        }
    }

    override suspend fun getRecentPartialSessions(habitId: Long, limit: Int): List<Pair<Int, String?>> {
        return withContext(Dispatchers.IO) {
            partialSessionDao.getRecentByHabit(habitId, limit).map { it.durationMinutes to it.note }
        }
    }
    
    private fun calculateConsistencyScore(metrics: List<com.habittracker.data.database.entity.timing.CompletionMetricsEntity>): Float {
        if (metrics.size < 3) return 0f
        
        // Simple consistency calculation based on completion time variance
        val times = metrics.mapNotNull { 
            try { LocalTime.parse(it.completionTime, DateTimeFormatter.ISO_LOCAL_TIME) } catch (e: Exception) { null }
        }
        
        if (times.size < 3) return 0f
        
        val avgMinutes = times.sumOf { it.hour * 60 + it.minute } / times.size
        val variance = times.sumOf { 
            val minutes = it.hour * 60 + it.minute
            (minutes - avgMinutes) * (minutes - avgMinutes) 
        } / times.size
        
        // Convert variance to consistency score (0-1, where lower variance = higher consistency)
        return maxOf(0f, 1f - (variance / (4 * 60 * 60))) // 4-hour window for "consistent"
    }

    // Phase 8 helpers: 30-minute bucket analytics and JSON slots
}

// Helper data and functions are internal for test access within module
internal data class BucketInfo(
    val start: LocalTime,
    val end: LocalTime,
    val count: Int,
    val avgEfficiency: Float
)

internal fun computeTimeBuckets(metrics: List<CompletionMetricsEntity>): List<BucketInfo> {
    if (metrics.isEmpty()) return emptyList()
    data class Agg(var count: Int = 0, var effSum: Float = 0f, var effN: Int = 0)
    val map = HashMap<Int, Agg>()
    metrics.forEach { m ->
        val t = runCatching { LocalTime.parse(m.completionTime, DateTimeFormatter.ISO_LOCAL_TIME) }.getOrNull() ?: return@forEach
        val idx = (t.hour * 2) + if (t.minute >= 30) 1 else 0
        val a = map.getOrPut(idx) { Agg() }
        a.count++
        m.efficiencyScore?.let { e -> a.effSum += e; a.effN++ }
    }
    if (map.isEmpty()) return emptyList()
    val sorted = map.entries.sortedByDescending { it.value.count }
    return sorted.map { (idx, agg) ->
        val hour = idx / 2
        val minute = if (idx % 2 == 0) 0 else 30
        val start = LocalTime.of(hour, minute)
        val end = start.plusMinutes(30)
        val avgEff = if (agg.effN > 0) agg.effSum / agg.effN else 0f
        BucketInfo(start, end, agg.count, avgEff)
    }
}

internal fun buildOptimalSlotsJson(buckets: List<BucketInfo>): String? {
    if (buckets.isEmpty()) return null
    val payload = buckets.map { b ->
        mapOf(
            "start" to b.start.toString(),
            "end" to b.end.toString(),
            "sampleSize" to b.count,
            "avgEff" to b.avgEfficiency
        )
    }
    return com.habittracker.data.database.converter.JsonAdapters.toJson(payload)
}

internal fun parseOptimalSlots(json: String?, totalCompletions: Int): List<TimeSlot> {
    if (json.isNullOrBlank()) return emptyList()
    val list = com.habittracker.data.database.converter.JsonAdapters.fromJson<List<Map<String, Any>>>(
        json,
        object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
    ) ?: return emptyList()
    return list.mapNotNull { m ->
        try {
            val start = LocalTime.parse(m["start"].toString())
            val end = LocalTime.parse(m["end"].toString())
            val samples = (m["sampleSize"] as Number).toInt()
            val avgEff = (m["avgEff"] as Number).toFloat()
            val successRate = if (totalCompletions > 0) samples.toFloat() / totalCompletions.toFloat() else 0f
            TimeSlot(
                startTime = start,
                endTime = end,
                successRate = successRate,
                sampleSize = samples,
                averageEfficiency = avgEff
            )
        } catch (_: Exception) { null }
    }
}
