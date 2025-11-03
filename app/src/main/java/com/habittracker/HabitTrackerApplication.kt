package com.habittracker

import android.app.Application
import android.content.pm.ApplicationInfo
import com.habittracker.nudges.service.NudgeService
import com.habittracker.timing.TimerFeatureFlags
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for the Habit Tracker app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class HabitTrackerApplication : Application() {
    
    @Inject
    lateinit var nudgeService: NudgeService
    
    override fun onCreate() {
        super.onCreate()
        
        val defaultCoordinatorEnabled = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        TimerFeatureFlags.initialize(
            context = this,
            defaults = TimerFeatureFlags.Defaults(
                enableAlertScheduling = true,
                enableActionCoordinator = defaultCoordinatorEnabled
            )
        )

        // Start the nudge service
        nudgeService.startService()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Stop the nudge service
        nudgeService.stopService()
    }
}
