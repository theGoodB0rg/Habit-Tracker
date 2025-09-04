package com.habittracker.export.data.repository

import com.habittracker.data.database.dao.HabitCompletionDao
import com.habittracker.data.database.dao.HabitDao
import com.habittracker.export.data.entity.HabitCompletionEntity
import com.habittracker.export.data.entity.HabitEntity
import com.habittracker.export.data.entity.HabitFrequency
import com.habittracker.export.data.entity.TimerSessionEntity as ExportTimerSessionEntity
import com.habittracker.export.data.entity.PartialSessionEntity as ExportPartialSessionEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import com.habittracker.data.database.entity.HabitCompletionEntity as AppHabitCompletionEntity
import com.habittracker.data.database.entity.HabitEntity as AppHabitEntity
import com.habittracker.data.database.entity.HabitFrequency as AppHabitFrequency

/**
 * Implementation of ExportDataRepository using the main app's database DAOs
 * This bridges the export engine with the main app's data layer
 */
@Singleton
class AppExportDataRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao,
    private val timerSessionDao: com.habittracker.data.database.dao.timing.TimerSessionDao,
    private val partialSessionDao: com.habittracker.data.database.dao.timing.PartialSessionDao
) : com.habittracker.export.data.repository.ExportDataRepository {

    override suspend fun getAllHabits(): List<HabitEntity> {
        return habitDao.getAllHabits().first().map { it.toExportEntity() }
    }

    override suspend fun getActiveHabits(): List<HabitEntity> {
        return habitDao.getAllHabits().first().filter { it.isActive }.map { it.toExportEntity() }
    }

    override suspend fun getHabitById(habitId: Long): HabitEntity? {
        return habitDao.getHabitById(habitId)?.toExportEntity()
    }

    override suspend fun getHabitsById(habitIds: List<Long>): List<HabitEntity> {
        return habitIds.mapNotNull { habitDao.getHabitById(it)?.toExportEntity() }
    }

    override suspend fun getAllCompletions(): List<HabitCompletionEntity> {
        // Since there's no getAllCompletions method, we'll get all habits and their completions
        val allHabits = habitDao.getAllHabits().first()
        val allCompletions = mutableListOf<HabitCompletionEntity>()
        
        allHabits.forEach { habit ->
            val completions = habitCompletionDao.getCompletionsForHabit(habit.id).first()
            allCompletions.addAll(completions.map { it.toExportEntity() })
        }
        
        return allCompletions
    }

    override suspend fun getCompletionsForHabit(habitId: Long): List<HabitCompletionEntity> {
        return habitCompletionDao.getCompletionsForHabit(habitId).first().map { it.toExportEntity() }
    }

    override suspend fun getCompletionsForHabits(habitIds: List<Long>): List<HabitCompletionEntity> {
        val allCompletions = mutableListOf<HabitCompletionEntity>()
        
        habitIds.forEach { habitId ->
            val completions = habitCompletionDao.getCompletionsForHabit(habitId).first()
            allCompletions.addAll(completions.map { it.toExportEntity() })
        }
        
        return allCompletions
    }

    override suspend fun getCompletionsInDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitCompletionEntity> {
        val allHabits = habitDao.getAllHabits().first()
        val allCompletions = mutableListOf<HabitCompletionEntity>()
        
        allHabits.forEach { habit ->
            val completions = habitCompletionDao.getCompletionsInDateRange(
                habit.id, 
                startDate, 
                endDate
            )
            allCompletions.addAll(completions.map { it.toExportEntity() })
        }
        
        return allCompletions
    }

    override suspend fun getCompletionsForHabitInDateRange(
        habitId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitCompletionEntity> {
        return habitCompletionDao.getCompletionsInDateRange(habitId, startDate, endDate)
            .map { it.toExportEntity() }
    }

    // Phase 7: Timer sessions and partials
    override suspend fun getTimerSessionsForHabits(habitIds: List<Long>): List<ExportTimerSessionEntity> {
        val all = mutableListOf<ExportTimerSessionEntity>()
        habitIds.forEach { hid ->
            val sessions = timerSessionDao.getSessionsByHabitId(hid)
            all.addAll(sessions.map { it.toExportTimerEntity() })
        }
        return all
    }

    override suspend fun getTimerSessionsForHabit(habitId: Long): List<ExportTimerSessionEntity> {
        return timerSessionDao.getSessionsByHabitId(habitId).map { it.toExportTimerEntity() }
    }

    override suspend fun getTimerSessionsInDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ExportTimerSessionEntity> {
        // No direct date-range query; fetch recent by habit then filter by start/end timestamps
    val habits = habitDao.getAllHabits().first()
    val zone = java.time.ZoneId.systemDefault()
    val startTs = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val endTs = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val results = mutableListOf<ExportTimerSessionEntity>()
        habits.forEach { h ->
            val sessions = timerSessionDao.getSessionsByHabitId(h.id)
            results.addAll(
                sessions.filter { s ->
                    val st = s.startTime ?: s.createdAt
                    st != null && st in startTs..endTs
                }.map { it.toExportTimerEntity() }
            )
        }
        return results
    }

    override suspend fun getPartialSessionsForHabit(habitId: Long): List<ExportPartialSessionEntity> {
        return partialSessionDao.getRecentByHabit(habitId, limit = 500).map { it.toExportPartialEntity() }
    }

    override suspend fun getPartialSessionsForHabits(habitIds: List<Long>): List<ExportPartialSessionEntity> {
        val all = mutableListOf<ExportPartialSessionEntity>()
        habitIds.forEach { hid ->
            val parts = partialSessionDao.getRecentByHabit(hid, limit = 500)
            all.addAll(parts.map { it.toExportPartialEntity() })
        }
        return all
    }
}

/**
 * Extension functions to convert between main app entities and export entities
 */
private fun AppHabitEntity.toExportEntity(): HabitEntity {
    return HabitEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        iconId = this.iconId,
        frequency = this.frequency.toExportFrequency(),
        createdDate = this.createdDate,
        streakCount = this.streakCount,
        longestStreak = this.longestStreak,
        lastCompletedDate = this.lastCompletedDate,
        isActive = this.isActive
    )
}

