package com.nexpass.passwordmanager.domain.model

/**
 * Domain model for a sync operation.
 */
data class SyncOperation(
    val id: String,
    val type: OperationType,
    val entryId: String,
    val entityType: EntityType,
    val data: String,
    val timestamp: Long,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val status: SyncStatus,
    val errorMessage: String? = null,
    val lastAttemptAt: Long? = null
)

/**
 * Type of sync operation.
 */
enum class OperationType {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Entity type being synced.
 */
enum class EntityType {
    PASSWORD,
    FOLDER,
    TAG
}

/**
 * Status of a sync operation.
 */
enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
