package com.habittracker.di

import android.content.Context
import com.habittracker.timing.TimerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimingControllerModule {

    @Provides
    @Singleton
    fun provideTimerController(
        @ApplicationContext context: Context
    ): TimerController = TimerController(context)
}
