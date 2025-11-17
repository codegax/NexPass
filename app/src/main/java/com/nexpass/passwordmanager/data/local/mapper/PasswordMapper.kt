package com.nexpass.passwordmanager.data.local.mapper

import com.nexpass.passwordmanager.data.local.entity.PasswordEntryEntity
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.security.encryption.CryptoManager
import com.nexpass.passwordmanager.security.encryption.EncryptedData

/**
 * Mapper for converting between PasswordEntry domain model and PasswordEntryEntity.
 *
 * Handles encryption/decryption of sensitive fields using CryptoManager.
 *
 * Security:
 * - Password and notes are encrypted before storage
 * - Decryption happens on-demand when converting to domain model
 * - Uses vault key for encryption/decryption
 */
class PasswordMapper(
    private val cryptoManager: CryptoManager
) {

    /**
     * Convert domain model to entity (encrypts sensitive data).
     *
     * @param entry Domain model with plain text data
     * @param vaultKey 256-bit vault key for encryption
     * @return Entity with encrypted data ready for database storage
     */
    suspend fun toEntity(entry: PasswordEntry, vaultKey: ByteArray): PasswordEntryEntity {
        // Encrypt password
        val encryptedPassword = cryptoManager.encryptData(
            plaintext = entry.password.toByteArray(Charsets.UTF_8),
            vaultKey = vaultKey
        ).toByteArray()

        // Encrypt notes if present
        val encryptedNotes = entry.notes?.let { notes ->
            cryptoManager.encryptData(
                plaintext = notes.toByteArray(Charsets.UTF_8),
                vaultKey = vaultKey
            ).toByteArray()
        }

        return PasswordEntryEntity(
            id = entry.id,
            title = entry.title,
            username = entry.username,
            encryptedPassword = encryptedPassword,
            url = entry.url,
            encryptedNotes = encryptedNotes,
            folderId = entry.folderId,
            tags = entry.tags.joinToString(","),
            packageNames = entry.packageNames.joinToString(","),
            favorite = entry.favorite,
            createdAt = entry.createdAt,
            updatedAt = entry.updatedAt,
            lastModified = entry.lastModified,
            isQuarantined = entry.isQuarantined,
            revisionId = entry.revisionId
        )
    }

    /**
     * Convert entity to domain model (decrypts sensitive data).
     *
     * @param entity Entity with encrypted data from database
     * @param vaultKey 256-bit vault key for decryption
     * @return Domain model with decrypted data
     * @throws SecurityException if decryption fails
     */
    suspend fun toDomain(entity: PasswordEntryEntity, vaultKey: ByteArray): PasswordEntry {
        try {
            // Decrypt password
            val encryptedPasswordData = EncryptedData.fromByteArray(entity.encryptedPassword)
            val decryptedPassword = cryptoManager.decryptData(
                encryptedData = encryptedPasswordData,
                vaultKey = vaultKey
            ).toString(Charsets.UTF_8)

            // Decrypt notes if present
            val decryptedNotes = entity.encryptedNotes?.let { encryptedNotes ->
                val encryptedNotesData = EncryptedData.fromByteArray(encryptedNotes)
                cryptoManager.decryptData(
                    encryptedData = encryptedNotesData,
                    vaultKey = vaultKey
                ).toString(Charsets.UTF_8)
            }

            return PasswordEntry(
                id = entity.id,
                title = entity.title,
                username = entity.username,
                password = decryptedPassword,
                url = entity.url,
                notes = decryptedNotes,
                folderId = entity.folderId,
                tags = entity.tags.split(",").filter { it.isNotBlank() },
                packageNames = entity.packageNames.split(",").filter { it.isNotBlank() },
                favorite = entity.favorite,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                lastModified = entity.lastModified,
                isQuarantined = entity.isQuarantined,
                revisionId = entity.revisionId
            )
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt password entry: ${entity.id}", e)
        }
    }

    /**
     * Convert list of entities to domain models.
     */
    suspend fun toDomainList(entities: List<PasswordEntryEntity>, vaultKey: ByteArray): List<PasswordEntry> {
        return entities.map { toDomain(it, vaultKey) }
    }
}
