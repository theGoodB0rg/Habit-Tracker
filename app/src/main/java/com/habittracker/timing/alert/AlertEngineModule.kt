package com.habittracker.timing.alert

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlertEngineModule {
    @Binds
    @Singleton
    abstract fun bindAlertEngine(impl: AlertEngineImpl): AlertEngine
}
