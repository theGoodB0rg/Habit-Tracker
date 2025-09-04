package com.habittracker.export.domain.model

/**
 * Enum representing supported export formats
 */
enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    JSON("json", "application/json", "JSON"),
    CSV("csv", "text/csv", "CSV"),
    PNG("png", "image/png", "PNG Visual Report")
}

/**
 * Enum representing different export scopes
 */
enum class ExportScope(
    val displayName: String
) {
    ALL_HABITS("All Habits"),
    ACTIVE_HABITS("Active Habits Only"),
    SPECIFIC_HABIT("Specific Habit"),
    DATE_RANGE("Date Range")
}

/**
 * Configuration for export operations
 */
data class ExportConfig(
    val format: ExportFormat,
    val scope: ExportScope,
    val includeCompletions: Boolean = true,
    val includeStreakHistory: Boolean = true,
    val includeMetadata: Boolean = true,
    val habitIds: List<Long> = emptyList(),
    val startDate: String? = null,
    val endDate: String? = null,
    val fileName: String? = null
)

/**
 * Result of export operation
 */
sealed class ExportResult {
    data class Success(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val recordCount: Int
    ) : ExportResult()
    
    data class Error(
        val exception: Exception,
        val message: String
    ) : ExportResult()
}

/**
 * Progress callback for export operations
 */
data class ExportProgress(
    val currentStep: String,
    val progressPercentage: Int,
    val isCompleted: Boolean = false
)
