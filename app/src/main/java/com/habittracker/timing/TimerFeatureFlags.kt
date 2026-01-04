package com.habittracker.timing

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Centralised feature flags for timing-related experiments.
 * Values persist to shared preferences so QA can toggle without recompiling.
 */
object TimerFeatureFlags {
    private const val PREF_NAME = "timer_feature_flags"
    private const val KEY_ALERT_SCHEDULING = "enable_alert_scheduling"
    private const val KEY_ACTION_COORDINATOR = "enable_action_coordinator"
    private const val KEY_SIMPLIFIED_HOME = "use_simplified_home_screen"

    @Volatile
    var enableAlertScheduling: Boolean = true
        private set

    @Volatile
    var enableActionCoordinator: Boolean = Defaults().enableActionCoordinator
        private set

    /** When true, uses the new SimpleMainScreen instead of legacy MainScreen */
    @Volatile
    var useSimplifiedHomeScreen: Boolean = Defaults().useSimplifiedHomeScreen
        private set

    private var prefs: SharedPreferences? = null
    private var defaults: Defaults = Defaults()

    fun initialize(context: Context, defaults: Defaults = Defaults()) {
        this.defaults = defaults
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs = sharedPreferences
        enableAlertScheduling = sharedPreferences.getBoolean(KEY_ALERT_SCHEDULING, defaults.enableAlertScheduling)
        enableActionCoordinator = sharedPreferences.getBoolean(KEY_ACTION_COORDINATOR, defaults.enableActionCoordinator)
        useSimplifiedHomeScreen = sharedPreferences.getBoolean(KEY_SIMPLIFIED_HOME, defaults.useSimplifiedHomeScreen)
    }

    fun overrideAlertScheduling(enabled: Boolean) {
        enableAlertScheduling = enabled
        prefs?.edit { putBoolean(KEY_ALERT_SCHEDULING, enabled) }
    }

    fun overrideActionCoordinator(enabled: Boolean) {
        enableActionCoordinator = enabled
        prefs?.edit { putBoolean(KEY_ACTION_COORDINATOR, enabled) }
    }

    fun overrideSimplifiedHomeScreen(enabled: Boolean) {
        useSimplifiedHomeScreen = enabled
        prefs?.edit { putBoolean(KEY_SIMPLIFIED_HOME, enabled) }
    }

    fun reset() {
        enableAlertScheduling = defaults.enableAlertScheduling
        enableActionCoordinator = defaults.enableActionCoordinator
        useSimplifiedHomeScreen = defaults.useSimplifiedHomeScreen
        prefs?.edit {
            putBoolean(KEY_ALERT_SCHEDULING, enableAlertScheduling)
            putBoolean(KEY_ACTION_COORDINATOR, enableActionCoordinator)
            putBoolean(KEY_SIMPLIFIED_HOME, useSimplifiedHomeScreen)
        }
    }

    data class Defaults(
        val enableAlertScheduling: Boolean = true,
        val enableActionCoordinator: Boolean = true,
        /** Start with false (legacy UI) until new UI is validated */
        val useSimplifiedHomeScreen: Boolean = false
    )
}
