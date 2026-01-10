package com.habittracker.ui.components.simple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Modern Confirmation Dialogs for Timer Actions
 * Follows Material Design 3 guidelines.
 */

@Composable
fun BelowMinDurationDialog(
    minSeconds: Int,
    elapsedSeconds: Int,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onLogPartial: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Outlined.Timer, contentDescription = null)
        },
        title = {
            Text("Complete early?")
        },
        text = {
            Column {
                Text(
                    "You've completed ${elapsedSeconds / 60}m of your ${minSeconds / 60}m minimum.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Would you like to complete anyway or log as partial progress?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onComplete) {
                Text("Complete Anyway")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Keep Timing")
                }
                TextButton(onClick = onLogPartial) {
                    Text("Log Partial")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscardSessionSheet(
    elapsedSeconds: Int,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onKeep: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp) // Extra padding for safe area logic if needed
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.TimerOff,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Discard ${elapsedSeconds / 60}m session?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "You've been timing for ${elapsedSeconds / 60} minutes. This progress will be lost if you discard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onKeep,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Keep Timing")
                }
                Button(
                    onClick = onDiscard,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Discard")
                }
            }
        }
    }
}

@Composable
fun EndPomodoroEarlyDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Outlined.Timer, contentDescription = null)
        },
        title = { Text("End focus early?") },
        text = { Text("End and complete the current Pomodoro session?") },
        confirmButton = {
            TextButton(onClick = onComplete) { Text("End & Complete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep Focus") }
        }
    )
}

@Composable
fun CompleteWithoutTimerDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Outlined.Warning, contentDescription = null)
        },
        title = { Text("Complete without timer?") },
        text = { Text("This habit usually requires a timer. Are you sure you want to mark it as done?") },
        confirmButton = {
            TextButton(onClick = onComplete) { Text("Mark Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
