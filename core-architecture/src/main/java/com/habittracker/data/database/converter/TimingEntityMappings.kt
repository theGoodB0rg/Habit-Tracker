package com.habittracker.data.database.converter

import com.google.gson.reflect.TypeToken
import com.habittracker.core.model.timing.*
import com.habittracker.data.database.entity.timing.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Extension functions to convert between domain models and database entities
 * for timing-related data structures
 */

// HabitTiming conversions
fun HabitTiming.toEntity(habitId: Long): HabitTimingEntity {
    return HabitTimingEntity(
        habitId = habitId,
        preferredTime = preferredTime?.toString(),
        estimatedDurationMinutes = targetDuration?.toMinutes()?.toInt(),
        timerEnabled = false, // Set from UI model if needed
        reminderStyle = reminderStyle.name,
        isSchedulingEnabled = smartSchedulingEnabled,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun HabitTimingEntity.toDomainModel(): HabitTiming {
    return HabitTiming(
        preferredTime = preferredTime?.let { LocalTime.parse(it) },
        targetDuration = estimatedDurationMinutes?.let { Duration.ofMinutes(it.toLong()) },
        smartSchedulingEnabled = isSchedulingEnabled,
        reminderStyle = ReminderStyle.valueOf(reminderStyle),
        contextTriggers = emptyList(), // Load separately if needed
        flexibleSlots = emptyList(),
        breakSchedule = emptyList(),
        weeklyPattern = emptyMap()
    )
}

// TimerSession conversions (core model)
fun TimerSession.toEntity(habitId: Long): TimerSessionEntity {
    return TimerSessionEntity(
        habitId = habitId,
        timerType = timerType.name,
        targetDurationMinutes = targetDuration.toMinutes().toInt(),
        isRunning = isRunning,
        isPaused = isPaused,
        startTime = startTime?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        pausedTime = pausedTime?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        endTime = endTime?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        completedSessions = completedSessions,
        currentBreakIndex = currentBreakIndex,
        // Store as array-of-arrays for stability: [[minutes, name], ...]
        breaksJson = JsonAdapters.toJson(
            breaks.map { listOf(it.duration.toMinutes(), it.name ?: "") }
        ),
        actualDurationMinutes = actualDuration.toMinutes().toInt(),
        interruptions = interruptions,
        createdAt = System.currentTimeMillis()
    )
}

fun TimerSessionEntity.toDomainModel(): TimerSession {
    return TimerSession(
        timerType = TimerType.valueOf(timerType),
        targetDuration = Duration.ofMinutes(targetDurationMinutes.toLong()),
        isRunning = isRunning,
        isPaused = isPaused,
        startTime = startTime?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), java.time.ZoneOffset.UTC) },
        pausedTime = pausedTime?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), java.time.ZoneOffset.UTC) },
        endTime = endTime?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), java.time.ZoneOffset.UTC) },
        completedSessions = completedSessions,
        currentBreakIndex = currentBreakIndex,
        breaks = (
            // Preferred shape: array-of-arrays
            JsonAdapters.fromJson<List<List<Any>>>(
                breaksJson,
                object : TypeToken<List<List<Any>>>() {}.type
            )?.mapNotNull { parts ->
                try {
                    val minutes = (parts.getOrNull(0) as Number).toLong()
                    val name = parts.getOrNull(1)?.toString()
                    com.habittracker.core.model.timing.Break(
                        name = name ?: "Break",
                        duration = Duration.ofMinutes(minutes)
                    )
                } catch (_: Exception) { null }
            }
                // Fallback: list of Pair serialized as objects with "first"/"second"
                ?: JsonAdapters.fromJson<List<Map<String, Any>>>(
                    breaksJson,
                    object : TypeToken<List<Map<String, Any>>>() {}.type
                )?.mapNotNull { m ->
                    try {
                        val minutes = (m["first"] as Number).toLong()
                        val name = m["second"]?.toString()
                        com.habittracker.core.model.timing.Break(
                            name = name ?: "Break",
                            duration = Duration.ofMinutes(minutes)
                        )
                    } catch (_: Exception) { null }
                }
        ) ?: emptyList(),
        actualDuration = Duration.ofMinutes(actualDurationMinutes.toLong()),
        interruptions = interruptions
    )
}

// SmartSuggestion conversions
fun SmartSuggestion.toEntity(habitId: Long): SmartSuggestionEntity {
    return SmartSuggestionEntity(
        habitId = habitId,
        suggestionType = suggestionType.name,
        suggestedTime = suggestedTime?.toString(),
        suggestedDurationMinutes = suggestedDuration?.toMinutes()?.toInt(),
        confidence = confidence,
        reason = reason,
        evidenceType = evidenceType.name,
        actionable = actionable,
        priority = priority.name,
        validUntil = validUntil?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    metadataJson = JsonAdapters.toJson(metadata),
        accepted = null, // Set when user responds
        createdAt = System.currentTimeMillis()
    )
}

fun SmartSuggestionEntity.toDomainModel(): SmartSuggestion {
    return SmartSuggestion(
        suggestionType = SuggestionType.valueOf(suggestionType),
        suggestedTime = suggestedTime?.let { LocalTime.parse(it) },
        suggestedDuration = suggestedDurationMinutes?.let { Duration.ofMinutes(it.toLong()) },
        confidence = confidence,
        reason = reason,
        evidenceType = EvidenceType.valueOf(evidenceType),
        actionable = actionable,
        priority = SuggestionPriority.valueOf(priority),
        validUntil = validUntil?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), java.time.ZoneOffset.UTC) },
        metadata = JsonAdapters.fromJson<Map<String, String>>(
            metadataJson,
            object : TypeToken<Map<String, String>>() {}.type
        ) ?: emptyMap()
    )
}

