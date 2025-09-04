package com.habittracker.data.database.entity.timing

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import com.habittracker.data.database.entity.HabitEntity
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Database entity for habit timing preferences and settings
 * Part of Phase 1 - Smart Timing Enhancement
 */
@Entity(
    tableName = "habit_timing",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"], unique = true)]
)
data class HabitTimingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "habit_id")
    val habitId: Long,
    
    @ColumnInfo(name = "preferred_time")
    val preferredTime: String? = null,              // LocalTime as ISO string (HH:mm:ss)
    
    @ColumnInfo(name = "estimated_duration_minutes")
    val estimatedDurationMinutes: Int? = null,      // Duration in minutes
    
    @ColumnInfo(name = "timer_enabled")
    val timerEnabled: Boolean = false,

    // Phase 1: Guard-rails
    @ColumnInfo(name = "min_duration_minutes")
    val minDurationMinutes: Int? = null,            // Minimum duration to count as done

    @ColumnInfo(name = "require_timer_to_complete")
    val requireTimerToComplete: Boolean = false,     // If true, disallow completion without timer
    
    // Phase 2: Auto-complete when target reached
    @ColumnInfo(name = "auto_complete_on_target")
    val autoCompleteOnTarget: Boolean = false,
    
    @ColumnInfo(name = "reminder_style")
    val reminderStyle: String = "GENTLE",           // ReminderStyle enum as string
    
    @ColumnInfo(name = "is_scheduling_enabled")
    val isSchedulingEnabled: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Database entity for timer sessions
 */
@Entity(
    tableName = "timer_sessions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"]), Index(value = ["start_time"])]
)
data class TimerSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "habit_id")
    val habitId: Long,
    
    @ColumnInfo(name = "timer_type")
    val timerType: String = "SIMPLE",               // TimerType enum as string
    
    @ColumnInfo(name = "target_duration_minutes")
    val targetDurationMinutes: Int,
    
    @ColumnInfo(name = "is_running")
    val isRunning: Boolean = false,
    
    @ColumnInfo(name = "is_paused")
    val isPaused: Boolean = false,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long? = null,                    // LocalDateTime as timestamp
    
    @ColumnInfo(name = "paused_time")
    val pausedTime: Long? = null,                   // LocalDateTime as timestamp
    
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,                      // LocalDateTime as timestamp
    
    @ColumnInfo(name = "completed_sessions")
    val completedSessions: Int = 0,
    
    @ColumnInfo(name = "current_break_index")
    val currentBreakIndex: Int = 0,
    
    @ColumnInfo(name = "breaks_json")
    val breaksJson: String? = null,                 // List<Break> as JSON string
    
    @ColumnInfo(name = "actual_duration_minutes")
    val actualDurationMinutes: Int = 0,             // Duration in minutes
    
    @ColumnInfo(name = "interruptions")
    val interruptions: Int = 0,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/** Stores partial (incomplete) sessions logged by user. */
@Entity(
    tableName = "partial_sessions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"]), Index(value = ["created_at"])]
)
data class PartialSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "habit_id")
    val habitId: Long,
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Database entity for smart suggestions
 */
@Entity(
    tableName = "smart_suggestions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"]), Index(value = ["suggestion_type"]), Index(value = ["created_at"])]
)
data class SmartSuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "habit_id")
    val habitId: Long,
    
    @ColumnInfo(name = "suggestion_type")
    val suggestionType: String,                     // SuggestionType enum as string
    
    @ColumnInfo(name = "suggested_time")
    val suggestedTime: String? = null,              // LocalTime as ISO string
    
    @ColumnInfo(name = "suggested_duration_minutes")
    val suggestedDurationMinutes: Int? = null,      // Duration in minutes
    
    @ColumnInfo(name = "confidence")
    val confidence: Float,                          // 0.0 to 1.0
    
    @ColumnInfo(name = "reason")
    val reason: String,
    
    @ColumnInfo(name = "evidence_type")
    val evidenceType: String,                       // EvidenceType enum as string
    
    @ColumnInfo(name = "actionable")
    val actionable: Boolean = true,
    
    @ColumnInfo(name = "priority")
    val priority: String = "NORMAL",                // SuggestionPriority enum as string
    
    @ColumnInfo(name = "valid_until")
    val validUntil: Long? = null,                   // LocalDateTime as timestamp
    
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null,               // Map<String, String> as JSON
    
    @ColumnInfo(name = "accepted")
    val accepted: Boolean? = null,                  // null = not responded, true = accepted, false = rejected
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Database entity for completion metrics
 */
@Entity(
    tableName = "completion_metrics",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"]), Index(value = ["completion_date"])]
)
data class CompletionMetricsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "habit_id")
    val habitId: Long,
    
    @ColumnInfo(name = "completion_time")
    val completionTime: String,                     // LocalTime as ISO string
    
    @ColumnInfo(name = "session_duration_minutes")
    val sessionDurationMinutes: Int? = null,        // Actual time spent on habit
    
    @ColumnInfo(name = "energy_level")
    val energyLevel: Int? = null,                   // 1-5 scale (optional user input)
    
    @ColumnInfo(name = "efficiency_score")
    val efficiencyScore: Float? = null,             // 0.0 to 1.0
    
    @ColumnInfo(name = "context_tags")
    val contextTags: String? = null,                // JSON array of context information
    
    @ColumnInfo(name = "completion_date")
    val completionDate: Long,                       // LocalDate as timestamp
    
    @ColumnInfo(name = "weather_condition")
    val weatherCondition: String? = null,           // Optional weather context
    
    @ColumnInfo(name = "location_context")
    val locationContext: String? = null,            // Optional location context
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Database entity for aggregated habit analytics
 * This stores computed metrics to avoid expensive calculations
 */
@Entity(
    tableName = "habit_analytics",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"], unique = true)]
)
data class HabitAnalyticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "habit_id")
    val habitId: Long,
    
    @ColumnInfo(name = "average_completion_time")
    val averageCompletionTime: String? = null,      // LocalTime as ISO string
    
    @ColumnInfo(name = "optimal_time_slots_json")
    val optimalTimeSlotsJson: String? = null,       // List<TimeSlot> as JSON
    
    @ColumnInfo(name = "contextual_triggers_json")
    val contextualTriggersJson: String? = null,     // List<ContextTrigger> as JSON
    
    @ColumnInfo(name = "efficiency_score")
    val efficiencyScore: Float = 0f,
    
    @ColumnInfo(name = "consistency_score")
    val consistencyScore: Float = 0f,
    
    @ColumnInfo(name = "total_completions")
    val totalCompletions: Int = 0,
    
    @ColumnInfo(name = "average_session_duration_minutes")
    val averageSessionDurationMinutes: Int? = null,
    
    @ColumnInfo(name = "best_performance_time")
    val bestPerformanceTime: String? = null,        // LocalTime as ISO string
    
    @ColumnInfo(name = "worst_performance_time")
    val worstPerformanceTime: String? = null,       // LocalTime as ISO string
    
    @ColumnInfo(name = "weekly_pattern_json")
    val weeklyPatternJson: String? = null,          // Map<Int, Float> as JSON
    
    @ColumnInfo(name = "monthly_trend_json")
    val monthlyTrendJson: String? = null,           // Map<Int, Float> as JSON
    
    @ColumnInfo(name = "last_calculated")
    val lastCalculated: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
