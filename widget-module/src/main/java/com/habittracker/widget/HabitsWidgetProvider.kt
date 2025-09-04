package com.habittracker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.*
import com.habittracker.widget.cache.WidgetCacheManager
import com.habittracker.widget.animation.WidgetAnimationController
import com.habittracker.widget.performance.WidgetPerformanceOptimizer
import com.habittracker.widget.error.WidgetErrorHandler
import com.habittracker.widget.scheduler.WidgetUpdateScheduler
import com.habittracker.widget.analytics.WidgetAnalytics
import com.habittracker.widget.R
import com.habittracker.core.WidgetHabitRepository

class HabitsWidgetProvider : AppWidgetProvider() {

    // Enhanced widget scope with performance optimization
    private val widgetScope = CoroutineScope(
        SupervisorJob() + 
        Dispatchers.Main.immediate +
        kotlinx.coroutines.CoroutineName("WidgetProvider")
    )

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Track widget update requests
        runBlocking {
            WidgetAnalytics.getInstance(context).trackInteraction(
                WidgetAnalytics.InteractionType.REFRESH_CLICK,
                "widget_update_requested",
                mapOf("widget_count" to appWidgetIds.size.toString())
            )
        }
        
        appWidgetIds.forEach { appWidgetId ->
            widgetScope.launch {
                try {
                    // Use performance optimizer for efficient execution
                    WidgetPerformanceOptimizer.getInstance(context).optimizedExecution(
                        operationName = "widget_update_$appWidgetId"
                    ) {
                        updateWidgetInternal(context, appWidgetManager, appWidgetId)
                    }.onSuccess { 
                        // Track successful update performance
                        WidgetAnalytics.getInstance(context).trackPerformance(
                            "widget_update_success",
                            System.currentTimeMillis(),
                            true,
                            mapOf(
                                "widget_id" to appWidgetId.toString()
                            )
                        )
                    }.onFailure { exception ->
                        // Track failed update
                        WidgetAnalytics.getInstance(context).trackPerformance(
                            "widget_update_failure",
                            0L,
                            false,
                            mapOf(
                                "widget_id" to appWidgetId.toString(),
                                "error_type" to exception.javaClass.simpleName,
                                "error_message" to (exception.message ?: "Unknown error")
                            )
                        )
                        
                        // Use error handler for graceful degradation
                        WidgetAnalytics.getInstance(context).trackError(
                            exception.javaClass.simpleName,
                            exception.message ?: "Unknown error",
                            WidgetAnalytics.ErrorSeverity.HIGH,
                            mapOf("widget_id" to appWidgetId.toString())
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WidgetProvider", "Error updating widget $appWidgetId", e)
                }
            }
        }
    }

    private suspend fun updateWidgetInternal(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        try {
            // Create basic widget views
            val views = createWidgetViews(context, appWidgetId)
            
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "Error updating widget $appWidgetId", e)
            
            // Handle error with basic fallback
            val errorViews = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
            errorViews.setTextViewText(android.R.id.text1, "Error loading widget")
            appWidgetManager.updateAppWidget(appWidgetId, errorViews)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_REFRESH -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (appWidgetId != -1) {
                    handleRefreshAction(context, appWidgetId)
                }
            }
            ACTION_TOGGLE_HABIT -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                if (habitId != -1L) {
                    handleToggleHabit(context, habitId)
                }
            }
        }
    }

    private fun handleRefreshAction(context: Context, appWidgetId: Int) {
        widgetScope.launch {
            try {
                val cacheManager = WidgetCacheManager.getInstance(context)
                val updateScheduler = WidgetUpdateScheduler.getInstance(context)
                
                // Clear cache for fresh data
                cacheManager.invalidateCache()
                
                // Schedule immediate update
                updateScheduler.scheduleUpdate(
                    operationName = "refresh_$appWidgetId",
                    priority = WidgetUpdateScheduler.UpdatePriority.HIGH,
                    operation = {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        updateWidgetInternal(context, appWidgetManager, appWidgetId)
                    }
                )
                
                // Track refresh interaction
                WidgetAnalytics.getInstance(context).trackInteraction(
                    WidgetAnalytics.InteractionType.REFRESH_CLICK,
                    "manual_refresh",
                    mapOf("widget_id" to appWidgetId.toString())
                )
                
            } catch (e: Exception) {
                // Track error
                WidgetAnalytics.getInstance(context).trackError(
                    e.javaClass.simpleName,
                    e.message ?: "Unknown error",
                    WidgetAnalytics.ErrorSeverity.MEDIUM,
                    mapOf("widget_id" to appWidgetId.toString())
                )
            }
        }
    }

    private fun handleToggleHabit(context: Context, habitId: Long) {
        widgetScope.launch {
            try {
                val updateScheduler = WidgetUpdateScheduler.getInstance(context)
                
                // Here you would typically call your habit repository
                // For now, we'll just track the interaction
                
                // Track habit toggle interaction
                WidgetAnalytics.getInstance(context).trackInteraction(
                    WidgetAnalytics.InteractionType.HABIT_TOGGLE,
                    "habit_toggled",
                    mapOf("habit_id" to habitId.toString())
                )
                
                // Schedule widget updates after habit state change
                updateScheduler.scheduleUpdate(
                    operationName = "habit_toggle_$habitId",
                    priority = WidgetUpdateScheduler.UpdatePriority.MEDIUM,
                    operation = {
                        // Update all widgets
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val appWidgetIds = getAllWidgetIds(context)
                        appWidgetIds.forEach { appWidgetId ->
                            updateWidgetInternal(context, appWidgetManager, appWidgetId)
                        }
                    }
                )
                
            } catch (e: Exception) {
                WidgetAnalytics.getInstance(context).trackError(
                    e.javaClass.simpleName,
                    e.message ?: "Unknown error",
                    WidgetAnalytics.ErrorSeverity.MEDIUM,
                    mapOf("habit_id" to habitId.toString())
                )
            }
        }
    }

    private suspend fun createWidgetViews(context: Context, appWidgetId: Int): RemoteViews {
        return try {
            // Create base layout (use a simple layout that exists)
            val views = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
            
            // Get habit data (mock data for now)
            val habitData = getHabitData(context)
            
            // Update views with data
            updateViewsWithData(context, views, habitData, appWidgetId)
            
            views
        } catch (e: Exception) {
            WidgetErrorHandler.getInstance(context).createErrorView(context, e)
        }
    }

    private suspend fun getHabitData(context: Context): HabitProgressStats {
        // This would typically fetch from your repository; provide mock values for basics
        val repo = WidgetHabitRepository.getInstance(context)
        val timersEnabled = try { repo.getTimerEnabledCount() } catch (_: Exception) { 0 }
        val nextTime = try { repo.getNextSuggestedTime() } catch (_: Exception) { null }
        return HabitProgressStats(
            completedToday = 3,
            totalToday = 5,
            weeklyStreak = 7,
            monthlyCompletion = 85.5f,
            timersEnabledCount = timersEnabled,
            nextSuggested = nextTime
        )
    }

    private fun updateViewsWithData(
        context: Context, 
        views: RemoteViews, 
        data: HabitProgressStats, 
        appWidgetId: Int
    ) {
        // Update text views with basic layout
    // Minimal timing surface: append compact timers indicator
    val timersIndicator = if (data.timersEnabledCount > 0) " • Timers On (${data.timersEnabledCount})" else " • Timers Off"
    val nextTimeText = data.nextSuggested?.let { " • Next: ${it.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))}" } ?: ""
    views.setTextViewText(android.R.id.text1, "${data.completedToday}/${data.totalToday} habits completed$timersIndicator$nextTimeText")
    // Colorize text when timers are on
    val colorRes = if (data.timersEnabledCount > 0) R.color.widgetAccentColor else R.color.widgetTextColor
    views.setTextColor(android.R.id.text1, androidx.core.content.ContextCompat.getColor(context, colorRes))
        
        // Set up click handlers
        setupClickHandlers(context, views, appWidgetId)
    }

    private fun setupClickHandlers(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Refresh button
        val refreshIntent = Intent(context, HabitsWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = android.app.PendingIntent.getBroadcast(
            context, appWidgetId, refreshIntent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(android.R.id.text1, refreshPendingIntent)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        
        widgetScope.launch {
            try {
                // Track widget enabled
                WidgetAnalytics.getInstance(context).trackBehavior(
                    WidgetAnalytics.BehaviorType.WIDGET_ENABLED,
                    "Widget enabled at ${System.currentTimeMillis()}"
                )
                
                // Initialize performance optimizer
                WidgetPerformanceOptimizer.getInstance(context).initialize(context)
                
                // Start update scheduler
                WidgetUpdateScheduler.getInstance(context).initialize(context)
                
            } catch (e: Exception) {
                WidgetAnalytics.getInstance(context).trackError(
                    e.javaClass.simpleName,
                    e.message ?: "Unknown error", 
                    WidgetAnalytics.ErrorSeverity.LOW,
                    mapOf("context" to "widget_enabled")
                )
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        
        widgetScope.launch {
            try {
                // Track widget disabled
                WidgetAnalytics.getInstance(context).trackBehavior(
                    WidgetAnalytics.BehaviorType.WIDGET_DISABLED,
                    "Widget disabled at ${System.currentTimeMillis()}"
                )
                
                // Clean up resources
                WidgetCacheManager.getInstance(context).clearAll()
                WidgetPerformanceOptimizer.getInstance(context) // cleanup is private, skip it
                WidgetUpdateScheduler.getInstance(context) // cleanup is not available, skip it
                
            } catch (e: Exception) {
                WidgetAnalytics.getInstance(context).trackError(
                    e.javaClass.simpleName,
                    e.message ?: "Unknown error",
                    WidgetAnalytics.ErrorSeverity.LOW,
                    mapOf("context" to "widget_disabled")
                )
            }
        }
        
        // Cancel all coroutines
        widgetScope.cancel()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        
        widgetScope.launch {
            try {
                val cacheManager = WidgetCacheManager.getInstance(context)
                
                appWidgetIds.forEach { appWidgetId ->
                    // Clean up widget-specific data - skip if method doesn't exist
                    // cacheManager.removeCache(appWidgetId)
                    
                    // Track widget deletion
                    WidgetAnalytics.getInstance(context).trackBehavior(
                        WidgetAnalytics.BehaviorType.WIDGET_DELETED,
                        "Widget $appWidgetId deleted at ${System.currentTimeMillis()}"
                    )
                }
            } catch (e: Exception) {
                WidgetAnalytics.getInstance(context).trackError(
                    e.javaClass.simpleName,
                    e.message ?: "Unknown error",
                    WidgetAnalytics.ErrorSeverity.LOW,
                    mapOf("context" to "widget_deleted")
                )
            }
        }
    }

    // Helper method to get all widget IDs
    private fun getAllWidgetIds(context: Context): IntArray {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = android.content.ComponentName(context, HabitsWidgetProvider::class.java)
        return appWidgetManager.getAppWidgetIds(provider)
    }

    // Helper function to convert HabitProgressStats to ProgressStats for compatibility
    private fun convertToProgressStats(habitStats: HabitProgressStats): ProgressStats {
        return ProgressStats(
            completedCount = habitStats.completedToday,
            totalCount = habitStats.totalToday,
            completionPercentage = habitStats.monthlyCompletion / 100f
        )
    }

    companion object {
        const val ACTION_REFRESH = "com.habittracker.widget.ACTION_REFRESH"
        const val ACTION_TOGGLE_HABIT = "com.habittracker.widget.ACTION_TOGGLE_HABIT"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        
        // Performance constants
        private const val UPDATE_THROTTLE_MS = 1000L // Minimum time between updates
        private const val MAX_CONCURRENT_UPDATES = 3 // Limit concurrent operations
    }

    // Data class for progress statistics (if not already defined elsewhere)
    data class ProgressStats(
        val completedCount: Int,
        val totalCount: Int,
        val completionPercentage: Float
    )

    // Data class for habit progress statistics
    data class HabitProgressStats(
        val completedToday: Int,
        val totalToday: Int,
        val weeklyStreak: Int,
        val monthlyCompletion: Float,
        val timersEnabledCount: Int,
        val nextSuggested: java.time.LocalTime?
    )
}
