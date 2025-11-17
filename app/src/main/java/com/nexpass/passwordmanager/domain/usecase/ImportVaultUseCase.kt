package com.nexpass.passwordmanager.domain.usecase

import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import com.nexpass.passwordmanager.security.encryption.CryptoManager
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.UUID

/**
 * Use case for importing a vault from an encrypted JSON file.
 *
 * Imports passwords from an exported vault file.
 */
class ImportVaultUseCase(
    private val passwordRepository: PasswordRepository,
    private val cryptoManager: CryptoManager
) {

    /**
     * Import vault from encrypted JSON.
     *
     * @param inputStream Stream to read the import from
     * @param importPassword Password to decrypt the import
     * @param replaceExisting If true, clear existing passwords before import
     * @return Number of passwords imported
     */
    suspend fun execute(
        inputStream: InputStream,
        importPassword: String,
        replaceExisting: Boolean = false
    ): Int {
        // Read encrypted data
        val importBytes = inputStream.readBytes()

        // Extract components (salt + IV + ciphertext + authTag)
        // Salt: 32 bytes, IV: 12 bytes, AuthTag: 16 bytes
        val saltSize = 32
        val ivSize = 12
        val authTagSize = 16
        val minSize = saltSize + ivSize + authTagSize

        if (importBytes.size < minSize) {
            throw IllegalArgumentException("Invalid import file: too small")
        }

        val salt = importBytes.copyOfRange(0, saltSize)
        val iv = importBytes.copyOfRange(saltSize, saltSize + ivSize)
        val authTag = importBytes.copyOfRange(importBytes.size - authTagSize, importBytes.size)
        val ciphertext = importBytes.copyOfRange(saltSize + ivSize, importBytes.size - authTagSize)

        // Derive decryption key from import password
        val decryptionKey = cryptoManager.deriveVaultKey(importPassword, salt)

        // Decrypt the JSON
        val encryptedData = com.nexpass.passwordmanager.security.encryption.EncryptedData(
            iv = iv,
            ciphertext = ciphertext,
            authTag = authTag
        )
        val decryptedBytes = cryptoManager.decryptData(encryptedData, decryptionKey)
        val jsonString = String(decryptedBytes, Charsets.UTF_8)

        // Parse JSON
        val json = Json {
            ignoreUnknownKeys = true
        }
        val exportData = json.decodeFromString<VaultExport>(jsonString)

        // Validate version
        if (exportData.version != 1) {
            throw IllegalArgumentException("Unsupported export version: ${exportData.version}")
        }

        // Clear existing passwords if requested
        if (replaceExisting) {
            val existing = passwordRepository.getAll()
            existing.forEach { passwordRepository.delete(it.id) }
        }

        // Import passwords
        val now = System.currentTimeMillis()
        exportData.passwords.forEach { exported ->
            val passwordEntry = PasswordEntry(
                id = UUID.randomUUID().toString(), // Generate new IDs to avoid conflicts
                title = exported.title,
                username = exported.username,
                password = exported.password,
                url = exported.url,
                notes = exported.notes,
                folderId = exported.folderId,
                tags = exported.tags,
                packageNames = emptyList(), // Package names are device-specific
                favorite = exported.favorite,
                createdAt = exported.createdAt,
                updatedAt = now, // Update timestamp to now
                lastModified = now,
                isQuarantined = false,
                revisionId = null
            )

            passwordRepository.insert(passwordEntry)
        }

        return exportData.passwords.size
    }
}
