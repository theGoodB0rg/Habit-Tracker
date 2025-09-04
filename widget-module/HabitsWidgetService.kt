
package com.habittracker.widget

import android.app.IntentService
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.habittracker.corearchitecture.data.HabitRepository
import com.habittracker.corearchitecture.data.model.HabitEntity
import com.habittracker.widget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service providing RemoteViewsFactory for the widget's ListView.
 * Handles DB sync, Mark Done, and race condition protection.
 */
class HabitsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitsRemoteViewsFactory(applicationContext, intent)
    }

    companion object {
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

class HabitsRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
    private val repo: HabitRepository = HabitRepository.getInstance(context)
    private var habits: List<HabitEntity> = emptyList()
    private val loading = AtomicBoolean(false)

    override fun onCreate() {
        // Initial load
        loadHabits()
    }

    override fun onDataSetChanged() {
        loadHabits()
    }

    private fun loadHabits() {
        if (loading.getAndSet(true)) return // Prevent race conditions
        try {
            // Synchronously fetch today's habits (should be off main thread in prod)
            habits = repo.getHabitsForDate(LocalDate.now())
        } catch (e: Exception) {
            habits = emptyList()
        } finally {
            loading.set(false)
        }
    }

    override fun getCount(): Int = habits.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= habits.size) return null
        val habit = habits[position]
        val views = RemoteViews(context.packageName, R.layout.widget_habit_item)
        views.setTextViewText(R.id.habit_name, habit.name)
        views.setContentDescription(R.id.habit_name, habit.name)
        views.setBoolean(R.id.toggle_done, "checked", habit.isDoneToday)

        // Mark Done toggle intent
        val toggleIntent = Intent(context, HabitsWidgetProvider::class.java).apply {
            action = "com.habittracker.widget.ACTION_TOGGLE_HABIT"
            putExtra("com.habittracker.widget.EXTRA_HABIT_ID", habit.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, habit.id.toInt(), toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        views.setOnClickPendingIntent(R.id.toggle_done, pendingIntent)

        // Accessibility: content description for toggle
        views.setContentDescription(R.id.toggle_done, if (habit.isDoneToday) "Mark as not done" else "Mark as done")

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = habits.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
    override fun onDestroy() { habits = emptyList() }
}
