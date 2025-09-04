package com.habittracker.themes.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.themes.domain.AccentColor
import com.habittracker.themes.domain.FontSize
import com.habittracker.themes.domain.ThemeManager
import com.habittracker.themes.domain.ThemeMode
import com.habittracker.themes.domain.ThemePreferences
import com.habittracker.themes.domain.AccentOverrideMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing theme state and handling theme changes
 * Provides thread-safe operations and proper state management
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeManager: ThemeManager
) : ViewModel() {

    // UI state for theme settings
    private val _uiState = MutableStateFlow(ThemeUiState())
    val uiState: StateFlow<ThemeUiState> = _uiState.asStateFlow()

    /**
     * Get current theme state as Compose State
     */
    val themeState: State<ThemePreferences>
        @Composable
        get() = themeManager.getThemeState()

    init {
        // Initialize UI state
        _uiState.value = ThemeUiState()
    }

    /**
     * Determine if dark theme should be used
     */
    @Composable
    fun shouldUseDarkTheme(themePreferences: ThemePreferences): Boolean {
        return themeManager.shouldUseDarkTheme(themePreferences)
    }

    /**
     * Check if dynamic color should be used
     */
    fun shouldUseDynamicColor(themePreferences: ThemePreferences): Boolean {
        return themeManager.shouldUseDynamicColor(themePreferences)
    }

    /**
     * Update theme mode with error handling
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                themeManager.setThemeMode(themeMode)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update theme mode: ${e.message}"
                )
            }
        }
    }

    /**
     * Update accent color with error handling
     */
    fun updateAccentColor(accentColor: AccentColor) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                themeManager.setAccentColor(accentColor)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update accent color: ${e.message}"
                )
            }
        }
    }

    /**
     * Update font size with error handling
     */
    fun updateFontSize(fontSize: FontSize) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                themeManager.setFontSize(fontSize)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update font size: ${e.message}"
                )
            }
        }
    }

    /**
     * Update dynamic color setting with error handling
     */
    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                themeManager.setDynamicColor(enabled)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update dynamic color setting: ${e.message}"
                )
            }
        }
    }

    /**
     * Update Material You setting with error handling
     */
    fun updateMaterialYou(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                themeManager.setMaterialYou(enabled)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update Material You setting: ${e.message}"
                )
            }
        }
    }

    fun updateAccentOverrideMode(mode: AccentOverrideMode) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                themeManager.setAccentOverrideMode(mode)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update accent override mode: ${e.message}"
                )
            }
        }
    }

    fun updateAccentOverrideStrength(strength: Float) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                themeManager.setAccentOverrideStrength(strength)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update accent strength: ${e.message}"
                )
            }
        }
    }

    /**
     * Reset all theme settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                themeManager.resetToDefaults()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to reset settings: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Show accent color picker dialog
     */
    fun showAccentColorPicker() {
        _uiState.value = _uiState.value.copy(showAccentColorPicker = true)
    }

    /**
     * Hide accent color picker dialog
     */
    fun hideAccentColorPicker() {
        _uiState.value = _uiState.value.copy(showAccentColorPicker = false)
    }

    /**
     * Show font size picker dialog
     */
    fun showFontSizePicker() {
        _uiState.value = _uiState.value.copy(showFontSizePicker = true)
    }

    /**
     * Hide font size picker dialog
     */
    fun hideFontSizePicker() {
        _uiState.value = _uiState.value.copy(showFontSizePicker = false)
    }
}

/**
 * UI state for theme settings screen
 */
data class ThemeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAccentColorPicker: Boolean = false,
    val showFontSizePicker: Boolean = false
)
