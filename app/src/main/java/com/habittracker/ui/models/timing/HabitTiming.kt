package com.habittracker.ui.models.timing

import androidx.compose.runtime.Stable
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Core timing models for Phase 1 - Smart Timing Enhancement
 * All models designed for progressive complexity and non-breaking changes
 */

// SIMPLE: For casual users
@Stable
data class HabitTiming(
    val preferredTime: LocalTime? = null,           // "I like to read at 8 PM"
    val estimatedDuration: Duration? = null,        // "Usually takes 30 minutes"
    val timerEnabled: Boolean = false,              // "Show me a timer"
    val minDuration: Duration? = null,              // Minimum required to count as done
    val requireTimerToComplete: Boolean = false,    // Disallow completion without timer
    val autoCompleteOnTarget: Boolean = false,      // Auto-complete when reaching target
    val reminderStyle: ReminderStyle = ReminderStyle.GENTLE,
    val isSchedulingEnabled: Boolean = false        // Whether user wants scheduling features
) {
    companion object {
        /**
         * Creates a default HabitTiming for new habits
         */
        fun createDefault(): HabitTiming = HabitTiming()
        
        /**
         * Creates a simple timer-enabled HabitTiming
         */
        fun createWithTimer(duration: Duration = Duration.ofMinutes(25)): HabitTiming = HabitTiming(
            estimatedDuration = duration,
            timerEnabled = true
        )
        
        /**
         * Creates a scheduled HabitTiming
         */
        fun createWithSchedule(preferredTime: LocalTime): HabitTiming = HabitTiming(
            preferredTime = preferredTime,
            isSchedulingEnabled = true
        )
    }
}

// INTERMEDIATE: For productivity users
@Stable
data class TimerSession(
    val id: Long = 0L,
    val habitId: Long,
    val type: TimerType = TimerType.SIMPLE,
    val targetDuration: Duration,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val startTime: LocalDateTime? = null,
    val pausedTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val completedSessions: Int = 0,
    val currentBreakIndex: Int = 0,
    val breaks: List<Break> = emptyList(),
    val actualDuration: Duration = Duration.ZERO,
    val interruptions: Int = 0
) {
    /**
     * Calculates elapsed time for the current session
     */
    val elapsedTime: Duration
        get() = when {
            startTime == null -> Duration.ZERO
            isPaused && pausedTime != null -> Duration.between(startTime, pausedTime)
            endTime != null -> Duration.between(startTime, endTime)
            isRunning -> Duration.between(startTime, LocalDateTime.now())
            else -> actualDuration
        }
    
    /**
     * Calculates remaining time for the current session
     */
    val remainingTime: Duration
        get() = (targetDuration - elapsedTime).let { remaining ->
            if (remaining.isNegative) Duration.ZERO else remaining
        }
    
    /**
     * Checks if the session is completed
     */
    val isCompleted: Boolean
        get() = endTime != null || elapsedTime >= targetDuration
    
    /**
     * Gets the current break if in break mode
     */
    val currentBreak: Break?
        get() = breaks.getOrNull(currentBreakIndex)
    
    /**
     * Checks if currently in a break period
     */
    val isInBreak: Boolean
        get() = currentBreak != null && !isCompleted
        
    companion object {
        /**
         * Creates a simple 25-minute timer session
         */
        fun createSimple(habitId: Long): TimerSession = TimerSession(
            habitId = habitId,
            type = TimerType.SIMPLE,
            targetDuration = Duration.ofMinutes(25)
        )
        
        /**
         * Creates a Pomodoro session (25 min work + 5 min break)
         */
        fun createPomodoro(habitId: Long): TimerSession = TimerSession(
            habitId = habitId,
            type = TimerType.POMODORO,
            targetDuration = Duration.ofMinutes(25),
            breaks = listOf(Break(Duration.ofMinutes(5), BreakType.SHORT))
        )
        
        /**
         * Creates a custom timer session
         */
        fun createCustom(habitId: Long, duration: Duration, breaks: List<Break> = emptyList()): TimerSession = TimerSession(
            habitId = habitId,
            type = TimerType.CUSTOM,
            targetDuration = duration,
            breaks = breaks
        )
    }
}

// ADVANCED: For power users
@Stable
data class SmartSuggestion(
    val id: Long = 0L,
    val habitId: Long,
    val type: SuggestionType,
    val suggestedTime: LocalTime? = null,
    val suggestedDuration: Duration? = null,
    val confidence: Float,                          // 0.0 to 1.0
    val reason: String,                            // "You're 83% more successful at this time"
    val evidenceType: EvidenceType,
    val actionable: Boolean = true,
    val priority: SuggestionPriority = SuggestionPriority.NORMAL,
    val validUntil: LocalDateTime? = null,         // When this suggestion expires
    val metadata: Map<String, String> = emptyMap() // Additional context data
) {
    /**
     * Checks if this suggestion is still valid
     */
    val isValid: Boolean
        get() = validUntil?.isAfter(LocalDateTime.now()) ?: true
    
    /**
     * Gets a user-friendly confidence description
     */
    val confidenceDescription: String
        get() = when {
            confidence >= 0.9f -> "Very confident"
            confidence >= 0.7f -> "Confident"
            confidence >= 0.5f -> "Moderately confident"
            confidence >= 0.3f -> "Low confidence"
            else -> "Experimental"
        }
        
    companion object {
        /**
         * Creates a time-based suggestion
         */
        fun createTimeSuggestion(
            habitId: Long,
            suggestedTime: LocalTime,
            confidence: Float,
            reason: String
        ): SmartSuggestion = SmartSuggestion(
            habitId = habitId,
            type = SuggestionType.OPTIMAL_TIME,
            suggestedTime = suggestedTime,
            confidence = confidence,
            reason = reason,
            evidenceType = EvidenceType.PERSONAL_PATTERN
        )
        
        /**
         * Creates a duration-based suggestion
         */
        fun createDurationSuggestion(
            habitId: Long,
            suggestedDuration: Duration,
            confidence: Float,
            reason: String
        ): SmartSuggestion = SmartSuggestion(
            habitId = habitId,
            type = SuggestionType.OPTIMAL_DURATION,
            suggestedDuration = suggestedDuration,
            confidence = confidence,
            reason = reason,
            evidenceType = EvidenceType.PERSONAL_PATTERN
        )
    }
}

