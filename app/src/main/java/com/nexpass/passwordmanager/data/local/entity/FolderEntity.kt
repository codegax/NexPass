package com.nexpass.passwordmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for storing folders.
 *
 * Folders organize password entries in a hierarchical structure.
 * Supports nested folders via the parentId field.
 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,

    /** Display name of the folder */
    val name: String,

    /** Parent folder ID (null for root level folders) */
    val parentId: String?,

    /** Timestamp when folder was created */
    val createdAt: Long,

    /** Timestamp when folder was last modified */
    val updatedAt: Long,

    /** Server timestamp from Nextcloud */
    val lastModified: Long,

    /** Revision ID from Nextcloud */
    val revisionId: String? = null
)
