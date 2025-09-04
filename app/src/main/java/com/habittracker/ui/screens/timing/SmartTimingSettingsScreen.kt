package com.habittracker.ui.screens.timing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.habittracker.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.ui.components.timing.LevelUpDialog
import com.habittracker.ui.components.timing.FeatureDiscoveryBanner
import com.habittracker.ui.models.timing.*
import com.habittracker.ui.components.timing.TimingFeatureIntroCard
import com.habittracker.ui.models.timing.*
import com.habittracker.ui.viewmodels.timing.TimingFeatureViewModel
import com.habittracker.data.preferences.TimingPreferencesRepository
import com.habittracker.ui.viewmodels.timing.TimingAudioSettingsViewModel
import com.habittracker.ui.viewmodels.timing.AlertProfilesViewModel
import com.habittracker.timing.AlertType
import java.time.Duration

/**
 * Phase 2: Smart Timing Settings Screen
 * 
 * Progressive settings that reveal complexity based on user engagement level
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTimingSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAlertProfiles: () -> Unit = {},
    modifier: Modifier = Modifier,
    timingViewModel: TimingFeatureViewModel = hiltViewModel()
) {
    val userEngagementLevel by timingViewModel.userEngagementLevel.collectAsState()
    val smartTimingPreferences by timingViewModel.smartTimingPreferences.collectAsState()
    val availableFeatures by timingViewModel.availableFeatures.collectAsState()
    val nextLevelBenefits by timingViewModel.nextLevelBenefits.collectAsState()
    val progressToNextLevel by timingViewModel.progressToNextLevel.collectAsState()
    val pendingLevelUp by timingViewModel.pendingLevelUp.collectAsState()
    
    // Handle level up dialog
    pendingLevelUp?.let { notification ->
        LevelUpDialog(
            notification = notification,
            onAccept = { timingViewModel.acceptLevelUp() },
            onDismiss = { timingViewModel.dismissLevelUp() }
        )
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    // Collect errors and show a snackbar with retry hint
    LaunchedEffect(Unit) {
        timingViewModel.errorMessages.collect { msg ->
            snackbarHostState.showSnackbar(message = msg)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text("Smart Timing") 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
    // Snackbar host
    SnackbarHost(hostState = snackbarHostState)

    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio & Alert Delivery Section (always visible once timers concept exists)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AlertAudioSettingsCard()
                    // Manage Profiles entry
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Alert Profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Create or edit alert schedules (Quiet, Focus, Verbose)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = onNavigateToAlertProfiles) { Text("Manage") }
                        }
                    }
                }
            }
            // Feature Discovery Banner
            if (nextLevelBenefits.isNotEmpty() && userEngagementLevel != UserEngagementLevel.PowerUser) {
                item {
                    FeatureDiscoveryBanner(
                        currentLevel = userEngagementLevel,
                        nextLevelBenefits = nextLevelBenefits,
                        progressToNextLevel = progressToNextLevel,
                        onLearnMore = { /* Show level benefits */ },
                        onDismiss = { /* Dismiss banner */ }
                    )
                }
            }
            
            // Current Level Display
            item {
                CurrentLevelCard(
                    level = userEngagementLevel,
                    availableFeatures = availableFeatures
                )
            }
            
            // Basic Settings (Always Available)
            item {
                SettingsSection(
                    title = "Basic Settings",
                    description = "Core timing preferences"
                ) {
                    BasicTimingSettings(
                        preferences = smartTimingPreferences,
                        onPreferencesUpdate = timingViewModel::updateTimingPreferences
                    )
                }
            }
            
            // Level 1+ Features
            if (availableFeatures.contains(Feature.SIMPLE_TIMER)) {
                item {
                    SettingsSection(
                        title = "Timer Settings",
                        description = "Configure your focus timers"
                    ) {
                        TimerSettings(
                            preferences = smartTimingPreferences,
                            onPreferencesUpdate = timingViewModel::updateTimingPreferences,
                            isNewlyUnlocked = timingViewModel.shouldShowFeatureIntro(Feature.SIMPLE_TIMER)
                        )
                    }
                }
            }
            
            // Level 2+ Features
            if (availableFeatures.contains(Feature.SMART_SUGGESTIONS)) {
                item {
                    SettingsSection(
                        title = "Smart Suggestions",
                        description = "AI-powered timing recommendations"
                    ) {
                        SmartSuggestionSettings(
                            preferences = smartTimingPreferences,
                            onPreferencesUpdate = timingViewModel::updateTimingPreferences,
                            isNewlyUnlocked = timingViewModel.shouldShowFeatureIntro(Feature.SMART_SUGGESTIONS)
                        )
                    }
                }
            }
            
            // Level 3+ Features
            if (availableFeatures.contains(Feature.CONTEXT_AWARENESS)) {
                item {
                    SettingsSection(
                        title = "Context Awareness",
                        description = "Environment-based optimizations"
                    ) {
                        ContextAwarenessSettings(
                            preferences = smartTimingPreferences,
                            onPreferencesUpdate = timingViewModel::updateTimingPreferences,
                            isNewlyUnlocked = timingViewModel.shouldShowFeatureIntro(Feature.CONTEXT_AWARENESS)
                        )
                    }
                }
            }
            
            // Power User Features
            if (userEngagementLevel == UserEngagementLevel.PowerUser) {
                item {
                    SettingsSection(
                        title = "Advanced Features",
                        description = "Power user customizations"
                    ) {
                        AdvancedTimingSettings(
                            preferences = smartTimingPreferences,
                            onPreferencesUpdate = timingViewModel::updateTimingPreferences
                        )
                    }
                }
            }
            
            // Feature Introduction Cards
            items(availableFeatures.filter { timingViewModel.shouldShowFeatureIntro(it) }) { feature ->
                TimingFeatureIntroCard(
                    feature = feature,
                    isNewlyUnlocked = true,
                    onTryFeature = {
                        timingViewModel.enableFeature(feature)
                        timingViewModel.markFeatureSeen(feature)
                    },
                    onDismiss = { timingViewModel.markFeatureSeen(feature) }
                )
            }
            
            // Footer spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- New Section: Alert & Audio Settings ---
