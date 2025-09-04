package com.habittracker.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    object Onboarding : Screen("onboarding")
    
    object Main : Screen("main")
    
    object AddHabit : Screen("add_habit")
    
    object EditHabit : Screen(
        route = "edit_habit/{habitId}",
        arguments = listOf(
            navArgument("habitId") {
                type = NavType.LongType
            }
        )
    ) {
        fun createRoute(habitId: Long): String {
            return "edit_habit/$habitId"
        }
    }
    
    object HabitDetail : Screen(
        route = "habit_detail/{habitId}",
        arguments = listOf(
            navArgument("habitId") {
                type = NavType.LongType
            }
        )
    ) {
        fun createRoute(habitId: Long): String {
            return "habit_detail/$habitId"
        }
    }
    
    object ThemeSettings : Screen("theme_settings")
    
    // Smart Timing feature settings (Slice 4 UI)
    object SmartTimingSettings : Screen("smart_timing_settings")
    object AlertProfiles : Screen("alert_profiles")
    
    // Reminder settings hub
    object ReminderSettings : Screen("reminder_settings")
    
    object Analytics : Screen("analytics")
    
    object Export : Screen("export")
    
    // Legal & Policy Module
    object About : Screen("about")
    
    object PrivacyPolicy : Screen("privacy_policy")
    
    object TermsOfService : Screen("terms_of_service")
    
    object Tutorial : Screen("tutorial")
    
    object Tips : Screen("tips")
    
    object HelpWebView : Screen(
        route = "help_webview/{pageType}",
        arguments = listOf(
            navArgument("pageType") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(pageType: String): String {
            return "help_webview/$pageType"
        }
    }
}
