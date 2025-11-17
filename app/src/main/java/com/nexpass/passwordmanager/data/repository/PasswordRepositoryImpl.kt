package com.nexpass.passwordmanager.data.repository

import com.nexpass.passwordmanager.data.local.dao.PasswordDao
import com.nexpass.passwordmanager.data.local.mapper.PasswordMapper
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of PasswordRepository.
 *
 * Handles encryption/decryption and database operations for password entries.
 *
 * Note: This requires a vault key to be provided for encryption/decryption.
 * The vault key should be stored in memory after user authentication.
 */
class PasswordRepositoryImpl(
    private val passwordDao: PasswordDao,
    private val passwordMapper: PasswordMapper,
    private val vaultKeyProvider: () -> ByteArray
) : PasswordRepository {

    override fun getAllFlow(): Flow<List<PasswordEntry>> {
        return passwordDao.getAllFlow().map { entities ->
            val vaultKey = vaultKeyProvider()
            passwordMapper.toDomainList(entities, vaultKey)
        }
    }

    override suspend fun getAll(): List<PasswordEntry> {
        val entities = passwordDao.getAll()
        val vaultKey = vaultKeyProvider()
        return passwordMapper.toDomainList(entities, vaultKey)
    }

    override suspend fun getById(id: String): PasswordEntry? {
        val entity = passwordDao.getById(id) ?: return null
        val vaultKey = vaultKeyProvider()
        return passwordMapper.toDomain(entity, vaultKey)
    }

    override fun getByFolder(folderId: String): Flow<List<PasswordEntry>> {
        return passwordDao.getByFolder(folderId).map { entities ->
            val vaultKey = vaultKeyProvider()
            passwordMapper.toDomainList(entities, vaultKey)
        }
    }

    override fun getFavorites(): Flow<List<PasswordEntry>> {
        return passwordDao.getFavorites().map { entities ->
            val vaultKey = vaultKeyProvider()
            passwordMapper.toDomainList(entities, vaultKey)
        }
    }

    override fun search(query: String): Flow<List<PasswordEntry>> {
        return passwordDao.search(query).map { entities ->
            val vaultKey = vaultKeyProvider()
            passwordMapper.toDomainList(entities, vaultKey)
        }
    }

    override fun getQuarantined(): Flow<List<PasswordEntry>> {
        return passwordDao.getQuarantined().map { entities ->
            // Don't decrypt quarantined entries
            entities.map { entity ->
                PasswordEntry(
                    id = entity.id,
                    title = entity.title,
                    username = entity.username,
                    password = "[ENCRYPTED]",
                    url = entity.url,
                    notes = "[ENCRYPTED]",
                    folderId = entity.folderId,
                    tags = entity.tags.split(",").filter { it.isNotBlank() },
                    packageNames = entity.packageNames.split(",").filter { it.isNotBlank() },
                    favorite = entity.favorite,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    lastModified = entity.lastModified,
                    isQuarantined = true,
                    revisionId = entity.revisionId
                )
            }
        }
    }

    override suspend fun insert(entry: PasswordEntry) {
        val vaultKey = vaultKeyProvider()
        val entity = passwordMapper.toEntity(entry, vaultKey)
        passwordDao.insert(entity)
    }

    override suspend fun update(entry: PasswordEntry) {
        val vaultKey = vaultKeyProvider()
        val entity = passwordMapper.toEntity(entry, vaultKey)
        passwordDao.update(entity)
    }

    override suspend fun delete(id: String) {
        passwordDao.deleteById(id)
    }

    override suspend fun deleteAll() {
        passwordDao.deleteAll()
    }

    override suspend fun getCount(): Int {
        return passwordDao.getCount()
    }
}
