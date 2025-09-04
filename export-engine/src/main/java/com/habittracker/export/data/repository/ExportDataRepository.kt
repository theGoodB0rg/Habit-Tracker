package com.habittracker.export.data.repository

import com.habittracker.export.data.entity.HabitCompletionEntity
import com.habittracker.export.data.entity.HabitEntity
import com.habittracker.export.data.entity.TimerSessionEntity
import com.habittracker.export.data.entity.PartialSessionEntity
import java.time.LocalDate

/**
 * Repository for accessing habit data for export operations
 * Provides a clean interface between the export engine and the main app's data layer
 * Implementation will be provided by the main app module
 */
interface ExportDataRepository {
    suspend fun getAllHabits(): List<HabitEntity>
    suspend fun getActiveHabits(): List<HabitEntity>
    suspend fun getHabitById(habitId: Long): HabitEntity?
    suspend fun getHabitsById(habitIds: List<Long>): List<HabitEntity>
    suspend fun getAllCompletions(): List<HabitCompletionEntity>
    suspend fun getCompletionsForHabit(habitId: Long): List<HabitCompletionEntity>
    suspend fun getCompletionsForHabits(habitIds: List<Long>): List<HabitCompletionEntity>
    suspend fun getCompletionsInDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitCompletionEntity>
    suspend fun getCompletionsForHabitInDateRange(
        habitId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitCompletionEntity>

    // Phase 7: Timer sessions and partials
    suspend fun getTimerSessionsForHabits(habitIds: List<Long>): List<TimerSessionEntity>
    suspend fun getTimerSessionsForHabit(habitId: Long): List<TimerSessionEntity>
    suspend fun getTimerSessionsInDateRange(startDate: LocalDate, endDate: LocalDate): List<TimerSessionEntity>
    suspend fun getPartialSessionsForHabit(habitId: Long): List<PartialSessionEntity>
    suspend fun getPartialSessionsForHabits(habitIds: List<Long>): List<PartialSessionEntity>
}
