package com.nexpass.passwordmanager.di

import android.content.Context
import androidx.room.Room
import com.nexpass.passwordmanager.data.local.database.AppDatabase
import com.nexpass.passwordmanager.data.local.mapper.FolderMapper
import com.nexpass.passwordmanager.data.local.mapper.PasswordMapper
import com.nexpass.passwordmanager.data.local.mapper.SyncOperationMapper
import com.nexpass.passwordmanager.data.local.mapper.TagMapper
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.data.repository.FolderRepositoryImpl
import com.nexpass.passwordmanager.data.repository.PasswordRepositoryImpl
import com.nexpass.passwordmanager.data.repository.SyncOperationRepositoryImpl
import com.nexpass.passwordmanager.data.repository.SyncRepositoryImpl
import com.nexpass.passwordmanager.data.repository.TagRepositoryImpl
import com.nexpass.passwordmanager.domain.repository.FolderRepository
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import com.nexpass.passwordmanager.domain.repository.SyncOperationRepository
import com.nexpass.passwordmanager.domain.repository.SyncRepository
import com.nexpass.passwordmanager.domain.repository.TagRepository
import com.nexpass.passwordmanager.security.vault.VaultKeyManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Data Module - Data layer dependencies
 *
 * Provides:
 * - Room database instance
 * - DAOs
 * - Repository implementations
 * - Local data sources
 * - Encrypted SharedPreferences
 */
val dataModule = module {

    // ========== Database ==========

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // TODO: Add SQLCipher support when integrating encryption
            // .openHelperFactory(SupportFactory(SQLiteDatabase.getBytes(passphrase)))
            .fallbackToDestructiveMigration() // For development only
            .build()
    }

    // ========== DAOs ==========

    single { get<AppDatabase>().passwordDao() }
    single { get<AppDatabase>().folderDao() }
    single { get<AppDatabase>().tagDao() }
    single { get<AppDatabase>().syncOperationDao() }

    // ========== Mappers ==========

    single { PasswordMapper(get()) } // Requires CryptoManager
    single { FolderMapper }
    single { TagMapper }
    single { SyncOperationMapper }

    // ========== Repositories ==========

    single<PasswordRepository> {
        PasswordRepositoryImpl(
            passwordDao = get(),
            passwordMapper = get(),
            vaultKeyProvider = {
                // This is a synchronous call but requireVaultKey is suspend
                // We use runBlocking here since this is called from coroutine contexts
                kotlinx.coroutines.runBlocking {
                    get<VaultKeyManager>().requireVaultKey()
                }
            }
        )
    }

    single<FolderRepository> {
        FolderRepositoryImpl(
            folderDao = get(),
            folderMapper = get()
        )
    }

    single<TagRepository> {
        TagRepositoryImpl(
            tagDao = get(),
            tagMapper = get()
        )
    }

    single<SyncOperationRepository> {
        SyncOperationRepositoryImpl(
            syncOperationDao = get(),
            syncOperationMapper = get()
        )
    }

    single<SyncRepository> {
        SyncRepositoryImpl(
            nextcloudApiClient = get(),
            passwordDao = get(),
            syncOperationDao = get(),
            passwordMapper = get(),
            vaultKeyManager = get(),
            securePreferences = get(),
            folderRepository = get()
        )
    }

    // ========== Secure Preferences ==========

    single { SecurePreferences(androidContext()) }
}
