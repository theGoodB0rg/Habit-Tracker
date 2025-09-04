package com.habittracker.domain.engine

import com.habittracker.data.database.dao.HabitCompletionDao
import com.habittracker.data.database.dao.HabitDao
import com.habittracker.data.database.entity.HabitCompletionEntity
import com.habittracker.data.database.entity.HabitEntity
import com.habittracker.domain.model.HabitStats
import com.habittracker.domain.model.HabitStreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core habit management engine - the heart of Phase 2
 * Handles all habit CRUD operations, streak management, and statistics
 */
@Singleton
class HabitManagementEngine @Inject constructor(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val streakEngine: StreakCalculationEngine
) {
    
    /**
     * Add a new habit
     */
    suspend fun addHabit(habit: HabitEntity): Long {
        return habitDao.insertHabit(habit)
    }
    
    /**
     * Update an existing habit
     */
    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }
    
    /**
     * Delete a habit (soft delete)
     */
    suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabit(habitId)
    }
    
    /**
     * Get all habits with live updates
     */
    fun getAllHabits(): Flow<List<HabitEntity>> {
        return habitDao.getAllHabits()
    }
    
    /**
     * Get a specific habit by ID
     */
    suspend fun getHabitById(habitId: Long): HabitEntity? {
        return habitDao.getHabitById(habitId)
    }
    
    /**
     * Mark habit as done for today (or specific date)
     */
    suspend fun markHabitAsDone(
        habitId: Long, 
        date: LocalDate = LocalDate.now(),
        note: String? = null
    ): HabitStreak {
        // Check if already completed today
        if (completionDao.isHabitCompletedOnDate(habitId, date)) {
            return getCurrentStreak(habitId)
        }
        
        // Add completion record
        val completion = HabitCompletionEntity(
            habitId = habitId,
            completedDate = date,
            completedAt = java.time.LocalDateTime.now(),
            note = note
        )
        completionDao.insertCompletion(completion)
        
        // Update habit entity with new streak information
        updateHabitStreaks(habitId)
        
        return getCurrentStreak(habitId)
    }
    
    /**
     * Unmark habit (remove completion for a specific date)
     */
    suspend fun unmarkHabitForDate(habitId: Long, date: LocalDate) {
        completionDao.deleteCompletion(habitId, date)
        updateHabitStreaks(habitId)
    }
    
    /**
     * Get current streak information for a habit
     */
    suspend fun getCurrentStreak(habitId: Long): HabitStreak {
        val habit = habitDao.getHabitById(habitId)
            ?: return HabitStreak(habitId, 0, 0, null)
        
        val completions = completionDao.getCompletionsForHabit(habitId).first()
        val completionDates = completions.map { it.completedDate }
        
        val currentStreak = streakEngine.calculateCurrentStreak(
            completionDates, 
            habit.frequency
        )
        
        val longestStreak = streakEngine.calculateLongestStreak(
            completionDates,
            habit.frequency
        )
        
        val lastCompletedDate = completionDates.maxOrNull()
        val isActiveToday = completionDates.contains(LocalDate.now())
        
        return HabitStreak(
            habitId = habitId,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastCompletedDate = lastCompletedDate,
            isActiveToday = isActiveToday
        )
    }
    
    /**
     * Get comprehensive statistics for a habit
     */
    suspend fun getHabitStats(habitId: Long): HabitStats {
        val habit = habitDao.getHabitById(habitId)
            ?: return HabitStats(habitId, 0, 0.0, 0.0, 0, 0, 0, 0)
        
        val completions = completionDao.getCompletionsForHabit(habitId).first()
        val completionDates = completions.map { it.completedDate }
        
        val totalCompletions = completions.size
        val currentStreak = getCurrentStreak(habitId)
        
        // Calculate completion rate (last 30 days)
        val thirtyDaysAgo = LocalDate.now().minusDays(30)
        val completionRate = streakEngine.calculateCompletionRate(
            completionDates,
            habit.frequency,
            thirtyDaysAgo,
            LocalDate.now()
        )
        
        // Calculate average streak length
        val averageStreakLength = if (totalCompletions > 0) {
            calculateAverageStreakLength(completionDates, habit.frequency)
        } else 0.0
        
        // Get week/month completions
        val weekStart = LocalDate.now().minusDays(6)
        val monthStart = LocalDate.now().minusDays(29)
        
        val weeklyCompletions = completionDao.getWeeklyCompletions(
            habitId, weekStart, LocalDate.now()
        )
        val monthlyCompletions = completionDao.getMonthlyCompletions(
            habitId, monthStart, LocalDate.now()
        )
        
        return HabitStats(
            habitId = habitId,
            totalCompletions = totalCompletions,
            completionRate = completionRate,
            averageStreakLength = averageStreakLength,
            longestStreak = currentStreak.longestStreak,
            currentStreak = currentStreak.currentStreak,
            completionsThisWeek = weeklyCompletions,
            completionsThisMonth = monthlyCompletions
        )
    }
    
    /**
     * Get habits with their streak information
     */
    suspend fun getHabitsWithStreaks(): List<Pair<HabitEntity, HabitStreak>> {
        val habits = habitDao.getAllHabits().first()
        return habits.map { habit ->
            val streak = getCurrentStreak(habit.id)
            habit to streak
        }
    }
    
    /**
     * Check if any habits are at risk of losing streaks
     */
    suspend fun getHabitsAtRisk(): List<HabitEntity> {
        val habits = habitDao.getAllHabits().first()
        val riskyHabits = mutableListOf<HabitEntity>()
        
        for (habit in habits) {
            val lastCompletion = completionDao.getLastCompletionDate(habit.id)
            if (streakEngine.isStreakAtRisk(lastCompletion, habit.frequency)) {
                riskyHabits.add(habit)
            }
        }
        
        return riskyHabits
    }
    
    /**
     * Get today's completion status for all habits
     */
    suspend fun getTodayCompletionStatus(): Map<Long, Boolean> {
        val today = LocalDate.now()
        val todayCompletions = completionDao.getTodayCompletions(today)
        val habits = habitDao.getAllHabits().first()
        
        return habits.associate { habit ->
            habit.id to todayCompletions.any { it.habitId == habit.id }
        }
    }
    
    /**
     * Update habit streak information in the database
     */
    private suspend fun updateHabitStreaks(habitId: Long) {
        val habit = habitDao.getHabitById(habitId) ?: return
        val streak = getCurrentStreak(habitId)
        
        val updatedHabit = habit.copy(
            streakCount = streak.currentStreak,
            longestStreak = streak.longestStreak,
            lastCompletedDate = streak.lastCompletedDate
        )
        
        habitDao.updateHabit(updatedHabit)
    }
    
    /**
     * Calculate average streak length from completion history
     */
    private fun calculateAverageStreakLength(
        completionDates: List<LocalDate>,
        frequency: com.habittracker.data.database.entity.HabitFrequency
    ): Double {
        if (completionDates.size < 2) return completionDates.size.toDouble()
        
        val sortedDates = completionDates.sorted()
        val streaks = mutableListOf<Int>()
        var currentStreak = 1
        
        for (i in 1 until sortedDates.size) {
            val daysBetween = ChronoUnit.DAYS.between(sortedDates[i-1], sortedDates[i]).toInt()
            val expectedInterval = when (frequency) {
                com.habittracker.data.database.entity.HabitFrequency.DAILY -> 1
                com.habittracker.data.database.entity.HabitFrequency.WEEKLY -> 7
                com.habittracker.data.database.entity.HabitFrequency.MONTHLY -> 30
            }
            
            if (daysBetween <= expectedInterval + 1) {
                currentStreak++
            } else {
                streaks.add(currentStreak)
                currentStreak = 1
            }
        }
        streaks.add(currentStreak)
        
        return streaks.average()
    }
}
