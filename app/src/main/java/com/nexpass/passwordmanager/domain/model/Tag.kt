package com.nexpass.passwordmanager.domain.model

/**
 * Domain model for a tag.
 */
data class Tag(
    val id: String,
    val name: String,
    val color: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastModified: Long,
    val revisionId: String? = null
)
