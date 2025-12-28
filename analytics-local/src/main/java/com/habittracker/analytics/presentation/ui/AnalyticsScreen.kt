package com.habittracker.analytics.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.analytics.domain.models.*
import com.habittracker.analytics.presentation.ui.components.*
import com.habittracker.analytics.presentation.viewmodel.*
import java.io.File
import android.content.Intent

/**
 * Modern Analytics Screen with comprehensive insights and beautiful visualizations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val analyticsData by viewModel.analyticsData.collectAsState()
    val selectedTimeFrame by viewModel.selectedTimeFrame.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val completionChartData by viewModel.completionRateChartData.collectAsState()
    val screenChartData by viewModel.screenVisitChartData.collectAsState()
    val userEngagementMode by viewModel.userEngagementMode.collectAsState()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle export success/error
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                if (state.isShare) {
                    // Share the file
                    val file = File(state.filePath)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (state.filePath.endsWith(".png")) "image/png" else "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Analytics"))
                } else {
                    // Show success message for download
                    snackbarHostState.showSnackbar("Export saved to Downloads")
                }
                viewModel.clearExportState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearExportState()
            }
            else -> { /* No action needed */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Quick share latest insights (Image)
                    IconButton(onClick = { viewModel.exportAnalytics(ExportFormat.IMAGE, isShare = true) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share insights",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Export menu
                    var showExportMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share as Image") },
                                onClick = {
                                    viewModel.exportAnalytics(ExportFormat.IMAGE, isShare = false)
                                    showExportMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as JSON") },
                                onClick = {
                                    viewModel.exportAnalytics(ExportFormat.JSON, isShare = false)
                                    showExportMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as CSV") },
                                onClick = {
                                    viewModel.exportAnalytics(ExportFormat.CSV, isShare = false)
                                    showExportMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        // Loading state with animated indicator
                        LoadingContent()
                    }
                    
                    uiState.error != null -> {
                        // Error state with retry option
                        uiState.error?.let { errorMessage ->
                            ErrorContent(
                                error = errorMessage,
                                onRetry = { viewModel.refreshData() },
                                onDismiss = { viewModel.clearError() }
                            )
                        }
                    }
                    
                    !uiState.hasData -> {
                        // Empty state
                        EmptyAnalyticsContent()
                    }
                    
                    else -> {
                        // Main analytics content
                        AnalyticsContent(
                            analyticsData = analyticsData,
                            selectedTimeFrame = selectedTimeFrame,
                            completionChartData = completionChartData,
                            screenChartData = screenChartData,
                            userEngagementMode = userEngagementMode,
                            onTimeFrameSelected = viewModel::selectTimeFrame,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // Export loading overlay
                if (exportState is ExportState.Exporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.padding(32.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Exporting analytics...")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    analyticsData: AnalyticsData?,
    selectedTimeFrame: TimeFrame,
    completionChartData: List<CompletionRateChartPoint>,
    screenChartData: List<ScreenVisitChartPoint>,
    userEngagementMode: UserEngagementMode,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time frame selector
        item {
            TimeFrameSelector(
                selectedTimeFrame = selectedTimeFrame,
                onTimeFrameSelected = onTimeFrameSelected
            )
        }
        
        // Engagement Mode Card
        item {
            UserEngagementModeCard(mode = userEngagementMode)
        }
        
        // Quick stats cards
        analyticsData?.let { data ->
            item {
                QuickStatsSection(data = data)
            }
            
            // Habit completion rate chart
            if (completionChartData.isNotEmpty()) {
                item {
                    AnalyticsCard(
                        title = "Habit Completion Rates",
                        icon = Icons.AutoMirrored.Filled.TrendingUp
                    ) {
                        CompletionRateChart(
                            data = completionChartData,
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }
            }
            
            // Screen engagement chart
            if (screenChartData.isNotEmpty()) {
                item {
                    AnalyticsCard(
                        title = "Screen Engagement",
                        icon = Icons.Default.Visibility
                    ) {
                        ScreenEngagementChart(
                            data = screenChartData,
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }
            }
            
            // Streak analysis
            item {
                StreakAnalysisSection(data = data)
            }
            
            // Insights section
            item {
                InsightsSection(data = data)
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TimeFrameSelector(
    selectedTimeFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Time Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimeFrame.values()) { timeFrame ->
                    FilterChip(
                        selected = timeFrame == selectedTimeFrame,
                        onClick = { onTimeFrameSelected(timeFrame) },
                        label = {
                            Text(
                                text = when (timeFrame) {
                                    TimeFrame.DAILY -> "Today"
                                    TimeFrame.WEEKLY -> "Week"
                                    TimeFrame.MONTHLY -> "Month"
                                    TimeFrame.QUARTERLY -> "Quarter"
                                    TimeFrame.YEARLY -> "Year"
                                    TimeFrame.ALL_TIME -> "All Time"
                                },
                                fontWeight = if (timeFrame == selectedTimeFrame) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsSection(data: AnalyticsData) {
    val avgCompletionRate = data.habitCompletionRates.map { it.completionPercentage }.average()
    val totalHabits = data.habitCompletionRates.size
    val activeStreaks = data.streakRetentions.count { it.isActive }
    val totalScreenTime = data.screenVisits.sumOf { it.totalTimeSpent }
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                title = "Completion Rate",
                value = "${avgCompletionRate.toInt()}%",
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        item {
            StatCard(
                title = "Total Habits",
                value = totalHabits.toString(),
                icon = Icons.AutoMirrored.Filled.List,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        item {
            StatCard(
                title = "Active Streaks",
                value = activeStreaks.toString(),
                icon = Icons.Default.Assessment,
                color = Color(0xFFFF6B35)
            )
        }
        
        item {
            StatCard(
                title = "Time Spent",
                value = "${totalScreenTime / (1000 * 60)}m",
                icon = Icons.Default.Schedule,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading analytics...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                    
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAnalyticsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Analytics Data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Start tracking your habits to see analytics",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun UserEngagementModeCard(mode: UserEngagementMode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Your Engagement Style",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mode.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}