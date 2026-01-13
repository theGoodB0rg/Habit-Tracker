package com.habittracker.ui.components.simple

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.ui.models.HabitUiModel

/**
 * SimpleHabitCard - Phase 2 simplified habit card following the HTML mockup design.
 * 
 * Features:
 * - Clean layout with habit info and action button
 * - Timer indicator when active
 * - Collapsible timer section
 * - States: default, completed, timer-active
 * 
 * Target: ~200-300 lines (simplified from EnhancedHabitCard's 1293 lines)
 */
@Composable
fun SimpleHabitCard(
    habit: HabitUiModel,
    isCompleted: Boolean,
    isTimerActive: Boolean,
    isTimerPaused: Boolean,
    remainingMs: Long,
    targetMs: Long,
    isLoading: Boolean,
    isBlocked: Boolean = false,
    isFinished: Boolean = false,
    onCardClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCompleteWithTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasTimer = habit.timing?.timerEnabled == true
    val timerRequired = habit.timing?.requireTimerToComplete == true
    val targetDuration = habit.timing?.estimatedDuration
    
    // Card background color based on state
    val containerColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "cardBackground"
    )
    
    // Border for timer active state
    val borderStroke = if (isTimerActive && !isCompleted) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 2.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Habit info + Action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Habit info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Habit name with timer indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Pulsing timer indicator - always composed to avoid SlotTable corruption
                        // but only visible when timer is active and not paused
                        PulsingTimerIndicator(
                            isVisible = isTimerActive && !isTimerPaused
                        )
                        
                        // Timer Required badge
                        if (timerRequired && !isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TimerRequiredBadge()
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Habit meta info
                    HabitMetaInfo(
                        habit = habit,
                        isCompleted = isCompleted,
                        elapsedMs = if (targetMs > 0) targetMs - remainingMs else 0L
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Action button
                ActionButton(
                    isCompleted = isCompleted,
                    isLoading = isLoading,
                    timerRequired = timerRequired,
                    isTimerActive = isTimerActive,
                    onClick = {
                        if (!timerRequired || isTimerActive) {
                            onCompleteClick()
                        }
                    }
                )
            }
            
            // Timer section (if timer enabled and not completed)
            if (hasTimer && !isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                HabitTimerSection(
                    isActive = isTimerActive,
                    isPaused = isTimerPaused,
                    remainingMs = remainingMs,
                    targetMs = targetMs,
                    targetDuration = targetDuration,
                    isLoading = isLoading,
                    isBlocked = isBlocked,
                    isFinished = isFinished,
                    onStart = onStartTimer,
                    onPause = onPauseTimer,
                    onResume = onResumeTimer,
                    onComplete = onCompleteWithTimer
                )
            }
        }
    }
}

@Composable
private fun HabitMetaInfo(
    habit: HabitUiModel,
    isCompleted: Boolean,
    elapsedMs: Long
) {
    val metaText = when {
        isCompleted && elapsedMs > 0 -> {
            val minutes = elapsedMs / 60_000
            "✓ Completed • ${minutes}m logged"
        }
        isCompleted -> "✓ Completed"
        habit.streakCount > 0 -> "🔥 ${habit.streakCount} day streak • ${habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() }}"
        else -> habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() }
    }
    
    Text(
        text = metaText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ActionButton(
    isCompleted: Boolean,
    isLoading: Boolean,
    timerRequired: Boolean,
    isTimerActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        label = "actionBtnBg"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.outline
        },
        label = "actionBtnContent"
    )
    
    val isDisabled = timerRequired && !isTimerActive && !isCompleted
    
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        enabled = !isLoading && !isDisabled,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isCompleted) MaterialTheme.colorScheme.primary 
                    else if (isDisabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline
        )
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            isCompleted -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(24.dp)
                )
            }
            isDisabled -> {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = "Timer required",
                    modifier = Modifier.size(24.dp)
                )
            }
            else -> {
                // Empty circle (the outline serves as the visual)
                Box(modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Pulsing timer indicator that is always composed but only visible when active.
 * This avoids SlotTable corruption by not conditionally composing infinite transitions.
 */
@Composable
private fun PulsingTimerIndicator(isVisible: Boolean) {
    // Always create the transition to avoid SlotTable corruption during measurement
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isVisible) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isVisible) 0.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    // Only show when visible - use width to control spacing
    if (isVisible) {
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(scale)
                .alpha(alpha)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun TimerRequiredBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = "Timer Required",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
