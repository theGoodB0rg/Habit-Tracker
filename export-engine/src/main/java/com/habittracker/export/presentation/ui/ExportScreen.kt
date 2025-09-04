package com.habittracker.export.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habittracker.export.domain.model.*
import com.habittracker.export.domain.usecase.ShareTarget
import com.habittracker.export.domain.usecase.ExportFileInfo
import com.habittracker.export.presentation.viewmodel.ExportViewModel
import com.habittracker.export.presentation.viewmodel.ExportUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Main export screen with modern Material 3 design
 * Provides comprehensive export functionality with excellent UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle share intents
    LaunchedEffect(Unit) {
        viewModel.shareIntent.collect { intent ->
            context.startActivity(intent)
        }
    }
    
    // Load export directory and files on first load
    LaunchedEffect(Unit) {
        viewModel.getExportDirectory()
        viewModel.loadAvailableExports()
    }

    // Get export preview when configuration changes
    LaunchedEffect(
        uiState.selectedFormat,
        uiState.selectedScope,
        uiState.includeCompletions,
        uiState.selectedHabitIds,
        uiState.startDate,
        uiState.endDate
    ) {
        viewModel.getExportPreview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Export Habits",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Export Configuration Section
            ExportConfigurationCard(
                uiState = uiState,
                onFormatChange = viewModel::updateFormat,
                onScopeChange = viewModel::updateScope,
                onIncludeCompletionsChange = viewModel::updateIncludeCompletions,
                onIncludeStreakHistoryChange = viewModel::updateIncludeStreakHistory,
                onIncludeMetadataChange = viewModel::updateIncludeMetadata,
                onCustomFileNameChange = viewModel::updateCustomFileName,
                onDateRangeChange = viewModel::updateDateRange
            )

            // Export Preview Section
            ExportPreviewCard(
                uiState = uiState,
                isLoading = uiState.isLoadingPreview,
                preview = uiState.exportPreview
            )
            
            // Export Directory & Files Section
            ExportDirectoryCard(
                uiState = uiState,
                onRefreshFiles = viewModel::loadAvailableExports,
                onDeleteFile = viewModel::deleteExportFile,
                onShareFile = viewModel::shareFile
            )

            // Export Progress Section
            AnimatedVisibility(
                visible = uiState.isExporting || exportProgress != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                ExportProgressCard(progress = exportProgress)
            }

            // Export Result Section
            AnimatedVisibility(
                visible = exportResult != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                exportResult?.let { result ->
                    ExportResultCard(
                        result = result,
                        onShare = viewModel::shareExport,
                        onShareToApp = viewModel::shareToApp,
                        onDismiss = viewModel::clearExportResult
                    )
                }
            }

            // Action Buttons
            ExportActionButtons(
                isExporting = uiState.isExporting,
                canExport = uiState.exportPreview?.habitCount ?: 0 > 0,
                hasLastExport = uiState.lastExportResult != null,
                onStartExport = viewModel::startExport,
                onShareLastExport = viewModel::shareLastExport
            )

            // Error Message
            uiState.errorMessage?.let { error ->
                ErrorCard(
                    message = error,
                    onDismiss = viewModel::clearError
                )
            }
        }
    }
}

