package com.habittracker.themes.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.habittracker.themes.domain.AccentColor
import com.habittracker.themes.domain.FontSize
import com.habittracker.themes.domain.ThemeMode
import com.habittracker.themes.domain.ThemePreferences
import com.habittracker.themes.domain.AccentOverrideMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Create DataStore instance
private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

/**
 * Repository for managing theme preferences using DataStore
 * Thread-safe with proper error handling and race condition protection
 */
@Singleton
class ThemePreferencesRepository @Inject constructor(
    private val context: Context
) {
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
        private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val MATERIAL_YOU_KEY = booleanPreferencesKey("material_you")
    private val ACCENT_OVERRIDE_MODE_KEY = stringPreferencesKey("accent_override_mode")
    private val ACCENT_OVERRIDE_STRENGTH_KEY = stringPreferencesKey("accent_override_strength")
    }

    /**
     * Flow of theme preferences with error handling and fallback to defaults
     */
    val themePreferences: Flow<ThemePreferences> = context.themeDataStore.data
        .catch { exception ->
            // Handle DataStore errors gracefully
            exception.printStackTrace()
            emit(emptyPreferences())
        }
        .map { preferences ->
            ThemePreferences(
                themeMode = getThemeMode(preferences),
                accentColor = getAccentColor(preferences),
                fontSize = getFontSize(preferences),
                dynamicColor = preferences[DYNAMIC_COLOR_KEY] ?: true,
                materialYou = preferences[MATERIAL_YOU_KEY] ?: true,
                accentOverrideMode = getAccentOverrideMode(preferences),
                accentOverrideStrength = (preferences[ACCENT_OVERRIDE_STRENGTH_KEY]?.toFloatOrNull()
                    ?: 1.0f).coerceIn(0f, 1f)
            )
        }

    /**
     * Update theme mode with atomic operation
     */
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }

    /**
     * Update accent color with atomic operation
     */
    suspend fun updateAccentColor(accentColor: AccentColor) {
        context.themeDataStore.edit { preferences ->
            preferences[ACCENT_COLOR_KEY] = accentColor.name
        }
    }

    /**
     * Update font size with atomic operation
     */
    suspend fun updateFontSize(fontSize: FontSize) {
        context.themeDataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = fontSize.name
        }
    }

    /**
     * Update dynamic color setting with atomic operation
     */
    suspend fun updateDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    /**
     * Update Material You setting with atomic operation
     */
    suspend fun updateMaterialYou(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[MATERIAL_YOU_KEY] = enabled
        }
    }

    suspend fun updateAccentOverrideMode(mode: AccentOverrideMode) {
        context.themeDataStore.edit { p ->
            p[ACCENT_OVERRIDE_MODE_KEY] = mode.name
        }
    }

    suspend fun updateAccentOverrideStrength(strength: Float) {
        context.themeDataStore.edit { p ->
            p[ACCENT_OVERRIDE_STRENGTH_KEY] = strength.coerceIn(0f, 1f).toString()
        }
    }

    /**
     * Reset all theme preferences to defaults
     */
    suspend fun resetToDefaults() {
        context.themeDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Update multiple preferences atomically to avoid race conditions
     */
    suspend fun updateThemePreferences(themePreferences: ThemePreferences) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themePreferences.themeMode.name
            preferences[ACCENT_COLOR_KEY] = themePreferences.accentColor.name
            preferences[FONT_SIZE_KEY] = themePreferences.fontSize.name
            preferences[DYNAMIC_COLOR_KEY] = themePreferences.dynamicColor
            preferences[MATERIAL_YOU_KEY] = themePreferences.materialYou
            preferences[ACCENT_OVERRIDE_MODE_KEY] = themePreferences.accentOverrideMode.name
            preferences[ACCENT_OVERRIDE_STRENGTH_KEY] = themePreferences.accentOverrideStrength.coerceIn(0f,1f).toString()
        }
    }

    // Helper methods with safe fallbacks
    private fun getThemeMode(preferences: Preferences): ThemeMode {
        return try {
            val modeString = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(modeString)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    private fun getAccentColor(preferences: Preferences): AccentColor {
        return try {
            val colorString = preferences[ACCENT_COLOR_KEY] ?: AccentColor.INDIGO.name
            AccentColor.valueOf(colorString)
        } catch (e: IllegalArgumentException) {
            AccentColor.INDIGO
        }
    }

    private fun getFontSize(preferences: Preferences): FontSize {
        return try {
            val sizeString = preferences[FONT_SIZE_KEY] ?: FontSize.NORMAL.name
            FontSize.valueOf(sizeString)
        } catch (e: IllegalArgumentException) {
            FontSize.NORMAL
        }
    }

    private fun getAccentOverrideMode(preferences: Preferences): AccentOverrideMode {
        return try {
            val s = preferences[ACCENT_OVERRIDE_MODE_KEY] ?: AccentOverrideMode.OFF.name
            AccentOverrideMode.valueOf(s)
        } catch (_: IllegalArgumentException) {
            AccentOverrideMode.OFF
        }
    }
}
