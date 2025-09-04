package com.habittracker.reminders.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.data.database.entity.HabitEntity
import com.habittracker.data.repository.HabitRepository
import com.habittracker.reminders.ReminderManager
import com.habittracker.reminders.ReminderPreferences
import com.habittracker.reminders.ReminderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel for managing reminder settings UI state and business logic
 */
@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val reminderManager: ReminderManager,
    private val reminderPreferences: ReminderPreferences,
    private val habitRepository: HabitRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReminderSettingsUiState())
    val uiState: StateFlow<ReminderSettingsUiState> = _uiState.asStateFlow()
    
    /**
     * Loads all reminder settings and habit data
     */
    fun loadSettings() {
        viewModelScope.launch {
            // Load habits
            habitRepository.getAllHabits()
                .combine(_uiState) { habits, currentState ->
                    val habitReminderStates = habits.associate { habit ->
                        habit.id to reminderPreferences.isReminderEnabled(habit.id)
                    }
                    
                    val habitReminderTimes = habits.associate { habit ->
                        habit.id to reminderPreferences.getReminderTime(habit.id)
                    }
                    
                    currentState.copy(
                        habits = habits,
                        habitReminderStates = habitReminderStates,
                        habitReminderTimes = habitReminderTimes
                    )
                }
                .collect { newState ->
                    _uiState.value = newState
                }
        }
        
        // Load global settings
        loadGlobalSettings()
        
        // Check reminder status
        updateReminderStatus()
    }
    
    /**
     * Loads global reminder settings
     */
    private fun loadGlobalSettings() {
        val settings = reminderPreferences.getReminderSettingsSummary()
        
        _uiState.value = _uiState.value.copy(
            summaryReminderEnabled = settings.summaryReminderEnabled,
            summaryReminderTime = settings.summaryReminderTime,
            soundEnabled = settings.soundEnabled,
            vibrationEnabled = settings.vibrationEnabled,
            snoozeDurationMinutes = settings.snoozeDurationMinutes
        )
    }
    
    /**
     * Updates the reminder status (permissions, etc.)
     */
    private fun updateReminderStatus() {
        val status = reminderManager.getReminderStatus()
        _uiState.value = _uiState.value.copy(reminderStatus = status)
    }
    
    /**
     * Updates summary reminder enabled state
     */
    fun updateSummaryReminderEnabled(enabled: Boolean) {
        reminderManager.updateSummaryReminderSettings(
            enabled = enabled,
            reminderTime = if (enabled) _uiState.value.summaryReminderTime else null
        )
        
        _uiState.value = _uiState.value.copy(summaryReminderEnabled = enabled)
    }
    
    /**
     * Updates summary reminder time
     */
    fun updateSummaryReminderTime(time: LocalTime) {
        reminderManager.updateSummaryReminderSettings(
            enabled = _uiState.value.summaryReminderEnabled,
            reminderTime = time
        )
        
        _uiState.value = _uiState.value.copy(summaryReminderTime = time)
    }
    
    /**
     * Updates sound enabled state
     */
    fun updateSoundEnabled(enabled: Boolean) {
        reminderPreferences.setNotificationSoundEnabled(enabled)
        _uiState.value = _uiState.value.copy(soundEnabled = enabled)
    }
    
    /**
     * Updates vibration enabled state
     */
    fun updateVibrationEnabled(enabled: Boolean) {
        reminderPreferences.setNotificationVibrationEnabled(enabled)
        _uiState.value = _uiState.value.copy(vibrationEnabled = enabled)
    }
    
    /**
     * Updates snooze duration
     */
    fun updateSnoozeDuration(minutes: Int) {
        reminderPreferences.setSnoozeDurationMinutes(minutes)
        _uiState.value = _uiState.value.copy(snoozeDurationMinutes = minutes)
    }
    
    /**
     * Updates reminder enabled state for a specific habit
     */
    fun updateHabitReminderEnabled(habitId: Long, enabled: Boolean) {
        reminderManager.updateHabitReminderSettings(habitId, enabled)
        
        val currentStates = _uiState.value.habitReminderStates.toMutableMap()
        currentStates[habitId] = enabled
        
        _uiState.value = _uiState.value.copy(habitReminderStates = currentStates)
    }
    
    /**
     * Updates reminder time for a specific habit
     */
    fun updateHabitReminderTime(habitId: Long, time: LocalTime) {
        reminderManager.updateHabitReminderSettings(
            habitId = habitId,
            enabled = _uiState.value.habitReminderStates[habitId] ?: false,
            reminderTime = time
        )
        
        val currentTimes = _uiState.value.habitReminderTimes.toMutableMap()
        currentTimes[habitId] = time
        
        _uiState.value = _uiState.value.copy(habitReminderTimes = currentTimes)
    }
    
    /**
     * Enables reminders for all habits
     */
    fun enableAllReminders() {
        viewModelScope.launch {
            val habits = _uiState.value.habits
            val newStates = habits.associate { habit ->
                reminderManager.updateHabitReminderSettings(habit.id, true)
                habit.id to true
            }
            
            _uiState.value = _uiState.value.copy(habitReminderStates = newStates)
        }
    }
    
    /**
     * Disables reminders for all habits
     */
    fun disableAllReminders() {
        viewModelScope.launch {
            val habits = _uiState.value.habits
            val newStates = habits.associate { habit ->
                reminderManager.updateHabitReminderSettings(habit.id, false)
                habit.id to false
            }
            
            _uiState.value = _uiState.value.copy(habitReminderStates = newStates)
        }
    }
    
    /**
     * Tests a reminder notification
     */
    fun testReminder() {
        viewModelScope.launch {
            try {
                val habits = _uiState.value.habits
                if (habits.isNotEmpty()) {
                    val testHabit = habits.first()
                    reminderManager.snoozeHabitReminder(
                        habitId = testHabit.id,
                        habitName = "Test Reminder",
                        habitDescription = "This is a test notification to verify reminders are working correctly."
                    )
                }
            } catch (e: Exception) {
                // Handle error - could show a snackbar or toast
            }
        }
    }
    
    /**
     * Requests notification permissions
     */
    fun requestNotificationPermissions(context: Context) {
        try {
            // Check if notifications are enabled
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                // Open app notification settings
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
                return
            }
            
            // For Android 12+, check exact alarm permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }
                context.startActivity(intent)
            }
            
            // Update reminder status after permission request
            updateReminderStatus()
            
        } catch (e: Exception) {
            // Handle error - permission request failed
        }
    }
}

/**
 * UI state for the reminder settings screen
 */
data class ReminderSettingsUiState(
    val habits: List<HabitEntity> = emptyList(),
    val habitReminderStates: Map<Long, Boolean> = emptyMap(),
    val habitReminderTimes: Map<Long, LocalTime> = emptyMap(),
    val summaryReminderEnabled: Boolean = true,
    val summaryReminderTime: LocalTime = LocalTime.of(20, 0),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 15,
    val reminderStatus: ReminderStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
