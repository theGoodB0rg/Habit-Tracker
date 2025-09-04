package com.habittracker.data.database.converter

import com.google.gson.reflect.TypeToken
import com.habittracker.ui.models.timing.*
import com.habittracker.data.database.entity.timing.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Extension functions to convert between UI models and database entities
 * for timing-related data structures in the app module
 */

// UI HabitTiming conversions
fun HabitTiming.toEntity(habitId: Long): HabitTimingEntity {
    return HabitTimingEntity(
        habitId = habitId,
        preferredTime = preferredTime?.toString(),
        estimatedDurationMinutes = estimatedDuration?.toMinutes()?.toInt(),
        timerEnabled = timerEnabled,
    minDurationMinutes = minDuration?.toMinutes()?.toInt(),
    requireTimerToComplete = requireTimerToComplete,
    autoCompleteOnTarget = autoCompleteOnTarget,
        reminderStyle = reminderStyle.name,
        isSchedulingEnabled = isSchedulingEnabled,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun HabitTimingEntity.toUiModel(): HabitTiming {
    return HabitTiming(
        preferredTime = preferredTime?.let { LocalTime.parse(it) },
        estimatedDuration = estimatedDurationMinutes?.let { Duration.ofMinutes(it.toLong()) },
        timerEnabled = timerEnabled,
    minDuration = minDurationMinutes?.let { Duration.ofMinutes(it.toLong()) },
    requireTimerToComplete = requireTimerToComplete,
    autoCompleteOnTarget = autoCompleteOnTarget,
        reminderStyle = ReminderStyle.valueOf(reminderStyle),
        isSchedulingEnabled = isSchedulingEnabled
    )
}

// UI TimerSession conversions
fun TimerSession.toEntity(habitId: Long): TimerSessionEntity {
    return TimerSessionEntity(
        habitId = habitId,
        timerType = type.name,
        targetDurationMinutes = targetDuration.toMinutes().toInt(),
        isRunning = isRunning,
        isPaused = isPaused,
        startTime = startTime?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        pausedTime = pausedTime?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        endTime = endTime?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        completedSessions = completedSessions,
        currentBreakIndex = currentBreakIndex,
        breaksJson = com.habittracker.data.database.converter.JsonAdapters.toJson(
            breaks.map { listOf(it.duration.toMinutes(), it.name ?: "") }
        ),
        actualDurationMinutes = actualDuration.toMinutes().toInt(),
        interruptions = interruptions,
        createdAt = System.currentTimeMillis()
    )
}

fun TimerSessionEntity.toUiModel(): TimerSession {
    return TimerSession(
        id = id,
        habitId = habitId,
        type = TimerType.valueOf(timerType),
        targetDuration = Duration.ofMinutes(targetDurationMinutes.toLong()),
        isRunning = isRunning,
        isPaused = isPaused,
        startTime = startTime?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), java.time.ZoneOffset.UTC) },
        pausedTime = pausedTime?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), java.time.ZoneOffset.UTC) },
        endTime = endTime?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), java.time.ZoneOffset.UTC) },
        completedSessions = completedSessions,
        currentBreakIndex = currentBreakIndex,
        breaks = com.habittracker.data.database.converter.JsonAdapters
            .fromJson<List<List<Any>>>(
                breaksJson,
                object : TypeToken<List<List<Any>>>() {}.type
            )
            ?.mapNotNull { parts ->
                try {
                    val minutes = (parts.getOrNull(0) as Number).toLong()
                    val name = parts.getOrNull(1)?.toString() ?: "Break"
                    Break(Duration.ofMinutes(minutes), BreakType.CUSTOM, name)
                } catch (_: Exception) { null }
            } ?: emptyList(),
        actualDuration = Duration.ofMinutes(actualDurationMinutes.toLong()),
        interruptions = interruptions
    )
}

