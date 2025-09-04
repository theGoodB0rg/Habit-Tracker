package com.habittracker.nudges.engine

import com.habittracker.nudges.data.MotivationalQuotes
import com.habittracker.nudges.model.*
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core engine for generating behavioral nudges based on habit patterns and user behavior
 */
@Singleton
class NudgeEngine @Inject constructor() {
    
    private val nudgeHistory = mutableListOf<Nudge>()
    private var config = NudgeConfig()
    
    /**
     * Generates appropriate nudges based on the current context
     */
    fun generateNudges(contexts: List<NudgeContext>): List<Nudge> {
        val nudges = mutableListOf<Nudge>()
        
        // Check if we've hit the daily limit
        val todayNudges = getTodayNudges()
        if (todayNudges.size >= config.maxNudgesPerDay) {
            return emptyList()
        }
        
        contexts.forEach { context ->
            nudges.addAll(generateNudgesForHabit(context))
        }
        
        // Sort by priority and take only what fits in daily limit
        val remainingSlots = config.maxNudgesPerDay - todayNudges.size
        return nudges
            .sortedByDescending { it.priority.ordinal }
            .take(remainingSlots)
            .also { generatedNudges ->
                nudgeHistory.addAll(generatedNudges)
            }
    }
    
    /**
     * Generates nudges specific to a single habit's context
     */
    private fun generateNudgesForHabit(context: NudgeContext): List<Nudge> {
        val nudges = mutableListOf<Nudge>()
        
        // 1. Streak break warning
        if (shouldShowStreakWarning(context)) {
            nudges.add(createStreakWarningNudge(context))
        }
        
        // 2. Motivational quote for missed habits
        if (shouldShowMotivationalQuote(context)) {
            nudges.add(createMotivationalQuoteNudge(context))
        }
        
        // 3. Easier goal suggestion for struggling habits
        if (shouldSuggestEasierGoal(context)) {
            nudges.add(createEasierGoalSuggestionNudge(context))
        }
        
        // 4. Celebration for good streaks
        if (shouldShowCelebration(context)) {
            nudges.add(createCelebrationNudge(context))
        }
        
        return nudges
    }
    
    /**
     * Determines if a streak warning should be shown
     */
    private fun shouldShowStreakWarning(context: NudgeContext): Boolean {
        return config.enableStreakWarnings &&
                context.currentStreak >= config.streakWarningThreshold &&
                !context.isActiveToday &&
                context.daysSinceLastCompletion <= 1
    }
    
    /**
     * Determines if a motivational quote should be shown
     */
    private fun shouldShowMotivationalQuote(context: NudgeContext): Boolean {
        return config.enableMotivationalQuotes &&
                context.daysSinceLastCompletion >= 1 &&
                context.consecutiveMisses >= 1
    }
    
    /**
     * Determines if an easier goal suggestion should be shown
     */
    private fun shouldSuggestEasierGoal(context: NudgeContext): Boolean {
        return config.enableGoalSuggestions &&
                context.consecutiveMisses >= config.failureThreshold &&
                context.completionRate < 0.5
    }
    
    /**
     * Determines if a celebration should be shown
     */
    private fun shouldShowCelebration(context: NudgeContext): Boolean {
        return config.enableCelebrations &&
                context.isActiveToday &&
                (context.currentStreak % 7 == 0 || context.currentStreak == context.longestStreak)
    }
    
    /**
     * Creates a streak warning nudge
     */
    private fun createStreakWarningNudge(context: NudgeContext): Nudge {
        return Nudge(
            id = generateNudgeId(),
            type = NudgeType.STREAK_BREAK_WARNING,
            priority = NudgePriority.HIGH,
            title = "⚠️ Streak at Risk!",
            message = MotivationalQuotes.getRandomStreakWarning(context.currentStreak),
            actionText = "Complete Now",
            habitId = context.habitId,
            expiresAt = LocalDateTime.now().plusHours(12)
        )
    }
    
