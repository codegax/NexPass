package com.nexpass.passwordmanager.data.network.mapper

import com.nexpass.passwordmanager.data.network.dto.CreatePasswordRequest
import com.nexpass.passwordmanager.data.network.dto.NextcloudPasswordDto
import com.nexpass.passwordmanager.data.network.dto.UpdatePasswordRequest
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import java.util.UUID

/**
 * Mapper to convert between Nextcloud API DTOs and domain models.
 *
 * Important: This mapper works with already encrypted data.
 * The password and notes fields in PasswordEntry should already be encrypted
 * before being passed to toCreateRequest or toUpdateRequest.
 */
object NextcloudPasswordMapper {

    /**
     * Convert Nextcloud DTO to domain model.
     * Note: The password and notes in the DTO are encrypted.
     */
    fun toDomain(dto: NextcloudPasswordDto): PasswordEntry {
        return PasswordEntry(
            id = dto.id,
            title = dto.label,
            username = dto.username,
            password = dto.password,  // Still encrypted
            url = dto.url.ifEmpty { null },
            notes = dto.notes.ifEmpty { null },  // Still encrypted
            folderId = if (dto.folder == "00000000-0000-0000-0000-000000000000") null else dto.folder,
            tags = dto.tags,
            packageNames = emptyList(),  // Nextcloud doesn't store package names
            favorite = dto.favorite,
            createdAt = dto.created * 1000,  // Convert to milliseconds
            updatedAt = dto.updated * 1000,
            lastModified = dto.edited * 1000,
            isQuarantined = false,
            revisionId = dto.revision
        )
    }

    /**
     * Convert domain model to Nextcloud create request.
     * Note: The password and notes should already be encrypted.
     */
    fun toCreateRequest(entry: PasswordEntry): CreatePasswordRequest {
        return CreatePasswordRequest(
            password = entry.password,  // Should be encrypted
            label = entry.title,
            username = entry.username,
            url = entry.url ?: "",
            notes = entry.notes ?: "",  // Should be encrypted
            folder = entry.folderId ?: "00000000-0000-0000-0000-000000000000",
            tags = entry.tags,
            favorite = entry.favorite,
            hidden = false,
            customFields = ""
        )
    }

    /**
     * Convert domain model to Nextcloud update request.
     * Note: The password and notes should already be encrypted.
     */
    fun toUpdateRequest(entry: PasswordEntry): UpdatePasswordRequest {
        return UpdatePasswordRequest(
            id = entry.id,
            password = entry.password,  // Should be encrypted
            label = entry.title,
            username = entry.username,
            url = entry.url ?: "",
            notes = entry.notes ?: "",  // Should be encrypted
            folder = entry.folderId ?: "00000000-0000-0000-0000-000000000000",
            tags = entry.tags,
            favorite = entry.favorite,
            hidden = false,
            customFields = ""
        )
    }

    /**
     * Merge remote DTO with local entry for conflict resolution.
     * This preserves local-only fields like packageNames.
     */
    fun mergeWithLocal(remote: NextcloudPasswordDto, local: PasswordEntry): PasswordEntry {
        return PasswordEntry(
            id = remote.id,
            title = remote.label,
            username = remote.username,
            password = remote.password,  // Encrypted
            url = remote.url.ifEmpty { null },
            notes = remote.notes.ifEmpty { null },  // Encrypted
            folderId = if (remote.folder == "00000000-0000-0000-0000-000000000000") null else remote.folder,
            tags = remote.tags,
            packageNames = local.packageNames,  // Preserve local package names
            favorite = remote.favorite,
            createdAt = local.createdAt,  // Preserve local creation time
            updatedAt = System.currentTimeMillis(),
            lastModified = remote.edited * 1000,
            isQuarantined = false,
            revisionId = remote.revision
        )
    }

    /**
     * Check if two entries have a conflict (both modified since last sync).
     */
    fun hasConflict(local: PasswordEntry, remote: NextcloudPasswordDto, lastSyncTime: Long): Boolean {
        val localModifiedSinceSync = local.updatedAt > lastSyncTime
        val remoteModifiedSinceSync = remote.edited * 1000 > lastSyncTime
        return localModifiedSinceSync && remoteModifiedSinceSync
    }

    /**
     * Resolve conflict using Last-Write-Wins strategy.
     * Returns the entry that should be kept.
     */
    fun resolveConflictLastWriteWins(local: PasswordEntry, remote: NextcloudPasswordDto): PasswordEntry {
        return if (local.updatedAt > remote.edited * 1000) {
            // Local is newer, keep local
            local
        } else {
            // Remote is newer, use remote but preserve local-only fields
            mergeWithLocal(remote, local)
        }
    }
}
