package com.habittracker.export.data.model

import androidx.annotation.ColorInt
import java.time.LocalDate

/**
 * Complete data structure for PNG export generation
 * Contains all necessary information for creating visual habit reports
 */
data class PngExportData(
    val metadata: PngExportMetadata,
    val statistics: HabitStatistics,
    val heatmapData: HeatmapData,
    val achievements: List<Achievement>,
    val habitSummaries: List<HabitSummary>,
    val customization: PngCustomization = PngCustomization()
)

/**
 * Metadata specific to PNG exports
 */
data class PngExportMetadata(
    val exportDate: LocalDate,
    val appVersion: String,
    val userName: String? = null,
    val timeRange: DateRange,
    val totalHabits: Int,
    val activeHabits: Int,
    val exportScope: String
)

/**
 * Aggregated statistics for visual display
 */
data class HabitStatistics(
    val totalCompletions: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Double, // 0.0 to 1.0
    val averageCompletionsPerDay: Double,
    val mostProductiveDay: String, // Day of week
    val habitCategories: Map<String, Int>, // Category name to count
    val weeklyTrends: List<WeeklyTrend>,
    val monthlyComparison: MonthlyComparison?
)

/**
 * Heatmap visualization data
 */
data class HeatmapData(
    val cells: List<HeatmapCell>,
    val maxCompletions: Int, // For scaling colors
    val dateRange: DateRange,
    val legend: HeatmapLegend
)

/**
 * Individual cell in the heatmap
 */
data class HeatmapCell(
    val date: LocalDate,
    val completionCount: Int,
    val intensity: Float, // 0.0 to 1.0 for color intensity
    val habitNames: List<String> = emptyList() // For tooltips/details
)

/**
 * Heatmap legend information
 */
data class HeatmapLegend(
    val intensityLevels: List<IntensityLevel>,
    val title: String = "Habit Completions"
)

/**
 * Intensity level for heatmap legend
 */
data class IntensityLevel(
    val range: String, // e.g., "1-2", "3-5"
    @ColorInt val color: Int,
    val intensity: Float
)

/**
 * Achievement/badge information
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconResource: String, // Resource name or emoji
    val earnedDate: LocalDate,
    val category: AchievementCategory,
    val isRecent: Boolean = false // Earned in last week
)

/**
 * Achievement categories
 */
enum class AchievementCategory {
    STREAK,
    CONSISTENCY,
    MILESTONE,
    FREQUENCY,
    VARIETY
}

/**
 * Summary information for individual habits
 */
data class HabitSummary(
    val habitId: Long,
    val name: String,
    val iconId: Int,
    val currentStreak: Int,
    val completionRate: Double,
    val totalCompletions: Int,
    val recentTrend: TrendDirection,
    val color: Int, // Habit-specific color
    val isHighlighted: Boolean = false // Top performer
)

/**
 * Trend direction indicator
 */
enum class TrendDirection {
    UP,
    DOWN,
    STABLE,
    NEW
}

/**
 * Weekly trend data for charts
 */
data class WeeklyTrend(
    val weekStartDate: LocalDate,
    val completionCount: Int,
    val habitCount: Int,
    val averageCompletionRate: Double
)

/**
 * Monthly comparison data
 */
data class MonthlyComparison(
    val currentMonth: MonthData,
    val previousMonth: MonthData,
    val percentageChange: Double,
    val improvement: Boolean
)

/**
 * Monthly aggregated data
 */
data class MonthData(
    val monthName: String,
    val year: Int,
    val totalCompletions: Int,
    val averageCompletionRate: Double,
    val activeHabits: Int
)

/**
 * Customization options for PNG export
 */
data class PngCustomization(
    val theme: PngTheme = PngTheme.LIGHT,
    val includePersonalMessage: Boolean = true,
    val personalMessage: String? = null,
    val showHabitNames: Boolean = true,
    val showDetailedStats: Boolean = true,
    val logoVisibility: LogoVisibility = LogoVisibility.VISIBLE,
    val aspectRatio: AspectRatio = AspectRatio.INSTAGRAM_PORTRAIT,
    val colorScheme: ColorScheme = ColorScheme.MATERIAL_3,
    val fontScale: Float = 1.0f // Accessibility scaling
)

/**
 * Visual themes for PNG exports
 */
enum class PngTheme {
    LIGHT,
    DARK,
    HIGH_CONTRAST,
    MINIMAL
}

/**
 * Logo visibility options
 */
enum class LogoVisibility {
    VISIBLE,
    HIDDEN,
    WATERMARK
}

/**
 * Export aspect ratios for different social platforms
 */
enum class AspectRatio(
    val width: Int,
    val height: Int,
    val displayName: String
) {
    SQUARE(1080, 1080, "Square (Instagram Post)"),
    INSTAGRAM_PORTRAIT(1080, 1350, "Instagram Portrait"),
    INSTAGRAM_STORY(1080, 1920, "Instagram Story"),
    FACEBOOK_POST(1200, 630, "Facebook Post"),
    TWITTER_CARD(1200, 675, "Twitter Card"),
    LINKEDIN_POST(1200, 627, "LinkedIn Post"),
    CUSTOM(1080, 1920, "Custom Size")
}

/**
 * Color schemes for theming
 */
enum class ColorScheme {
    MATERIAL_3,
    HABIT_TRACKER_BRAND,
    MONOCHROME,
    VIBRANT,
    PASTEL,
    DARK_MODE
}

/**
 * Layout configuration for PNG rendering
 */
data class PngLayoutConfig(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val padding: PaddingConfig,
    val sections: List<LayoutSection>,
    val typography: TypographyConfig,
    val colorPalette: ColorPalette
)

/**
 * Padding configuration
 */
data class PaddingConfig(
    val outer: Int = 24, // Outer margins
    val section: Int = 16, // Between sections
    val element: Int = 8, // Between elements
    val text: Int = 4 // Text spacing
)

/**
 * Layout section configuration
 */
data class LayoutSection(
    val type: SectionType,
    val weight: Float, // Relative height
    val minHeight: Int,
    val maxHeight: Int? = null,
    val backgroundColor: Int? = null
)

/**
 * Section types for layout
 */
enum class SectionType {
    HEADER,
    STATISTICS,
    HEATMAP,
    ACHIEVEMENTS,
    HABIT_SUMMARY,
    FOOTER
}

/**
 * Typography configuration
 */
data class TypographyConfig(
    val titleSize: Float = 24f,
    val headingSize: Float = 20f,
    val bodySize: Float = 16f,
    val captionSize: Float = 12f,
    val fontFamily: String = "sans-serif",
    val lineHeight: Float = 1.4f
)

/**
 * Color palette for theming
 */
data class ColorPalette(
    @ColorInt val primary: Int,
    @ColorInt val onPrimary: Int,
    @ColorInt val secondary: Int,
    @ColorInt val onSecondary: Int,
    @ColorInt val background: Int,
    @ColorInt val onBackground: Int,
    @ColorInt val surface: Int,
    @ColorInt val onSurface: Int,
    @ColorInt val success: Int,
    @ColorInt val warning: Int,
    @ColorInt val error: Int,
    val heatmapColors: List<Int> = emptyList() // Gradient colors for heatmap
)