private fun AppHabitCompletionEntity.toExportEntity(): HabitCompletionEntity {
    return HabitCompletionEntity(
        id = this.id,
        habitId = this.habitId,
        completedDate = this.completedDate,
        completedAt = this.completedAt,
        note = this.note
    )
}

private fun AppHabitFrequency.toExportFrequency(): HabitFrequency {
    return when (this) {
        AppHabitFrequency.DAILY -> HabitFrequency.DAILY
        AppHabitFrequency.WEEKLY -> HabitFrequency.WEEKLY
        AppHabitFrequency.MONTHLY -> HabitFrequency.MONTHLY
    }
}

private fun com.habittracker.data.database.entity.timing.TimerSessionEntity.toExportTimerEntity(): ExportTimerSessionEntity {
    // Convert epoch millis to LocalDateTime via system default zone
    fun ts(ms: Long?): java.time.LocalDateTime? = ms?.let {
        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    }
    return ExportTimerSessionEntity(
        id = this.id,
        habitId = this.habitId,
        timerType = this.timerType,
        targetDurationMinutes = this.targetDurationMinutes,
        isRunning = this.isRunning,
        isPaused = this.isPaused,
        startTime = ts(this.startTime),
        pausedTime = ts(this.pausedTime),
        endTime = ts(this.endTime),
        actualDurationMinutes = this.actualDurationMinutes,
        interruptions = this.interruptions,
        createdAt = ts(this.createdAt)
    )
}

private fun com.habittracker.data.database.entity.timing.PartialSessionEntity.toExportPartialEntity(): ExportPartialSessionEntity {
    val created = java.time.Instant.ofEpochMilli(this.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    return ExportPartialSessionEntity(
        id = this.id,
        habitId = this.habitId,
        durationMinutes = this.durationMinutes,
        note = this.note,
        createdAt = created
    )
}
