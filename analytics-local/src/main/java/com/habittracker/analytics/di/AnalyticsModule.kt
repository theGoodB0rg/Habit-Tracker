package com.habittracker.analytics.di

import android.content.Context
import com.habittracker.analytics.data.database.AnalyticsDao
import com.habittracker.analytics.data.database.AnalyticsDatabase
import com.habittracker.analytics.data.initialization.AnalyticsDataInitializer
import com.habittracker.analytics.data.repository.AnalyticsRepository
import com.habittracker.analytics.data.repository.AnalyticsRepositoryImpl
import com.habittracker.analytics.domain.usecases.*
import com.habittracker.analytics.utils.AnalyticsExporter
import com.habittracker.analytics.utils.DateUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Comprehensive Dagger Hilt module for analytics dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsDatabase(@ApplicationContext context: Context): AnalyticsDatabase {
        return AnalyticsDatabase.getDatabase(context)
    }

    @Provides
    fun provideAnalyticsDao(database: AnalyticsDatabase): AnalyticsDao {
        return database.analyticsDao()
    }

    @Provides
    @Singleton
    fun provideDateUtils(): DateUtils {
        return DateUtils()
    }

    @Provides
    @Singleton
    fun provideAnalyticsExporter(): AnalyticsExporter {
        return AnalyticsExporter()
    }

    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        analyticsDao: AnalyticsDao,
        dateUtils: DateUtils
    ): AnalyticsRepository {
        return AnalyticsRepositoryImpl(analyticsDao, dateUtils)
    }

    @Provides
    @Singleton
    fun provideGetAnalyticsDataUseCase(
        analyticsRepository: AnalyticsRepository
    ): GetAnalyticsDataUseCase {
        return GetAnalyticsDataUseCase(analyticsRepository)
    }

    @Provides
    @Singleton
    fun provideExportAnalyticsUseCase(
        analyticsRepository: AnalyticsRepository,
        analyticsExporter: AnalyticsExporter,
        @ApplicationContext context: Context
    ): ExportAnalyticsUseCase {
        return ExportAnalyticsUseCase(analyticsRepository, analyticsExporter, context)
    }

    @Provides
    @Singleton
    fun provideTrackHabitCompletionUseCase(
        analyticsRepository: AnalyticsRepository
    ): TrackHabitCompletionUseCase {
        return TrackHabitCompletionUseCase(analyticsRepository)
    }

    @Provides
    @Singleton
    fun provideTrackScreenVisitUseCase(
        analyticsRepository: AnalyticsRepository
    ): TrackScreenVisitUseCase {
        return TrackScreenVisitUseCase(analyticsRepository)
    }

    @Provides
    @Singleton
    fun provideTrackStreakRetentionUseCase(
        analyticsRepository: AnalyticsRepository
    ): TrackStreakRetentionUseCase {
        return TrackStreakRetentionUseCase(analyticsRepository)
    }

    @Provides
    @Singleton
    fun provideTrackingUseCases(
        trackHabitCompletionUseCase: TrackHabitCompletionUseCase,
        trackScreenVisitUseCase: TrackScreenVisitUseCase,
        trackStreakRetentionUseCase: TrackStreakRetentionUseCase,
        trackTimerEventUseCase: TrackTimerEventUseCase
    ): TrackingUseCases {
        return TrackingUseCases(
            trackHabitCompletionUseCase,
            trackScreenVisitUseCase,
            trackStreakRetentionUseCase,
            trackTimerEventUseCase
        )
    }

    @Provides
    @Singleton
    fun provideTrackTimerEventUseCase(
        analyticsRepository: AnalyticsRepository
    ): TrackTimerEventUseCase {
        return TrackTimerEventUseCase(analyticsRepository)
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsDataInitializer(
        analyticsDao: AnalyticsDao,
        dateUtils: DateUtils
    ): AnalyticsDataInitializer {
        return AnalyticsDataInitializer(analyticsDao, dateUtils)
    }
}