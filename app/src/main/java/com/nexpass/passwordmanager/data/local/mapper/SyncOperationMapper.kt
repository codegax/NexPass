package com.nexpass.passwordmanager.data.local.mapper

import com.nexpass.passwordmanager.data.local.entity.SyncOperationEntity
import com.nexpass.passwordmanager.domain.model.EntityType
import com.nexpass.passwordmanager.domain.model.OperationType
import com.nexpass.passwordmanager.domain.model.SyncOperation
import com.nexpass.passwordmanager.domain.model.SyncStatus

/**
 * Mapper for SyncOperation.
 */
object SyncOperationMapper {

    fun toEntity(operation: SyncOperation): SyncOperationEntity {
        return SyncOperationEntity(
            id = operation.id,
            type = operation.type.name,
            entryId = operation.entryId,
            entityType = operation.entityType.name,
            data = operation.data,
            timestamp = operation.timestamp,
            retryCount = operation.retryCount,
            maxRetries = operation.maxRetries,
            status = operation.status.name,
            errorMessage = operation.errorMessage,
            lastAttemptAt = operation.lastAttemptAt
        )
    }

    fun toDomain(entity: SyncOperationEntity): SyncOperation {
        return SyncOperation(
            id = entity.id,
            type = OperationType.valueOf(entity.type),
            entryId = entity.entryId,
            entityType = EntityType.valueOf(entity.entityType),
            data = entity.data,
            timestamp = entity.timestamp,
            retryCount = entity.retryCount,
            maxRetries = entity.maxRetries,
            status = SyncStatus.valueOf(entity.status),
            errorMessage = entity.errorMessage,
            lastAttemptAt = entity.lastAttemptAt
        )
    }

    fun toDomainList(entities: List<SyncOperationEntity>): List<SyncOperation> {
        return entities.map { toDomain(it) }
    }
}
