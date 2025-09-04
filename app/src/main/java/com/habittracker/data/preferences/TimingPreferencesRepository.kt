package com.habittracker.data.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.habittracker.ui.models.timing.ReminderStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Timing feature user preferences persisted via DataStore (Preferences)
 */
data class TimingPreferences(
    val reminderStyle: ReminderStyle = ReminderStyle.GENTLE,
    val timerDefaultDuration: Duration = Duration.ofMinutes(25),
    val isSchedulingEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val respectDoNotDisturb: Boolean = true,
    // Smart Timing feature toggles persisted for Slice 4
    val enableTimers: Boolean = false,
    val enableSmartSuggestions: Boolean = false,
    val enableContextAwareness: Boolean = false,
    val enableHabitStacking: Boolean = false,
    val autoLevelUp: Boolean = true,
    val showLevelUpPrompts: Boolean = true,
    // Phase UIX-1 global timer foundations
    val defaultAlertProfileId: String = "focus",
    val enableGlobalAudioCues: Boolean = true,
    val reducedMotion: Boolean = false
    ,
    // Phase UIX-3 alert delivery channels
    val enableHaptics: Boolean = true,
    val enableTts: Boolean = false,
    // Phase UIX-3 hybrid sound strategy
    val selectedSoundPackId: String = "default",
    val enableProgressCues: Boolean = true
    ,
    val soundMasterVolumePercent: Int = 100, // 0..100 scaling for all alert sounds
    val enableToneVariation: Boolean = false,
    // Phase UIX-7 heads-up final alert (last 10s)
    val enableHeadsUpFinal: Boolean = false
)

interface TimingPreferencesRepository {
    fun preferences(): Flow<TimingPreferences>
    suspend fun setReminderStyle(style: ReminderStyle)
    suspend fun setTimerDefaultDuration(duration: Duration)
    suspend fun setSchedulingEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setRespectDoNotDisturb(enabled: Boolean)
    // Smart Timing feature toggles
    suspend fun setEnableTimers(enabled: Boolean)
    suspend fun setEnableSmartSuggestions(enabled: Boolean)
    suspend fun setEnableContextAwareness(enabled: Boolean)
    suspend fun setEnableHabitStacking(enabled: Boolean)
    suspend fun setAutoLevelUp(enabled: Boolean)
    suspend fun setShowLevelUpPrompts(enabled: Boolean)
    // Phase UIX-1 setters
    suspend fun setDefaultAlertProfileId(id: String)
    suspend fun setEnableGlobalAudioCues(enabled: Boolean)
    suspend fun setReducedMotion(enabled: Boolean)
    suspend fun setEnableHaptics(enabled: Boolean)
    suspend fun setEnableTts(enabled: Boolean)
    suspend fun setSelectedSoundPack(id: String)
    suspend fun setEnableProgressCues(enabled: Boolean)
    suspend fun setSoundMasterVolume(percent: Int)
    suspend fun setEnableToneVariation(enabled: Boolean)
    suspend fun setEnableHeadsUpFinal(enabled: Boolean)
}

@Singleton
class TimingPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : TimingPreferencesRepository {

