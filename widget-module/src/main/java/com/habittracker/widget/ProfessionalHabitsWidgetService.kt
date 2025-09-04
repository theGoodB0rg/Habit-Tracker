package com.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.habittracker.core.HabitWidgetData
import com.habittracker.core.WidgetHabitRepository
import kotlinx.coroutines.runBlocking

/**
 * Professional RemoteViewsService for the enhanced habits widget
 * Provides real-time habit data with interactive functionality
 */
class ProfessionalHabitsWidgetService : RemoteViewsService() {
    
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitsRemoteViewsFactory(applicationContext, intent)
    }
}

/**
 * RemoteViewsFactory that manages the ListView content
 * with real database integration and professional UI
 */
class HabitsRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
    
    private var habits: List<HabitWidgetData> = emptyList()
    private val repository = WidgetHabitRepository.getInstance(context)
    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID, 
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    
    companion object {
        private const val TAG = "HabitsRemoteViewsFactory"
    }
    
    override fun onCreate() {
        android.util.Log.d(TAG, "onCreate() called for widget: $appWidgetId")
        loadHabits()
    }
    
    override fun onDataSetChanged() {
        android.util.Log.d(TAG, "onDataSetChanged() called for widget: $appWidgetId")
        loadHabits()
    }
    
    override fun onDestroy() {
        android.util.Log.d(TAG, "onDestroy() called for widget: $appWidgetId")
        habits = emptyList()
    }
    
    /**
     * Load habits from real database with error handling
     */
    private fun loadHabits() {
        try {
            runBlocking {
                habits = repository.getTodaysHabits()
                android.util.Log.d(TAG, "Loaded ${habits.size} habits for widget $appWidgetId")
                android.util.Log.d(TAG, "Habit details: ${habits.map { "${it.name}(id=${it.id}, completed=${it.isCompleted})" }}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading habits for widget $appWidgetId", e)
            habits = emptyList()
        }
    }
    
    override fun getCount(): Int {
        val count = habits.size
        android.util.Log.d(TAG, "getCount() returning: $count")
        return count
    }
    
    override fun getViewAt(position: Int): RemoteViews {
        if (position >= habits.size) {
            android.util.Log.w(TAG, "getViewAt($position) out of bounds, habits size: ${habits.size}")
            return getErrorView()
        }
        
        val habit = habits[position]
        android.util.Log.d(TAG, "getViewAt($position) for habit: ${habit.name}")
        
        return createHabitItemView(habit)
    }
    
    /**
     * Create a professional habit item view with full functionality
     */
    private fun createHabitItemView(habit: HabitWidgetData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_habit_item)
        
        try {
            // Set habit name with proper styling
            views.setTextViewText(R.id.habit_name, habit.name)
            views.setTextColor(
                R.id.habit_name, 
                ContextCompat.getColor(context, R.color.widget_text_primary)
            )
            
            // Set habit icon (use default if not specified)
            val iconResource = if (habit.icon != 0) habit.icon else R.drawable.ic_habit_default
            views.setImageViewResource(R.id.habit_icon, iconResource)
            
            // Set completion checkbox state with visual feedback
            views.setBoolean(R.id.completion_checkbox, "setChecked", habit.isCompleted)
            
            // Style checkbox based on completion status
            val checkboxTint = if (habit.isCompleted) {
                ContextCompat.getColor(context, R.color.habit_completed)
            } else {
                ContextCompat.getColor(context, R.color.habit_pending)
            }
            views.setInt(R.id.completion_checkbox, "setColorFilter", checkboxTint)
            
            // Set streak display with fire emoji
            val streakText = "🔥${habit.currentStreak}"
            views.setTextViewText(R.id.streak_display, streakText)
            
            // Color streak based on value
            val streakColor = when {
                habit.currentStreak >= 7 -> R.color.progress_excellent
                habit.currentStreak >= 3 -> R.color.progress_good
                habit.currentStreak >= 1 -> R.color.progress_fair
                else -> R.color.progress_poor
            }
            views.setTextColor(R.id.streak_display, ContextCompat.getColor(context, streakColor))
            
            // Set up click intent for habit completion toggle
            val toggleIntent = Intent().apply {
                // Use action to identify this as a habit toggle
                action = ProfessionalHabitsWidgetProvider.ACTION_TOGGLE_HABIT
                putExtra(ProfessionalHabitsWidgetProvider.EXTRA_HABIT_ID, habit.id)
                putExtra(ProfessionalHabitsWidgetProvider.EXTRA_WIDGET_ID, appWidgetId)
                // Add unique data to differentiate between different habits
                data = Uri.parse("habit://toggle/${habit.id}")
            }
            views.setOnClickFillInIntent(R.id.completion_checkbox, toggleIntent)
            
            // Also allow clicking the entire row to toggle
            views.setOnClickFillInIntent(R.id.habit_item_root, toggleIntent)

            // Provide fill-in for timer start on row button
            val timerIntent = Intent().apply {
                action = ProfessionalHabitsWidgetProvider.ACTION_TIMER_START
                putExtra(ProfessionalHabitsWidgetProvider.EXTRA_HABIT_ID, habit.id)
                putExtra(ProfessionalHabitsWidgetProvider.EXTRA_WIDGET_ID, appWidgetId)
                data = Uri.parse("habit://timer/start/${habit.id}")
            }
            views.setOnClickFillInIntent(R.id.widget_timer_start, timerIntent)
            
            android.util.Log.d(TAG, "Set click intent for habit ${habit.name} (ID: ${habit.id}) with action: ${toggleIntent.action}")
            
            // Set accessibility content descriptions
            setupAccessibilityForHabitItem(views, habit)
            
            // Add visual feedback for completion status
            val backgroundTint = if (habit.isCompleted) {
                ContextCompat.getColor(context, R.color.habit_completed).and(0x20FFFFFF)
            } else {
                ContextCompat.getColor(context, android.R.color.transparent)
            }
            views.setInt(R.id.habit_item_root, "setBackgroundColor", backgroundTint)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating habit item view for ${habit.name}", e)
            return getErrorView()
        }
        
        return views
    }
    
    /**
     * Setup accessibility features for habit items
     */
    private fun setupAccessibilityForHabitItem(views: RemoteViews, habit: HabitWidgetData) {
        val habitDescription = "Habit: ${habit.name}, streak: ${habit.currentStreak} days"
        views.setContentDescription(R.id.habit_name, habitDescription)
        
        val checkboxDescription = if (habit.isCompleted) {
            "Mark ${habit.name} as not done"
        } else {
            "Mark ${habit.name} as done"
        }
        views.setContentDescription(R.id.completion_checkbox, checkboxDescription)
        
        val streakDescription = "${habit.currentStreak} day streak for ${habit.name}"
        views.setContentDescription(R.id.streak_display, streakDescription)
    }
    
    /**
     * Create error view for when habit loading fails
     */
    private fun getErrorView(): RemoteViews {
        val errorViews = RemoteViews(context.packageName, R.layout.widget_habit_item)
        
        errorViews.setTextViewText(R.id.habit_name, "⚠️ Error loading habit")
        errorViews.setTextColor(
            R.id.habit_name, 
            ContextCompat.getColor(context, R.color.error_color)
        )
        errorViews.setImageViewResource(R.id.habit_icon, R.drawable.ic_error)
        errorViews.setBoolean(R.id.completion_checkbox, "setChecked", false)
        errorViews.setBoolean(R.id.completion_checkbox, "setEnabled", false)
        errorViews.setTextViewText(R.id.streak_display, "🔥0")
        
        errorViews.setContentDescription(
            R.id.habit_name, 
            "Error loading habit data. Try refreshing the widget."
        )
        
        return errorViews
    }
    
    override fun getLoadingView(): RemoteViews? {
        // Return null to use default loading view
        return null
    }
    
    override fun getViewTypeCount(): Int {
        // Only one view type for habit items
        return 1
    }
    
    override fun getItemId(position: Int): Long {
        return if (position < habits.size) {
            habits[position].id
        } else {
            position.toLong()
        }
    }
    
    override fun hasStableIds(): Boolean {
        // Stable IDs help with ListView performance
        return true
    }
}