    /**
     * Creates a motivational quote nudge
     */
    private fun createMotivationalQuoteNudge(context: NudgeContext): Nudge {
        return Nudge(
            id = generateNudgeId(),
            type = NudgeType.MOTIVATIONAL_QUOTE,
            priority = NudgePriority.MEDIUM,
            title = "💪 Stay Motivated",
            message = if (context.consecutiveMisses >= 2) {
                MotivationalQuotes.getRandomEncouragementAfterMiss()
            } else {
                MotivationalQuotes.getRandomMotivationalQuote()
            },
            actionText = "Let's Go!",
            habitId = context.habitId,
            expiresAt = LocalDateTime.now().plusDays(1)
        )
    }
    
    /**
     * Creates an easier goal suggestion nudge
     */
    private fun createEasierGoalSuggestionNudge(context: NudgeContext): Nudge {
        return Nudge(
            id = generateNudgeId(),
            type = NudgeType.EASIER_GOAL_SUGGESTION,
            priority = NudgePriority.MEDIUM,
            title = "🎯 Try an Easier Approach",
            message = "Having trouble with '${context.habitName}'? ${MotivationalQuotes.getRandomGoalSuggestion()}",
            actionText = "Adjust Goal",
            habitId = context.habitId,
            expiresAt = LocalDateTime.now().plusDays(3)
        )
    }
    
    /**
     * Creates a celebration nudge
     */
    private fun createCelebrationNudge(context: NudgeContext): Nudge {
        val celebrationMessage = when {
            context.currentStreak == context.longestStreak && context.currentStreak > 7 -> 
                "🎉 New personal record! ${context.currentStreak} days strong!"
            context.currentStreak % 30 == 0 -> 
                "🏆 Amazing! ${context.currentStreak} days of dedication!"
            context.currentStreak % 7 == 0 -> 
                "⭐ Week ${context.currentStreak / 7} complete! ${MotivationalQuotes.getRandomCelebrationQuote()}"
            else -> MotivationalQuotes.getRandomCelebrationQuote()
        }
        
        return Nudge(
            id = generateNudgeId(),
            type = NudgeType.CELEBRATION,
            priority = NudgePriority.LOW,
            title = "🎊 Celebration Time!",
            message = celebrationMessage,
            actionText = "Keep Going!",
            habitId = context.habitId,
            expiresAt = LocalDateTime.now().plusHours(6)
        )
    }
    
    /**
     * Gets nudges created today
     */
    private fun getTodayNudges(): List<Nudge> {
        val today = LocalDateTime.now().toLocalDate()
        return nudgeHistory.filter { 
            it.createdAt.toLocalDate() == today && !it.isDismissed
        }
    }
    
    /**
     * Generates a unique nudge ID
     */
    private fun generateNudgeId(): String {
        return "nudge_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
    
    /**
     * Dismisses a nudge
     */
    fun dismissNudge(nudgeId: String) {
        nudgeHistory.find { it.id == nudgeId }?.let { nudge ->
            val index = nudgeHistory.indexOf(nudge)
            nudgeHistory[index] = nudge.copy(isDismissed = true)
        }
    }
    
    /**
     * Marks a nudge action as taken
     */
    fun markActionTaken(nudgeId: String) {
        nudgeHistory.find { it.id == nudgeId }?.let { nudge ->
            val index = nudgeHistory.indexOf(nudge)
            nudgeHistory[index] = nudge.copy(isActionTaken = true)
        }
    }
    
    /**
     * Updates the nudge configuration
     */
    fun updateConfig(newConfig: NudgeConfig) {
        this.config = newConfig
    }
    
    /**
     * Gets the current nudge configuration
     */
    fun getConfig(): NudgeConfig = config
    
    /**
     * Gets active nudges (not dismissed and not expired)
     */
    fun getActiveNudges(): List<Nudge> {
        val now = LocalDateTime.now()
        return nudgeHistory.filter { nudge ->
            !nudge.isDismissed && 
            (nudge.expiresAt == null || nudge.expiresAt.isAfter(now))
        }
    }
    
    /**
     * Clears old nudges from history (older than 30 days)
     */
    fun cleanupOldNudges() {
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        nudgeHistory.removeAll { it.createdAt.isBefore(thirtyDaysAgo) }
    }
}
