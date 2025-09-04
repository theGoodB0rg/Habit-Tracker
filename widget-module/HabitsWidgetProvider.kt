
package com.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

/**
 * Home screen widget provider for displaying today's habits with Mark Done toggle and refresh.
 * Fully accessible, responsive, and syncs with the DB.
 */
class HabitsWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val ACTION_TOGGLE_HABIT = "com.habittracker.widget.ACTION_TOGGLE_HABIT"
        private const val ACTION_REFRESH = "com.habittracker.widget.ACTION_REFRESH"
        private const val EXTRA_HABIT_ID = "com.habittracker.widget.EXTRA_HABIT_ID"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_habits)

            // Set up the collection adapter
            val intent = Intent(context, HabitsWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.habits_list, intent)

            // Set up refresh button
            val refreshIntent = Intent(context, HabitsWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            )
            views.setOnClickPendingIntent(R.id.button_refresh, refreshPendingIntent)

            // Set up empty view
            views.setEmptyView(R.id.habits_list, R.id.empty_view)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.habits_list)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_HABIT -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                if (habitId != -1L) {
                    // Mark habit as done in DB (async, safe)
                    ContextCompat.startForegroundService(
                        context,
                        HabitsWidgetService.createToggleIntent(context, habitId)
                    )
                }
            }
            ACTION_REFRESH -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val mgr = AppWidgetManager.getInstance(context)
                    mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.habits_list)
                }
            }
        }
    }

    // For accessibility, ensure content descriptions are set in RemoteViewsFactory
}
