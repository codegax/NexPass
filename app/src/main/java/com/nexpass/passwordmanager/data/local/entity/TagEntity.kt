package com.nexpass.passwordmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for storing tags.
 *
 * Tags provide flexible categorization of password entries.
 * Multiple tags can be assigned to a single password entry.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: String,

    /** Display name of the tag */
    val name: String,

    /** Color code for UI display (hex color, e.g., "#FF5722") */
    val color: String,

    /** Timestamp when tag was created */
    val createdAt: Long,

    /** Timestamp when tag was last modified */
    val updatedAt: Long,

    /** Server timestamp from Nextcloud */
    val lastModified: Long,

    /** Revision ID from Nextcloud */
    val revisionId: String? = null
)
