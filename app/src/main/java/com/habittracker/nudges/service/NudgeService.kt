package com.habittracker.nudges.service

import com.habittracker.data.repository.HabitRepository
import com.habittracker.domain.model.HabitStats
import com.habittracker.nudges.scheduler.NudgeScheduler
import com.habittracker.nudges.usecase.HabitAnalysisData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import com.habittracker.domain.isCompletedThisPeriod
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing nudge generation integrated with habit data
 */
@Singleton
class NudgeService @Inject constructor(
    private val nudgeScheduler: NudgeScheduler,
    private val habitRepository: HabitRepository
) {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Starts the nudge service
     */
    fun startService() {
        nudgeScheduler.startScheduler()
        
        // Generate initial nudges
        serviceScope.launch {
            generateInitialNudges()
        }
    }
    
    /**
     * Stops the nudge service
     */
    fun stopService() {
        nudgeScheduler.stopScheduler()
        serviceScope.cancel()
    }
    
    /**
     * Generates nudges for all current habits
     */
    suspend fun generateNudgesForAllHabits() {
        try {
            val habits = habitRepository.getAllHabits().first()
            val habitsData = habits.map { habit ->
                val stats = createHabitStats(habit.id) // This would normally come from a stats repository
                val isCompletedToday = isCompletedThisPeriod(habit.frequency, habit.lastCompletedDate)
                
                HabitAnalysisData(
                    habitId = habit.id,
                    habitName = habit.name,
                    stats = stats,
                    lastCompletedDate = habit.lastCompletedDate,
                    isCompletedToday = isCompletedToday
                )
            }
            
            nudgeScheduler.generateNudgesForAllHabits(habitsData)
        } catch (e: Exception) {
            // Handle error - in a real app, log this
        }
    }
    
    /**
     * Generates nudges when a habit is completed
     */
    suspend fun onHabitCompleted(habitId: Long) {
        try {
            val habit = habitRepository.getHabitById(habitId)
            if (habit != null) {
                val stats = createHabitStats(habitId)
                val habitData = HabitAnalysisData(
                    habitId = habitId,
                    habitName = habit.name,
                    stats = stats,
                    lastCompletedDate = habit.lastCompletedDate,
                    isCompletedToday = isCompletedThisPeriod(habit.frequency, habit.lastCompletedDate)
                )
                
                nudgeScheduler.generateNudgesForHabit(habitData)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Generates nudges when a habit is missed
     */
    suspend fun onHabitMissed(habitId: Long) {
        try {
            val habit = habitRepository.getHabitById(habitId)
            if (habit != null) {
                val stats = createHabitStats(habitId)
                val habitData = HabitAnalysisData(
                    habitId = habitId,
                    habitName = habit.name,
                    stats = stats,
                    lastCompletedDate = habit.lastCompletedDate,
                    isCompletedToday = false
                )
                
                nudgeScheduler.generateNudgesForHabit(habitData)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Generates initial nudges when the service starts
     */
    private suspend fun generateInitialNudges() {
        delay(2000) // Wait a bit for the app to fully initialize
        generateNudgesForAllHabits()
    }
    
    /**
     * Creates habit statistics - in a real app this would come from a dedicated stats repository
     */
    private suspend fun createHabitStats(habitId: Long): HabitStats {
        // This is a placeholder implementation
        // In a real app, you'd calculate these from completion history
        return HabitStats(
            habitId = habitId,
            totalCompletions = 20,
            completionRate = 0.7,
            averageStreakLength = 4.5,
            longestStreak = 12,
            currentStreak = 3,
            completionsThisWeek = 4,
            completionsThisMonth = 20
        )
    }
}
