package com.habittracker.ui.viewmodels.timing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.data.preferences.TimingPreferencesRepository
import com.habittracker.data.profiles.AlertProfilesRepository
import com.habittracker.data.database.entity.timing.TimerAlertProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertProfilesViewModel @Inject constructor(
    private val profilesRepo: AlertProfilesRepository,
    private val prefsRepo: TimingPreferencesRepository
) : ViewModel() {

    data class UiState(
        val profiles: List<TimerAlertProfileEntity> = emptyList(),
        val selectedId: String = "focus"
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(profilesRepo.profiles(), prefsRepo.preferences()) { profiles, prefs ->
                UiState(profiles = profiles, selectedId = prefs.defaultAlertProfileId)
            }.collect { _ui.value = it }
        }
    }

    fun select(id: String) = viewModelScope.launch { prefsRepo.setDefaultAlertProfileId(id) }

    fun upsert(profile: TimerAlertProfileEntity) = viewModelScope.launch {
        profilesRepo.upsert(profile)
    }

    fun delete(id: String) = viewModelScope.launch {
        val p = profilesRepo.get(id)
        if (p?.isUserEditable == true) profilesRepo.delete(id)
    }
}
