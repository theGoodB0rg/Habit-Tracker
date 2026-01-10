package com.habittracker.onboarding.model

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data models for the onboarding flow
 * 
 * @author Google-level Developer
 */

/**
 * Represents a single onboarding slide
 */
data class OnboardingSlide(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val backgroundGradientColors: List<androidx.compose.ui.graphics.Color> = emptyList(),
    val animationDelay: Long = 0L
)

/**
 * Represents the complete onboarding flow state
 */
data class OnboardingState(
    val currentSlide: Int = 0,
    val totalSlides: Int = 0,
    val isLoading: Boolean = false,
    val canProceed: Boolean = true,
    val slides: List<OnboardingSlide> = emptyList(),
    val error: String? = null
)

/**
 * Onboarding events that can be triggered
 */
sealed class OnboardingEvent {
    object NextSlide : OnboardingEvent()
    object PreviousSlide : OnboardingEvent()
    object SkipOnboarding : OnboardingEvent()
    object CompleteOnboarding : OnboardingEvent()
    data class GoToSlide(val index: Int) : OnboardingEvent()
}

/**
 * Tooltip configuration for in-app guidance
 */
data class TooltipConfig(
    val id: String,
    val title: String,
    val description: String,
    val targetComposableKey: String,
    val position: TooltipPosition = TooltipPosition.BOTTOM,
    val highlightColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Yellow.copy(alpha = 0.3f),
    val showOnlyOnce: Boolean = true,
    val highlightArea: androidx.compose.ui.geometry.Rect? = null,
    val primaryAction: (() -> Unit)? = null,
    val primaryActionText: String? = null,
    val secondaryAction: (() -> Unit)? = null,
    val secondaryActionText: String? = null
)

/**
 * Tooltip positioning options
 */
enum class TooltipPosition {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

/**
 * Predefined onboarding slides
 */
object OnboardingSlides {
    
    val welcomeSlide = OnboardingSlide(
        id = "welcome",
        title = "Welcome to Habit Tracker",
        subtitle = "Your Personal Growth Companion",
        description = "Transform your life one habit at a time. Build consistency, track progress, and achieve your goals with our powerful yet simple habit tracking system.",
        icon = Icons.Default.Favorite,
        backgroundGradientColors = listOf(
            androidx.compose.ui.graphics.Color(0xFF6366F1),
            androidx.compose.ui.graphics.Color(0xFF8B5CF6)
        )
    )
    
    val habitsExplainedSlide = OnboardingSlide(
        id = "habits_explained",
        title = "What is a Habit?",
        subtitle = "The Building Blocks of Success",
        description = "A habit is a routine behavior performed regularly and automatically. Research shows it takes 21-66 days to form a new habit. Start small, stay consistent, and watch your life transform.",
        icon = Icons.Default.Psychology,
        backgroundGradientColors = listOf(
            androidx.compose.ui.graphics.Color(0xFF10B981),
            androidx.compose.ui.graphics.Color(0xFF06B6D4)
        ),
        animationDelay = 200L
    )
    
    val howToUseSlide = OnboardingSlide(
        id = "how_to_use",
        title = "How to Use the App",
        subtitle = "Simple Steps to Success",
        description = "1. Add your habits with custom icons\n2. Mark them complete each day\n3. Build streaks and stay motivated\n4. Review your progress over time\n5. Celebrate your achievements!",
        icon = Icons.Default.CheckCircle,
        backgroundGradientColors = listOf(
            androidx.compose.ui.graphics.Color(0xFFF59E0B),
            androidx.compose.ui.graphics.Color(0xFFEF4444)
        ),
        animationDelay = 400L
    )
    
    val streakBenefitsSlide = OnboardingSlide(
        id = "streak_benefits",
        title = "Benefits of Streaks",
        subtitle = "Consistency is Key",
        description = "Streaks create momentum and motivation. Each consecutive day builds stronger neural pathways. You'll receive gentle reminders and motivational nudges to keep you on track.",
        icon = Icons.Default.TrendingUp,
        backgroundGradientColors = listOf(
            androidx.compose.ui.graphics.Color(0xFFEC4899),
            androidx.compose.ui.graphics.Color(0xFF8B5CF6)
        ),
        animationDelay = 600L
    )
    
