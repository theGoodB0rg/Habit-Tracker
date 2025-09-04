package com.habittracker.ui.viewmodels.timing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.data.preferences.TimingPreferences
import com.habittracker.data.preferences.TimingPreferencesRepository
import com.habittracker.timing.AlertType
import com.habittracker.R
import android.app.Application
import com.habittracker.timing.alert.AlertChannels
import com.habittracker.timing.alert.AlertEngine
import com.habittracker.timing.alert.soundPackById
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimingAudioSettingsViewModel @Inject constructor(
    private val repo: TimingPreferencesRepository,
    private val alertEngine: AlertEngine,
    private val app: Application
) : ViewModel() {

    val prefs: StateFlow<TimingPreferences> = repo.preferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimingPreferences())

    fun toggleGlobalAudio(enabled: Boolean) = viewModelScope.launch { repo.setEnableGlobalAudioCues(enabled) }
    fun toggleProgress(enabled: Boolean) = viewModelScope.launch { repo.setEnableProgressCues(enabled) }
    fun toggleHaptics(enabled: Boolean) = viewModelScope.launch { repo.setEnableHaptics(enabled) }
    fun toggleTts(enabled: Boolean) = viewModelScope.launch { repo.setEnableTts(enabled) }
    fun setSoundPack(id: String) = viewModelScope.launch { repo.setSelectedSoundPack(id) }
    fun setSoundMasterVolume(percent: Int) = viewModelScope.launch { repo.setSoundMasterVolume(percent.coerceIn(0,100)) }
    fun setToneVariation(enabled: Boolean) = viewModelScope.launch { repo.setEnableToneVariation(enabled) }
    fun toggleHeadsUpFinal(enabled: Boolean) = viewModelScope.launch { repo.setEnableHeadsUpFinal(enabled) }
    fun toggleReducedMotion(enabled: Boolean) = viewModelScope.launch { repo.setReducedMotion(enabled) }

    fun playPreview(type: AlertType) {
        val p = prefs.value
        val pack = soundPackById(p.selectedSoundPackId)
        val resId = if (p.enableGlobalAudioCues) pack.resIdFor(type) else null
        if (resId == null && !p.enableHaptics && !p.enableTts) return
        val spoken = if (p.enableTts) when (type) {
            AlertType.FINAL -> app.getString(R.string.tts_timer_complete)
            AlertType.MIDPOINT -> app.getString(R.string.tts_timer_halfway)
            AlertType.START -> app.getString(R.string.tts_timer_started)
            else -> null
        } else null
        val vol = p.soundMasterVolumePercent / 100f
        val pitch = if (p.enableToneVariation) when (type) {
            AlertType.START -> 1.06f
            AlertType.MIDPOINT -> 1.0f
            AlertType.PROGRESS -> 1.03f
            AlertType.FINAL -> 0.94f
        } else 1f
        alertEngine.playAlert(type, AlertChannels(
            sound = resId != null,
            haptics = p.enableHaptics && !p.reducedMotion,
            spokenText = spoken,
            rawResId = resId,
            volume = vol,
            pitchRate = pitch
        ))
    }
}
