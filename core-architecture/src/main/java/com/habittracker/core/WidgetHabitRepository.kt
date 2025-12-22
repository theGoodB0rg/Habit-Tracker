package com.habittracker.core

import android.content.Context
import com.habittracker.data.database.HabitDatabase
import com.habittracker.data.database.dao.HabitDao
import com.habittracker.data.database.dao.HabitCompletionDao
import com.habittracker.data.database.dao.timing.HabitTimingDao
import com.habittracker.data.database.dao.timing.SmartSuggestionDao
import com.habittracker.data.database.entity.HabitCompletionEntity
import com.habittracker.data.database.entity.HabitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.habittracker.core.PeriodKeyCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Singleton

/**
 * Widget-specific repository for efficient habit data operations.
 * Provides real database connectivity for widget functionality with optimized performance.
 * 
 * This repository is specifically designed for widget usage and implements:
 * - Direct database access without dependency injection complexity
 * - Efficient data queries optimized for widget display
 * - Immediate completion status updates
 * - Real-time progress calculations
 * - Professional error handling and fallbacks
 */
@Singleton
class WidgetHabitRepository private constructor(
    private val context: Context
) {
    
    companion object {
        @Volatile
        private var INSTANCE: WidgetHabitRepository? = null
        
        /**
         * Get singleton instance of WidgetHabitRepository
         * Thread-safe implementation for widget usage
         */
        fun getInstance(context: Context): WidgetHabitRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = WidgetHabitRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    // Database components - lazily initialized for performance
    private val database: HabitDatabase by lazy { 
        HabitDatabase.getDatabase(context) 
    }
    
    private val habitDao: HabitDao by lazy { 
        database.habitDao() 
    }
    
    private val completionDao: HabitCompletionDao by lazy { 
        database.habitCompletionDao() 
    }
    
    private val habitTimingDao: HabitTimingDao by lazy {
        database.habitTimingDao()
    }
    private val smartSuggestionDao: SmartSuggestionDao by lazy {
        database.smartSuggestionDao()
    }
    
    /**
     * Get today's habits with their current completion status and streak information.
     * Optimized for widget display with all necessary data in one call.
     * 
     * @return List of HabitWidgetData with real-time completion status
     */
    suspend fun getTodaysHabits(): List<HabitWidgetData> = withContext(Dispatchers.IO) {
        try {
            // Get all active habits
            val allHabits = habitDao.getAllHabits().first()
            val today = LocalDate.now()
            
            // Get today's completion status for all habits in one query
            val todayCompletions = completionDao.getTodayCompletions(today)
            val completionMap = todayCompletions.associateBy { it.habitId }
            
            // Transform to widget data format with all necessary information
            allHabits.map { habit ->
                val isCompleted = completionMap.containsKey(habit.id)
                
                HabitWidgetData(
                    id = habit.id,
                    name = habit.name,
                    description = habit.description,
                    icon = habit.iconId,
                    isCompleted = isCompleted,
                    currentStreak = habit.streakCount,
                    priority = 0, // Can be enhanced later based on UI needs
                    frequency = habit.frequency.name,
                    lastCompletedDate = habit.lastCompletedDate
                )
            }
        } catch (e: Exception) {
            // Graceful fallback - return empty list instead of crashing
            emptyList()
        }
    }
    
    /**
     * Toggle habit completion status for today.
     * Handles both marking complete and uncomplete with proper database operations.
     * 
     * @param habitId The ID of the habit to toggle
     * @return Boolean indicating the new completion status (true = completed, false = not completed)
     */
    suspend fun toggleHabitCompletion(habitId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
                val habit = habitDao.getHabitById(habitId)
                val frequency = habit?.frequency ?: com.habittracker.data.database.entity.HabitFrequency.DAILY
                val periodKey = PeriodKeyCalculator.fromDate(frequency, today)
                val isCurrentlyCompleted = completionDao.isHabitCompletedForPeriod(habitId, periodKey)
            
            if (isCurrentlyCompleted) {
                // Mark as incomplete - delete the completion record
                    completionDao.deleteCompletionForPeriod(habitId, periodKey)
                
                // Update habit streak (would need proper streak calculation logic)
                updateHabitStreakAfterUncomplete(habitId)
                
                false // Now incomplete
            } else {
                // Mark as complete - insert completion record
                    val completionEntity = HabitCompletionEntity(
                        habitId = habitId,
                        completedDate = today,
                        completedAt = LocalDateTime.now(),
                        periodKey = periodKey,
                        note = null // Widget doesn't support notes
                    )
                
                completionDao.insertCompletion(completionEntity)
                
                // Update habit streak
                updateHabitStreakAfterComplete(habitId, today)
                
                true // Now completed
            }
        } catch (e: Exception) {
            // Return current status on error to prevent UI inconsistency
            try {
                completionDao.isHabitCompletedOnDate(habitId, LocalDate.now())
            } catch (e2: Exception) {
                false
            }
        }
    }
    
    /**
     * Get progress statistics for today across all habits.
     * Used for widget progress display and summary information.
     * 
     * @return HabitProgressStats with completion counts and percentage
     */
    suspend fun getTodayProgressStats(): HabitProgressStats = withContext(Dispatchers.IO) {
        try {
            val habits = getTodaysHabits()
            val totalHabits = habits.size
            val completedHabits = habits.count { it.isCompleted }
            val percentage = if (totalHabits > 0) {
                (completedHabits * 100) / totalHabits
            } else {
                0
            }
            
            HabitProgressStats(
                totalHabits = totalHabits,
                completedHabits = completedHabits,
                percentage = percentage,
                remainingHabits = totalHabits - completedHabits
            )
        } catch (e: Exception) {
            // Fallback empty stats
            HabitProgressStats(0, 0, 0, 0)
        }
    }

    /**
     * Count how many habits have timers enabled.
     * Useful for showing a compact "Timers On" indicator in the widget.
     */
    suspend fun getTimerEnabledCount(): Int = withContext(Dispatchers.IO) {
        try {
            habitTimingDao.getTimerEnabledHabits().size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Compute the next suggested time across all habits based on Smart Suggestions.
     * Picks the soonest upcoming time today; if all suggestions are in the past, picks the earliest time tomorrow.
     */
    suspend fun getNextSuggestedTime(): LocalTime? = withContext(Dispatchers.IO) {
        try {
            val habits = habitDao.getAllHabits().first()
            if (habits.isEmpty()) return@withContext null

            val now = LocalTime.now()
            val suggestions: MutableList<LocalTime> = mutableListOf()
            for (h in habits) {
                // Query time suggestions for each habit (type string consistent with repository usage)
                val byType = smartSuggestionDao.getSuggestionsByType(h.id, "OPTIMAL_TIME")
                val times = byType.mapNotNull { e ->
                    try { e.suggestedTime?.let { LocalTime.parse(it) } } catch (_: Exception) { null }
                }
                if (times.isNotEmpty()) suggestions += times.first() // already sorted by confidence desc
            }

            if (suggestions.isEmpty()) return@withContext null

            // Choose soonest upcoming today; else earliest tomorrow
            val (future, past) = suggestions.partition { it >= now }
            val candidate = future.minOrNull() ?: past.minOrNull()
            candidate
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get specific habit details for widget operations.
     * 
     * @param habitId The ID of the habit to retrieve
     * @return HabitWidgetData or null if habit not found
     */
    suspend fun getHabitById(habitId: Long): HabitWidgetData? = withContext(Dispatchers.IO) {
        try {
            val habit = habitDao.getHabitById(habitId) ?: return@withContext null
            val today = LocalDate.now()
            val isCompleted = completionDao.isHabitCompletedOnDate(habitId, today)
            
            HabitWidgetData(
                id = habit.id,
                name = habit.name,
                description = habit.description,
                icon = habit.iconId,
                isCompleted = isCompleted,
                currentStreak = habit.streakCount,
                priority = 0,
                frequency = habit.frequency.name,
                lastCompletedDate = habit.lastCompletedDate
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Validate database connectivity and data integrity.
     * Used for widget health checks and error diagnostics.
     * 
     * @return Boolean indicating if database is accessible and working
     */
    suspend fun validateDatabaseConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simple query to test database connectivity
            habitDao.getActiveHabitsCount()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Private helper methods for streak management
    
    /**
     * Update habit streak after marking complete.
     * Implements proper streak calculation logic.
     */
    private suspend fun updateHabitStreakAfterComplete(habitId: Long, completionDate: LocalDate) {
        try {
            val habit = habitDao.getHabitById(habitId) ?: return
            val lastCompletedDate = habit.lastCompletedDate
            
            val newStreakCount = when {
                lastCompletedDate == null -> 1 // First completion
                lastCompletedDate == completionDate.minusDays(1) -> habit.streakCount + 1 // Consecutive day
                lastCompletedDate == completionDate -> habit.streakCount // Same day (shouldn't happen)
                else -> 1 // Non-consecutive, restart streak
            }
            
            habitDao.updateHabitStreak(habitId, newStreakCount, completionDate)
        } catch (e: Exception) {
            // Fail silently to prevent widget crashes
        }
    }
    
    /**
     * Update habit streak after marking incomplete.
     * Handles streak reduction logic properly.
     */
    private suspend fun updateHabitStreakAfterUncomplete(habitId: Long) {
        try {
            val habit = habitDao.getHabitById(habitId) ?: return
            val today = LocalDate.now()
            
            // If uncompleting today and it was the last completion, reduce streak
            if (habit.lastCompletedDate == today) {
                val newStreakCount = maxOf(0, habit.streakCount - 1)
                val previousCompletionDate = if (newStreakCount > 0) {
                    // Find the previous completion date
                    findPreviousCompletionDate(habitId, today)
                } else {
                    null
                }
                
                habitDao.updateHabitStreak(habitId, newStreakCount, previousCompletionDate)
            }
        } catch (e: Exception) {
            // Fail silently to prevent widget crashes
        }
    }
    
    /**
     * Find the most recent completion date before the given date.
     * Used for proper streak calculation after uncompleting.
     */
    private suspend fun findPreviousCompletionDate(habitId: Long, beforeDate: LocalDate): LocalDate? {
        return try {
            val completions = completionDao.getCompletionsInDateRange(
                habitId, 
                beforeDate.minusDays(365), // Look back up to a year
                beforeDate.minusDays(1)
            )
            completions.maxByOrNull { it.completedDate }?.completedDate
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Data class representing daily progress statistics for widget display.
 */
data class HabitProgressStats(
    val totalHabits: Int,
    val completedHabits: Int,
    val percentage: Int,
    val remainingHabits: Int
) {
    /**
     * Get formatted progress text for widget display
     */
    fun getProgressText(): String = "$completedHabits/$totalHabits ($percentage%)"
    
    /**
     * Get formatted daily summary text
     */
    fun getDailySummaryText(): String = "$percentage% Complete Today"
    
    /**
     * Check if all habits are completed
     */
    fun isAllCompleted(): Boolean = totalHabits > 0 && completedHabits == totalHabits
    
    /**
     * Check if no habits are completed
     */
    fun isNoneCompleted(): Boolean = completedHabits == 0
}
