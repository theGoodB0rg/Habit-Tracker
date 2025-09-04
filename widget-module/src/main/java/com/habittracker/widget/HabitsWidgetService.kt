
package com.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.habittracker.core.WidgetHabitRepository
import com.habittracker.core.HabitWidgetData
import com.habittracker.widget.cache.WidgetCacheManager
import com.habittracker.widget.animation.WidgetAnimationController
import com.habittracker.widget.performance.WidgetPerformanceOptimizer
import com.habittracker.widget.error.WidgetErrorHandler
import com.habittracker.widget.scheduler.WidgetUpdateScheduler
import com.habittracker.widget.analytics.WidgetAnalytics
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Professional widget service with advanced Phase C enhancements.
 * 
 * Phase C Features:
 * - Advanced caching system for ultra-fast performance
 * - Smooth animations and micro-interactions
 * - Intelligent performance optimization
 * - Comprehensive error handling with recovery
 * - Smart update scheduling with predictive loading
 * - Local analytics for usage insights
 * 
 * Performance Targets:
 * - Update speed: <200ms
 * - Cache hit rate: >99%
 * - Memory usage: <50MB
 * - Battery drain: <1% per day
 * - Error rate: <0.1%
 */
class HabitsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AdvancedHabitsRemoteViewsFactory(applicationContext, intent)
    }

    companion object {
        /**
         * Create intent for habit completion toggle service
         */
        fun createToggleIntent(context: Context, habitId: Long): Intent {
            return Intent(context, HabitsWidgetService::class.java).apply {
                action = ACTION_TOGGLE_HABIT
                putExtra(EXTRA_HABIT_ID, habitId)
            }
        }
        
        private const val ACTION_TOGGLE_HABIT = "com.habittracker.widget.ACTION_TOGGLE_HABIT_SERVICE"
        private const val EXTRA_HABIT_ID = "com.habittracker.widget.EXTRA_HABIT_ID"
    }
}

/**
 * Advanced RemoteViewsFactory with Phase C enhancements.
 * Implements professional-grade widget service with:
 * - Lightning-fast caching system
 * - Smooth animations and transitions
 * - Intelligent performance optimization
 * - Comprehensive error handling
 * - Smart update scheduling
 * - Local analytics tracking
 */
class AdvancedHabitsRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
    
    // Phase C Professional Components
    private val repository: WidgetHabitRepository = WidgetHabitRepository.getInstance(context)
    private val cacheManager: WidgetCacheManager = WidgetCacheManager.getInstance(context)
    private val animationController: WidgetAnimationController = WidgetAnimationController.getInstance(context)
    private val performanceOptimizer: WidgetPerformanceOptimizer = WidgetPerformanceOptimizer.getInstance(context)
    private val errorHandler: WidgetErrorHandler = WidgetErrorHandler.getInstance(context)
    private val updateScheduler: WidgetUpdateScheduler = WidgetUpdateScheduler.getInstance(context)
    private val analytics: WidgetAnalytics = WidgetAnalytics.getInstance(context)
    
    // Current habits data with intelligent caching
    private var habits: List<HabitWidgetData> = emptyList()
    
    // Performance and state tracking
    private val loading = AtomicBoolean(false)
    private var lastLoadTime = 0L
    private var loadAttempts = 0
    
    override fun onCreate() {
        // Track widget service creation
        runBlocking {
            analytics.trackBehavior(
                WidgetAnalytics.BehaviorType.FIRST_TIME_USER,
                "widget_service_created"
            )
        }
        
        // Initial optimized load with caching
        loadHabitsDataWithOptimization()
    }

    override fun onDataSetChanged() {
        // Track data refresh requests
        runBlocking {
            analytics.trackInteraction(
                WidgetAnalytics.InteractionType.REFRESH_CLICK,
                "data_set_changed"
            )
        }
        
        // Smart reload with change detection
        loadHabitsDataWithOptimization()
    }

    /**
     * Advanced data loading with Phase C optimizations
     */
    private fun loadHabitsDataWithOptimization() {
        if (loading.getAndSet(true)) return // Prevent concurrent loading
        
        runBlocking {
            performanceOptimizer.optimizedExecution(
                operationName = "load_habits_data",
                priority = WidgetPerformanceOptimizer.Priority.HIGH
            ) {
                errorHandler.withErrorHandling(
                    operation = {
                        loadWithIntelligentCaching()
                    },
                    operationName = "load_habits_with_cache",
                    maxRetries = 3,
                    fallback = {
                        // Fallback to basic loading without cache
                        repository.getTodaysHabits()
                    }
                )
            }.onSuccess { result ->
                when (result) {
                    is WidgetErrorHandler.ErrorResult.Success -> {
                        habits = result.data
                        lastLoadTime = System.currentTimeMillis()
                        loadAttempts = 0
                        
                        // Track successful load
                        analytics.trackPerformance(
                            operation = "load_habits_success",
                            executionTime = System.currentTimeMillis() - lastLoadTime,
                            success = true
                        )
                    }
                    is WidgetErrorHandler.ErrorResult.Fallback -> {
                        habits = result.data
                        android.util.Log.w("WidgetService", "Used fallback: ${result.reason}")
                        
                        analytics.trackPerformance(
                            operation = "load_habits_fallback",
                            executionTime = System.currentTimeMillis() - lastLoadTime,
                            success = true,
                            metadata = mapOf("reason" to result.reason)
                        )
                    }
                    is WidgetErrorHandler.ErrorResult.Failure -> {
                        habits = emptyList()
                        loadAttempts++
                        
                        analytics.trackError(
                            errorType = "data_load_failure",
                            errorMessage = result.message,
                            severity = WidgetAnalytics.ErrorSeverity.HIGH,
                            context = mapOf("attempts" to loadAttempts)
                        )
                    }
                }
            }.onFailure { exception ->
                habits = emptyList()
                loadAttempts++
                
                analytics.trackError(
                    errorType = "optimization_failure",
                    errorMessage = exception.message ?: "Unknown error",
                    severity = WidgetAnalytics.ErrorSeverity.CRITICAL
                )
            }
        }
        
        loading.set(false)
    }
    
    /**
     * Intelligent caching strategy for lightning-fast loading
     */
    private suspend fun loadWithIntelligentCaching(): List<HabitWidgetData> {
        // Try cache first for ultra-fast response
        val cachedHabits = cacheManager.getCachedHabits()
        if (cachedHabits != null && cachedHabits.isNotEmpty()) {
            // Update cache in background for next time
            updateScheduler.scheduleUpdate(
                updateId = "background_cache_refresh",
                priority = WidgetUpdateScheduler.UpdatePriority.LOW,
                updateAction = {
                    val freshData = repository.getTodaysHabits()
                    cacheManager.cacheHabits(freshData)
                }
            )
            return cachedHabits
        }
        
        // Cache miss - load from database and cache result
        val freshHabits = repository.getTodaysHabits()
        cacheManager.cacheHabits(freshHabits)
        
        return freshHabits
    }

    override fun getCount(): Int = habits.size

    override fun getViewAt(position: Int): RemoteViews? {
        // Bounds checking for safety
        if (position < 0 || position >= habits.size) {
            return null
        }
        
        val habit = habits[position]
        val views = RemoteViews(context.packageName, R.layout.widget_habit_item)
        
        return runBlocking {
            performanceOptimizer.optimizedExecution(
                operationName = "create_habit_view",
                priority = WidgetPerformanceOptimizer.Priority.NORMAL
            ) {
                createOptimizedHabitView(views, habit, position)
            }.fold(
                onSuccess = { it },
                onFailure = { 
                    createErrorView(views, habit)
                }
            )
        }
    }
    
    /**
     * Create optimized habit view with Phase C enhancements
     */
    private suspend fun createOptimizedHabitView(
        views: RemoteViews, 
        habit: HabitWidgetData,
        position: Int
    ): RemoteViews {
        
        try {
            // Core habit information
            views.setTextViewText(R.id.habit_name, habit.getDisplayName())
            views.setContentDescription(R.id.habit_name, habit.name)
            
            // Completion status with animation preparation
            views.setBoolean(R.id.toggle_done, "setChecked", habit.isCompleted)
            
            // Set habit icon if available
            if (habit.icon != 0) {
                views.setImageViewResource(R.id.habit_icon, habit.icon)
                views.setInt(R.id.habit_icon, "setVisibility", android.view.View.VISIBLE)
            } else {
                views.setInt(R.id.habit_icon, "setVisibility", android.view.View.GONE)
            }
            
            // Set up completion toggle intent with analytics
            val toggleIntent = Intent().apply {
                action = ProfessionalHabitsWidgetProvider.ACTION_TOGGLE_HABIT
                putExtra(ProfessionalHabitsWidgetProvider.EXTRA_HABIT_ID, habit.id)
            }
            views.setOnClickFillInIntent(R.id.toggle_done, toggleIntent)
            
            // Enhanced accessibility support
            views.setContentDescription(
                R.id.toggle_done, 
                habit.getCompletionAccessibilityText()
            )
            
            // Apply visual styling with smooth animations
            if (habit.isCompleted) {
                applyCompletedHabitStyling(views, habit)
            } else {
                applyIncompleteHabitStyling(views, habit)
            }
            
            // Track view creation for analytics
            analytics.trackBehavior(
                WidgetAnalytics.BehaviorType.CASUAL_USER,
                "habit_view_created",
                mapOf("habit_id" to habit.id, "position" to position)
            )
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetService", "Error creating habit view: ${habit.name}", e)
            
            analytics.trackError(
                errorType = "view_creation_error",
                errorMessage = e.message ?: "View creation failed",
                severity = WidgetAnalytics.ErrorSeverity.MEDIUM,
                context = mapOf("habit_id" to habit.id, "position" to position)
            )
            
            return createErrorView(views, habit)
        }
        
        return views
    }
    
    /**
     * Apply completed habit styling with smooth visual feedback
     */
    private fun applyCompletedHabitStyling(views: RemoteViews, habit: HabitWidgetData) {
        // Completed habit styling with subtle visual feedback
        views.setInt(R.id.habit_name, "setTextColor", 
            context.resources.getColor(android.R.color.darker_gray, null))
        views.setFloat(R.id.habit_name, "setAlpha", 0.7f)
        
        // Show checkmark with current streak
        val streakText = if (habit.currentStreak > 0) {
            "✅ ${habit.currentStreak}"
        } else {
            "✅"
        }
        views.setTextViewText(R.id.streak_display, streakText)
        views.setInt(R.id.streak_display, "setTextColor", 
            context.resources.getColor(android.R.color.holo_green_dark, null))
    }
    
    /**
     * Apply incomplete habit styling with motivational visual cues
     */
    private fun applyIncompleteHabitStyling(views: RemoteViews, habit: HabitWidgetData) {
        // Incomplete habit styling with full prominence
        views.setInt(R.id.habit_name, "setTextColor", 
            context.resources.getColor(R.color.widgetTextColor, null))
        views.setFloat(R.id.habit_name, "setAlpha", 1.0f)
        
        // Show streak with fire emoji for motivation
        views.setTextViewText(R.id.streak_display, habit.getStreakDisplay())
        views.setInt(R.id.streak_display, "setTextColor", 
            context.resources.getColor(R.color.widgetTextColor, null))
    }
    
    /**
     * Create error view with user-friendly message
     */
    private suspend fun createErrorView(views: RemoteViews, habit: HabitWidgetData): RemoteViews {
        views.setTextViewText(R.id.habit_name, context.getString(R.string.widget_error))
        views.setBoolean(R.id.toggle_done, "setEnabled", false)
        views.setTextViewText(R.id.streak_display, "❌")
        views.setInt(R.id.habit_icon, "setVisibility", android.view.View.INVISIBLE)
        
        // Apply error styling
        views.setInt(R.id.habit_name, "setTextColor", 
            context.resources.getColor(android.R.color.holo_red_light, null))
        
        return views
    }

    override fun getLoadingView(): RemoteViews? {
        val loadingView = RemoteViews(context.packageName, R.layout.widget_habit_item)
        
        runBlocking {
            // Apply loading animation
            animationController.animateLoadingState(loadingView).collect { state ->
                when (state) {
                    is WidgetAnimationController.AnimationState.Started -> {
                        analytics.trackBehavior(
                            WidgetAnalytics.BehaviorType.CASUAL_USER,
                            "loading_animation_started"
                        )
                    }
                    is WidgetAnimationController.AnimationState.Completed -> {
                        analytics.trackBehavior(
                            WidgetAnalytics.BehaviorType.CASUAL_USER,
                            "loading_animation_completed"
                        )
                    }
                    else -> { /* Handle other states */ }
                }
            }
        }
        
        loadingView.setTextViewText(R.id.habit_name, context.getString(R.string.widget_loading))
        loadingView.setBoolean(R.id.toggle_done, "setEnabled", false)
        loadingView.setTextViewText(R.id.streak_display, "🔄")
        loadingView.setInt(R.id.habit_icon, "setVisibility", android.view.View.INVISIBLE)
        
        return loadingView
    }
    
    override fun getViewTypeCount(): Int = 1
    
    override fun getItemId(position: Int): Long = 
        habits.getOrNull(position)?.id ?: position.toLong()
    
    override fun hasStableIds(): Boolean = true
    
    override fun onDestroy() {
        // Track service destruction
        runBlocking {
            analytics.trackBehavior(
                WidgetAnalytics.BehaviorType.CASUAL_USER,
                "widget_service_destroyed",
                mapOf("session_duration" to (System.currentTimeMillis() - lastLoadTime))
            )
        }
        
        // Clean up resources
        habits = emptyList()
        animationController.stopAllAnimations()
        
        // Perform final cache optimization
        runBlocking {
            if (habits.isNotEmpty()) {
                cacheManager.cacheHabits(habits)
            }
        }
    }
}
