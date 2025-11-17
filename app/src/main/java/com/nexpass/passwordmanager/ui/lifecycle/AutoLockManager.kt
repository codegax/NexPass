package com.nexpass.passwordmanager.ui.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.security.vault.VaultKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages auto-lock functionality for the vault.
 *
 * Monitors app lifecycle and locks vault after configured timeout period
 * when app goes to background.
 */
class AutoLockManager(
    private val vaultKeyManager: VaultKeyManager,
    private val securePreferences: SecurePreferences
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var backgroundTimestamp: Long = 0
    private var isInBackground = false

    private val _shouldNavigateToUnlock = MutableStateFlow(false)
    val shouldNavigateToUnlock: StateFlow<Boolean> = _shouldNavigateToUnlock.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App moved to background
        onAppBackground()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App moved to foreground
        onAppForeground()
    }

    /**
     * Called when app moves to background.
     */
    private fun onAppBackground() {
        isInBackground = true
        backgroundTimestamp = System.currentTimeMillis()
    }

    /**
     * Called when app moves to foreground.
     */
    private fun onAppForeground() {
        if (!isInBackground) return

        isInBackground = false

        // Check if vault should be locked based on timeout
        val timeoutMinutes = securePreferences.getAutoLockTimeout()

        // Never = -1, don't lock
        if (timeoutMinutes == -1) {
            return
        }

        val timeInBackground = System.currentTimeMillis() - backgroundTimestamp
        val timeoutMillis = timeoutMinutes * 60 * 1000L

        if (timeInBackground >= timeoutMillis && vaultKeyManager.isUnlocked()) {
            lockVault()
        }
    }

    /**
     * Manually lock the vault (called from settings).
     */
    fun lockVault() {
        scope.launch {
            vaultKeyManager.lock()
            _shouldNavigateToUnlock.value = true
        }
    }

    /**
     * Reset navigation flag after navigating.
     */
    fun resetNavigationFlag() {
        _shouldNavigateToUnlock.value = false
    }

    /**
     * Update last activity time (called on user interaction).
     */
    fun updateActivity() {
        if (!isInBackground) {
            backgroundTimestamp = System.currentTimeMillis()
        }
    }
}
