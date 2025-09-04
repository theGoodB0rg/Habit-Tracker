package com.habittracker.export.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * Exportable habit data model
 * This is the main data structure used for JSON export
 */
@Serializable
data class ExportableHabit(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("icon_id")
    val iconId: Int,
    
    @SerializedName("frequency")
    val frequency: String,
    
    @SerializedName("created_date")
    val createdDate: String,
    
    @SerializedName("streak_count")
    val streakCount: Int,
    
    @SerializedName("longest_streak")
    val longestStreak: Int,
    
    @SerializedName("last_completed_date")
    val lastCompletedDate: String?,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("completions")
    val completions: List<ExportableCompletion> = emptyList(),
    @SerializedName("sessions")
    val sessions: List<ExportableTimerSession> = emptyList(),
    @SerializedName("partials")
    val partials: List<ExportablePartialSession> = emptyList()
)

/**
 * Exportable completion data model
 */
@Serializable
data class ExportableCompletion(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("habit_id")
    val habitId: Long,
    
    @SerializedName("completed_date")
    val completedDate: String,
    
    @SerializedName("completed_at")
    val completedAt: String,
    
    @SerializedName("note")
    val note: String?,
    // Optional source of completion (e.g., MANUAL, AUTO, WIDGET, NOTIF); may be null if not tracked
    @SerializedName("source")
    val source: String? = null
)

/**
 * Exportable timer session data model
 */
@Serializable
data class ExportableTimerSession(
    @SerializedName("id")
    val id: Long,
    @SerializedName("habit_id")
    val habitId: Long,
    @SerializedName("timer_type")
    val timerType: String,
    @SerializedName("target_duration_minutes")
    val targetDurationMinutes: Int,
    @SerializedName("state")
    val state: String, // RUNNING | PAUSED | ENDED
    @SerializedName("start_time")
    val startTime: String?,
    @SerializedName("paused_time")
    val pausedTime: String?,
    @SerializedName("end_time")
    val endTime: String?,
    @SerializedName("actual_duration_minutes")
    val actualDurationMinutes: Int,
    @SerializedName("interruptions")
    val interruptions: Int
)

/**
 * Exportable partial (incomplete) session data model
 */
@Serializable
data class ExportablePartialSession(
    @SerializedName("id")
    val id: Long,
    @SerializedName("habit_id")
    val habitId: Long,
    @SerializedName("duration_minutes")
    val durationMinutes: Int,
    @SerializedName("note")
    val note: String?,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Export metadata containing information about the export
 */
@Serializable
data class ExportMetadata(
    @SerializedName("export_version")
    val exportVersion: String = "1.0",
    
    @SerializedName("app_version")
    val appVersion: String,
    
    @SerializedName("export_date")
    val exportDate: String,
    
    @SerializedName("export_format")
    val exportFormat: String,
    
    @SerializedName("total_habits")
    val totalHabits: Int,
    
    @SerializedName("total_completions")
    val totalCompletions: Int,
    @SerializedName("total_sessions")
    val totalSessions: Int = 0,
    @SerializedName("total_partials")
    val totalPartials: Int = 0,
    
    @SerializedName("date_range")
    val dateRange: DateRange?,
    
    @SerializedName("export_scope")
    val exportScope: String
)

/**
 * Date range for filtered exports
 */
@Serializable
data class DateRange(
    @SerializedName("start_date")
    val startDate: String,
    
    @SerializedName("end_date")
    val endDate: String
)

/**
 * Complete export data structure
 */
@Serializable
data class HabitExportData(
    @SerializedName("metadata")
    val metadata: ExportMetadata,
    
    @SerializedName("habits")
    val habits: List<ExportableHabit>
)

/**
 * CSV row data for habit export
 */
data class HabitCsvRow(
    val habitId: Long,
    val habitName: String,
    val description: String,
    val frequency: String,
    val createdDate: String,
    val streakCount: Int,
    val longestStreak: Int,
    val lastCompletedDate: String?,
    val isActive: Boolean,
    val completionDate: String?,
    val completionTime: String?,
    val completionNote: String?
)
