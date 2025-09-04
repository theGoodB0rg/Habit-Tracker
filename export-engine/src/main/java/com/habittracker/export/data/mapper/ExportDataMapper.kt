package com.habittracker.export.data.mapper

import com.habittracker.export.data.entity.HabitCompletionEntity
import com.habittracker.export.data.entity.HabitEntity
import com.habittracker.export.data.entity.PartialSessionEntity
import com.habittracker.export.data.entity.TimerSessionEntity
import com.habittracker.export.data.model.ExportableCompletion
import com.habittracker.export.data.model.ExportableHabit
import com.habittracker.export.data.model.ExportablePartialSession
import com.habittracker.export.data.model.ExportableTimerSession
import com.habittracker.export.data.model.HabitCsvRow
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for converting database entities to exportable data models
 * Handles all data transformation logic for exports
 */
@Singleton
class ExportDataMapper @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Convert HabitEntity to ExportableHabit
     */
    fun toExportableHabit(
        habit: HabitEntity,
        completions: List<HabitCompletionEntity> = emptyList(),
        sessions: List<TimerSessionEntity> = emptyList(),
        partials: List<PartialSessionEntity> = emptyList()
    ): ExportableHabit {
        return ExportableHabit(
            id = habit.id,
            name = habit.name,
            description = habit.description,
            iconId = habit.iconId,
            frequency = habit.frequency.name,
            createdDate = formatDate(habit.createdDate),
            streakCount = habit.streakCount,
            longestStreak = habit.longestStreak,
            lastCompletedDate = habit.lastCompletedDate?.format(dateFormatter),
            isActive = habit.isActive,
            completions = completions.map { toExportableCompletion(it) },
            sessions = sessions.map { toExportableTimerSession(it) },
            partials = partials.map { toExportablePartial(it) }
        )
    }

    /**
     * Convert HabitCompletionEntity to ExportableCompletion
     */
    fun toExportableCompletion(completion: HabitCompletionEntity): ExportableCompletion {
        return ExportableCompletion(
            id = completion.id,
            habitId = completion.habitId,
            completedDate = completion.completedDate.format(dateFormatter),
            completedAt = completion.completedAt.format(dateTimeFormatter),
            note = completion.note,
            source = null // source not tracked yet in core entities
        )
    }

    /** Convert TimerSessionEntity to export model */
    fun toExportableTimerSession(session: TimerSessionEntity): ExportableTimerSession {
        fun fmt(dt: java.time.LocalDateTime?): String? = dt?.format(dateTimeFormatter)
        val state = when {
            session.endTime != null -> "ENDED"
            session.isPaused -> "PAUSED"
            session.isRunning -> "RUNNING"
            else -> "IDLE"
        }
        return ExportableTimerSession(
            id = session.id,
            habitId = session.habitId,
            timerType = session.timerType,
            targetDurationMinutes = session.targetDurationMinutes,
            state = state,
            startTime = fmt(session.startTime),
            pausedTime = fmt(session.pausedTime),
            endTime = fmt(session.endTime),
            actualDurationMinutes = session.actualDurationMinutes,
            interruptions = session.interruptions
        )
    }

    /** Convert PartialSessionEntity to export model */
    fun toExportablePartial(partial: PartialSessionEntity): ExportablePartialSession {
        val created = partial.createdAt.format(dateTimeFormatter)
        return ExportablePartialSession(
            id = partial.id,
            habitId = partial.habitId,
            durationMinutes = partial.durationMinutes,
            note = partial.note,
            createdAt = created
        )
    }

    /**
     * Convert habit and completion data to CSV row
     */
    fun toHabitCsvRow(
        habit: HabitEntity,
        completion: HabitCompletionEntity? = null
    ): HabitCsvRow {
        return HabitCsvRow(
            habitId = habit.id,
            habitName = habit.name,
            description = habit.description,
            frequency = habit.frequency.name,
            createdDate = formatDate(habit.createdDate),
            streakCount = habit.streakCount,
            longestStreak = habit.longestStreak,
            lastCompletedDate = habit.lastCompletedDate?.format(dateFormatter),
            isActive = habit.isActive,
            completionDate = completion?.completedDate?.format(dateFormatter),
            completionTime = completion?.completedAt?.format(dateTimeFormatter),
            completionNote = completion?.note
        )
    }

    /**
     * Generate CSV rows for habits with their completions
     */
    fun toHabitCsvRows(
        habits: List<HabitEntity>,
        completions: Map<Long, List<HabitCompletionEntity>>
    ): List<HabitCsvRow> {
        val rows = mutableListOf<HabitCsvRow>()
        
        habits.forEach { habit ->
            val habitCompletions = completions[habit.id] ?: emptyList()
            
            if (habitCompletions.isEmpty()) {
                // Add habit row without completion data
                rows.add(toHabitCsvRow(habit))
            } else {
                // Add one row for each completion
                habitCompletions.forEach { completion ->
                    rows.add(toHabitCsvRow(habit, completion))
                }
            }
        }
        
        return rows
    }

    /**
     * Format java.util.Date to string
     */
    private fun formatDate(date: java.util.Date): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
    }
}
