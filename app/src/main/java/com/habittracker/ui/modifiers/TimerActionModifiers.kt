package com.habittracker.ui.modifiers

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.habittracker.timerux.TimerActionCoordinator

/**
 * Modifier that visually indicates when a timer action is being processed.
 * Reduces opacity and can optionally pulse to indicate activity.
 */
fun Modifier.disableDuringTimerAction(state: TimerActionCoordinator.CoordinatorState): Modifier {
    return if (state.waitingForService) {
        this.alpha(0.5f)
    } else {
        this
    }
}

/**
 * Composable wrapper that shows a loading indicator overlay when timer action is in progress.
 * Use this to wrap buttons/chips for clearer feedback.
 */
@Composable
fun TimerActionLoadingWrapper(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Apply alpha to content when loading
        Box(modifier = Modifier.alpha(if (isLoading) 0.4f else 1f)) {
            content()
        }
        // Show loading indicator overlay
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Animated pulsing modifier for active timer states.
 * Provides subtle visual feedback that an action is processing.
 */
@Composable
fun Modifier.pulseWhenProcessing(isProcessing: Boolean): Modifier {
    return if (isProcessing) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600)
            ),
            label = "pulseAlpha"
        )
        this.alpha(alpha)
    } else {
        this
    }
}

/**
 * Enum representing different types of haptic feedback for timer actions.
 */
enum class TimerHapticType {
    /** Light tap for button press acknowledgment */
    CLICK,
    /** Confirmation feel for successful action */
    SUCCESS,
    /** Alert pattern for failed/blocked action */
    ERROR,
    /** Subtle tick for timer tick events */
    TICK,
    /** Strong confirmation for habit completion */
    COMPLETION
}

/**
 * Controller for triggering haptic feedback on timer actions.
 * Respects system settings and provides consistent haptic patterns.
 */
class TimerHapticController(
    private val context: Context,
    private val view: View?,
    private val hapticFeedback: HapticFeedback?
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    /**
     * Triggers haptic feedback based on the specified type.
     */
    fun trigger(type: TimerHapticType) {
        when (type) {
            TimerHapticType.CLICK -> triggerClick()
            TimerHapticType.SUCCESS -> triggerSuccess()
            TimerHapticType.ERROR -> triggerError()
            TimerHapticType.TICK -> triggerTick()
            TimerHapticType.COMPLETION -> triggerCompletion()
        }
    }
    
    private fun triggerClick() {
        // Use Compose haptic feedback for standard clicks
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            ?: view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
    
    private fun triggerSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            // Fallback: short gentle vibration
            vibratePattern(longArrayOf(0, 30, 50, 30))
        }
    }
    
    private fun triggerError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            // Fallback: double buzz for error
            vibratePattern(longArrayOf(0, 50, 80, 50))
        }
    }
    
    private fun triggerTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            vibratePattern(longArrayOf(0, 10))
        }
    }
    
    private fun triggerCompletion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            // Double confirm for emphasis
            view?.postDelayed({
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }, 100)
        } else {
            // Fallback: celebratory pattern
            vibratePattern(longArrayOf(0, 40, 60, 40, 60, 80))
        }
    }
    
    private fun vibratePattern(pattern: LongArray) {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, -1)
            }
        }
    }
}

/**
 * Remember a TimerHapticController for use in composables.
 */
@Composable
fun rememberTimerHaptics(): TimerHapticController {
    val context = LocalContext.current
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    return remember(context, view, hapticFeedback) {
        TimerHapticController(context, view, hapticFeedback)
    }
}
