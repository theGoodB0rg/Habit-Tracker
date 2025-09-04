package com.habittracker.reminders.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime

/**
 * Professional time picker dialog component for reminder settings.
 * Provides a user-friendly interface for setting reminder times.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Time"
) {
    var selectedHour by remember { mutableIntStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hour and Minute Selectors
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour Selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { 
                                    selectedHour = if (selectedHour > 0) selectedHour - 1 else 23 
                                }
                            ) {
                                Text("−", style = MaterialTheme.typography.headlineMedium)
                            }
                            
                            Card(
                                modifier = Modifier.width(60.dp).height(40.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = String.format("%02d", selectedHour),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = { 
                                    selectedHour = if (selectedHour < 23) selectedHour + 1 else 0 
                                }
                            ) {
                                Text("+", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        ":",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Minute Selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { 
                                    selectedMinute = if (selectedMinute > 0) selectedMinute - 1 else 59 
                                }
                            ) {
                                Text("−", style = MaterialTheme.typography.headlineMedium)
                            }
                            
                            Card(
                                modifier = Modifier.width(60.dp).height(40.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = String.format("%02d", selectedMinute),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = { 
                                    selectedMinute = if (selectedMinute < 59) selectedMinute + 1 else 0 
                                }
                            ) {
                                Text("+", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick time presets
                Text("Quick Presets", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "7:00" to LocalTime.of(7, 0),
                        "9:00" to LocalTime.of(9, 0),
                        "12:00" to LocalTime.of(12, 0),
                        "18:00" to LocalTime.of(18, 0),
                        "20:00" to LocalTime.of(20, 0)
                    ).forEach { (label, time) ->
                        OutlinedButton(
                            onClick = {
                                selectedHour = time.hour
                                selectedMinute = time.minute
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
                }
            ) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
