package com.habittracker.reminders

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.habittracker.MainActivity
import com.habittracker.R
import com.habittracker.data.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Service that handles creating and managing habit reminder notifications.
 * Supports individual habit reminders, summary notifications, and quick actions.
 */
@AndroidEntryPoint
class ReminderNotificationService : Service() {
    
    @Inject
    lateinit var habitRepository: HabitRepository
    
    @Inject
    lateinit var reminderPreferences: ReminderPreferences
    
    @Inject
    lateinit var reminderManager: ReminderManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "ReminderNotificationService"
        
        // Action constants
        const val ACTION_SHOW_HABIT_REMINDER = "show_habit_reminder"
        const val ACTION_SHOW_SUMMARY_REMINDER = "show_summary_reminder"
        const val ACTION_CANCEL_NOTIFICATION = "cancel_notification"
        const val ACTION_MARK_HABIT_COMPLETED = "mark_habit_completed"
        
        // Notification constants
        private const val CHANNEL_ID_HABIT_REMINDERS = "habit_reminders"
        private const val CHANNEL_ID_SUMMARY = "summary_reminders"
        private const val NOTIFICATION_ID_BASE = 1000
        private const val NOTIFICATION_ID_SUMMARY = 999
        
        // Intent request codes
        private const val REQUEST_CODE_MAIN_ACTIVITY = 100
        private const val REQUEST_CODE_SNOOZE_BASE = 200
        private const val REQUEST_CODE_DISMISS_BASE = 300
        private const val REQUEST_CODE_COMPLETE_BASE = 400
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
    
