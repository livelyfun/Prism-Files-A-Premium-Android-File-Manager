package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.repository.FileRepository
import com.example.security.VaultManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class BrowserUiState(
    val currentPath: String = "",
    val breadcrumbs: List<Pair<String, String>> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val isGridView: Boolean = false,
    val sortType: String = "NAME", // NAME, DATE, SIZE, TYPE
    val sortAscending: Boolean = true,
    val selectedCategory: FileCategory? = null,
    val searchQuery: String = "",
    val selectedItems: Set<String> = emptySet(), // FileItem path or id
    val isMultiSelectMode: Boolean = false,
    val lastDeletedItem: FileItem? = null,
    val snackbarMessage: String? = null
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application)
    private val vaultManager = VaultManager(application)

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        val root = repository.getRootPath()
        navigateToPath(root)
    }

    fun navigateToPath(path: String, category: FileCategory? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                currentPath = path,
                selectedCategory = category,
                selectedItems = emptySet(),
                isMultiSelectMode = false
            )

            val breadcrumbs = repository.getBreadcrumbs(path)
            val files = repository.getFiles(
                path = path,
                categoryFilter = category,
                sortType = _uiState.value.sortType,
                sortAscending = _uiState.value.sortAscending
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                breadcrumbs = breadcrumbs,
                files = files
            )
        }
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
    }

    fun setSort(type: String) {
        val newAsc = if (_uiState.value.sortType == type) !_uiState.value.sortAscending else true
        _uiState.value = _uiState.value.copy(sortType = type, sortAscending = newAsc)
        refreshFiles()
    }

    fun setCategoryFilter(category: FileCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        refreshFiles()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                refreshFiles()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val results = repository.searchFiles(query)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    files = results
                )
            }
        }
    }

    fun refreshFiles() {
        val path = _uiState.value.currentPath
        val cat = _uiState.value.selectedCategory
        navigateToPath(path, cat)
    }

    fun toggleSelection(item: FileItem) {
        val currentSelected = _uiState.value.selectedItems.toMutableSet()
        if (currentSelected.contains(item.path)) {
            currentSelected.remove(item.path)
        } else {
            currentSelected.add(item.path)
        }
        val isMulti = currentSelected.isNotEmpty()
        _uiState.value = _uiState.value.copy(
            selectedItems = currentSelected,
            isMultiSelectMode = isMulti
        )
    }

    fun selectAll() {
        val allPaths = _uiState.value.files.map { it.path }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedItems = allPaths,
            isMultiSelectMode = true
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedItems = emptySet(),
            isMultiSelectMode = false
        )
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createFolder(_uiState.value.currentPath, name)
                refreshFiles()
            }
        }
    }

    fun deleteItem(item: FileItem) {
        viewModelScope.launch {
            repository.deleteFile(item)
            _uiState.value = _uiState.value.copy(
                lastDeletedItem = item,
                snackbarMessage = "Deleted ${item.name}"
            )
            refreshFiles()
        }
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val selectedPaths = _uiState.value.selectedItems
            val itemsToDelete = _uiState.value.files.filter { it.path in selectedPaths }
            for (item in itemsToDelete) {
                repository.deleteFile(item)
            }
            _uiState.value = _uiState.value.copy(
                selectedItems = emptySet(),
                isMultiSelectMode = false,
                snackbarMessage = "Deleted ${itemsToDelete.size} items"
            )
            refreshFiles()
        }
    }

    fun hideSelectedToVault(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val selectedPaths = _uiState.value.selectedItems
            val itemsToHide = _uiState.value.files.filter { it.path in selectedPaths && !it.isDirectory }
            var count = 0
            for (item in itemsToHide) {
                val res = vaultManager.encryptAndImportFile(File(item.path))
                if (res.isSuccess) count++
            }
            _uiState.value = _uiState.value.copy(
                selectedItems = emptySet(),
                isMultiSelectMode = false,
                snackbarMessage = "Encrypted & moved $count items to Vault"
            )
            refreshFiles()
            onComplete(count)
        }
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
