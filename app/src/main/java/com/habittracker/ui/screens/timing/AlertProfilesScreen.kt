package com.habittracker.ui.screens.timing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.data.database.entity.timing.TimerAlertProfileEntity
import com.habittracker.ui.viewmodels.timing.AlertProfilesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertProfilesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AlertProfilesViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<TimerAlertProfileEntity?>(null) }

    if (showEditor) {
        ProfileEditorDialog(
            initial = editingProfile,
            onDismiss = { showEditor = false },
            onSave = { entity ->
                vm.upsert(entity)
                showEditor = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert Profiles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editingProfile = null; showEditor = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add profile")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ui.profiles) { profile ->
                Card(onClick = { editingProfile = profile; showEditor = true }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(profile.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (!profile.description.isNullOrBlank()) {
                                Text(profile.description!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "Thresholds: ${profile.thresholdsJson}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (profile.isUserEditable) {
                            IconButton(onClick = { vm.delete(profile.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete profile")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    initial: TimerAlertProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (TimerAlertProfileEntity) -> Unit
) {
    var id by remember { mutableStateOf(initial?.id ?: "custom-${System.currentTimeMillis()}") }
    var displayName by remember { mutableStateOf(initial?.displayName ?: "Custom") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var thresholds by remember { mutableStateOf(initial?.thresholdsJson ?: "[0,25,50,75,100]") }
    val editable = initial?.isUserEditable != false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Profile" else "Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display name") }, enabled = editable)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") }, enabled = editable)
                OutlinedTextField(
                    value = thresholds,
                    onValueChange = { thresholds = it },
                    label = { Text("Thresholds JSON (e.g. [0,25,50,75,100])") },
                    enabled = editable
                )
                if (!editable) Text("Built-in profile (read-only)", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val entity = TimerAlertProfileEntity(
                        id = id,
                        displayName = displayName.ifBlank { "Custom" },
                        description = description.ifBlank { null },
                        thresholdsJson = thresholds.ifBlank { "[0,25,50,75,100]" },
                        isUserEditable = true
                    )
                    onSave(entity)
                },
                enabled = editable
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
