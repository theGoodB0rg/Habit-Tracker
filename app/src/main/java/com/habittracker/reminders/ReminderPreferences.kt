package com.habittracker.reminders

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages reminder preferences using SharedPreferences.
 * Handles per-habit notification settings and global reminder preferences.
 */
@Singleton
class ReminderPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFERENCES_NAME = "habit_reminder_preferences"
        
        // Keys for individual habit preferences
        private const val KEY_HABIT_REMINDER_ENABLED = "habit_reminder_enabled_"
        private const val KEY_HABIT_REMINDER_HOUR = "habit_reminder_hour_"
        private const val KEY_HABIT_REMINDER_MINUTE = "habit_reminder_minute_"
        
        // Keys for global preferences
        private const val KEY_SUMMARY_REMINDER_ENABLED = "summary_reminder_enabled"
        private const val KEY_SUMMARY_REMINDER_HOUR = "summary_reminder_hour"
        private const val KEY_SUMMARY_REMINDER_MINUTE = "summary_reminder_minute"
        private const val KEY_NOTIFICATION_SOUND_ENABLED = "notification_sound_enabled"
        private const val KEY_NOTIFICATION_VIBRATION_ENABLED = "notification_vibration_enabled"
        private const val KEY_SNOOZE_DURATION = "snooze_duration_minutes"
        
        // Default values
        private const val DEFAULT_REMINDER_HOUR = 9 // 9 AM
        private const val DEFAULT_REMINDER_MINUTE = 0
        private const val DEFAULT_SUMMARY_HOUR = 20 // 8 PM
        private const val DEFAULT_SUMMARY_MINUTE = 0
        private const val DEFAULT_SNOOZE_DURATION = 15 // 15 minutes
    }
    
    // Individual habit reminder settings
    
    /**
     * Checks if reminders are enabled for a specific habit
     */
    fun isReminderEnabled(habitId: Long): Boolean {
        return sharedPreferences.getBoolean("$KEY_HABIT_REMINDER_ENABLED$habitId", true)
    }
    
    /**
     * Sets reminder enabled/disabled for a specific habit
     */
    fun setReminderEnabled(habitId: Long, enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("$KEY_HABIT_REMINDER_ENABLED$habitId", enabled)
            .apply()
    }
    
    /**
     * Gets the reminder time for a specific habit
     */
    fun getReminderTime(habitId: Long): LocalTime {
        val hour = sharedPreferences.getInt("$KEY_HABIT_REMINDER_HOUR$habitId", DEFAULT_REMINDER_HOUR)
        val minute = sharedPreferences.getInt("$KEY_HABIT_REMINDER_MINUTE$habitId", DEFAULT_REMINDER_MINUTE)
        return LocalTime.of(hour, minute)
    }
    
    /**
     * Sets the reminder time for a specific habit
     */
    fun setReminderTime(habitId: Long, time: LocalTime) {
        sharedPreferences.edit()
            .putInt("$KEY_HABIT_REMINDER_HOUR$habitId", time.hour)
            .putInt("$KEY_HABIT_REMINDER_MINUTE$habitId", time.minute)
            .apply()
    }
    
    /**
     * Removes all preferences for a specific habit (when habit is deleted)
     */
    fun removeHabitPreferences(habitId: Long) {
        sharedPreferences.edit()
            .remove("$KEY_HABIT_REMINDER_ENABLED$habitId")
            .remove("$KEY_HABIT_REMINDER_HOUR$habitId")
            .remove("$KEY_HABIT_REMINDER_MINUTE$habitId")
            .apply()
    }
    
    // Global summary reminder settings
    
    /**
     * Checks if summary reminders are enabled
     */
    fun isSummaryReminderEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SUMMARY_REMINDER_ENABLED, true)
    }
    
    /**
     * Sets summary reminders enabled/disabled
     */
    fun setSummaryReminderEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SUMMARY_REMINDER_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Gets the summary reminder time
     */
    fun getSummaryReminderTime(): LocalTime {
        val hour = sharedPreferences.getInt(KEY_SUMMARY_REMINDER_HOUR, DEFAULT_SUMMARY_HOUR)
        val minute = sharedPreferences.getInt(KEY_SUMMARY_REMINDER_MINUTE, DEFAULT_SUMMARY_MINUTE)
        return LocalTime.of(hour, minute)
    }
    
    /**
     * Sets the summary reminder time
     */
    fun setSummaryReminderTime(time: LocalTime) {
        sharedPreferences.edit()
            .putInt(KEY_SUMMARY_REMINDER_HOUR, time.hour)
            .putInt(KEY_SUMMARY_REMINDER_MINUTE, time.minute)
            .apply()
    }
    
    // Notification behavior settings
    
    /**
     * Checks if notification sound is enabled
     */
    fun isNotificationSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_SOUND_ENABLED, true)
    }
    
    /**
     * Sets notification sound enabled/disabled
     */
    fun setNotificationSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_NOTIFICATION_SOUND_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Checks if notification vibration is enabled
     */
    fun isNotificationVibrationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_VIBRATION_ENABLED, true)
    }
    
    /**
     * Sets notification vibration enabled/disabled
     */
    fun setNotificationVibrationEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_NOTIFICATION_VIBRATION_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Gets the snooze duration in minutes
     */
    fun getSnoozeDurationMinutes(): Int {
        return sharedPreferences.getInt(KEY_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION)
    }
    
    /**
     * Sets the snooze duration in minutes
     */
    fun setSnoozeDurationMinutes(minutes: Int) {
        sharedPreferences.edit()
            .putInt(KEY_SNOOZE_DURATION, minutes)
            .apply()
    }
    
    /**
     * Gets all habits that have reminders enabled
     */
    fun getHabitsWithRemindersEnabled(): List<Long> {
        val allPrefs = sharedPreferences.all
        val habitIds = mutableListOf<Long>()
        
        allPrefs.keys.forEach { key ->
            if (key.startsWith(KEY_HABIT_REMINDER_ENABLED)) {
                val habitId = key.removePrefix(KEY_HABIT_REMINDER_ENABLED).toLongOrNull()
                if (habitId != null && sharedPreferences.getBoolean(key, false)) {
                    habitIds.add(habitId)
                }
            }
        }
        
        return habitIds
    }
    
    /**
     * Resets all reminder preferences to defaults
     */
    fun resetAllPreferences() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * Gets a summary of current reminder settings
     */
    fun getReminderSettingsSummary(): ReminderSettingsSummary {
        return ReminderSettingsSummary(
            summaryReminderEnabled = isSummaryReminderEnabled(),
            summaryReminderTime = getSummaryReminderTime(),
            soundEnabled = isNotificationSoundEnabled(),
            vibrationEnabled = isNotificationVibrationEnabled(),
            snoozeDurationMinutes = getSnoozeDurationMinutes(),
            habitsWithRemindersCount = getHabitsWithRemindersEnabled().size
        )
    }
}

/**
 * Data class containing a summary of reminder settings
 */
data class ReminderSettingsSummary(
    val summaryReminderEnabled: Boolean,
    val summaryReminderTime: LocalTime,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val snoozeDurationMinutes: Int,
    val habitsWithRemindersCount: Int
)
