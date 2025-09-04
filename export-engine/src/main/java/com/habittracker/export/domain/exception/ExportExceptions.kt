package com.habittracker.export.domain.exception

/**
 * Base exception for export operations
 */
sealed class ExportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when there's no data to export
 */
class NoDataToExportException(
    message: String = "No data available to export"
) : ExportException(message)

/**
 * Exception thrown when file operations fail
 */
class FileOperationException(
    message: String,
    cause: Throwable? = null
) : ExportException(message, cause)

/**
 * Exception thrown when data serialization fails
 */
class SerializationException(
    message: String,
    cause: Throwable? = null
) : ExportException(message, cause)

/**
 * Exception thrown when invalid configuration is provided
 */
class InvalidConfigurationException(
    message: String
) : ExportException(message)

/**
 * Exception thrown when storage permission is denied
 */
class StoragePermissionException(
    message: String = "Storage permission is required for export"
) : ExportException(message)

/**
 * Exception thrown when insufficient storage space is available
 */
class InsufficientStorageException(
    message: String = "Insufficient storage space for export"
) : ExportException(message)

/**
 * Exception thrown when PNG rendering fails
 */
class RenderingException(
    message: String,
    cause: Throwable? = null
) : ExportException(message, cause)
