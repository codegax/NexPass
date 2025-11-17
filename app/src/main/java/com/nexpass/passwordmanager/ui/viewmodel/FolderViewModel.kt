package com.nexpass.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.domain.model.Folder
import com.nexpass.passwordmanager.domain.model.SyncOperationType
import com.nexpass.passwordmanager.domain.repository.FolderRepository
import com.nexpass.passwordmanager.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for folder management.
 */
class FolderViewModel(
    private val folderRepository: FolderRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
    }

    /**
     * Load all folders.
     */
    private fun loadFolders() {
        viewModelScope.launch {
            folderRepository.getAllFlow().collect { folders ->
                _uiState.value = _uiState.value.copy(
                    folders = folders,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /**
     * Create a new folder.
     */
    fun createFolder(name: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Folder name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val folder = Folder(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    parentId = null, // Simple folders for now, no hierarchy
                    createdAt = now,
                    updatedAt = now,
                    lastModified = now
                )
                folderRepository.insert(folder)

                // Queue folder for sync with Nextcloud
                syncRepository.queueFolderChange(folder, SyncOperationType.CREATE)

                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create folder: ${e.message}")
            }
        }
    }

    /**
     * Update an existing folder.
     */
    fun updateFolder(folderId: String, newName: String) {
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Folder name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val folder = folderRepository.getById(folderId)
                if (folder != null) {
                    val updatedFolder = folder.copy(
                        name = newName.trim(),
                        updatedAt = System.currentTimeMillis(),
                        lastModified = System.currentTimeMillis()
                    )
                    folderRepository.update(updatedFolder)

                    // Queue folder for sync with Nextcloud
                    syncRepository.queueFolderChange(updatedFolder, SyncOperationType.UPDATE)

                    _uiState.value = _uiState.value.copy(error = null)
                } else {
                    _uiState.value = _uiState.value.copy(error = "Folder not found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update folder: ${e.message}")
            }
        }
    }

    /**
     * Delete a folder.
     */
    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            try {
                // Get folder before deleting to queue for sync
                val folder = folderRepository.getById(folderId)

                folderRepository.delete(folderId)

                // Queue folder deletion for sync with Nextcloud
                folder?.let {
                    syncRepository.queueFolderChange(it, SyncOperationType.DELETE)
                }

                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete folder: ${e.message}")
            }
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for folder management.
 */
data class FolderUiState(
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
