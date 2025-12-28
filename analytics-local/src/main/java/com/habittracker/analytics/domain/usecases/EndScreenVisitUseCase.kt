package com.habittracker.analytics.domain.usecases

import com.habittracker.analytics.data.repository.AnalyticsRepository
import javax.inject.Inject

class EndScreenVisitUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    suspend operator fun invoke(interactionCount: Int = 0, bounced: Boolean = false) {
        analyticsRepository.endScreenVisit(interactionCount, bounced)
    }
}
