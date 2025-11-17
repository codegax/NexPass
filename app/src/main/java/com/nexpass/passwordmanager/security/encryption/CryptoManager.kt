package com.nexpass.passwordmanager.security.encryption

/**
 * CryptoManager interface for all cryptographic operations
 *
 * This interface defines the contract for:
 * - Key derivation from master password
 * - Data encryption/decryption using AES-GCM-256
 * - Secure random generation (salt, IV)
 * - Memory wiping for sensitive data
 *
 * Security Requirements:
 * - Uses PBKDF2 with 100,000 iterations for key derivation
 * - Uses AES-GCM-256 for encryption (provides both confidentiality and authenticity)
 * - Generates cryptographically secure random values
 * - Wipes sensitive data from memory after use
 * - Never logs sensitive information
 */
interface CryptoManager {

    /**
     * Derive a vault encryption key from the master password
     *
     * Uses PBKDF2-HMAC-SHA256 with 100,000 iterations to derive a 256-bit key.
     * The derived key is used for encrypting/decrypting vault entries.
     *
     * @param masterPassword The user's master password (will be wiped after use)
     * @param salt Random salt for key derivation (must be at least 16 bytes)
     * @return 256-bit (32 byte) derived key
     * @throws IllegalArgumentException if salt is too short
     */
    suspend fun deriveVaultKey(masterPassword: String, salt: ByteArray): ByteArray

    /**
     * Encrypt data using AES-GCM-256
     *
     * Uses AES in Galois/Counter Mode (GCM) which provides:
     * - Confidentiality (encryption)
     * - Authenticity (authentication tag)
     * - Protection against tampering
     *
     * A unique IV (Initialization Vector) is generated for each encryption.
     *
     * @param plaintext The data to encrypt
     * @param vaultKey The 256-bit encryption key (from deriveVaultKey)
     * @return EncryptedData containing ciphertext, IV, and authentication tag
     * @throws IllegalArgumentException if vaultKey is not 32 bytes
     */
    suspend fun encryptData(plaintext: ByteArray, vaultKey: ByteArray): EncryptedData

    /**
     * Decrypt data using AES-GCM-256
     *
     * Decrypts data and verifies the authentication tag. If the tag doesn't match,
     * the data has been tampered with and decryption fails.
     *
     * @param encryptedData The encrypted data with IV and auth tag
     * @param vaultKey The 256-bit encryption key (same as used for encryption)
     * @return Decrypted plaintext bytes
     * @throws SecurityException if authentication tag verification fails
     * @throws IllegalArgumentException if vaultKey is not 32 bytes
     */
    suspend fun decryptData(encryptedData: EncryptedData, vaultKey: ByteArray): ByteArray

    /**
     * Generate a cryptographically secure random salt
     *
     * Salt is used for key derivation and should be unique per user.
     * Recommended size: 32 bytes (256 bits).
     *
     * @return 32-byte random salt
     */
    suspend fun generateSalt(): ByteArray

    /**
     * Generate a cryptographically secure random IV (Initialization Vector)
     *
     * IV must be unique for each encryption operation.
     * For AES-GCM, the IV size is 12 bytes (96 bits).
     *
     * @return 12-byte random IV
     */
    suspend fun generateIV(): ByteArray

    /**
     * Securely wipe a byte array from memory
     *
     * Overwrites the array with zeros to prevent sensitive data from remaining
     * in memory after use. This is critical for security.
     *
     * IMPORTANT: Call this for all sensitive data (passwords, keys, plaintext)
     * as soon as you're done with them.
     *
     * @param array The byte array to wipe (will be modified in place)
     */
    fun wipeByteArray(array: ByteArray)
}
