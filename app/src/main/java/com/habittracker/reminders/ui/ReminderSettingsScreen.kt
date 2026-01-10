package com.habittracker.reminders.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habittracker.R
import com.habittracker.data.database.entity.HabitEntity
import com.habittracker.reminders.viewmodel.ReminderSettingsViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Comprehensive reminder settings screen with individual habit controls,
 * global settings, and permission management.
 */
@Composable
fun ReminderSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReminderSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Reminder Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Status Card
            item {
                ReminderPermissionCard(
                    reminderStatus = uiState.reminderStatus,
                    onRequestPermissions = { viewModel.requestNotificationPermissions(context) }
                )
            }
            
            // Global Settings Card
            item {
                GlobalReminderSettingsCard(
                    summaryEnabled = uiState.summaryReminderEnabled,
                    summaryTime = uiState.summaryReminderTime,
                    soundEnabled = uiState.soundEnabled,
                    vibrationEnabled = uiState.vibrationEnabled,
                    snoozeDuration = uiState.snoozeDurationMinutes,
                    onSummaryEnabledChanged = viewModel::updateSummaryReminderEnabled,
                    onSummaryTimeChanged = viewModel::updateSummaryReminderTime,
                    onSoundEnabledChanged = viewModel::updateSoundEnabled,
                    onVibrationEnabledChanged = viewModel::updateVibrationEnabled,
                    onSnoozeDurationChanged = viewModel::updateSnoozeDuration
                )
            }
            
            // Individual Habit Settings
            item {
                Text(
                    text = "Individual Habit Reminders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(uiState.habits) { habit ->
                HabitReminderCard(
                    habit = habit,
                    isEnabled = uiState.habitReminderStates[habit.id] ?: false,
                    reminderTime = uiState.habitReminderTimes[habit.id] ?: LocalTime.of(9, 0),
                    onEnabledChanged = { enabled ->
                        viewModel.updateHabitReminderEnabled(habit.id, enabled)
                    },
                    onTimeChanged = { time ->
                        viewModel.updateHabitReminderTime(habit.id, time)
                    }
                )
            }
            
            // Quick Actions
            item {
                QuickActionsCard(
                    onEnableAll = viewModel::enableAllReminders,
                    onDisableAll = viewModel::disableAllReminders,
                    onTestReminder = viewModel::testReminder
                )
            }
        }
    }
}

@Composable
private fun ReminderPermissionCard(
    reminderStatus: com.habittracker.reminders.ReminderStatus?,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reminderStatus?.canScheduleExactAlarms == true) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (reminderStatus?.canScheduleExactAlarms == true) 
                        Icons.Default.CheckCircle 
                    else 
                        Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (reminderStatus?.canScheduleExactAlarms == true) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reminder Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (reminderStatus?.canScheduleExactAlarms == true) {
                Text(
                    text = "✓ All permissions granted. Reminders will work perfectly!",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "⚠️ Some permissions are missing. Reminders may not work reliably.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

@Composable
private fun GlobalReminderSettingsCard(
    summaryEnabled: Boolean,
    summaryTime: LocalTime,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    snoozeDuration: Int,
    onSummaryEnabledChanged: (Boolean) -> Unit,
    onSummaryTimeChanged: (LocalTime) -> Unit,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onVibrationEnabledChanged: (Boolean) -> Unit,
    onSnoozeDurationChanged: (Int) -> Unit
) {
    var showSummaryTimePicker by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Global Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary Reminder Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Summary",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Get a daily overview of pending habits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = summaryEnabled,
                    onCheckedChange = onSummaryEnabledChanged
                )
            }
            
            // Summary Time Picker (if enabled)
            if (summaryEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Summary Time",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = { showSummaryTimePicker = true }
                    ) {
                        Text(summaryTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sound Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sound",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = onSoundEnabledChanged
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Vibration Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Vibration, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vibration",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = onVibrationEnabledChanged
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Snooze Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Snooze Duration",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (snoozeDuration > 5) onSnoozeDurationChanged(snoozeDuration - 5) }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = "$snoozeDuration min",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = { if (snoozeDuration < 60) onSnoozeDurationChanged(snoozeDuration + 5) }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
        }
    }
    
    // Summary Time Picker Dialog
    if (showSummaryTimePicker) {
        TimePickerDialog(
            initialTime = summaryTime,
            onTimeSelected = { newTime ->
                onSummaryTimeChanged(newTime)
                showSummaryTimePicker = false
            },
            onDismiss = { showSummaryTimePicker = false },
            title = "Set Summary Time"
        )
    }
}

@Composable
private fun HabitReminderCard(
    habit: HabitEntity,
    isEnabled: Boolean,
    reminderTime: LocalTime,
    onEnabledChanged: (Boolean) -> Unit,
    onTimeChanged: (LocalTime) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (habit.description.isNotEmpty()) {
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChanged
                )
            }
            
            if (isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reminder Time",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = { showTimePicker = true }
                    ) {
                        Text(reminderTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }
            }
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = reminderTime,
            onTimeSelected = { newTime ->
                onTimeChanged(newTime)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
            title = "Set Reminder Time for ${habit.name}"
        )
    }
}

@Composable
private fun QuickActionsCard(
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onTestReminder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEnableAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enable All")
                }
                OutlinedButton(
                    onClick = onDisableAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disable All")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onTestReminder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Reminder")
            }
        }
    }
}
