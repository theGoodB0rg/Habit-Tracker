package com.habittracker.core.model.timing

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Core timing models for the habit tracker
 * These are the domain models used throughout the application
 * Part of Phase 1 - Smart Timing Enhancement
 */

/**
 * Represents a break interval during habit execution
 */
data class Break(
    val name: String,
    val duration: Duration,
    val type: BreakType = BreakType.REST
)

/**
 * Types of breaks during habit execution
 */
enum class BreakType {
    REST,           // Simple rest break
    ACTIVE,         // Active recovery break
    MEDITATION,     // Mindfulness break
    HYDRATION,      // Water/nutrition break
    CUSTOM          // User-defined break
}

/**
 * Represents a flexible time slot for habit execution
 */
data class TimeSlot(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val priority: SlotPriority = SlotPriority.MEDIUM,
    val contextTags: List<String> = emptyList()
)

/**
 * Priority levels for time slots
 */
enum class SlotPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Represents a context-based trigger for habit execution
 */
data class ContextTrigger(
    val triggerType: TriggerType,
    val value: String,
    val condition: TriggerCondition,
    val confidence: Float = 1.0f
)

/**
 * Types of context triggers
 */
enum class TriggerType {
    LOCATION,       // Based on user location
    ACTIVITY,       // Based on current activity
    TIME,           // Based on specific time patterns
    WEATHER,        // Based on weather conditions
    CALENDAR,       // Based on calendar events
    CUSTOM          // User-defined trigger
}

/**
 * Conditions for trigger activation
 */
enum class TriggerCondition {
    EQUALS,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    GREATER_THAN,
    LESS_THAN,
    IN_RANGE
}

/**
 * Main habit timing configuration
 */
data class HabitTiming(
    val preferredTime: LocalTime? = null,
    val targetDuration: Duration? = null,
    val flexibleSlots: List<TimeSlot> = emptyList(),
    val breakSchedule: List<Break> = emptyList(),
    val smartSchedulingEnabled: Boolean = false,
    val contextTriggers: List<ContextTrigger> = emptyList(),
    val weeklyPattern: Map<Int, Float> = emptyMap(), // Day to probability mapping
    val reminderStyle: ReminderStyle = ReminderStyle.GENTLE
)

/**
 * Reminder styles for habit notifications
 */
enum class ReminderStyle {
    GENTLE,         // Soft, non-intrusive reminders
    PERSISTENT,     // Regular reminders until acknowledged
    MOTIVATIONAL,   // Encouraging and motivational
    MINIMAL,        // Very subtle reminders
    CUSTOM          // User-defined style
}

/**
 * Timer session for habit execution
 */
data class TimerSession(
    val timerType: TimerType = TimerType.SIMPLE,
    val targetDuration: Duration,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val startTime: LocalDateTime? = null,
    val pausedTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val completedSessions: Int = 0,
    val breaks: List<Break> = emptyList(),
    val currentBreakIndex: Int = 0,
    val actualDuration: Duration = Duration.ZERO,
    val interruptions: Int = 0
)

/**
 * Types of timers available
 */
enum class TimerType {
    SIMPLE,         // Basic countdown timer
    POMODORO,       // Pomodoro technique with breaks
    INTERVAL,       // Custom interval training
    PROGRESSIVE,    // Gradually increasing duration
    FLEXIBLE        // Adaptive timing based on context
}

/**
 * Smart suggestion for habit optimization
 */
data class SmartSuggestion(
    val suggestionType: SuggestionType,
    val suggestedTime: LocalTime? = null,
    val suggestedDuration: Duration? = null,
    val confidence: Float,
    val reason: String,
    val evidenceType: EvidenceType,
    val actionable: Boolean = true,
    val priority: SuggestionPriority = SuggestionPriority.NORMAL,
    val validUntil: LocalDateTime? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Types of smart suggestions
 */
enum class SuggestionType {
    OPTIMAL_TIME,           // Suggest better timing
    DURATION_ADJUSTMENT,    // Suggest duration changes
    BREAK_OPTIMIZATION,     // Suggest break improvements
    CONTEXT_OPPORTUNITY,    // Suggest contextual opportunities
    EFFICIENCY_BOOST,       // Suggest efficiency improvements
    HABIT_PAIRING,         // Suggest habit stacking
    RECOVERY_TIME          // Suggest recovery periods
}

/**
 * Evidence types for suggestions
 */
enum class EvidenceType {
    COMPLETION_PATTERN,     // Based on completion history
    TIME_ANALYSIS,          // Based on timing analysis
    EFFICIENCY_METRICS,     // Based on efficiency data
    CONTEXT_CORRELATION,    // Based on context patterns
    USER_FEEDBACK,          // Based on user ratings
    EXTERNAL_DATA,          // Based on external factors
    MACHINE_LEARNING        // Based on ML predictions
}

/**
 * Priority levels for suggestions
 */
enum class SuggestionPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Completion metrics for habit performance analysis
 */
data class CompletionMetrics(
    val completionTime: LocalTime,
    val sessionDuration: Duration? = null,
    val energyLevel: Int? = null,        // 1-5 scale
    val efficiencyScore: Float? = null,  // 0.0 to 1.0
    val contextTags: List<String> = emptyList(),
    val weatherCondition: String? = null,
    val locationContext: String? = null
)

/**
 * Aggregated analytics for habit optimization
 */
data class HabitAnalytics(
    val averageCompletionTime: LocalTime? = null,
    val optimalTimeSlots: List<TimeSlot> = emptyList(),
    val contextualTriggers: List<ContextTrigger> = emptyList(),
    val efficiencyScore: Float = 0f,
    val consistencyScore: Float = 0f,
    val totalCompletions: Int = 0,
    val averageSessionDuration: Duration? = null,
    val bestPerformanceTime: LocalTime? = null,
    val worstPerformanceTime: LocalTime? = null,
    val weeklyPattern: Map<Int, Float> = emptyMap(),
    val monthlyTrend: Map<Int, Float> = emptyMap()
)
