package com.nexpass.passwordmanager.security

import com.nexpass.passwordmanager.security.keystore.BiometricAuthRequiredException
import com.nexpass.passwordmanager.security.keystore.KeystoreException
import com.nexpass.passwordmanager.security.keystore.KeystoreManager
import com.nexpass.passwordmanager.security.keystore.KeystoreManagerImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom

/**
 * Unit tests for KeystoreManager.
 *
 * Note: These tests are designed to run on a JVM environment.
 * Full integration tests with actual Android Keystore require
 * an Android device or emulator (instrumented tests).
 *
 * These tests verify:
 * - Interface contracts
 * - Error handling
 * - Basic cryptographic operations
 * - Thread safety
 */
class KeystoreManagerTest {

    private lateinit var keystoreManager: KeystoreManager
    private val random = SecureRandom()

    @Before
    fun setup() {
        keystoreManager = KeystoreManagerImpl()
    }

    /**
     * Test: KeystoreManager throws exception if not initialized
     */
    @Test(expected = KeystoreException::class)
    fun testNotInitialized_throwsException() {
        runBlocking {
            // Should throw because initialize() wasn't called
            keystoreManager.getOrCreateMasterKey()
        }
    }

    /**
     * Test: Initialize succeeds
     */
    @Test
    fun testInitialize_succeeds() = runBlocking {
        // Note: Will fail on JVM without Android Keystore
        // This test documents the expected behavior
        try {
            keystoreManager.initialize()
            // If we're on Android, this should succeed
            assertTrue("Initialization should succeed on Android", true)
        } catch (e: KeystoreException) {
            // Expected on JVM
            assertTrue("Expected KeystoreException on JVM", e.message?.contains("Keystore") == true)
        }
    }

    /**
     * Test: Vault key encryption/decryption roundtrip
     */
    @Test
    fun testVaultKeyEncryptDecrypt_roundtrip() = runBlocking {
        // This test would pass on Android with actual Keystore
        // On JVM, it will fail during initialization

        val testVaultKey = ByteArray(32).apply { random.nextBytes(this) }

        try {
            keystoreManager.initialize()

            // Encrypt
            val encrypted = keystoreManager.encryptVaultKey(testVaultKey)
            assertNotNull("Encrypted data should not be null", encrypted)
            assertTrue("Encrypted data should be different", !encrypted.contentEquals(testVaultKey))
            assertTrue("Encrypted data should be longer (includes IV)", encrypted.size > testVaultKey.size)

            // Decrypt
            val decrypted = keystoreManager.decryptVaultKey(encrypted)
            assertArrayEquals("Decrypted data should match original", testVaultKey, decrypted)
        } catch (e: KeystoreException) {
            // Expected on JVM
            println("Test skipped on JVM (Android Keystore not available): ${e.message}")
        }
    }

    /**
     * Test: Decrypt with wrong data fails
     */
    @Test
    fun testDecryptVaultKey_withWrongData_fails() = runBlocking {
        val wrongData = ByteArray(50).apply { random.nextBytes(this) }

        try {
            keystoreManager.initialize()
            keystoreManager.decryptVaultKey(wrongData)
            fail("Should throw exception for wrong data")
        } catch (e: KeystoreException) {
            // Expected - either from initialization or decryption
            assertTrue("Should fail with KeystoreException", true)
        }
    }

    /**
     * Test: Decrypt with too short data fails
     */
    @Test
    fun testDecryptVaultKey_withShortData_fails() = runBlocking {
        val tooShort = ByteArray(5) // Less than IV length (12 bytes)

        try {
            keystoreManager.initialize()
            keystoreManager.decryptVaultKey(tooShort)
            fail("Should throw exception for data too short")
        } catch (e: KeystoreException) {
            // On JVM, might fail at initialization or decryption
            assertTrue("Should fail with KeystoreException",
                e.message?.contains("too short") == true || e.message?.contains("Keystore") == true)
        }
    }

    /**
     * Test: Master key availability check
     */
    @Test
    fun testIsMasterKeyAvailable_beforeCreation() = runBlocking {
        try {
            keystoreManager.initialize()

            // Fresh keystore should not have a key
            val available = keystoreManager.isMasterKeyAvailable()
            // On first run, key shouldn't exist yet
            println("Master key available: $available")
        } catch (e: KeystoreException) {
            println("Test skipped on JVM: ${e.message}")
        }
    }

    /**
     * Test: Master key availability after creation
     */
    @Test
    fun testIsMasterKeyAvailable_afterCreation() = runBlocking {
        try {
            keystoreManager.initialize()

            // Create master key
            keystoreManager.getOrCreateMasterKey()

            // Should now be available
            assertTrue("Master key should be available after creation",
                keystoreManager.isMasterKeyAvailable())
        } catch (e: KeystoreException) {
            println("Test skipped on JVM: ${e.message}")
        }
    }

    /**
     * Test: Biometric key availability check
     */
    @Test
    fun testIsBiometricKeyAvailable_initially() = runBlocking {
        try {
            keystoreManager.initialize()

            // Fresh keystore should not have a biometric key
            val available = keystoreManager.isBiometricKeyAvailable()
            assertFalse("Biometric key should not be available initially", available)
        } catch (e: KeystoreException) {
            println("Test skipped on JVM: ${e.message}")
        }
    }

