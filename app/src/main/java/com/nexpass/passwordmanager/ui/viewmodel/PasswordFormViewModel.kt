package com.nexpass.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.domain.model.AppError
import com.nexpass.passwordmanager.domain.model.Folder
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.repository.FolderRepository
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import com.nexpass.passwordmanager.util.toAppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for password create/edit form.
 *
 * Handles:
 * - Form state management
 * - Validation
 * - Create/Update operations
 * - Password generation integration
 * - Folder selection
 */
class PasswordFormViewModel(
    private val passwordRepository: PasswordRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PasswordFormUiState>(PasswordFormUiState.Idle)
    val uiState: StateFlow<PasswordFormUiState> = _uiState.asStateFlow()

    private val _formData = MutableStateFlow(PasswordFormData())
    val formData: StateFlow<PasswordFormData> = _formData.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    init {
        loadFolders()
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
     * Load existing password for editing.
     */
    fun loadPassword(passwordId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PasswordFormUiState.Loading

                val password = passwordRepository.getById(passwordId)
                if (password != null) {
                    _formData.value = PasswordFormData(
                        id = password.id,
                        title = password.title,
                        username = password.username,
                        password = password.password,
                        url = password.url ?: "",
                        notes = password.notes ?: "",
                        folderId = password.folderId,
                        favorite = password.favorite,
                        isEditMode = true
                    )
                    _uiState.value = PasswordFormUiState.Idle
                } else {
                    _uiState.value = PasswordFormUiState.Error(
                        AppError.Storage.ReadFailed("Password not found")
                    )
                }
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.value = PasswordFormUiState.Error(appError)
            }
        }
    }

    /**
     * Update form field.
     */
    fun updateField(field: FormField, value: String) {
        _formData.value = when (field) {
            FormField.TITLE -> _formData.value.copy(title = value)
            FormField.USERNAME -> _formData.value.copy(username = value)
            FormField.PASSWORD -> _formData.value.copy(password = value)
            FormField.URL -> _formData.value.copy(url = value)
            FormField.NOTES -> _formData.value.copy(notes = value)
        }
    }

    /**
     * Toggle favorite status.
     */
    fun toggleFavorite() {
        _formData.value = _formData.value.copy(favorite = !_formData.value.favorite)
    }

    /**
     * Update selected folder.
     */
    fun updateFolder(folderId: String?) {
        _formData.value = _formData.value.copy(folderId = folderId)
    }

    /**
     * Validate form data.
     */
    private fun validate(): ValidationResult {
        val errors = mutableListOf<AppError.Validation>()

        if (_formData.value.title.isBlank()) {
            errors.add(AppError.Validation.RequiredFieldMissing("Title"))
        }

        if (_formData.value.username.isBlank()) {
            errors.add(AppError.Validation.RequiredFieldMissing("Username"))
        }

        if (_formData.value.password.isBlank()) {
            errors.add(AppError.Validation.RequiredFieldMissing("Password"))
        }

        if (_formData.value.password.length < 8 && _formData.value.password.isNotBlank()) {
            errors.add(AppError.Validation.WeakPassword(
                "Password should be at least 8 characters"
            ))
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Save password (create or update).
     */
    fun savePassword() {
        viewModelScope.launch {
            try {
                val validation = validate()
                if (!validation.isValid) {
                    _uiState.value = PasswordFormUiState.ValidationError(validation.errors)
                    return@launch
                }

                _uiState.value = PasswordFormUiState.Loading

                val data = _formData.value
                val entry = PasswordEntry(
                    id = data.id ?: UUID.randomUUID().toString(),
                    title = data.title,
                    username = data.username,
                    password = data.password,
                    url = data.url.ifBlank { null },
                    notes = data.notes.ifBlank { null },
                    folderId = data.folderId,
                    tags = emptyList(), // TODO: Add tag support in future phase
                    packageNames = emptyList(),
                    favorite = data.favorite,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis(),
                    isQuarantined = false,
                    revisionId = null
                )

                if (data.isEditMode) {
                    passwordRepository.update(entry)
                } else {
                    passwordRepository.insert(entry)
                }

                _uiState.value = PasswordFormUiState.Success
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.value = PasswordFormUiState.Error(appError)
            }
        }
    }

    /**
     * Reset form to initial state.
     */
    fun reset() {
        _formData.value = PasswordFormData()
        _uiState.value = PasswordFormUiState.Idle
    }

    /**
     * Clear error state and retry
     */
    fun clearError() {
        _uiState.value = PasswordFormUiState.Idle
    }
}

/**
 * Form data for password entry.
 */
data class PasswordFormData(
    val id: String? = null,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val folderId: String? = null,
    val favorite: Boolean = false,
    val isEditMode: Boolean = false
)

/**
 * Form fields enum.
 */
enum class FormField {
    TITLE, USERNAME, PASSWORD, URL, NOTES
}

/**
 * Validation result.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<AppError.Validation>
)

/**
 * UI state for password form.
 */
sealed class PasswordFormUiState {
    data object Idle : PasswordFormUiState()
    data object Loading : PasswordFormUiState()
    data object Success : PasswordFormUiState()
    data class ValidationError(val errors: List<AppError.Validation>) : PasswordFormUiState()
    data class Error(val error: AppError) : PasswordFormUiState()
}
