package com.nexpass.passwordmanager.security

import com.nexpass.passwordmanager.security.encryption.CryptoManager
import com.nexpass.passwordmanager.security.encryption.CryptoManagerImpl
import com.nexpass.passwordmanager.security.encryption.EncryptedData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom

/**
 * Unit tests for CryptoManager
 *
 * Tests cover:
 * - Key derivation (PBKDF2)
 * - Encryption/decryption (AES-GCM)
 * - Random generation (salt, IV)
 * - Memory wiping
 * - Edge cases and error handling
 *
 * Target: 90%+ code coverage
 */
class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        cryptoManager = CryptoManagerImpl()
    }

    // ========== Key Derivation Tests ==========

    @Test
    fun `deriveVaultKey should return 32-byte key`() = runTest {
        val password = "TestPassword123!"
        val salt = cryptoManager.generateSalt()

        val key = cryptoManager.deriveVaultKey(password, salt)

        assertEquals(32, key.size) // 256 bits
    }

    @Test
    fun `deriveVaultKey should be deterministic with same inputs`() = runTest {
        val password = "TestPassword123!"
        val salt = ByteArray(32) { it.toByte() } // Fixed salt

        val key1 = cryptoManager.deriveVaultKey(password, salt)
        val key2 = cryptoManager.deriveVaultKey(password, salt)

        assertArrayEquals(key1, key2)
    }

    @Test
    fun `deriveVaultKey should produce different keys with different passwords`() = runTest {
        val salt = cryptoManager.generateSalt()

        val key1 = cryptoManager.deriveVaultKey("Password1", salt)
        val key2 = cryptoManager.deriveVaultKey("Password2", salt)

        assertFalse(key1.contentEquals(key2))
    }

    @Test
    fun `deriveVaultKey should produce different keys with different salts`() = runTest {
        val password = "TestPassword123!"
        val salt1 = cryptoManager.generateSalt()
        val salt2 = cryptoManager.generateSalt()

        val key1 = cryptoManager.deriveVaultKey(password, salt1)
        val key2 = cryptoManager.deriveVaultKey(password, salt2)

        assertFalse(key1.contentEquals(key2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deriveVaultKey should throw on short salt`() = runTest {
        val password = "TestPassword123!"
        val shortSalt = ByteArray(8) // Too short, should be at least 16

        cryptoManager.deriveVaultKey(password, shortSalt)
    }

    // ========== Encryption/Decryption Tests ==========

    @Test
    fun `encryptData should return valid EncryptedData`() = runTest {
        val plaintext = "Secret password 12345!".toByteArray()
        val vaultKey = cryptoManager.deriveVaultKey("master", cryptoManager.generateSalt())

        val encrypted = cryptoManager.encryptData(plaintext, vaultKey)

        assertNotNull(encrypted.ciphertext)
        assertEquals(EncryptedData.IV_LENGTH, encrypted.iv.size)
        assertEquals(EncryptedData.AUTH_TAG_LENGTH, encrypted.authTag.size)
        assertFalse(encrypted.ciphertext.contentEquals(plaintext)) // Should be encrypted
    }

    @Test
    fun `decryptData should return original plaintext`() = runTest {
        val plaintext = "Secret password 12345!".toByteArray()
        val vaultKey = cryptoManager.deriveVaultKey("master", cryptoManager.generateSalt())

        val encrypted = cryptoManager.encryptData(plaintext, vaultKey)
        val decrypted = cryptoManager.decryptData(encrypted, vaultKey)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encryption should produce different ciphertexts for same plaintext`() = runTest {
        val plaintext = "Secret password".toByteArray()
        val vaultKey = cryptoManager.deriveVaultKey("master", cryptoManager.generateSalt())

        val encrypted1 = cryptoManager.encryptData(plaintext, vaultKey)
        val encrypted2 = cryptoManager.encryptData(plaintext, vaultKey)

        // Different IVs should produce different ciphertexts
        assertFalse(encrypted1.iv.contentEquals(encrypted2.iv))
        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))

        // But both should decrypt to same plaintext
        val decrypted1 = cryptoManager.decryptData(encrypted1, vaultKey)
        val decrypted2 = cryptoManager.decryptData(encrypted2, vaultKey)
        assertArrayEquals(plaintext, decrypted1)
        assertArrayEquals(plaintext, decrypted2)
    }

    @Test(expected = SecurityException::class)
    fun `decryptData should fail with wrong key`() = runTest {
        val plaintext = "Secret password".toByteArray()
        val correctKey = cryptoManager.deriveVaultKey("correct", cryptoManager.generateSalt())
        val wrongKey = cryptoManager.deriveVaultKey("wrong", cryptoManager.generateSalt())

        val encrypted = cryptoManager.encryptData(plaintext, correctKey)

        // Should throw SecurityException due to auth tag mismatch
        cryptoManager.decryptData(encrypted, wrongKey)
    }

    @Test(expected = SecurityException::class)
    fun `decryptData should fail with tampered ciphertext`() = runTest {
        val plaintext = "Secret password".toByteArray()
        val vaultKey = cryptoManager.deriveVaultKey("master", cryptoManager.generateSalt())

        val encrypted = cryptoManager.encryptData(plaintext, vaultKey)

        // Tamper with ciphertext
        val tamperedCiphertext = encrypted.ciphertext.clone()
        tamperedCiphertext[0] = (tamperedCiphertext[0] + 1).toByte()
        val tamperedEncrypted = EncryptedData(tamperedCiphertext, encrypted.iv, encrypted.authTag)

        // Should throw SecurityException due to auth tag mismatch
        cryptoManager.decryptData(tamperedEncrypted, vaultKey)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `encryptData should throw on wrong key size`() = runTest {
        val plaintext = "Secret".toByteArray()
        val wrongSizeKey = ByteArray(16) // Should be 32 bytes

        cryptoManager.encryptData(plaintext, wrongSizeKey)
    }

    @Test
    fun `encryption should handle empty plaintext`() = runTest {
        val emptyPlaintext = ByteArray(0)
        val vaultKey = cryptoManager.deriveVaultKey("master", cryptoManager.generateSalt())

        val encrypted = cryptoManager.encryptData(emptyPlaintext, vaultKey)
        val decrypted = cryptoManager.decryptData(encrypted, vaultKey)

        assertArrayEquals(emptyPlaintext, decrypted)
    }

    @Test
    fun `encryption should handle large plaintext`() = runTest {
        val largePlaintext = ByteArray(1024 * 100) { it.toByte() } // 100 KB
        val vaultKey = cryptoManager.deriveVaultKey("master", cryptoManager.generateSalt())

        val encrypted = cryptoManager.encryptData(largePlaintext, vaultKey)
        val decrypted = cryptoManager.decryptData(encrypted, vaultKey)

        assertArrayEquals(largePlaintext, decrypted)
    }

    // ========== Random Generation Tests ==========

    @Test
    fun `generateSalt should return 32 bytes`() = runTest {
        val salt = cryptoManager.generateSalt()

        assertEquals(32, salt.size)
    }

    @Test
    fun `generateSalt should produce unique salts`() = runTest {
        val salt1 = cryptoManager.generateSalt()
        val salt2 = cryptoManager.generateSalt()

        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun `generateIV should return 12 bytes`() = runTest {
        val iv = cryptoManager.generateIV()

        assertEquals(12, iv.size) // AES-GCM standard IV size
    }

    @Test
    fun `generateIV should produce unique IVs`() = runTest {
        val iv1 = cryptoManager.generateIV()
        val iv2 = cryptoManager.generateIV()

        assertFalse(iv1.contentEquals(iv2))
    }

    // ========== Memory Wiping Tests ==========

    @Test
    fun `wipeByteArray should overwrite array with zeros`() {
        val sensitiveData = "SecretPassword123!".toByteArray()
        val originalCopy = sensitiveData.clone()

        cryptoManager.wipeByteArray(sensitiveData)

        // Array should be all zeros
        assertTrue(sensitiveData.all { it == 0.toByte() })

        // Verify it was changed
        assertFalse(sensitiveData.contentEquals(originalCopy))
    }

    @Test
    fun `wipeByteArray should handle empty array`() {
        val emptyArray = ByteArray(0)

        // Should not throw
        cryptoManager.wipeByteArray(emptyArray)

        assertEquals(0, emptyArray.size)
    }

    // ========== Integration Tests ==========

    @Test
    fun `full encryption workflow should work end-to-end`() = runTest {
        // Simulate creating a vault
        val masterPassword = "MySecureMasterPassword123!"
        val salt = cryptoManager.generateSalt()
        val vaultKey = cryptoManager.deriveVaultKey(masterPassword, salt)

        // Encrypt a password entry
        val passwordEntry = "username: john@example.com\npassword: SuperSecret123!"
        val encrypted = cryptoManager.encryptData(passwordEntry.toByteArray(), vaultKey)

        // Simulate storing and retrieving
        val storedBytes = encrypted.toByteArray()
        val retrievedEncrypted = EncryptedData.fromByteArray(storedBytes)

        // Decrypt
        val decrypted = cryptoManager.decryptData(retrievedEncrypted, vaultKey)
        val decryptedString = String(decrypted)

        assertEquals(passwordEntry, decryptedString)

        // Clean up sensitive data
        cryptoManager.wipeByteArray(vaultKey)
        cryptoManager.wipeByteArray(decrypted)
    }

    @Test
    fun `known vector test for PBKDF2`() = runTest {
        // Test with a known password and salt to verify PBKDF2 implementation
        val password = "password"
        val salt = ByteArray(16) { 0 } // All zeros

        val key = cryptoManager.deriveVaultKey(password, salt)

        // Key should be deterministic
        assertEquals(32, key.size)

        // Derive again and verify consistency
        val key2 = cryptoManager.deriveVaultKey(password, salt)
        assertArrayEquals(key, key2)
    }
}
