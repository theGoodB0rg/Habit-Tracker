package com.habittracker.timing.alert

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import com.habittracker.timing.AlertType
import android.media.SoundPool
import android.media.AudioAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/** Simple contract for delivering timer alerts through multiple channels. */
interface AlertEngine {
    fun playAlert(type: AlertType, channels: AlertChannels)
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
    private val soundPool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            ).build()
    }
    private val loaded = ConcurrentHashMap<Int, Int>() // resId -> soundId
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.getDefault()
    }

    private fun ensureLoaded() { if (tts == null) tts = TextToSpeech(context, this) }

    override fun playAlert(type: AlertType, channels: AlertChannels) {
        runCatching {
            ensureLoaded()
            if (channels.sound) channels.rawResId?.let { resId ->
                val soundId = loaded[resId] ?: kotlin.run {
                    var sid = 0
                    try { sid = soundPool.load(context, resId, 1) } catch (_: Exception) {}
                    if (sid != 0) loaded[resId] = sid
                    sid
                }
                if (soundId != 0) soundPool.play(soundId, channels.volume, channels.volume,1,0, channels.pitchRate.coerceIn(0.5f,2f))
            }
            if (channels.haptics) vibrate(type)
            if (channels.spokenText != null && ttsReady) {
                tts?.speak(channels.spokenText, TextToSpeech.QUEUE_ADD, null, "alert-${type.name}")
            }
        }.onFailure { Log.w("AlertEngine", "playAlert failed", it) }
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

    override fun dispose() { runCatching { tts?.shutdown() } }
}
