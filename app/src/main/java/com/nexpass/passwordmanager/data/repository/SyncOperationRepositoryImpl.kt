package com.nexpass.passwordmanager.data.repository

import com.nexpass.passwordmanager.data.local.dao.SyncOperationDao
import com.nexpass.passwordmanager.data.local.mapper.SyncOperationMapper
import com.nexpass.passwordmanager.domain.model.SyncOperation
import com.nexpass.passwordmanager.domain.model.SyncStatus
import com.nexpass.passwordmanager.domain.repository.SyncOperationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of SyncOperationRepository.
 */
class SyncOperationRepositoryImpl(
    private val syncOperationDao: SyncOperationDao,
    private val syncOperationMapper: SyncOperationMapper
) : SyncOperationRepository {

    override fun getAllFlow(): Flow<List<SyncOperation>> {
        return syncOperationDao.getAllFlow().map { entities ->
            syncOperationMapper.toDomainList(entities)
        }
    }

    override fun getPending(): Flow<List<SyncOperation>> {
        return syncOperationDao.getPending().map { entities ->
            syncOperationMapper.toDomainList(entities)
        }
    }

    override fun getRetriable(): Flow<List<SyncOperation>> {
        return syncOperationDao.getRetriable().map { entities ->
            syncOperationMapper.toDomainList(entities)
        }
    }

    override fun getByStatus(status: SyncStatus): Flow<List<SyncOperation>> {
        return syncOperationDao.getByStatus(status.name).map { entities ->
            syncOperationMapper.toDomainList(entities)
        }
    }

    override suspend fun getById(id: String): SyncOperation? {
        val entity = syncOperationDao.getById(id) ?: return null
        return syncOperationMapper.toDomain(entity)
    }

    override suspend fun insert(operation: SyncOperation) {
        val entity = syncOperationMapper.toEntity(operation)
        syncOperationDao.insert(entity)
    }

    override suspend fun update(operation: SyncOperation) {
        val entity = syncOperationMapper.toEntity(operation)
        syncOperationDao.update(entity)
    }

    override suspend fun delete(id: String) {
        syncOperationDao.deleteById(id)
    }

    override suspend fun deleteCompleted() {
        syncOperationDao.deleteByStatus(SyncStatus.COMPLETED.name)
    }

    override suspend fun getPendingCount(): Int {
        return syncOperationDao.getCountByStatus(SyncStatus.PENDING.name)
    }
}
