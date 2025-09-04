package com.habittracker.legal.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.legal.data.LegalRepository
import com.habittracker.legal.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing legal and about information with proper state management
 * and race condition protection
 */
@HiltViewModel
class LegalViewModel @Inject constructor(
    private val repository: LegalRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    // State for app version info
    private val _appVersionState = MutableStateFlow<UiState<AppVersionInfo>>(UiState.Loading)
    val appVersionState: StateFlow<UiState<AppVersionInfo>> = _appVersionState.asStateFlow()
    
    // State for HTML content
    private val _htmlContentState = MutableStateFlow<UiState<String>>(UiState.Loading)
    val htmlContentState: StateFlow<UiState<String>> = _htmlContentState.asStateFlow()
    
    // State for tutorial
    private val _tutorialState = MutableStateFlow<TutorialUiState>(TutorialUiState())
    val tutorialState: StateFlow<TutorialUiState> = _tutorialState.asStateFlow()
    
    // State for tips
    private val _tipsState = MutableStateFlow<UiState<List<HabitTip>>>(UiState.Loading)
    val tipsState: StateFlow<UiState<List<HabitTip>>> = _tipsState.asStateFlow()
    
    // Developer info (static, so can be synchronous)
    val developerInfo: DeveloperInfo = repository.getDeveloperInfo()
    
    init {
        loadAppVersionInfo()
        loadTips()
    }
    
    /**
     * Loads app version information with error handling
     */
    fun loadAppVersionInfo() {
        viewModelScope.launch {
            _appVersionState.value = UiState.Loading
            repository.getAppVersionInfo()
                .onSuccess { versionInfo ->
                    _appVersionState.value = UiState.Success(versionInfo)
                }
                .onFailure { error ->
                    _appVersionState.value = UiState.Error(error.message ?: "Failed to load version info")
                }
        }
    }
    
    /**
     * Loads HTML content for legal pages
     */
    fun loadHtmlContent(pageType: HelpPageType) {
        viewModelScope.launch {
            _htmlContentState.value = UiState.Loading
            repository.loadHtmlContent(pageType.fileName)
                .onSuccess { content ->
                    _htmlContentState.value = UiState.Success(content)
                }
                .onFailure { error ->
                    _htmlContentState.value = UiState.Error(error.message ?: "Failed to load content")
                }
        }
    }
    
    /**
     * Initializes tutorial with steps
     */
    fun initializeTutorial() {
        val steps = repository.getTutorialSteps()
        _tutorialState.value = TutorialUiState(
            steps = steps,
            currentStepIndex = 0,
            isVisible = true,
            isCompleted = false
        )
    }
    
    /**
     * Navigates to next tutorial step
     */
    fun nextTutorialStep() {
        val currentState = _tutorialState.value
        if (currentState.currentStepIndex < currentState.steps.size - 1) {
            _tutorialState.value = currentState.copy(
                currentStepIndex = currentState.currentStepIndex + 1
            )
        } else {
            completeTutorial()
        }
    }
    
    /**
     * Navigates to previous tutorial step
     */
    fun previousTutorialStep() {
        val currentState = _tutorialState.value
        if (currentState.currentStepIndex > 0) {
            _tutorialState.value = currentState.copy(
                currentStepIndex = currentState.currentStepIndex - 1
            )
        }
    }
    
    /**
     * Skips tutorial
     */
    fun skipTutorial() {
        _tutorialState.value = _tutorialState.value.copy(
            isVisible = false,
            isCompleted = true,
            isSkipped = true
        )
    }
    
    /**
     * Completes tutorial
     */
    private fun completeTutorial() {
        _tutorialState.value = _tutorialState.value.copy(
            isVisible = false,
            isCompleted = true,
            isSkipped = false
        )
    }
    
    /**
     * Closes tutorial overlay
     */
    fun closeTutorial() {
        _tutorialState.value = _tutorialState.value.copy(
            isVisible = false
        )
    }
    
    /**
     * Loads tips for users
     */
    private fun loadTips() {
        viewModelScope.launch {
            _tipsState.value = UiState.Loading
            try {
                val tips = repository.getHabitTips()
                _tipsState.value = UiState.Success(tips)
            } catch (e: Exception) {
                _tipsState.value = UiState.Error(e.message ?: "Failed to load tips")
            }
        }
    }
    
    /**
     * Filters tips by category
     */
    fun filterTipsByCategory(category: TipCategory?): StateFlow<UiState<List<HabitTip>>> {
        return _tipsState.map { state ->
            when (state) {
                is UiState.Success -> {
                    val filteredTips = if (category != null) {
                        state.data.filter { it.category == category }
                    } else {
                        state.data
                    }
                    UiState.Success(filteredTips)
                }
                else -> state
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )
    }
    
    /**
     * Opens email app for feedback with pre-filled information
     */
    fun sendFeedback(subject: String = "Habit Tracker Feedback", userMessage: String = "") {
        viewModelScope.launch {
            repository.generateFeedbackInfo(subject, userMessage)
                .onSuccess { feedbackInfo ->
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(developerInfo.email))
                        putExtra(Intent.EXTRA_SUBJECT, feedbackInfo.subject)
                        putExtra(Intent.EXTRA_TEXT, feedbackInfo.body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    try {
                        context.startActivity(emailIntent)
                    } catch (e: Exception) {
                        // Handle case where no email app is available
                        // Could show a toast or dialog with the email address to copy
                    }
                }
                .onFailure {
                    // Handle error in generating feedback info
                }
        }
    }
    
    /**
     * Opens Play Store for app rating
     */
    fun rateApp() {
        try {
            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        } catch (e: Exception) {
            // Fallback to browser if Play Store is not available
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                // Handle case where neither Play Store nor browser is available
            }
        }
    }
    
    /**
     * Opens external URL in browser
     */
    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where no browser is available
        }
    }
}

/**
 * UI state wrapper for better state management
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/**
 * State for tutorial UI
 */
data class TutorialUiState(
    val steps: List<TutorialStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val isVisible: Boolean = false,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false
) {
    val currentStep: TutorialStep?
        get() = if (currentStepIndex < steps.size) steps[currentStepIndex] else null
    
    val isFirstStep: Boolean
        get() = currentStepIndex == 0
    
    val isLastStep: Boolean
        get() = currentStepIndex == steps.size - 1
    
    val progressPercentage: Float
        get() = if (steps.isEmpty()) 0f else (currentStepIndex + 1).toFloat() / steps.size.toFloat()
}