    private object Keys {
        val REMINDER_STYLE = stringPreferencesKey("timing.reminder_style")
        val TIMER_DEFAULT_MIN = intPreferencesKey("timing.timer_default_min")
        val SCHEDULING_ENABLED = booleanPreferencesKey("timing.scheduling_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("timing.notifications_enabled")
        val RESPECT_DND = booleanPreferencesKey("timing.respect_dnd")
    // Smart Timing flags
    val ENABLE_TIMERS = booleanPreferencesKey("timing.enable_timers")
    val ENABLE_SMART_SUGGESTIONS = booleanPreferencesKey("timing.enable_smart_suggestions")
    val ENABLE_CONTEXT_AWARENESS = booleanPreferencesKey("timing.enable_context_awareness")
    val ENABLE_HABIT_STACKING = booleanPreferencesKey("timing.enable_habit_stacking")
    val AUTO_LEVEL_UP = booleanPreferencesKey("timing.auto_level_up")
    val SHOW_LEVEL_UP_PROMPTS = booleanPreferencesKey("timing.show_level_up_prompts")
    // Phase UIX-1 new global keys
    val DEFAULT_ALERT_PROFILE_ID = stringPreferencesKey("timing.default_alert_profile_id")
    val ENABLE_GLOBAL_AUDIO_CUES = booleanPreferencesKey("timing.enable_global_audio_cues")
    val REDUCED_MOTION = booleanPreferencesKey("timing.reduced_motion")
    val ENABLE_HAPTICS = booleanPreferencesKey("timing.enable_haptics")
    val ENABLE_TTS = booleanPreferencesKey("timing.enable_tts")
    val SELECTED_SOUND_PACK_ID = stringPreferencesKey("timing.selected_sound_pack_id")
    val ENABLE_PROGRESS_CUES = booleanPreferencesKey("timing.enable_progress_cues")
    val SOUND_MASTER_VOLUME_PERCENT = intPreferencesKey("timing.sound_master_volume_percent")
    val ENABLE_TONE_VARIATION = booleanPreferencesKey("timing.enable_tone_variation")
    val ENABLE_HEADS_UP_FINAL = booleanPreferencesKey("timing.enable_heads_up_final")
    }

    override fun preferences(): Flow<TimingPreferences> =
        dataStore.data
            .catch { e ->
                Log.w("TimingPrefsRepo", "DataStore read failed", e)
                emit(emptyPreferences())
            }
            .map { prefs ->
                val style = prefs[Keys.REMINDER_STYLE]?.let { runCatching { ReminderStyle.valueOf(it) }.getOrNull() }
                    ?: ReminderStyle.GENTLE
                val durationMin = prefs[Keys.TIMER_DEFAULT_MIN] ?: 25
                val scheduling = prefs[Keys.SCHEDULING_ENABLED] ?: false
                val notifications = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
                val respectDnd = prefs[Keys.RESPECT_DND] ?: true
                val enableTimers = prefs[Keys.ENABLE_TIMERS] ?: false
                val enableSmart = prefs[Keys.ENABLE_SMART_SUGGESTIONS] ?: false
                val enableContext = prefs[Keys.ENABLE_CONTEXT_AWARENESS] ?: false
                val enableStacking = prefs[Keys.ENABLE_HABIT_STACKING] ?: false
                val autoLevelUp = prefs[Keys.AUTO_LEVEL_UP] ?: true
                val showPrompts = prefs[Keys.SHOW_LEVEL_UP_PROMPTS] ?: true
                val defaultProfile = prefs[Keys.DEFAULT_ALERT_PROFILE_ID] ?: "focus"
                val audioCues = prefs[Keys.ENABLE_GLOBAL_AUDIO_CUES] ?: true
                val reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false
                val enableHaptics = prefs[Keys.ENABLE_HAPTICS] ?: true
                val enableTts = prefs[Keys.ENABLE_TTS] ?: false
                val soundPack = prefs[Keys.SELECTED_SOUND_PACK_ID] ?: "default"
                val progressCues = prefs[Keys.ENABLE_PROGRESS_CUES] ?: true
                val masterVol = (prefs[Keys.SOUND_MASTER_VOLUME_PERCENT] ?: 100).coerceIn(0,100)
                val toneVar = prefs[Keys.ENABLE_TONE_VARIATION] ?: false
                val headsUp = prefs[Keys.ENABLE_HEADS_UP_FINAL] ?: false

                TimingPreferences(
                    reminderStyle = style,
                    timerDefaultDuration = Duration.ofMinutes(durationMin.toLong()),
                    isSchedulingEnabled = scheduling,
                    notificationsEnabled = notifications,
                    respectDoNotDisturb = respectDnd,
                    enableTimers = enableTimers,
                    enableSmartSuggestions = enableSmart,
                    enableContextAwareness = enableContext,
                    enableHabitStacking = enableStacking,
                    autoLevelUp = autoLevelUp,
                    showLevelUpPrompts = showPrompts,
                    defaultAlertProfileId = defaultProfile,
                    enableGlobalAudioCues = audioCues,
                    reducedMotion = reducedMotion,
                    enableHaptics = enableHaptics,
                    enableTts = enableTts,
                    selectedSoundPackId = soundPack,
                    enableProgressCues = progressCues
                    ,
                    soundMasterVolumePercent = masterVol,
                    enableToneVariation = toneVar,
                    enableHeadsUpFinal = headsUp
                )
            }
            .distinctUntilChanged()

