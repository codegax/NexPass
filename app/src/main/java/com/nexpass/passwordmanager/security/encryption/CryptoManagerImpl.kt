package com.nexpass.passwordmanager.security.encryption

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Implementation of CryptoManager using industry-standard cryptographic algorithms
 *
 * Security Implementation:
 * - Key Derivation: PBKDF2-HMAC-SHA256 with 100,000 iterations
 * - Encryption: AES-256-GCM (Galois/Counter Mode)
 * - Random Generation: SecureRandom for cryptographically secure randomness
 * - Memory Wiping: Overwrite sensitive arrays with zeros
 *
 * Thread Safety: All operations use Dispatchers.Default for computation
 *
 * IMPORTANT SECURITY NOTES:
 * - Never log any sensitive data (passwords, keys, plaintext)
 * - Always wipe sensitive byte arrays after use
 * - Each encryption uses a unique IV (never reuse IVs)
 * - GCM provides authentication - tampering will be detected
 */
class CryptoManagerImpl : CryptoManager {

    private val secureRandom = SecureRandom()

    companion object {
        // PBKDF2 parameters
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256

        // AES-GCM parameters
        private const val AES_ALGORITHM = "AES"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128 // 16 bytes

        // Size constants
        private const val SALT_LENGTH_BYTES = 32
        private const val IV_LENGTH_BYTES = 12 // Standard for AES-GCM
        private const val KEY_LENGTH_BYTES = 32 // 256 bits
        private const val MIN_SALT_LENGTH_BYTES = 16
    }

    /**
     * Derive vault key using PBKDF2-HMAC-SHA256
     *
     * Security: 100,000 iterations makes brute-force attacks computationally expensive
     */
    override suspend fun deriveVaultKey(masterPassword: String, salt: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            require(salt.size >= MIN_SALT_LENGTH_BYTES) {
                "Salt must be at least $MIN_SALT_LENGTH_BYTES bytes, got ${salt.size}"
            }

            try {
                val passwordChars = masterPassword.toCharArray()
                val spec: KeySpec = PBEKeySpec(
                    passwordChars,
                    salt,
                    PBKDF2_ITERATIONS,
                    KEY_LENGTH_BITS
                )

                val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                val key = factory.generateSecret(spec).encoded

                // Wipe the password char array
                MemoryUtils.wipeCharArray(passwordChars)

                return@withContext key
            } catch (e: Exception) {
                throw SecurityException("Key derivation failed", e)
            }
        }

    /**
     * Encrypt data using AES-256-GCM
     *
     * GCM Mode provides:
     * - Confidentiality (encryption)
     * - Authenticity (authentication tag)
     * - Protection against tampering
     */
    override suspend fun encryptData(plaintext: ByteArray, vaultKey: ByteArray): EncryptedData =
        withContext(Dispatchers.Default) {
            require(vaultKey.size == KEY_LENGTH_BYTES) {
                "Vault key must be $KEY_LENGTH_BYTES bytes, got ${vaultKey.size}"
            }

            try {
                // Generate unique IV for this encryption
                val iv = generateIV()

                // Create cipher in GCM mode
                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                val keySpec = SecretKeySpec(vaultKey, AES_ALGORITHM)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)

                // Initialize for encryption
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

                // Perform encryption (includes authentication tag)
                val ciphertextWithTag = cipher.doFinal(plaintext)

                // In GCM mode, the authentication tag is appended to the ciphertext
                // Split ciphertext and authentication tag
                val ciphertextLength = ciphertextWithTag.size - (GCM_TAG_LENGTH_BITS / 8)
                val ciphertext = ciphertextWithTag.copyOfRange(0, ciphertextLength)
                val authTag = ciphertextWithTag.copyOfRange(ciphertextLength, ciphertextWithTag.size)

                // Wipe the combined array
                wipeByteArray(ciphertextWithTag)

                return@withContext EncryptedData(ciphertext, iv, authTag)
            } catch (e: Exception) {
                throw SecurityException("Encryption failed", e)
            }
        }

    /**
     * Decrypt data using AES-256-GCM
     *
     * Security: Verifies authentication tag - decryption fails if data was tampered
     */
    override suspend fun decryptData(encryptedData: EncryptedData, vaultKey: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            require(vaultKey.size == KEY_LENGTH_BYTES) {
                "Vault key must be $KEY_LENGTH_BYTES bytes, got ${vaultKey.size}"
            }

            try {
                // Reconstruct ciphertext with auth tag (required by GCM)
                val ciphertextWithTag = encryptedData.ciphertext + encryptedData.authTag

                // Create cipher in GCM mode
                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                val keySpec = SecretKeySpec(vaultKey, AES_ALGORITHM)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, encryptedData.iv)

                // Initialize for decryption
                cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

                // Perform decryption and verify authentication tag
                // Will throw AEADBadTagException if tag doesn't match (tampering detected)
                val plaintext = cipher.doFinal(ciphertextWithTag)

                // Wipe the combined array
                wipeByteArray(ciphertextWithTag)

                return@withContext plaintext
            } catch (e: javax.crypto.AEADBadTagException) {
                throw SecurityException("Authentication failed - data may have been tampered with", e)
            } catch (e: Exception) {
                throw SecurityException("Decryption failed", e)
            }
        }

    /**
     * Generate cryptographically secure random salt (32 bytes)
     */
    override suspend fun generateSalt(): ByteArray = withContext(Dispatchers.Default) {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return@withContext salt
    }

    /**
     * Generate cryptographically secure random IV (12 bytes for GCM)
     */
    override suspend fun generateIV(): ByteArray = withContext(Dispatchers.Default) {
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        return@withContext iv
    }

    /**
     * Securely wipe byte array by overwriting with zeros
     *
     * CRITICAL SECURITY: This prevents sensitive data from remaining in memory
     * Call this for ALL sensitive data (passwords, keys, plaintext) after use
     */
    override fun wipeByteArray(array: ByteArray) {
        array.fill(0)
    }
}
