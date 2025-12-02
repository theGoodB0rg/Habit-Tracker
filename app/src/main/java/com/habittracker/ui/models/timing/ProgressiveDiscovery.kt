package com.habittracker.ui.models.timing

import com.habittracker.ui.models.timing.ReminderStyle

/**
 * Phase 2: Progressive Feature Discovery System
 * 
 * Tracks user engagement to gradually introduce timing features
 * based on actual usage patterns rather than overwhelming users
 */

// User Engagement Levels for Progressive Disclosure
sealed class UserEngagementLevel {
    object Casual : UserEngagementLevel()           // Just wants to track habits
    object Interested : UserEngagementLevel()       // Uses basic timer
    object Engaged : UserEngagementLevel()          // Uses scheduling
    object PowerUser : UserEngagementLevel()        // Uses all features
}

// Features available at each level
enum class Feature {
    BASIC_TRACKING,           // Core habit tracking (always available)
    SIMPLE_TIMER,             // One-tap timer
    SMART_SUGGESTIONS,        // AI-powered recommendations
    SCHEDULE_OPTIMIZATION,    // Timing optimization
    CONTEXT_AWARENESS,        // Environment-based suggestions
    HABIT_STACKING,          // Combination recommendations
    ADVANCED_ANALYTICS,       // Detailed metrics
    ENERGY_OPTIMIZATION,      // Circadian rhythm features
    FOCUS_ENHANCEMENT,        // Background sounds, distraction blocking
    CALENDAR_INTEGRATION      // External calendar sync
}

// Timing Complexity Preferences
enum class TimingComplexityLevel {
    BASIC,          // Just basic timers
    INTERMEDIATE,   // + Smart suggestions
    ADVANCED,       // + Context awareness
    POWER_USER      // All features
}

// Feature Gradualizer - Controls progressive disclosure
class FeatureGradualizer {
    
    fun getAvailableFeatures(engagementLevel: UserEngagementLevel): List<Feature> {
        return when (engagementLevel) {
            UserEngagementLevel.Casual -> listOf(
                Feature.BASIC_TRACKING
            )
            
            UserEngagementLevel.Interested -> listOf(
                Feature.BASIC_TRACKING,
                Feature.SIMPLE_TIMER
            )
            
            UserEngagementLevel.Engaged -> listOf(
                Feature.BASIC_TRACKING,
                Feature.SIMPLE_TIMER,
                Feature.SMART_SUGGESTIONS,
                Feature.SCHEDULE_OPTIMIZATION
            )
            
            UserEngagementLevel.PowerUser -> Feature.values().toList()
        }
    }
    
    fun shouldLevelUp(
        currentLevel: UserEngagementLevel,
        userBehavior: UserBehaviorMetrics
    ): Boolean {
        return when (currentLevel) {
            UserEngagementLevel.Casual -> {
                // Level up if user consistently tracks habits for 1+ weeks
                userBehavior.consistentTrackingDays >= 7 && 
                userBehavior.habitCompletionRate > 0.6f
            }
            
            UserEngagementLevel.Interested -> {
                // Level up if user actively uses timers
                userBehavior.timerUsageCount >= 5 &&
                userBehavior.timerCompletionRate > 0.7f
            }
            
            UserEngagementLevel.Engaged -> {
                // Level up if user engages with suggestions and optimization
                userBehavior.suggestionAcceptanceRate > 0.4f &&
                userBehavior.schedulingInteractions >= 3
            }
            
            UserEngagementLevel.PowerUser -> false // Already at max level
        }
    }
    
    fun getNextLevelBenefits(currentLevel: UserEngagementLevel): List<String> {
        return when (currentLevel) {
            UserEngagementLevel.Casual -> listOf(
                "🎯 One-tap timers to stay focused",
                "⏰ Track how long habits actually take",
                "📈 See your productivity patterns"
            )
            
            UserEngagementLevel.Interested -> listOf(
                "💡 AI suggestions for optimal timing",
                "📅 Smart schedule optimization",
                "🔗 Habit stacking recommendations"
            )
            
            UserEngagementLevel.Engaged -> listOf(
                "🧠 Advanced energy optimization",
                "🌤️ Context-aware suggestions",
                "📊 Detailed analytics and insights",
                "🎵 Focus enhancement features"
            )
            
            UserEngagementLevel.PowerUser -> emptyList()
        }
    }
}

