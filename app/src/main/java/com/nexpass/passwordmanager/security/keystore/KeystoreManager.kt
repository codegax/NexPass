package com.nexpass.passwordmanager.security.keystore

import javax.crypto.SecretKey

/**
 * KeystoreManager provides secure key storage using Android Keystore.
 *
 * This interface abstracts all Android Keystore operations including:
 * - Master key generation and storage
 * - Vault key encryption/decryption
 * - Biometric-protected key management
 *
 * Security properties:
 * - Keys never leave the Android Keystore
 * - Hardware-backed security when available
 * - Biometric authentication required for sensitive operations
 * - Automatic key invalidation on device security changes
 */
interface KeystoreManager {

    /**
     * Initialize the Android Keystore.
     * Must be called before any other operations.
     *
     * @throws KeystoreException if initialization fails
     */
    suspend fun initialize()

    /**
     * Get the master encryption key from Keystore, or create it if it doesn't exist.
     * This key is used to encrypt the vault key derived from the master password.
     *
     * Key properties:
     * - AES-256-GCM
     * - Hardware-backed when available
     * - User authentication NOT required (for non-biometric unlock)
     *
     * @return The master SecretKey
     * @throws KeystoreException if key generation/retrieval fails
     */
    suspend fun getOrCreateMasterKey(): SecretKey

    /**
     * Encrypt the vault key using the master key from Keystore.
     * The vault key is derived from the master password and needs secure storage.
     *
     * @param vaultKey The 256-bit vault key to encrypt
     * @return Encrypted vault key as ByteArray
     * @throws KeystoreException if encryption fails
     */
    suspend fun encryptVaultKey(vaultKey: ByteArray): ByteArray

    /**
     * Decrypt the vault key using the master key from Keystore.
     *
     * @param encryptedVaultKey The encrypted vault key
     * @return Decrypted vault key as ByteArray
     * @throws KeystoreException if decryption fails or authentication required
     */
    suspend fun decryptVaultKey(encryptedVaultKey: ByteArray): ByteArray

    /**
     * Check if a biometric-protected key is available.
     * This key requires biometric authentication to use.
     *
     * @return true if biometric key exists, false otherwise
     */
    suspend fun isBiometricKeyAvailable(): Boolean

    /**
     * Create a biometric-protected key in the Keystore.
     * This key requires biometric authentication for decryption.
     *
     * Key properties:
     * - AES-256-GCM
     * - User authentication REQUIRED (biometric)
     * - Timeout: 0 (requires auth for every use)
     * - Invalidated on new biometric enrollment
     *
     * @throws KeystoreException if key creation fails
     * @throws BiometricNotAvailableException if device doesn't support biometrics
     */
    suspend fun createBiometricKey()

    /**
     * Encrypt the vault key using the biometric-protected key.
     * Requires biometric authentication.
     *
     * @param vaultKey The 256-bit vault key to encrypt
     * @return Encrypted vault key as ByteArray
     * @throws KeystoreException if encryption fails
     * @throws BiometricAuthRequiredException if biometric auth is needed
     */
    suspend fun encryptVaultKeyWithBiometric(vaultKey: ByteArray): ByteArray

    /**
     * Decrypt the vault key using the biometric-protected key.
     * Requires biometric authentication.
     *
     * @param encryptedVaultKey The encrypted vault key
     * @return Decrypted vault key as ByteArray
     * @throws KeystoreException if decryption fails
     * @throws BiometricAuthRequiredException if biometric auth is needed
     */
    suspend fun decryptVaultKeyWithBiometric(encryptedVaultKey: ByteArray): ByteArray

    /**
     * Delete the biometric-protected key from Keystore.
     * Call this when user disables biometric unlock.
     */
    suspend fun deleteBiometricKey()

    /**
     * Delete all keys from Keystore.
     * This is a destructive operation - vault will be unrecoverable.
     * Call this when user resets the app or logs out permanently.
     */
    suspend fun deleteAllKeys()

    /**
     * Check if the master key exists in Keystore.
     *
     * @return true if master key exists, false otherwise
     */
    suspend fun isMasterKeyAvailable(): Boolean
}

/**
 * Exception thrown when Keystore operations fail.
 */
class KeystoreException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when biometric authentication is not available on the device.
 */
class BiometricNotAvailableException(message: String) : Exception(message)

/**
 * Exception thrown when biometric authentication is required but not provided.
 */
class BiometricAuthRequiredException(message: String) : Exception(message)
