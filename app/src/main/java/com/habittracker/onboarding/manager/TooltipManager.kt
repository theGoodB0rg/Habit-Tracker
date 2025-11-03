package com.habittracker.onboarding.manager

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.onboarding.OnboardingPreferences
import com.habittracker.onboarding.model.AppTooltips
import com.habittracker.onboarding.model.TooltipConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Global tooltip manager with race condition protection and professional state management
 * Manages tooltip flow and user progress through the guided experience
 * 
 * @author Google-level Developer
 */
@HiltViewModel
class TooltipManager @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {
    
    // Mutex for thread-safe operations
    private val tooltipMutex = Mutex()
    
    private val _currentTooltipIndex = MutableStateFlow(-1)
    val currentTooltipIndex: StateFlow<Int> = _currentTooltipIndex.asStateFlow()
    
    private val _isTooltipActive = MutableStateFlow(false)
    val isTooltipActive: StateFlow<Boolean> = _isTooltipActive.asStateFlow()
    
    private val _currentTooltip = MutableStateFlow<TooltipConfig?>(null)
    val currentTooltip: StateFlow<TooltipConfig?> = _currentTooltip.asStateFlow()
    
    private val _errorEvent = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()
    
    private val tooltips = AppTooltips.getAllTooltips()
    
    // Debouncing for rapid interactions
    private var lastInteractionTime = 0L
    private val interactionDebounceMs = 500L
    
    /**
     * Starts the guided tour if the user hasn't seen it (with safety checks)
     */
    fun startGuidedTour() {
        viewModelScope.launch {
            // Quick exits
            if (_isTooltipActive.value || tooltips.isEmpty()) return@launch
            if (!(onboardingPreferences.isOnboardingCompleted() && !hasCompletedGuidedTour())) return@launch

            // Wait until first tooltip anchor coordinates are registered to avoid blank scrim
            val firstKey = tooltips.first().targetComposableKey
            var attempts = 0
            while (com.habittracker.onboarding.components.TooltipCoordinateManager.getTarget(firstKey) == null && attempts < 12) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
            if (com.habittracker.onboarding.components.TooltipCoordinateManager.getTarget(firstKey) == null) {
                handleError("Aborting guided tour: missing anchor '$firstKey'", Exception("Missing anchor"))
                return@launch
            }

            tooltipMutex.withLock {
                try {
                    if (_isTooltipActive.value) return@withLock
                    _currentTooltipIndex.value = 0
                    _isTooltipActive.value = true
                    updateCurrentTooltip()
                    println("TooltipManager: Guided tour started with ${tooltips.size} tooltips (anchor ready)")
                    // Fallback safety: if after delay no tooltip, cleanup to remove dim overlay
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(600)
                        if (_isTooltipActive.value && _currentTooltip.value == null) {
                            println("TooltipManager: Fallback cleanup (no tooltip content loaded)")
                            forceCleanup()
                        }
                    }
                } catch (e: Exception) {
                    handleError("Failed to start guided tour", e)
                }
            }
        }
    }
    
    /**
     * Shows a specific tooltip by ID (with debouncing and safety)
     */
    fun showTooltip(tooltipId: String) {
        viewModelScope.launch {
            tooltipMutex.withLock {
                try {
                    // Debounce rapid calls
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastInteractionTime < interactionDebounceMs) {
                        return@withLock
                    }
                    lastInteractionTime = currentTime
                    
                    // Safety checks
                    if (_isTooltipActive.value || tooltipId.isBlank()) {
                        return@withLock
                    }
                    
                    if (!onboardingPreferences.isTooltipShown(tooltipId)) {
                        val tooltip = tooltips.find { it.id == tooltipId }
                        if (tooltip != null) {
                            _currentTooltip.value = tooltip
                            _isTooltipActive.value = true
                            _currentTooltipIndex.value = -1 // Single tooltip mode
                        } else {
                            handleError("Tooltip not found: $tooltipId", IllegalArgumentException("Missing tooltip"))
                        }
                    }
                } catch (e: Exception) {
                    handleError("Failed to show tooltip: $tooltipId", e)
                }
            }
        }
    }
    
    /**
     * Proceeds to the next tooltip in the guided tour (thread-safe)
     */
    fun nextTooltip() {
        viewModelScope.launch {
            tooltipMutex.withLock {
                try {
                    val current = _currentTooltipIndex.value
                    if (current in 0 until (tooltips.size - 1)) {
                        // Mark current tooltip as shown
                        markCurrentTooltipAsShown()
                        
                        _currentTooltipIndex.value = current + 1
                        updateCurrentTooltip()
                    } else {
                        // Tour complete
                        completeGuidedTour()
                    }
                } catch (e: Exception) {
                    handleError("Failed to proceed to next tooltip", e)
                }
            }
        }
    }
    
    /**
     * Dismisses the current tooltip safely
     */
    fun dismissCurrentTooltip() {
        viewModelScope.launch {
            tooltipMutex.withLock {
                try {
                    markCurrentTooltipAsShown()
                    
                    // If we're in guided tour mode, continue to next
                    if (_currentTooltipIndex.value in 0 until tooltips.size) {
                        nextTooltip()
                    } else {
                        // Single tooltip dismissal
                        _isTooltipActive.value = false
                        _currentTooltip.value = null
                    }
                } catch (e: Exception) {
                    handleError("Failed to dismiss tooltip", e)
                    // Force cleanup on error
                    forceCleanup()
                }
            }
        }
    }
    
    /**
     * Skips the entire guided tour safely
     */
    fun skipGuidedTour() {
        viewModelScope.launch {
            tooltipMutex.withLock {
                try {
                    // Mark all tooltips as shown
                    tooltips.forEach { tooltip ->
                        onboardingPreferences.setTooltipShown(tooltip.id)
                    }
                    
                    setGuidedTourCompleted()
                    forceCleanup()
                } catch (e: Exception) {
                    handleError("Failed to skip guided tour", e)
                    forceCleanup()
                }
            }
        }
    }
    
    /**
     * Completes the guided tour safely
     */
    private fun completeGuidedTour() {
        try {
            markCurrentTooltipAsShown()
            setGuidedTourCompleted()
            forceCleanup()
        } catch (e: Exception) {
            handleError("Failed to complete guided tour", e)
            forceCleanup()
        }
    }
    
    /**
     * Force cleanup of tooltip state (used in error scenarios)
     */
    private fun forceCleanup() {
        _isTooltipActive.value = false
        _currentTooltipIndex.value = -1
        _currentTooltip.value = null
    }
    
    private fun updateCurrentTooltip() {
    val safeIndex = _currentTooltipIndex.value
        val index = _currentTooltipIndex.value
        // Extra safety: only provide a tooltip if index is within bounds
        _currentTooltip.value = if (index in 0 until tooltips.size) {
            tooltips[index]
        } else {
            null
        }
    }
    
    private fun markCurrentTooltipAsShown() {
        _currentTooltip.value?.let { tooltip ->
            onboardingPreferences.setTooltipShown(tooltip.id)
        }
    }
    
    private fun hasCompletedGuidedTour(): Boolean {
        return onboardingPreferences.isTooltipShown("guided_tour_completed")
    }
    
    private fun setGuidedTourCompleted() {
        onboardingPreferences.setTooltipShown("guided_tour_completed")
    }
    
    private fun handleError(message: String, exception: Exception) {
        _errorEvent.tryEmit(message)
        // In production, log to crash reporting service
        println("TooltipManager Error: $message - ${exception.message}")
    }
    
    /**
     * Checks if a specific tooltip should be shown
     */
    fun shouldShowTooltip(tooltipId: String): Boolean {
        return try {
            !onboardingPreferences.isTooltipShown(tooltipId)
        } catch (e: Exception) {
            handleError("Failed to check tooltip status: $tooltipId", e)
            false
        }
    }
    
    /**
     * Resets all tooltip progress (for testing or settings reset)
     */
    fun resetTooltips() {
        viewModelScope.launch {
            tooltipMutex.withLock {
                try {
                    onboardingPreferences.resetTooltips()
                    forceCleanup()
                } catch (e: Exception) {
                    handleError("Failed to reset tooltips", e)
                }
            }
        }
    }
    
    /**
     * Gets the current progress in the guided tour (0.0 to 1.0)
     */
    fun getGuidedTourProgress(): Float {
        return try {
            val current = _currentTooltipIndex.value
            if (current in tooltips.indices && tooltips.isNotEmpty()) (current + 1).toFloat() / tooltips.size.toFloat() else 0f
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Safe cleanup when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        forceCleanup()
    }
}

