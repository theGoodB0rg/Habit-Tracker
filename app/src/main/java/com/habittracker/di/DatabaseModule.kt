package com.habittracker.di

import android.content.Context
import androidx.room.Room
import com.habittracker.data.database.HabitDatabase
import com.habittracker.data.database.dao.HabitDao
import com.habittracker.data.database.dao.HabitCompletionDao
import com.habittracker.data.database.dao.timing.*
import com.habittracker.data.repository.HabitRepository
import com.habittracker.data.repository.HabitRepositoryImpl
import com.habittracker.data.repository.timing.TimingRepository
import com.habittracker.data.repository.timing.TimingRepositoryImpl
import com.habittracker.domain.engine.HabitManagementEngine
import com.habittracker.domain.engine.StreakCalculationEngine
import com.habittracker.timing.suggestions.PatternSuggestionEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database-related dependencies.
 * Enhanced for Phase 1 with smart timing capabilities and habit engine dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideHabitDatabase(@ApplicationContext context: Context): HabitDatabase {
    // Use the core singleton to keep migration behavior consistent across app and widgets
    return HabitDatabase.getDatabase(context)
    }
    
    // Original DAOs
    @Provides
    fun provideHabitDao(database: HabitDatabase): HabitDao {
        return database.habitDao()
    }
    
    @Provides
    fun provideHabitCompletionDao(database: HabitDatabase): HabitCompletionDao {
        return database.habitCompletionDao()
    }
    
    // Phase 1 - Smart Timing Enhancement DAOs
    @Provides
    fun provideHabitTimingDao(database: HabitDatabase): HabitTimingDao {
        return database.habitTimingDao()
    }
    
    @Provides
    fun provideTimerSessionDao(database: HabitDatabase): TimerSessionDao {
        return database.timerSessionDao()
    }
    
    @Provides
    fun provideSmartSuggestionDao(database: HabitDatabase): SmartSuggestionDao {
        return database.smartSuggestionDao()
    }
    
    @Provides
    fun provideCompletionMetricsDao(database: HabitDatabase): CompletionMetricsDao {
        return database.completionMetricsDao()
    }
    
    @Provides
    fun provideHabitAnalyticsDao(database: HabitDatabase): HabitAnalyticsDao {
        return database.habitAnalyticsDao()
    }

    @Provides
    fun providePartialSessionDao(database: HabitDatabase): PartialSessionDao {
        return database.partialSessionDao()
    }
    
    // Phase UIX-1 Alert Profiles DAO
    @Provides
    fun provideTimerAlertProfileDao(database: HabitDatabase): TimerAlertProfileDao {
        return database.timerAlertProfileDao()
    }
    
    // Engines
    @Provides
    @Singleton
    fun provideStreakCalculationEngine(): StreakCalculationEngine {
        return StreakCalculationEngine()
    }

    @Provides
    @Singleton
    fun providePatternSuggestionEngine(): PatternSuggestionEngine = PatternSuggestionEngine()
    
    @Provides
    @Singleton
    fun provideHabitManagementEngine(
        habitDao: HabitDao,
        completionDao: HabitCompletionDao,
        streakEngine: StreakCalculationEngine
    ): HabitManagementEngine {
        return HabitManagementEngine(habitDao, completionDao, streakEngine)
    }
    
    // Repositories
    @Provides
    @Singleton
    fun provideTimingRepository(
        habitTimingDao: HabitTimingDao,
        timerSessionDao: TimerSessionDao,
        smartSuggestionDao: SmartSuggestionDao,
        completionMetricsDao: CompletionMetricsDao,
        habitAnalyticsDao: HabitAnalyticsDao,
        partialSessionDao: PartialSessionDao,
        patternSuggestionEngine: PatternSuggestionEngine
    ): TimingRepository {
        return TimingRepositoryImpl(
            habitTimingDao,
            timerSessionDao,
            smartSuggestionDao,
            completionMetricsDao,
            habitAnalyticsDao,
            partialSessionDao,
            patternSuggestionEngine
        )
    }
    
    @Provides
    @Singleton
    fun provideHabitRepository(
        habitManagementEngine: HabitManagementEngine,
        timingRepository: TimingRepository
    ): HabitRepository {
        return HabitRepositoryImpl(habitManagementEngine, timingRepository)
    }
}
