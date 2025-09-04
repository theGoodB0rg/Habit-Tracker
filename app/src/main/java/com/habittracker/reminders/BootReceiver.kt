package com.habittracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver that handles device boot completed events.
 * Reschedules all habit reminders after device restart.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var reminderManager: ReminderManager
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Device boot completed - rescheduling reminders")
                handleBootCompleted()
            }
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                if (intent.dataString?.contains(context.packageName) == true) {
                    Log.d(TAG, "App package replaced - rescheduling reminders")
                    handlePackageReplaced()
                }
            }
            else -> {
                Log.d(TAG, "Received unexpected action: ${intent.action}")
            }
        }
    }
    
    /**
     * Handles device boot completed event
     */
    private fun handleBootCompleted() {
        try {
            // Reschedule all habit reminders
            reminderManager.rescheduleAllReminders()
            Log.d(TAG, "Successfully rescheduled all reminders after boot")
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling reminders after boot", e)
        }
    }
    
    /**
     * Handles app package replacement (update) event
     */
    private fun handlePackageReplaced() {
        try {
            // Reschedule all habit reminders after app update
            reminderManager.rescheduleAllReminders()
            Log.d(TAG, "Successfully rescheduled all reminders after app update")
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling reminders after app update", e)
        }
    }
}
