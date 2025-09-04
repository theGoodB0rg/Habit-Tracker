package com.habittracker.analytics.domain.usecases

import com.habittracker.analytics.data.repository.AnalyticsRepository
import com.habittracker.analytics.domain.models.DifficultyLevel
import javax.inject.Inject

/**
 * Comprehensive tracking use cases for analytics
 */
class TrackingUseCases @Inject constructor(
    val trackHabitCompletionUseCase: TrackHabitCompletionUseCase,
    val trackScreenVisitUseCase: TrackScreenVisitUseCase,
    val trackStreakRetentionUseCase: TrackStreakRetentionUseCase,
    val trackTimerEventUseCase: TrackTimerEventUseCase
)

/**
 * Use case for tracking habit completion with comprehensive metrics
 */
class TrackHabitCompletionUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    suspend operator fun invoke(
        habitId: String,
        habitName: String,
        isCompleted: Boolean,
        timeSpentMinutes: Int = 0,
        difficultyLevel: DifficultyLevel = DifficultyLevel.MODERATE
    ) {
        analyticsRepository.trackHabitCompletion(
            habitId = habitId,
            habitName = habitName,
            isCompleted = isCompleted,
            timeSpentMinutes = timeSpentMinutes,
            difficultyLevel = difficultyLevel
        )
    }
}

/**
 * Use case for tracking screen visits with navigation flow
 */
class TrackScreenVisitUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    suspend operator fun invoke(
        screenName: String,
        fromScreen: String? = null
    ) {
        analyticsRepository.trackScreenVisit(
            screenName = screenName,
            fromScreen = fromScreen
        )
    }
    
    suspend fun endVisit(interactionCount: Int = 0, bounced: Boolean = false) {
        analyticsRepository.endScreenVisit(
            interactionCount = interactionCount,
            bounced = bounced
        )
    }
}

/**
 * Use case for tracking streak retention (handled internally by repository)
 */
class TrackStreakRetentionUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    // This is handled automatically by the habit completion tracking
    // but can be used for manual streak updates if needed
    
    suspend operator fun invoke(
        habitId: String,
        streakLength: Int
    ) {
        // Manual streak tracking if needed
        // Implementation would depend on specific requirements
    }
}

/**
 * Use case for tracking generic timer events
 */
class TrackTimerEventUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    suspend operator fun invoke(
        eventType: String,
        habitId: Long? = null,
        sessionId: Long? = null,
        source: String? = null,
        extra: Map<String, Any?> = emptyMap()
    ) {
        analyticsRepository.trackTimerEvent(eventType, habitId, sessionId, source, extra)
    }
}
