package com.nexpass.passwordmanager.di

import com.nexpass.passwordmanager.ui.lifecycle.AutoLockManager
import org.koin.dsl.module

/**
 * App Module - Application-level dependencies
 *
 * Provides:
 * - Application context
 * - Global utilities
 * - App-wide singletons
 */
val appModule = module {
    // Auto-lock manager
    single {
        AutoLockManager(
            vaultKeyManager = get(),
            securePreferences = get()
        )
    }
}
