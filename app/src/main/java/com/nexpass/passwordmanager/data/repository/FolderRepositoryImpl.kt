package com.nexpass.passwordmanager.data.repository

import com.nexpass.passwordmanager.data.local.dao.FolderDao
import com.nexpass.passwordmanager.data.local.mapper.FolderMapper
import com.nexpass.passwordmanager.domain.model.Folder
import com.nexpass.passwordmanager.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of FolderRepository.
 */
class FolderRepositoryImpl(
    private val folderDao: FolderDao,
    private val folderMapper: FolderMapper
) : FolderRepository {

    override fun getAllFlow(): Flow<List<Folder>> {
        return folderDao.getAllFlow().map { entities ->
            folderMapper.toDomainList(entities)
        }
    }

    override suspend fun getAll(): List<Folder> {
        val entities = folderDao.getAll()
        return folderMapper.toDomainList(entities)
    }

    override suspend fun getById(id: String): Folder? {
        val entity = folderDao.getById(id) ?: return null
        return folderMapper.toDomain(entity)
    }

    override fun getRootFolders(): Flow<List<Folder>> {
        return folderDao.getRootFolders().map { entities ->
            folderMapper.toDomainList(entities)
        }
    }

    override fun getChildFolders(parentId: String): Flow<List<Folder>> {
        return folderDao.getChildFolders(parentId).map { entities ->
            folderMapper.toDomainList(entities)
        }
    }

    override suspend fun insert(folder: Folder) {
        val entity = folderMapper.toEntity(folder)
        folderDao.insert(entity)
    }

    override suspend fun update(folder: Folder) {
        val entity = folderMapper.toEntity(folder)
        folderDao.update(entity)
    }

    override suspend fun delete(id: String) {
        folderDao.deleteById(id)
    }

    override suspend fun getCount(): Int {
        return folderDao.getCount()
    }
}
