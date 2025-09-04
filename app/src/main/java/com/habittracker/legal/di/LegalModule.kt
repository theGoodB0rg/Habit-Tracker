package com.habittracker.legal.di

import android.content.Context
import com.habittracker.legal.data.LegalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing legal module dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object LegalModule {
    
    @Provides
    @Singleton
    fun provideLegalRepository(
        @ApplicationContext context: Context
    ): LegalRepository {
        return LegalRepository(context)
    }
}
