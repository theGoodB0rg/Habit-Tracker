package com.habittracker.reminders.di

import android.content.Context
import com.habittracker.data.repository.HabitRepository
import com.habittracker.reminders.ReminderManager
import com.habittracker.reminders.ReminderPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing reminder-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RemindersModule {
    
    @Provides
    @Singleton
    fun provideReminderPreferences(
        @ApplicationContext context: Context
    ): ReminderPreferences {
        return ReminderPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideReminderManager(
        @ApplicationContext context: Context,
        habitRepository: HabitRepository,
        reminderPreferences: ReminderPreferences
    ): ReminderManager {
        return ReminderManager(context, habitRepository, reminderPreferences)
    }
}