// CompletionMetrics conversions
fun CompletionMetrics.toEntity(habitId: Long): CompletionMetricsEntity {
    return CompletionMetricsEntity(
        habitId = habitId,
        completionTime = completionTime.toString(),
        sessionDurationMinutes = sessionDuration?.toMinutes()?.toInt(),
        energyLevel = energyLevel,
        efficiencyScore = efficiencyScore,
        contextTags = contextTags.toString(), // Convert to JSON if needed
        completionDate = System.currentTimeMillis(), // Current timestamp
        weatherCondition = weatherCondition,
        locationContext = locationContext,
        createdAt = System.currentTimeMillis()
    )
}

fun CompletionMetricsEntity.toDomainModel(): CompletionMetrics {
    return CompletionMetrics(
        completionTime = LocalTime.parse(completionTime),
        sessionDuration = sessionDurationMinutes?.let { Duration.ofMinutes(it.toLong()) },
        energyLevel = energyLevel,
        efficiencyScore = efficiencyScore,
        contextTags = emptyList(), // Parse contextTags if needed
        weatherCondition = weatherCondition,
        locationContext = locationContext
    )
}

// HabitAnalytics conversions
fun HabitAnalytics.toEntity(habitId: Long): HabitAnalyticsEntity {
    return HabitAnalyticsEntity(
        habitId = habitId,
        averageCompletionTime = averageCompletionTime?.toString(),
        optimalTimeSlotsJson = JsonAdapters.toJson(optimalTimeSlots.map { slot ->
            mapOf(
                "start" to slot.startTime.toString(),
                "end" to slot.endTime.toString(),
                "priority" to slot.priority.name,
                "tags" to slot.contextTags
            )
        }),
        contextualTriggersJson = JsonAdapters.toJson(contextualTriggers.map { ct ->
            mapOf(
                "type" to ct.triggerType.name,
                "value" to ct.value,
                "condition" to ct.condition.name,
                "confidence" to ct.confidence
            )
        }),
        efficiencyScore = efficiencyScore,
        consistencyScore = consistencyScore,
        totalCompletions = totalCompletions,
        averageSessionDurationMinutes = averageSessionDuration?.toMinutes()?.toInt(),
        bestPerformanceTime = bestPerformanceTime?.toString(),
        worstPerformanceTime = worstPerformanceTime?.toString(),
    weeklyPatternJson = JsonAdapters.toJson(weeklyPattern),
    monthlyTrendJson = JsonAdapters.toJson(monthlyTrend),
        lastCalculated = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun HabitAnalyticsEntity.toDomainModel(): HabitAnalytics {
    return HabitAnalytics(
        averageCompletionTime = averageCompletionTime?.let { LocalTime.parse(it) },
        optimalTimeSlots = JsonAdapters.fromJson<List<Map<String, Any>>>(
            optimalTimeSlotsJson,
            object : TypeToken<List<Map<String, Any>>>() {}.type
        )?.mapNotNull { m ->
            try {
                val start = LocalTime.parse(m["start"].toString())
                val end = LocalTime.parse(m["end"].toString())
                val priority = com.habittracker.core.model.timing.SlotPriority.valueOf(m["priority"].toString())
                val tags = (m["tags"] as? List<*>)?.map { it.toString() } ?: emptyList()
                com.habittracker.core.model.timing.TimeSlot(start, end, priority, tags)
            } catch (_: Exception) { null }
        } ?: emptyList(),
        contextualTriggers = JsonAdapters.fromJson<List<Map<String, Any>>>(
            contextualTriggersJson,
            object : TypeToken<List<Map<String, Any>>>() {}.type
        )?.mapNotNull { m ->
            try {
                com.habittracker.core.model.timing.ContextTrigger(
                    triggerType = com.habittracker.core.model.timing.TriggerType.valueOf(m["type"].toString()),
                    value = m["value"].toString(),
                    condition = com.habittracker.core.model.timing.TriggerCondition.valueOf(m["condition"].toString()),
                    confidence = (m["confidence"] as Number).toFloat()
                )
            } catch (_: Exception) { null }
        } ?: emptyList(),
        efficiencyScore = efficiencyScore,
        consistencyScore = consistencyScore,
        totalCompletions = totalCompletions,
        averageSessionDuration = averageSessionDurationMinutes?.let { Duration.ofMinutes(it.toLong()) },
        bestPerformanceTime = bestPerformanceTime?.let { LocalTime.parse(it) },
        worstPerformanceTime = worstPerformanceTime?.let { LocalTime.parse(it) },
        weeklyPattern = JsonAdapters.fromJson<Map<Int, Float>>(
            weeklyPatternJson,
            object : TypeToken<Map<Int, Float>>() {}.type
        ) ?: emptyMap(),
        monthlyTrend = JsonAdapters.fromJson<Map<Int, Float>>(
            monthlyTrendJson,
            object : TypeToken<Map<Int, Float>>() {}.type
        ) ?: emptyMap()
    )
}
