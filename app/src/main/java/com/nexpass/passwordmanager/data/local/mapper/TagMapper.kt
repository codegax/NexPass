package com.nexpass.passwordmanager.data.local.mapper

import com.nexpass.passwordmanager.data.local.entity.TagEntity
import com.nexpass.passwordmanager.domain.model.Tag

/**
 * Mapper for Tag (no encryption needed).
 */
object TagMapper {

    fun toEntity(tag: Tag): TagEntity {
        return TagEntity(
            id = tag.id,
            name = tag.name,
            color = tag.color,
            createdAt = tag.createdAt,
            updatedAt = tag.updatedAt,
            lastModified = tag.lastModified,
            revisionId = tag.revisionId
        )
    }

    fun toDomain(entity: TagEntity): Tag {
        return Tag(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            lastModified = entity.lastModified,
            revisionId = entity.revisionId
        )
    }

    fun toDomainList(entities: List<TagEntity>): List<Tag> {
        return entities.map { toDomain(it) }
    }
}
