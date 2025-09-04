package com.habittracker.domain.engine

import com.habittracker.data.database.entity.HabitFrequency
import com.habittracker.domain.model.HabitStreak
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Professional-grade streak calculation engine
 * Handles complex streak logic with grace periods and frequency considerations
 */
@Singleton
class StreakCalculationEngine @Inject constructor() {
    
    /**
     * Calculate current streak for a habit based on completion history
     */
    fun calculateCurrentStreak(
        completionDates: List<LocalDate>,
        frequency: HabitFrequency,
        today: LocalDate = LocalDate.now()
    ): Int {
        if (completionDates.isEmpty()) return 0
        
        val sortedDates = completionDates.sorted().reversed() // Most recent first
        val expectedInterval = getExpectedInterval(frequency)
        
        var currentStreak = 0
        
        // Check if today is completed (or within grace period)
        val lastCompletion = sortedDates.first()
        val daysSinceLastCompletion = ChronoUnit.DAYS.between(lastCompletion, today).toInt()
        
        // Grace period: allow 1 day for daily habits, proportional for others
        val gracePeriod = when (frequency) {
            HabitFrequency.DAILY -> 1
            HabitFrequency.WEEKLY -> 2
            HabitFrequency.MONTHLY -> 7
        }
        
        if (daysSinceLastCompletion > expectedInterval + gracePeriod) {
            return 0 // Streak is broken beyond grace period
        }
        
        // Calculate streak by working backwards
        var expectedDate = today
        for (completionDate in sortedDates) {
            val daysDifference = ChronoUnit.DAYS.between(completionDate, expectedDate).toInt()
            
            when {
                daysDifference == 0 -> {
                    // Completed on expected date
                    currentStreak++
                    expectedDate = completionDate.minusDays(expectedInterval.toLong())
                }
                daysDifference <= gracePeriod -> {
                    // Within grace period
                    currentStreak++
                    expectedDate = completionDate.minusDays(expectedInterval.toLong())
                }
                else -> {
                    // Gap is too large, streak ends
                    break
                }
            }
        }
        
        return currentStreak
    }
    
    /**
     * Calculate the longest streak from completion history
     */
    fun calculateLongestStreak(
        completionDates: List<LocalDate>,
        frequency: HabitFrequency
    ): Int {
        if (completionDates.isEmpty()) return 0
        
        val sortedDates = completionDates.sorted()
        val expectedInterval = getExpectedInterval(frequency)
        
        var longestStreak = 1
        var currentStreak = 1
        
        for (i in 1 until sortedDates.size) {
            val daysBetween = ChronoUnit.DAYS.between(sortedDates[i-1], sortedDates[i]).toInt()
            
            if (daysBetween <= expectedInterval + 1) { // Allow 1 day grace
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return longestStreak
    }
    
    /**
     * Determine if streak is at risk (missed expected completion)
     */
    fun isStreakAtRisk(
        lastCompletionDate: LocalDate?,
        frequency: HabitFrequency,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        if (lastCompletionDate == null) return true
        
        val expectedInterval = getExpectedInterval(frequency)
        val daysSinceCompletion = ChronoUnit.DAYS.between(lastCompletionDate, today).toInt()
        
        return daysSinceCompletion > expectedInterval
    }
    
    /**
     * Get the next expected completion date
     */
    fun getNextExpectedDate(
        lastCompletionDate: LocalDate,
        frequency: HabitFrequency,
        backwards: Boolean = false
    ): LocalDate {
        val days = getExpectedInterval(frequency)
        return if (backwards) {
            lastCompletionDate.minusDays(days.toLong())
        } else {
            lastCompletionDate.plusDays(days.toLong())
        }
    }
    
    /**
     * Calculate completion rate for a given period
     */
    fun calculateCompletionRate(
        completionDates: List<LocalDate>,
        frequency: HabitFrequency,
        startDate: LocalDate,
        endDate: LocalDate
    ): Double {
        val expectedCompletions = calculateExpectedCompletions(frequency, startDate, endDate)
        val actualCompletions = completionDates.count { it in startDate..endDate }
        
        return if (expectedCompletions > 0) {
            minOf(1.0, actualCompletions.toDouble() / expectedCompletions)
        } else {
            0.0
        }
    }
    
    private fun getExpectedInterval(frequency: HabitFrequency): Int {
        return when (frequency) {
            HabitFrequency.DAILY -> 1
            HabitFrequency.WEEKLY -> 7
            HabitFrequency.MONTHLY -> 30
        }
    }
    
    private fun calculateExpectedCompletions(
        frequency: HabitFrequency,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        
        return when (frequency) {
            HabitFrequency.DAILY -> totalDays
            HabitFrequency.WEEKLY -> totalDays / 7
            HabitFrequency.MONTHLY -> totalDays / 30
        }
    }
}