    val privacySlide = OnboardingSlide(
        id = "privacy",
        title = "Your Privacy Matters",
        subtitle = "100% Offline & Secure",
        description = "All your data stays on your device. No accounts, no tracking, no data collection. Your habits and progress are completely private and secure.",
        icon = Icons.Default.Security,
        backgroundGradientColors = listOf(
            androidx.compose.ui.graphics.Color(0xFF059669),
            androidx.compose.ui.graphics.Color(0xFF0891B2)
        ),
        animationDelay = 800L
    )
    
    val readyToStartSlide = OnboardingSlide(
        id = "ready_to_start",
        title = "Ready to Start?",
        subtitle = "Your Journey Begins Now",
        description = "You're all set to begin building amazing habits! Remember: progress, not perfection. Start with one small habit and build from there.",
        icon = Icons.Default.Rocket,
        backgroundGradientColors = listOf(
            androidx.compose.ui.graphics.Color(0xFF7C3AED),
            androidx.compose.ui.graphics.Color(0xFF3B82F6)
        ),
        animationDelay = 1000L
    )
    
    fun getAllSlides(): List<OnboardingSlide> = listOf(
        welcomeSlide,
        habitsExplainedSlide,
        howToUseSlide,
        streakBenefitsSlide,
        privacySlide,
        readyToStartSlide
    )
}

/**
 * Predefined tooltips for in-app guidance
 */
object AppTooltips {
    
    val addHabitTooltip = TooltipConfig(
        id = "add_habit_fab",
        title = "Add Your First Habit",
        description = "Tap here to create your first habit. Start with something small and achievable!",
        targetComposableKey = "add_habit_fab",
        position = TooltipPosition.TOP
    )
    
    val markHabitCompleteTooltip = TooltipConfig(
        id = "mark_habit_complete",
        title = "Mark as Complete",
        description = "Tap the circle to mark your habit as complete for today. Watch your streak grow!",
        targetComposableKey = "habit_complete_button",
        position = TooltipPosition.LEFT
    )
    
    val habitDetailTooltip = TooltipConfig(
        id = "habit_detail",
        title = "View Details",
        description = "Tap on a habit to see your streak history and detailed progress.",
        targetComposableKey = "habit_card",
        position = TooltipPosition.BOTTOM
    )
    
    val settingsTooltip = TooltipConfig(
        id = "settings_menu",
        title = "Settings & More",
        description = "Access themes, reminders, and other settings from the menu.",
        targetComposableKey = "settings_menu",
        position = TooltipPosition.BOTTOM
    )
    
    val streakCounterTooltip = TooltipConfig(
        id = "streak_counter",
        title = "Your Streak",
        description = "This shows how many days in a row you've completed this habit.",
        targetComposableKey = "streak_counter",
        position = TooltipPosition.TOP
    )
    
    // Progressive timing feature discovery tooltips
    val timerButtonTooltip = TooltipConfig(
        id = "timer_button",
        title = "Timer Unlocked!",
        description = "You've unlocked habit timers! Tap to start a focused session with countdown alerts.",
        targetComposableKey = "timer_start_button",
        position = TooltipPosition.TOP
    )
    
    val smartSuggestionTooltip = TooltipConfig(
        id = "smart_suggestion",
        title = "Smart Suggestions",
        description = "Based on your patterns, we suggest optimal times for this habit. Tap to accept!",
        targetComposableKey = "smart_suggestion_chip",
        position = TooltipPosition.BOTTOM
    )
    
    val miniSessionBarTooltip = TooltipConfig(
        id = "mini_session_bar",
        title = "Active Timer",
        description = "Your timer is running! Tap to view full controls or complete your session.",
        targetComposableKey = "mini_session_bar",
        position = TooltipPosition.TOP
    )
    
    val timerControlsTooltip = TooltipConfig(
        id = "timer_controls",
        title = "Timer Controls",
        description = "Pause, extend, or complete your timer from here. You control the session!",
        targetComposableKey = "timer_control_sheet",
        position = TooltipPosition.TOP
    )
    
    fun getAllTooltips(): List<TooltipConfig> = listOf(
    addHabitTooltip,
        markHabitCompleteTooltip,
        habitDetailTooltip,
        streakCounterTooltip,
        settingsTooltip
    )
    
    /**
     * Tooltips for progressive timing feature discovery.
     * These are shown when users unlock new engagement levels.
     */
    fun getTimingFeatureTooltips(): List<TooltipConfig> = listOf(
        timerButtonTooltip,
        smartSuggestionTooltip,
        miniSessionBarTooltip,
        timerControlsTooltip
    )
}
