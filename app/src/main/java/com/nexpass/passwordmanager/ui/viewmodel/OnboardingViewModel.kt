package com.nexpass.passwordmanager.ui.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.security.encryption.CryptoManager
import com.nexpass.passwordmanager.security.vault.VaultKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the onboarding flow.
 *
 * Handles:
 * - Master password creation
 * - Password validation
 * - Vault initialization
 * - Salt generation and storage
 */
class OnboardingViewModel(
    private val cryptoManager: CryptoManager,
    private val vaultKeyManager: VaultKeyManager,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Initialize the vault with the master password.
     *
     * This:
     * 1. Generates a random salt
     * 2. Derives the vault key from the master password
     * 3. Stores the salt securely
     * 4. Unlocks the vault in VaultKeyManager
     * 5. Marks vault as initialized
     */
    fun initializeVault(masterPassword: String) {
        viewModelScope.launch {
            try {
                _uiState.value = OnboardingUiState.Loading

                // Generate a random salt for key derivation
                val salt = cryptoManager.generateSalt()

                // Store salt (encoded as Base64 string)
                val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
                securePreferences.setMasterSalt(saltBase64)

                // Derive vault key and unlock
                vaultKeyManager.unlockWithPassword(masterPassword, salt)

                // Mark vault as initialized
                securePreferences.setVaultInitialized(true)

                _uiState.value = OnboardingUiState.Success
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to initialize vault")
            }
        }
    }

    /**
     * Validate master password strength.
     */
    fun validatePassword(password: String): PasswordValidationResult {
        val errors = mutableListOf<String>()

        if (password.length < 8) {
            errors.add("Password must be at least 8 characters")
        }
        if (password.length < 12) {
            errors.add("Consider using at least 12 characters for better security")
        }
        if (!password.any { it.isUpperCase() }) {
            errors.add("Include at least one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            errors.add("Include at least one lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            errors.add("Include at least one number")
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            errors.add("Include at least one special character")
        }

        return PasswordValidationResult(
            isValid = password.length >= 8,
            errors = errors
        )
    }
}

/**
 * UI state for onboarding flow.
 */
sealed class OnboardingUiState {
    data object Idle : OnboardingUiState()
    data object Loading : OnboardingUiState()
    data object Success : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

/**
 * Result of password validation.
 */
data class PasswordValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
