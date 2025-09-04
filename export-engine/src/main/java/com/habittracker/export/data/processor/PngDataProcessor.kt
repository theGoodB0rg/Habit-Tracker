package com.habittracker.export.data.processor

import android.graphics.Color
import com.habittracker.export.data.entity.HabitEntity
import com.habittracker.export.data.entity.HabitCompletionEntity
import com.habittracker.export.data.model.*
import com.habittracker.export.domain.model.ExportConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Professional data processor for PNG export preparation
 * Handles statistics calculation, heatmap generation, and achievement detection
 */
@Singleton
class PngDataProcessor @Inject constructor() {
    
    companion object {
        private const val DAYS_IN_HEATMAP = 365 // One year of data
        private const val MIN_STREAK_FOR_ACHIEVEMENT = 7
        private const val HIGH_COMPLETION_THRESHOLD = 0.8
        private const val EXCELLENT_COMPLETION_THRESHOLD = 0.9
    }
    
    /**
     * Transforms habit and completion data into PNG-ready export data
     */
    suspend fun preparePngExportData(
        habits: List<HabitEntity>,
        completions: Map<Long, List<HabitCompletionEntity>>,
        config: ExportConfig,
        customization: PngCustomization
    ): PngExportData = withContext(Dispatchers.Default) {
        
        val exportDate = LocalDate.now()
        val timeRange = calculateTimeRange(config, exportDate)
        
        // Calculate comprehensive statistics
        val statistics = calculateHabitStatistics(habits, completions, timeRange)
        
        // Generate heatmap data
        val heatmapData = generateHeatmapData(habits, completions, timeRange)
        
        // Detect and create achievements
        val achievements = detectAchievements(habits, completions, statistics)
        
        // Create habit summaries
        val habitSummaries = createHabitSummaries(habits, completions, timeRange)
        
        // Create metadata
        val metadata = PngExportMetadata(
            exportDate = exportDate,
            appVersion = "1.0.0", // Should come from BuildConfig
            userName = null, // Could be added later for personalization
            timeRange = timeRange,
            totalHabits = habits.size,
            activeHabits = habits.count { it.isActive },
            exportScope = config.scope.displayName
        )
        
        return@withContext PngExportData(
            metadata = metadata,
            statistics = statistics,
            heatmapData = heatmapData,
            achievements = achievements,
            habitSummaries = habitSummaries,
            customization = customization
        )
    }
    
    /**
     * Calculates comprehensive habit statistics
     */
    private fun calculateHabitStatistics(
        habits: List<HabitEntity>,
        completions: Map<Long, List<HabitCompletionEntity>>,
        timeRange: DateRange
    ): HabitStatistics {
        
        val allCompletions = completions.values.flatten()
        val filteredCompletions = filterCompletionsByDateRange(allCompletions, timeRange)
        
        // Basic statistics
        val totalCompletions = filteredCompletions.size
        val currentStreak = calculateCurrentStreak(allCompletions)
        val longestStreak = calculateLongestStreak(allCompletions)
        
        // Completion rate calculation
        val startDate = LocalDate.parse(timeRange.startDate)
        val endDate = LocalDate.parse(timeRange.endDate)
        val daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1
        val expectedCompletions = habits.size * daysBetween
        val completionRate = if (expectedCompletions > 0) {
            totalCompletions.toDouble() / expectedCompletions
        } else 0.0
        
        // Average completions per day
        val averageCompletionsPerDay = if (daysBetween > 0) {
            totalCompletions.toDouble() / daysBetween
        } else 0.0
        
        // Most productive day of week
        val mostProductiveDay = findMostProductiveDay(filteredCompletions)
        
        // Habit categories (simplified - could be enhanced with actual categories)
        val habitCategories = groupHabitsByFrequency(habits)
        
        // Weekly trends
        val weeklyTrends = calculateWeeklyTrends(filteredCompletions, habits, timeRange)
        
        // Monthly comparison (if enough data)
        val monthlyComparison = calculateMonthlyComparison(filteredCompletions, habits)
        
        return HabitStatistics(
            totalCompletions = totalCompletions,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completionRate = completionRate.coerceIn(0.0, 1.0),
            averageCompletionsPerDay = averageCompletionsPerDay,
            mostProductiveDay = mostProductiveDay,
            habitCategories = habitCategories,
            weeklyTrends = weeklyTrends,
            monthlyComparison = monthlyComparison
        )
    }
    
