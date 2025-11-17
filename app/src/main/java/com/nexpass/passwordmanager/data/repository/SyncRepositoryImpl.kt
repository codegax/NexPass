package com.nexpass.passwordmanager.data.repository

import android.util.Log
import com.nexpass.passwordmanager.data.local.dao.PasswordDao
import com.nexpass.passwordmanager.data.local.dao.SyncOperationDao
import com.nexpass.passwordmanager.data.local.entity.SyncOperationEntity
import com.nexpass.passwordmanager.data.local.mapper.PasswordMapper
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.data.network.NextcloudApiClient
import com.nexpass.passwordmanager.data.network.mapper.NextcloudPasswordMapper
import com.nexpass.passwordmanager.data.network.mapper.NextcloudFolderMapper
import com.nexpass.passwordmanager.domain.model.*
import com.nexpass.passwordmanager.domain.repository.SyncRepository
import com.nexpass.passwordmanager.security.vault.VaultKeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.UUID

/**
 * Implementation of SyncRepository for Nextcloud synchronization.
 *
 * Handles:
 * - Full and incremental sync
 * - Last-write-wins conflict resolution
 * - Offline pending queue
 * - Sync status tracking
 */
class SyncRepositoryImpl(
    private val nextcloudApiClient: NextcloudApiClient,
    private val passwordDao: PasswordDao,
    private val syncOperationDao: SyncOperationDao,
    private val passwordMapper: PasswordMapper,
    private val vaultKeyManager: VaultKeyManager,
    private val securePreferences: SecurePreferences,
    private val folderRepository: com.nexpass.passwordmanager.domain.repository.FolderRepository
) : SyncRepository {

    companion object {
        private const val TAG = "SyncRepositoryImpl"
    }

    private val _syncStatus = MutableStateFlow<SyncEngineStatus>(SyncEngineStatus.Idle)
    private val json = Json { ignoreUnknownKeys = true }

    override fun observeSyncStatus(): Flow<SyncEngineStatus> = _syncStatus.asStateFlow()

    override suspend fun testConnection(): Result<Boolean> {
        return nextcloudApiClient.testConnection()
    }

    override suspend fun getLastSyncTimestamp(): Long {
        return securePreferences.getLastSyncTimestamp()
    }

    override suspend fun performFullSync(): SyncResult {
        if (!securePreferences.isNextcloudConfigured()) {
            return SyncResult.Error(
                SyncErrorType.AUTHENTICATION,
                "Nextcloud not configured"
            )
        }

        _syncStatus.value = SyncEngineStatus.Syncing(0)

        return try {
            // 1. Sync folders FIRST (passwords reference folders)
            val folderSyncResult = syncFolders()
            if (folderSyncResult.isFailure) {
                Log.w(TAG, "Folder sync failed, continuing with password sync", folderSyncResult.exceptionOrNull())
                // Don't fail the entire sync if just folders failed
            }

            val foldersSynced = folderSyncResult.getOrDefault(0)
            Log.d(TAG, "Synced $foldersSynced folders")

            // 2. Process pending queue (folders and passwords)
            processPendingQueue()
            processPendingFolderQueue()

            // 3. Fetch all remote passwords
            val remoteResult = nextcloudApiClient.listPasswords()
            if (remoteResult.isFailure) {
                _syncStatus.value = SyncEngineStatus.Error("Failed to fetch remote passwords")
                return SyncResult.Error(
                    SyncErrorType.NETWORK,
                    remoteResult.exceptionOrNull()?.message ?: "Unknown error"
                )
            }

            val remotePasswords = remoteResult.getOrNull() ?: emptyList()
            val vaultKey = vaultKeyManager.getVaultKey()
                ?: return SyncResult.Error(
                    SyncErrorType.ENCRYPTION,
                    "Vault is locked"
                )

            val localPasswords = passwordDao.getAll().map { entity ->
                passwordMapper.toDomain(entity, vaultKey)
            }

            var pulledCount = 0
            var pushedCount = 0
            var conflictCount = 0

            val lastSyncTime = securePreferences.getLastSyncTimestamp()

            // 3. Process each remote password
            for (remoteDto in remotePasswords) {
                val localEntry = localPasswords.find { it.id == remoteDto.id }

                if (localEntry == null) {
                    // Remote entry doesn't exist locally - pull it
                    val domainEntry = NextcloudPasswordMapper.toDomain(remoteDto)
                    val entity = passwordMapper.toEntity(domainEntry, vaultKey)
                    passwordDao.insert(entity)
                    pulledCount++
                } else {
                    // Entry exists both locally and remotely - check for conflicts
                    if (NextcloudPasswordMapper.hasConflict(localEntry, remoteDto, lastSyncTime)) {
                        // Conflict - resolve using last-write-wins
                        val resolved = NextcloudPasswordMapper.resolveConflictLastWriteWins(
                            localEntry,
                            remoteDto
                        )
                        val entity = passwordMapper.toEntity(resolved, vaultKey)
                        passwordDao.update(entity)
                        conflictCount++

                        // If local was newer, push to server
                        if (resolved.id == localEntry.id && localEntry.updatedAt > remoteDto.edited * 1000) {
                            val updateRequest = NextcloudPasswordMapper.toUpdateRequest(resolved)
                            nextcloudApiClient.updatePassword(updateRequest)
                            pushedCount++
                        }
                    } else if (remoteDto.edited * 1000 > localEntry.updatedAt) {
                        // Remote is newer, no conflict - pull
                        val merged = NextcloudPasswordMapper.mergeWithLocal(remoteDto, localEntry)
                        val entity = passwordMapper.toEntity(merged, vaultKey)
                        passwordDao.update(entity)
                        pulledCount++
                    }
                }
            }

            // 4. Check for local entries that don't exist remotely (push them)
            val remoteIds = remotePasswords.map { it.id }.toSet()
            for (localEntry in localPasswords) {
                if (localEntry.id !in remoteIds && !localEntry.isQuarantined) {
                    // Local entry not on server - push it
                    val createRequest = NextcloudPasswordMapper.toCreateRequest(localEntry)
                    nextcloudApiClient.createPassword(createRequest)
                    pushedCount++
                }
            }

            // 5. Update last sync timestamp
            securePreferences.setLastSyncTimestamp(System.currentTimeMillis())

            val totalSynced = foldersSynced + pulledCount + pushedCount

            _syncStatus.value = SyncEngineStatus.Success(
                System.currentTimeMillis(),
                totalSynced
            )

            Log.d(TAG, "Full sync completed: $foldersSynced folders, $pulledCount pulled, $pushedCount pushed, $conflictCount conflicts")

            SyncResult.Success(pulledCount, pushedCount, conflictCount)
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
            _syncStatus.value = SyncEngineStatus.Error(e.message ?: "Unknown error")
            SyncResult.Error(SyncErrorType.UNKNOWN, e.message ?: "Unknown error", e)
        }
    }

    override suspend fun performIncrementalSync(): SyncResult {
        // For now, incremental sync is the same as full sync
        // In the future, we can optimize by only fetching changes since lastSyncTime
        return performFullSync()
    }

    override suspend fun queueLocalChange(
        entry: PasswordEntry,
        operation: SyncOperationType
    ): Result<Unit> {
        return try {
            val operationEntity = SyncOperationEntity(
                id = UUID.randomUUID().toString(),
                type = operation.name,
                entryId = entry.id,
                entityType = "PASSWORD",
                data = json.encodeToString(entry),
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                maxRetries = 3,
                status = SyncStatus.PENDING.name
            )

            syncOperationDao.insert(operationEntity)

            // If online and sync enabled, process immediately
            if (securePreferences.isSyncEnabled()) {
                processPendingQueue()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue local change", e)
            Result.failure(e)
        }
    }

    override suspend fun processPendingQueue(): Result<Int> {
        if (!securePreferences.isNextcloudConfigured() || !securePreferences.isSyncEnabled()) {
            return Result.success(0)
        }

        return try {
            var processedCount = 0

            // Get pending operations using firstOrNull to get a single emission
            val pendingOperations = syncOperationDao.getPending().firstOrNull() ?: emptyList()

            for (op in pendingOperations) {
                if (op.entityType == "PASSWORD") {
                    val success = processPasswordSyncOperation(op)
                    if (success) {
                        syncOperationDao.markCompleted(op.id)
                        processedCount++
                    } else {
                        syncOperationDao.markFailed(
                            op.id,
                            "Failed to sync",
                            System.currentTimeMillis()
                        )
                    }
                }
            }

            Result.success(processedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process pending queue", e)
            Result.failure(e)
        }
    }

    private suspend fun processPasswordSyncOperation(op: SyncOperationEntity): Boolean {
        return try {
            val entry: PasswordEntry = json.decodeFromString(op.data)

            when (SyncOperationType.valueOf(op.type)) {
                SyncOperationType.CREATE -> {
                    val request = NextcloudPasswordMapper.toCreateRequest(entry)
                    nextcloudApiClient.createPassword(request).isSuccess
                }
                SyncOperationType.UPDATE -> {
                    val request = NextcloudPasswordMapper.toUpdateRequest(entry)
                    nextcloudApiClient.updatePassword(request).isSuccess
                }
                SyncOperationType.DELETE -> {
                    nextcloudApiClient.deletePassword(entry.id).isSuccess
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process sync operation ${op.id}", e)
            false
        }
    }

    override suspend fun clearSyncData(): Result<Unit> {
        return try {
            syncOperationDao.deleteAll()
            securePreferences.clearNextcloudConfig()
            _syncStatus.value = SyncEngineStatus.NotConfigured
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear sync data", e)
            Result.failure(e)
        }
    }

    // ========== Folder Sync Implementation ==========

    override suspend fun syncFolders(): Result<Int> {
        if (!securePreferences.isNextcloudConfigured()) {
            return Result.success(0)
        }

        return try {
            Log.d(TAG, "Starting folder sync")

            // 1. Fetch all remote folders
            val remoteFoldersResult = nextcloudApiClient.listFolders()
            if (remoteFoldersResult.isFailure) {
                Log.e(TAG, "Failed to fetch remote folders", remoteFoldersResult.exceptionOrNull())
                return Result.failure(remoteFoldersResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val remoteFolders = remoteFoldersResult.getOrNull() ?: emptyList()
            val localFolders = folderRepository.getAll()
            val lastSyncTime = securePreferences.getLastSyncTimestamp()

            var syncCount = 0

            // 2. Download remote folders (create or update local)
            for (remoteDto in remoteFolders) {
                val localFolder = localFolders.find { it.id == remoteDto.id }

                if (localFolder == null) {
                    // New remote folder - insert locally
                    val folder = NextcloudFolderMapper.toDomain(remoteDto)
                    folderRepository.insert(folder)
                    Log.d(TAG, "Inserted new folder from remote: ${folder.name}")
                    syncCount++
                } else {
                    // Folder exists locally - check for conflicts
                    if (NextcloudFolderMapper.hasConflict(localFolder, remoteDto, lastSyncTime)) {
                        // Conflict - resolve using Last-Write-Wins
                        val resolved = NextcloudFolderMapper.resolveConflictLastWriteWins(localFolder, remoteDto)
                        folderRepository.update(resolved)
                        Log.d(TAG, "Resolved folder conflict: ${resolved.name}")
                        syncCount++

                        // If local was newer, push to server
                        if (resolved.id == localFolder.id && localFolder.updatedAt > remoteDto.edited * 1000) {
                            val updateRequest = NextcloudFolderMapper.toUpdateRequest(resolved)
                            nextcloudApiClient.updateFolder(updateRequest)
                        }
                    } else if (remoteDto.edited * 1000 > localFolder.updatedAt) {
                        // Remote is newer, no conflict - update local
                        val merged = NextcloudFolderMapper.mergeWithLocal(remoteDto, localFolder)
                        folderRepository.update(merged)
                        Log.d(TAG, "Updated folder from remote: ${merged.name}")
                        syncCount++
                    }
                }
            }

            // 3. Upload local folders that don't exist remotely
            val remoteFolderIds = remoteFolders.map { it.id }.toSet()
            for (localFolder in localFolders) {
                if (localFolder.id !in remoteFolderIds) {
                    // Local folder doesn't exist on server - create it
                    val createRequest = NextcloudFolderMapper.toCreateRequest(localFolder)
                    val createResult = nextcloudApiClient.createFolder(createRequest)

                    if (createResult.isSuccess) {
                        val created = createResult.getOrNull()
                        created?.let {
                            // Update local with server-generated metadata
                            val updated = localFolder.copy(
                                revisionId = it.revision,
                                lastModified = it.edited * 1000
                            )
                            folderRepository.update(updated)
                            Log.d(TAG, "Created folder on remote: ${localFolder.name}")
                            syncCount++
                        }
                    } else {
                        Log.e(TAG, "Failed to create folder on remote: ${localFolder.name}", createResult.exceptionOrNull())
                    }
                }
            }

            Log.d(TAG, "Folder sync completed: $syncCount folders synced")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Folder sync failed", e)
            Result.failure(e)
        }
    }

    override suspend fun queueFolderChange(
        folder: Folder,
        operation: SyncOperationType
    ): Result<Unit> {
        return try {
            val operationEntity = SyncOperationEntity(
                id = UUID.randomUUID().toString(),
                type = operation.name,
                entryId = folder.id,
                entityType = "FOLDER",
                data = json.encodeToString(folder),
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                maxRetries = 3,
                status = SyncStatus.PENDING.name
            )

            syncOperationDao.insert(operationEntity)
            Log.d(TAG, "Queued folder change: ${operation.name} for ${folder.name}")

            // If online and sync enabled, process immediately
            if (securePreferences.isSyncEnabled()) {
                processPendingFolderQueue()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue folder change", e)
            Result.failure(e)
        }
    }

    override suspend fun processPendingFolderQueue(): Result<Int> {
        if (!securePreferences.isNextcloudConfigured() || !securePreferences.isSyncEnabled()) {
            return Result.success(0)
        }

        return try {
            var processedCount = 0

            // Get pending folder operations
            val pendingOperations = syncOperationDao.getPending().firstOrNull() ?: emptyList()
            val folderOperations = pendingOperations.filter { it.entityType == "FOLDER" }

            for (op in folderOperations) {
                val success = processFolderSyncOperation(op)
                if (success) {
                    syncOperationDao.markCompleted(op.id)
                    processedCount++
                    Log.d(TAG, "Processed folder sync operation: ${op.type}")
                } else {
                    syncOperationDao.markFailed(
                        op.id,
                        "Failed to sync folder",
                        System.currentTimeMillis()
                    )
                    Log.e(TAG, "Failed to process folder sync operation: ${op.type}")
                }
            }

            Result.success(processedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process pending folder queue", e)
            Result.failure(e)
        }
    }

    private suspend fun processFolderSyncOperation(op: SyncOperationEntity): Boolean {
        return try {
            val folder: Folder = json.decodeFromString(op.data)

            when (SyncOperationType.valueOf(op.type)) {
                SyncOperationType.CREATE -> {
                    val request = NextcloudFolderMapper.toCreateRequest(folder)
                    nextcloudApiClient.createFolder(request).isSuccess
                }
                SyncOperationType.UPDATE -> {
                    val request = NextcloudFolderMapper.toUpdateRequest(folder)
                    nextcloudApiClient.updateFolder(request).isSuccess
                }
                SyncOperationType.DELETE -> {
                    nextcloudApiClient.deleteFolder(folder.id).isSuccess
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process folder sync operation ${op.id}", e)
            false
        }
    }
}
