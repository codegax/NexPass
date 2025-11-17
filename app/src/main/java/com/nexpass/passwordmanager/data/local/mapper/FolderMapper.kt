package com.nexpass.passwordmanager.data.local.mapper

import com.nexpass.passwordmanager.data.local.entity.FolderEntity
import com.nexpass.passwordmanager.domain.model.Folder

/**
 * Mapper for Folder (no encryption needed).
 */
object FolderMapper {

    fun toEntity(folder: Folder): FolderEntity {
        return FolderEntity(
            id = folder.id,
            name = folder.name,
            parentId = folder.parentId,
            createdAt = folder.createdAt,
            updatedAt = folder.updatedAt,
            lastModified = folder.lastModified,
            revisionId = folder.revisionId
        )
    }

    fun toDomain(entity: FolderEntity): Folder {
        return Folder(
            id = entity.id,
            name = entity.name,
            parentId = entity.parentId,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            lastModified = entity.lastModified,
            revisionId = entity.revisionId
        )
    }

    fun toDomainList(entities: List<FolderEntity>): List<Folder> {
        return entities.map { toDomain(it) }
    }
}
