package com.habittracker.analytics.domain.usecases

import com.habittracker.analytics.data.repository.AnalyticsRepository
import com.habittracker.analytics.domain.models.AnalyticsData
import com.habittracker.analytics.domain.models.TimeFrame
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving comprehensive analytics data with proper error handling
 */
class GetAnalyticsDataUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    
    suspend operator fun invoke(timeFrame: TimeFrame = TimeFrame.MONTHLY): Flow<AnalyticsData> {
        return analyticsRepository.getComprehensiveAnalytics(timeFrame)
    }
}