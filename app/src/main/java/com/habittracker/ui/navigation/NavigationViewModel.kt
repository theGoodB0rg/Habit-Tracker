package com.habittracker.ui.navigation

import androidx.lifecycle.ViewModel
import com.habittracker.onboarding.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Helper ViewModel for navigation-related dependencies
 * 
 * @author Google-level Developer
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    val onboardingPreferences: OnboardingPreferences
) : ViewModel()
