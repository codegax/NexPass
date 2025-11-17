package com.nexpass.passwordmanager.domain.repository

import com.nexpass.passwordmanager.domain.model.SyncOperation
import com.nexpass.passwordmanager.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for sync operation tracking.
 */
interface SyncOperationRepository {

    /**
     * Get all sync operations as a Flow.
     */
    fun getAllFlow(): Flow<List<SyncOperation>>

    /**
     * Get pending sync operations.
     */
    fun getPending(): Flow<List<SyncOperation>>

    /**
     * Get failed sync operations that can be retried.
     */
    fun getRetriable(): Flow<List<SyncOperation>>

    /**
     * Get sync operations by status.
     */
    fun getByStatus(status: SyncStatus): Flow<List<SyncOperation>>

    /**
     * Get a sync operation by ID.
     */
    suspend fun getById(id: String): SyncOperation?

    /**
     * Insert a new sync operation.
     */
    suspend fun insert(operation: SyncOperation)

    /**
     * Update an existing sync operation.
     */
    suspend fun update(operation: SyncOperation)

    /**
     * Delete a sync operation.
     */
    suspend fun delete(id: String)

    /**
     * Delete all completed operations.
     */
    suspend fun deleteCompleted()

    /**
     * Get count of pending operations.
     */
    suspend fun getPendingCount(): Int
}
