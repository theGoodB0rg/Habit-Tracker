package com.habittracker.analytics.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.analytics.data.repository.AnalyticsRepository
import com.habittracker.analytics.domain.models.*
import com.habittracker.analytics.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Comprehensive ViewModel for analytics with modern state management and error handling
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val getAnalyticsDataUseCase: GetAnalyticsDataUseCase,
    private val exportAnalyticsUseCase: ExportAnalyticsUseCase,
    private val trackingUseCases: TrackingUseCases
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                getAnalyticsDataUseCase(timeFrame).collect { analyticsData ->
                    _analyticsData.value = analyticsData
                    processChartData(analyticsData)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasData = analyticsData.habitCompletionRates.isNotEmpty()
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
     * Process analytics data into chart-friendly formats
     */
    private fun processChartData(analyticsData: AnalyticsData) {
        // Process completion rate data for charts
        val completionChartData = analyticsData.habitCompletionRates.map { habit ->
            CompletionRateChartPoint(
                habitName = habit.habitName,
                completionRate = habit.completionPercentage.toFloat(),
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
    val currentStreak: Int,
    val color: Long
)

data class ScreenVisitChartPoint(
    val screenName: String,
    val visitCount: Int,
    val engagementScore: Float,
    val totalTime: Long
)