package com.habittracker.analytics.data.initialization

import com.habittracker.analytics.data.database.AnalyticsDao
import com.habittracker.analytics.data.database.entities.*
import com.habittracker.analytics.domain.models.DifficultyLevel
import com.habittracker.analytics.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Initializes analytics database with comprehensive sample data for demonstration
 * This creates realistic analytics data to showcase the full functionality
 */
@Singleton
class AnalyticsDataInitializer @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val dateUtils: DateUtils
) {
    
    suspend fun initializeSampleData() = withContext(Dispatchers.IO) {
        // Check if data already exists
        val existingHabits = analyticsDao.getUniqueHabitsCount(
            startDate = dateUtils.getDateString(LocalDate.now().minusDays(30)),
            endDate = dateUtils.getCurrentDateString()
        )
        
        if (existingHabits > 0) {
            return@withContext // Data already exists
        }
        
        // Create sample habits data
        val sampleHabits = listOf(
            HabitData("1", "Morning Exercise", DifficultyLevel.MODERATE),
            HabitData("2", "Read for 30 minutes", DifficultyLevel.EASY),
            HabitData("3", "Meditate", DifficultyLevel.EASY),
            HabitData("4", "Drink 8 glasses of water", DifficultyLevel.EASY),
            HabitData("5", "Learn a new language", DifficultyLevel.HARD),
            HabitData("6", "Write in journal", DifficultyLevel.MODERATE),
            HabitData("7", "Practice instrument", DifficultyLevel.HARD),
            HabitData("8", "Healthy meal prep", DifficultyLevel.MODERATE)
        )
        
        // Generate data for the last 60 days
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(60)
        
        generateHabitCompletionData(sampleHabits, startDate, endDate)
        generateScreenVisitData(startDate, endDate)
        generateUserEngagementData(startDate, endDate)
        generateAppSessionData(startDate, endDate)
        generatePerformanceMetrics(startDate, endDate)
    }
    
    private suspend fun generateHabitCompletionData(
        habits: List<HabitData>,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            for (habit in habits) {
                // Generate realistic completion patterns
                val completionProbability = when (habit.difficulty) {
                    DifficultyLevel.EASY -> 0.85
                    DifficultyLevel.MODERATE -> 0.70
                    DifficultyLevel.HARD -> 0.55
                    DifficultyLevel.EXPERT -> 0.40
                }
                
                val isCompleted = Random.nextDouble() < completionProbability
                val timeSpent = if (isCompleted) {
                    when (habit.difficulty) {
                        DifficultyLevel.EASY -> Random.nextInt(10, 20)
                        DifficultyLevel.MODERATE -> Random.nextInt(20, 45)
                        DifficultyLevel.HARD -> Random.nextInt(30, 60)
                        DifficultyLevel.EXPERT -> Random.nextInt(45, 90)
                    }
                } else 0
                
                // Calculate streak
                val streak = if (isCompleted) calculateStreak(habit.id, currentDate) else 0
                
                val completion = HabitCompletionAnalyticsEntity(
                    habitId = habit.id,
                    habitName = habit.name,
                    isCompleted = isCompleted,
                    completionTimestamp = currentDate.atStartOfDay()
                        .toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
                    date = dateUtils.getDateString(currentDate),
                    streakCount = streak,
                    difficultyLevel = habit.difficulty.name,
                    timeSpentMinutes = timeSpent,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                analyticsDao.insertHabitCompletionAnalytics(completion)
                
                // Create streak retention entry if completed
                if (isCompleted) {
                    val streakRetention = StreakRetentionAnalyticsEntity(
                        habitId = habit.id,
                        habitName = habit.name,
                        streakLength = streak,
                        startDate = dateUtils.getDateString(currentDate),
                        endDate = null,
                        isActive = true,
                        maxStreakLength = streak,
                        difficultyLevel = habit.difficulty.name,
                        retentionProbability = calculateRetentionProbability(streak, habit.difficulty),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    analyticsDao.insertStreakRetentionAnalytics(streakRetention)
                }
            }
            currentDate = currentDate.plusDays(1)
        }
    }
    
    private suspend fun generateScreenVisitData(startDate: LocalDate, endDate: LocalDate) {
        val screens = listOf(
            "MainScreen", "AddHabitScreen", "EditHabitScreen", "HabitDetailScreen",
            "AnalyticsScreen", "SettingsScreen", "AboutScreen"
        )
        
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            // Generate 3-8 screen visits per day
            val visitsToday = Random.nextInt(3, 9)
            val sessionId = UUID.randomUUID().toString()
            
            repeat(visitsToday) { visitIndex ->
                val screen = screens.random()
                val entryTime = currentDate.atStartOfDay()
                    .plusHours(Random.nextLong(8, 22))
                    .plusMinutes(Random.nextLong(0, 60))
                    .toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                
                val timeSpent = Random.nextLong(5000, 180000) // 5 seconds to 3 minutes
                val interactionCount = Random.nextInt(1, 20)
                val bounced = timeSpent < 10000 // Less than 10 seconds is considered bounced
                
                val screenVisit = ScreenVisitAnalyticsEntity(
                    screenName = screen,
                    sessionId = "$sessionId-$visitIndex",
                    entryTimestamp = entryTime,
                    exitTimestamp = entryTime + timeSpent,
                    timeSpentMs = timeSpent,
                    date = dateUtils.getDateString(currentDate),
                    fromScreen = if (visitIndex > 0) screens.random() else null,
                    interactionCount = interactionCount,
                    bounced = bounced,
                    createdAt = System.currentTimeMillis()
                )
                
                analyticsDao.insertScreenVisitAnalytics(screenVisit)
            }
            
            currentDate = currentDate.plusDays(1)
        }
    }
    
    private suspend fun generateUserEngagementData(startDate: LocalDate, endDate: LocalDate) {
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val sessionCount = Random.nextInt(1, 5)
            val totalTimeSpent = Random.nextLong(300000, 1800000) // 5-30 minutes
            val habitsCompleted = Random.nextInt(0, 6)
            val habitsInteracted = Random.nextInt(habitsCompleted, 8)
            val deepEngagement = totalTimeSpent > 900000 && habitsCompleted > 2
            
            val engagement = UserEngagementEntity(
                date = dateUtils.getDateString(currentDate),
                sessionCount = sessionCount,
                totalTimeSpentMs = totalTimeSpent,
                averageSessionMs = totalTimeSpent / sessionCount,
                screenTransitions = Random.nextInt(10, 50),
                habitsCompleted = habitsCompleted,
                habitsInteracted = habitsInteracted,
                deepEngagement = deepEngagement,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            analyticsDao.insertUserEngagement(engagement)
            
            currentDate = currentDate.plusDays(1)
        }
    }
    
    private suspend fun generateAppSessionData(startDate: LocalDate, endDate: LocalDate) {
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            // Generate 1-3 app sessions per day
            val sessionsToday = Random.nextInt(1, 4)
            
            repeat(sessionsToday) { sessionIndex ->
                val sessionId = UUID.randomUUID().toString()
                val startTime = currentDate.atStartOfDay()
                    .plusHours(Random.nextLong(6, 23))
                    .plusMinutes(Random.nextLong(0, 60))
                    .toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                
                val duration = Random.nextLong(60000, 1200000) // 1-20 minutes
                
                val session = AppSessionEntity(
                    sessionId = sessionId,
                    startTimestamp = startTime,
                    endTimestamp = startTime + duration,
                    durationMs = duration,
                    startDate = dateUtils.getDateString(currentDate),
                    endDate = dateUtils.getDateString(currentDate),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                analyticsDao.insertAppSession(session)
            }
            
            currentDate = currentDate.plusDays(1)
        }
    }
    
    private suspend fun generatePerformanceMetrics(startDate: LocalDate, endDate: LocalDate) {
        val metricTypes = listOf("app_startup_time", "database_query_time", "ui_render_time")
        
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            for (metricType in metricTypes) {
                val value = when (metricType) {
                    "app_startup_time" -> Random.nextDouble(800.0, 3000.0) // ms
                    "database_query_time" -> Random.nextDouble(5.0, 50.0) // ms
                    "ui_render_time" -> Random.nextDouble(16.0, 32.0) // ms
                    else -> Random.nextDouble(10.0, 100.0)
                }
                
                val metric = PerformanceMetricEntity(
                    metricType = metricType,
                    value = value,
                    unit = when (metricType) {
                        "app_startup_time", "database_query_time", "ui_render_time" -> "ms"
                        else -> "units"
                    },
                    date = dateUtils.getDateString(currentDate),
                    createdAt = currentDate.atStartOfDay()
                        .toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                )
                
                analyticsDao.insertPerformanceMetric(metric)
            }
            
            currentDate = currentDate.plusDays(1)
        }
    }
    
    private suspend fun calculateStreak(habitId: String, currentDate: LocalDate): Int {
        // Simple streak calculation for demo purposes
        val yesterday = currentDate.minusDays(1)
        val recentCompletions = analyticsDao.getHabitCompletions(
            habitId = habitId,
            startDate = dateUtils.getDateString(yesterday.minusDays(30)),
            endDate = dateUtils.getDateString(yesterday)
        )
        
        // This is a simplified calculation - in real implementation, 
        // it would check consecutive days
        return Random.nextInt(0, 15) // Demo streak
    }
    
    private fun calculateRetentionProbability(streakLength: Int, difficulty: DifficultyLevel): Double {
        val baseRetention = when (difficulty) {
            DifficultyLevel.EASY -> 0.9
            DifficultyLevel.MODERATE -> 0.7
            DifficultyLevel.HARD -> 0.5
            DifficultyLevel.EXPERT -> 0.3
        }
        
        val streakFactor = 1.0 - (streakLength * 0.01)
        return (baseRetention * streakFactor).coerceIn(0.1, 0.95)
    }
}

private data class HabitData(
    val id: String,
    val name: String,
    val difficulty: DifficultyLevel
)
