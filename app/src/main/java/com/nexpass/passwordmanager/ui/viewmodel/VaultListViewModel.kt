package com.nexpass.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.domain.model.AppError
import com.nexpass.passwordmanager.domain.model.Folder
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.repository.FolderRepository
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import com.nexpass.passwordmanager.security.vault.VaultKeyManager
import com.nexpass.passwordmanager.util.toAppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the vault list screen.
 *
 * Handles:
 * - Loading password entries
 * - Search functionality
 * - Delete operations
 * - Favorite toggles
 * - Folder filtering
 */
class VaultListViewModel(
    private val passwordRepository: PasswordRepository,
    private val vaultKeyManager: VaultKeyManager,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VaultListUiState>(VaultListUiState.Loading)
    val uiState: StateFlow<VaultListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    init {
        loadFolders()
        loadPasswords()
    }

    /**
     * Load available folders.
     */
    private fun loadFolders() {
        viewModelScope.launch {
            folderRepository.getAllFlow().collect { folders ->
                _folders.value = folders
            }
        }
    }

    /**
     * Load all passwords from repository.
     */
    fun loadPasswords() {
        viewModelScope.launch {
            try {
                if (!vaultKeyManager.isUnlocked()) {
                    _uiState.value = VaultListUiState.Locked
                    return@launch
                }

                _uiState.value = VaultListUiState.Loading

                passwordRepository.getAllFlow()
                    .catch { e ->
                        val appError = e.toAppError()
                        _uiState.value = VaultListUiState.Error(appError)
                    }
                    .collect { passwords ->
                        var filteredPasswords = passwords

                        // Apply folder filter
                        if (_selectedFolderId.value != null) {
                            filteredPasswords = filteredPasswords.filter { it.folderId == _selectedFolderId.value }
                        }

                        // Apply favorites filter
                        if (_showFavoritesOnly.value) {
                            filteredPasswords = filteredPasswords.filter { it.favorite }
                        }

                        _uiState.value = if (filteredPasswords.isEmpty()) {
                            if (_showFavoritesOnly.value) {
                                VaultListUiState.EmptyFavorites
                            } else {
                                VaultListUiState.Empty
                            }
                        } else {
                            VaultListUiState.Success(filteredPasswords)
                        }
                    }
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.value = VaultListUiState.Error(appError)
            }
        }
    }

    /**
     * Search passwords by query.
     */
    fun search(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            loadPasswords()
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = VaultListUiState.Loading

                passwordRepository.search(query)
                    .catch { e ->
                        val appError = e.toAppError()
                        _uiState.value = VaultListUiState.Error(appError)
                    }
                    .collect { passwords ->
                        var filteredPasswords = passwords

                        // Apply folder filter
                        if (_selectedFolderId.value != null) {
                            filteredPasswords = filteredPasswords.filter { it.folderId == _selectedFolderId.value }
                        }

                        // Apply favorites filter
                        if (_showFavoritesOnly.value) {
                            filteredPasswords = filteredPasswords.filter { it.favorite }
                        }

                        _uiState.value = if (filteredPasswords.isEmpty()) {
                            if (_showFavoritesOnly.value && passwords.isNotEmpty()) {
                                VaultListUiState.EmptyFavorites
                            } else {
                                VaultListUiState.EmptySearch(query)
                            }
                        } else {
                            VaultListUiState.Success(filteredPasswords)
                        }
                    }
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.value = VaultListUiState.Error(appError)
            }
        }
    }

    /**
     * Delete a password entry.
     */
    fun deletePassword(id: String) {
        viewModelScope.launch {
            try {
                passwordRepository.delete(id)
                // List will auto-update via Flow
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.value = VaultListUiState.Error(appError)
            }
        }
    }

    /**
     * Toggle favorite status of a password.
     */
    fun toggleFavorite(entry: PasswordEntry) {
        viewModelScope.launch {
            try {
                val updated = entry.copy(favorite = !entry.favorite)
                passwordRepository.update(updated)
                // List will auto-update via Flow
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.value = VaultListUiState.Error(appError)
            }
        }
    }

    /**
     * Retry after error
     */
    fun retry() {
        loadPasswords()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        if (_uiState.value is VaultListUiState.Error) {
            loadPasswords()
        }
    }

    /**
     * Lock the vault.
     */
    fun lockVault() {
        viewModelScope.launch {
            vaultKeyManager.lock()
            _uiState.value = VaultListUiState.Locked
        }
    }

    /**
     * Toggle favorites filter.
     */
    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
        // Reload to apply filter
        if (_searchQuery.value.isBlank()) {
            loadPasswords()
        } else {
            search(_searchQuery.value)
        }
    }

    /**
     * Set folder filter.
     */
    fun setFolderFilter(folderId: String?) {
        _selectedFolderId.value = folderId
        // Reload to apply filter
        if (_searchQuery.value.isBlank()) {
            loadPasswords()
        } else {
            search(_searchQuery.value)
        }
    }

    /**
     * Clear folder filter.
     */
    fun clearFolderFilter() {
        setFolderFilter(null)
    }
}

/**
 * UI state for vault list screen.
 */
sealed class VaultListUiState {
    data object Loading : VaultListUiState()
    data object Empty : VaultListUiState()
    data object EmptyFavorites : VaultListUiState()
    data class EmptySearch(val query: String) : VaultListUiState()
    data object Locked : VaultListUiState()
    data class Success(val passwords: List<PasswordEntry>) : VaultListUiState()
    data class Error(val error: AppError) : VaultListUiState()
}