// ANALYTICS: For data-driven users
@Stable
data class CompletionMetrics(
    val habitId: Long,
    val averageCompletionTime: LocalTime? = null,
    val optimalTimeSlots: List<TimeSlot> = emptyList(),
    val contextualTriggers: List<ContextTrigger> = emptyList(),
    val efficiencyScore: Float = 0f,              // 0.0 to 1.0
    val consistencyScore: Float = 0f,             // 0.0 to 1.0
    val totalCompletions: Int = 0,
    val averageSessionDuration: Duration? = null,
    val bestPerformanceTime: LocalTime? = null,
    val worstPerformanceTime: LocalTime? = null,
    val weeklyPattern: Map<Int, Float> = emptyMap(), // Day of week (1-7) to success rate
    val monthlyTrend: Map<Int, Float> = emptyMap()   // Month (1-12) to success rate
) {
    /**
     * Gets an overall performance score
     */
    val performanceScore: Float
        get() = (efficiencyScore + consistencyScore) / 2f
    
    /**
     * Gets the best day of the week for this habit
     */
    val bestDayOfWeek: Int?
        get() = weeklyPattern.maxByOrNull { it.value }?.key
    
    /**
     * Gets a performance description
     */
    val performanceDescription: String
        get() = when {
            performanceScore >= 0.8f -> "Excellent"
            performanceScore >= 0.6f -> "Good"
            performanceScore >= 0.4f -> "Fair"
            performanceScore >= 0.2f -> "Needs improvement"
            else -> "Getting started"
        }
}

// Supporting data classes
@Stable
data class Break(
    val duration: Duration,
    val type: BreakType = BreakType.SHORT,
    val name: String? = null
) {
    companion object {
        fun shortBreak(): Break = Break(Duration.ofMinutes(5), BreakType.SHORT, "Short Break")
        fun longBreak(): Break = Break(Duration.ofMinutes(15), BreakType.LONG, "Long Break")
        fun custom(duration: Duration, name: String): Break = Break(duration, BreakType.CUSTOM, name)
    }
}

@Stable
data class TimeSlot(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val successRate: Float,                        // 0.0 to 1.0
    val sampleSize: Int,                          // Number of attempts in this slot
    val averageEfficiency: Float = 0f             // How efficiently habits are completed
) {
    val duration: Duration
        get() = Duration.between(startTime, endTime)
    
    val isOptimal: Boolean
        get() = successRate >= 0.7f && sampleSize >= 3
}

@Stable
data class ContextTrigger(
    val type: ContextType,
    val value: String,
    val successRate: Float,
    val description: String
)

// Enums for type safety
enum class TimerType {
    SIMPLE,                 // Basic countdown timer
    POMODORO,              // 25/5/15 cycle
    INTERVAL,              // Custom interval training  
    PROGRESSIVE,           // Gradually increasing duration
    CUSTOM,                // User-defined duration and breaks
    FLEXIBLE,              // Open-ended with tracking / Adaptive timing based on context
    FOCUS_SESSION          // Distraction-free mode
}

enum class ReminderStyle {
    GENTLE,                // Soft notifications
    STANDARD,              // Normal notifications
    PERSISTENT,            // More frequent reminders
    SMART,                 // Context-aware timing
    OFF                    // No reminders
}

enum class SuggestionType {
    OPTIMAL_TIME,          // Best time to do this habit
    OPTIMAL_DURATION,      // Best duration for this habit
    DURATION_ADJUSTMENT,   // Suggest duration changes
    BREAK_OPTIMIZATION,    // Suggest break improvements
    CONTEXT_OPPORTUNITY,   // Suggest contextual opportunities
    EFFICIENCY_BOOST,      // Suggest efficiency improvements
    HABIT_PAIRING,         // Suggest habit stacking
    RECOVERY_TIME,         // Suggest recovery periods
    HABIT_STACKING,        // Combine with other habits
    CONTEXT_OPTIMIZATION,  // Environmental suggestions
    ENERGY_ALIGNMENT,      // Match with energy levels
    SCHEDULE_OPTIMIZATION, // Calendar integration
    WEATHER_ALTERNATIVE    // Weather-based alternatives
}

enum class EvidenceType {
    PERSONAL_PATTERN,      // Based on user's own data
    RESEARCH_BASED,        // Based on scientific research
    COMMUNITY_PATTERN,     // Based on similar users (future)
    CONTEXTUAL_ANALYSIS,   // Based on environmental factors
    MACHINE_LEARNING       // AI-generated insights (future)
}

enum class SuggestionPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

enum class BreakType {
    SHORT,                 // 5-10 minutes
    LONG,                  // 15-30 minutes
    CUSTOM,                // User-defined
    MICRO                  // 1-2 minutes
}

enum class ContextType {
    LOCATION,              // Where the habit is done
    WEATHER,               // Weather conditions
    TIME_OF_DAY,          // Morning, afternoon, evening
    ENERGY_LEVEL,         // High, medium, low energy
    SOCIAL_CONTEXT,       // Alone, with others
    DEVICE_USAGE,         // Phone usage patterns
    CALENDAR_CONTEXT      // Meetings, free time, etc.
}
