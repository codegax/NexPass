package com.nexpass.passwordmanager.data.local.dao

import androidx.room.*
import com.nexpass.passwordmanager.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for sync operations.
 */
@Dao
interface SyncOperationDao {

    @Query("SELECT * FROM sync_operations ORDER BY timestamp ASC")
    fun getAllFlow(): Flow<List<SyncOperationEntity>>

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun getPending(): Flow<List<SyncOperationEntity>>

    @Query("SELECT * FROM sync_operations WHERE status = 'FAILED' AND retryCount < maxRetries ORDER BY timestamp ASC")
    fun getRetriable(): Flow<List<SyncOperationEntity>>

    @Query("SELECT * FROM sync_operations WHERE status = :status")
    fun getByStatus(status: String): Flow<List<SyncOperationEntity>>

    @Query("SELECT * FROM sync_operations WHERE id = :id")
    suspend fun getById(id: String): SyncOperationEntity?

    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int

    @Query("DELETE FROM sync_operations WHERE status = :status")
    suspend fun deleteByStatus(status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncOperationEntity)

    @Update
    suspend fun update(operation: SyncOperationEntity)

    @Query("UPDATE sync_operations SET status = 'COMPLETED' WHERE id = :id")
    suspend fun markCompleted(id: String)

    @Query("UPDATE sync_operations SET status = 'FAILED', errorMessage = :errorMessage, retryCount = retryCount + 1, lastAttemptAt = :timestamp WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String, timestamp: Long)

    @Query("UPDATE sync_operations SET status = 'IN_PROGRESS' WHERE id = :id")
    suspend fun markInProgress(id: String)

    @Query("DELETE FROM sync_operations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sync_operations WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM sync_operations")
    suspend fun deleteAll()
}
