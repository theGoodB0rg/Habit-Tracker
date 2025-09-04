package com.habittracker.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.habittracker.data.database.entity.HabitEntity
import com.habittracker.data.repository.HabitRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive reminder manager handling alarm scheduling, notifications,
 * and reminder preferences for habit tracking.
 */
@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val reminderPreferences: ReminderPreferences
) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "ReminderManager"
        private const val HABIT_REMINDER_REQUEST_CODE_BASE = 10000
        private const val SUMMARY_REMINDER_REQUEST_CODE = 99999
        private const val SNOOZE_DURATION_MINUTES = 15
    }
    
    /**
     * Schedules reminders for all active habits that have notifications enabled
     */
    fun scheduleAllHabitReminders() {
        coroutineScope.launch {
            try {
                val activeHabits = habitRepository.getAllHabits().first().filter { it.isActive }
                
                activeHabits.forEach { habit ->
                    if (reminderPreferences.isReminderEnabled(habit.id)) {
                        scheduleHabitReminder(habit)
                    }
                }
                
                // Schedule summary notification if enabled
                if (reminderPreferences.isSummaryReminderEnabled()) {
                    scheduleSummaryReminder()
                }
                
                Log.d(TAG, "Scheduled reminders for ${activeHabits.size} habits")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling habit reminders", e)
            }
        }
    }
    
    /**
     * Schedules a reminder for a specific habit
     */
    fun scheduleHabitReminder(habit: HabitEntity) {
        if (!reminderPreferences.isReminderEnabled(habit.id)) {
            Log.d(TAG, "Reminder disabled for habit: ${habit.name}")
            return
        }
        
        val reminderTime = reminderPreferences.getReminderTime(habit.id)
        val triggerTime = calculateNextTriggerTime(reminderTime)
        
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, habit.name)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_DESCRIPTION, habit.description)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (HABIT_REMINDER_REQUEST_CODE_BASE + habit.id).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Scheduled reminder for habit '${habit.name}' at ${Date(triggerTime)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder for habit: ${habit.name}", e)
        }
    }
    
    /**
     * Schedules the daily summary reminder
     */
    fun scheduleSummaryReminder() {
        if (!reminderPreferences.isSummaryReminderEnabled()) {
            return
        }
        
        val summaryTime = reminderPreferences.getSummaryReminderTime()
        val triggerTime = calculateNextTriggerTime(summaryTime)
        
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_SUMMARY_REMINDER
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SUMMARY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Scheduled summary reminder at ${Date(triggerTime)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule summary reminder", e)
        }
    }
    
    /**
     * Cancels reminder for a specific habit
     */
    fun cancelHabitReminder(habitId: Long) {
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (HABIT_REMINDER_REQUEST_CODE_BASE + habitId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled reminder for habit ID: $habitId")
    }
    
    /**
     * Cancels all habit reminders
     */
    fun cancelAllReminders() {
        coroutineScope.launch {
            try {
                val habits = habitRepository.getAllHabits().first()
                habits.forEach { habit ->
                    cancelHabitReminder(habit.id)
                }
                
                // Cancel summary reminder
                val summaryIntent = Intent(context, HabitReminderReceiver::class.java)
                val summaryPendingIntent = PendingIntent.getBroadcast(
                    context,
                    SUMMARY_REMINDER_REQUEST_CODE,
                    summaryIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(summaryPendingIntent)
                
                Log.d(TAG, "Cancelled all reminders")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling all reminders", e)
            }
        }
    }
    
    /**
     * Schedules a snooze reminder for a specific habit
     */
    fun snoozeHabitReminder(habitId: Long, habitName: String, habitDescription: String) {
        val snoozeTime = System.currentTimeMillis() + (SNOOZE_DURATION_MINUTES * 60 * 1000)
        
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_DESCRIPTION, habitDescription)
            putExtra(HabitReminderReceiver.EXTRA_IS_SNOOZED, true)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (HABIT_REMINDER_REQUEST_CODE_BASE + habitId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Snoozed reminder for habit '$habitName' for $SNOOZE_DURATION_MINUTES minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to snooze reminder for habit: $habitName", e)
        }
    }
    
    /**
     * Updates reminder settings for a habit
     */
    fun updateHabitReminderSettings(habitId: Long, enabled: Boolean, reminderTime: LocalTime? = null) {
        reminderPreferences.setReminderEnabled(habitId, enabled)
        
        if (enabled && reminderTime != null) {
            reminderPreferences.setReminderTime(habitId, reminderTime)
            
            // Reschedule the reminder with new time
            coroutineScope.launch {
                try {
                    val habit = habitRepository.getHabitById(habitId)
                    habit?.let { scheduleHabitReminder(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating reminder settings for habit: $habitId", e)
                }
            }
        } else if (!enabled) {
            cancelHabitReminder(habitId)
        }
    }
    
    /**
     * Updates summary reminder settings
     */
    fun updateSummaryReminderSettings(enabled: Boolean, reminderTime: LocalTime? = null) {
        reminderPreferences.setSummaryReminderEnabled(enabled)
        
        if (enabled && reminderTime != null) {
            reminderPreferences.setSummaryReminderTime(reminderTime)
            scheduleSummaryReminder()
        } else if (!enabled) {
            val summaryIntent = Intent(context, HabitReminderReceiver::class.java)
            val summaryPendingIntent = PendingIntent.getBroadcast(
                context,
                SUMMARY_REMINDER_REQUEST_CODE,
                summaryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(summaryPendingIntent)
        }
    }
    
    /**
     * Calculates the next trigger time for a given LocalTime
     */
    private fun calculateNextTriggerTime(time: LocalTime): Long {
        val now = LocalDateTime.now()
        var triggerDateTime = now.toLocalDate().atTime(time)
        
        // If the time has already passed today, schedule for tomorrow
        if (triggerDateTime.isBefore(now) || triggerDateTime.isEqual(now)) {
            triggerDateTime = triggerDateTime.plusDays(1)
        }
        
        return triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    /**
     * Checks if the app can schedule exact alarms (Android 12+)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
    
    /**
     * Reschedules all reminders (useful after device reboot or app update)
     */
    fun rescheduleAllReminders() {
        Log.d(TAG, "Rescheduling all reminders...")
        cancelAllReminders()
        scheduleAllHabitReminders()
    }
    
    /**
     * Gets the status of reminder permissions and scheduling capability
     */
    fun getReminderStatus(): ReminderStatus {
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canScheduleExactAlarms()
        } else {
            true
        }
        
        return ReminderStatus(
            canScheduleExactAlarms = canScheduleExact,
            batteryOptimizationDisabled = true // Can be enhanced to check actual battery optimization status
        )
    }
}

/**
 * Data class representing the status of reminder capabilities
 */
data class ReminderStatus(
    val canScheduleExactAlarms: Boolean,
    val batteryOptimizationDisabled: Boolean
)
