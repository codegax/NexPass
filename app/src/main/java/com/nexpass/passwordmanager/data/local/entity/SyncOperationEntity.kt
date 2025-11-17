package com.nexpass.passwordmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for tracking pending sync operations.
 *
 * When changes are made offline, they are queued here
 * and processed when network connectivity is restored.
 *
 * This ensures no data is lost when working offline.
 */
@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey
    val id: String,

    /** Type of operation: CREATE, UPDATE, DELETE */
    val type: String,

    /** ID of the entry being synced */
    val entryId: String,

    /** Entity type: PASSWORD, FOLDER, TAG */
    val entityType: String,

    /** Serialized data payload (JSON) */
    val data: String,

    /** Timestamp when operation was queued */
    val timestamp: Long,

    /** Number of retry attempts */
    val retryCount: Int = 0,

    /** Maximum retry attempts before giving up */
    val maxRetries: Int = 3,

    /** Current status: PENDING, IN_PROGRESS, COMPLETED, FAILED */
    val status: String,

    /** Error message if operation failed */
    val errorMessage: String? = null,

    /** Last attempt timestamp */
    val lastAttemptAt: Long? = null
)
