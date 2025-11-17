package com.nexpass.passwordmanager.domain.repository

import com.nexpass.passwordmanager.domain.model.Folder
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for folder operations.
 */
interface FolderRepository {

    /**
     * Get all folders as a Flow.
     */
    fun getAllFlow(): Flow<List<Folder>>

    /**
     * Get all folders (one-time query).
     */
    suspend fun getAll(): List<Folder>

    /**
     * Get a folder by ID.
     */
    suspend fun getById(id: String): Folder?

    /**
     * Get root folders.
     */
    fun getRootFolders(): Flow<List<Folder>>

    /**
     * Get child folders.
     */
    fun getChildFolders(parentId: String): Flow<List<Folder>>

    /**
     * Insert a new folder.
     */
    suspend fun insert(folder: Folder)

    /**
     * Update an existing folder.
     */
    suspend fun update(folder: Folder)

    /**
     * Delete a folder.
     */
    suspend fun delete(id: String)

    /**
     * Get count of folders.
     */
    suspend fun getCount(): Int
}
