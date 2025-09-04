package com.habittracker.legal.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.habittracker.R

/**
 * Data class representing app version information
 */
data class AppVersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildDate: String,
    val buildType: String,
    val targetSdk: Int,
    val minSdk: Int
)

/**
 * Data class for developer information
 */
data class DeveloperInfo(
    val name: String,
    val email: String,
    val website: String?,
    val githubProfile: String?,
    val linkedinProfile: String?
)

/**
 * Enum for different help page types
 */
enum class HelpPageType(val fileName: String, @StringRes val titleRes: Int) {
    ABOUT("about_us.html", R.string.page_title_about),
    PRIVACY_POLICY("privacy_policy.html", R.string.page_title_privacy),
    TERMS_OF_SERVICE("terms_of_service.html", R.string.page_title_terms)
}

/**
 * Data class for tutorial steps
 */
data class TutorialStep(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val illustrationRes: Int,
    val targetViewId: String? = null,
    val highlightType: HighlightType = HighlightType.NONE
)

/**
 * Enum for highlight types in tutorial
 */
enum class HighlightType {
    NONE,
    CIRCLE,
    RECTANGLE,
    ROUNDED_RECTANGLE
}

/**
 * Data class for tips
 */
data class HabitTip(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val category: TipCategory
)

/**
 * Enum for tip categories
 */
enum class TipCategory {
    GETTING_STARTED,
    MOTIVATION,
    STREAK_BUILDING,
    HABIT_FORMATION,
    PRODUCTIVITY,
    TROUBLESHOOTING
}

/**
 * Data class for feedback information
 */
data class FeedbackInfo(
    val subject: String,
    val body: String,
    val deviceInfo: String,
    val appVersion: String
)
