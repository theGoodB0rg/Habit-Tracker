package com.habittracker.themes.domain

import androidx.compose.ui.graphics.Color

/**
 * Represents the app's theme mode
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/** Accent override application mode when dynamic color is enabled */
enum class AccentOverrideMode {
    OFF,          // Do not override dynamic colors
    PRIMARY,      // Override primary family only
    TRIO          // Override primary, secondary, tertiary
}

/**
 * Represents available font sizes
 */
enum class FontSize(val scaleFactor: Float, val displayName: String) {
    SMALL(0.85f, "Small"),
    NORMAL(1.0f, "Normal"),
    LARGE(1.15f, "Large")
}

/**
 * Predefined accent colors for the app
 */
enum class AccentColor(
    val lightColor: Color,
    val darkColor: Color,
    val displayName: String
) {
    INDIGO(
        lightColor = Color(0xFF6366F1),
        darkColor = Color(0xFF818CF8),
        displayName = "Indigo"
    ),
    EMERALD(
        lightColor = Color(0xFF10B981),
        darkColor = Color(0xFF34D399),
        displayName = "Emerald"
    ),
    AMBER(
        lightColor = Color(0xFFF59E0B),
        darkColor = Color(0xFFFBBF24),
        displayName = "Amber"
    ),
    ROSE(
        lightColor = Color(0xFFF43F5E),
        darkColor = Color(0xFFFB7185),
        displayName = "Rose"
    ),
    PURPLE(
        lightColor = Color(0xFF8B5CF6),
        darkColor = Color(0xFFA78BFA),
        displayName = "Purple"
    ),
    BLUE(
        lightColor = Color(0xFF3B82F6),
        darkColor = Color(0xFF60A5FA),
        displayName = "Blue"
    ),
    TEAL(
        lightColor = Color(0xFF14B8A6),
        darkColor = Color(0xFF2DD4BF),
        displayName = "Teal"
    ),
    ORANGE(
        lightColor = Color(0xFFF97316),
        darkColor = Color(0xFFFB923C),
        displayName = "Orange"
    )
}

/**
 * Theme preferences data class with default values
 */
data class ThemePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.INDIGO,
    val fontSize: FontSize = FontSize.NORMAL,
    val dynamicColor: Boolean = true,
    val materialYou: Boolean = true,
    // New: controls for user accent overriding dynamic color schemes
    val accentOverrideMode: AccentOverrideMode = AccentOverrideMode.OFF,
    // 0.0 .. 1.0 blending of base dynamic accent with user accent
    val accentOverrideStrength: Float = 1.0f
) {
    companion object {
        fun default() = ThemePreferences()
    }
}
