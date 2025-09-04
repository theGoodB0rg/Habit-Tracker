package com.habittracker.themes.di

import android.content.Context
import com.habittracker.themes.data.ThemePreferencesRepository
import com.habittracker.themes.domain.ThemeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing theme-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {

    @Provides
    @Singleton
    fun provideThemePreferencesRepository(
        @ApplicationContext context: Context
    ): ThemePreferencesRepository {
        return ThemePreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideThemeManager(
        themePreferencesRepository: ThemePreferencesRepository
    ): ThemeManager {
        return ThemeManager(themePreferencesRepository)
    }
}
