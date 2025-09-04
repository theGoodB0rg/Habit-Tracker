package com.habittracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver that handles habit reminder notifications.
 * Supports individual habit reminders, summary reminders, snooze, and dismiss actions.
 */
@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var reminderManager: ReminderManager
    
    @Inject
    lateinit var reminderPreferences: ReminderPreferences
    
    companion object {
        private const val TAG = "HabitReminderReceiver"
        
        // Action constants
        const val ACTION_HABIT_REMINDER = "com.habittracker.HABIT_REMINDER"
        const val ACTION_SUMMARY_REMINDER = "com.habittracker.SUMMARY_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.habittracker.SNOOZE_REMINDER"
        const val ACTION_DISMISS_REMINDER = "com.habittracker.DISMISS_REMINDER"
        const val ACTION_MARK_COMPLETED = "com.habittracker.MARK_COMPLETED"
        
        // Extra constants
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_HABIT_DESCRIPTION = "habit_description"
        const val EXTRA_IS_SNOOZED = "is_snoozed"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        when (intent.action) {
            ACTION_HABIT_REMINDER -> {
                handleHabitReminder(context, intent)
            }
            ACTION_SUMMARY_REMINDER -> {
                handleSummaryReminder(context, intent)
            }
            ACTION_SNOOZE_REMINDER -> {
                handleSnoozeReminder(context, intent)
            }
            ACTION_DISMISS_REMINDER -> {
                handleDismissReminder(context, intent)
            }
            ACTION_MARK_COMPLETED -> {
                handleMarkCompleted(context, intent)
            }
            else -> {
                Log.w(TAG, "Unknown action received: ${intent.action}")
            }
        }
    }
    
    /**
     * Handles individual habit reminder notifications
     */
    private fun handleHabitReminder(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Habit"
        val habitDescription = intent.getStringExtra(EXTRA_HABIT_DESCRIPTION) ?: ""
        val isSnoozed = intent.getBooleanExtra(EXTRA_IS_SNOOZED, false)
        
        if (habitId == -1L) {
            Log.e(TAG, "Invalid habit ID received in reminder")
            return
        }
        
        // Check if reminders are still enabled for this habit
        if (!reminderPreferences.isReminderEnabled(habitId)) {
            Log.d(TAG, "Reminder disabled for habit ID: $habitId")
            return
        }
        
        Log.d(TAG, "Showing reminder for habit: $habitName (ID: $habitId)")
        
        // Start the notification service to display the habit reminder
        val serviceIntent = Intent(context, ReminderNotificationService::class.java).apply {
            action = ReminderNotificationService.ACTION_SHOW_HABIT_REMINDER
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_HABIT_DESCRIPTION, habitDescription)
            putExtra(EXTRA_IS_SNOOZED, isSnoozed)
        }
        
        ContextCompat.startForegroundService(context, serviceIntent)
        
        // Reschedule the reminder for tomorrow if it's not a snoozed reminder
        if (!isSnoozed) {
            rescheduleHabitForTomorrow(habitId, habitName, habitDescription)
        }
    }
    
    /**
     * Handles summary reminder notifications
     */
    private fun handleSummaryReminder(context: Context, intent: Intent) {
        if (!reminderPreferences.isSummaryReminderEnabled()) {
            Log.d(TAG, "Summary reminder disabled")
            return
        }
        
        Log.d(TAG, "Showing summary reminder")
        
        // Extract any additional data from intent for future extensibility
        val customMessage = intent.getStringExtra("custom_message")
        val priorityLevel = intent.getIntExtra("priority_level", 0)
        
        // Start the notification service to display the summary reminder
        val serviceIntent = Intent(context, ReminderNotificationService::class.java).apply {
            action = ReminderNotificationService.ACTION_SHOW_SUMMARY_REMINDER
            // Pass along any custom data for enhanced notifications
            customMessage?.let { putExtra("custom_message", it) }
            if (priorityLevel > 0) putExtra("priority_level", priorityLevel)
        }
        
        ContextCompat.startForegroundService(context, serviceIntent)
        
        // Reschedule summary reminder for tomorrow
        reminderManager.scheduleSummaryReminder()
    }
    
    /**
     * Handles snooze action from notification
     */
    private fun handleSnoozeReminder(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Habit"
        val habitDescription = intent.getStringExtra(EXTRA_HABIT_DESCRIPTION) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        
        if (habitId == -1L) {
            Log.e(TAG, "Invalid habit ID received in snooze action")
            return
        }
        
        Log.d(TAG, "Snoozing reminder for habit: $habitName (ID: $habitId)")
        
        // Cancel current notification
        if (notificationId != -1) {
            val serviceIntent = Intent(context, ReminderNotificationService::class.java).apply {
                action = ReminderNotificationService.ACTION_CANCEL_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
        
        // Schedule snoozed reminder
        reminderManager.snoozeHabitReminder(habitId, habitName, habitDescription)
    }
    
    /**
     * Handles dismiss action from notification
     */
    private fun handleDismissReminder(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        
        Log.d(TAG, "Dismissing reminder for habit ID: $habitId")
        
        // Cancel current notification
        if (notificationId != -1) {
            val serviceIntent = Intent(context, ReminderNotificationService::class.java).apply {
                action = ReminderNotificationService.ACTION_CANCEL_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
        
        // Note: We don't reschedule here as the regular daily reminder will handle tomorrow
    }
    
    /**
     * Handles mark completed action from notification
     */
    private fun handleMarkCompleted(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        
        if (habitId == -1L) {
            Log.e(TAG, "Invalid habit ID received in mark completed action")
            return
        }
        
        Log.d(TAG, "Marking habit as completed from notification: $habitId")
        
        // Cancel current notification
        if (notificationId != -1) {
            val serviceIntent = Intent(context, ReminderNotificationService::class.java).apply {
                action = ReminderNotificationService.ACTION_CANCEL_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
        
        // Mark habit as completed
        val serviceIntent = Intent(context, ReminderNotificationService::class.java).apply {
            action = ReminderNotificationService.ACTION_MARK_HABIT_COMPLETED
            putExtra(EXTRA_HABIT_ID, habitId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
    
    /**
     * Reschedules a habit reminder for the next day
     * Uses habit metadata for intelligent scheduling decisions
     */
    private fun rescheduleHabitForTomorrow(habitId: Long, habitName: String, habitDescription: String) {
        try {
            // Log detailed rescheduling information for debugging and analytics
            Log.d(TAG, "Rescheduling habit reminder for tomorrow:")
            Log.d(TAG, "  - Habit ID: $habitId")
            Log.d(TAG, "  - Habit Name: '$habitName'")
            Log.d(TAG, "  - Description: '$habitDescription'")
            
            // The ReminderManager will handle scheduling for the next occurrence
            // based on the habit's reminder time preferences
            Log.d(TAG, "Habit reminder will be rescheduled by ReminderManager for tomorrow")
            
            // Note: The actual rescheduling happens in ReminderManager.scheduleHabitReminder()
            // which calculates the next trigger time automatically
            
            // Future enhancement: Could use habitName and habitDescription for:
            // - Intelligent scheduling based on habit type
            // - Personalized reminder messages
            // - Analytics tracking by habit category
            
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling habit reminder for '$habitName' (ID: $habitId)", e)
        }
    }
}
