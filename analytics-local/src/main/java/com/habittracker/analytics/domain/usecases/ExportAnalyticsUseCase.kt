package com.habittracker.analytics.domain.usecases

import android.content.Context
import com.habittracker.analytics.data.repository.AnalyticsRepository
import com.habittracker.analytics.domain.models.ExportFormat
import com.habittracker.analytics.domain.models.TimeFrame
import com.habittracker.analytics.utils.AnalyticsExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for exporting analytics data in various formats with proper error handling
 */
class ExportAnalyticsUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val analyticsExporter: AnalyticsExporter,
    @ApplicationContext private val context: Context
) {
    
    suspend operator fun invoke(
        format: ExportFormat,
        timeFrame: TimeFrame = TimeFrame.MONTHLY
    ): String = withContext(Dispatchers.IO) {
        try {
            // Get comprehensive analytics data
            val analyticsData = analyticsRepository.getComprehensiveAnalytics(timeFrame).first()
            
            // Export using the analytics exporter
            analyticsExporter.exportAnalyticsData(
                context = context,
                analyticsData = analyticsData,
                format = format
            )
        } catch (e: Exception) {
            throw ExportException("Failed to export analytics: ${e.message}", e)
        }
    }
}

/**
 * Custom exception for export errors
 */
class ExportException(message: String, cause: Throwable? = null) : Exception(message, cause)