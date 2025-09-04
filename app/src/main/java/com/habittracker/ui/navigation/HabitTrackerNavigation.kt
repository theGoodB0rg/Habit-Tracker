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
            MainScreen(
                viewModel = habitViewModel,
                onNavigateToAddHabit = {
                    navController.navigate(Screen.AddHabit.route)
                },
                onNavigateToEditHabit = { habitId ->
                    navController.navigate(Screen.EditHabit.createRoute(habitId))
                },
                onNavigateToHabitDetail = { habitId ->
                    navController.navigate(Screen.HabitDetail.createRoute(habitId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.ThemeSettings.route)
                },
                onNavigateToSmartTimingSettings = {
                    navController.navigate(Screen.SmartTimingSettings.route)
                },
                onNavigateToReminderSettings = {
                    navController.navigate(Screen.ReminderSettings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToExport = {
                    navController.navigate(Screen.Export.route)
                }
            )
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
            val habits by habitViewModel.habits.collectAsState(initial = emptyList())
            val targetHabit = habits.firstOrNull { it.id == habitId }
            if (targetHabit != null && targetHabit.lastCompletedDate == null) {
                androidx.compose.material3.Scaffold(
                    topBar = {
                        androidx.compose.material3.TopAppBar(
                            title = { androidx.compose.material3.Text("Habit Details") },
                            navigationIcon = {
                                androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        )
                    }
                ) { padding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            "No analytics for this habit yet — complete it at least once.",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
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
