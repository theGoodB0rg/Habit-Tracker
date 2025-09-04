package com.habittracker.export.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.habittracker.export.domain.exception.FileOperationException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for sharing exported files via Android's share intent
 * Supports sharing to various apps like Telegram, Gmail, Google Drive, etc.
 */
@Singleton
class ShareExportUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Share an exported file using Android's share intent
     */
    suspend fun shareFile(
        filePath: String,
        title: String = "Share Habit Data",
        chooserTitle: String = "Share via"
    ): Intent = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                throw FileOperationException("Export file not found: $filePath")
            }

            // Get file URI using FileProvider for security
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.export.fileprovider",
                file
            )

            createShareIntent(fileUri, file, title, chooserTitle)

        } catch (e: Exception) {
            throw FileOperationException("Failed to create share intent", e)
        }
    }

    /**
     * Create a share intent for multiple files
     */
    suspend fun shareMultipleFiles(
        filePaths: List<String>,
        title: String = "Share Habit Data",
        chooserTitle: String = "Share via"
    ): Intent = withContext(Dispatchers.IO) {
        try {
            val fileUris = filePaths.map { filePath ->
                val file = File(filePath)
                if (!file.exists()) {
                    throw FileOperationException("Export file not found: $filePath")
                }

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.export.fileprovider",
                    file
                )
            }

            createMultipleShareIntent(fileUris, title, chooserTitle)

        } catch (e: Exception) {
            throw FileOperationException("Failed to create multiple share intent", e)
        }
    }

    /**
     * Get available apps that can handle the file type
     */
    fun getAvailableShareApps(mimeType: String): List<ShareAppInfo> {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
        }

        val packageManager = context.packageManager
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        return resolveInfos.map { resolveInfo ->
            ShareAppInfo(
                packageName = resolveInfo.activityInfo.packageName,
                appName = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager)
            )
        }
    }

    /**
     * Create share intent specifically for popular apps
     */
    suspend fun shareToSpecificApp(
        filePath: String,
        targetApp: ShareTarget,
        title: String = "Habit Tracker Export"
    ): Intent = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw FileOperationException("Export file not found: $filePath")
        }

        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.export.fileprovider",
            file
        )

        when (targetApp) {
            ShareTarget.EMAIL -> createEmailIntent(fileUri, file, title)
            ShareTarget.TELEGRAM -> createTelegramIntent(fileUri, file, title)
            ShareTarget.WHATSAPP -> createWhatsAppIntent(fileUri, file, title)
            ShareTarget.GOOGLE_DRIVE -> createGoogleDriveIntent(fileUri, file, title)
            ShareTarget.DROPBOX -> createDropboxIntent(fileUri, file, title)
        }
    }

    private fun createShareIntent(
        fileUri: Uri,
        file: File,
        title: String,
        chooserTitle: String
    ): Intent {
        val mimeType = getMimeType(file)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Habit Tracker export: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun createMultipleShareIntent(
        fileUris: List<Uri>,
        title: String,
        chooserTitle: String
    ): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*" // Mixed file types
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(fileUris))
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Habit Tracker export files")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun createEmailIntent(fileUri: Uri, file: File, title: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf<String>())
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, 
                "Please find attached my habit tracking data exported from Habit Tracker app.\n\n" +
                "File: ${file.name}\n" +
                "Size: ${formatFileSize(file.length())}"
            )
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun createTelegramIntent(fileUri: Uri, file: File, title: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(file)
            setPackage("org.telegram.messenger")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, "Habit Tracker export: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun createWhatsAppIntent(fileUri: Uri, file: File, title: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(file)
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, "Habit Tracker export: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun createGoogleDriveIntent(fileUri: Uri, file: File, title: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(file)
            setPackage("com.google.android.apps.docs")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun createDropboxIntent(fileUri: Uri, file: File, title: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(file)
            setPackage("com.dropbox.android")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "json" -> "application/json"
            "csv" -> "text/csv"
            else -> "application/octet-stream"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}

/**
 * Information about apps that can handle sharing
 */
data class ShareAppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable
)

/**
 * Predefined share targets for popular apps
 */
enum class ShareTarget {
    EMAIL,
    TELEGRAM,
    WHATSAPP,
    GOOGLE_DRIVE,
    DROPBOX
}
