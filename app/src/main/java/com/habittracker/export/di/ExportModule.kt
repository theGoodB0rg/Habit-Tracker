package com.habittracker.export.di

import com.habittracker.export.data.repository.ExportDataRepository
import com.habittracker.export.data.repository.AppExportDataRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for export functionality
 * Provides the implementation of ExportDataRepository in the main app
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {

    @Binds
    @Singleton
    abstract fun bindExportDataRepository(
        exportDataRepositoryImpl: AppExportDataRepositoryImpl
    ): ExportDataRepository
}