// UI SmartSuggestion conversions
fun SmartSuggestion.toEntity(habitId: Long): SmartSuggestionEntity {
    return SmartSuggestionEntity(
        habitId = habitId,
        suggestionType = type.name,
        suggestedTime = suggestedTime?.toString(),
        suggestedDurationMinutes = suggestedDuration?.toMinutes()?.toInt(),
        confidence = confidence,
        reason = reason,
        evidenceType = evidenceType.name,
        actionable = actionable,
        priority = priority.name,
        validUntil = validUntil?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    metadataJson = com.habittracker.data.database.converter.JsonAdapters.toJson(metadata),
        accepted = null, // Set when user responds
        createdAt = System.currentTimeMillis()
    )
}

fun SmartSuggestionEntity.toUiModel(): SmartSuggestion {
    return SmartSuggestion(
        id = id,
        habitId = habitId,
        type = SuggestionType.valueOf(suggestionType),
        suggestedTime = suggestedTime?.let { LocalTime.parse(it) },
        suggestedDuration = suggestedDurationMinutes?.let { Duration.ofMinutes(it.toLong()) },
        confidence = confidence,
        reason = reason,
        evidenceType = EvidenceType.valueOf(evidenceType),
        actionable = actionable,
        priority = SuggestionPriority.valueOf(priority),
        validUntil = validUntil?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), java.time.ZoneOffset.UTC) },
        metadata = com.habittracker.data.database.converter.JsonAdapters
            .fromJson<Map<String, String>>(
                metadataJson,
                object : TypeToken<Map<String, String>>() {}.type
            ) ?: emptyMap()
    )
}

// Core to UI model conversions
fun com.habittracker.core.model.timing.HabitTiming.toUiModel(): HabitTiming {
    return HabitTiming(
        preferredTime = preferredTime,
        estimatedDuration = targetDuration,
        timerEnabled = false, // Default
        reminderStyle = ReminderStyle.valueOf(reminderStyle.name),
        isSchedulingEnabled = smartSchedulingEnabled
    )
}

fun HabitTiming.toCoreModel(): com.habittracker.core.model.timing.HabitTiming {
    return com.habittracker.core.model.timing.HabitTiming(
        preferredTime = preferredTime,
        targetDuration = estimatedDuration,
        smartSchedulingEnabled = isSchedulingEnabled,
        reminderStyle = com.habittracker.core.model.timing.ReminderStyle.valueOf(reminderStyle.name),
        contextTriggers = emptyList(),
        flexibleSlots = emptyList(),
        breakSchedule = emptyList(),
        weeklyPattern = emptyMap()
    )
}

fun com.habittracker.core.model.timing.TimerSession.toUiModel(): TimerSession {
    return TimerSession(
        id = 0, // Core model doesn't have ID
        habitId = 0, // Will be set by repository
        type = TimerType.valueOf(timerType.name),
        targetDuration = targetDuration,
        isRunning = isRunning,
        isPaused = isPaused,
        startTime = startTime,
        pausedTime = pausedTime,
        endTime = endTime,
        completedSessions = completedSessions,
        currentBreakIndex = currentBreakIndex,
        breaks = breaks.map { Break(it.duration, BreakType.CUSTOM, it.name) },
        actualDuration = actualDuration,
        interruptions = interruptions
    )
}

fun com.habittracker.core.model.timing.SmartSuggestion.toUiModel(): SmartSuggestion {
    return SmartSuggestion(
        id = 0, // Core model doesn't have ID
        habitId = 0, // Will be set by repository
        type = SuggestionType.valueOf(suggestionType.name),
        suggestedTime = suggestedTime,
        suggestedDuration = suggestedDuration,
        confidence = confidence,
        reason = reason,
        evidenceType = EvidenceType.valueOf(evidenceType.name),
        actionable = actionable,
        priority = SuggestionPriority.valueOf(priority.name),
        validUntil = validUntil,
        metadata = metadata
    )
}
