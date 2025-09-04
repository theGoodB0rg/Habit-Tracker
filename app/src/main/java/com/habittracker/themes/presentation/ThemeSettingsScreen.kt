package com.habittracker.themes.presentation

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.themes.domain.AccentColor
import com.habittracker.themes.domain.FontSize
import com.habittracker.themes.domain.ThemeMode
import com.habittracker.themes.domain.AccentOverrideMode

/**
 * Theme settings screen with modern Material 3 design
 * Supports theme switching, accent color selection, and font size adjustment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeState by themeViewModel.themeState
    val uiState by themeViewModel.uiState.collectAsState()

    // Handle error display with proper error management
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Log error for debugging
            android.util.Log.w("ThemeSettings", "Theme error: $error")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Theme Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Section
            item {
                ThemeSettingsSection(
                    title = "Theme Mode",
                    icon = Icons.Default.Palette
                ) {
                    ThemeModeSelector(
                        currentMode = themeState.themeMode,
                        onModeChange = themeViewModel::updateThemeMode,
                        enabled = !uiState.isLoading
                    )
                }
            }

            // Accent Color Section
            item {
                ThemeSettingsSection(
                    title = "Accent Color",
                    icon = Icons.Default.ColorLens,
                    subtitle = if (themeState.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "Using Material You colors"
                    } else {
                        "Choose your favorite color"
                    }
                ) {
                    AccentColorSelector(
                        currentColor = themeState.accentColor,
                        onColorChange = themeViewModel::updateAccentColor,
                        enabled = !uiState.isLoading && 
                                 (!themeState.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                    )
                }
            }

            // Font Size Section
            item {
                ThemeSettingsSection(
                    title = "Font Size",
                    icon = Icons.Default.FormatSize,
                    subtitle = "Adjust text size for better readability"
                ) {
                    FontSizeSelector(
                        currentSize = themeState.fontSize,
                        onSizeChange = themeViewModel::updateFontSize,
                        enabled = !uiState.isLoading
                    )
                }
            }

            // Dynamic Color Section (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    ThemeSettingsSection(
                        title = "Dynamic Color",
                        icon = Icons.Default.AutoAwesome,
                        subtitle = "Use colors from your wallpaper (Material You)"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Material You",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Adapts to your wallpaper colors",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = themeState.dynamicColor,
                                onCheckedChange = themeViewModel::updateDynamicColor,
                                enabled = !uiState.isLoading
                            )
                        }
                    }
                }

                // Accent override controls when dynamic color is ON
                if (themeState.dynamicColor) {
                    item {
                        ThemeSettingsSection(
                            title = "Accent Override",
                            icon = Icons.Default.Colorize,
                            subtitle = "Apply your accent over Material You"
                        ) {
                            // Mode selector
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Apply to",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                AssistChip(
                                    onClick = { themeViewModel.updateAccentOverrideMode(AccentOverrideMode.OFF) },
                                    label = { Text("Off") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (themeState.accentOverrideMode == AccentOverrideMode.OFF) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = if (themeState.accentOverrideMode == AccentOverrideMode.OFF) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                AssistChip(
                                    onClick = { themeViewModel.updateAccentOverrideMode(AccentOverrideMode.PRIMARY) },
                                    label = { Text("Primary") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (themeState.accentOverrideMode == AccentOverrideMode.PRIMARY) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = if (themeState.accentOverrideMode == AccentOverrideMode.PRIMARY) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                AssistChip(
                                    onClick = { themeViewModel.updateAccentOverrideMode(AccentOverrideMode.TRIO) },
                                    label = { Text("Trio") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (themeState.accentOverrideMode == AccentOverrideMode.TRIO) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = if (themeState.accentOverrideMode == AccentOverrideMode.TRIO) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            // Strength slider
                            Column {
                                Text(
                                    text = "Strength",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Slider(
                                    value = themeState.accentOverrideStrength.coerceIn(0f,1f),
                                    onValueChange = { themeViewModel.updateAccentOverrideStrength(it) },
                                    valueRange = 0f..1f
                                )
                            }
                        }
                    }
                }
            }

            // Reset Settings Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Reset to Defaults",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Reset all theme settings to their default values",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = themeViewModel::resetToDefaults,
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onError,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Reset")
                            }
                        }
                    }
                }
            }

            // Add some bottom padding
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ThemeSettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
