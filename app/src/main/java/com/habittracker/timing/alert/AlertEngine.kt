package com.habittracker.timing.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import com.habittracker.timing.AlertType
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.habittracker.R

/** Simple contract for delivering timer alerts through multiple channels. */
interface AlertEngine {
    fun playAlert(type: AlertType, channels: AlertChannels)
    fun preload()  // Eagerly load sounds and TTS
    fun dispose()
}

data class AlertChannels(
    val sound: Boolean,
    val haptics: Boolean,
    val spokenText: String? = null,
    val rawResId: Int? = null,
    val volume: Float = 1f, // 0f..1f master scaling
    val pitchRate: Float = 1f // 0.8f..1.2f subtle differentiation
)

@Singleton
class AlertEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlertEngine, TextToSpeech.OnInitListener {
    private val soundLoadedReady = ConcurrentHashMap<Int, Boolean>() // soundId -> ready
    
    private val soundPool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            ).build().also { pool ->
                // Set up load complete listener to track when sounds are ready
                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        soundLoadedReady[sampleId] = true
                        Log.d("AlertEngine", "Sound ready: sampleId=$sampleId")
                    } else {
                        Log.w("AlertEngine", "Sound load failed: sampleId=$sampleId status=$status")
                    }
                }
            }
    }
    private val loaded = ConcurrentHashMap<Int, Int>() // resId -> soundId
    private val audioManager: AudioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var focusRequest: AudioFocusRequest? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var preloaded = false

    private val ttsVolumeFloor = 0.2f // prevent effectively silent utterances

    init {
        // Eagerly initialize TTS and preload sounds on creation
        preload()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language = Locale.getDefault()
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            Log.d("AlertEngine", "TTS initialized successfully")
        } else {
            Log.w("AlertEngine", "TTS initialization failed with status: $status")
        }
    }

    /**
     * Eagerly preload TTS and common sound resources so they're ready on first use
     */
    override fun preload() {
        if (preloaded) return
        preloaded = true
        
        // Initialize TTS eagerly
        if (tts == null) {
            tts = TextToSpeech(context, this)
        }
        
        // Preload common timer sounds
        val soundResIds = listOf(
            R.raw.timer_start,
            R.raw.timer_mid,
            R.raw.timer_progress_soft,
            R.raw.timer_final
        )
        
        soundResIds.forEach { resId ->
            try {
                if (!loaded.containsKey(resId)) {
                    val sid = soundPool.load(context, resId, 1)
                    if (sid != 0) {
                        loaded[resId] = sid
                        Log.d("AlertEngine", "Preloading sound resId=$resId -> soundId=$sid")
                    }
                }
            } catch (e: Exception) {
                Log.w("AlertEngine", "Failed to preload sound resId=$resId", e)
            }
        }
    }

    override fun playAlert(type: AlertType, channels: AlertChannels) {
        runCatching {
            // Ensure preloaded (should already be done in init, but just in case)
            preload()
            
            if (channels.sound) channels.rawResId?.let { resId ->
                val soundId = loaded[resId] ?: kotlin.run {
                    var sid = 0
                    try { sid = soundPool.load(context, resId, 1) } catch (_: Exception) {}
                    if (sid != 0) loaded[resId] = sid
                    sid
                }
                if (soundId != 0) {
                    // Check if sound is ready
                    val isReady = soundLoadedReady[soundId] == true
                    if (isReady) {
                        soundPool.play(soundId, channels.volume, channels.volume, 1, 0, channels.pitchRate.coerceIn(0.5f, 2f))
                    } else {
                        // Sound not ready yet, play with a slight delay
                        Log.d("AlertEngine", "Sound $soundId not ready, scheduling delayed play")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            soundPool.play(soundId, channels.volume, channels.volume, 1, 0, channels.pitchRate.coerceIn(0.5f, 2f))
                        }, 100)
                    }
                }
            }
            if (channels.haptics) vibrate(type)
            val spoken = channels.spokenText
            if (spoken != null) {
                if (!ttsReady) {
                    Log.w("AlertEngine", "TTS not ready; skipping spoken alert for ${type.name}")
                } else {
                    val vol = channels.volume.coerceIn(ttsVolumeFloor, 1f)
                    val focusGained = requestAudioFocus()
                    val params = Bundle().apply {
                        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, vol)
                        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                    }
                    val utteranceId = "alert-${type.name}-${System.currentTimeMillis()}"
                    Log.d(
                        "AlertEngine",
                        "Speaking alert type=${type.name} vol=$vol focusGained=$focusGained text='${spoken.take(64)}'"
                    )
                    tts?.speak(spoken, TextToSpeech.QUEUE_ADD, params, utteranceId)
                    if (focusGained) abandonAudioFocus()
                }
            }
        }.onFailure { Log.w("AlertEngine", "playAlert failed", it) }
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { /* no-op */ }
                    .build()
                focusRequest = req
                audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w("AlertEngine", "Audio focus request failed", e)
            false
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION") audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w("AlertEngine", "Audio focus abandon failed", e)
        }
    }

    private fun vibrate(type: AlertType) {
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = when (type) {
            AlertType.START -> longArrayOf(0, 40)
            AlertType.MIDPOINT -> longArrayOf(0, 30, 60, 30)
            AlertType.FINAL -> longArrayOf(0, 60, 80, 60, 80, 120)
            AlertType.PROGRESS -> longArrayOf(0, 20)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION") vib.vibrate(pattern, -1)
        }
    }

    override fun dispose() { 
        runCatching { 
            tts?.shutdown() 
            tts = null
            ttsReady = false
            preloaded = false
        } 
    }
}
