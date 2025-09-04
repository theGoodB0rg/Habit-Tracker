package com.habittracker.core

import android.content.Context
import com.habittracker.data.database.HabitDatabase
import com.habittracker.data.database.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Core repository interface for habit data operations.
 * Enhanced interface for real database connectivity across modules including widgets.
 */
interface HabitRepository {
    fun getAllHabits(): Flow<List<HabitEntity>>
    suspend fun getHabitById(id: Long): HabitEntity?
    suspend fun getTodayCompletionStatus(): Map<Long, Boolean>
    suspend fun markHabitCompleted(habitId: Long, date: LocalDate = LocalDate.now())
    suspend fun toggleHabitCompletion(habitId: Long, date: LocalDate = LocalDate.now()): Boolean
    
    companion object {
        @Volatile
        private var INSTANCE: HabitRepository? = null
        
        fun getInstance(context: Context): HabitRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DatabaseHabitRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Real database implementation for habit operations.
 * Connects directly to the app's Room database for authentic data.
 */
private class DatabaseHabitRepository(private val context: Context) : HabitRepository {
    
    private val database = HabitDatabase.getDatabase(context)
    private val habitDao = database.habitDao()
    private val completionDao = database.habitCompletionDao()
    
    override fun getAllHabits(): Flow<List<HabitEntity>> {
        return habitDao.getAllHabits().map { databaseHabits ->
            databaseHabits.map { dbHabit ->
                HabitEntity(
                    id = dbHabit.id,
                    name = dbHabit.name,
                    description = dbHabit.description,
                    iconId = dbHabit.iconId,
                    streakCount = dbHabit.streakCount,
                    lastCompletedDate = dbHabit.lastCompletedDate,
                    isDoneToday = false // Will be updated by getTodayCompletionStatus
                )
            }
        }
    }
    
    override suspend fun getHabitById(id: Long): HabitEntity? {
        val dbHabit = habitDao.getHabitById(id) ?: return null
        val today = LocalDate.now()
        val isCompleted = completionDao.isHabitCompletedOnDate(id, today)
        
        return HabitEntity(
            id = dbHabit.id,
            name = dbHabit.name,
            description = dbHabit.description,
            iconId = dbHabit.iconId,
            streakCount = dbHabit.streakCount,
            lastCompletedDate = dbHabit.lastCompletedDate,
            isDoneToday = isCompleted
        )
    }
    
    override suspend fun getTodayCompletionStatus(): Map<Long, Boolean> {
        val today = LocalDate.now()
        val todayCompletions = completionDao.getTodayCompletions(today)
        return todayCompletions.associate { it.habitId to true }
    }
    
    override suspend fun markHabitCompleted(habitId: Long, date: LocalDate) {
        val completionEntity = HabitCompletionEntity(
            habitId = habitId,
            completedDate = date,
            completedAt = LocalDateTime.now(),
            note = null
        )
        completionDao.insertCompletion(completionEntity)
        
        // Update streak
        updateHabitStreak(habitId, date)
    }
    
    override suspend fun toggleHabitCompletion(habitId: Long, date: LocalDate): Boolean {
        val isCurrentlyCompleted = completionDao.isHabitCompletedOnDate(habitId, date)
        
        return if (isCurrentlyCompleted) {
            // Mark as incomplete
            completionDao.deleteCompletion(habitId, date)
            false
        } else {
            // Mark as complete
            markHabitCompleted(habitId, date)
            true
        }
    }
    
    private suspend fun updateHabitStreak(habitId: Long, completionDate: LocalDate) {
        val habit = habitDao.getHabitById(habitId) ?: return
        val lastCompletedDate = habit.lastCompletedDate
        
        val newStreakCount = when {
            lastCompletedDate == null -> 1
            lastCompletedDate == completionDate.minusDays(1) -> habit.streakCount + 1
            lastCompletedDate == completionDate -> habit.streakCount
            else -> 1
        }
        
        habitDao.updateHabitStreak(habitId, newStreakCount, completionDate)
    }
}

/**
 * Core habit entity for cross-module usage.
 * Simplified version of the database entity for widget and shared operations.
 */
data class HabitEntity(
    val id: Long = 0,
    val name: String,
    val description: String,
    val iconId: Int = 0,
    val streakCount: Int = 0,
    val lastCompletedDate: LocalDate? = null,
    val isDoneToday: Boolean = false
)