package com.habittracker.data.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.habittracker.ui.models.timing.Feature
import com.habittracker.ui.models.timing.UserBehaviorMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface BehaviorMetricsRepository {
    fun metrics(): Flow<UserBehaviorMetrics>
    suspend fun setMetrics(metrics: UserBehaviorMetrics)
}

@Singleton
class BehaviorMetricsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson
) : BehaviorMetricsRepository {

    private object Keys {
        val METRICS_JSON = stringPreferencesKey("timing.behavior_metrics.json")
    }

    override fun metrics(): Flow<UserBehaviorMetrics> =
        dataStore.data
            .catch { e ->
                // Surface DataStore read errors by falling back to defaults
                Log.w("BehaviorMetricsRepo", "DataStore read failed", e)
                emit(emptyPreferences())
            }
            .map { prefs ->
                val json = prefs[Keys.METRICS_JSON]
                if (json.isNullOrBlank()) {
                    UserBehaviorMetrics()
                } else {
                    runCatching { gson.fromJson(json, UserBehaviorMetrics::class.java) }
                        .getOrElse { UserBehaviorMetrics() }
                }
            }
            .distinctUntilChanged()

    override suspend fun setMetrics(metrics: UserBehaviorMetrics) {
        // Robust write with best-effort safety
        val json = gson.toJson(metrics)
        try {
            dataStore.edit { it[Keys.METRICS_JSON] = json }
        } catch (e: Exception) {
            Log.e("BehaviorMetricsRepo", "DataStore write failed", e)
        }
    }
}
