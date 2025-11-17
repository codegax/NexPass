package com.nexpass.passwordmanager.domain.model

/**
 * Result of a synchronization operation.
 */
sealed class SyncResult {
    /**
     * Synchronization completed successfully.
     */
    data class Success(
        val pulledCount: Int = 0,
        val pushedCount: Int = 0,
        val conflictCount: Int = 0
    ) : SyncResult()

    /**
     * Synchronization failed.
     */
    data class Error(
        val errorType: SyncErrorType,
        val message: String,
        val throwable: Throwable? = null
    ) : SyncResult()

    /**
     * Synchronization is in progress.
     */
    object InProgress : SyncResult()

    /**
     * Synchronization is idle (not running).
     */
    object Idle : SyncResult()
}

/**
 * Types of synchronization errors.
 */
enum class SyncErrorType {
    NETWORK,           // Network connectivity issues
    AUTHENTICATION,    // Invalid credentials
    ENCRYPTION,        // Encryption/decryption failure
    CONFLICT,          // Unresolvable conflict
    SERVER_ERROR,      // Server returned an error
    UNKNOWN            // Unknown error
}

/**
 * Status of the synchronization engine (UI level).
 */
sealed class SyncEngineStatus {
    /**
     * No sync activity.
     */
    object Idle : SyncEngineStatus()

    /**
     * Sync is currently running.
     */
    data class Syncing(val progress: Int = 0) : SyncEngineStatus()

    /**
     * Last sync was successful.
     */
    data class Success(
        val timestamp: Long,
        val passwordsUpdated: Int
    ) : SyncEngineStatus()

    /**
     * Last sync failed.
     */
    data class Error(val message: String) : SyncEngineStatus()

    /**
     * Not configured yet.
     */
    object NotConfigured : SyncEngineStatus()
}

/**
 * Types of sync operations that can be queued.
 */
enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE
}
