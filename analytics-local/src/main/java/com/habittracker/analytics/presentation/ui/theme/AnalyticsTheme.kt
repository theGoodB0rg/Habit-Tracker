package com.habittracker.analytics.presentation.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val analyticsColorScheme = lightColorScheme(
    primary = AnalyticsColors.primaryColor,
    secondary = AnalyticsColors.secondaryColor,
    background = AnalyticsColors.backgroundColor,
    surface = AnalyticsColors.surfaceColor,
    error = AnalyticsColors.errorColor,
    onPrimary = AnalyticsColors.onPrimary,
    onSecondary = AnalyticsColors.onSecondary,
    onBackground = AnalyticsColors.onBackground,
    onSurface = AnalyticsColors.onSurface,
    onError = AnalyticsColors.onError
)

@Composable
fun AnalyticsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = analyticsColorScheme,
        content = content
    )
}