package com.habittracker.ui.components.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.timing.TimerController
import com.habittracker.timerux.resolveTimerUxEntryPoint
import com.habittracker.timerux.TimerCompletionInteractor.Intent as TimerIntent
import com.habittracker.timing.TimerFeatureFlags
import com.habittracker.ui.viewmodels.timing.ActiveTimerViewModel
import com.habittracker.ui.viewmodels.timing.TimerTickerViewModel

/**
 * Data class representing a timer session for the switcher UI.
 */
data class TimerSwitcherSession(
    val sessionId: Long,
    val habitId: Long,
    val habitName: String,
    val remainingMs: Long,
    val isPaused: Boolean,
    val isActive: Boolean
)

/**
 * Bottom sheet that appears when a timer is auto-paused due to single-active timer enforcement.
 * Shows both the newly started timer and the paused timer, allowing quick switching.
 * 
 * Improves UX over the simple "Paused previous timer — Resume" snackbar by:
 * 1. Showing both timer states at a glance
 * 2. Providing clear visual indication of which is active
 * 3. Offering explicit "Switch" action instead of implicit undo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSwitcherSheet(
    activeSession: TimerSwitcherSession,
    pausedSession: TimerSwitcherSession,
    onSwitchToActive: () -> Unit,
    onSwitchToPaused: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Timer Switched",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "You started a new timer. Your previous timer has been paused.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Active Timer Card
            TimerSessionCard(
                session = activeSession,
                isHighlighted = true,
                label = "Now Running",
                onClick = onSwitchToActive,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Paused Timer Card
            TimerSessionCard(
                session = pausedSession,
                isHighlighted = false,
                label = "Paused",
                onClick = onSwitchToPaused,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSwitchToPaused,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume Paused")
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Continue")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TimerSessionCard(
    session: TimerSwitcherSession,
    isHighlighted: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isHighlighted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (session.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (session.isPaused) "Paused" else "Running",
                    tint = if (isHighlighted) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Timer info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = session.habitName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Time remaining
            Text(
                text = formatTime(session.remainingMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else contentColor
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * State holder for timer switcher visibility and sessions.
 */
@Composable
fun rememberTimerSwitcherState(): TimerSwitcherState {
    return remember { TimerSwitcherState() }
}

class TimerSwitcherState {
    var isVisible by mutableStateOf(false)
        private set
    
    var activeSession by mutableStateOf<TimerSwitcherSession?>(null)
        private set
    
    var pausedSession by mutableStateOf<TimerSwitcherSession?>(null)
        private set
    
    fun show(active: TimerSwitcherSession, paused: TimerSwitcherSession) {
        activeSession = active
        pausedSession = paused
        isVisible = true
    }
    
    fun dismiss() {
        isVisible = false
    }
}
