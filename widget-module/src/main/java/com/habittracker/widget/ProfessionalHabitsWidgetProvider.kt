package com.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.habittracker.core.WidgetHabitRepository
import com.habittracker.widget.cache.WidgetCacheManager
import com.habittracker.widget.scheduler.WidgetUpdateScheduler
import com.habittracker.widget.scheduler.WidgetUpdateScheduler.UpdatePriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Professional AppWidgetProvider with full functionality:
 * - Real database integration
 * - Interactive habit completion
 * - Progress tracking
 * - Professional UI/UX
 * - Accessibility compliance
 * - Error handling
 */
class ProfessionalHabitsWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_TOGGLE_HABIT = "com.habittracker.widget.TOGGLE_HABIT"
        const val ACTION_REFRESH = "com.habittracker.widget.REFRESH"
        const val ACTION_OPEN_APP = "com.habittracker.widget.OPEN_APP"
    const val ACTION_TIMER_START = "com.habittracker.widget.TIMER_START"
    const val ACTION_TIMER_PAUSE = "com.habittracker.widget.TIMER_PAUSE"
    const val ACTION_TIMER_RESUME = "com.habittracker.widget.TIMER_RESUME"
    const val ACTION_TIMER_DONE = "com.habittracker.widget.TIMER_DONE"
    const val EXTRA_HABIT_ID = "habit_id"
    const val EXTRA_WIDGET_ID = "widget_id"

    // Coordinated action constants (routed through TimerActionReceiver for debouncing/confirmation)
    private const val COORDINATED_ACTION_START = "com.habittracker.timer.action.COORDINATED_START"
    private const val COORDINATED_ACTION_PAUSE = "com.habittracker.timer.action.COORDINATED_PAUSE"
    private const val COORDINATED_ACTION_RESUME = "com.habittracker.timer.action.COORDINATED_RESUME"
    private const val COORDINATED_ACTION_COMPLETE = "com.habittracker.timer.action.COORDINATED_COMPLETE"
    private const val COORDINATED_EXTRA_HABIT_ID = "extra_habit_id"
    private const val COORDINATED_EXTRA_SOURCE = "extra_source"

    // Mirror TimerService constants locally to avoid module dependency (fallback only)
    private const val TS_ACTION_START = "com.habittracker.timing.action.START"
    private const val TS_ACTION_PAUSE = "com.habittracker.timing.action.PAUSE"
    private const val TS_ACTION_RESUME = "com.habittracker.timing.action.RESUME"
    private const val TS_ACTION_COMPLETE = "com.habittracker.timing.action.COMPLETE"
    private const val TS_EXTRA_HABIT_ID = "extra_habit_id"
    }
    
    override fun onUpdate(
        context: Context, 
        appWidgetManager: AppWidgetManager, 
        appWidgetIds: IntArray
    ) {
        // Update all widget instances
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // First widget instance added - initialize if needed
        // Initialize smart scheduler for debounced/throttled updates
        try {
            WidgetUpdateScheduler.getInstance(context).initialize(context)
        } catch (e: Exception) {
            android.util.Log.w("WidgetProvider", "Failed to initialize scheduler: ${e.message}")
        }
        schedulePeriodicUpdates(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Last widget instance removed - cleanup if needed
        cancelPeriodicUpdates(context)
    }
    
    /**
     * Core widget update method with professional implementation
     */
    private fun updateWidget(
        context: Context, 
        appWidgetManager: AppWidgetManager, 
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_habits)
            
            // Set up dynamic header with current date
            updateWidgetHeader(context, views)
            
            // Set up ListView with real habit data
            setupHabitsListView(context, views, appWidgetId)
            
            // Set up interactive buttons
            setupWidgetActions(context, views, appWidgetId)
            
            // Update progress indicators
            updateProgressDisplay(context, views)
            
            // Apply error handling and accessibility
            setupAccessibility(views)
            
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // Notify ListView to refresh
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.habits_list)
            
        } catch (e: Exception) {
            // Fallback error state
            showErrorState(context, appWidgetManager, appWidgetId, e)
        }
    }
    
    /**
     * Setup dynamic header with date and branding
     */
    private fun updateWidgetHeader(context: Context, views: RemoteViews) {
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
        val formattedDate = today.format(dateFormatter)
        
        views.setTextViewText(
            R.id.widget_title, 
            "📊 My Habits - $formattedDate"
        )
        
        // Set header styling
        views.setTextColor(
            R.id.widget_title, 
            ContextCompat.getColor(context, R.color.widget_text_primary)
        )
    }
    
    /**
     * Setup ListView with real database connection
     */
    private fun setupHabitsListView(
        context: Context, 
        views: RemoteViews, 
        appWidgetId: Int
    ) {
        // Create service intent for ListView adapter
        val serviceIntent = Intent(context, ProfessionalHabitsWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // Add unique data to force refresh
            data = Uri.parse("content://widget/habits/$appWidgetId/${System.currentTimeMillis()}")
        }
        
        // Set remote adapter for ListView
        views.setRemoteAdapter(R.id.habits_list, serviceIntent)
        
        // Set empty view for when no habits exist
        views.setEmptyView(R.id.habits_list, R.id.empty_state_text)
        
        // Set up click template for habit items
    val habitClickIntent = Intent(context, ProfessionalHabitsWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_HABIT
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            // Add a unique data URI to ensure each widget instance has a unique template
            data = Uri.parse("widget://habit/toggle/$appWidgetId")
        }
        
        val habitClickPendingIntent = PendingIntent.getBroadcast(
            context, 
            appWidgetId, 
            habitClickIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        views.setPendingIntentTemplate(R.id.habits_list, habitClickPendingIntent)

    // Note: A single PendingIntent template is sufficient; individual row views use fill-in intents
    }
    
    /**
     * Setup interactive buttons and actions
     */
    private fun setupWidgetActions(
        context: Context, 
        views: RemoteViews, 
        appWidgetId: Int
    ) {
        // Refresh button action
        val refreshIntent = Intent(context, ProfessionalHabitsWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
        }
        
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 
            appWidgetId + 1000, // Unique request code
            refreshIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)
        
        // Open main app action (on title click)
        val openAppIntent = Intent().apply {
            action = ACTION_OPEN_APP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Try to open main app activity
            setClassName(
                context.packageName.replace(".widget", ""), 
                "${context.packageName.replace(".widget", "")}.MainActivity"
            )
        }
        
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 
            appWidgetId + 2000, // Unique request code
            openAppIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)
    }
    
    /**
     * Update progress indicators with real data
     */
    private fun updateProgressDisplay(context: Context, views: RemoteViews) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WidgetHabitRepository.getInstance(context)
                val habits = repository.getTodaysHabits()
                val completed = habits.count { it.isCompleted }
                val total = habits.size
                val percentage = if (total > 0) (completed * 100 / total) else 0
                // Minimal timing surface: count how many habits have timers enabled
                val timersEnabledCount = try {
                    repository.getTimerEnabledCount()
                } catch (e: Exception) { 0 }
                // Next suggested time (if available)
                val nextSuggested = try {
                    repository.getNextSuggestedTime()
                } catch (e: Exception) { null }
                
                // Update UI on main thread
                CoroutineScope(Dispatchers.Main).launch {
                    // Progress indicator in header
                    val progressText = "$completed/$total ($percentage%)"
                    val timersIndicator = if (timersEnabledCount > 0) " • Timers On ($timersEnabledCount)" else " • Timers Off"
                    views.setTextViewText(
                        R.id.progress_indicator,
                        progressText + timersIndicator
                    )

                    // Update visual progress bar
                    try {
                        if (total > 0) {
                            views.setViewVisibility(R.id.daily_progress_bar, android.view.View.VISIBLE)
                            views.setInt(R.id.daily_progress_bar, "setProgress", percentage)
                        } else {
                            views.setViewVisibility(R.id.daily_progress_bar, android.view.View.GONE)
                        }
                    } catch (_: Exception) { /* ignore */ }

                    // Dedicated line for next suggested time
                    if (nextSuggested != null) {
                        val fmt = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
                        val nextStr = context.getString(R.string.widget_next_suggested_prefix) + " " + nextSuggested.format(fmt)
                        views.setTextViewText(R.id.next_suggested_time, nextStr)
                        views.setViewVisibility(R.id.next_suggested_time, android.view.View.VISIBLE)
                        views.setContentDescription(R.id.next_suggested_time, context.getString(R.string.widget_next_suggested_content_desc))
                    } else {
                        views.setViewVisibility(R.id.next_suggested_time, android.view.View.GONE)
                    }
                    
                    // Progress text in footer
                    val progressFooterText = when {
                        total == 0 -> "No habits configured"
                        percentage == 100 -> "🎉 All habits complete!"
                        percentage >= 75 -> "Great progress today!"
                        percentage >= 50 -> "$percentage% complete today"
                        percentage > 0 -> "Keep going - $percentage% done"
                        else -> "Start your first habit"
                    }
                    
                    views.setTextViewText(R.id.daily_progress, progressFooterText)
                    
                    // Color coding for progress
                    val progressColor = when {
                        percentage >= 80 -> R.color.progress_excellent
                        percentage >= 60 -> R.color.progress_good
                        percentage >= 30 -> R.color.progress_fair
                        else -> R.color.progress_poor
                    }
                    
                    // Colorize timers indicator when timers are on: mix accent with progress color
                    val indicatorColor = if (timersEnabledCount > 0) R.color.widgetAccentColor else progressColor
                    views.setTextColor(
                        R.id.progress_indicator, 
                        ContextCompat.getColor(context, indicatorColor)
                    )
                }
            } catch (e: Exception) {
                // Error state
                CoroutineScope(Dispatchers.Main).launch {
                    views.setTextViewText(R.id.progress_indicator, "Error")
                    views.setTextViewText(R.id.daily_progress, "Failed to load data")
                    views.setTextColor(
                        R.id.progress_indicator, 
                        ContextCompat.getColor(context, R.color.error_color)
                    )
                }
            }
        }
    }
    
    /**
     * Setup accessibility features
     */
    private fun setupAccessibility(views: RemoteViews) {
        views.setContentDescription(
            R.id.widget_title, 
            "Habit tracker widget. Tap to open main app."
        )
        
        views.setContentDescription(
            R.id.refresh_button, 
            "Refresh habits data"
        )
        
        views.setContentDescription(
            R.id.habits_list, 
            "List of today's habits. Tap checkboxes to mark complete."
        )
    }
    
    /**
     * Handle broadcast intents for widget interactions
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.d("WidgetProvider", "=== onReceive called ===")
        android.util.Log.d("WidgetProvider", "Action: ${intent.action}")
        android.util.Log.d("WidgetProvider", "Data: ${intent.data}")
        android.util.Log.d("WidgetProvider", "Extras: ${intent.extras?.keySet()?.map { "$it=${intent.extras?.get(it)}" }}")
        
        when (intent.action) {
            ACTION_TOGGLE_HABIT -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1)
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                
                android.util.Log.d("WidgetProvider", "🎯 Toggle habit request: habitId=$habitId, widgetId=$widgetId")
                
                if (habitId != -1L) {
                    handleHabitToggle(context, habitId, widgetId)
                } else {
                    android.util.Log.w("WidgetProvider", "❌ Invalid habit ID in toggle request")
                }
            }
            ACTION_TIMER_START -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1)
                if (habitId != -1L) launchTimerAction(context, habitId, ActionType.START)
            }
            ACTION_TIMER_PAUSE -> {
                launchTimerAction(context, -1, ActionType.PAUSE)
            }
            ACTION_TIMER_RESUME -> {
                launchTimerAction(context, -1, ActionType.RESUME)
            }
            ACTION_TIMER_DONE -> {
                launchTimerAction(context, -1, ActionType.DONE)
            }
            
            ACTION_REFRESH -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                android.util.Log.d("WidgetProvider", "🔄 Refresh request for widget: $widgetId")
                handleRefresh(context, widgetId)
            }
            
            // Handle system-wide updates
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                android.util.Log.d("WidgetProvider", "📅 System date/time changed, refreshing widgets")
                // Clear cache when date changes to force fresh data
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        WidgetCacheManager.getInstance(context).invalidateCache()
                    } catch (e: Exception) {
                        android.util.Log.w("WidgetProvider", "Failed to clear cache on date change: ${e.message}")
                    }
                }
                // Schedule a medium-priority refresh for all widgets
                scheduleWidgetUpdate(context, appWidgetId = null, priority = UpdatePriority.MEDIUM, reason = "system_time_change")
            }
            
            else -> {
                android.util.Log.d("WidgetProvider", "⚠️ Unhandled action: ${intent.action}")
                android.util.Log.d("WidgetProvider", "Intent details: action=${intent.action}, data=${intent.data}, extras=${intent.extras}")
            }
        }
    }

    private enum class ActionType { START, PAUSE, RESUME, DONE }

    /**
     * Launch timer action through coordinated receiver for consistent debouncing and confirmation flows.
     * Falls back to direct TimerService if broadcast fails.
     */
    private fun launchTimerAction(context: Context, habitId: Long, type: ActionType) {
        try {
            val appPackage = context.packageName.replace(".widget", "")
            
            // Prefer coordinated actions via broadcast receiver (provides debouncing + coordinator parity)
            val coordinatedAction = when (type) {
                ActionType.START -> COORDINATED_ACTION_START
                ActionType.PAUSE -> COORDINATED_ACTION_PAUSE
                ActionType.RESUME -> COORDINATED_ACTION_RESUME
                ActionType.DONE -> COORDINATED_ACTION_COMPLETE
            }
            
            val broadcastIntent = Intent(coordinatedAction).apply {
                setPackage(appPackage)
                putExtra(COORDINATED_EXTRA_HABIT_ID, habitId)
                putExtra(COORDINATED_EXTRA_SOURCE, "widget")
            }
            
            context.sendBroadcast(broadcastIntent)
            android.util.Log.d("WidgetProvider", "Timer action sent via coordinator: $coordinatedAction, habitId=$habitId")
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetProvider", "Coordinated timer action failed, falling back to service: ${e.message}")
            // Fallback to direct service call
            launchTimerActionFallback(context, habitId, type)
        }
    }
    
    /**
     * Fallback: Direct TimerService call when coordinated broadcast fails.
     */
    private fun launchTimerActionFallback(context: Context, habitId: Long, type: ActionType) {
        try {
            val appPackage = context.packageName.replace(".widget", "")
            val serviceClass = "$appPackage.timing.TimerService"
            when (type) {
                ActionType.START -> {
                    val i = Intent(TS_ACTION_START).apply {
                        setClassName(appPackage, serviceClass)
                        putExtra(TS_EXTRA_HABIT_ID, habitId)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
                }
                ActionType.PAUSE -> {
                    val i = Intent(TS_ACTION_PAUSE).apply { setClassName(appPackage, serviceClass) }
                    context.startService(i)
                }
                ActionType.RESUME -> {
                    val i = Intent(TS_ACTION_RESUME).apply { setClassName(appPackage, serviceClass) }
                    context.startService(i)
                }
                ActionType.DONE -> {
                    val i = Intent(TS_ACTION_COMPLETE).apply { setClassName(appPackage, serviceClass) }
                    context.startService(i)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("WidgetProvider", "Timer action fallback failed: ${e.message}")
        }
    }
    
    /**
     * Handle habit completion toggle with full functionality
     */
    private fun handleHabitToggle(context: Context, habitId: Long, widgetId: Int) {
        android.util.Log.d("WidgetProvider", "handleHabitToggle called: habitId=$habitId, widgetId=$widgetId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WidgetHabitRepository.getInstance(context)
                
                // Get habit details before toggle for logging
                val habitBefore = repository.getHabitById(habitId)
                android.util.Log.d("WidgetProvider", "Habit before toggle: ${habitBefore?.name}, completed: ${habitBefore?.isCompleted}")
                
                val newCompletionStatus = repository.toggleHabitCompletion(habitId)
                
                android.util.Log.d("WidgetProvider", "Toggle result: newCompletionStatus=$newCompletionStatus")
                
                // Clear cache to ensure fresh data
                WidgetCacheManager.getInstance(context).invalidateCache()
                
                // Schedule a critical-priority update via scheduler with optimistic UI
                scheduleWidgetUpdate(
                    context = context,
                    appWidgetId = widgetId.takeIf { it != -1 },
                    priority = UpdatePriority.CRITICAL,
                    reason = "toggle_${habitId}"
                )
                
                // Show user feedback
                val habit = repository.getHabitById(habitId)
                val message = if (newCompletionStatus) {
                    "✅ ${habit?.name ?: "Habit"} completed!"
                } else {
                    "⏺️ ${habit?.name ?: "Habit"} marked as not done"
                }
                provideFeedback(context, message)
                
            } catch (e: Exception) {
                android.util.Log.e("WidgetProvider", "Error toggling habit $habitId", e)
                // Handle error gracefully
                provideFeedback(context, "Failed to update habit")
            }
        }
    }
    
    /**
     * Handle refresh action
     */
    private fun handleRefresh(context: Context, widgetId: Int) {
        // Clear cache to force fresh data loading
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetCacheManager.getInstance(context).invalidateCache()
            } catch (e: Exception) {
                android.util.Log.w("WidgetProvider", "Failed to clear cache: ${e.message}")
            }
        }
        
        // Debounce refresh using the scheduler (HIGH priority for explicit refresh)
        scheduleWidgetUpdate(
            context = context,
            appWidgetId = widgetId.takeIf { it != -1 },
            priority = UpdatePriority.HIGH,
            reason = "manual_refresh"
        )
        
        provideFeedback(context, "Refreshed")
    }

    /**
     * Schedule a widget update through the smart scheduler with optional optimistic UI
     */
    private fun scheduleWidgetUpdate(
        context: Context,
        appWidgetId: Int?,
        priority: UpdatePriority,
        reason: String
    ) {
        val scheduler = WidgetUpdateScheduler.getInstance(context)
        // Record behavior context for predictive updates
        scheduler.recordUserAction("widget", reason)

        CoroutineScope(Dispatchers.Default).launch {
            val updateId = "${reason}_${System.currentTimeMillis()}"
            val appWidgetManager = AppWidgetManager.getInstance(context)

            scheduler.scheduleUpdate(
                updateId = updateId,
                priority = priority,
                updateAction = {
                    if (appWidgetId != null) {
                        updateWidget(context, appWidgetManager, appWidgetId)
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.habits_list)
                    } else {
                        val ids = appWidgetManager.getAppWidgetIds(
                            ComponentName(context, ProfessionalHabitsWidgetProvider::class.java)
                        )
                        ids.forEach { id ->
                            updateWidget(context, appWidgetManager, id)
                        }
                        ids.forEach { id ->
                            appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.habits_list)
                        }
                    }
                },
                optimisticUpdate = {
                    try {
                        if (appWidgetId != null) {
                            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.habits_list)
                        }
                    } catch (_: Exception) { }
                }
            )
        }
    }
    
    /**
     * Update specific widget instance
     */
    private fun updateSpecificWidget(context: Context, widgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateWidget(context, appWidgetManager, widgetId)
    }
    
    /**
     * Refresh all widget instances
     */
    private fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, ProfessionalHabitsWidgetProvider::class.java)
        )
        onUpdate(context, appWidgetManager, widgetIds)
    }
    
    /**
     * Show error state when widget fails to load
     */
    private fun showErrorState(
        context: Context, 
        appWidgetManager: AppWidgetManager, 
        appWidgetId: Int, 
        error: Exception
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_error_state)
        
        views.setTextViewText(
            R.id.error_title, 
            "⚠️ Widget Error"
        )
        
        views.setTextViewText(
            R.id.error_message, 
            "Failed to load habits data. Tap to retry."
        )
        
        // Retry button
        val retryIntent = Intent(context, ProfessionalHabitsWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
        }
        
        val retryPendingIntent = PendingIntent.getBroadcast(
            context, 
            appWidgetId + 3000, 
            retryIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        views.setOnClickPendingIntent(R.id.retry_button, retryPendingIntent)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    /**
     * Provide user feedback for actions
     */
    private fun provideFeedback(context: Context, message: String) {
        android.util.Log.d("WidgetProvider", "Feedback: $message")
        
        // Show toast feedback on main thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.widget.Toast.makeText(
                    context, 
                    message, 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.w("WidgetProvider", "Failed to show toast: ${e.message}")
            }
        }
    }
    
    /**
     * Schedule periodic updates for better data freshness
     */
    private fun schedulePeriodicUpdates(context: Context) {
        // Implementation could use AlarmManager for periodic updates
        // For now, rely on system update cycles
    }
    
    /**
     * Cancel periodic updates when no widgets remain
     */
    private fun cancelPeriodicUpdates(context: Context) {
        // Implementation to cancel any scheduled updates
    }
}