    /**
     * Generates heatmap data for visualization
     */
    private fun generateHeatmapData(
        habits: List<HabitEntity>,
        completions: Map<Long, List<HabitCompletionEntity>>,
        timeRange: DateRange
    ): HeatmapData {
        
        val startDate = LocalDate.parse(timeRange.startDate)
        val endDate = LocalDate.parse(timeRange.endDate)
        val allCompletions = completions.values.flatten()
        
        // Group completions by date
        val completionsByDate = allCompletions
            .mapNotNull { completion ->
                try {
                    completion.completedDate to completion
                } catch (e: Exception) {
                    null // Filter out invalid dates
                }
            }
            .groupBy { it.first }
            .mapValues { it.value.size }
        
        // Generate cells for each day in range
        val cells = mutableListOf<HeatmapCell>()
        var currentDate = startDate
        var maxCompletions = 0
        
        while (!currentDate.isAfter(endDate)) {
            val completionCount = completionsByDate[currentDate] ?: 0
            maxCompletions = maxOf(maxCompletions, completionCount)
            
            val habitNames = allCompletions
                .filter { 
                    try {
                        it.completedDate == currentDate
                    } catch (e: Exception) {
                        false
                    }
                }
                .mapNotNull { completion ->
                    habits.find { it.id == completion.habitId }?.name
                }
            
            cells.add(
                HeatmapCell(
                    date = currentDate,
                    completionCount = completionCount,
                    intensity = 0f, // Will be calculated after max is known
                    habitNames = habitNames
                )
            )
            
            currentDate = currentDate.plusDays(1)
        }
        
        // Calculate intensities
        val cellsWithIntensity = cells.map { cell ->
            val intensity = if (maxCompletions > 0) {
                cell.completionCount.toFloat() / maxCompletions
            } else 0f
            
            cell.copy(intensity = intensity)
        }
        
        // Create legend
        val legend = createHeatmapLegend(maxCompletions)
        
        return HeatmapData(
            cells = cellsWithIntensity,
            maxCompletions = maxCompletions,
            dateRange = timeRange,
            legend = legend
        )
    }
    
    /**
     * Detects achievements based on habit data
     */
    private fun detectAchievements(
        habits: List<HabitEntity>,
        completions: Map<Long, List<HabitCompletionEntity>>,
        statistics: HabitStatistics
    ): List<Achievement> {
        
        val achievements = mutableListOf<Achievement>()
        val recentDate = LocalDate.now().minusDays(7)
        
        // Streak achievements
        if (statistics.longestStreak >= 30) {
            achievements.add(
                Achievement(
                    id = "streak_30",
                    title = "Streak Master",
                    description = "Completed habits for 30 consecutive days",
                    iconResource = "🔥",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.STREAK,
                    isRecent = true
                )
            )
        } else if (statistics.longestStreak >= MIN_STREAK_FOR_ACHIEVEMENT) {
            achievements.add(
                Achievement(
                    id = "streak_7",
                    title = "Week Warrior",
                    description = "Completed habits for 7 consecutive days",
                    iconResource = "⚡",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.STREAK,
                    isRecent = statistics.currentStreak >= MIN_STREAK_FOR_ACHIEVEMENT
                )
            )
        }
        
        // Consistency achievements
        if (statistics.completionRate >= EXCELLENT_COMPLETION_THRESHOLD) {
            achievements.add(
                Achievement(
                    id = "consistency_excellent",
                    title = "Consistency Champion",
                    description = "Maintained 90%+ completion rate",
                    iconResource = "🏆",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.CONSISTENCY,
                    isRecent = true
                )
            )
        } else if (statistics.completionRate >= HIGH_COMPLETION_THRESHOLD) {
            achievements.add(
                Achievement(
                    id = "consistency_high",
                    title = "Steady Performer",
                    description = "Maintained 80%+ completion rate",
                    iconResource = "⭐",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.CONSISTENCY,
                    isRecent = true
                )
            )
        }
        
        // Milestone achievements
        if (statistics.totalCompletions >= 1000) {
            achievements.add(
                Achievement(
                    id = "milestone_1000",
                    title = "Thousand Club",
                    description = "Completed 1000+ habits",
                    iconResource = "💎",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.MILESTONE,
                    isRecent = false
                )
            )
        } else if (statistics.totalCompletions >= 100) {
            achievements.add(
                Achievement(
                    id = "milestone_100",
                    title = "Century Maker",
                    description = "Completed 100+ habits",
                    iconResource = "💯",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.MILESTONE,
                    isRecent = false
                )
            )
        }
        
        // Variety achievement
        if (habits.size >= 5) {
            achievements.add(
                Achievement(
                    id = "variety_5",
                    title = "Habit Collector",
                    description = "Created 5+ different habits",
                    iconResource = "🌟",
                    earnedDate = LocalDate.now(),
                    category = AchievementCategory.VARIETY,
                    isRecent = false
                )
            )
        }
        
        return achievements.sortedByDescending { it.isRecent }
    }
    
