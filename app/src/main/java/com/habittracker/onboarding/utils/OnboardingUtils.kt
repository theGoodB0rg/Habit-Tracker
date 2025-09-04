package com.habittracker.onboarding.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.habittracker.onboarding.OnboardingPreferences
import com.habittracker.onboarding.manager.TooltipManager
import com.habittracker.onboarding.manager.rememberTooltipManager
import kotlinx.coroutines.delay

/**
 * Utility functions for onboarding flow management
 * 
 * @author Google-level Developer
 */
object OnboardingUtils {
    
    /**
     * Checks if the user should see onboarding based on various conditions
     */
    fun shouldShowOnboarding(
        preferences: OnboardingPreferences,
        isFirstLaunch: Boolean = preferences.isFirstLaunch(),
        forceShow: Boolean = false
    ): Boolean {
        return forceShow || isFirstLaunch || !preferences.isOnboardingCompleted()
    }
    
    /**
     * Determines the appropriate starting point for tooltips
     */
    fun getTooltipStartingPoint(
        preferences: OnboardingPreferences,
        hasHabits: Boolean
    ): String? {
        return when {
            !preferences.isOnboardingCompleted() -> null // Show onboarding first
            !hasHabits && preferences.shouldShowTooltip("add_habit_fab") -> "add_habit_fab"
            hasHabits && preferences.shouldShowTooltip("habit_complete_button") -> "habit_complete_button"
            else -> null
        }
    }
    
    /**
     * Marks onboarding milestone achievements
     */
    fun markMilestone(
        preferences: OnboardingPreferences,
        milestone: OnboardingMilestone
    ) {
        when (milestone) {
            OnboardingMilestone.FIRST_HABIT_CREATED -> {
                preferences.setTooltipShown("first_habit_created")
            }
            OnboardingMilestone.FIRST_HABIT_COMPLETED -> {
                preferences.setTooltipShown("first_habit_completed")
            }
            OnboardingMilestone.FIRST_STREAK_ACHIEVED -> {
                preferences.setTooltipShown("first_streak_achieved")
            }
            OnboardingMilestone.WEEK_STREAK_ACHIEVED -> {
                preferences.setTooltipShown("week_streak_achieved")
            }
        }
    }
    
    /**
     * Checks if a milestone has been achieved
     */
    fun hasMilestoneBeenAchieved(
        preferences: OnboardingPreferences,
        milestone: OnboardingMilestone
    ): Boolean {
        return when (milestone) {
            OnboardingMilestone.FIRST_HABIT_CREATED -> preferences.isTooltipShown("first_habit_created")
            OnboardingMilestone.FIRST_HABIT_COMPLETED -> preferences.isTooltipShown("first_habit_completed")
            OnboardingMilestone.FIRST_STREAK_ACHIEVED -> preferences.isTooltipShown("first_streak_achieved")
            OnboardingMilestone.WEEK_STREAK_ACHIEVED -> preferences.isTooltipShown("week_streak_achieved")
        }
    }
}

/**
 * Onboarding milestones that can be tracked
 */
enum class OnboardingMilestone {
    FIRST_HABIT_CREATED,
    FIRST_HABIT_COMPLETED,
    FIRST_STREAK_ACHIEVED,
    WEEK_STREAK_ACHIEVED
}

/**
 * Composable function to handle automatic tooltip triggering
 */
@Composable
fun AutoTooltipTrigger(
    tooltipId: String,
    condition: Boolean,
    delay: Long = 1000L,
    tooltipManager: TooltipManager = rememberTooltipManager()
) {
    LaunchedEffect(condition) {
        if (condition) {
            delay(delay)
            tooltipManager.showTooltip(tooltipId)
        }
    }
}

/**
 * Composable function for milestone-based tooltip triggering
 */
@Composable
fun MilestoneTooltipTrigger(
    milestone: OnboardingMilestone,
    condition: Boolean,
    tooltipId: String,
    preferences: OnboardingPreferences,
    delay: Long = 500L,
    tooltipManager: TooltipManager = rememberTooltipManager()
) {
    val hasAchieved = remember(milestone) {
        OnboardingUtils.hasMilestoneBeenAchieved(preferences, milestone)
    }
    
    LaunchedEffect(condition, hasAchieved) {
        if (condition && !hasAchieved) {
            OnboardingUtils.markMilestone(preferences, milestone)
            delay(delay)
            tooltipManager.showTooltip(tooltipId)
        }
    }
}

/**
 * Extension functions for easy onboarding integration
 */
fun OnboardingPreferences.shouldShowTooltip(tooltipId: String): Boolean {
    return !this.isTooltipShown(tooltipId)
}

fun OnboardingPreferences.markTooltipShown(tooltipId: String) {
    this.setTooltipShown(tooltipId)
}
