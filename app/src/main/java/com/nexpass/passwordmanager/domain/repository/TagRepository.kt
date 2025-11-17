package com.nexpass.passwordmanager.domain.repository

import com.nexpass.passwordmanager.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for tag operations.
 */
interface TagRepository {

    /**
     * Get all tags as a Flow.
     */
    fun getAllFlow(): Flow<List<Tag>>

    /**
     * Get all tags (one-time query).
     */
    suspend fun getAll(): List<Tag>

    /**
     * Get a tag by ID.
     */
    suspend fun getById(id: String): Tag?

    /**
     * Get tag by name.
     */
    suspend fun getByName(name: String): Tag?

    /**
     * Search tags.
     */
    fun search(query: String): Flow<List<Tag>>

    /**
     * Insert a new tag.
     */
    suspend fun insert(tag: Tag)

    /**
     * Update an existing tag.
     */
    suspend fun update(tag: Tag)

    /**
     * Delete a tag.
     */
    suspend fun delete(id: String)

    /**
     * Get count of tags.
     */
    suspend fun getCount(): Int
}