    /**
     * Handles incoming intents and routes to appropriate action
     */
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_HABIT_REMINDER -> {
                showHabitReminder(intent)
            }
            ACTION_SHOW_SUMMARY_REMINDER -> {
                showSummaryReminder()
            }
            ACTION_CANCEL_NOTIFICATION -> {
                cancelNotification(intent)
            }
            ACTION_MARK_HABIT_COMPLETED -> {
                markHabitCompleted(intent)
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }
    }
    
    /**
     * Shows a notification for an individual habit reminder
     */
    private fun showHabitReminder(intent: Intent) {
        val habitId = intent.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(HabitReminderReceiver.EXTRA_HABIT_NAME) ?: "Habit"
        val habitDescription = intent.getStringExtra(HabitReminderReceiver.EXTRA_HABIT_DESCRIPTION) ?: ""
        val isSnoozed = intent.getBooleanExtra(HabitReminderReceiver.EXTRA_IS_SNOOZED, false)
        
        if (habitId == -1L) {
            Log.e(TAG, "Invalid habit ID for reminder")
            stopSelf()
            return
        }
        
        serviceScope.launch {
            try {
                // Verify habit still exists and is active
                val habit = habitRepository.getHabitById(habitId)
                if (habit == null || !habit.isActive) {
                    Log.w(TAG, "Habit no longer exists or is inactive: $habitId")
                    stopSelf()
                    return@launch
                }
                
                // Check if habit was already completed today
                val today = LocalDate.now()
                if (habit.lastCompletedDate == today) {
                    Log.d(TAG, "Habit already completed today: $habitName")
                    stopSelf()
                    return@launch
                }
                
                val notificationId = (NOTIFICATION_ID_BASE + habitId).toInt()
                val notification = createHabitReminderNotification(
                    habitId, habitName, habitDescription, isSnoozed, notificationId
                )
                
                val notificationManager = NotificationManagerCompat.from(this@ReminderNotificationService)
                if (notificationManager.areNotificationsEnabled()) {
                    notificationManager.notify(notificationId, notification)
                    Log.d(TAG, "Showed habit reminder notification: $habitName")
                } else {
                    Log.w(TAG, "Notifications are disabled")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing habit reminder", e)
            } finally {
                stopSelf()
            }
        }
    }
    
    /**
     * Shows a summary notification with all pending habits
     */
    private fun showSummaryReminder() {
        serviceScope.launch {
            try {
                val today = LocalDate.now()
                val allHabits = habitRepository.getAllHabits().first()
                
                // Find habits that haven't been completed today
                val pendingHabits = allHabits.filter { habit ->
                    habit.isActive && 
                    reminderPreferences.isReminderEnabled(habit.id) &&
                    habit.lastCompletedDate != today
                }
                
                if (pendingHabits.isEmpty()) {
                    Log.d(TAG, "No pending habits for summary reminder")
                    stopSelf()
                    return@launch
                }
                
                val notification = createSummaryNotification(pendingHabits.size, pendingHabits)
                
                val notificationManager = NotificationManagerCompat.from(this@ReminderNotificationService)
                if (notificationManager.areNotificationsEnabled()) {
                    notificationManager.notify(NOTIFICATION_ID_SUMMARY, notification)
                    Log.d(TAG, "Showed summary reminder notification for ${pendingHabits.size} habits")
                } else {
                    Log.w(TAG, "Notifications are disabled")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing summary reminder", e)
            } finally {
                stopSelf()
            }
        }
    }
    
    /**
     * Cancels a specific notification
     */
    private fun cancelNotification(intent: Intent) {
        val notificationId = intent.getIntExtra(HabitReminderReceiver.EXTRA_NOTIFICATION_ID, -1)
        
        if (notificationId != -1) {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.cancel(notificationId)
            Log.d(TAG, "Cancelled notification: $notificationId")
        }
        
        stopSelf()
    }
    
    /**
     * Marks a habit as completed from notification action
     */
    private fun markHabitCompleted(intent: Intent) {
        val habitId = intent.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L)
        
        if (habitId == -1L) {
            Log.e(TAG, "Invalid habit ID for mark completed")
            stopSelf()
            return
        }
        
        serviceScope.launch {
            try {
                // Mark habit as completed for today
                habitRepository.markHabitCompleted(habitId, LocalDate.now())
                
                // Cancel the notification
                val notificationId = (NOTIFICATION_ID_BASE + habitId).toInt()
                val notificationManager = NotificationManagerCompat.from(this@ReminderNotificationService)
                notificationManager.cancel(notificationId)
                
                Log.d(TAG, "Marked habit as completed from notification: $habitId")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error marking habit as completed", e)
            } finally {
                stopSelf()
            }
        }
    }
    
    /**
     * Creates a notification for an individual habit reminder
     */
    private fun createHabitReminderNotification(
        habitId: Long,
        habitName: String,
        habitDescription: String,
        isSnoozed: Boolean,
        notificationId: Int
    ): Notification {
        
        // Main intent to open the app
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, REQUEST_CODE_MAIN_ACTIVITY, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Snooze action
        val snoozeIntent = Intent(this, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_SNOOZE_REMINDER
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_DESCRIPTION, habitDescription)
            putExtra(HabitReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, REQUEST_CODE_SNOOZE_BASE + habitId.toInt(), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Dismiss action
        val dismissIntent = Intent(this, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_DISMISS_REMINDER
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, REQUEST_CODE_DISMISS_BASE + habitId.toInt(), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Mark completed action
        val completeIntent = Intent(this, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_MARK_COMPLETED
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            this, REQUEST_CODE_COMPLETE_BASE + habitId.toInt(), completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = if (isSnoozed) "⏰ Snoozed: $habitName" else "🎯 Time for: $habitName"
        val content = if (habitDescription.isNotEmpty()) habitDescription else "Don't break your streak!"
        
        return NotificationCompat.Builder(this, CHANNEL_ID_HABIT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_habit) // You'll need to add this icon
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(if (reminderPreferences.isNotificationSoundEnabled()) null else null)
            .setVibrate(if (reminderPreferences.isNotificationVibrationEnabled()) longArrayOf(0, 250, 250, 250) else null)
            .addAction(R.drawable.ic_check, "Done", completePendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .build()
    }
    
    /**
     * Creates a summary notification for multiple pending habits
     */
    private fun createSummaryNotification(
        habitCount: Int,
        pendingHabits: List<com.habittracker.data.database.entity.HabitEntity>
    ): Notification {
        
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, REQUEST_CODE_MAIN_ACTIVITY, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = "📋 Habit Reminder"
        val content = if (habitCount == 1) {
            "You have 1 habit to complete: ${pendingHabits.first().name}"
        } else {
            "You have $habitCount habits to complete today"
        }
        
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(buildString {
                append("Today's pending habits:\n")
                pendingHabits.take(5).forEach { habit ->
                    append("• ${habit.name}\n")
                }
                if (pendingHabits.size > 5) {
                    append("...and ${pendingHabits.size - 5} more")
                }
            })
        
        return NotificationCompat.Builder(this, CHANNEL_ID_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification_summary) // You'll need to add this icon
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(if (reminderPreferences.isNotificationSoundEnabled()) null else null)
            .setVibrate(if (reminderPreferences.isNotificationVibrationEnabled()) longArrayOf(0, 250, 250, 250) else null)
            .setStyle(bigTextStyle)
            .setNumber(habitCount)
            .build()
    }
    
    /**
     * Creates notification channels for different types of reminders
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Habit reminders channel
            val habitChannel = NotificationChannel(
                CHANNEL_ID_HABIT_REMINDERS,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Individual habit reminder notifications"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            // Summary reminders channel
            val summaryChannel = NotificationChannel(
                CHANNEL_ID_SUMMARY,
                "Summary Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily summary of pending habits"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(listOf(habitChannel, summaryChannel))
            Log.d(TAG, "Created notification channels")
        }
    }
}
