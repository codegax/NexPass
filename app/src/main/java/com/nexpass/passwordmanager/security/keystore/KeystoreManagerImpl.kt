package com.nexpass.passwordmanager.security.keystore

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Implementation of KeystoreManager using Android Keystore System.
 *
 * This implementation provides secure key storage with hardware-backed security
 * when available. All cryptographic operations are performed using keys that
 * never leave the Keystore.
 *
 * Thread safety: All operations are performed on Dispatchers.Default for thread safety.
 *
 * Security properties:
 * - AES-256-GCM for all encryption
 * - Hardware-backed keys when available
 * - Keys invalidated on security state changes
 * - No sensitive data in logs
 */
class KeystoreManagerImpl : KeystoreManager {

    private var keyStore: KeyStore? = null

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "nexpass_master_key"
        private const val BIOMETRIC_KEY_ALIAS = "nexpass_biometric_key"

        // AES-GCM parameters
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/" +
                "${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
    }

    override suspend fun initialize(): Unit = withContext(Dispatchers.Default) {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }
        } catch (e: Exception) {
            throw KeystoreException("Failed to initialize Keystore", e)
        }
    }

    override suspend fun getOrCreateMasterKey(): SecretKey = withContext(Dispatchers.Default) {
        ensureInitialized()

        // Try to retrieve existing key
        keyStore?.getKey(MASTER_KEY_ALIAS, null)?.let { key ->
            return@withContext key as SecretKey
        }

        // Generate new key if it doesn't exist
        return@withContext generateMasterKey()
    }

    override suspend fun encryptVaultKey(vaultKey: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        try {
            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)

            val iv = cipher.iv
            val ciphertext = cipher.doFinal(vaultKey)

            // Prepend IV to ciphertext: [IV (12 bytes) | Ciphertext + Tag]
            return@withContext iv + ciphertext
        } catch (e: Exception) {
            throw KeystoreException("Failed to encrypt vault key", e)
        }
    }

    override suspend fun decryptVaultKey(encryptedVaultKey: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        try {
            if (encryptedVaultKey.size < GCM_IV_LENGTH) {
                throw KeystoreException("Invalid encrypted data: too short")
            }

            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)

            // Extract IV and ciphertext
            val iv = encryptedVaultKey.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = encryptedVaultKey.copyOfRange(GCM_IV_LENGTH, encryptedVaultKey.size)

            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmSpec)

            return@withContext cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw KeystoreException("Failed to decrypt vault key", e)
        }
    }

    override suspend fun isBiometricKeyAvailable(): Boolean = withContext(Dispatchers.Default) {
        ensureInitialized()
        keyStore?.containsAlias(BIOMETRIC_KEY_ALIAS) == true
    }

    override suspend fun createBiometricKey(): Unit = withContext(Dispatchers.Default) {
        ensureInitialized()

        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val builder = KeyGenParameterSpec.Builder(
                BIOMETRIC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)

            // API 30+ uses setUserAuthenticationParameters, API 29 uses deprecated method
            val keyGenParameterSpec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                    0, // Timeout: 0 = require auth for every use
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                ).build()
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1) // -1 = require auth for every use
                    .build()
            }

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            when {
                e.message?.contains("biometric") == true -> {
                    throw BiometricNotAvailableException("Biometrics not available on this device")
                }
                else -> {
                    throw KeystoreException("Failed to create biometric key", e)
                }
            }
        }
    }

    override suspend fun encryptVaultKeyWithBiometric(vaultKey: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        try {
            if (!isBiometricKeyAvailable()) {
                throw KeystoreException("Biometric key not available")
            }

            val biometricKey = keyStore?.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey
                ?: throw KeystoreException("Failed to retrieve biometric key")

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, biometricKey)

            val iv = cipher.iv
            val ciphertext = cipher.doFinal(vaultKey)

            // Prepend IV to ciphertext
            return@withContext iv + ciphertext
        } catch (e: android.security.keystore.UserNotAuthenticatedException) {
            throw BiometricAuthRequiredException("Biometric authentication required")
        } catch (e: Exception) {
            throw KeystoreException("Failed to encrypt vault key with biometric", e)
        }
    }

    override suspend fun decryptVaultKeyWithBiometric(encryptedVaultKey: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        try {
            if (!isBiometricKeyAvailable()) {
                throw KeystoreException("Biometric key not available")
            }

            if (encryptedVaultKey.size < GCM_IV_LENGTH) {
                throw KeystoreException("Invalid encrypted data: too short")
            }

            val biometricKey = keyStore?.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey
                ?: throw KeystoreException("Failed to retrieve biometric key")

            val cipher = Cipher.getInstance(TRANSFORMATION)

            // Extract IV and ciphertext
            val iv = encryptedVaultKey.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = encryptedVaultKey.copyOfRange(GCM_IV_LENGTH, encryptedVaultKey.size)

            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, biometricKey, gcmSpec)

            return@withContext cipher.doFinal(ciphertext)
        } catch (e: android.security.keystore.UserNotAuthenticatedException) {
            throw BiometricAuthRequiredException("Biometric authentication required")
        } catch (e: Exception) {
            throw KeystoreException("Failed to decrypt vault key with biometric", e)
        }
    }

    override suspend fun deleteBiometricKey(): Unit = withContext(Dispatchers.Default) {
        ensureInitialized()

        try {
            if (keyStore?.containsAlias(BIOMETRIC_KEY_ALIAS) == true) {
                keyStore?.deleteEntry(BIOMETRIC_KEY_ALIAS)
            }
        } catch (e: Exception) {
            throw KeystoreException("Failed to delete biometric key", e)
        }
    }

    override suspend fun deleteAllKeys(): Unit = withContext(Dispatchers.Default) {
        ensureInitialized()

        try {
            listOf(MASTER_KEY_ALIAS, BIOMETRIC_KEY_ALIAS).forEach { alias ->
                if (keyStore?.containsAlias(alias) == true) {
                    keyStore?.deleteEntry(alias)
                }
            }
        } catch (e: Exception) {
            throw KeystoreException("Failed to delete keys", e)
        }
    }

    override suspend fun isMasterKeyAvailable(): Boolean = withContext(Dispatchers.Default) {
        ensureInitialized()
        keyStore?.containsAlias(MASTER_KEY_ALIAS) == true
    }

    /**
     * Generate a new master key in the Keystore.
     */
    private fun generateMasterKey(): SecretKey {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            throw KeystoreException("Failed to generate master key", e)
        }
    }

    /**
     * Ensure the Keystore has been initialized.
     */
    private fun ensureInitialized() {
        if (keyStore == null) {
            throw KeystoreException("Keystore not initialized. Call initialize() first.")
        }
    }
}
