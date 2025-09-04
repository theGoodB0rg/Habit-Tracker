package com.habittracker.export.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.export.domain.model.*
import com.habittracker.export.domain.usecase.ExportHabitsUseCase
import com.habittracker.export.domain.usecase.ExportFileManagerUseCase
import com.habittracker.export.domain.usecase.ExportFileInfo
import com.habittracker.export.domain.usecase.Result
import com.habittracker.export.domain.usecase.ShareExportUseCase
import com.habittracker.export.domain.usecase.ShareTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the export screen
 * Manages export configuration, progress, and results
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportHabitsUseCase: ExportHabitsUseCase,
    private val shareExportUseCase: ShareExportUseCase,
    private val fileManagerUseCase: ExportFileManagerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _exportProgress = MutableStateFlow<ExportProgress?>(null)
    val exportProgress: StateFlow<ExportProgress?> = _exportProgress.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    private val _shareIntent = MutableSharedFlow<android.content.Intent>()
    val shareIntent: SharedFlow<android.content.Intent> = _shareIntent.asSharedFlow()

    fun updateFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun updateScope(scope: ExportScope) {
        _uiState.update { it.copy(selectedScope = scope) }
        
        // Reset scope-specific configurations
        when (scope) {
            ExportScope.SPECIFIC_HABIT -> {
                _uiState.update { it.copy(selectedHabitIds = emptyList()) }
            }
            ExportScope.DATE_RANGE -> {
                _uiState.update { 
                    it.copy(
                        startDate = LocalDate.now().minusMonths(1),
                        endDate = LocalDate.now()
                    ) 
                }
            }
            else -> {
                _uiState.update { 
                    it.copy(
                        selectedHabitIds = emptyList(),
                        startDate = null,
                        endDate = null
                    ) 
                }
            }
        }
    }

    fun updateIncludeCompletions(include: Boolean) {
        _uiState.update { it.copy(includeCompletions = include) }
    }

    fun updateIncludeStreakHistory(include: Boolean) {
        _uiState.update { it.copy(includeStreakHistory = include) }
    }

    fun updateIncludeMetadata(include: Boolean) {
        _uiState.update { it.copy(includeMetadata = include) }
    }

    fun updateCustomFileName(fileName: String) {
        _uiState.update { it.copy(customFileName = fileName) }
    }

    fun updateSelectedHabits(habitIds: List<Long>) {
        _uiState.update { it.copy(selectedHabitIds = habitIds) }
    }

    fun updateDateRange(startDate: LocalDate?, endDate: LocalDate?) {
        _uiState.update { it.copy(startDate = startDate, endDate = endDate) }
    }

    fun getExportPreview() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingPreview = true) }
                
                val config = createExportConfig()
                val preview = exportHabitsUseCase.getExportPreview(config)
                
                _uiState.update { 
                    it.copy(
                        exportPreview = preview,
                        isLoadingPreview = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingPreview = false,
                        errorMessage = "Failed to load preview: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun startExport() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, errorMessage = null) }
                _exportProgress.value = null
                _exportResult.value = null

                val config = createExportConfig()
                
                exportHabitsUseCase.exportHabits(config).collect { result ->
                    when (result) {
                        is Result.Progress -> {
                            _exportProgress.value = result.progress
                        }
                        is Result.Success -> {
                            _exportResult.value = result.data
                            _uiState.update { it.copy(isExporting = false) }
                            
                            // If export was successful, update last export info
                            if (result.data is ExportResult.Success) {
                                _uiState.update { 
                                    it.copy(lastExportResult = result.data) 
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.message}"
                    ) 
                }
                _exportProgress.value = null
            }
        }
    }

    fun shareLastExport() {
        val lastResult = _uiState.value.lastExportResult
        if (lastResult != null) {
            shareExport(lastResult.filePath)
        }
    }

    fun shareExport(filePath: String) {
        viewModelScope.launch {
            try {
                val intent = shareExportUseCase.shareFile(filePath)
                _shareIntent.emit(intent)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to share file: ${e.message}") 
                }
            }
        }
    }

    fun shareToApp(filePath: String, target: ShareTarget) {
        viewModelScope.launch {
            try {
                val intent = shareExportUseCase.shareToSpecificApp(filePath, target)
                _shareIntent.emit(intent)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to share to ${target.name}: ${e.message}") 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearExportResult() {
        _exportResult.value = null
        _exportProgress.value = null
    }
    
    fun getExportDirectory() {
        viewModelScope.launch {
            try {
                val directory = fileManagerUseCase.getExportDirectory()
                _uiState.update { 
                    it.copy(exportDirectory = directory.absolutePath) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to get export directory: ${e.message}") 
                }
            }
        }
    }
    
    fun loadAvailableExports() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingFiles = true) }
                val files = fileManagerUseCase.getExportedFiles()
                _uiState.update { 
                    it.copy(
                        availableExports = files,
                        isLoadingFiles = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingFiles = false,
                        errorMessage = "Failed to load export files: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun deleteExportFile(filePath: String) {
        viewModelScope.launch {
            try {
                fileManagerUseCase.deleteExportFile(filePath)
                // Refresh the file list
                loadAvailableExports()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to delete file: ${e.message}") 
                }
            }
        }
    }
    
    fun shareFile(filePath: String) {
        viewModelScope.launch {
            try {
                val intent = shareExportUseCase.shareFile(filePath)
                _shareIntent.emit(intent)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to share file: ${e.message}") 
                }
            }
        }
    }

    private fun createExportConfig(): ExportConfig {
        val state = _uiState.value
        return ExportConfig(
            format = state.selectedFormat,
            scope = state.selectedScope,
            includeCompletions = state.includeCompletions,
            includeStreakHistory = state.includeStreakHistory,
            includeMetadata = state.includeMetadata,
            habitIds = state.selectedHabitIds,
            startDate = state.startDate?.toString(),
            endDate = state.endDate?.toString(),
            fileName = state.customFileName.takeIf { it.isNotBlank() }
        )
    }
}

/**
 * UI state for the export screen
 */
data class ExportUiState(
    val selectedFormat: ExportFormat = ExportFormat.JSON,
    val selectedScope: ExportScope = ExportScope.ALL_HABITS,
    val includeCompletions: Boolean = true,
    val includeStreakHistory: Boolean = true,
    val includeMetadata: Boolean = true,
    val customFileName: String = "",
    val selectedHabitIds: List<Long> = emptyList(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isExporting: Boolean = false,
    val isLoadingPreview: Boolean = false,
    val exportPreview: com.habittracker.export.domain.usecase.ExportPreview? = null,
    val lastExportResult: ExportResult.Success? = null,
    val errorMessage: String? = null,
    val exportDirectory: String? = null,
    val availableExports: List<ExportFileInfo> = emptyList(),
    val isLoadingFiles: Boolean = false
)
