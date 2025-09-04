package com.habittracker.themes.domain

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import com.habittracker.themes.data.ThemePreferencesRepository
import com.habittracker.themes.domain.AccentOverrideMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central theme manager that provides current theme state and handles theme transitions
 * Ensures consistency across the app and handles system theme changes
 */
@Singleton
class ThemeManager @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository
) {
    /**
     * Flow of current theme preferences
     */
    val themePreferences: Flow<ThemePreferences> = themePreferencesRepository.themePreferences

    /**
     * Composable to get current theme state
     */
    @Composable
    fun getThemeState(): State<ThemePreferences> {
        return themePreferences.collectAsState(initial = ThemePreferences.default())
    }

    /**
     * Determine if dark theme should be used based on preferences and system state
     */
    @Composable
    fun shouldUseDarkTheme(themePreferences: ThemePreferences): Boolean {
        return when (themePreferences.themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    }

    /**
     * Get the current accent color based on theme mode
     */
    fun getCurrentAccentColor(
        accentColor: AccentColor,
        isDarkTheme: Boolean
    ): Color {
        return if (isDarkTheme) {
            accentColor.darkColor
        } else {
            accentColor.lightColor
        }
    }

    /**
     * Check if dynamic color is supported and enabled
     */
    fun shouldUseDynamicColor(themePreferences: ThemePreferences): Boolean {
        return themePreferences.dynamicColor && 
               themePreferences.materialYou && 
               Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Update theme mode
     */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        themePreferencesRepository.updateThemeMode(themeMode)
    }

    /**
     * Update accent color
     */
    suspend fun setAccentColor(accentColor: AccentColor) {
        themePreferencesRepository.updateAccentColor(accentColor)
    }

    /**
     * Update font size
     */
    suspend fun setFontSize(fontSize: FontSize) {
        themePreferencesRepository.updateFontSize(fontSize)
    }

    /**
     * Update dynamic color setting
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        themePreferencesRepository.updateDynamicColor(enabled)
    }

    /**
     * Update Material You setting
     */
    suspend fun setMaterialYou(enabled: Boolean) {
        themePreferencesRepository.updateMaterialYou(enabled)
    }

    suspend fun setAccentOverrideMode(mode: AccentOverrideMode) {
        themePreferencesRepository.updateAccentOverrideMode(mode)
    }

    suspend fun setAccentOverrideStrength(strength: Float) {
        themePreferencesRepository.updateAccentOverrideStrength(strength)
    }

    /**
     * Reset all theme settings to defaults
     */
    suspend fun resetToDefaults() {
        themePreferencesRepository.resetToDefaults()
    }

    /**
     * Flow that indicates if dynamic colors are available and enabled
     */
    val isDynamicColorAvailable: Flow<Boolean> = themePreferences.map { preferences ->
        shouldUseDynamicColor(preferences)
    }

    /**
     * Flow that provides the current effective accent color
     */
    @Composable
    fun getCurrentAccentColorFlow(isDarkTheme: Boolean): Flow<Color> {
        return themePreferences.map { preferences ->
            getCurrentAccentColor(preferences.accentColor, isDarkTheme)
        }
    }
}
