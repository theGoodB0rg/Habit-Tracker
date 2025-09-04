package com.habittracker.export.di

import com.habittracker.export.domain.formatter.ExportFormatter
import com.habittracker.export.domain.formatter.ExportFormatterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for the export engine
 * The ExportDataRepository implementation will be provided by the main app module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExportEngineModule {

    @Binds
    @Singleton
    abstract fun bindExportFormatter(
        exportFormatterImpl: ExportFormatterImpl
    ): ExportFormatter
}
