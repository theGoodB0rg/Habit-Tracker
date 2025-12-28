package com.habittracker.analytics.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.analytics.data.repository.AnalyticsRepository
import com.habittracker.analytics.domain.models.*
import com.habittracker.analytics.domain.models.UserEngagementMode
import com.habittracker.analytics.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate

/**
 * Comprehensive ViewModel for analytics with modern state management and error handling
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val getAnalyticsDataUseCase: GetAnalyticsDataUseCase,
    private val exportAnalyticsUseCase: ExportAnalyticsUseCase,
    private val trackingUseCases: TrackingUseCases,
    private val coreHabitRepository: com.habittracker.core.HabitRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    // Selected time frame
    private val _selectedTimeFrame = MutableStateFlow(TimeFrame.MONTHLY)
    val selectedTimeFrame: StateFlow<TimeFrame> = _selectedTimeFrame.asStateFlow()

    // Analytics data
    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData.asStateFlow()

    // Chart data states
    private val _completionRateChartData = MutableStateFlow<List<CompletionRateChartPoint>>(emptyList())
    val completionRateChartData: StateFlow<List<CompletionRateChartPoint>> = _completionRateChartData.asStateFlow()

    private val _screenVisitChartData = MutableStateFlow<List<ScreenVisitChartPoint>>(emptyList())
    val screenVisitChartData: StateFlow<List<ScreenVisitChartPoint>> = _screenVisitChartData.asStateFlow()

    // Export state
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // User Engagement Mode
    private val _userEngagementMode = MutableStateFlow(UserEngagementMode.UNKNOWN)
    val userEngagementMode: StateFlow<UserEngagementMode> = _userEngagementMode.asStateFlow()

    init {
        // Load analytics data on initialization
        loadAnalyticsData()
        
        // React to time frame changes
        viewModelScope.launch {
            selectedTimeFrame.collect { timeFrame ->
                loadAnalyticsData(timeFrame)
            }
        }
    }

    /**
     * Load comprehensive analytics data
     */
    fun loadAnalyticsData(timeFrame: TimeFrame = _selectedTimeFrame.value) {
        viewModelScope.launch {
            try {
                analyticsRepository.getUserEngagementMode(timeFrame).collect { mode ->
                    _userEngagementMode.value = mode
                }
            } catch (e: Exception) {
                // Ignore error for engagement mode
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Fetch real habits from core repository
                val realHabits = try {
                    coreHabitRepository.getAllHabits().first()
                } catch (e: Exception) {
                    emptyList()
                }

                getAnalyticsDataUseCase(timeFrame).collect { analyticsData ->
                    // Merge real habits with analytics data
                    // 1. Map existing analytics to a map for quick lookup
                    val analyticsMap = analyticsData.habitCompletionRates.associateBy { it.habitId }
                    
                    // 2. Create a new list based on REAL habits
                    val mergedRates = realHabits.map { habit ->
                        val habitIdStr = habit.id.toString()
                        val existing = analyticsMap[habitIdStr]
                        
                        // If we have existing analytics data, use it but ensure the name is up to date
                        if (existing != null) {
                            existing.copy(habitName = habit.name)
                        } else {
                            // Otherwise create a fresh entry for the real habit
                            CompletionRate(
                                habitId = habitIdStr,
                                habitName = habit.name,
                                totalDays = 0,
                                completedDays = 0,
                                completionPercentage = 0.0,
                                currentStreak = 0, // Use 0 or fetch from habit if available, but habit.streakCount might not be reliable here without sync
                                longestStreak = 0,
                                weeklyAverage = 0.0,
                                monthlyAverage = 0.0,
                                timeFrame = timeFrame,
                                lastUpdated = LocalDate.now()
                            )
                        }
                    }

                    // 3. If we have real habits, use them. Otherwise fallback to analytics data (e.g. if DB is empty but analytics has sample data)
                    // But user wants to see REAL habits, so we should prioritize real habits.
                    // If realHabits is empty, maybe show empty state instead of sample data?
                    // The user complained about "placeholders", so we should probably hide them if they are not in real habits.
                    
                    val finalRates = if (realHabits.isNotEmpty()) mergedRates else emptyList() // Hide sample data if no real habits, or show nothing?
                    // Actually if realHabits is empty, finalRates is empty, which triggers "No Analytics Data" state, which is correct.
                    
                    val mergedData = analyticsData.copy(habitCompletionRates = finalRates)

                    _analyticsData.value = mergedData
                    processChartData(mergedData)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasData = mergedData.habitCompletionRates.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load analytics: ${e.message}"
                )
            }
        }
    }

    /**
     * Change selected time frame
     */
    fun selectTimeFrame(timeFrame: TimeFrame) {
        _selectedTimeFrame.value = timeFrame
    }

    /**
     * Refresh analytics data
     */
    fun refreshData() {
        loadAnalyticsData()
    }

    /**
     * Export analytics data in specified format
     */
    fun exportAnalytics(format: ExportFormat) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            
            try {
                val exportResult = exportAnalyticsUseCase(format, _selectedTimeFrame.value)
                _exportState.value = ExportState.Success(exportResult)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
            }
        }
    }

    /**
     * Clear export state
     */
    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }

    /**
     * Track screen visit for analytics
     */
    fun trackScreenVisit(screenName: String, fromScreen: String? = null) {
        viewModelScope.launch {
            try {
                trackingUseCases.trackScreenVisitUseCase(screenName, fromScreen)
            } catch (e: Exception) {
                android.util.Log.w("AnalyticsViewModel", "Failed to track screen visit", e)
            }
        }
    }

    /**
     * End screen visit tracking
     */
    fun endScreenVisit(interactionCount: Int = 0, bounced: Boolean = false) {
        viewModelScope.launch {
            try {
                trackingUseCases.endScreenVisitUseCase(interactionCount, bounced)
            } catch (e: Exception) {
                android.util.Log.w("AnalyticsViewModel", "Failed to end screen visit", e)
            }
        }
    }

    /**
     * Track a habit completion event. Useful for logging explicit user actions like
     * pressing "Complete now" from an overtime nudge.
     */
    fun trackHabitCompletion(
        habitId: String,
        habitName: String,
        isCompleted: Boolean,
        timeSpentMinutes: Int = 0,
        difficultyLevel: DifficultyLevel = DifficultyLevel.MODERATE
    ) {
        viewModelScope.launch {
            try {
                trackingUseCases.trackHabitCompletionUseCase(
                    habitId,
                    habitName,
                    isCompleted,
                    timeSpentMinutes,
                    difficultyLevel
                )
            } catch (e: Exception) {
                android.util.Log.w("AnalyticsViewModel", "Failed to track habit completion", e)
            }
        }
    }

    /**
     * Track timer action events emitted by the coordinator so analytics stay in sync.
     */
    fun trackTimerEvent(
        eventType: String,
        habitId: Long? = null,
        sessionId: Long? = null,
        source: String? = null,
        extra: Map<String, Any?> = emptyMap()
    ) {
        viewModelScope.launch {
            try {
                trackingUseCases.trackTimerEventUseCase(
                    eventType = eventType,
                    habitId = habitId,
                    sessionId = sessionId,
                    source = source,
                    extra = extra
                )
            } catch (e: Exception) {
                android.util.Log.w("AnalyticsViewModel", "Failed to track timer event", e)
            }
        }
    }
    /**
     * Process analytics data into chart-friendly formats
     */
    private fun processChartData(analyticsData: AnalyticsData) {
        // Process completion rate data for charts
        val completionChartData = analyticsData.habitCompletionRates.map { habit ->
            CompletionRateChartPoint(
                habitName = habit.habitName,
                completionRate = habit.completionPercentage.toFloat(),
                completedDays = habit.completedDays,
                currentStreak = habit.currentStreak,
                color = getHabitColor(habit.habitId)
            )
        }
        _completionRateChartData.value = completionChartData

        // Process screen visit data for charts
        val screenChartData = analyticsData.screenVisits.map { screen ->
            ScreenVisitChartPoint(
                screenName = screen.screenName,
                visitCount = screen.visitCount,
                engagementScore = screen.engagementScore.toFloat(),
                totalTime = screen.totalTimeSpent
            )
        }
        _screenVisitChartData.value = screenChartData
    }

    /**
     * Get color for habit visualization
     */
    private fun getHabitColor(habitId: String): Long {
        val colors = listOf(
            0xFF2196F3, 0xFF4CAF50, 0xFFFF9800, 0xFF9C27B0,
            0xFFF44336, 0xFF00BCD4, 0xFF8BC34A, 0xFFFFEB3B
        )
        return colors[habitId.hashCode().rem(colors.size).let { if (it < 0) it + colors.size else it }]
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for analytics screen
 */
data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val hasData: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

/**
 * Export state management
 */
sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val filePath: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

/**
 * Chart data models for visualization
 */
data class CompletionRateChartPoint(
    val habitName: String,
    val completionRate: Float,
    val completedDays: Int,
    val currentStreak: Int,
    val color: Long
)

data class ScreenVisitChartPoint(
    val screenName: String,
    val visitCount: Int,
    val engagementScore: Float,
    val totalTime: Long
)
