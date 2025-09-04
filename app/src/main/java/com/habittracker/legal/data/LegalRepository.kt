package com.habittracker.legal.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.habittracker.R
import com.habittracker.legal.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalRepository @Inject constructor(
    private val context: Context
) {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * Retrieves comprehensive app version information with race condition protection
     */
    suspend fun getAppVersionInfo(): Result<AppVersionInfo> = withContext(Dispatchers.IO) {
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            
            val buildDate = dateFormatter.format(Date(packageInfo.firstInstallTime))
            val buildType = if (context.applicationInfo.flags and 
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) "Debug" else "Release"
                
            val applicationInfo = context.applicationInfo
            val targetSdk = applicationInfo.targetSdkVersion
            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                applicationInfo.minSdkVersion
            } else {
                24 // Default min SDK for our app
            }
            
            Result.success(
                AppVersionInfo(
                    versionName = versionName,
                    versionCode = versionCode,
                    buildDate = buildDate,
                    buildType = buildType,
                    targetSdk = targetSdk,
                    minSdk = minSdk
                )
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Retrieves developer information
     */
    fun getDeveloperInfo(): DeveloperInfo {
        return DeveloperInfo(
            name = "Habit Tracker Team",
            email = "support@habittracker.app",
            website = "https://habittracker.app",
            githubProfile = "https://github.com/habittracker",
            linkedinProfile = null
        )
    }
    
    /**
     * Loads HTML content from assets with proper error handling
     */
    suspend fun loadHtmlContent(fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = context.assets.open(fileName).use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
            Result.success(content)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets tutorial steps for the comprehensive visual tutorial
     */
    fun getTutorialSteps(): List<TutorialStep> {
        return listOf(
            TutorialStep(
                titleRes = R.string.tutorial_welcome_title,
                descriptionRes = R.string.tutorial_welcome_description,
                illustrationRes = android.R.drawable.ic_dialog_info,
                highlightType = HighlightType.CIRCLE
            ),
            TutorialStep(
                titleRes = R.string.tutorial_add_habit_title,
                descriptionRes = R.string.tutorial_add_habit_description,
                illustrationRes = android.R.drawable.ic_dialog_info,
                targetViewId = "main_add_button",
                highlightType = HighlightType.CIRCLE
            ),
            TutorialStep(
                titleRes = R.string.tutorial_track_progress_title,
                descriptionRes = R.string.tutorial_track_progress_description,
                illustrationRes = android.R.drawable.ic_dialog_info,
                targetViewId = "habit_card",
                highlightType = HighlightType.ROUNDED_RECTANGLE
            ),
            TutorialStep(
                titleRes = R.string.tutorial_build_streaks_title,
                descriptionRes = R.string.tutorial_build_streaks_description,
                illustrationRes = android.R.drawable.ic_dialog_info,
                targetViewId = "streak_counter",
                highlightType = HighlightType.RECTANGLE
            ),
            TutorialStep(
                titleRes = R.string.tutorial_stay_motivated_title,
                descriptionRes = R.string.tutorial_stay_motivated_description,
                illustrationRes = android.R.drawable.ic_dialog_info,
                highlightType = HighlightType.NONE
            )
        )
    }
    
    /**
     * Gets categorized tips for users
     */
    fun getHabitTips(): List<HabitTip> {
        return listOf(
            HabitTip(
                titleRes = R.string.tip_start_small_title,
                descriptionRes = R.string.tip_start_small_description,
                iconRes = android.R.drawable.ic_dialog_info,
                category = TipCategory.GETTING_STARTED
            ),
            HabitTip(
                titleRes = R.string.tip_celebrate_wins_title,
                descriptionRes = R.string.tip_celebrate_wins_description,
                iconRes = android.R.drawable.ic_dialog_info,
                category = TipCategory.MOTIVATION
            ),
            HabitTip(
                titleRes = R.string.tip_focus_consistency_title,
                descriptionRes = R.string.tip_focus_consistency_description,
                iconRes = android.R.drawable.ic_dialog_info,
                category = TipCategory.STREAK_BUILDING
            ),
            HabitTip(
                titleRes = R.string.tip_stack_habits_title,
                descriptionRes = R.string.tip_stack_habits_description,
                iconRes = android.R.drawable.ic_dialog_info,
                category = TipCategory.HABIT_FORMATION
            ),
            HabitTip(
                titleRes = R.string.tip_time_blocking_title,
                descriptionRes = R.string.tip_time_blocking_description,
                iconRes = android.R.drawable.ic_dialog_info,
                category = TipCategory.PRODUCTIVITY
            ),
            HabitTip(
                titleRes = R.string.tip_missing_days_title,
                descriptionRes = R.string.tip_missing_days_description,
                iconRes = android.R.drawable.ic_dialog_info,
                category = TipCategory.TROUBLESHOOTING
            )
        )
    }
    
    /**
     * Generates device and app info for feedback
     */
    suspend fun generateFeedbackInfo(subject: String, userMessage: String): Result<FeedbackInfo> = 
        withContext(Dispatchers.IO) {
            try {
                val versionInfo = getAppVersionInfo().getOrThrow()
                
                val deviceInfo = buildString {
                    appendLine("Device Information:")
                    appendLine("- Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("- Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    appendLine("- App Version: ${versionInfo.versionName} (${versionInfo.versionCode})")
                    appendLine("- Build Type: ${versionInfo.buildType}")
                    appendLine("- Target SDK: ${versionInfo.targetSdk}")
                    appendLine("- Min SDK: ${versionInfo.minSdk}")
                    appendLine("- Build Date: ${versionInfo.buildDate}")
                    appendLine()
                }
                
                val fullBody = buildString {
                    appendLine(userMessage)
                    appendLine()
                    appendLine("---")
                    append(deviceInfo)
                }
                
                Result.success(
                    FeedbackInfo(
                        subject = subject,
                        body = fullBody,
                        deviceInfo = deviceInfo,
                        appVersion = "${versionInfo.versionName} (${versionInfo.versionCode})"
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
