package com.nexpass.passwordmanager.di

import com.nexpass.passwordmanager.domain.usecase.ExportVaultUseCase
import com.nexpass.passwordmanager.domain.usecase.ImportVaultUseCase
import org.koin.dsl.module

/**
 * Domain Module - Business logic dependencies
 *
 * Provides:
 * - Use cases
 * - Domain services
 * - Business logic orchestrators
 */
val domainModule = module {
    // Export/Import use cases
    factory {
        ExportVaultUseCase(
            passwordRepository = get(),
            cryptoManager = get()
        )
    }

    factory {
        ImportVaultUseCase(
            passwordRepository = get(),
            cryptoManager = get()
        )
    }
}
