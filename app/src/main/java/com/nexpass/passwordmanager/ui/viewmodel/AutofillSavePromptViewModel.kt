package com.nexpass.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.domain.model.AppError
import com.nexpass.passwordmanager.domain.model.Folder
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.model.Tag
import com.nexpass.passwordmanager.domain.repository.FolderRepository
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import com.nexpass.passwordmanager.domain.repository.TagRepository
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.util.toAppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for autofill save prompt dialog.
 *
 * Handles:
 * - Form state management for saving autofilled credentials
 * - Duplicate detection and update logic
 * - Folder and tag selection
 * - Never-save-again functionality
 */
class AutofillSavePromptViewModel(
    private val passwordRepository: PasswordRepository,
    private val folderRepository: FolderRepository,
    private val tagRepository: TagRepository,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<AutofillSavePromptUiState>(
        AutofillSavePromptUiState.Idle
    )
    val uiState: StateFlow<AutofillSavePromptUiState> = _uiState.asStateFlow()

    private val _formData = MutableStateFlow(AutofillSaveFormData())
    val formData: StateFlow<AutofillSaveFormData> = _formData.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _existingEntry = MutableStateFlow<PasswordEntry?>(null)
    val existingEntry: StateFlow<PasswordEntry?> = _existingEntry.asStateFlow()

    init {
        loadFolders()
        loadTags()
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
     * Load available tags.
     */
    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getAllFlow().collect { tags ->
                _tags.value = tags
            }
        }
    }

    /**
     * Initialize the form with autofill data.
     */
    fun initializeWithAutofillData(
        username: String,
        password: String,
        domain: String?,
        packageName: String
    ) {
        viewModelScope.launch {
            try {
                // Check for existing entry
                val existing = findExistingEntry(domain, packageName)
                _existingEntry.value = existing

                // Set form data
                val defaultFolder = securePreferences.getAutosaveDefaultFolder()
                _formData.value = AutofillSaveFormData(
                    title = domain ?: packageName,
                    username = username,
                    password = password,
                    url = domain?.let { "https://$it" } ?: "",
                    packageName = packageName,
                    webDomain = domain,
                    folderId = existing?.folderId ?: defaultFolder,
                    selectedTagIds = existing?.tags ?: emptyList(),
                    neverSaveAgain = false,
                    isUpdate = existing != null
                )
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.value = AutofillSavePromptUiState.Error(appError)
            }
        }
    }

    /**
     * Find existing password entry for the same domain/package.
     */
    private suspend fun findExistingEntry(domain: String?, packageName: String): PasswordEntry? {
        val allEntries = passwordRepository.getAll()

        return allEntries.firstOrNull { entry ->
            // Check domain match
            val domainMatches = domain != null &&
                (entry.url?.contains(domain, ignoreCase = true) == true)

            // Check package name match
            val packageMatches = entry.packageNames.contains(packageName)

            domainMatches || packageMatches
        }
    }

    /**
     * Update form field.
     */
    fun updateField(field: AutofillSaveFormField, value: String) {
        _formData.value = when (field) {
            AutofillSaveFormField.TITLE -> _formData.value.copy(title = value)
            AutofillSaveFormField.USERNAME -> _formData.value.copy(username = value)
            AutofillSaveFormField.PASSWORD -> _formData.value.copy(password = value)
            AutofillSaveFormField.URL -> _formData.value.copy(url = value)
            AutofillSaveFormField.NOTES -> _formData.value.copy(notes = value)
        }
    }

    /**
     * Update selected folder.
     */
    fun updateFolder(folderId: String?) {
        _formData.value = _formData.value.copy(folderId = folderId)
    }

    /**
     * Toggle tag selection.
     */
    fun toggleTag(tagId: String) {
        val currentTags = _formData.value.selectedTagIds.toMutableList()
        if (currentTags.contains(tagId)) {
            currentTags.remove(tagId)
        } else {
            currentTags.add(tagId)
        }
        _formData.value = _formData.value.copy(selectedTagIds = currentTags)
    }

    /**
     * Toggle never save again checkbox.
     */
    fun toggleNeverSaveAgain() {
        _formData.value = _formData.value.copy(
            neverSaveAgain = !_formData.value.neverSaveAgain
        )
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

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Save the password entry.
     */
    fun savePassword() {
        viewModelScope.launch {
            try {
                val validation = validate()
                if (!validation.isValid) {
                    _uiState.value = AutofillSavePromptUiState.ValidationError(validation.errors)
                    return@launch
                }

                _uiState.value = AutofillSavePromptUiState.Saving

                val data = _formData.value

                // Handle never-save-again
                if (data.neverSaveAgain) {
                    val identifier = data.webDomain ?: data.packageName
                    securePreferences.addNeverSaveDomain(identifier)
                    _uiState.value = AutofillSavePromptUiState.Cancelled
                    return@launch
                }

                // Create or update password entry
                val entry = PasswordEntry(
                    id = _existingEntry.value?.id ?: UUID.randomUUID().toString(),
                    title = data.title,
                    username = data.username,
                    password = data.password,
                    url = data.url.ifBlank { null },
                    notes = data.notes.ifBlank { null },
                    folderId = data.folderId,
                    tags = data.selectedTagIds,
                    packageNames = listOf(data.packageName),
                    favorite = _existingEntry.value?.favorite ?: false,
                    createdAt = _existingEntry.value?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis(),
                    isQuarantined = false,
                    revisionId = _existingEntry.value?.revisionId
                )

                if (data.isUpdate) {
                    passwordRepository.update(entry)
                    _uiState.value = AutofillSavePromptUiState.SavedUpdate
                } else {
                    passwordRepository.insert(entry)
                    _uiState.value = AutofillSavePromptUiState.SavedNew
                }
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.value = AutofillSavePromptUiState.Error(appError)
            }
        }
    }

    /**
     * Cancel the save operation.
     */
    fun cancel() {
        _uiState.value = AutofillSavePromptUiState.Cancelled
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.value = AutofillSavePromptUiState.Idle
    }
}

/**
 * Form data for autofill save.
 */
data class AutofillSaveFormData(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val packageName: String = "",
    val webDomain: String? = null,
    val folderId: String? = null,
    val selectedTagIds: List<String> = emptyList(),
    val neverSaveAgain: Boolean = false,
    val isUpdate: Boolean = false
)

/**
 * Form fields enum.
 */
enum class AutofillSaveFormField {
    TITLE, USERNAME, PASSWORD, URL, NOTES
}

/**
 * UI state for autofill save prompt.
 */
sealed class AutofillSavePromptUiState {
    data object Idle : AutofillSavePromptUiState()
    data object Saving : AutofillSavePromptUiState()
    data object SavedNew : AutofillSavePromptUiState()
    data object SavedUpdate : AutofillSavePromptUiState()
    data object Cancelled : AutofillSavePromptUiState()
    data class ValidationError(val errors: List<AppError.Validation>) : AutofillSavePromptUiState()
    data class Error(val error: AppError) : AutofillSavePromptUiState()
}
