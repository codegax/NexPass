package com.nexpass.passwordmanager.data.local.dao

import androidx.room.*
import com.nexpass.passwordmanager.data.local.entity.PasswordEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for password entries.
 *
 * Provides CRUD operations and queries for password entries in the local database.
 * All operations return Flow for reactive updates.
 */
@Dao
interface PasswordDao {

    /**
     * Get all password entries as a Flow.
     * Automatically updates when data changes.
     */
    @Query("SELECT * FROM password_entries WHERE isQuarantined = 0 ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<PasswordEntryEntity>>

    /**
     * Get all password entries (one-time query).
     */
    @Query("SELECT * FROM password_entries WHERE isQuarantined = 0 ORDER BY updatedAt DESC")
    suspend fun getAll(): List<PasswordEntryEntity>

    /**
     * Get a specific password entry by ID.
     */
    @Query("SELECT * FROM password_entries WHERE id = :id")
    suspend fun getById(id: String): PasswordEntryEntity?

    /**
     * Get password entries by folder ID.
     */
    @Query("SELECT * FROM password_entries WHERE folderId = :folderId AND isQuarantined = 0 ORDER BY updatedAt DESC")
    fun getByFolder(folderId: String): Flow<List<PasswordEntryEntity>>

    /**
     * Get favorite password entries.
     */
    @Query("SELECT * FROM password_entries WHERE favorite = 1 AND isQuarantined = 0 ORDER BY updatedAt DESC")
    fun getFavorites(): Flow<List<PasswordEntryEntity>>

    /**
     * Search password entries by title or username.
     */
    @Query("SELECT * FROM password_entries WHERE (title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%') AND isQuarantined = 0 ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<PasswordEntryEntity>>

    /**
     * Get quarantined entries (failed to decrypt).
     */
    @Query("SELECT * FROM password_entries WHERE isQuarantined = 1")
    fun getQuarantined(): Flow<List<PasswordEntryEntity>>

    /**
     * Get entries modified after a specific timestamp (for sync).
     */
    @Query("SELECT * FROM password_entries WHERE updatedAt > :timestamp")
    suspend fun getModifiedAfter(timestamp: Long): List<PasswordEntryEntity>

    /**
     * Insert a new password entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PasswordEntryEntity)

    /**
     * Insert multiple password entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PasswordEntryEntity>)

    /**
     * Update an existing password entry.
     */
    @Update
    suspend fun update(entry: PasswordEntryEntity)

    /**
     * Update multiple password entries.
     */
    @Update
    suspend fun updateAll(entries: List<PasswordEntryEntity>)

    /**
     * Delete a password entry by ID.
     */
    @Query("DELETE FROM password_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete a password entry.
     */
    @Delete
    suspend fun delete(entry: PasswordEntryEntity)

    /**
     * Delete all password entries (use with caution!).
     */
    @Query("DELETE FROM password_entries")
    suspend fun deleteAll()

    /**
     * Get count of all entries.
     */
    @Query("SELECT COUNT(*) FROM password_entries WHERE isQuarantined = 0")
    suspend fun getCount(): Int

    /**
     * Mark an entry as quarantined (failed to decrypt).
     */
    @Query("UPDATE password_entries SET isQuarantined = 1 WHERE id = :id")
    suspend fun markAsQuarantined(id: String)
}
