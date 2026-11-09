package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SmartCleanResult
import com.example.model.StorageStats
import com.example.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val storageStats: StorageStats = StorageStats(0, 0, 0),
    val recentFiles: List<FileItem> = emptyList(),
    val smartCleanResult: SmartCleanResult = SmartCleanResult(),
    val isCleaning: Boolean = false,
    val cleanedBytesMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val stats = repository.getStorageStats()
            val recents = repository.getRecentFiles(limit = 12)
            val cleanData = repository.getSmartCleanData()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                storageStats = stats,
                recentFiles = recents,
                smartCleanResult = cleanData
            )
        }
    }

    fun runSmartClean() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true)

            val cleanResult = _uiState.value.smartCleanResult
            val filesToDelete = cleanResult.cacheFiles + cleanResult.temporaryFiles + cleanResult.duplicateFiles
            val reclaimed = repository.cleanCacheAndTempFiles(filesToDelete)

            val message = "Reclaimed ${StorageStats.formatBytes(reclaimed)} of storage space"
            val updatedStats = repository.getStorageStats()
            val updatedClean = repository.getSmartCleanData()

            _uiState.value = _uiState.value.copy(
                isCleaning = false,
                cleanedBytesMessage = message,
                storageStats = updatedStats,
                smartCleanResult = updatedClean
            )
        }
    }

    fun dismissCleanMessage() {
        _uiState.value = _uiState.value.copy(cleanedBytesMessage = null)
    }
}
