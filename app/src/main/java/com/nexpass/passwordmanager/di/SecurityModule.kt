package com.nexpass.passwordmanager.di

import com.nexpass.passwordmanager.security.biometric.BiometricManager
import com.nexpass.passwordmanager.security.biometric.BiometricManagerImpl
import com.nexpass.passwordmanager.security.encryption.CryptoManager
import com.nexpass.passwordmanager.security.encryption.CryptoManagerImpl
import com.nexpass.passwordmanager.security.keystore.KeystoreManager
import com.nexpass.passwordmanager.security.keystore.KeystoreManagerImpl
import com.nexpass.passwordmanager.security.vault.VaultKeyManager
import org.koin.dsl.module

/**
 * Security Module - Security layer dependencies
 *
 * Provides:
 * - CryptoManager (singleton) - Encryption/decryption operations
 * - KeystoreManager (singleton) - Android Keystore integration
 * - BiometricManager (singleton) - Biometric authentication
 * - VaultKeyManager (singleton) - Vault key lifecycle management
 *
 * All security components are singletons to ensure:
 * - Single source of truth for security state
 * - Efficient resource usage
 * - Proper keystore initialization
 */
val securityModule = module {

    /**
     * CryptoManager singleton.
     * Handles all encryption/decryption operations using AES-GCM.
     */
    single<CryptoManager> {
        CryptoManagerImpl()
    }

    /**
     * KeystoreManager singleton.
     * Manages Android Keystore keys for secure key storage.
     * Must call initialize() before use.
     */
    single<KeystoreManager> {
        KeystoreManagerImpl()
    }

    /**
     * BiometricManager singleton.
     * Handles biometric authentication prompts.
     */
    single<BiometricManager> {
        BiometricManagerImpl()
    }

    /**
     * VaultKeyManager singleton.
     * Manages vault key in memory and provides unlock/lock functionality.
     */
    single {
        VaultKeyManager(
            cryptoManager = get(),
            keystoreManager = get()
        )
    }
}
