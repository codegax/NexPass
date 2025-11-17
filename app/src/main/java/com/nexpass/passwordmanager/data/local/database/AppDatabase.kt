package com.nexpass.passwordmanager.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexpass.passwordmanager.data.local.dao.FolderDao
import com.nexpass.passwordmanager.data.local.dao.PasswordDao
import com.nexpass.passwordmanager.data.local.dao.SyncOperationDao
import com.nexpass.passwordmanager.data.local.dao.TagDao
import com.nexpass.passwordmanager.data.local.entity.FolderEntity
import com.nexpass.passwordmanager.data.local.entity.PasswordEntryEntity
import com.nexpass.passwordmanager.data.local.entity.SyncOperationEntity
import com.nexpass.passwordmanager.data.local.entity.TagEntity

/**
 * Main Room database for NexPass.
 *
 * This database is encrypted using SQLCipher for maximum security.
 * All password data is doubly encrypted:
 * 1. Individual fields encrypted with CryptoManager
 * 2. Entire database encrypted with SQLCipher
 *
 * Security:
 * - SQLCipher encryption at rest
 * - AES-256 encryption for sensitive fields
 * - No sensitive data in clear text
 */
@Database(
    entities = [
        PasswordEntryEntity::class,
        FolderEntity::class,
        TagEntity::class,
        SyncOperationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun passwordDao(): PasswordDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun syncOperationDao(): SyncOperationDao

    companion object {
        const val DATABASE_NAME = "nexpass_vault.db"
    }
}
