package com.nexpass.passwordmanager.data.network.mapper

import com.nexpass.passwordmanager.data.network.dto.CreateFolderRequest
import com.nexpass.passwordmanager.data.network.dto.NextcloudFolderDto
import com.nexpass.passwordmanager.data.network.dto.UpdateFolderRequest
import com.nexpass.passwordmanager.domain.model.Folder

/**
 * Mapper to convert between Nextcloud Folder API DTOs and domain models.
 *
 * Handles:
 * - DTO to domain conversion
 * - Domain to create/update request conversion
 * - Conflict detection and resolution
 */
object NextcloudFolderMapper {

    private const val DEFAULT_FOLDER_ID = "00000000-0000-0000-0000-000000000000"

    /**
     * Convert Nextcloud folder DTO to domain model.
     */
    fun toDomain(dto: NextcloudFolderDto): Folder {
        return Folder(
            id = dto.id,
            name = dto.label,
            parentId = if (dto.parent == DEFAULT_FOLDER_ID) null else dto.parent,
            createdAt = dto.created * 1000,  // Convert seconds to milliseconds
            updatedAt = dto.updated * 1000,
            lastModified = dto.edited * 1000,
            revisionId = dto.revision.ifEmpty { null }
        )
    }

    /**
     * Convert domain folder to create request.
     */
    fun toCreateRequest(folder: Folder): CreateFolderRequest {
        return CreateFolderRequest(
            label = folder.name,
            parent = folder.parentId ?: DEFAULT_FOLDER_ID,
            hidden = false
        )
    }

    /**
     * Convert domain folder to update request.
     */
    fun toUpdateRequest(folder: Folder): UpdateFolderRequest {
        return UpdateFolderRequest(
            id = folder.id,
            label = folder.name,
            parent = folder.parentId ?: DEFAULT_FOLDER_ID,
            hidden = false
        )
    }

    /**
     * Check if folder has conflict (both modified since last sync).
     */
    fun hasConflict(local: Folder, remote: NextcloudFolderDto, lastSyncTime: Long): Boolean {
        val localModifiedSinceSync = local.updatedAt > lastSyncTime
        val remoteModifiedSinceSync = remote.edited * 1000 > lastSyncTime
        return localModifiedSinceSync && remoteModifiedSinceSync
    }

    /**
     * Resolve conflict using Last-Write-Wins strategy.
     * Returns the folder that should be kept based on most recent modification time.
     */
    fun resolveConflictLastWriteWins(local: Folder, remote: NextcloudFolderDto): Folder {
        return if (local.updatedAt > remote.edited * 1000) {
            // Local is newer, keep local
            local
        } else {
            // Remote is newer, use remote
            toDomain(remote)
        }
    }

    /**
     * Merge remote folder with local folder for sync.
     * This preserves local creation timestamp but uses remote data.
     */
    fun mergeWithLocal(remote: NextcloudFolderDto, local: Folder): Folder {
        return Folder(
            id = remote.id,
            name = remote.label,
            parentId = if (remote.parent == DEFAULT_FOLDER_ID) null else remote.parent,
            createdAt = local.createdAt,  // Preserve local creation time
            updatedAt = System.currentTimeMillis(),
            lastModified = remote.edited * 1000,
            revisionId = remote.revision.ifEmpty { null }
        )
    }
}
