package com.habittracker.ui.components.timer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import com.habittracker.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import kotlin.math.roundToInt

/**
 * Radial timer showing remaining time with an animated sweep.
 * Keeps logic simple: caller passes totalMillis & remainingMillis.
 */
@Composable
fun RadialTimer(
    totalMillis: Long,
    remainingMillis: Long,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    reducedMotion: Boolean = false,
) {
    val percent by remember(totalMillis, remainingMillis) {
        derivedStateOf { if (totalMillis <= 0) 0f else (1f - (remainingMillis.coerceAtLeast(0) / totalMillis.toFloat())) }
    }
    val clamped = percent.coerceIn(0f,1f)
    val timeStr by remember(remainingMillis) {
        derivedStateOf {
            val totalSeconds = (remainingMillis / 1000)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
    }
    val animatedSweep by animateFloatAsState(
        targetValue = 360f * clamped,
        animationSpec = tween(durationMillis = if (isPaused || reducedMotion) 0 else 550),
        label = "sweepAnim"
    )
    val cd by remember(timeStr, clamped, isPaused) {
        derivedStateOf {
            "$timeStr remaining (${(clamped*100).roundToInt()}%)" + if (isPaused) " paused" else ""
        }
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(percent, 0f..1f)
                contentDescription = cd
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val diameter = size.minDimension - stroke.width
            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset((size.width - diameter)/2, (size.height - diameter)/2),
                size = Size(diameter, diameter),
                style = stroke
            )
            // Progress
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset((size.width - diameter)/2, (size.height - diameter)/2),
                size = Size(diameter, diameter),
                style = stroke
            )
        }
        if (reducedMotion) {
            Text(
                text = if (isPaused) stringResource(id = R.string.paused_label) else timeStr,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        } else {
            Crossfade(targetState = isPaused, label = "pausedLabel") { paused ->
                Text(
                    text = if (paused) stringResource(id = R.string.paused_label) else timeStr,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