@Composable
private fun ExportConfigurationCard(
    uiState: ExportUiState,
    onFormatChange: (ExportFormat) -> Unit,
    onScopeChange: (ExportScope) -> Unit,
    onIncludeCompletionsChange: (Boolean) -> Unit,
    onIncludeStreakHistoryChange: (Boolean) -> Unit,
    onIncludeMetadataChange: (Boolean) -> Unit,
    onCustomFileNameChange: (String) -> Unit,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Export Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Format Selection
            FormatSelectionSection(
                selectedFormat = uiState.selectedFormat,
                onFormatChange = onFormatChange
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Scope Selection
            ScopeSelectionSection(
                selectedScope = uiState.selectedScope,
                onScopeChange = onScopeChange,
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                onDateRangeChange = onDateRangeChange
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Include Options
            IncludeOptionsSection(
                includeCompletions = uiState.includeCompletions,
                includeStreakHistory = uiState.includeStreakHistory,
                includeMetadata = uiState.includeMetadata,
                onIncludeCompletionsChange = onIncludeCompletionsChange,
                onIncludeStreakHistoryChange = onIncludeStreakHistoryChange,
                onIncludeMetadataChange = onIncludeMetadataChange
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Custom File Name
            CustomFileNameSection(
                fileName = uiState.customFileName,
                onFileNameChange = onCustomFileNameChange
            )
        }
    }
}

@Composable
private fun FormatSelectionSection(
    selectedFormat: ExportFormat,
    onFormatChange: (ExportFormat) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Export Format",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExportFormat.values().forEach { format ->
                FilterChip(
                    onClick = { onFormatChange(format) },
                    label = { 
                        Text(
                            format.displayName,
                            fontWeight = if (selectedFormat == format) FontWeight.SemiBold else FontWeight.Normal
                        ) 
                    },
                    selected = selectedFormat == format,
                    leadingIcon = {
                        Icon(
                            when (format) {
                                ExportFormat.JSON -> Icons.Default.Code
                                ExportFormat.CSV -> Icons.Default.TableChart
                                ExportFormat.PNG -> Icons.Default.Image
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ScopeSelectionSection(
    selectedScope: ExportScope,
    onScopeChange: (ExportScope) -> Unit,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Export Scope",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        LazyColumn(
            modifier = Modifier.height(120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ExportScope.values()) { scope ->
                ScopeOption(
                    scope = scope,
                    isSelected = selectedScope == scope,
                    onClick = { onScopeChange(scope) }
                )
            }
        }

        // Date Range Picker for DATE_RANGE scope
        AnimatedVisibility(
            visible = selectedScope == ExportScope.DATE_RANGE,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            DateRangeSection(
                startDate = startDate,
                endDate = endDate,
                onDateRangeChange = onDateRangeChange
            )
        }
    }
}

@Composable
private fun ScopeOption(
    scope: ExportScope,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (scope) {
        ExportScope.ALL_HABITS -> Icons.Default.SelectAll
        ExportScope.ACTIVE_HABITS -> Icons.Default.CheckCircle
        ExportScope.SPECIFIC_HABIT -> Icons.Default.FilterList
        ExportScope.DATE_RANGE -> Icons.Default.DateRange
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = scope.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DateRangeSection(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select Date Range",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                    onValueChange = { },
                    label = { Text("Start Date") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { /* Open date picker */ }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select start date")
                        }
                    }
                )

                OutlinedTextField(
                    value = endDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                    onValueChange = { },
                    label = { Text("End Date") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { /* Open date picker */ }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select end date")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun IncludeOptionsSection(
    includeCompletions: Boolean,
    includeStreakHistory: Boolean,
    includeMetadata: Boolean,
    onIncludeCompletionsChange: (Boolean) -> Unit,
    onIncludeStreakHistoryChange: (Boolean) -> Unit,
    onIncludeMetadataChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Include Data",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        OptionCheckbox(
            label = "Completion History",
            description = "Include all habit completion records",
            checked = includeCompletions,
            onCheckedChange = onIncludeCompletionsChange,
            icon = Icons.Default.History
        )

        OptionCheckbox(
            label = "Streak Statistics",
            description = "Include current and longest streak data",
            checked = includeStreakHistory,
            onCheckedChange = onIncludeStreakHistoryChange,
            icon = Icons.Default.TrendingUp
        )

        OptionCheckbox(
            label = "Export Metadata",
            description = "Include export information and timestamps",
            checked = includeMetadata,
            onCheckedChange = onIncludeMetadataChange,
            icon = Icons.Default.Info
        )
    }
}

@Composable
private fun OptionCheckbox(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomFileNameSection(
    fileName: String,
    onFileNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Custom File Name (Optional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        OutlinedTextField(
            value = fileName,
            onValueChange = onFileNameChange,
            label = { Text("File name") },
            placeholder = { Text("Auto-generated if empty") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Description, contentDescription = null)
            },
            supportingText = {
                Text(
                    text = "File extension will be added automatically",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )
    }
}

@Composable
private fun ExportPreviewCard(
    uiState: ExportUiState,
    isLoading: Boolean,
    preview: com.habittracker.export.domain.usecase.ExportPreview?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Preview,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Export Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Loading preview...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (preview != null) {
                PreviewContent(preview = preview, format = uiState.selectedFormat)
            } else {
                Text(
                    text = "Configure export settings to see preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PreviewContent(
    preview: com.habittracker.export.domain.usecase.ExportPreview,
    format: ExportFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PreviewStat(
            icon = Icons.Default.FolderSpecial,
            label = "Habits",
            value = preview.habitCount.toString()
        )

        PreviewStat(
            icon = Icons.Default.CheckCircle,
            label = "Completions", 
            value = preview.completionCount.toString()
        )

        PreviewStat(
            icon = Icons.Default.Storage,
            label = "Size",
            value = "${preview.estimatedFileSizeKB} KB"
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "File: ${preview.fileName}.${format.extension}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PreviewStat(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExportProgressCard(
    progress: ExportProgress?
) {
    if (progress == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!progress.isCompleted) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Text(
                    text = progress.currentStep,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }

            LinearProgressIndicator(
                progress = progress.progressPercentage / 100f,
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Text(
                text = "${progress.progressPercentage}% complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ExportResultCard(
    result: ExportResult,
    onShare: (String) -> Unit,
    onShareToApp: (String, ShareTarget) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (result) {
                is ExportResult.Success -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                is ExportResult.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        when (result) {
                            is ExportResult.Success -> Icons.Default.CheckCircle
                            is ExportResult.Error -> Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = when (result) {
                            is ExportResult.Success -> MaterialTheme.colorScheme.primary
                            is ExportResult.Error -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = when (result) {
                            is ExportResult.Success -> "Export Successful"
                            is ExportResult.Error -> "Export Failed"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            when (result) {
                is ExportResult.Success -> {
                    SuccessContent(
                        result = result,
                        onShare = onShare,
                        onShareToApp = onShareToApp
                    )
                }
                is ExportResult.Error -> {
                    ErrorContent(result = result)
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    result: ExportResult.Success,
    onShare: (String) -> Unit,
    onShareToApp: (String, ShareTarget) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }
    
    // Show snackbar when file path is copied
    LaunchedEffect(showCopiedSnackbar) {
        if (showCopiedSnackbar) {
            kotlinx.coroutines.delay(1500)
            showCopiedSnackbar = false
        }
    }
    
    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        showCopiedSnackbar = true
    }
    
    fun openFileManager() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(
                    android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2F${context.packageName}%2Ffiles%2FDocuments%2FHabitTracker_Exports"),
                    "resource/folder"
                )
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic file manager
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(android.content.Intent.CATEGORY_OPENABLE)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Open File Manager"))
            } catch (ex: Exception) {
                // Handle error silently
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // File information
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "File: ${result.fileName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            // File path - prominently displayed
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved to:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = result.filePath,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = { 
                            // Copy file path to clipboard
                            copyToClipboard(result.filePath)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy file path",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Size: ${formatFileSize(result.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Records: ${result.recordCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // File Management Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { openFileManager() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open Folder", fontSize = 12.sp)
            }
            
            OutlinedButton(
                onClick = { copyToClipboard(result.filePath) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Path", fontSize = 12.sp)
            }
        }

        // Share options
        Text(
            text = "Share Options",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )

        // Quick share buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onShare(result.filePath) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share")
            }

            OutlinedButton(
                onClick = { onShareToApp(result.filePath, ShareTarget.EMAIL) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Email")
            }
        }
        
        // Snackbar for clipboard feedback
        if (showCopiedSnackbar) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Text(
                        text = "File path copied to clipboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(result: ExportResult.Error) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = result.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )

        if (result.exception.message != null && result.exception.message != result.message) {
            Text(
                text = "Details: ${result.exception.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ExportActionButtons(
    isExporting: Boolean,
    canExport: Boolean,
    hasLastExport: Boolean,
    onStartExport: () -> Unit,
    onShareLastExport: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onStartExport,
            enabled = !isExporting && canExport,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exporting...")
            } else {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Start Export",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (hasLastExport && !isExporting) {
            OutlinedButton(
                onClick = onShareLastExport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Share Last Export",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

@Composable
private fun ExportDirectoryCard(
    uiState: ExportUiState,
    onRefreshFiles: () -> Unit,
    onDeleteFile: (String) -> Unit,
    onShareFile: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Export Location & Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(
                    onClick = onRefreshFiles,
                    enabled = !uiState.isLoadingFiles
                ) {
                    if (uiState.isLoadingFiles) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh files",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Export Directory Info
            uiState.exportDirectory?.let { directory ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Export Directory",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Text(
                            text = directory,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "All exported files are saved to your device's Documents folder under HabitTracker_Exports",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Available Export Files
            if (uiState.availableExports.isNotEmpty()) {
                Text(
                    text = "Available Export Files (${uiState.availableExports.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.availableExports) { fileInfo ->
                        ExportFileItem(
                            fileInfo = fileInfo,
                            onShare = { onShareFile(fileInfo.path) },
                            onDelete = { onDeleteFile(fileInfo.path) }
                        )
                    }
                }
            } else if (!uiState.isLoadingFiles) {
                Text(
                    text = "No export files found. Create your first export above!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ExportFileItem(
    fileInfo: ExportFileInfo,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = fileInfo.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = fileInfo.format,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = formatFileSize(fileInfo.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                            .format(fileInfo.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share file",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete file",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
