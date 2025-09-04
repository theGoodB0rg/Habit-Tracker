package com.habittracker.ui.viewmodels.timing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.habittracker.data.repository.HabitRepository
import com.habittracker.data.preferences.TimingPreferencesRepository
import com.habittracker.data.preferences.BehaviorMetricsRepository
import com.habittracker.ui.models.timing.*
import javax.inject.Inject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Phase 2: Progressive Feature Discovery ViewModel
 * 
 * Manages user engagement tracking and gradual feature introduction
 * without overwhelming users with complexity
 */

@HiltViewModel
class TimingFeatureViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val timingPreferencesRepository: TimingPreferencesRepository,
    private val behaviorMetricsRepository: BehaviorMetricsRepository
) : ViewModel() {
    
    private val featureGradualizer = FeatureGradualizer()
    
    // User engagement state
    private val _userEngagementLevel: MutableStateFlow<UserEngagementLevel> = MutableStateFlow(UserEngagementLevel.Casual)
    val userEngagementLevel: StateFlow<UserEngagementLevel> = _userEngagementLevel.asStateFlow()
    
    private val _userBehaviorMetrics = MutableStateFlow(UserBehaviorMetrics())
    val userBehaviorMetrics: StateFlow<UserBehaviorMetrics> = _userBehaviorMetrics.asStateFlow()
    
    private val _smartTimingPreferences = MutableStateFlow(SmartTimingPreferences())
    val smartTimingPreferences: StateFlow<SmartTimingPreferences> = _smartTimingPreferences.asStateFlow()
    
    // Error surfaces for DataStore/DB
    private val _errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    // Level up notifications
    private val _pendingLevelUp = MutableStateFlow<LevelUpNotification?>(null)
    val pendingLevelUp: StateFlow<LevelUpNotification?> = _pendingLevelUp.asStateFlow()
    
    // Available features for current level
    val availableFeatures: StateFlow<List<Feature>> = userEngagementLevel.map { level ->
        featureGradualizer.getAvailableFeatures(level)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(Feature.BASIC_TRACKING)
    )
    
    // Next level benefits
    val nextLevelBenefits: StateFlow<List<String>> = userEngagementLevel.map { level ->
        featureGradualizer.getNextLevelBenefits(level)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Progress to next level (0.0 to 1.0)
    val progressToNextLevel: StateFlow<Float> = combine(
        userEngagementLevel,
        userBehaviorMetrics
    ) { level, metrics ->
        calculateProgressToNextLevel(level, metrics)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )
    
    init {
        loadUserPreferences()
        startEngagementTracking()
    observeMetricsPersistence()
    }
    
    // MARK: - Public Methods
    
    fun recordHabitCompletion(habitId: Long) {
        viewModelScope.launch {
            updateBehaviorMetrics { metrics ->
                val newCompletions = metrics.habitCompletionRate + 0.1f
                val newTrackingDays = if (isNewDay()) metrics.consistentTrackingDays + 1 
                                     else metrics.consistentTrackingDays
                
                metrics.copy(
                    habitCompletionRate = minOf(1f, newCompletions),
                    consistentTrackingDays = newTrackingDays,
                    lastEngagementDate = System.currentTimeMillis()
                )
            }
            
            checkForLevelUp()
        }
    }
    
    fun recordTimerUsage(habitId: Long, completed: Boolean) {
        viewModelScope.launch {
            updateBehaviorMetrics { metrics ->
                val newUsageCount = metrics.timerUsageCount + 1
                val newCompletionRate = if (completed) {
                    ((metrics.timerCompletionRate * metrics.timerUsageCount) + 1f) / newUsageCount
                } else {
                    (metrics.timerCompletionRate * metrics.timerUsageCount) / newUsageCount
                }
                
                metrics.copy(
                    timerUsageCount = newUsageCount,
                    timerCompletionRate = newCompletionRate,
                    lastEngagementDate = System.currentTimeMillis()
                )
            }
            
            checkForLevelUp()
        }
    }
    
    fun recordSuggestionInteraction(suggestion: SmartSuggestion, accepted: Boolean) {
        viewModelScope.launch {
            updateBehaviorMetrics { metrics ->
                val currentAcceptances = metrics.suggestionAcceptanceRate * 
                                       (metrics.advancedFeatureUsage[Feature.SMART_SUGGESTIONS] ?: 0)
                val newTotal = (metrics.advancedFeatureUsage[Feature.SMART_SUGGESTIONS] ?: 0) + 1
                val newAcceptanceRate = if (accepted) {
                    (currentAcceptances + 1f) / newTotal
                } else {
                    currentAcceptances / newTotal
                }
                
                metrics.copy(
                    suggestionAcceptanceRate = newAcceptanceRate,
                    advancedFeatureUsage = metrics.advancedFeatureUsage + 
                                         (Feature.SMART_SUGGESTIONS to newTotal),
                    lastEngagementDate = System.currentTimeMillis()
                )
            }
            
            checkForLevelUp()
        }
    }
    
    fun recordSchedulingInteraction() {
        viewModelScope.launch {
            updateBehaviorMetrics { metrics ->
                metrics.copy(
                    schedulingInteractions = metrics.schedulingInteractions + 1,
                    lastEngagementDate = System.currentTimeMillis()
                )
            }
            
            checkForLevelUp()
        }
    }
    
    fun acceptLevelUp() {
        viewModelScope.launch {
            _pendingLevelUp.value?.let { notification ->
                _userEngagementLevel.value = notification.toLevel
                _smartTimingPreferences.value = _smartTimingPreferences.value.copy(
                    currentEngagementLevel = notification.toLevel
                )
                
                // Record level up event
                recordLevelUpEvent(notification, accepted = true)
                
                // Save preferences
                saveUserPreferences()
                
                // Clear notification
                _pendingLevelUp.value = null
            }
        }
    }
    
    fun dismissLevelUp() {
        viewModelScope.launch {
            _pendingLevelUp.value?.let { notification ->
                recordLevelUpEvent(notification, accepted = false)
                _pendingLevelUp.value = null
            }
        }
    }
    
    fun updateTimingPreferences(preferences: SmartTimingPreferences) {
        viewModelScope.launch {
            _smartTimingPreferences.value = preferences
            saveUserPreferences()
        }
    }
    
    fun enableFeature(feature: Feature) {
        viewModelScope.launch {
            val currentPrefs = _smartTimingPreferences.value
            val updatedPrefs = when (feature) {
                Feature.SIMPLE_TIMER -> currentPrefs.copy(enableTimers = true)
                Feature.SMART_SUGGESTIONS -> currentPrefs.copy(enableSmartSuggestions = true)
                Feature.CONTEXT_AWARENESS -> currentPrefs.copy(enableContextAwareness = true)
                Feature.HABIT_STACKING -> currentPrefs.copy(enableHabitStacking = true)
                else -> currentPrefs
            }
            
            _smartTimingPreferences.value = updatedPrefs
            saveUserPreferences()
            
            // Record feature usage
            recordFeatureUsage(feature)
        }
    }
    
    // MARK: - Private Methods
    
    private fun loadUserPreferences() {
        viewModelScope.launch {
            // Observe DataStore-backed timing preferences and map into SmartTimingPreferences.
            timingPreferencesRepository.preferences()
                .catch { e -> _errorMessages.tryEmit("Failed to load timing preferences") }
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collect { prefs ->
                    // Merge persisted fields into the in-memory preferences model
                    _smartTimingPreferences.value = _smartTimingPreferences.value.copy(
                        enableTimers = prefs.enableTimers,
                        enableSmartSuggestions = prefs.enableSmartSuggestions,
                        enableContextAwareness = prefs.enableContextAwareness,
                        enableHabitStacking = prefs.enableHabitStacking,
                        autoLevelUp = prefs.autoLevelUp,
                        showLevelUpPrompts = prefs.showLevelUpPrompts,
                        preferredReminderStyle = prefs.reminderStyle,
                        timerDefaultDuration = prefs.timerDefaultDuration
                    )
                    // Keep engagement level unchanged (not persisted yet)
                    _userEngagementLevel.value = _smartTimingPreferences.value.currentEngagementLevel
                }
        }
    }
    
    private fun saveUserPreferences() {
        viewModelScope.launch {
            val prefs = _smartTimingPreferences.value
            // Persist overlapping fields to DataStore
            try {
                timingPreferencesRepository.setEnableTimers(prefs.enableTimers)
                timingPreferencesRepository.setEnableSmartSuggestions(prefs.enableSmartSuggestions)
                timingPreferencesRepository.setEnableContextAwareness(prefs.enableContextAwareness)
                timingPreferencesRepository.setEnableHabitStacking(prefs.enableHabitStacking)
                timingPreferencesRepository.setAutoLevelUp(prefs.autoLevelUp)
                timingPreferencesRepository.setShowLevelUpPrompts(prefs.showLevelUpPrompts)
                timingPreferencesRepository.setReminderStyle(prefs.preferredReminderStyle)
                timingPreferencesRepository.setTimerDefaultDuration(prefs.timerDefaultDuration)
            } catch (e: Exception) {
                _errorMessages.tryEmit("Failed to save preferences")
            }
            // Future: persist additional SmartTimingPreferences fields as repository evolves
        }
    }
    
    private fun startEngagementTracking() {
        viewModelScope.launch {
            // Load and observe behavior metrics from DataStore
            behaviorMetricsRepository.metrics()
                .catch { _errorMessages.tryEmit("Failed to load behavior metrics") }
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collect { metrics -> _userBehaviorMetrics.value = metrics }
        }
    }

    private fun observeMetricsPersistence() {
        viewModelScope.launch {
            userBehaviorMetrics
                .drop(1) // avoid writing initial default immediately
                .flowOn(Dispatchers.Default)
                .collect { metrics ->
                    // Persist every update; repository can debounce if needed in future
                    try {
                        behaviorMetricsRepository.setMetrics(metrics)
                    } catch (e: Exception) {
                        _errorMessages.tryEmit("Failed to persist metrics")
                    }
                }
        }
    }
    
    private suspend fun updateBehaviorMetrics(update: (UserBehaviorMetrics) -> UserBehaviorMetrics) {
        val currentMetrics = _userBehaviorMetrics.value
        val updatedMetrics = update(currentMetrics)
        _userBehaviorMetrics.value = updatedMetrics
        
        // Persist immediately to repository to avoid races; observeMetricsPersistence also persists as a backup
        try {
            behaviorMetricsRepository.setMetrics(updatedMetrics)
        } catch (e: Exception) {
            _errorMessages.tryEmit("Failed to persist metrics")
        }
    }
    
    private suspend fun checkForLevelUp() {
        val currentLevel = _userEngagementLevel.value
        val metrics = _userBehaviorMetrics.value
        
        if (featureGradualizer.shouldLevelUp(currentLevel, metrics)) {
            val nextLevel = getNextLevel(currentLevel)
            if (nextLevel != null) {
                val notification = LevelUpNotification.create(currentLevel, nextLevel)
                if (_smartTimingPreferences.value.showLevelUpPrompts) {
                    _pendingLevelUp.value = notification
                } else {
                    // Auto-advance without prompt when disabled
                    _userEngagementLevel.value = notification.toLevel
                    _smartTimingPreferences.value = _smartTimingPreferences.value.copy(
                        currentEngagementLevel = notification.toLevel
                    )
                    recordLevelUpEvent(notification, accepted = true)
                    saveUserPreferences()
                }
            }
        }
    }
    
    private fun getNextLevel(current: UserEngagementLevel): UserEngagementLevel? {
        return when (current) {
            UserEngagementLevel.Casual -> UserEngagementLevel.Interested
            UserEngagementLevel.Interested -> UserEngagementLevel.Engaged
            UserEngagementLevel.Engaged -> UserEngagementLevel.PowerUser
            UserEngagementLevel.PowerUser -> null
        }
    }
    
    private fun calculateProgressToNextLevel(
        level: UserEngagementLevel,
        metrics: UserBehaviorMetrics
    ): Float {
        return when (level) {
            UserEngagementLevel.Casual -> {
                val trackingProgress = minOf(1f, metrics.consistentTrackingDays / 7f)
                val completionProgress = metrics.habitCompletionRate
                (trackingProgress + completionProgress) / 2f
            }
            
            UserEngagementLevel.Interested -> {
                val timerProgress = minOf(1f, metrics.timerUsageCount / 5f)
                val completionProgress = metrics.timerCompletionRate
                (timerProgress + completionProgress) / 2f
            }
            
            UserEngagementLevel.Engaged -> {
                val suggestionProgress = metrics.suggestionAcceptanceRate / 0.4f
                val schedulingProgress = minOf(1f, metrics.schedulingInteractions / 3f)
                (suggestionProgress + schedulingProgress) / 2f
            }
            
            UserEngagementLevel.PowerUser -> 1f // Already at max level
        }
    }
    
    private fun isNewDay(): Boolean {
        val lastEngagement = _userBehaviorMetrics.value.lastEngagementDate
        val today = System.currentTimeMillis()
        val daysBetween = ChronoUnit.DAYS.between(
            java.time.Instant.ofEpochMilli(lastEngagement),
            java.time.Instant.ofEpochMilli(today)
        )
        return daysBetween >= 1
    }
    
    private fun recordLevelUpEvent(notification: LevelUpNotification, accepted: Boolean) {
        viewModelScope.launch {
            val event = LevelUpEvent(
                timestamp = System.currentTimeMillis(),
                fromLevel = notification.fromLevel,
                toLevel = notification.toLevel,
                triggerMetric = "behavior_threshold",
                userAccepted = accepted
            )
            // Append event to metrics for persistence
            updateBehaviorMetrics { metrics ->
                metrics.copy(levelUpEvents = metrics.levelUpEvents + event)
            }
        }
    }
    
    private fun recordFeatureUsage(feature: Feature) {
        viewModelScope.launch {
            updateBehaviorMetrics { metrics ->
                val currentUsage = metrics.advancedFeatureUsage[feature] ?: 0
                val now = System.currentTimeMillis()
                metrics.copy(
                    advancedFeatureUsage = metrics.advancedFeatureUsage + (feature to currentUsage + 1),
                    featureFirstUsed = if (feature !in metrics.featureFirstUsed) metrics.featureFirstUsed + (feature to now) else metrics.featureFirstUsed
                )
            }
        }
    }
    
    // MARK: - Utility Methods
    
    fun isFeatureEnabled(feature: Feature): Boolean {
        val prefs = _smartTimingPreferences.value
        val availableFeatures = featureGradualizer.getAvailableFeatures(_userEngagementLevel.value)
        
        return when (feature) {
            Feature.BASIC_TRACKING -> true // Always available
            Feature.SIMPLE_TIMER -> prefs.enableTimers && availableFeatures.contains(feature)
            Feature.SMART_SUGGESTIONS -> prefs.enableSmartSuggestions && availableFeatures.contains(feature)
            Feature.CONTEXT_AWARENESS -> prefs.enableContextAwareness && availableFeatures.contains(feature)
            Feature.HABIT_STACKING -> prefs.enableHabitStacking && availableFeatures.contains(feature)
            else -> availableFeatures.contains(feature)
        }
    }
    
    fun getFeatureUsageCount(feature: Feature): Int {
        return _userBehaviorMetrics.value.advancedFeatureUsage[feature] ?: 0
    }
    
    fun shouldShowFeatureIntro(feature: Feature): Boolean {
        val isNewlyUnlocked = availableFeatures.value.contains(feature)
        val hasBeenUsed = getFeatureUsageCount(feature) > 0
        val seen = _userBehaviorMetrics.value.featureFirstSeen.containsKey(feature)
        return _smartTimingPreferences.value.showLevelUpPrompts && isNewlyUnlocked && !hasBeenUsed && !seen
    }

    fun markFeatureSeen(feature: Feature) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            updateBehaviorMetrics { m ->
                if (feature in m.featureFirstSeen) m else m.copy(
                    featureFirstSeen = m.featureFirstSeen + (feature to now)
                )
            }
        }
    }
}
