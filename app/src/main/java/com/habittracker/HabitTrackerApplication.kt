package com.habittracker

import android.app.Application
import com.habittracker.nudges.service.NudgeService
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
        
        // Start the nudge service
        nudgeService.startService()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Stop the nudge service
        nudgeService.stopService()
    }
}
