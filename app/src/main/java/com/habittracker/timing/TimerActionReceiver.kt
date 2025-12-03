package com.habittracker.timing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.habittracker.timerux.TimerActionCoordinator
import com.habittracker.timerux.TimerCompletionInteractor.Intent as TimerIntent
import com.habittracker.timerux.resolveTimerUxEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Unified BroadcastReceiver for timer actions from external surfaces (widgets, notifications).
 * Routes all actions through TimerActionCoordinator for consistent debouncing and confirmation flows.
 * 
 * This receiver provides parity between in-app timer controls and external surfaces by:
 * 1. Applying the same 500ms debounce as in-app controls
 * 2. Emitting telemetry through the coordinator
 * 3. Maintaining single-active-timer enforcement
 * 
 * Note: Since widgets can't show dialogs, confirmation-required actions will:
 * - For COMPLETE with below minimum: auto-complete (skip confirmation)
 * - For DISCARD: auto-discard (skip confirmation)
 * 
 * The coordinator's confirmation override is used to bypass dialog prompts for external surfaces.
 */
class TimerActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TimerActionReceiver"
        
        // Public action constants for external surfaces to use
        const val ACTION_TIMER_START = "com.habittracker.timer.action.COORDINATED_START"
        const val ACTION_TIMER_PAUSE = "com.habittracker.timer.action.COORDINATED_PAUSE"
        const val ACTION_TIMER_RESUME = "com.habittracker.timer.action.COORDINATED_RESUME"
        const val ACTION_TIMER_COMPLETE = "com.habittracker.timer.action.COORDINATED_COMPLETE"
        const val ACTION_TIMER_EXTEND = "com.habittracker.timer.action.COORDINATED_EXTEND"
        const val ACTION_TIMER_DISCARD = "com.habittracker.timer.action.COORDINATED_DISCARD"
        
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_EXTEND_MINUTES = "extra_extend_minutes"
        
        // Source identifiers for telemetry
        const val SOURCE_WIDGET = "widget"
        const val SOURCE_NOTIFICATION = "notification"
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: "unknown"
        
        Log.d(TAG, "Received action=$action, habitId=$habitId, source=$source")
        
        // Get coordinator via entry point (safe for non-Hilt contexts like BroadcastReceiver)
        val handler = try {
            resolveTimerUxEntryPoint(context).timerActionHandler()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve TimerActionHandler, falling back to service", e)
            // Fallback: forward to TimerService directly if coordinator unavailable
            forwardToService(context, action, habitId, intent)
            return
        }
        
        // Build decision context for external surfaces (skip confirmations)
        val decisionContext = when (action) {
            ACTION_TIMER_COMPLETE -> TimerActionCoordinator.DecisionContext(
                platform = com.habittracker.timerux.TimerCompletionInteractor.Platform.WIDGET,
                confirmation = TimerActionCoordinator.ConfirmationOverride.COMPLETE_WITHOUT_TIMER
            )
            ACTION_TIMER_DISCARD -> TimerActionCoordinator.DecisionContext(
                platform = com.habittracker.timerux.TimerCompletionInteractor.Platform.WIDGET,
                confirmation = TimerActionCoordinator.ConfirmationOverride.DISCARD_SESSION
            )
            else -> TimerActionCoordinator.DecisionContext(
                platform = com.habittracker.timerux.TimerCompletionInteractor.Platform.WIDGET
            )
        }
        
        when (action) {
            ACTION_TIMER_START -> {
                if (habitId > 0) {
                    handler.handle(TimerIntent.Start, habitId, decisionContext)
                } else {
                    Log.w(TAG, "START action requires valid habitId")
                }
            }
            ACTION_TIMER_PAUSE -> {
                // Pause doesn't need habitId - pauses current active timer
                val currentHabitId = handler.state.value.trackedHabitId
                if (currentHabitId != null) {
                    handler.handle(TimerIntent.Pause, currentHabitId, decisionContext)
                } else {
                    Log.w(TAG, "No active timer to pause")
                }
            }
            ACTION_TIMER_RESUME -> {
                val currentHabitId = handler.state.value.trackedHabitId
                if (currentHabitId != null) {
                    handler.handle(TimerIntent.Resume, currentHabitId, decisionContext)
                } else {
                    Log.w(TAG, "No active timer to resume")
                }
            }
            ACTION_TIMER_COMPLETE -> {
                val targetHabitId = if (habitId > 0) habitId else handler.state.value.trackedHabitId
                if (targetHabitId != null) {
                    handler.handle(TimerIntent.Done, targetHabitId, decisionContext)
                } else {
                    Log.w(TAG, "No timer to complete")
                }
            }
            ACTION_TIMER_DISCARD -> {
                val targetHabitId = if (habitId > 0) habitId else handler.state.value.trackedHabitId
                if (targetHabitId != null) {
                    handler.handle(TimerIntent.StopWithoutComplete, targetHabitId, decisionContext)
                } else {
                    Log.w(TAG, "No timer to discard")
                }
            }
            ACTION_TIMER_EXTEND -> {
                // Extension is handled directly by service for now
                forwardToService(context, action, habitId, intent)
            }
            else -> {
                Log.w(TAG, "Unknown action: $action")
            }
        }
    }
    
    /**
     * Fallback: forward action directly to TimerService when coordinator is unavailable.
     */
    private fun forwardToService(context: Context, action: String, habitId: Long, intent: Intent) {
        val serviceAction = when (action) {
            ACTION_TIMER_START -> TimerService.ACTION_START
            ACTION_TIMER_PAUSE -> TimerService.ACTION_PAUSE
            ACTION_TIMER_RESUME -> TimerService.ACTION_RESUME
            ACTION_TIMER_COMPLETE -> TimerService.ACTION_COMPLETE
            ACTION_TIMER_EXTEND -> {
                val minutes = intent.getIntExtra(EXTRA_EXTEND_MINUTES, 5)
                if (minutes == 1) TimerService.ACTION_ADD_1M else TimerService.ACTION_EXTEND_5M
            }
            ACTION_TIMER_DISCARD -> TimerService.ACTION_STOP
            else -> return
        }
        
        val serviceIntent = Intent(context, TimerService::class.java).apply {
            this.action = serviceAction
            if (habitId > 0) putExtra(TimerService.EXTRA_HABIT_ID, habitId)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= 26 && action == ACTION_TIMER_START) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
