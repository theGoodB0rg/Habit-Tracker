package com.habittracker.analytics.domain

import com.habittracker.analytics.data.repository.AnalyticsRepository
import com.habittracker.analytics.domain.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AnalyticsTracker(
    private val repository: AnalyticsRepository,
    private val coroutineScope: CoroutineScope
) {

    fun trackHabitCompletion(habitId: String, habitName: String, isCompleted: Boolean) {
        coroutineScope.launch {
            repository.trackHabitCompletion(
                habitId = habitId, 
                habitName = habitName, 
                isCompleted = isCompleted
            )
        }
    }

    fun trackScreenVisit(screenName: String) {
        coroutineScope.launch {
            repository.trackScreenVisit(screenName)
        }
    }

    suspend fun getCompletionRate(timeFrame: TimeFrame): List<CompletionRate> {
        // Get comprehensive analytics and extract completion rates
        val analytics = repository.getComprehensiveAnalytics(timeFrame).first()
        return analytics.habitCompletionRates
    }

    suspend fun getScreenVisits(timeFrame: TimeFrame): List<ScreenVisit> {
        return repository.getScreenVisitData(timeFrame).first()
    }

    suspend fun getStreakRetention(timeFrame: TimeFrame): List<StreakRetention> {
        return repository.getStreakRetentionData(timeFrame).first()
    }

    suspend fun exportAnalyticsData(format: ExportFormat): String {
        return repository.exportAnalyticsData(format)
    }
}