// User Behavior Tracking for Level Progression
data class UserBehaviorMetrics(
    val consistentTrackingDays: Int = 0,
    val habitCompletionRate: Float = 0f,
    val timerUsageCount: Int = 0,
    val timerCompletionRate: Float = 0f,
    val suggestionAcceptanceRate: Float = 0f,
    val schedulingInteractions: Int = 0,
    val advancedFeatureUsage: Map<Feature, Int> = emptyMap(),
    val lastEngagementDate: Long = System.currentTimeMillis(),
    // Phase 10: Progressive Discovery completeness
    val featureFirstSeen: Map<Feature, Long> = emptyMap(),
    val featureFirstUsed: Map<Feature, Long> = emptyMap(),
    val levelUpEvents: List<LevelUpEvent> = emptyList()
)

// User Preferences for Timing Features
data class SmartTimingPreferences(
    val enableTimers: Boolean = false,
    val enableSmartSuggestions: Boolean = false,
    val enableContextAwareness: Boolean = false,
    val enableHabitStacking: Boolean = false,
    val complexityLevel: TimingComplexityLevel = TimingComplexityLevel.BASIC,
    val timerDefaultDuration: java.time.Duration = java.time.Duration.ofMinutes(25),
    val preferredReminderStyle: ReminderStyle = ReminderStyle.GENTLE,
    val autoLevelUp: Boolean = true,  // Allow automatic feature discovery
    val showLevelUpPrompts: Boolean = true,  // Show benefits of next level
    val currentEngagementLevel: UserEngagementLevel = UserEngagementLevel.Casual,
    val askToCompleteWithoutTimer: Boolean = true // Ask for confirmation when completing without timer
)

// Level Up Notification Data
data class LevelUpNotification(
    val fromLevel: UserEngagementLevel,
    val toLevel: UserEngagementLevel,
    val unlockedFeatures: List<Feature>,
    val benefits: List<String>,
    val congratsMessage: String
) {
    companion object {
        fun create(from: UserEngagementLevel, to: UserEngagementLevel): LevelUpNotification {
            val gradualizer = FeatureGradualizer()
            val newFeatures = gradualizer.getAvailableFeatures(to) - 
                             gradualizer.getAvailableFeatures(from).toSet()
            
            return LevelUpNotification(
                fromLevel = from,
                toLevel = to,
                unlockedFeatures = newFeatures,
                benefits = gradualizer.getNextLevelBenefits(from),
                congratsMessage = generateCongratsMessage(from, to)
            )
        }
        
        private fun generateCongratsMessage(from: UserEngagementLevel, to: UserEngagementLevel): String {
            return when (to) {
                UserEngagementLevel.Interested -> 
                    "🎉 You're building consistent habits! Unlock timing features to boost your productivity."
                    
                UserEngagementLevel.Engaged -> 
                    "🚀 You're a timing pro! Unlock smart suggestions and schedule optimization."
                    
                UserEngagementLevel.PowerUser -> 
                    "⭐ Welcome to Power User status! All advanced features are now available."
                    
                UserEngagementLevel.Casual -> ""
            }
        }
    }
}

// Feature Discovery Analytics
data class FeatureDiscoveryAnalytics(
    val featureFirstSeen: Map<Feature, Long> = emptyMap(),
    val featureFirstUsed: Map<Feature, Long> = emptyMap(),
    val featureUsageCount: Map<Feature, Int> = emptyMap(),
    val levelUpEvents: List<LevelUpEvent> = emptyList(),
    val featureAbandonmentRate: Map<Feature, Float> = emptyMap()
)

data class LevelUpEvent(
    val timestamp: Long,
    val fromLevel: UserEngagementLevel,
    val toLevel: UserEngagementLevel,
    val triggerMetric: String,  // What caused the level up
    val userAccepted: Boolean   // Did user accept the level up?
)
