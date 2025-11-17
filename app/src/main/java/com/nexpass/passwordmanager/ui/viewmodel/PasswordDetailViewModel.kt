package com.nexpass.passwordmanager.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for password detail screen.
 *
 * Handles:
 * - Loading password details
 * - Copy to clipboard
 * - Delete password
 * - Navigation to edit
 */
class PasswordDetailViewModel(
    private val passwordRepository: PasswordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PasswordDetailUiState>(PasswordDetailUiState.Loading)
    val uiState: StateFlow<PasswordDetailUiState> = _uiState.asStateFlow()

    /**
     * Load password by ID.
     */
    fun loadPassword(passwordId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PasswordDetailUiState.Loading

                val password = passwordRepository.getById(passwordId)

                _uiState.value = if (password != null) {
                    PasswordDetailUiState.Success(password)
                } else {
                    PasswordDetailUiState.Error("Password not found")
                }
            } catch (e: Exception) {
                _uiState.value = PasswordDetailUiState.Error(
                    e.message ?: "Failed to load password"
                )
            }
        }
    }

    /**
     * Copy password to clipboard.
     */
    fun copyPassword(context: Context, password: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("password", password)
        clipboard.setPrimaryClip(clip)

        // TODO: Clear clipboard after timeout (security feature for Phase 7)
    }

    /**
     * Copy username to clipboard.
     */
    fun copyUsername(context: Context, username: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("username", username)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Delete the current password.
     */
    fun deletePassword(passwordId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                passwordRepository.delete(passwordId)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = PasswordDetailUiState.Error(
                    e.message ?: "Failed to delete password"
                )
            }
        }
    }

    /**
     * Toggle favorite status.
     */
    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is PasswordDetailUiState.Success) {
                    val updated = currentState.password.copy(
                        favorite = !currentState.password.favorite
                    )
                    passwordRepository.update(updated)
                    _uiState.value = PasswordDetailUiState.Success(updated)
                }
            } catch (e: Exception) {
                _uiState.value = PasswordDetailUiState.Error(
                    e.message ?: "Failed to update password"
                )
            }
        }
    }
}

/**
 * UI state for password detail screen.
 */
sealed class PasswordDetailUiState {
    data object Loading : PasswordDetailUiState()
    data class Success(val password: PasswordEntry) : PasswordDetailUiState()
    data class Error(val message: String) : PasswordDetailUiState()
}
