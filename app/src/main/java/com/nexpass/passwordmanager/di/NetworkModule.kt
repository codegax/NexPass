package com.nexpass.passwordmanager.di

import com.nexpass.passwordmanager.data.network.NextcloudApiClient
import org.koin.dsl.module

/**
 * Network Module - Network layer dependencies
 *
 * Provides:
 * - NextcloudApiClient for Nextcloud Passwords API
 * - Ktor HttpClient (configured in NextcloudApiClient)
 * - Network interceptors
 * - Serialization configuration
 */
val networkModule = module {
    // Nextcloud API Client - singleton
    single {
        NextcloudApiClient(
            securePreferences = get()
        )
    }
}
