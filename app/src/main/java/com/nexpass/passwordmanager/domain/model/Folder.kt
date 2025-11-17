package com.nexpass.passwordmanager.domain.model

/**
 * Domain model for a folder.
 */
data class Folder(
    val id: String,
    val name: String,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastModified: Long,
    val revisionId: String? = null
)
