package com.habittracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.habittracker.analytics.presentation.ui.AnalyticsScreen
import com.habittracker.export.presentation.ui.ExportScreen
import com.habittracker.legal.presentation.AboutScreen
import com.habittracker.legal.presentation.HelpWebViewScreen
import com.habittracker.legal.presentation.TipsScreen
import com.habittracker.legal.presentation.TutorialScreen
import com.habittracker.onboarding.OnboardingPreferences
import com.habittracker.onboarding.ui.OnboardingScreen
import com.habittracker.presentation.viewmodel.HabitViewModel
import com.habittracker.themes.presentation.ThemeSettingsScreen
import com.habittracker.ui.screens.timing.SmartTimingSettingsScreen
import com.habittracker.ui.screens.timing.AlertProfilesScreen
import com.habittracker.reminders.ui.ReminderSettingsScreen
import com.habittracker.ui.screens.AddHabitScreen
import com.habittracker.ui.screens.EditHabitScreen
import com.habittracker.ui.screens.HabitDetailScreen
import com.habittracker.ui.screens.MainScreen
import com.habittracker.ui.screens.simple.SimpleMainScreen
import com.habittracker.timing.TimerFeatureFlags
import javax.inject.Inject

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HabitTrackerNavigation(
    navController: NavHostController = rememberNavController()
) {
    val habitViewModel: HabitViewModel = hiltViewModel()
    
    // Get onboarding preferences through DI
    val onboardingPreferences: OnboardingPreferences = hiltViewModel<NavigationViewModel>().onboardingPreferences
    
    // Determine start destination once (NavHost only reads it on first composition)
    val startDestination = remember {
        if (onboardingPreferences.shouldShowOnboarding()) {
            Screen.Onboarding.route
        } else {
            Screen.Main.route
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding flow
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            // Phase 2: Feature flag switches between legacy MainScreen and new SimpleMainScreen
            if (TimerFeatureFlags.useSimplifiedHomeScreen) {
                SimpleMainScreen(
                    viewModel = habitViewModel,
                    onNavigateToAddHabit = {
                        navController.navigate(Screen.AddHabit.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToEditHabit = { habitId ->
                        navController.navigate(Screen.EditHabit.createRoute(habitId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHabitDetail = { habitId ->
                        navController.navigate(Screen.HabitDetail.createRoute(habitId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.ThemeSettings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSmartTimingSettings = {
                        navController.navigate(Screen.SmartTimingSettings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToReminderSettings = {
                        navController.navigate(Screen.ReminderSettings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.Analytics.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToExport = {
                        navController.navigate(Screen.Export.route) {
                            launchSingleTop = true
                        }
                    }
                )
            } else {
                // Legacy MainScreen
                MainScreen(
                    viewModel = habitViewModel,
                    onNavigateToAddHabit = {
                        navController.navigate(Screen.AddHabit.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToEditHabit = { habitId ->
                        navController.navigate(Screen.EditHabit.createRoute(habitId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHabitDetail = { habitId ->
                        // Use launchSingleTop to prevent duplicate navigation crashes
                        navController.navigate(Screen.HabitDetail.createRoute(habitId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.ThemeSettings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSmartTimingSettings = {
                        navController.navigate(Screen.SmartTimingSettings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToReminderSettings = {
                        navController.navigate(Screen.ReminderSettings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.Analytics.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToExport = {
                        navController.navigate(Screen.Export.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        
        composable(Screen.AddHabit.route) {
            AddHabitScreen(
                viewModel = habitViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.EditHabit.route,
            arguments = Screen.EditHabit.arguments
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: return@composable
            EditHabitScreen(
                habitId = habitId,
                viewModel = habitViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.HabitDetail.route,
            arguments = Screen.HabitDetail.arguments
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: return@composable
            // Removed state collection from navigation lambda to prevent SlotTable race condition
            // HabitDetailScreen handles its own loading state internally
            HabitDetailScreen(
                habitId = habitId,
                viewModel = habitViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = {
                    navController.navigate(Screen.EditHabit.createRoute(habitId))
                }
            )
        }
        
        composable(Screen.ThemeSettings.route) {
            ThemeSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.SmartTimingSettings.route) {
            SmartTimingSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAlertProfiles = { navController.navigate(Screen.AlertProfiles.route) }
            )
        }

        composable(Screen.AlertProfiles.route) {
            AlertProfilesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ReminderSettings.route) {
            ReminderSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Export.route) {
            ExportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Legal Module Screens
        composable(Screen.About.route) {
            AboutScreen(
                navController = navController
            )
        }
        
        composable(Screen.Tutorial.route) {
            TutorialScreen(
                navController = navController
            )
        }
        
        composable(Screen.Tips.route) {
            TipsScreen(
                navController = navController
            )
        }
        
        composable(
            route = Screen.HelpWebView.route,
            arguments = Screen.HelpWebView.arguments
        ) { backStackEntry ->
            val pageType = backStackEntry.arguments?.getString("pageType") ?: return@composable
            HelpWebViewScreen(
                pageType = pageType,
                navController = navController
            )
        }
    }
}
