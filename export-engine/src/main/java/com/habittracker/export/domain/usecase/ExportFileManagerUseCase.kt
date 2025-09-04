package com.habittracker.export.domain.usecase

import android.content.Context
import android.os.Environment
import com.habittracker.export.domain.exception.FileOperationException
import com.habittracker.export.domain.exception.InsufficientStorageException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing export files
 * Handles file operations, cleanup, and storage management
 */
@Singleton
class ExportFileManagerUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Get the export directory, creating it if necessary
     */
    suspend fun getExportDirectory(): File = withContext(Dispatchers.IO) {
        val exportDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "HabitTracker_Exports"
        )
        
        if (!exportDir.exists()) {
            val created = exportDir.mkdirs()
            if (!created) {
                throw FileOperationException("Failed to create export directory")
            }
        }
        
        exportDir
    }

    /**
     * Get all exported files
     */
    suspend fun getExportedFiles(): List<ExportFileInfo> = withContext(Dispatchers.IO) {
        try {
            val exportDir = getExportDirectory()
            val files = exportDir.listFiles { file ->
                file.isFile && (file.extension == "json" || file.extension == "csv")
            } ?: return@withContext emptyList()

            files.map { file ->
                ExportFileInfo(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = Date(file.lastModified()),
                    format = when (file.extension) {
                        "json" -> "JSON"
                        "csv" -> "CSV"
                        else -> "Unknown"
                    }
                )
            }.sortedByDescending { it.lastModified }
        } catch (e: Exception) {
            throw FileOperationException("Failed to list export files", e)
        }
    }

    /**
     * Delete an export file
     */
    suspend fun deleteExportFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists() && file.parentFile?.name == "HabitTracker_Exports") {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            throw FileOperationException("Failed to delete export file", e)
        }
    }

    /**
     * Clean up old export files (keep only last 10)
     */
    suspend fun cleanupOldExports(maxFiles: Int = 10) = withContext(Dispatchers.IO) {
        try {
            val files = getExportedFiles()
            if (files.size > maxFiles) {
                val filesToDelete = files.drop(maxFiles)
                filesToDelete.forEach { fileInfo ->
                    deleteExportFile(fileInfo.path)
                }
            }
        } catch (e: Exception) {
            // Log error but don't throw - cleanup is not critical
        }
    }

    /**
     * Check available storage space
     */
    suspend fun getAvailableStorageSpace(): Long = withContext(Dispatchers.IO) {
        try {
            val exportDir = getExportDirectory()
            exportDir.freeSpace
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Check if there's enough space for a file of given size
     */
    suspend fun hasEnoughSpace(requiredBytes: Long): Boolean {
        val availableSpace = getAvailableStorageSpace()
        return availableSpace > requiredBytes * 2 // Require 2x the file size for safety
    }

    /**
     * Get total size of all export files
     */
    suspend fun getTotalExportSize(): Long = withContext(Dispatchers.IO) {
        try {
            val files = getExportedFiles()
            files.sumOf { it.size }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Generate a unique filename for export
     */
    fun generateUniqueFileName(
        baseName: String? = null,
        format: String,
        scope: String = "habits"
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = baseName?.takeIf { it.isNotBlank() } ?: "${scope}_$timestamp"
        return if (name.endsWith(".$format")) name else "$name.$format"
    }

    /**
     * Validate filename for security and OS compatibility
     */
    fun validateFileName(fileName: String): String {
        // Remove or replace invalid characters
        val invalidChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        var validName = fileName
        
        for (char in invalidChars) {
            validName = validName.replace(char, '_')
        }
        
        // Trim whitespace and dots
        validName = validName.trim().trimEnd('.')
        
        // Ensure it's not empty
        if (validName.isBlank()) {
            validName = "export"
        }
        
        // Limit length
        if (validName.length > 100) {
            validName = validName.take(100)
        }
        
        return validName
    }
}

/**
 * Information about an exported file
 */
data class ExportFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Date,
    val format: String
)
