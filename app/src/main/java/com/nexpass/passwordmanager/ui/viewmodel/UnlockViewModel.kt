package com.nexpass.passwordmanager.ui.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.security.biometric.BiometricManager
import com.nexpass.passwordmanager.security.vault.VaultKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the unlock screen.
 *
 * Handles:
 * - Master password unlock
 * - Biometric unlock
 * - Auto-lock timer
 * - Failed attempt tracking
 */
class UnlockViewModel(
    private val vaultKeyManager: VaultKeyManager,
    private val securePreferences: SecurePreferences,
    private val biometricManager: BiometricManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UnlockUiState>(UnlockUiState.Idle())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private var failedAttempts = 0

    init {
        checkBiometricAvailability()
    }

    /**
     * Check if biometric authentication is available.
     */
    private fun checkBiometricAvailability() {
        val isBiometricEnabled = securePreferences.isBiometricEnabled()
        val biometricAvailability = biometricManager.isBiometricAvailable()
        val canUseBiometric = biometricAvailability is com.nexpass.passwordmanager.security.biometric.BiometricAvailability.Available

        _uiState.value = UnlockUiState.Idle(
            isBiometricAvailable = isBiometricEnabled && canUseBiometric
        )
    }

    /**
     * Unlock the vault with master password.
     */
    fun unlockWithPassword(password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UnlockUiState.Loading

                // Get stored salt
                val saltBase64 = securePreferences.getMasterSalt()
                    ?: throw IllegalStateException("Vault not initialized")

                val salt = Base64.decode(saltBase64, Base64.NO_WRAP)

                // Try to unlock
                vaultKeyManager.unlockWithPassword(password, salt)

                // Reset failed attempts on success
                failedAttempts = 0

                _uiState.value = UnlockUiState.Success
            } catch (e: Exception) {
                failedAttempts++
                _uiState.value = UnlockUiState.Error(
                    message = "Incorrect password",
                    attempts = failedAttempts
                )
            }
        }
    }

    /**
     * Unlock the vault with biometric authentication.
     */
    fun unlockWithBiometric() {
        viewModelScope.launch {
            try {
                _uiState.value = UnlockUiState.Loading

                // Get encrypted vault key
                val encryptedKeyBase64 = securePreferences.getEncryptedVaultKey()
                    ?: throw IllegalStateException("Biometric unlock not configured")

                val encryptedKey = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)

                // Unlock with biometric (this will trigger biometric prompt via BiometricManager)
                vaultKeyManager.unlockWithBiometric(encryptedKey)

                _uiState.value = UnlockUiState.Success
            } catch (e: Exception) {
                _uiState.value = UnlockUiState.Error(
                    message = e.message ?: "Biometric unlock failed",
                    attempts = 0
                )
            }
        }
    }

    /**
     * Reset error state.
     */
    fun resetError() {
        checkBiometricAvailability()
    }
}

/**
 * UI state for unlock screen.
 */
sealed class UnlockUiState {
    data class Idle(val isBiometricAvailable: Boolean = false) : UnlockUiState()
    data object Loading : UnlockUiState()
    data object Success : UnlockUiState()
    data class Error(
        val message: String,
        val attempts: Int = 0
    ) : UnlockUiState()
}
