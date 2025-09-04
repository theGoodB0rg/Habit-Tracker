package com.habittracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habittracker.analytics.presentation.viewmodel.AnalyticsViewModel
import com.habittracker.presentation.viewmodel.HabitViewModel

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
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Analytics integration
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()
    
    // Track screen visit
    LaunchedEffect(Unit) {
        analyticsViewModel.trackScreenVisit("AddHabitScreen", "MainScreen")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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
                            iconId = selectedIconId
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

@Composable
private fun IconPicker(
    selectedIconId: Int,
    onIconSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = getAvailableIcons()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier.height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
