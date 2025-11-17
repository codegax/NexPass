package com.nexpass.passwordmanager.security.vault

import com.nexpass.passwordmanager.security.encryption.CryptoManager
import com.nexpass.passwordmanager.security.keystore.KeystoreManager
import com.nexpass.passwordmanager.security.encryption.MemoryUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the vault key lifecycle in memory.
 *
 * The vault key is the master encryption key used to encrypt/decrypt all vault data.
 * It is derived from the user's master password and stored in memory only while the vault is unlocked.
 *
 * Security features:
 * - In-memory only storage (never persisted)
 * - Automatic wiping on lock
 * - Thread-safe access with mutex
 * - Integration with biometric authentication
 */
class VaultKeyManager(
    private val cryptoManager: CryptoManager,
    private val keystoreManager: KeystoreManager
) {
    private var vaultKey: ByteArray? = null
    private val mutex = Mutex()

    /**
     * Checks if the vault is currently unlocked.
     */
    fun isUnlocked(): Boolean {
        return vaultKey != null
    }

    /**
     * Unlock the vault with a master password.
     *
     * @param masterPassword The user's master password
     * @param salt The salt used for key derivation (must be stored securely)
     * @throws Exception if unlock fails
     */
    suspend fun unlockWithPassword(masterPassword: String, salt: ByteArray) = mutex.withLock {
        if (vaultKey != null) {
            MemoryUtils.wipeByteArray(vaultKey!!)
        }

        // Derive vault key from master password and salt
        vaultKey = cryptoManager.deriveVaultKey(masterPassword, salt)
    }

    /**
     * Unlock the vault with biometric authentication.
     *
     * @param encryptedVaultKey The encrypted vault key from secure storage
     * @throws Exception if unlock fails
     */
    suspend fun unlockWithBiometric(encryptedVaultKey: ByteArray) = mutex.withLock {
        if (vaultKey != null) {
            MemoryUtils.wipeByteArray(vaultKey!!)
        }

        // Decrypt vault key using biometric-protected key
        vaultKey = keystoreManager.decryptVaultKey(encryptedVaultKey)
    }

    /**
     * Lock the vault and wipe the vault key from memory.
     */
    suspend fun lock() = mutex.withLock {
        vaultKey?.let { key ->
            MemoryUtils.wipeByteArray(key)
            vaultKey = null
        }
    }

    /**
     * Get the current vault key.
     *
     * @return The vault key if unlocked, null otherwise
     */
    suspend fun getVaultKey(): ByteArray? = mutex.withLock {
        return vaultKey
    }

    /**
     * Get the current vault key or throw if locked.
     *
     * @return The vault key
     * @throws IllegalStateException if vault is locked
     */
    suspend fun requireVaultKey(): ByteArray = mutex.withLock {
        return vaultKey ?: throw IllegalStateException("Vault is locked")
    }

    /**
     * Enable biometric unlock by encrypting and storing the vault key.
     *
     * This should only be called when the vault is unlocked.
     *
     * @return The encrypted vault key to be stored securely
     * @throws IllegalStateException if vault is locked
     */
    suspend fun enableBiometric(): ByteArray = mutex.withLock {
        val key = vaultKey ?: throw IllegalStateException("Vault must be unlocked to enable biometric")

        // Ensure biometric key exists
        keystoreManager.createBiometricKey()

        // Encrypt vault key using biometric-protected key
        return keystoreManager.encryptVaultKey(key)
    }

    /**
     * Disable biometric unlock by deleting the biometric key.
     */
    suspend fun disableBiometric() {
        keystoreManager.deleteBiometricKey()
    }
}