/**
 * Composable function to integrate tooltip manager into UI safely
 */
@Composable
fun rememberTooltipManager(): TooltipManager {
    return androidx.hilt.navigation.compose.hiltViewModel()
}

/**
 * Composable function to handle tooltip display with error protection
 */
@Composable
fun TooltipDisplay(
    tooltipManager: TooltipManager = rememberTooltipManager()
) {
    val isActive by tooltipManager.isTooltipActive.collectAsState()
    val currentTooltip by tooltipManager.currentTooltip.collectAsState()
    val errorEvent by tooltipManager.errorEvent.collectAsState(initial = "")
    
    // Handle error events with proper null safety
    LaunchedEffect(errorEvent) {
        errorEvent.takeIf { it.isNotEmpty() }?.let { error ->
            // In a real app, show snackbar or error dialog
            android.util.Log.w("TooltipManager", "Tooltip Error: $error")
        }
    }

    LaunchedEffect(currentTooltip) {
        currentTooltip?.let { tooltip ->
            runCatching {
                com.habittracker.onboarding.components.TooltipCoordinateManager.bringTargetIntoView(tooltip.targetComposableKey)
            }
        }
    }
    
    // Show tooltip with safety checks
    if (isActive && currentTooltip != null) {
        com.habittracker.onboarding.components.TooltipOverlay(
            tooltipConfig = currentTooltip!!,
            isVisible = true,
            onDismiss = { 
                try {
                    tooltipManager.dismissCurrentTooltip()
                } catch (e: Exception) {
                    println("Error dismissing tooltip: ${e.message}")
                }
            }
        )
    }
}

