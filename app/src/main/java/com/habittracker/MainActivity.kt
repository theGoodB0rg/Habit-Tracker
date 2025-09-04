package com.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.habittracker.analytics.data.initialization.AnalyticsDataInitializer
import com.habittracker.themes.presentation.ThemeViewModel
import com.habittracker.ui.navigation.HabitTrackerNavigation
import com.habittracker.ui.theme.HabitTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var analyticsDataInitializer: AnalyticsDataInitializer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize analytics data in background
        lifecycleScope.launch {
            try {
                analyticsDataInitializer.initializeSampleData()
            } catch (e: Exception) {
                // Log error but don't crash the app
                android.util.Log.w("MainActivity", "Failed to initialize analytics data", e)
            }
        }
        
        setContent {
            // Use the new theme system with ViewModel
            HabitTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HabitTrackerNavigation()
                }
            }
        }
    }
}