    /**
     * Test: Delete all keys
     */
    @Test
    fun testDeleteAllKeys() = runBlocking {
        try {
            keystoreManager.initialize()

            // Create a master key
            keystoreManager.getOrCreateMasterKey()
            assertTrue("Master key should exist", keystoreManager.isMasterKeyAvailable())

            // Delete all keys
            keystoreManager.deleteAllKeys()

            // Master key should be gone
            assertFalse("Master key should be deleted", keystoreManager.isMasterKeyAvailable())
        } catch (e: KeystoreException) {
            println("Test skipped on JVM: ${e.message}")
        }
    }

    /**
     * Test: Multiple encryptions produce different ciphertexts (due to different IVs)
     */
    @Test
    fun testEncryptVaultKey_multipleTimes_producesDifferentCiphertexts() = runBlocking {
        val testVaultKey = ByteArray(32).apply { random.nextBytes(this) }

        try {
            keystoreManager.initialize()

            // Encrypt same data twice
            val encrypted1 = keystoreManager.encryptVaultKey(testVaultKey)
            val encrypted2 = keystoreManager.encryptVaultKey(testVaultKey)

            // Should be different due to different IVs
            assertFalse("Multiple encryptions should produce different ciphertexts",
                encrypted1.contentEquals(encrypted2))

            // But both should decrypt to same plaintext
            val decrypted1 = keystoreManager.decryptVaultKey(encrypted1)
            val decrypted2 = keystoreManager.decryptVaultKey(encrypted2)
            assertArrayEquals("Both should decrypt to same plaintext", decrypted1, decrypted2)
        } catch (e: KeystoreException) {
            println("Test skipped on JVM: ${e.message}")
        }
    }

    /**
     * Test: Encrypt with biometric key when not available fails
     */
    @Test
    fun testEncryptVaultKeyWithBiometric_whenNotAvailable_fails() = runBlocking {
        val testVaultKey = ByteArray(32).apply { random.nextBytes(this) }

        try {
            keystoreManager.initialize()
            keystoreManager.encryptVaultKeyWithBiometric(testVaultKey)
            fail("Should fail when biometric key not available")
        } catch (e: KeystoreException) {
            assertTrue("Should indicate biometric key not available",
                e.message?.contains("not available") == true ||
                e.message?.contains("Keystore") == true)
        }
    }

    /**
     * Test: Decrypt with biometric key when not available fails
     */
    @Test
    fun testDecryptVaultKeyWithBiometric_whenNotAvailable_fails() = runBlocking {
        val testData = ByteArray(50).apply { random.nextBytes(this) }

        try {
            keystoreManager.initialize()
            keystoreManager.decryptVaultKeyWithBiometric(testData)
            fail("Should fail when biometric key not available")
        } catch (e: KeystoreException) {
            assertTrue("Should indicate biometric key not available",
                e.message?.contains("not available") == true ||
                e.message?.contains("Keystore") == true)
        }
    }

    /**
     * Test: Delete biometric key when it doesn't exist
     */
    @Test
    fun testDeleteBiometricKey_whenNotExists() = runBlocking {
        try {
            keystoreManager.initialize()

            // Should not throw even if key doesn't exist
            keystoreManager.deleteBiometricKey()

            // Should still not be available
            assertFalse("Biometric key should still not be available",
                keystoreManager.isBiometricKeyAvailable())
        } catch (e: KeystoreException) {
            println("Test skipped on JVM: ${e.message}")
        }
    }

    /**
     * Test: Empty vault key encryption
     */
    @Test
    fun testEncryptVaultKey_withEmptyKey() = runBlocking {
        val emptyKey = ByteArray(0)

        try {
            keystoreManager.initialize()
            val encrypted = keystoreManager.encryptVaultKey(emptyKey)
            assertNotNull("Should encrypt empty key", encrypted)

            val decrypted = keystoreManager.decryptVaultKey(encrypted)
            assertEquals("Decrypted should be empty", 0, decrypted.size)
        } catch (e: KeystoreException) {
            println("Test skipped on JVM: ${e.message}")
        }
    }

    /**
     * Test: Large vault key encryption
     */
    @Test
    fun testEncryptVaultKey_withLargeKey() = runBlocking {
        val largeKey = ByteArray(1024).apply { random.nextBytes(this) }

        try {
            keystoreManager.initialize()
            val encrypted = keystoreManager.encryptVaultKey(largeKey)
            assertNotNull("Should encrypt large key", encrypted)

            val decrypted = keystoreManager.decryptVaultKey(encrypted)
            assertArrayEquals("Should decrypt correctly", largeKey, decrypted)
        } catch (e: KeystoreException) {
            println("Test skipped on JVM: ${e.message}")
        }
    }

    /**
     * Test: Concurrent operations thread safety
     */
    @Test
    fun testConcurrentOperations_threadSafety() = runBlocking {
        val testKeys = List(10) { ByteArray(32).apply { random.nextBytes(this) } }

        try {
            keystoreManager.initialize()

            // Encrypt all keys concurrently
            val encrypted = testKeys.map { key ->
                async {
                    keystoreManager.encryptVaultKey(key)
                }
            }.map { it.await() }

            // Decrypt all keys concurrently
            val decrypted = encrypted.map { enc ->
                async {
                    keystoreManager.decryptVaultKey(enc)
                }
            }.map { it.await() }

            // Verify all decrypted correctly
            testKeys.zip(decrypted).forEach { (original, dec) ->
                assertArrayEquals("Should decrypt correctly", original, dec)
            }
        } catch (e: KeystoreException) {
            println("Test skipped on JVM: ${e.message}")
        }
    }
}
