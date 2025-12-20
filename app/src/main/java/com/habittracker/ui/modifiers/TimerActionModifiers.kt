package com.habittracker.ui.modifiers

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
