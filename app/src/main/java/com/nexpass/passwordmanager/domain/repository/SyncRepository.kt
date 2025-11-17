package com.nexpass.passwordmanager.domain.repository

import com.nexpass.passwordmanager.domain.model.Folder
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.model.SyncOperationType
import com.nexpass.passwordmanager.domain.model.SyncResult
import com.nexpass.passwordmanager.domain.model.SyncEngineStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for synchronization operations with Nextcloud.
 *
 * Handles:
 * - Full and incremental sync
 * - Conflict resolution
 * - Pending queue management
 * - Sync status tracking
 */
interface SyncRepository {
    /**
     * Perform a full synchronization with Nextcloud.
     * Downloads all remote passwords and reconciles with local vault.
     */
    suspend fun performFullSync(): SyncResult

    /**
     * Perform an incremental synchronization.
     * Only syncs changes since the last sync timestamp.
     */
    suspend fun performIncrementalSync(): SyncResult

    /**
     * Queue a local change for sync when online.
     */
    suspend fun queueLocalChange(
        entry: PasswordEntry,
        operation: SyncOperationType
    ): Result<Unit>

    /**
     * Process all pending sync operations in the queue.
     */
    suspend fun processPendingQueue(): Result<Int>

    /**
     * Observe the current sync status.
     */
    fun observeSyncStatus(): Flow<SyncEngineStatus>

    /**
     * Test the connection to Nextcloud server.
     */
    suspend fun testConnection(): Result<Boolean>

    /**
     * Get the last sync timestamp.
     */
    suspend fun getLastSyncTimestamp(): Long

    /**
     * Clear all sync-related data.
     */
    suspend fun clearSyncData(): Result<Unit>

    // ========== Folder Sync Methods ==========

    /**
     * Perform full folder synchronization with Nextcloud.
     * Downloads all remote folders and reconciles with local folders.
     */
    suspend fun syncFolders(): Result<Int>

    /**
     * Queue a local folder change for sync when online.
     */
    suspend fun queueFolderChange(
        folder: Folder,
        operation: SyncOperationType
    ): Result<Unit>

    /**
     * Process pending folder sync operations.
     */
    suspend fun processPendingFolderQueue(): Result<Int>
}