@Composable
private fun AlertAudioSettingsCard(
    modifier: Modifier = Modifier,
    vm: TimingAudioSettingsViewModel = hiltViewModel()
) {
    val prefs by vm.prefs.collectAsState()
    var showPackDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    if (showProfileDialog) {
        val profileVm: AlertProfilesViewModel = hiltViewModel()
        val ui by profileVm.ui.collectAsState()
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            confirmButton = {},
            title = { Text(stringResource(id = R.string.select_alert_profile)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.profiles.forEach { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = ui.selectedId == p.id,
                                    onClick = {
                                        profileVm.select(p.id)
                                        showProfileDialog = false
                                    },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = ui.selectedId == p.id, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.displayName, fontWeight = FontWeight.Medium)
                                if (!p.description.isNullOrBlank()) Text(p.description!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        )
    }
    if (showPackDialog) {
        AlertDialog(
            onDismissRequest = { showPackDialog = false },
            confirmButton = {},
            title = { Text(stringResource(id = R.string.select_sound_pack)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "default" to "Default Chimes",
                        "minimal" to "Minimal (Start/Final)",
                        "silent" to "Silent",
                        "system" to "System Final Only"
                    ).forEach { (id,label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = prefs.selectedSoundPackId == id,
                                    onClick = {
                                        vm.setSoundPack(id)
                                        showPackDialog = false
                                    }, role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = prefs.selectedSoundPackId == id, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            }
        )
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(id = R.string.alert_audio_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            // Global audio toggle
            SettingToggleRow(
                title = stringResource(id = R.string.audio_cues),
                subtitle = stringResource(id = R.string.audio_cues_sub),
                checked = prefs.enableGlobalAudioCues,
                onChange = vm::toggleGlobalAudio
            )
            // Reduced Motion (accessibility)
            SettingToggleRow(
                title = "Reduced motion",
                subtitle = "Limit animations and haptic intensity",
                checked = prefs.reducedMotion,
                onChange = { enabled -> vm.toggleReducedMotion(enabled) }
            )
            SettingToggleRow(
                title = stringResource(id = R.string.progress_cues),
                subtitle = stringResource(id = R.string.progress_cues_sub),
                checked = prefs.enableProgressCues,
                onChange = vm::toggleProgress,
                enabled = prefs.enableGlobalAudioCues
            )
            SettingToggleRow(
                title = stringResource(id = R.string.haptics),
                subtitle = stringResource(id = R.string.haptics_sub),
                checked = prefs.enableHaptics,
                onChange = vm::toggleHaptics
            )
            SettingToggleRow(
                title = stringResource(id = R.string.voice_tts),
                subtitle = stringResource(id = R.string.voice_tts_sub),
                checked = prefs.enableTts,
                onChange = vm::toggleTts
            )
            SettingToggleRow(
                title = stringResource(id = R.string.tone_variation),
                subtitle = stringResource(id = R.string.tone_variation_sub),
                checked = prefs.enableToneVariation,
                onChange = vm::setToneVariation,
                enabled = prefs.enableGlobalAudioCues
            )
            // Heads-up final notification
            SettingToggleRow(
                title = stringResource(id = R.string.heads_up_final),
                subtitle = stringResource(id = R.string.heads_up_final_sub),
                checked = prefs.enableHeadsUpFinal,
                onChange = vm::toggleHeadsUpFinal
            )
            // Sound pack selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.sound_pack), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(prefs.selectedSoundPackId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = { showPackDialog = true }, label = { Text(stringResource(id = R.string.change)) })
            }
            // Alert profile selection
            val profileVm: AlertProfilesViewModel = hiltViewModel()
            val ui by profileVm.ui.collectAsState()
            val selectedProfile = ui.profiles.firstOrNull { it.id == prefs.defaultAlertProfileId }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.alert_profile), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(selectedProfile?.displayName ?: prefs.defaultAlertProfileId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = { showProfileDialog = true }, label = { Text(stringResource(id = R.string.change)) })
            }
            // Master volume
            Column {
                Text(stringResource(id = R.string.volume), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                val volState = prefs.soundMasterVolumePercent
                Slider(
                    value = volState / 100f,
                    onValueChange = { vm.setSoundMasterVolume((it * 100).toInt()) },
                    steps = 9,
                    valueRange = 0f..1f
                )
                Text("${volState}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Preview buttons
            val audioVm: TimingAudioSettingsViewModel = hiltViewModel()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { audioVm.playPreview(AlertType.START) }) { Text(stringResource(id = R.string.preview_start)) }
                Button(onClick = { audioVm.playPreview(AlertType.FINAL) }) { Text(stringResource(id = R.string.preview_final)) }
            }
        }
    }
 }
@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                contentDescription = "$title. $subtitle"
                stateDescription = if (checked) "On" else "Off"
            }
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

// PreviewButton removed: integrated directly into AlertAudioSettingsCard

@Composable
private fun CurrentLevelCard(
    level: UserEngagementLevel,
    availableFeatures: List<Feature>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getLevelIcon(level),
                    contentDescription = "Current level icon",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Current Level: ${level.displayName()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${availableFeatures.size} features unlocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun BasicTimingSettings(
    preferences: SmartTimingPreferences,
    onPreferencesUpdate: (SmartTimingPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Auto Level Up Setting
        SettingToggleRow(
            title = "Auto-unlock features",
            subtitle = "Automatically discover new features as you use the app",
            checked = preferences.autoLevelUp,
            onChange = { onPreferencesUpdate(preferences.copy(autoLevelUp = it)) }
        )
        
        // Show Level Up Prompts
        SettingToggleRow(
            title = "Level up notifications",
            subtitle = "Show benefits when new features become available",
            checked = preferences.showLevelUpPrompts,
            onChange = { onPreferencesUpdate(preferences.copy(showLevelUpPrompts = it)) }
        )
    }
}

@Composable
private fun TimerSettings(
    preferences: SmartTimingPreferences,
    onPreferencesUpdate: (SmartTimingPreferences) -> Unit,
    isNewlyUnlocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isNewlyUnlocked) {
            Text(
                text = "🎉 NEW FEATURE UNLOCKED!",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Enable Timers
        SettingToggleRow(
            title = "Enable timers",
            subtitle = "Show timer buttons on habit cards",
            checked = preferences.enableTimers,
            onChange = { onPreferencesUpdate(preferences.copy(enableTimers = it)) }
        )
        
        if (preferences.enableTimers) {
            // Default Timer Duration
            Column {
                Text(
                    text = "Default timer duration",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                val durations = listOf(
                    Duration.ofMinutes(15) to "15 minutes",
                    Duration.ofMinutes(25) to "25 minutes (Pomodoro)",
                    Duration.ofMinutes(30) to "30 minutes",
                    Duration.ofMinutes(45) to "45 minutes",
                    Duration.ofMinutes(60) to "1 hour"
                )
                
                durations.forEach { (duration, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = preferences.timerDefaultDuration == duration,
                                onClick = {
                                    onPreferencesUpdate(preferences.copy(timerDefaultDuration = duration))
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferences.timerDefaultDuration == duration,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartSuggestionSettings(
    preferences: SmartTimingPreferences,
    onPreferencesUpdate: (SmartTimingPreferences) -> Unit,
    isNewlyUnlocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isNewlyUnlocked) {
            Text(
                text = "🎉 SMART SUGGESTIONS UNLOCKED!",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        SettingToggleRow(
            title = "Enable smart suggestions",
            subtitle = "Get AI-powered timing recommendations",
            checked = preferences.enableSmartSuggestions,
            onChange = { onPreferencesUpdate(preferences.copy(enableSmartSuggestions = it)) }
        )
    }
}

@Composable
private fun ContextAwarenessSettings(
    preferences: SmartTimingPreferences,
    onPreferencesUpdate: (SmartTimingPreferences) -> Unit,
    isNewlyUnlocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isNewlyUnlocked) {
            Text(
                text = "🎉 CONTEXT AWARENESS UNLOCKED!",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        SettingToggleRow(
            title = "Enable context awareness",
            subtitle = "Suggestions based on time, weather, and patterns",
            checked = preferences.enableContextAwareness,
            onChange = { onPreferencesUpdate(preferences.copy(enableContextAwareness = it)) }
        )
    }
}

@Composable
private fun AdvancedTimingSettings(
    preferences: SmartTimingPreferences,
    onPreferencesUpdate: (SmartTimingPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "⭐ POWER USER FEATURES",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        // Habit Stacking
        SettingToggleRow(
            title = "Habit stacking",
            subtitle = "Recommendations for combining habits",
            checked = preferences.enableHabitStacking,
            onChange = { onPreferencesUpdate(preferences.copy(enableHabitStacking = it)) }
        )
        
        // Complexity Level
        Column {
            Text(
                text = "Complexity level",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            TimingComplexityLevel.values().forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = preferences.complexityLevel == level,
                            onClick = {
                                onPreferencesUpdate(preferences.copy(complexityLevel = level))
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferences.complexityLevel == level,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = level.displayName(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = level.description(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Helper functions
private fun getLevelIcon(level: UserEngagementLevel): ImageVector {
    return when (level) {
        UserEngagementLevel.Casual -> Icons.Default.Person
        UserEngagementLevel.Interested -> Icons.Default.Timer
        UserEngagementLevel.Engaged -> Icons.Default.Psychology
        UserEngagementLevel.PowerUser -> Icons.Default.Star
    }
}

private fun UserEngagementLevel.displayName(): String {
    return when (this) {
        UserEngagementLevel.Casual -> "Casual"
        UserEngagementLevel.Interested -> "Interested"
        UserEngagementLevel.Engaged -> "Engaged"
        UserEngagementLevel.PowerUser -> "Power User"
    }
}

private fun TimingComplexityLevel.displayName(): String {
    return when (this) {
        TimingComplexityLevel.BASIC -> "Basic"
        TimingComplexityLevel.INTERMEDIATE -> "Intermediate"
        TimingComplexityLevel.ADVANCED -> "Advanced"
        TimingComplexityLevel.POWER_USER -> "Power User"
    }
}

private fun TimingComplexityLevel.description(): String {
    return when (this) {
        TimingComplexityLevel.BASIC -> "Simple timers only"
        TimingComplexityLevel.INTERMEDIATE -> "Basic timers + smart suggestions"
        TimingComplexityLevel.ADVANCED -> "Full features except power user tools"
        TimingComplexityLevel.POWER_USER -> "All advanced features enabled"
    }
}
