package com.habittracker.ui.components.simple

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration

/**
 * HabitTimerSection - Collapsible timer section for SimpleHabitCard.
 * 
 * Features:
 * - Collapsed: AssistChip with "Start Xm"
 * - Expanded: Large time display + pause/complete buttons + progress bar
 * - Auto-expands when timer is active
 */
@Composable
fun HabitTimerSection(
    isActive: Boolean,
    isPaused: Boolean,
    remainingMs: Long,
    targetMs: Long,
    targetDuration: Duration?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Show expanded state when timer is active, collapsed when idle
        AnimatedVisibility(
            visible = isActive,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            TimerExpandedState(
                remainingMs = remainingMs,
                targetMs = targetMs,
                isPaused = isPaused,
                onPause = onPause,
                onResume = onResume,
                onComplete = onComplete
            )
        }
        
        AnimatedVisibility(
            visible = !isActive,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            TimerCollapsedState(
                targetDuration = targetDuration,
                onStart = onStart
            )
        }
    }
}

@Composable
private fun TimerCollapsedState(
    targetDuration: Duration?,
    onStart: () -> Unit
) {
    val durationText = targetDuration?.let {
        val minutes = it.toMinutes()
        "Start ${minutes}m"
    } ?: "Start timer"
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        AssistChip(
            onClick = onStart,
            label = { Text(durationText) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun TimerExpandedState(
    remainingMs: Long,
    targetMs: Long,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Time display and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large time display
                Text(
                    text = formatTime(remainingMs),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFeatureSettings = "tnum" // Tabular numbers for stable width
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                
                // Controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pause/Resume button
                    FilledIconButton(
                        onClick = if (isPaused) onResume else onPause,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause"
                        )
                    }
                    
                    // Complete button
                    FilledIconButton(
                        onClick = onComplete,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            val progress = if (targetMs > 0) {
                ((targetMs - remainingMs).toFloat() / targetMs).coerceIn(0f, 1f)
            } else 0f
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Elapsed / Target labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val elapsedMs = targetMs - remainingMs
                Text(
                    text = "${formatTime(elapsedMs.coerceAtLeast(0L))} elapsed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Target: ${formatTime(targetMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Formats milliseconds to MM:SS format
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
