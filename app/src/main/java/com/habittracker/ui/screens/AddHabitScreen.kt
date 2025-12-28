package com.habittracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habittracker.analytics.presentation.viewmodel.AnalyticsViewModel
import com.habittracker.presentation.viewmodel.HabitViewModel
import com.habittracker.ui.design.WindowWidthClass
import com.habittracker.ui.design.rememberResponsiveHorizontalPadding
import com.habittracker.ui.design.rememberWindowWidthClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var habitDescription by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("Daily") }
    var selectedIconId by remember { mutableStateOf(0) }
    var showIconPicker by remember { mutableStateOf(false) }
    
    // Timer & Focus settings - default to timer enabled with meaningful duration
    var timerEnabled by remember { mutableStateOf(true) }
    var customDurationText by remember { mutableStateOf("15") } // Default 15 min
    var minDurationText by remember { mutableStateOf("") }
    var requireTimerToComplete by remember { mutableStateOf(true) } // Default: require timer
    var autoCompleteOnTarget by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Analytics integration
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()
    
    // Track screen visit
    DisposableEffect(Unit) {
        analyticsViewModel.trackScreenVisit("AddHabitScreen", "MainScreen")
        onDispose {
            analyticsViewModel.endScreenVisit()
        }
    }
    
    // Handle success navigation
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage?.contains("added") == true) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Habit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                        IconPicker(
                            selectedIconId = selectedIconId,
                            onIconSelected = { selectedIconId = it },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
            
            // Focus Setup - Quick Presets
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
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
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
                        }
                        item {
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
                        }
                        item {
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

            // Timer & Focus Card
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Timer & Focus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Enable timer toggle
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
                            onCheckedChange = null
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
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                supportingText = { Text("Leave blank to use global default") }
                            )

                            Spacer(Modifier.height(12.dp))

                            // Minimum duration to count as done
                            OutlinedTextField(
                                value = minDurationText,
                                onValueChange = { value ->
                                    if (value.length <= 3 && value.all { it.isDigit() }) minDurationText = value
                                    else if (value.isEmpty()) minDurationText = ""
                                },
                                label = { Text("Minimum duration to count (min)") },
                                placeholder = { Text("optional, e.g. 5") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                supportingText = { Text("Below this, a confirm dialog will appear") }
                            )

                            Spacer(Modifier.height(16.dp))

                            // Require timer to complete toggle
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
                                    onCheckedChange = null
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
                                        contentDescription = "Auto-complete at target. Automatically mark done when reaching the target duration."
                                        stateDescription = if (autoCompleteOnTarget) "On" else "Off"
                                    }
                                    .toggleable(value = autoCompleteOnTarget, role = Role.Switch, onValueChange = { autoCompleteOnTarget = it })
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Auto-complete at target", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Automatically mark done when reaching the target duration.",
                                        style = MaterialTheme.typography.bodySmall
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
                        viewModel.addHabit(
                            name = habitName.trim(),
                            description = habitDescription.trim(),
                            frequency = selectedFrequency,
                            iconId = selectedIconId,
                            timerEnabled = timerEnabled,
                            customDurationMinutes = customDurationText.toIntOrNull(),
                            minDurationMinutes = minDurationText.toIntOrNull(),
                            requireTimerToComplete = requireTimerToComplete,
                            autoCompleteOnTarget = autoCompleteOnTarget
                        )
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
                Text("Create Habit")
            }
        }
        }
    }
}

@Composable
private fun IconPicker(
    selectedIconId: Int,
    onIconSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = getAvailableIcons()
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 56.dp),
        modifier = modifier.heightIn(max = 240.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(icons) { (id, icon) ->
            IconButton(
                onClick = { onIconSelected(id) },
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (selectedIconId == id) {
                            Modifier
                        } else {
                            Modifier
                        }
                    )
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
    }
}

private fun getAvailableIcons(): List<Pair<Int, ImageVector>> {
    return listOf(
        1 to Icons.Filled.FitnessCenter,   // Exercise
        2 to Icons.Filled.LocalDrink,      // Water
        3 to Icons.Filled.MenuBook,        // Reading
        4 to Icons.Filled.DirectionsRun,   // Running
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
