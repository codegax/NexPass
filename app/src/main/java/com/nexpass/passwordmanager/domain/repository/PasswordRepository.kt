package com.nexpass.passwordmanager.domain.repository

import com.nexpass.passwordmanager.domain.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for password operations.
 *
 * This interface defines the contract for password data access.
 * Implementations handle encryption, storage, and retrieval.
 */
interface PasswordRepository {

    /**
     * Get all password entries as a Flow.
     */
    fun getAllFlow(): Flow<List<PasswordEntry>>

    /**
     * Get all password entries (one-time query).
     */
    suspend fun getAll(): List<PasswordEntry>

    /**
     * Get a password entry by ID.
     */
    suspend fun getById(id: String): PasswordEntry?

    /**
     * Get password entries by folder.
     */
    fun getByFolder(folderId: String): Flow<List<PasswordEntry>>

    /**
     * Get favorite password entries.
     */
    fun getFavorites(): Flow<List<PasswordEntry>>

    /**
     * Search password entries.
     */
    fun search(query: String): Flow<List<PasswordEntry>>

    /**
     * Get quarantined entries (failed to decrypt).
     */
    fun getQuarantined(): Flow<List<PasswordEntry>>

    /**
     * Insert a new password entry.
     */
    suspend fun insert(entry: PasswordEntry)

    /**
     * Update an existing password entry.
     */
    suspend fun update(entry: PasswordEntry)

    /**
     * Delete a password entry.
     */
    suspend fun delete(id: String)

    /**
     * Delete all password entries.
     */
    suspend fun deleteAll()

    /**
     * Get count of entries.
     */
    suspend fun getCount(): Int
}
