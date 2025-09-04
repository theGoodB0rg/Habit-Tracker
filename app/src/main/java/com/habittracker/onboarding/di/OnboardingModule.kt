package com.habittracker.onboarding.di

import android.content.Context
import com.habittracker.onboarding.OnboardingPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for onboarding components
 * 
 * @author Google-level Developer
 */
@Module
@InstallIn(SingletonComponent::class)
object OnboardingModule {
    
    @Provides
    @Singleton
    fun provideOnboardingPreferences(
        @ApplicationContext context: Context
    ): OnboardingPreferences {
        return OnboardingPreferences(context)
    }
}
