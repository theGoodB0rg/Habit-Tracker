package com.habittracker.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.onboarding.OnboardingPreferences
import com.habittracker.onboarding.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * ViewModel for managing onboarding flow state and business logic
 * Features race condition protection and professional state management
 * 
 * @author Google-level Developer
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {
    
    private val slides = OnboardingSlides.getAllSlides()
    
    // Mutex for thread-safe operations
    private val stateMutex = Mutex()
    private val navigationMutex = Mutex()
    
    private val _uiState = MutableStateFlow(
        OnboardingState(
            currentSlide = 0,
            totalSlides = slides.size,
            slides = slides,
            isLoading = false,
            canProceed = true
        )
    )
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()
    
    private val _navigationEvent = MutableSharedFlow<OnboardingNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val navigationEvent: SharedFlow<OnboardingNavigationEvent> = _navigationEvent.asSharedFlow()
    
    private val _errorEvent = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()
    
    // Debounce rapid events
    private var lastEventTime = 0L
    private val eventDebounceMs = 300L
    
    /**
     * Handles onboarding events from the UI with race condition protection
     */
    fun onEvent(event: OnboardingEvent) {
        viewModelScope.launch {
            stateMutex.withLock {
                try {
                    // Debounce rapid events
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastEventTime < eventDebounceMs) {
                        return@withLock
                    }
                    lastEventTime = currentTime
                    
                    // Check if we can proceed
                    if (!_uiState.value.canProceed || _uiState.value.isLoading) {
                        return@withLock
                    }
                    
                    when (event) {
                        is OnboardingEvent.NextSlide -> handleNextSlide()
                        is OnboardingEvent.PreviousSlide -> handlePreviousSlide()
                        is OnboardingEvent.SkipOnboarding -> handleSkipOnboarding()
                        is OnboardingEvent.CompleteOnboarding -> handleCompleteOnboarding()
                        is OnboardingEvent.GoToSlide -> handleGoToSlide(event.index)
                    }
                } catch (e: Exception) {
                    handleError("Navigation error occurred", e)
                }
            }
        }
    }
    
    private suspend fun handleNextSlide() {
        val currentState = _uiState.value
        if (currentState.currentSlide < currentState.totalSlides - 1) {
            updateSlide(currentState.currentSlide + 1)
        } else {
            // Last slide reached - complete onboarding
            handleCompleteOnboarding()
        }
    }
    
    private suspend fun handlePreviousSlide() {
        val currentState = _uiState.value
        if (currentState.currentSlide > 0) {
            updateSlide(currentState.currentSlide - 1)
        }
    }
    
    private suspend fun handleGoToSlide(index: Int) {
        val currentState = _uiState.value
        if (index in 0 until currentState.totalSlides && index != currentState.currentSlide) {
            updateSlide(index)
        }
    }
    
    private suspend fun updateSlide(newSlideIndex: Int) {
        _uiState.value = _uiState.value.copy(
            currentSlide = newSlideIndex,
            canProceed = true
        )
    }
    
    private suspend fun handleSkipOnboarding() {
        navigationMutex.withLock {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    canProceed = false
                )
                
                onboardingPreferences.setOnboardingSkipped()
                onboardingPreferences.setFirstLaunchCompleted()
                
                _navigationEvent.tryEmit(OnboardingNavigationEvent.NavigateToMain)
            } catch (e: Exception) {
                handleError("Failed to skip onboarding", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    canProceed = true
                )
            }
        }
    }
    
    private suspend fun handleCompleteOnboarding() {
        navigationMutex.withLock {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    canProceed = false
                )
                
                onboardingPreferences.setOnboardingCompleted()
                onboardingPreferences.setFirstLaunchCompleted()
                
                _navigationEvent.tryEmit(OnboardingNavigationEvent.NavigateToMain)
            } catch (e: Exception) {
                handleError("Failed to complete onboarding", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    canProceed = true
                )
            }
        }
    }
    
    private suspend fun handleError(message: String, exception: Exception) {
        _errorEvent.tryEmit(message)
        // Log error in production app
        println("OnboardingViewModel Error: $message - ${exception.message}")
    }
    
    /**
     * Gets the current slide data safely
     */
    fun getCurrentSlide(): OnboardingSlide? {
        val currentState = _uiState.value
        return currentState.slides.getOrNull(currentState.currentSlide)
    }
    
    /**
     * Checks if this is the first slide
     */
    fun isFirstSlide(): Boolean {
        return _uiState.value.currentSlide == 0
    }
    
    /**
     * Checks if this is the last slide
     */
    fun isLastSlide(): Boolean {
        val currentState = _uiState.value
        return currentState.currentSlide == currentState.totalSlides - 1
    }
    
    /**
     * Gets the progress percentage (0.0 to 1.0)
     */
    fun getProgress(): Float {
        val currentState = _uiState.value
        return if (currentState.totalSlides > 0) {
            (currentState.currentSlide + 1).toFloat() / currentState.totalSlides.toFloat()
        } else 0f
    }
    
    /**
     * Safely updates loading state
     */
    fun setLoadingState(isLoading: Boolean) {
        viewModelScope.launch {
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(
                    isLoading = isLoading,
                    canProceed = !isLoading
                )
            }
        }
    }
    
    /**
     * Clears any error states
     */
    fun clearErrors() {
        // Errors are handled via SharedFlow and auto-clear
    }
}

/**
 * Navigation events from onboarding
 */
sealed class OnboardingNavigationEvent {
    object NavigateToMain : OnboardingNavigationEvent()
}