    /**
     * Creates summaries for individual habits
     */
    private fun createHabitSummaries(
        habits: List<HabitEntity>,
        completions: Map<Long, List<HabitCompletionEntity>>,
        timeRange: DateRange
    ): List<HabitSummary> {
        
        return habits.mapIndexed { index, habit ->
            val habitCompletions = completions[habit.id] ?: emptyList()
            val filteredCompletions = filterCompletionsByDateRange(habitCompletions, timeRange)
            
            val currentStreak = calculateHabitStreak(habitCompletions)
            val totalCompletions = filteredCompletions.size
            
            // Calculate completion rate for this specific habit
            val startDate = LocalDate.parse(timeRange.startDate)
            val endDate = LocalDate.parse(timeRange.endDate)
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1
            val completionRate = if (daysBetween > 0) {
                totalCompletions.toDouble() / daysBetween
            } else 0.0
            
            val recentTrend = calculateRecentTrend(habitCompletions)
            val habitColor = generateHabitColor(index, habit.iconId)
            
            HabitSummary(
                habitId = habit.id,
                name = habit.name,
                iconId = habit.iconId,
                currentStreak = currentStreak,
                completionRate = completionRate.coerceIn(0.0, 1.0),
                totalCompletions = totalCompletions,
                recentTrend = recentTrend,
                color = habitColor,
                isHighlighted = completionRate >= HIGH_COMPLETION_THRESHOLD
            )
        }.sortedByDescending { it.completionRate }
    }
    
    /**
     * Helper methods for calculations
     */
    
