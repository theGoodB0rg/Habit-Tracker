package com.habittracker.analytics.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class AnalyticsPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var isAnalyticsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_ANALYTICS_ENABLED, true)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_ANALYTICS_ENABLED, value).apply()
        }

    var exportAnonymizedData: Boolean
        get() = sharedPreferences.getBoolean(KEY_EXPORT_ANONYMIZED_DATA, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_EXPORT_ANONYMIZED_DATA, value).apply()
        }

    companion object {
        private const val KEY_ANALYTICS_ENABLED = "key_analytics_enabled"
        private const val KEY_EXPORT_ANONYMIZED_DATA = "key_export_anonymized_data"
    }
}