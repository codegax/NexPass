package com.nexpass.passwordmanager.domain.model

/**
 * Domain model for a password entry.
 *
 * This is the business logic representation of a password entry.
 * All sensitive data is in decrypted form here.
 *
 * Note: This should only exist in memory and never be persisted directly.
 */
data class PasswordEntry(
    val id: String,
    val title: String,
    val username: String,
    val password: String, // Decrypted password
    val url: String?,
    val notes: String?, // Decrypted notes
    val folderId: String?,
    val tags: List<String>,
    val packageNames: List<String>,
    val favorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastModified: Long,
    val isQuarantined: Boolean = false,
    val revisionId: String? = null
)
