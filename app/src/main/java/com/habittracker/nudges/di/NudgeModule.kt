package com.habittracker.nudges.di

import com.habittracker.data.repository.HabitRepository
import com.habittracker.nudges.analyzer.HabitPatternAnalyzer
import com.habittracker.nudges.engine.NudgeEngine
import com.habittracker.nudges.repository.NudgeRepository
import com.habittracker.nudges.scheduler.NudgeScheduler
import com.habittracker.nudges.service.NudgeService
import com.habittracker.nudges.usecase.GenerateNudgesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for nudges engine dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NudgeModule {
    
    @Provides
    @Singleton
    fun provideNudgeEngine(): NudgeEngine {
        return NudgeEngine()
    }
    
    @Provides
    @Singleton
    fun provideHabitPatternAnalyzer(): HabitPatternAnalyzer {
        return HabitPatternAnalyzer()
    }
    
    @Provides
    @Singleton
    fun provideNudgeRepository(): NudgeRepository {
        return NudgeRepository()
    }
    
    @Provides
    @Singleton
    fun provideGenerateNudgesUseCase(
        nudgeEngine: NudgeEngine,
        patternAnalyzer: HabitPatternAnalyzer,
        nudgeRepository: NudgeRepository
    ): GenerateNudgesUseCase {
        return GenerateNudgesUseCase(nudgeEngine, patternAnalyzer, nudgeRepository)
    }
    
    @Provides
    @Singleton
    fun provideNudgeScheduler(
        generateNudgesUseCase: GenerateNudgesUseCase
    ): NudgeScheduler {
        return NudgeScheduler(generateNudgesUseCase)
    }
    
    @Provides
    @Singleton
    fun provideNudgeService(
        nudgeScheduler: NudgeScheduler,
        habitRepository: HabitRepository
    ): NudgeService {
        return NudgeService(nudgeScheduler, habitRepository)
    }
}
