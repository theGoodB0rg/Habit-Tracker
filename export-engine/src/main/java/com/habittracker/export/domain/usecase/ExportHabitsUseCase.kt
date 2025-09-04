package com.habittracker.export.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat
import com.habittracker.export.data.entity.HabitCompletionEntity
import com.habittracker.export.data.entity.HabitEntity
import com.habittracker.export.data.entity.PartialSessionEntity
import com.habittracker.export.data.entity.TimerSessionEntity
import com.habittracker.export.data.mapper.ExportDataMapper
import com.habittracker.export.data.model.DateRange
import com.habittracker.export.data.model.ExportMetadata
import com.habittracker.export.data.model.HabitExportData
import com.habittracker.export.data.model.PngCustomization
import com.habittracker.export.data.processor.PngDataProcessor
import com.habittracker.export.data.repository.ExportDataRepository
import com.habittracker.export.domain.exception.*
import com.habittracker.export.domain.formatter.ExportFormatter
import com.habittracker.export.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for handling habit data export operations
 * Provides comprehensive export functionality with proper error handling and progress tracking
 */
@Singleton
class ExportHabitsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ExportDataRepository,
    private val mapper: ExportDataMapper,
    private val formatter: ExportFormatter,
    private val pngDataProcessor: PngDataProcessor
) {

    /**
     * Export habits with progress tracking
     */
    fun exportHabits(config: ExportConfig): Flow<Result<ExportProgress, ExportResult>> = flow {
        try {
            // Validate configuration
            emit(Result.Progress(ExportProgress("Validating configuration...", 0)))
            validateConfiguration(config)

            // Check storage permissions
            emit(Result.Progress(ExportProgress("Checking permissions...", 5)))
            checkStoragePermissions()

            // Collect data
            emit(Result.Progress(ExportProgress("Collecting habit data...", 10)))
            val habits = collectHabits(config)
            
            if (habits.isEmpty()) {
                throw NoDataToExportException("No habits found matching the export criteria")
            }

            // Collect completions
            emit(Result.Progress(ExportProgress("Collecting completion data...", 30)))
            val completions = if (config.includeCompletions) {
                collectCompletions(config, habits)
            } else {
                emptyMap()
            }

        // Transform data
            emit(Result.Progress(ExportProgress("Transforming data...", 50)))
            val result = when (config.format) {
                ExportFormat.JSON -> {
            // Collect timer sessions and partials for JSON export
            emit(Result.Progress(ExportProgress("Collecting timer sessions...", 55)))
            val sessionsByHabit = collectTimerSessions(config, habits)
            emit(Result.Progress(ExportProgress("Collecting partial sessions...", 58)))
            val partialsByHabit = collectPartialSessions(config, habits)
            val exportData = createExportData(config, habits, completions, sessionsByHabit, partialsByHabit)
                    val jsonContent = formatter.formatAsJson(exportData)
                    writeToFile(jsonContent, config, ExportFormat.JSON)
                }
                ExportFormat.CSV -> {
                    val csvRows = mapper.toHabitCsvRows(habits, completions)
                    val csvContent = formatter.formatAsCsv(csvRows)
                    writeToFile(csvContent, config, ExportFormat.CSV)
                }
                ExportFormat.PNG -> {
                    emit(Result.Progress(ExportProgress("Generating visual report...", 60)))
                    val pngData = pngDataProcessor.preparePngExportData(
                        habits = habits,
                        completions = completions,
                        config = config,
                        customization = PngCustomization() // Use default customization for now
                    )
                    emit(Result.Progress(ExportProgress("Rendering PNG image...", 80)))
                    val pngContent = formatter.formatAsPng(pngData)
                    writeBinaryToFile(pngContent, config, ExportFormat.PNG)
                }
            }

            emit(Result.Progress(ExportProgress("Export completed successfully", 100, true)))
            emit(Result.Success(result))

        } catch (e: ExportException) {
            emit(Result.Success(ExportResult.Error(e, e.message ?: "Export failed")))
        } catch (e: Exception) {
            emit(Result.Success(ExportResult.Error(e, "Unexpected error during export: ${e.message}")))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get export file info without performing the actual export
     */
    suspend fun getExportPreview(config: ExportConfig): ExportPreview = withContext(Dispatchers.IO) {
        val habits = collectHabits(config)
        val completions = if (config.includeCompletions) {
            collectCompletions(config, habits)
        } else {
            emptyMap()
        }

        val totalCompletions = completions.values.sumOf { it.size }
        val estimatedSize = estimateFileSize(config.format, habits.size, totalCompletions)

        ExportPreview(
            habitCount = habits.size,
            completionCount = totalCompletions,
            estimatedFileSizeKB = estimatedSize,
            fileName = generateFileName(config)
        )
    }

    private fun validateConfiguration(config: ExportConfig) {
        when (config.scope) {
            ExportScope.SPECIFIC_HABIT -> {
                if (config.habitIds.isEmpty()) {
                    throw InvalidConfigurationException("Habit IDs must be specified for specific habit export")
                }
            }
            ExportScope.DATE_RANGE -> {
                if (config.startDate == null || config.endDate == null) {
                    throw InvalidConfigurationException("Start and end dates must be specified for date range export")
                }
                try {
                    val start = LocalDate.parse(config.startDate)
                    val end = LocalDate.parse(config.endDate)
                    if (start.isAfter(end)) {
                        throw InvalidConfigurationException("Start date cannot be after end date")
                    }
                } catch (e: Exception) {
                    throw InvalidConfigurationException("Invalid date format. Use yyyy-MM-dd format")
                }
            }
            else -> {
                // No additional validation needed for ALL_HABITS and ACTIVE_HABITS
            }
        }
    }

    private fun checkStoragePermissions() {
        // For API 30+, MANAGE_EXTERNAL_STORAGE permission might be needed
        // For now, we'll use the app's external files directory which doesn't require permissions
        val externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (externalFilesDir == null || !externalFilesDir.canWrite()) {
            throw StoragePermissionException("Cannot access external storage directory")
        }
    }

    private suspend fun collectHabits(config: ExportConfig): List<HabitEntity> {
        return when (config.scope) {
            ExportScope.ALL_HABITS -> repository.getAllHabits()
            ExportScope.ACTIVE_HABITS -> repository.getActiveHabits()
            ExportScope.SPECIFIC_HABIT -> repository.getHabitsById(config.habitIds)
            ExportScope.DATE_RANGE -> {
                // For date range, we get all habits but filter completions by date
                repository.getAllHabits()
            }
        }
    }

    private suspend fun collectCompletions(
        config: ExportConfig,
        habits: List<HabitEntity>
    ): Map<Long, List<HabitCompletionEntity>> {
        val habitIds = habits.map { it.id }
        
        val completions = when (config.scope) {
            ExportScope.DATE_RANGE -> {
                val startDate = LocalDate.parse(config.startDate!!)
                val endDate = LocalDate.parse(config.endDate!!)
                repository.getCompletionsInDateRange(startDate, endDate)
                    .filter { it.habitId in habitIds }
            }
            else -> {
                repository.getCompletionsForHabits(habitIds)
            }
        }

        return completions.groupBy { it.habitId }
    }

    private suspend fun collectTimerSessions(
        config: ExportConfig,
        habits: List<HabitEntity>
    ): Map<Long, List<TimerSessionEntity>> {
        val habitIds = habits.map { it.id }
        val sessions = when (config.scope) {
            ExportScope.DATE_RANGE -> {
                val startDate = LocalDate.parse(config.startDate!!)
                val endDate = LocalDate.parse(config.endDate!!)
                repository.getTimerSessionsInDateRange(startDate, endDate)
                    .filter { it.habitId in habitIds }
            }
            else -> repository.getTimerSessionsForHabits(habitIds)
        }
        return sessions.groupBy { it.habitId }
    }

    private suspend fun collectPartialSessions(
        config: ExportConfig,
        habits: List<HabitEntity>
    ): Map<Long, List<PartialSessionEntity>> {
        val habitIds = habits.map { it.id }
        val partials = repository.getPartialSessionsForHabits(habitIds)
        return partials.groupBy { it.habitId }
    }

    private fun createExportData(
        config: ExportConfig,
        habits: List<HabitEntity>,
        completions: Map<Long, List<HabitCompletionEntity>>,
        sessions: Map<Long, List<TimerSessionEntity>>,
        partials: Map<Long, List<PartialSessionEntity>>
    ): HabitExportData {
        val exportableHabits = habits.map { habit ->
            val habitCompletions = if (config.includeCompletions) {
                completions[habit.id] ?: emptyList()
            } else {
                emptyList()
            }
            val habitSessions = sessions[habit.id] ?: emptyList()
            val habitPartials = partials[habit.id] ?: emptyList()
            mapper.toExportableHabit(habit, habitCompletions, habitSessions, habitPartials)
        }

        val metadata = if (config.includeMetadata) {
            createExportMetadata(config, habits, completions, sessions, partials)
        } else {
            createMinimalMetadata(config)
        }

        return HabitExportData(
            metadata = metadata,
            habits = exportableHabits
        )
    }

    private fun createExportMetadata(
        config: ExportConfig,
        habits: List<HabitEntity>,
        completions: Map<Long, List<HabitCompletionEntity>>,
        sessions: Map<Long, List<TimerSessionEntity>>,
        partials: Map<Long, List<PartialSessionEntity>>
    ): ExportMetadata {
        val totalCompletions = completions.values.sumOf { it.size }
        val totalSessions = sessions.values.sumOf { it.size }
        val totalPartials = partials.values.sumOf { it.size }
        val dateRange = if (config.startDate != null && config.endDate != null) {
            DateRange(config.startDate, config.endDate)
        } else null

        return ExportMetadata(
            appVersion = getAppVersion(),
            exportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            exportFormat = config.format.displayName,
            totalHabits = habits.size,
            totalCompletions = totalCompletions,
            totalSessions = totalSessions,
            totalPartials = totalPartials,
            dateRange = dateRange,
            exportScope = config.scope.displayName
        )
    }

    private fun createMinimalMetadata(config: ExportConfig): ExportMetadata {
        return ExportMetadata(
            appVersion = getAppVersion(),
            exportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            exportFormat = config.format.displayName,
            totalHabits = 0,
            totalCompletions = 0,
            dateRange = null,
            exportScope = config.scope.displayName
        )
    }

    private suspend fun writeToFile(
        content: String,
        config: ExportConfig,
        format: ExportFormat
    ): ExportResult.Success = withContext(Dispatchers.IO) {
        try {
            val fileName = config.fileName ?: generateFileName(config)
            val file = createExportFile(fileName, format)
            
            // Check available space
            val contentBytes = content.toByteArray()
            val availableSpace = file.parentFile?.freeSpace ?: 0L
            if (availableSpace < contentBytes.size * 2) { // Require 2x the file size for safety
                throw InsufficientStorageException()
            }

            file.writeText(content)

            ExportResult.Success(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                recordCount = content.lines().size - 1 // Subtract header for CSV
            )
        } catch (e: Exception) {
            throw FileOperationException("Failed to write export file", e)
        }
    }

    /**
     * Writes binary data (like PNG) to file with proper memory management
     */
    private suspend fun writeBinaryToFile(
        content: ByteArray,
        config: ExportConfig,
        format: ExportFormat
    ): ExportResult.Success = withContext(Dispatchers.IO) {
        try {
            val fileName = config.fileName ?: generateFileName(config)
            val file = createExportFile(fileName, format)
            
            // Check available space
            val availableSpace = file.parentFile?.freeSpace ?: 0L
            if (availableSpace < content.size * 2) { // Require 2x the file size for safety
                throw InsufficientStorageException()
            }

            file.writeBytes(content)

            ExportResult.Success(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                recordCount = 1 // PNG is a single visual record
            )
        } catch (e: Exception) {
            throw FileOperationException("Failed to write binary export file", e)
        }
    }

    private fun createExportFile(fileName: String, format: ExportFormat): File {
        val exportDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "HabitTracker_Exports"
        )
        
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val finalFileName = if (fileName.endsWith(".${format.extension}")) {
            fileName
        } else {
            "$fileName.${format.extension}"
        }

        return File(exportDir, finalFileName)
    }

    private fun generateFileName(config: ExportConfig): String {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        
        val scopePrefix = when (config.scope) {
            ExportScope.ALL_HABITS -> "all_habits"
            ExportScope.ACTIVE_HABITS -> "active_habits"
            ExportScope.SPECIFIC_HABIT -> "selected_habits"
            ExportScope.DATE_RANGE -> "habits_${config.startDate}_to_${config.endDate}"
        }

        return "${scopePrefix}_$timestamp"
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    private fun estimateFileSize(format: ExportFormat, habitCount: Int, completionCount: Int): Long {
        return when (format) {
            ExportFormat.JSON -> {
                // Rough estimate: 500 bytes per habit + 200 bytes per completion + metadata
                (habitCount * 500L + completionCount * 200L + 1024L) / 1024L // Convert to KB
            }
            ExportFormat.CSV -> {
                // Rough estimate: 150 bytes per row (habit + completion)
                ((habitCount + completionCount) * 150L + 200L) / 1024L // Convert to KB
            }
            ExportFormat.PNG -> {
                // PNG files are typically 500KB - 2MB depending on resolution and content
                1024L // 1MB estimate in KB
            }
        }
    }
}

/**
 * Result wrapper for export operations with progress support
 */
sealed class Result<out P, out S> {
    data class Progress<P>(val progress: P) : Result<P, Nothing>()
    data class Success<S>(val data: S) : Result<Nothing, S>()
}

/**
 * Preview information for export operations
 */
data class ExportPreview(
    val habitCount: Int,
    val completionCount: Int,
    val estimatedFileSizeKB: Long,
    val fileName: String
)