    private fun calculateTimeRange(config: ExportConfig, exportDate: LocalDate): DateRange {
        val endDate = config.endDate?.let { LocalDate.parse(it) } ?: exportDate
        val startDate = config.startDate?.let { LocalDate.parse(it) } 
            ?: endDate.minusMonths(3) // Default to 3 months
        
        return DateRange(
            startDate = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }
    
    private fun filterCompletionsByDateRange(
        completions: List<HabitCompletionEntity>,
        timeRange: DateRange
    ): List<HabitCompletionEntity> {
        val startDate = LocalDate.parse(timeRange.startDate)
        val endDate = LocalDate.parse(timeRange.endDate)
        
        return completions.filter { completion ->
            try {
                val completionDate = completion.completedDate
                !completionDate.isBefore(startDate) && !completionDate.isAfter(endDate)
            } catch (e: Exception) {
                false // Exclude invalid dates
            }
        }
    }
    
    private fun calculateCurrentStreak(completions: List<HabitCompletionEntity>): Int {
        if (completions.isEmpty()) return 0
        
        val sortedCompletions = completions
            .sortedByDescending { 
                try {
                    it.completedDate
                } catch (e: Exception) {
                    LocalDate.MIN // Put invalid dates at the end
                }
            }
        
        var streak = 0
        var currentDate = LocalDate.now()
        
        for (completion in sortedCompletions) {
            try {
                val completionDate = completion.completedDate
                
                if (completionDate == currentDate || completionDate == currentDate.minusDays(1)) {
                    streak++
                    currentDate = completionDate.minusDays(1)
                } else {
                    break
                }
            } catch (e: Exception) {
                // Skip invalid dates
                continue
            }
        }
        
        return streak
    }
    
    private fun calculateLongestStreak(completions: List<HabitCompletionEntity>): Int {
        if (completions.isEmpty()) return 0
        
        val sortedDates = completions
            .mapNotNull { 
                try {
                    it.completedDate
                } catch (e: Exception) {
                    null // Filter out invalid dates
                }
            }
            .distinct()
            .sorted()
        
        var longestStreak = 1
        var currentStreak = 1
        
        for (i in 1 until sortedDates.size) {
            if (ChronoUnit.DAYS.between(sortedDates[i-1], sortedDates[i]) == 1L) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return longestStreak
    }
    
    private fun calculateHabitStreak(completions: List<HabitCompletionEntity>): Int {
        return calculateCurrentStreak(completions)
    }
    
    private fun findMostProductiveDay(completions: List<HabitCompletionEntity>): String {
        if (completions.isEmpty()) return "No data"
        
        val dayFrequency = completions
            .mapNotNull { completion ->
                try {
                    completion.completedDate.dayOfWeek
                } catch (e: Exception) {
                    null // Filter out invalid dates
                }
            }
            .groupBy { it }
            .mapValues { it.value.size }
        
        val mostProductiveDay = dayFrequency.maxByOrNull { it.value }?.key
            ?: return "No data"
        
        return when (mostProductiveDay) {
            DayOfWeek.MONDAY -> "Monday"
            DayOfWeek.TUESDAY -> "Tuesday"
            DayOfWeek.WEDNESDAY -> "Wednesday"
            DayOfWeek.THURSDAY -> "Thursday"
            DayOfWeek.FRIDAY -> "Friday"
            DayOfWeek.SATURDAY -> "Saturday"
            DayOfWeek.SUNDAY -> "Sunday"
        }
    }
    
    private fun groupHabitsByFrequency(habits: List<HabitEntity>): Map<String, Int> {
        return habits.groupBy { it.frequency.name }
            .mapValues { it.value.size }
    }
    
    private fun calculateWeeklyTrends(
        completions: List<HabitCompletionEntity>,
        habits: List<HabitEntity>,
        timeRange: DateRange
    ): List<WeeklyTrend> {
        
        val startDate = LocalDate.parse(timeRange.startDate)
        val endDate = LocalDate.parse(timeRange.endDate)
        
        val weeklyData = mutableListOf<WeeklyTrend>()
        var currentWeekStart = startDate.with(DayOfWeek.MONDAY)
        
        while (currentWeekStart.isBefore(endDate)) {
            val weekEnd = currentWeekStart.plusDays(6)
            
            val weekCompletions = completions.filter { completion ->
                try {
                    val completionDate = completion.completedDate
                    !completionDate.isBefore(currentWeekStart) && !completionDate.isAfter(weekEnd)
                } catch (e: Exception) {
                    false
                }
            }
            
            val completionCount = weekCompletions.size
            val habitCount = weekCompletions.map { it.habitId }.distinct().size
            val averageCompletionRate = if (habits.isNotEmpty()) {
                completionCount.toDouble() / (habits.size * 7)
            } else 0.0
            
            weeklyData.add(
                WeeklyTrend(
                    weekStartDate = currentWeekStart,
                    completionCount = completionCount,
                    habitCount = habitCount,
                    averageCompletionRate = averageCompletionRate.coerceIn(0.0, 1.0)
                )
            )
            
            currentWeekStart = currentWeekStart.plusWeeks(1)
        }
        
        return weeklyData
    }
    
    private fun calculateMonthlyComparison(
        completions: List<HabitCompletionEntity>,
        habits: List<HabitEntity>
    ): MonthlyComparison? {
        
        val now = LocalDate.now()
        val currentMonthStart = now.withDayOfMonth(1)
        val previousMonthStart = currentMonthStart.minusMonths(1)
        val previousMonthEnd = currentMonthStart.minusDays(1)
        
        // Current month data
        val currentMonthCompletions = completions.filter { completion ->
            try {
                val completionDate = completion.completedDate
                !completionDate.isBefore(currentMonthStart)
            } catch (e: Exception) {
                false
            }
        }
        
        // Previous month data
        val previousMonthCompletions = completions.filter { completion ->
            try {
                val completionDate = completion.completedDate
                !completionDate.isBefore(previousMonthStart) && !completionDate.isAfter(previousMonthEnd)
            } catch (e: Exception) {
                false
            }
        }
        
        if (previousMonthCompletions.isEmpty()) return null
        
        val currentMonth = MonthData(
            monthName = now.month.name,
            year = now.year,
            totalCompletions = currentMonthCompletions.size,
            averageCompletionRate = calculateMonthCompletionRate(currentMonthCompletions, habits, now.lengthOfMonth()),
            activeHabits = currentMonthCompletions.map { it.habitId }.distinct().size
        )
        
        val previousMonth = MonthData(
            monthName = previousMonthStart.month.name,
            year = previousMonthStart.year,
            totalCompletions = previousMonthCompletions.size,
            averageCompletionRate = calculateMonthCompletionRate(previousMonthCompletions, habits, previousMonthStart.lengthOfMonth()),
            activeHabits = previousMonthCompletions.map { it.habitId }.distinct().size
        )
        
        val percentageChange = if (previousMonth.totalCompletions > 0) {
            ((currentMonth.totalCompletions - previousMonth.totalCompletions).toDouble() / previousMonth.totalCompletions) * 100
        } else 0.0
        
        return MonthlyComparison(
            currentMonth = currentMonth,
            previousMonth = previousMonth,
            percentageChange = percentageChange,
            improvement = percentageChange > 0
        )
    }
    
    private fun calculateMonthCompletionRate(
        completions: List<HabitCompletionEntity>,
        habits: List<HabitEntity>,
        daysInMonth: Int
    ): Double {
        val expectedCompletions = habits.size * daysInMonth
        return if (expectedCompletions > 0) {
            completions.size.toDouble() / expectedCompletions
        } else 0.0
    }
    
    private fun calculateRecentTrend(completions: List<HabitCompletionEntity>): TrendDirection {
        if (completions.size < 7) return TrendDirection.NEW
        
        val recentCompletions = completions
            .sortedByDescending { it.completedDate }
            .take(14)
        
        val firstWeek = recentCompletions.drop(7).size
        val secondWeek = recentCompletions.take(7).size
        
        return when {
            secondWeek > firstWeek -> TrendDirection.UP
            secondWeek < firstWeek -> TrendDirection.DOWN
            else -> TrendDirection.STABLE
        }
    }
    
    private fun generateHabitColor(index: Int, iconId: Int): Int {
        // Generate pleasant colors for habits
        val colors = listOf(
            Color.parseColor("#FF6B6B"), // Red
            Color.parseColor("#4ECDC4"), // Teal
            Color.parseColor("#45B7D1"), // Blue
            Color.parseColor("#96CEB4"), // Green
            Color.parseColor("#FFEAA7"), // Yellow
            Color.parseColor("#DDA0DD"), // Plum
            Color.parseColor("#FFA07A"), // Light Salmon
            Color.parseColor("#87CEEB"), // Sky Blue
            Color.parseColor("#98D8C8"), // Mint
            Color.parseColor("#F7DC6F")  // Light Gold
        )
        
        return colors[index % colors.size]
    }
    
    private fun createHeatmapLegend(maxCompletions: Int): HeatmapLegend {
        val intensityLevels = when {
            maxCompletions <= 1 -> listOf(
                IntensityLevel("0", Color.parseColor("#EBEDF0"), 0f),
                IntensityLevel("1", Color.parseColor("#C6E48B"), 1f)
            )
            maxCompletions <= 3 -> listOf(
                IntensityLevel("0", Color.parseColor("#EBEDF0"), 0f),
                IntensityLevel("1", Color.parseColor("#C6E48B"), 0.33f),
                IntensityLevel("2", Color.parseColor("#7BC96F"), 0.66f),
                IntensityLevel("3+", Color.parseColor("#239A3B"), 1f)
            )
            else -> listOf(
                IntensityLevel("0", Color.parseColor("#EBEDF0"), 0f),
                IntensityLevel("1-2", Color.parseColor("#C6E48B"), 0.25f),
                IntensityLevel("3-4", Color.parseColor("#7BC96F"), 0.5f),
                IntensityLevel("5-6", Color.parseColor("#239A3B"), 0.75f),
                IntensityLevel("7+", Color.parseColor("#196127"), 1f)
            )
        }
        
        return HeatmapLegend(
            intensityLevels = intensityLevels,
            title = "Daily Completions"
        )
    }
}
