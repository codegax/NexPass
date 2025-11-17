package com.nexpass.passwordmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for storing password entries.
 *
 * This entity stores encrypted password data in the local SQLCipher database.
 * All sensitive fields (password, notes) are stored as encrypted ByteArrays.
 *
 * Security:
 * - Password and notes are encrypted using CryptoManager
 * - Each entry has its own encryption IV
 * - Encryption happens before database storage
 */
@Entity(tableName = "password_entries")
data class PasswordEntryEntity(
    @PrimaryKey
    val id: String,

    /** Display name for the password entry */
    val title: String,

    /** Username/email for login */
    val username: String,

    /** Encrypted password data (ciphertext + IV + tag) */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val encryptedPassword: ByteArray,

    /** Website URL or app identifier */
    val url: String?,

    /** Encrypted notes (ciphertext + IV + tag), nullable */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val encryptedNotes: ByteArray?,

    /** Folder ID this entry belongs to (nullable for root) */
    val folderId: String?,

    /** Comma-separated tag IDs */
    val tags: String,

    /** Comma-separated Android package names for autofill matching */
    val packageNames: String,

    /** Whether this entry is marked as favorite */
    val favorite: Boolean,

    /** Timestamp when entry was created locally */
    val createdAt: Long,

    /** Timestamp when entry was last modified locally */
    val updatedAt: Long,

    /** Server timestamp from Nextcloud (for sync conflict resolution) */
    val lastModified: Long,

    /** Whether this entry failed to decrypt (quarantine) */
    val isQuarantined: Boolean = false,

    /** Revision ID from Nextcloud (for optimistic locking) */
    val revisionId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PasswordEntryEntity

        if (id != other.id) return false
        if (title != other.title) return false
        if (username != other.username) return false
        if (!encryptedPassword.contentEquals(other.encryptedPassword)) return false
        if (url != other.url) return false
        if (encryptedNotes != null) {
            if (other.encryptedNotes == null) return false
            if (!encryptedNotes.contentEquals(other.encryptedNotes)) return false
        } else if (other.encryptedNotes != null) return false
        if (folderId != other.folderId) return false
        if (tags != other.tags) return false
        if (packageNames != other.packageNames) return false
        if (favorite != other.favorite) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (lastModified != other.lastModified) return false
        if (isQuarantined != other.isQuarantined) return false
        if (revisionId != other.revisionId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + encryptedPassword.contentHashCode()
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (encryptedNotes?.contentHashCode() ?: 0)
        result = 31 * result + (folderId?.hashCode() ?: 0)
        result = 31 * result + tags.hashCode()
        result = 31 * result + packageNames.hashCode()
        result = 31 * result + favorite.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + isQuarantined.hashCode()
        result = 31 * result + (revisionId?.hashCode() ?: 0)
        return result
    }
}
