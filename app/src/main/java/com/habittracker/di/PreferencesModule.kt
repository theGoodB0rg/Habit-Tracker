package com.habittracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.habittracker.data.preferences.TimingPreferencesRepository
import com.habittracker.data.preferences.TimingPreferencesRepositoryImpl
import com.habittracker.data.preferences.BehaviorMetricsRepository
import com.habittracker.data.preferences.BehaviorMetricsRepositoryImpl
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesBindModule {
    @Binds
    @Singleton
    abstract fun bindTimingPreferencesRepository(
        impl: TimingPreferencesRepositoryImpl
    ): TimingPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindBehaviorMetricsRepository(
        impl: BehaviorMetricsRepositoryImpl
    ): BehaviorMetricsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesProvideModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return PreferenceDataStoreFactory.create(
            scope = scope,
        ) {
            context.preferencesDataStoreFile("timing_preferences")
        }
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}
