package com.nexpass.passwordmanager.ui.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.domain.model.SyncEngineStatus
import com.nexpass.passwordmanager.domain.model.ThemeMode
import com.nexpass.passwordmanager.domain.repository.SyncRepository
import com.nexpass.passwordmanager.domain.usecase.ExportVaultUseCase
import com.nexpass.passwordmanager.domain.usecase.ImportVaultUseCase
import com.nexpass.passwordmanager.security.biometric.BiometricManager
import com.nexpass.passwordmanager.security.vault.VaultKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

/**
 * ViewModel for settings screen.
 *
 * Handles:
 * - Biometric unlock toggle
 * - Auto-lock timeout
 * - Theme selection
 * - Clear vault data
 */
class SettingsViewModel(
    private val securePreferences: SecurePreferences,
    private val vaultKeyManager: VaultKeyManager,
    private val biometricManager: BiometricManager,
    private val syncRepository: SyncRepository,
    private val exportVaultUseCase: ExportVaultUseCase,
    private val importVaultUseCase: ImportVaultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeSyncStatus()
    }

    /**
     * Load current settings.
     */
    private fun loadSettings() {
        val biometricAvailability = biometricManager.isBiometricAvailable()
        val isBiometricAvailable = biometricAvailability is com.nexpass.passwordmanager.security.biometric.BiometricAvailability.Available
        val isBiometricEnabled = securePreferences.isBiometricEnabled()
        val nextcloudServerUrl = securePreferences.getNextcloudServerUrl() ?: ""
        val nextcloudUsername = securePreferences.getNextcloudUsername() ?: ""
        val syncEnabled = securePreferences.isSyncEnabled()
        val lastSyncTimestamp = securePreferences.getLastSyncTimestamp()
        val themeMode = ThemeMode.fromString(securePreferences.getThemeMode())
        val autoLockTimeout = securePreferences.getAutoLockTimeout()

        _uiState.value = SettingsUiState(
            isBiometricAvailable = isBiometricAvailable,
            isBiometricEnabled = isBiometricEnabled,
            themeMode = themeMode,
            autoLockTimeout = autoLockTimeout,
            nextcloudServerUrl = nextcloudServerUrl,
            nextcloudUsername = nextcloudUsername,
            nextcloudConfigured = securePreferences.isNextcloudConfigured(),
            syncEnabled = syncEnabled,
            lastSyncTimestamp = if (lastSyncTimestamp > 0) lastSyncTimestamp else null
        )
    }

    /**
     * Observe sync status changes.
     */
    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncRepository.observeSyncStatus().collect { status ->
                _uiState.value = _uiState.value.copy(syncStatus = status)
            }
        }
    }

    /**
     * Toggle biometric unlock.
     */
    fun toggleBiometric(enabled: Boolean, onBiometricPrompt: () -> Unit) {
        viewModelScope.launch {
            try {
                if (!vaultKeyManager.isUnlocked()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Vault must be unlocked to enable biometric"
                    )
                    return@launch
                }

                if (enabled) {
                    // Enable biometric
                    onBiometricPrompt() // Trigger biometric prompt in UI

                    val encryptedVaultKey = vaultKeyManager.enableBiometric()
                    val encryptedKeyBase64 = Base64.encodeToString(encryptedVaultKey, Base64.NO_WRAP)

                    securePreferences.setEncryptedVaultKey(encryptedKeyBase64)
                    securePreferences.setBiometricEnabled(true)

                    _uiState.value = _uiState.value.copy(
                        isBiometricEnabled = true,
                        error = null
                    )
                } else {
                    // Disable biometric
                    vaultKeyManager.disableBiometric()
                    securePreferences.setBiometricEnabled(false)
                    securePreferences.setEncryptedVaultKey("")

                    _uiState.value = _uiState.value.copy(
                        isBiometricEnabled = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to toggle biometric"
                )
            }
        }
    }

    /**
     * Clear all vault data (factory reset).
     */
    fun clearVaultData(onCleared: () -> Unit) {
        viewModelScope.launch {
            try {
                // Lock vault first
                vaultKeyManager.lock()

                // Clear all preferences
                securePreferences.clear()

                // TODO: Clear database in Phase 7

                onCleared()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to clear vault data"
                )
            }
        }
    }

    /**
     * Lock the vault immediately.
     */
    fun lockVault(onLocked: () -> Unit) {
        viewModelScope.launch {
            vaultKeyManager.lock()
            onLocked()
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Update theme mode.
     */
    fun updateThemeMode(mode: ThemeMode) {
        securePreferences.setThemeMode(mode.toString())
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    /**
     * Update auto-lock timeout.
     */
    fun updateAutoLockTimeout(minutes: Int) {
        securePreferences.setAutoLockTimeout(minutes)
        _uiState.value = _uiState.value.copy(autoLockTimeout = minutes)
    }

    // ========== Export/Import Methods ==========

    /**
     * Export vault to encrypted file.
     */
    fun exportVault(
        outputStream: OutputStream,
        exportPassword: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val count = exportVaultUseCase.execute(outputStream, exportPassword)
                onComplete(true, "Successfully exported $count passwords")
            } catch (e: Exception) {
                onComplete(false, "Export failed: ${e.message}")
            }
        }
    }

    /**
     * Import vault from encrypted file.
     */
    fun importVault(
        inputStream: InputStream,
        importPassword: String,
        replaceExisting: Boolean = false,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val count = importVaultUseCase.execute(inputStream, importPassword, replaceExisting)
                onComplete(true, "Successfully imported $count passwords")
            } catch (e: Exception) {
                onComplete(false, "Import failed: ${e.message}")
            }
        }
    }

    // ========== Nextcloud Sync Methods ==========

    /**
     * Configure Nextcloud sync settings.
     */
    fun configureNextcloud(serverUrl: String, username: String, appPassword: String) {
        viewModelScope.launch {
            try {
                // Ensure URL is properly formatted
                val formattedUrl = serverUrl.trim().let {
                    if (!it.startsWith("http://") && !it.startsWith("https://")) {
                        "https://$it"
                    } else {
                        it
                    }
                }.removeSuffix("/")

                // Save configuration
                securePreferences.setNextcloudServerUrl(formattedUrl)
                securePreferences.setNextcloudUsername(username.trim())
                securePreferences.setNextcloudAppPassword(appPassword.trim())
                securePreferences.setSyncEnabled(true)

                _uiState.value = _uiState.value.copy(
                    nextcloudServerUrl = formattedUrl,
                    nextcloudUsername = username.trim(),
                    nextcloudConfigured = true,
                    syncEnabled = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to configure Nextcloud"
                )
            }
        }
    }

    /**
     * Test Nextcloud connection.
     */
    fun testNextcloudConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = syncRepository.testConnection()
                if (result.isSuccess && result.getOrNull() == true) {
                    onResult(true, "Connection successful!")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Connection failed"
                    onResult(false, error)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Connection failed")
            }
        }
    }

    /**
     * Manually trigger sync.
     */
    fun triggerSync() {
        viewModelScope.launch {
            try {
                val result = syncRepository.performFullSync()
                when (result) {
                    is com.nexpass.passwordmanager.domain.model.SyncResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            lastSyncTimestamp = System.currentTimeMillis(),
                            error = null
                        )
                    }
                    is com.nexpass.passwordmanager.domain.model.SyncResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Sync failed: ${result.message}"
                        )
                    }
                    else -> {
                        // InProgress or Idle - handled by observeSyncStatus
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Sync failed"
                )
            }
        }
    }

    /**
     * Toggle sync enabled/disabled.
     */
    fun toggleSync(enabled: Boolean) {
        securePreferences.setSyncEnabled(enabled)
        _uiState.value = _uiState.value.copy(syncEnabled = enabled)
    }

    /**
     * Clear Nextcloud configuration.
     */
    fun clearNextcloudConfig() {
        viewModelScope.launch {
            try {
                syncRepository.clearSyncData()
                _uiState.value = _uiState.value.copy(
                    nextcloudServerUrl = "",
                    nextcloudUsername = "",
                    nextcloudConfigured = false,
                    syncEnabled = false,
                    lastSyncTimestamp = null,
                    syncStatus = SyncEngineStatus.NotConfigured,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to clear Nextcloud config"
                )
            }
        }
    }
}

/**
 * UI state for settings screen.
 */
data class SettingsUiState(
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoLockTimeout: Int = 5, // minutes (-1 for never)
    val nextcloudServerUrl: String = "",
    val nextcloudUsername: String = "",
    val nextcloudConfigured: Boolean = false,
    val syncEnabled: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val syncStatus: SyncEngineStatus = SyncEngineStatus.NotConfigured,
    val error: String? = null
)