    override suspend fun setReminderStyle(style: ReminderStyle) {
        try {
            dataStore.edit { it[Keys.REMINDER_STYLE] = style.name }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write reminder style", e)
        }
    }

    override suspend fun setTimerDefaultDuration(duration: Duration) {
        val minutes = duration.toMinutes().coerceAtLeast(1)
        try {
            dataStore.edit { it[Keys.TIMER_DEFAULT_MIN] = minutes.toInt() }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write default duration", e)
        }
    }

    override suspend fun setSchedulingEnabled(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.SCHEDULING_ENABLED] = enabled }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write scheduling flag", e)
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write notifications flag", e)
        }
    }

    override suspend fun setRespectDoNotDisturb(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.RESPECT_DND] = enabled }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write DND flag", e)
        }
    }

    override suspend fun setEnableTimers(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.ENABLE_TIMERS] = enabled }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableTimers", e)
        }
    }

    override suspend fun setEnableSmartSuggestions(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.ENABLE_SMART_SUGGESTIONS] = enabled }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableSmartSuggestions", e)
        }
    }

    override suspend fun setEnableContextAwareness(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.ENABLE_CONTEXT_AWARENESS] = enabled }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableContextAwareness", e)
        }
    }

    override suspend fun setEnableHabitStacking(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.ENABLE_HABIT_STACKING] = enabled }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableHabitStacking", e)
        }
    }

    override suspend fun setAutoLevelUp(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.AUTO_LEVEL_UP] = enabled }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write autoLevelUp", e)
        }
    }

    override suspend fun setShowLevelUpPrompts(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.SHOW_LEVEL_UP_PROMPTS] = enabled }
        } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write showLevelUpPrompts", e)
        }
    }

    override suspend fun setDefaultAlertProfileId(id: String) {
        try { dataStore.edit { it[Keys.DEFAULT_ALERT_PROFILE_ID] = id } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write defaultAlertProfileId", e)
        }
    }

    override suspend fun setEnableGlobalAudioCues(enabled: Boolean) {
        try { dataStore.edit { it[Keys.ENABLE_GLOBAL_AUDIO_CUES] = enabled } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableGlobalAudioCues", e)
        }
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        try { dataStore.edit { it[Keys.REDUCED_MOTION] = enabled } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write reducedMotion", e)
        }
    }

    override suspend fun setEnableHaptics(enabled: Boolean) {
        try { dataStore.edit { it[Keys.ENABLE_HAPTICS] = enabled } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableHaptics", e)
        }
    }

    override suspend fun setEnableTts(enabled: Boolean) {
        try { dataStore.edit { it[Keys.ENABLE_TTS] = enabled } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableTts", e)
        }
    }
    override suspend fun setSelectedSoundPack(id: String) {
        try { dataStore.edit { it[Keys.SELECTED_SOUND_PACK_ID] = id } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write selectedSoundPackId", e)
        }
    }
    override suspend fun setEnableProgressCues(enabled: Boolean) {
        try { dataStore.edit { it[Keys.ENABLE_PROGRESS_CUES] = enabled } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableProgressCues", e)
        }
    }
    override suspend fun setSoundMasterVolume(percent: Int) {
        val clamped = percent.coerceIn(0,100)
        try { dataStore.edit { it[Keys.SOUND_MASTER_VOLUME_PERCENT] = clamped } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write soundMasterVolumePercent", e)
        }
    }
    override suspend fun setEnableToneVariation(enabled: Boolean) {
        try { dataStore.edit { it[Keys.ENABLE_TONE_VARIATION] = enabled } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableToneVariation", e)
        }
    }

    override suspend fun setEnableHeadsUpFinal(enabled: Boolean) {
        try { dataStore.edit { it[Keys.ENABLE_HEADS_UP_FINAL] = enabled } } catch (e: Exception) {
            Log.e("TimingPrefsRepo", "Failed to write enableHeadsUpFinal", e)
        }
    }
}
