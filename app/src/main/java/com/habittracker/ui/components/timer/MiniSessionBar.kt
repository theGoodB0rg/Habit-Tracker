package com.habittracker.ui.components.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.ui.viewmodels.timing.ActiveTimerViewModel
import com.habittracker.timing.TimerController

@Composable
fun MiniSessionBar(modifier: Modifier = Modifier) {
    val vm: ActiveTimerViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val controller = remember(ctx) { TimerController(ctx) }
    var showSheet by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = state.active,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .padding(12.dp)
                .semantics { contentDescription = "Active timer session controls" }
        ) {
            if (showSheet) {
                TimerControlSheet { showSheet = false }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val totalSeconds = (state.remainingMs / 1000)
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                
                val timeText = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%d:%02d", minutes, seconds)
                }
                
                Column(Modifier.weight(1f)) {
                    Text(timeText, style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { if (state.totalMs == 0L) 0f else 1f - (state.remainingMs / state.totalMs.toFloat()) },
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { if (state.paused) controller.resume() else controller.pause() }) {
                    Icon(imageVector = if (state.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = if (state.paused) "Resume" else "Pause")
                }
                IconButton(onClick = { controller.extendFiveMinutes() }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Extend 5 minutes")
                }
                IconButton(onClick = { controller.complete() }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Complete session")
                }
                TextButton(onClick = { showSheet = true }) { Text("More") }
            }
        }
    }
}
