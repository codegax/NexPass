package com.nexpass.passwordmanager.domain.usecase

import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import com.nexpass.passwordmanager.security.encryption.CryptoManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream

/**
 * Use case for exporting the vault to an encrypted JSON file.
 *
 * Exports all passwords in an encrypted format that can be imported later.
 */
class ExportVaultUseCase(
    private val passwordRepository: PasswordRepository,
    private val cryptoManager: CryptoManager
) {

    /**
     * Export vault to encrypted JSON.
     *
     * @param outputStream Stream to write the export to
     * @param exportPassword Password to encrypt the export (separate from master password)
     * @return Number of passwords exported
     */
    suspend fun execute(
        outputStream: OutputStream,
        exportPassword: String
    ): Int {
        // Get all passwords
        val passwords = passwordRepository.getAll()

        // Create export data
        val exportData = VaultExport(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            passwords = passwords.map { password ->
                ExportedPassword(
                    title = password.title,
                    username = password.username,
                    password = password.password,
                    url = password.url,
                    notes = password.notes,
                    folderId = password.folderId,
                    tags = password.tags,
                    favorite = password.favorite,
                    createdAt = password.createdAt,
                    updatedAt = password.updatedAt
                )
            }
        )

        // Serialize to JSON
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val jsonString = json.encodeToString(exportData)

        // Generate salt for key derivation
        val salt = cryptoManager.generateSalt()

        // Derive encryption key from export password
        val encryptionKey = cryptoManager.deriveVaultKey(exportPassword, salt)

        // Encrypt the JSON
        val encryptedData = cryptoManager.encryptData(
            plaintext = jsonString.toByteArray(Charsets.UTF_8),
            vaultKey = encryptionKey
        )

        // Combine salt + IV + ciphertext + authTag for export
        val exportBytes = salt + encryptedData.iv + encryptedData.ciphertext + encryptedData.authTag

        // Write to output stream
        outputStream.write(exportBytes)
        outputStream.flush()

        return passwords.size
    }
}

/**
 * Vault export data structure.
 */
@Serializable
data class VaultExport(
    val version: Int,
    val exportedAt: Long,
    val passwords: List<ExportedPassword>
)

/**
 * Exported password entry.
 */
@Serializable
data class ExportedPassword(
    val title: String,
    val username: String,
    val password: String,
    val url: String?,
    val notes: String?,
    val folderId: String?,
    val tags: List<String>,
    val favorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
