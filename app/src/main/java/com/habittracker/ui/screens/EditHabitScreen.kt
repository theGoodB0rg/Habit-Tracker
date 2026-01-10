package com.habittracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habittracker.presentation.viewmodel.HabitViewModel
import com.habittracker.ui.design.WindowWidthClass
import com.habittracker.ui.design.rememberResponsiveHorizontalPadding
import com.habittracker.ui.design.rememberWindowWidthClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitScreen(
    habitId: Long,
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle(initialValue = emptyList())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val habit = habits.find { it.id == habitId }
    
    var habitName by remember { mutableStateOf("") }
    var habitDescription by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("Daily") }
    var selectedIconId by remember { mutableStateOf(0) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Phase UIX-1 internal flag (set to true for developer validation)
    val enableTimerFeatureFlag = true
    // Default timer enabled to true - users can disable per-habit if they prefer no timer
    var timerEnabled by remember { mutableStateOf(true) }
    var customDurationText by remember { mutableStateOf("") }
    var autoCompleteOnTarget by remember { mutableStateOf(false) }
    var minDurationText by remember { mutableStateOf("") }
    var requireTimerToComplete by remember { mutableStateOf(false) }
    var showAlertProfileDialog by remember { mutableStateOf(false) }
    
    // Initialize form with habit data
    LaunchedEffect(habit) {
        habit?.let {
            habitName = it.name
            habitDescription = it.description
            selectedFrequency = it.frequency.name.lowercase().replaceFirstChar { char -> char.uppercase() }
            selectedIconId = it.iconId
            timerEnabled = it.timerEnabled
            customDurationText = it.customDurationMinutes?.toString() ?: ""
            // Load timing config if available (use this LaunchedEffect's coroutine)
            val timing = viewModel.getHabitTiming(it.id)
            autoCompleteOnTarget = timing?.autoCompleteOnTarget ?: false
            minDurationText = timing?.minDuration?.toMinutes()?.toString() ?: ""
            requireTimerToComplete = timing?.requireTimerToComplete ?: false
        }
    }
    
    // Handle success navigation
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage?.contains("updated") == true || 
            uiState.successMessage?.contains("deleted") == true) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Habit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (habit != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete habit",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (habit) {
                null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading habit...")
                    }
                }
                else -> {

        val widthClass = rememberWindowWidthClass()
        val horizontalPadding = rememberResponsiveHorizontalPadding()
        val maxContentWidth = if (widthClass == WindowWidthClass.Expanded) 840.dp else 720.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Habit Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Habit Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Current Streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${habit.streakCount} days",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                "Created",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                    .format(habit.createdDate),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Habit Name Field
            OutlinedTextField(
                value = habitName,
                onValueChange = { habitName = it },
                label = { Text("Habit Name") },
                placeholder = { Text("e.g., Morning Exercise") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                isError = habitName.isBlank() && uiState.errorMessage != null
            )
            
            // Description Field
            OutlinedTextField(
                value = habitDescription,
                onValueChange = { habitDescription = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("What does this habit involve?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
            
            // Frequency Selection
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Frequency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val frequencies = listOf("Daily", "Weekly", "Monthly")
                    frequencies.forEach { frequency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedFrequency == frequency,
                                    onClick = { selectedFrequency = frequency }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFrequency == frequency,
                                onClick = { selectedFrequency = frequency }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(frequency)
                        }
                    }
                }
            }
            
            // Icon Selection
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Choose Icon",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = { showIconPicker = !showIconPicker }
                        ) {
                            Text(if (showIconPicker) "Hide Icons" else "Show Icons")
                        }
                    }
                    
                    if (selectedIconId > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getIconById(selectedIconId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Selected icon")
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = showIconPicker,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        EditIconPicker(
                            selectedIconId = selectedIconId,
                            onIconSelected = { selectedIconId = it },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            // Quick Setup for Focus Habits
            if (enableTimerFeatureFlag) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Focus Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Quick presets for habits that benefit from focused practice",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            FilterChip(
                                onClick = {
                                    timerEnabled = true
                                    requireTimerToComplete = true
                                    customDurationText = "25"
                                },
                                label = { Text("Focus Session (25 min)") },
                                selected = timerEnabled && requireTimerToComplete && customDurationText == "25",
                                leadingIcon = if (timerEnabled && requireTimerToComplete && customDurationText == "25") {
                                    { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else {
                                    { Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                }
                            )
                            
                            FilterChip(
                                onClick = {
                                    timerEnabled = true
                                    requireTimerToComplete = true
                                    customDurationText = "15"
                                },
                                label = { Text("Short Focus (15 min)") },
                                selected = timerEnabled && requireTimerToComplete && customDurationText == "15",
                                leadingIcon = if (timerEnabled && requireTimerToComplete && customDurationText == "15") {
                                    { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else {
                                    { Icon(Icons.Filled.Timelapse, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                }
                            )
                            
                            FilterChip(
                                onClick = {
                                    timerEnabled = false
                                    requireTimerToComplete = false
                                    customDurationText = ""
                                },
                                label = { Text("Quick Check-off") },
                                selected = !timerEnabled && !requireTimerToComplete,
                                leadingIcon = if (!timerEnabled && !requireTimerToComplete) {
                                    { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else {
                                    { Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                }
                            )
                        }
                    }
                }
            }

            // Timer (Phase 2) – only show if flag enabled
            if (enableTimerFeatureFlag) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Timer & Focus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    role = Role.Switch
                                    contentDescription = "Enable per-habit timer. Adds a customizable session duration for this habit."
                                    stateDescription = if (timerEnabled) "On" else "Off"
                                }
                                .toggleable(value = timerEnabled, role = Role.Switch, onValueChange = { timerEnabled = it })
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Enable timer", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Adds a timer button with customizable duration.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = timerEnabled,
                                onCheckedChange = null,
                                colors = SwitchDefaults.colors()
                            )
                        }
                        AnimatedVisibility(visible = timerEnabled) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                OutlinedTextField(
                                    value = customDurationText,
                                    onValueChange = { value ->
                                        if (value.length <= 3 && value.all { it.isDigit() }) customDurationText = value
                                        if (value.isEmpty()) customDurationText = ""
                                    },
                                    label = { Text("Custom Duration (min)") },
                                    placeholder = { Text("e.g. 25") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    supportingText = { Text("Leave blank to use global default") }
                                )

                                Spacer(Modifier.height(12.dp))

                                // Minimum duration to count as done
                                OutlinedTextField(
                                    value = minDurationText,
                                    onValueChange = { value ->
                                        if (value.length <= 3 && value.all { it.isDigit() }) minDurationText = value else if (value.isEmpty()) minDurationText = ""
                                    },
                                    label = { Text("Minimum duration to count (min)") },
                                    placeholder = { Text("optional, e.g. 5") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    supportingText = { Text("Below this, a confirm dialog will appear") }
                                )

                                Spacer(Modifier.height(16.dp))

                                // Require timer to complete toggle - Prominent placement
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics(mergeDescendants = true) {
                                            role = Role.Switch
                                            contentDescription = "Require timer to complete. Prevent quick check-off; user must use the timer."
                                            stateDescription = if (requireTimerToComplete) "On" else "Off"
                                        }
                                        .toggleable(value = requireTimerToComplete, role = Role.Switch, onValueChange = { requireTimerToComplete = it })
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Timer,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Require timer to complete", fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                                                tooltip = {
                                                    RichTooltip(
                                                        title = { Text("Timer Required Mode") },
                                                        text = { 
                                                            Text(
                                                                "When enabled, you must start the timer before marking this habit complete. " +
                                                                "The checkmark button will show a timer icon instead, and tapping it will " +
                                                                "display a reminder to start the timer first.\n\n" +
                                                                "This helps ensure you actually spend focused time on the habit rather than " +
                                                                "just checking it off."
                                                            )
                                                        }
                                                    )
                                                },
                                                state = rememberTooltipState()
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Info,
                                                    contentDescription = "Learn more about timer required mode",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            "Users must start the timer before marking as done. Promotes focused habit practice.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = requireTimerToComplete,
                                        onCheckedChange = null,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                // Auto-complete at target toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics(mergeDescendants = true) {
                                            role = Role.Switch
                                            contentDescription = "Auto-complete at target. If enabled, the habit is marked done automatically when the timer finishes."
                                            stateDescription = if (autoCompleteOnTarget) "On" else "Off"
                                        }
                                        .toggleable(value = autoCompleteOnTarget, role = Role.Switch, onValueChange = { autoCompleteOnTarget = it })
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Auto-complete at target", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (autoCompleteOnTarget) "Automatically mark done when timer finishes"
                                            else "Timer will prompt you to complete manually",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = autoCompleteOnTarget,
                                        onCheckedChange = null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // UIX-6: Per-habit alert profile selector
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Alert Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val profileVm: com.habittracker.ui.viewmodels.timing.AlertProfilesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                        val ui by profileVm.ui.collectAsStateWithLifecycle()
                        Column(Modifier.weight(1f)) {
                            val display = ui.profiles.firstOrNull { it.id == habit.alertProfileId }
                            Text(display?.displayName ?: (habit.alertProfileId ?: ui.selectedId), style = MaterialTheme.typography.bodyMedium)
                            Text("Per-habit alert schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AssistChip(onClick = { showAlertProfileDialog = true }, label = { Text("Change") })
                    }
                }
            }

            if (showAlertProfileDialog) {
                val profileVm: com.habittracker.ui.viewmodels.timing.AlertProfilesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val ui by profileVm.ui.collectAsStateWithLifecycle()
                AlertDialog(
                    onDismissRequest = { showAlertProfileDialog = false },
                    confirmButton = {},
                    title = { Text("Select alert profile") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Allow clearing to follow global default
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = habit.alertProfileId == null,
                                        onClick = {
                                            viewModel.setHabitAlertProfile(habitId, null)
                                            showAlertProfileDialog = false
                                        },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = habit.alertProfileId == null, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Use default (${ui.selectedId})")
                            }
                            ui.profiles.forEach { p ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = habit.alertProfileId == p.id,
                                            onClick = {
                                                viewModel.setHabitAlertProfile(habitId, p.id)
                                                showAlertProfileDialog = false
                                            },
                                            role = Role.RadioButton
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = habit.alertProfileId == p.id, onClick = null)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(p.displayName, fontWeight = FontWeight.Medium)
                                        if (!p.description.isNullOrBlank()) Text(p.description!!, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                )
            }
            
            // Error message
            uiState.errorMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Button
            Button(
                onClick = {
                    if (habitName.isNotBlank()) {
                        viewModel.updateHabit(
                            habitId = habitId,
                            name = habitName.trim(),
                            description = habitDescription.trim(),
                            frequency = selectedFrequency,
                            iconId = selectedIconId,
                            timerEnabledOverride = if (enableTimerFeatureFlag) timerEnabled else null,
                            customDurationMinutesOverride = if (enableTimerFeatureFlag) customDurationText.toIntOrNull() else null
                        )

            // Persist per-habit timing settings (Phase 2/6)
                        if (enableTimerFeatureFlag) {
                            val timing = com.habittracker.ui.models.timing.HabitTiming(
                                estimatedDuration = customDurationText.toIntOrNull()?.let { java.time.Duration.ofMinutes(it.toLong()) },
                                timerEnabled = timerEnabled,
                                minDuration = minDurationText.toIntOrNull()?.let { java.time.Duration.ofMinutes(it.toLong()) },
                requireTimerToComplete = requireTimerToComplete,
                                autoCompleteOnTarget = autoCompleteOnTarget,
                                reminderStyle = com.habittracker.ui.models.timing.ReminderStyle.GENTLE,
                                isSchedulingEnabled = false
                            )
                            viewModel.saveHabitTiming(habitId, timing)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = habitName.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Update Habit")
            }
        }
        
        // Delete Confirmation Dialog - Moved inside the habit non-null check
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Habit") },
                    text = { 
                        Text("Are you sure you want to delete \"${habit?.name ?: ""}\"? This action cannot be undone and you'll lose all streak progress.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteHabit(habitId)
                                showDeleteDialog = false
                            }
                        ) {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
}
}
}








@Composable
private fun EditIconPicker(
    selectedIconId: Int,
    onIconSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = getAvailableIcons()
    // Manual grid implementation to avoid LazyGrid crash
    val columns = 5
    val rows = icons.chunked(columns)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowIcons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowIcons.forEach { (id, icon) ->
                    IconButton(
                        onClick = { onIconSelected(id) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selectedIconId == id) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                // Fill empty space in last row
                repeat(columns - rowIcons.size) {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

// Reuse the same icon functions from AddHabitScreen
private fun getAvailableIcons(): List<Pair<Int, ImageVector>> {
    return listOf(
        1 to Icons.Filled.FitnessCenter,   // Exercise
        2 to Icons.Filled.LocalDrink,      // Water
        3 to Icons.AutoMirrored.Filled.MenuBook,        // Reading
        4 to Icons.AutoMirrored.Filled.DirectionsRun,   // Running
        5 to Icons.Filled.Bedtime,         // Sleep
        6 to Icons.Filled.Restaurant,      // Eating
        7 to Icons.Filled.School,          // Learning
        8 to Icons.Filled.Work,            // Work
        9 to Icons.Filled.Favorite,        // Health
        10 to Icons.Filled.SelfImprovement, // Meditation
        11 to Icons.Filled.MusicNote,      // Music
        12 to Icons.Filled.Brush,          // Art
        13 to Icons.Filled.Code,           // Coding
        14 to Icons.Filled.Language,       // Languages
        15 to Icons.Filled.Savings,        // Money
        16 to Icons.Filled.CleanHands,     // Hygiene
        17 to Icons.Filled.Eco,            // Environment
        18 to Icons.Filled.Group,          // Social
        19 to Icons.Filled.Timer,          // Time management
        20 to Icons.Filled.Star,           // Goals
    )
}

private fun getIconById(iconId: Int): ImageVector {
    return getAvailableIcons().find { it.first == iconId }?.second ?: Icons.Filled.Circle
}
