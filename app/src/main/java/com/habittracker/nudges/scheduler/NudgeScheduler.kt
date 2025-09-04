package com.habittracker.nudges.scheduler

import com.habittracker.domain.model.HabitStats
import com.habittracker.nudges.usecase.GenerateNudgesUseCase
import com.habittracker.nudges.usecase.HabitAnalysisData
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for automatically generating nudges based on habit patterns
 */
@Singleton
class NudgeScheduler @Inject constructor(
    private val generateNudgesUseCase: GenerateNudgesUseCase
) {
    
    private var schedulerJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Starts the nudge scheduler that generates nudges at specific times
     */
    fun startScheduler() {
        stopScheduler() // Stop any existing scheduler
        
        schedulerJob = coroutineScope.launch {
            while (isActive) {
                try {
                    // Generate nudges every 4 hours during active hours (8 AM to 10 PM)
                    val now = LocalDateTime.now()
                    val currentHour = now.hour
                    
                    if (currentHour in 8..22) { // Active hours
                        // This would normally get data from the habit repository
                        // For now, we'll use placeholder logic
                        generateScheduledNudges()
                    }
                    
                    // Wait 4 hours before next check
                    delay(TimeUnit.HOURS.toMillis(4))
                    
                } catch (e: Exception) {
                    // Log error but continue scheduling
                    delay(TimeUnit.MINUTES.toMillis(30)) // Wait 30 minutes on error
                }
            }
        }
    }
    
    /**
     * Stops the nudge scheduler
     */
    fun stopScheduler() {
        schedulerJob?.cancel()
        schedulerJob = null
    }
    
    /**
     * Generates nudges for all habits based on current patterns
     */
    suspend fun generateNudgesForAllHabits(habitsData: List<HabitAnalysisData>) {
        try {
            generateNudgesUseCase.generateNudges(habitsData)
        } catch (e: Exception) {
            // Handle error - in a real app, log this
        }
    }
    
    /**
     * Generates nudges for a specific habit
     */
    suspend fun generateNudgesForHabit(habitData: HabitAnalysisData) {
        try {
            generateNudgesUseCase.generateNudgesForHabit(habitData)
        } catch (e: Exception) {
            // Handle error - in a real app, log this
        }
    }
    
    /**
     * Generates nudges on schedule (placeholder implementation)
     */
    private suspend fun generateScheduledNudges() {
        // In a real implementation, this would:
        // 1. Get all active habits from repository
        // 2. Calculate their statistics
        // 3. Generate nudges based on patterns
        
        // For demonstration, we'll create sample data
        val sampleHabitsData = createSampleHabitsData()
        generateNudgesForAllHabits(sampleHabitsData)
    }
    
    /**
     * Creates sample habit data for demonstration
     * In a real app, this would come from the habit repository
     */
    private fun createSampleHabitsData(): List<HabitAnalysisData> {
        return listOf(
            HabitAnalysisData(
                habitId = 1L,
                habitName = "Morning Exercise",
                stats = HabitStats(
                    habitId = 1L,
                    totalCompletions = 15,
                    completionRate = 0.6,
                    averageStreakLength = 3.5,
                    longestStreak = 7,
                    currentStreak = 2,
                    completionsThisWeek = 3,
                    completionsThisMonth = 15
                ),
                lastCompletedDate = LocalDate.now().minusDays(2),
                isCompletedToday = false
            ),
            HabitAnalysisData(
                habitId = 2L,
                habitName = "Read 30 Minutes",
                stats = HabitStats(
                    habitId = 2L,
                    totalCompletions = 25,
                    completionRate = 0.8,
                    averageStreakLength = 5.2,
                    longestStreak = 12,
                    currentStreak = 8,
                    completionsThisWeek = 6,
                    completionsThisMonth = 25
                ),
                lastCompletedDate = LocalDate.now().minusDays(1),
                isCompletedToday = false
            )
        )
    }
    
    /**
     * Cancels all coroutines when the scheduler is destroyed
     */
    fun cleanup() {
        stopScheduler()
        coroutineScope.cancel()
    }
}
