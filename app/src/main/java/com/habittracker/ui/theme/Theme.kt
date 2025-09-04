package com.habittracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.themes.domain.AccentColor
import com.habittracker.themes.domain.AccentOverrideMode
import com.habittracker.themes.domain.ThemeManager
import com.habittracker.themes.domain.ThemePreferences
import com.habittracker.themes.presentation.ThemeViewModel

/**
 * Creates a light color scheme with custom accent color
 */
private fun relativeLuminance(c: Color): Double {
    fun channel(v: Float): Double {
        val srgb = v.coerceIn(0f, 1f).toDouble()
        return if (srgb <= 0.04045) srgb / 12.92 else Math.pow((srgb + 0.055) / 1.055, 2.4)
    }
    val r = channel(c.red)
    val g = channel(c.green)
    val b = channel(c.blue)
    // Rec. 709 luma coefficients per WCAG
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun contrastRatio(a: Color, b: Color): Double {
    val l1 = relativeLuminance(a)
    val l2 = relativeLuminance(b)
    val (lighter, darker) = if (l1 >= l2) l1 to l2 else l2 to l1
    return (lighter + 0.05) / (darker + 0.05)
}

private fun bestOnColor(bg: Color): Color {
    val black = Color.Black
    val white = Color.White
    val blackC = contrastRatio(black, bg)
    val whiteC = contrastRatio(white, bg)
    return if (blackC >= whiteC) black else white
}

private fun createLightColorScheme(accentColor: AccentColor): ColorScheme {
    val surface = SurfaceLight
    val background = BackgroundLight

    val primary = accentColor.lightColor
    val primaryContainer = accentColor.lightColor.copy(alpha = 0.14f)
    val primaryContainerOnSurface = primaryContainer.compositeOver(surface)

    val secondary = HabitSecondary
    val secondaryContainer = HabitSecondary.copy(alpha = 0.14f)
    val secondaryContainerOnSurface = secondaryContainer.compositeOver(surface)

    val tertiary = HabitAccent
    val tertiaryContainer = HabitAccent.copy(alpha = 0.14f)
    val tertiaryContainerOnSurface = tertiaryContainer.compositeOver(surface)

    val error = ErrorRed
    val errorContainer = ErrorRed.copy(alpha = 0.14f)
    val errorContainerOnSurface = errorContainer.compositeOver(surface)

    return lightColorScheme(
        primary = primary,
        onPrimary = bestOnColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = bestOnColor(primaryContainerOnSurface),
        secondary = secondary,
        onSecondary = bestOnColor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = bestOnColor(secondaryContainerOnSurface),
        tertiary = tertiary,
        onTertiary = bestOnColor(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = bestOnColor(tertiaryContainerOnSurface),
        background = background,
        onBackground = bestOnColor(background),
        surface = surface,
        onSurface = bestOnColor(surface),
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = bestOnColor(SurfaceVariantLight),
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        error = error,
        onError = bestOnColor(error),
        errorContainer = errorContainer,
        onErrorContainer = bestOnColor(errorContainerOnSurface)
    )
}

/**
 * Creates a dark color scheme with custom accent color
 */
private fun createDarkColorScheme(accentColor: AccentColor): ColorScheme {
    val surface = SurfaceDark
    val background = BackgroundDark

    val primary = accentColor.darkColor
    val primaryContainer = accentColor.darkColor.copy(alpha = 0.22f)
    val primaryContainerOnSurface = primaryContainer.compositeOver(surface)

    val secondary = HabitSecondary
    val secondaryContainer = HabitSecondary.copy(alpha = 0.22f)
    val secondaryContainerOnSurface = secondaryContainer.compositeOver(surface)

    val tertiary = HabitAccent
    val tertiaryContainer = HabitAccent.copy(alpha = 0.22f)
    val tertiaryContainerOnSurface = tertiaryContainer.compositeOver(surface)

    val error = ErrorRed
    val errorContainer = ErrorRed.copy(alpha = 0.22f)
    val errorContainerOnSurface = errorContainer.compositeOver(surface)

    return darkColorScheme(
        primary = primary,
        onPrimary = bestOnColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = bestOnColor(primaryContainerOnSurface),
        secondary = secondary,
        onSecondary = bestOnColor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = bestOnColor(secondaryContainerOnSurface),
        tertiary = tertiary,
        onTertiary = bestOnColor(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = bestOnColor(tertiaryContainerOnSurface),
        background = background,
        onBackground = bestOnColor(background),
        surface = surface,
        onSurface = bestOnColor(surface),
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = bestOnColor(SurfaceVariantDark),
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        error = error,
        onError = bestOnColor(error),
        errorContainer = errorContainer,
        onErrorContainer = bestOnColor(errorContainerOnSurface)
    )
}

@Composable
fun HabitTrackerTheme(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val themeState by themeViewModel.themeState
    val isDarkTheme = themeViewModel.shouldUseDarkTheme(themeState)
    val context = LocalContext.current

    // Determine color scheme based on preferences
    val colorScheme = when {
        // Use dynamic colors if supported and enabled
        themeViewModel.shouldUseDynamicColor(themeState) -> {
            val base = if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // If user wants accent override, blend/replace primary/secondary/tertiary families
            val mode = themeState.accentOverrideMode
            if (mode == AccentOverrideMode.OFF) {
                base
            } else {
                val strength = themeState.accentOverrideStrength.coerceIn(0f, 1f)
                val accent = if (isDarkTheme) themeState.accentColor.darkColor else themeState.accentColor.lightColor
                fun blend(a: Color, b: Color): Color {
                    // Linear blend in sRGB; simple and fast
                    val inv = 1f - strength
                    return Color(
                        red = a.red * inv + b.red * strength,
                        green = a.green * inv + b.green * strength,
                        blue = a.blue * inv + b.blue * strength,
                        alpha = 1f
                    )
                }

                val primary = blend(base.primary, accent)
                val onPrimary = bestOnColor(primary)
                val primaryContainer = blend(base.primaryContainer, accent.copy(alpha = if (isDarkTheme) 0.22f else 0.14f))
                val onPrimaryContainer = bestOnColor(primaryContainer.compositeOver(base.surface))

                val secondaryAccent = if (mode == AccentOverrideMode.TRIO) accent.copy(blue = (accent.blue * 0.9f).coerceIn(0f,1f)) else base.secondary
                val secondary = if (mode == AccentOverrideMode.TRIO) blend(base.secondary, secondaryAccent) else base.secondary
                val secondaryContainer = if (mode == AccentOverrideMode.TRIO) blend(base.secondaryContainer, secondaryAccent.copy(alpha = if (isDarkTheme) 0.22f else 0.14f)) else base.secondaryContainer
                val onSecondary = bestOnColor(secondary)
                val onSecondaryContainer = bestOnColor(secondaryContainer.compositeOver(base.surface))

                val tertiaryAccent = if (mode == AccentOverrideMode.TRIO) accent.copy(green = (accent.green * 0.9f).coerceIn(0f,1f)) else base.tertiary
                val tertiary = if (mode == AccentOverrideMode.TRIO) blend(base.tertiary, tertiaryAccent) else base.tertiary
                val tertiaryContainer = if (mode == AccentOverrideMode.TRIO) blend(base.tertiaryContainer, tertiaryAccent.copy(alpha = if (isDarkTheme) 0.22f else 0.14f)) else base.tertiaryContainer
                val onTertiary = bestOnColor(tertiary)
                val onTertiaryContainer = bestOnColor(tertiaryContainer.compositeOver(base.surface))

                base.copy(
                    primary = primary,
                    onPrimary = onPrimary,
                    primaryContainer = primaryContainer,
                    onPrimaryContainer = onPrimaryContainer,
                    secondary = secondary,
                    onSecondary = onSecondary,
                    secondaryContainer = secondaryContainer,
                    onSecondaryContainer = onSecondaryContainer,
                    tertiary = tertiary,
                    onTertiary = onTertiary,
                    tertiaryContainer = tertiaryContainer,
                    onTertiaryContainer = onTertiaryContainer
                )
            }
        }
        // Use custom accent colors
        isDarkTheme -> createDarkColorScheme(themeState.accentColor)
        else -> createLightColorScheme(themeState.accentColor)
    }

    // Create typography with font scaling
    val typography = createTypography(themeState.fontSize)

    // Set status bar color
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

/**
 * Legacy theme composable for backward compatibility
 */
@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createDarkColorScheme(AccentColor.INDIGO)
        else -> createLightColorScheme(AccentColor.INDIGO)
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
