package com.habittracker.ui.models

import androidx.compose.runtime.Stable
import com.habittracker.data.database.entity.HabitEntity
import com.habittracker.data.database.entity.HabitFrequency
import com.habittracker.ui.models.timing.HabitTiming
import com.habittracker.ui.models.timing.TimerSession
import com.habittracker.ui.models.timing.SmartSuggestion
import com.habittracker.ui.models.timing.CompletionMetrics
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date

/**
 * Stable UI model for Habit that can be safely used in Compose
 * Enhanced with smart timing capabilities for Phase 1 implementation
 */
@Stable
data class HabitUiModel(
    val id: Long,
    val name: String,
    val description: String,
    val iconId: Int,
    val frequency: HabitFrequency,
    val createdDate: Date,
    val streakCount: Int,
    val longestStreak: Int,
    val lastCompletedDate: LocalDate?,
    val isActive: Boolean,
    
    // NEW: Phase 1 - Optional timing features (default = null/disabled for backward compatibility)
    val timing: HabitTiming? = null,
    val timerSession: TimerSession? = null,
    val smartSuggestions: List<SmartSuggestion> = emptyList(),
    val completionMetrics: CompletionMetrics? = null
) {
    // Original constructor for backward compatibility
    constructor(entity: HabitEntity) : this(
        id = entity.id,
        name = entity.name,
        description = entity.description,
        iconId = entity.iconId,
        frequency = entity.frequency,
        createdDate = entity.createdDate,
        streakCount = entity.streakCount,
        longestStreak = entity.longestStreak,
        lastCompletedDate = entity.lastCompletedDate,
        isActive = entity.isActive,
        
        // NEW: Map timing data if available (future implementation)
        timing = null, // Will be populated from database in Phase 1
        timerSession = null, // Will be populated from active timer state
        smartSuggestions = emptyList(), // Will be calculated dynamically
        completionMetrics = null // Will be populated from analytics
    )
    
    // NEW: Enhanced constructor with timing data
    constructor(
        entity: HabitEntity,
        timing: HabitTiming? = null,
        timerSession: TimerSession? = null,
        smartSuggestions: List<SmartSuggestion> = emptyList(),
        completionMetrics: CompletionMetrics? = null
    ) : this(
        id = entity.id,
        name = entity.name,
        description = entity.description,
        iconId = entity.iconId,
        frequency = entity.frequency,
        createdDate = entity.createdDate,
        streakCount = entity.streakCount,
        longestStreak = entity.longestStreak,
        lastCompletedDate = entity.lastCompletedDate,
        isActive = entity.isActive,
        timing = timing,
        timerSession = timerSession,
        smartSuggestions = smartSuggestions,
        completionMetrics = completionMetrics
    )
    
    // NEW: Convenience properties for all user types
    val hasTimer: Boolean 
        get() = timing?.timerEnabled == true
    
    val hasSchedule: Boolean 
        get() = timing?.preferredTime != null && timing.isSchedulingEnabled
    
    val isTimerActive: Boolean 
        get() = timerSession?.isRunning == true
    
    val isTimerPaused: Boolean 
        get() = timerSession?.isPaused == true
    
    val nextSuggestedTime: LocalTime? 
        get() = smartSuggestions.firstOrNull()?.suggestedTime
    
    val hasSmartFeatures: Boolean 
        get() = timing != null || smartSuggestions.isNotEmpty() || completionMetrics != null
    
    val timingComplexity: TimingComplexity 
        get() = when {
            !hasSmartFeatures -> TimingComplexity.NONE
            hasTimer && !hasSchedule && smartSuggestions.isEmpty() -> TimingComplexity.BASIC
            (hasTimer || hasSchedule) && smartSuggestions.isNotEmpty() -> TimingComplexity.INTERMEDIATE
            hasSmartFeatures && completionMetrics != null -> TimingComplexity.ADVANCED
            else -> TimingComplexity.BASIC
        }
    
    val estimatedDuration: java.time.Duration?
        get() = timing?.estimatedDuration
    
    val preferredTime: LocalTime?
        get() = timing?.preferredTime
        
    val highestConfidenceSuggestion: SmartSuggestion?
        get() = smartSuggestions.maxByOrNull { it.confidence }
}

/**
 * Extension function to convert HabitEntity to HabitUiModel
 */
fun HabitEntity.toUiModel(): HabitUiModel = HabitUiModel(this)

/**
 * Extension function to convert list of HabitEntity to list of HabitUiModel
 */
fun List<HabitEntity>.toUiModels(): List<HabitUiModel> = map { it.toUiModel() }

/**
 * Enhanced extension function to convert HabitEntity to HabitUiModel with timing data
 */
fun HabitEntity.toUiModelWithTiming(
    timing: HabitTiming? = null,
    timerSession: TimerSession? = null,
    smartSuggestions: List<SmartSuggestion> = emptyList(),
    completionMetrics: CompletionMetrics? = null
): HabitUiModel = HabitUiModel(
    entity = this,
    timing = timing,
    timerSession = timerSession,
    smartSuggestions = smartSuggestions,
    completionMetrics = completionMetrics
)

/**
 * Enum to represent timing complexity levels for progressive disclosure
 */
enum class TimingComplexity {
    NONE,           // No timing features enabled
    BASIC,          // Simple timer only
    INTERMEDIATE,   // Timer + basic scheduling/suggestions
    ADVANCED        // Full analytics and optimization
}
