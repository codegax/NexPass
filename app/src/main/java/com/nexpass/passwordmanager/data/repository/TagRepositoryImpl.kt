package com.nexpass.passwordmanager.data.repository

import com.nexpass.passwordmanager.data.local.dao.TagDao
import com.nexpass.passwordmanager.data.local.mapper.TagMapper
import com.nexpass.passwordmanager.domain.model.Tag
import com.nexpass.passwordmanager.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of TagRepository.
 */
class TagRepositoryImpl(
    private val tagDao: TagDao,
    private val tagMapper: TagMapper
) : TagRepository {

    override fun getAllFlow(): Flow<List<Tag>> {
        return tagDao.getAllFlow().map { entities ->
            tagMapper.toDomainList(entities)
        }
    }

    override suspend fun getAll(): List<Tag> {
        val entities = tagDao.getAll()
        return tagMapper.toDomainList(entities)
    }

    override suspend fun getById(id: String): Tag? {
        val entity = tagDao.getById(id) ?: return null
        return tagMapper.toDomain(entity)
    }

    override suspend fun getByName(name: String): Tag? {
        val entity = tagDao.getByName(name) ?: return null
        return tagMapper.toDomain(entity)
    }

    override fun search(query: String): Flow<List<Tag>> {
        return tagDao.search(query).map { entities ->
            tagMapper.toDomainList(entities)
        }
    }

    override suspend fun insert(tag: Tag) {
        val entity = tagMapper.toEntity(tag)
        tagDao.insert(entity)
    }

    override suspend fun update(tag: Tag) {
        val entity = tagMapper.toEntity(tag)
        tagDao.update(entity)
    }

    override suspend fun delete(id: String) {
        tagDao.deleteById(id)
    }

    override suspend fun getCount(): Int {
        return tagDao.getCount()
    }
}
