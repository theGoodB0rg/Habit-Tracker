package com.habittracker.onboarding

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages onboarding preferences using SharedPreferences.
 * Tracks completion status and user choices for the onboarding flow.
 * 
 * @author Google-level Developer
 */
@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREF_NAME = "onboarding_preferences"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ONBOARDING_SKIPPED = "onboarding_skipped"
        private const val KEY_TOOLTIP_SHOWN_PREFIX = "tooltip_shown_"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_ONBOARDING_VERSION = "onboarding_version"
        
        // Increment this when onboarding content changes significantly
        private const val CURRENT_ONBOARDING_VERSION = 1
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * Checks if this is the first time the app is launched
     */
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * Marks the app as having been launched before
     */
    fun setFirstLaunchCompleted() {
        sharedPreferences.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }
    
    /**
     * Checks if the user has completed the onboarding flow
     */
    fun isOnboardingCompleted(): Boolean {
        val currentVersion = sharedPreferences.getInt(KEY_ONBOARDING_VERSION, 0)
        val isCompleted = sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        
        // Show onboarding again if version changed (new features)
        return isCompleted && currentVersion >= CURRENT_ONBOARDING_VERSION
    }
    
    /**
     * Marks the onboarding as completed
     */
    fun setOnboardingCompleted() {
        sharedPreferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION)
            .apply()
    }
    
    /**
     * Checks if the user skipped the onboarding
     */
    fun isOnboardingSkipped(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_SKIPPED, false)
    }
    
    /**
     * Marks the onboarding as skipped
     */
    fun setOnboardingSkipped() {
        sharedPreferences.edit()
            .putBoolean(KEY_ONBOARDING_SKIPPED, true)
            .putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION)
            .apply()
    }
    
    /**
     * Checks if a specific tooltip has been shown
     */
    fun isTooltipShown(tooltipId: String): Boolean {
        return sharedPreferences.getBoolean("$KEY_TOOLTIP_SHOWN_PREFIX$tooltipId", false)
    }
    
    /**
     * Marks a specific tooltip as shown
     */
    fun setTooltipShown(tooltipId: String) {
        sharedPreferences.edit()
            .putBoolean("$KEY_TOOLTIP_SHOWN_PREFIX$tooltipId", true)
            .apply()
    }
    
    /**
     * Resets all onboarding preferences (for testing or reset functionality)
     */
    fun resetOnboarding() {
        sharedPreferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .putBoolean(KEY_ONBOARDING_SKIPPED, false)
            .putInt(KEY_ONBOARDING_VERSION, 0)
            .apply()
    }
    
    /**
     * Resets all tooltip preferences
     */
    fun resetTooltips() {
        val editor = sharedPreferences.edit()
        
        // Remove all tooltip-related keys
        sharedPreferences.all.keys
            .filter { it.startsWith(KEY_TOOLTIP_SHOWN_PREFIX) }
            .forEach { key ->
                editor.remove(key)
            }
        
        editor.apply()
    }

    /**
     * Fully resets onboarding state including first launch and all tooltips (debug / testing aid)
     */
    fun resetAll() {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_ONBOARDING_COMPLETED, false)
        editor.putBoolean(KEY_ONBOARDING_SKIPPED, false)
        editor.putInt(KEY_ONBOARDING_VERSION, 0)
        editor.putBoolean(KEY_FIRST_LAUNCH, true)
        // Remove tooltips
        sharedPreferences.all.keys
            .filter { it.startsWith(KEY_TOOLTIP_SHOWN_PREFIX) }
            .forEach { key -> editor.remove(key) }
        editor.apply()
    }
    
    /**
     * Gets the current onboarding version
     */
    fun getCurrentOnboardingVersion(): Int {
        return sharedPreferences.getInt(KEY_ONBOARDING_VERSION, 0)
    }
    
    /**
     * Checks if onboarding should be shown (first launch or version update)
     */
    fun shouldShowOnboarding(): Boolean {
        return isFirstLaunch() || !isOnboardingCompleted()
    }
}
