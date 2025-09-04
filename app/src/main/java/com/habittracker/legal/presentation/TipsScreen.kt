package com.habittracker.legal.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.habittracker.R
import com.habittracker.legal.domain.HabitTip
import com.habittracker.legal.domain.TipCategory

/**
 * Tips screen with categorized helpful tips for users
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TipsScreen(
    navController: NavController,
    viewModel: LegalViewModel = hiltViewModel()
) {
    var selectedCategory by remember { mutableStateOf<TipCategory?>(null) }
    
    val filteredTipsState by viewModel.filterTipsByCategory(selectedCategory).collectAsState()
    
    // Load tips on first composition
    LaunchedEffect(Unit) {
        // Tips are automatically loaded by the ViewModel when first accessed
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tips & Advice") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category filter chips
            CategoryFilterSection(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            // Tips content
            AnimatedContent(
                targetState = filteredTipsState,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                },
                label = "tips_content"
            ) { state ->
                when (state) {
                    is UiState.Loading -> {
                        LoadingTipsContent()
                    }
                    is UiState.Success -> {
                        TipsContent(
                            tips = state.data,
                            selectedCategory = selectedCategory
                        )
                    }
                    is UiState.Error -> {
                        ErrorTipsContent(error = state.message)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterSection(
    selectedCategory: TipCategory?,
    onCategorySelected: (TipCategory?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            // All categories chip
            item {
                FilterChip(
                    onClick = { onCategorySelected(null) },
                    label = { Text("All") },
                    selected = selectedCategory == null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            
            // Individual category chips
            items(TipCategory.values()) { category ->
                FilterChip(
                    onClick = { onCategorySelected(category) },
                    label = { Text(getCategoryDisplayName(category)) },
                    selected = selectedCategory == category,
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TipsContent(
    tips: List<HabitTip>,
    selectedCategory: TipCategory?
) {
    if (tips.isEmpty()) {
        EmptyTipsContent(selectedCategory)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tips) { tip ->
                TipCard(tip = tip)
            }
            
            // Additional space at bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TipCard(tip: HabitTip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tip icon with background
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = getCategoryIconVector(tip.category),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = getCategoryDisplayName(tip.category),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tip title
                    Text(
                        text = getTipTitle(tip),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tip description
                    Text(
                        text = getTipDescription(tip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingTipsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Loading tips...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorTipsContent(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = "Failed to Load Tips",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyTipsContent(selectedCategory: TipCategory?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = if (selectedCategory != null) {
                        "No tips available for ${getCategoryDisplayName(selectedCategory)}"
                    } else {
                        "No tips available"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Check back later for helpful tips and advice!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper functions
private fun getCategoryDisplayName(category: TipCategory): String {
    return when (category) {
        TipCategory.GETTING_STARTED -> "Getting Started"
        TipCategory.MOTIVATION -> "Motivation"
        TipCategory.STREAK_BUILDING -> "Streak Building"
        TipCategory.HABIT_FORMATION -> "Habit Formation"
        TipCategory.PRODUCTIVITY -> "Productivity"
        TipCategory.TROUBLESHOOTING -> "Troubleshooting"
    }
}

private fun getCategoryIcon(category: TipCategory): ImageVector {
    return when (category) {
        TipCategory.GETTING_STARTED -> Icons.Default.PlayArrow
        TipCategory.MOTIVATION -> Icons.Default.Favorite
        TipCategory.STREAK_BUILDING -> Icons.Default.TrendingUp
        TipCategory.HABIT_FORMATION -> Icons.Default.Psychology
        TipCategory.PRODUCTIVITY -> Icons.Default.Speed
        TipCategory.TROUBLESHOOTING -> Icons.Default.Build
    }
}

private fun getCategoryIconVector(category: TipCategory): ImageVector {
    return getCategoryIcon(category)
}

@Composable
private fun getTipTitle(tip: HabitTip): String {
    val context = LocalContext.current
    return context.getString(tip.titleRes)
}

@Composable
private fun getTipDescription(tip: HabitTip): String {
    val context = LocalContext.current
    return context.getString(tip.descriptionRes)
}
