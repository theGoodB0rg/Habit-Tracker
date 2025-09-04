package com.habittracker.nudges.analyzer

import com.habittracker.domain.model.HabitStats
import com.habittracker.nudges.model.HabitDifficulty
import com.habittracker.nudges.model.NudgeContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes habit patterns to generate context for nudge generation
 */
@Singleton
class HabitPatternAnalyzer @Inject constructor() {
    
    /**
     * Analyzes a habit's pattern and creates context for nudge generation
     */
    fun analyzeHabitPattern(
        habitId: Long,
        habitName: String,
        stats: HabitStats,
        lastCompletedDate: LocalDate?,
        isCompletedToday: Boolean
    ): NudgeContext {
        
        val daysSinceLastCompletion = calculateDaysSinceLastCompletion(lastCompletedDate)
        val consecutiveMisses = calculateConsecutiveMisses(stats, lastCompletedDate)
        val difficulty = analyzeDifficulty(stats)
        
        return NudgeContext(
            habitId = habitId,
            habitName = habitName,
            currentStreak = stats.currentStreak,
            longestStreak = stats.longestStreak,
            daysSinceLastCompletion = daysSinceLastCompletion,
            completionRate = stats.completionRate,
            consecutiveMisses = consecutiveMisses,
            isActiveToday = isCompletedToday,
            difficulty = difficulty
        )
    }
    
    /**
     * Calculates days since last completion
     */
    private fun calculateDaysSinceLastCompletion(lastCompletedDate: LocalDate?): Int {
        return if (lastCompletedDate != null) {
            ChronoUnit.DAYS.between(lastCompletedDate, LocalDate.now()).toInt()
        } else {
            Int.MAX_VALUE // Never completed
        }
    }
    
    /**
     * Calculates consecutive missed days with enhanced analysis using habit statistics
     */
    private fun calculateConsecutiveMisses(stats: HabitStats, lastCompletedDate: LocalDate?): Int {
        val daysSinceCompletion = calculateDaysSinceLastCompletion(lastCompletedDate)
        
        // If completed today, no consecutive misses
        if (daysSinceCompletion == 0) return 0
        
        // Use stats to provide more accurate consecutive miss calculation
        val baseConsecutiveMisses = daysSinceCompletion
        
        // If completion rate is very low and we haven't completed recently, 
        // it's likely there have been more consecutive misses
        return if (stats.completionRate < 0.3 && daysSinceCompletion > 1) {
            // For very low completion rates, assume longer consecutive miss patterns
            minOf(baseConsecutiveMisses + 1, 7) // Cap at 7 days for practical purposes
        } else {
            baseConsecutiveMisses
        }
    }
    
    /**
     * Analyzes habit difficulty based on completion patterns
     */
    private fun analyzeDifficulty(stats: HabitStats): HabitDifficulty {
        return when {
            stats.completionRate >= 0.9 -> HabitDifficulty.VERY_EASY
            stats.completionRate >= 0.7 -> HabitDifficulty.EASY
            stats.completionRate >= 0.5 -> HabitDifficulty.MEDIUM
            stats.completionRate >= 0.3 -> HabitDifficulty.HARD
            else -> HabitDifficulty.VERY_HARD
        }
    }
    
    /**
     * Determines if a habit is at risk of breaking a significant streak
     */
    fun isStreakAtRisk(context: NudgeContext): Boolean {
        return context.currentStreak >= 3 && 
               context.daysSinceLastCompletion >= 1 && 
               !context.isActiveToday
    }
    
    /**
     * Determines if a habit is struggling (multiple consecutive misses)
     */
    fun isHabitStruggling(context: NudgeContext): Boolean {
        return context.consecutiveMisses >= 3 || 
               (context.completionRate < 0.5 && context.consecutiveMisses >= 2)
    }
    
    /**
     * Determines if a habit deserves celebration
     */
    fun deservesCelebration(context: NudgeContext): Boolean {
        return context.isActiveToday && (
            context.currentStreak % 7 == 0 || // Weekly milestone
            context.currentStreak == context.longestStreak || // New record
            context.currentStreak % 30 == 0 // Monthly milestone
        )
    }
    
    /**
     * Calculates habit momentum (trending up, down, or stable)
     */
    fun calculateMomentum(stats: HabitStats): HabitMomentum {
        return when {
            stats.currentStreak > stats.averageStreakLength * 1.5 -> HabitMomentum.STRONG_POSITIVE
            stats.currentStreak > stats.averageStreakLength -> HabitMomentum.POSITIVE
            stats.currentStreak == 0 && stats.completionRate < 0.3 -> HabitMomentum.NEGATIVE
            stats.currentStreak < stats.averageStreakLength * 0.5 -> HabitMomentum.WEAK_NEGATIVE
            else -> HabitMomentum.STABLE
        }
    }
}

/**
 * Represents the momentum direction of a habit
 */
enum class HabitMomentum {
    STRONG_POSITIVE,
    POSITIVE,
    STABLE,
    WEAK_NEGATIVE,
    NEGATIVE
}
