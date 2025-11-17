package com.nexpass.passwordmanager

import android.app.Application
import com.nexpass.passwordmanager.di.appModule
import com.nexpass.passwordmanager.di.autofillModule
import com.nexpass.passwordmanager.di.dataModule
import com.nexpass.passwordmanager.di.domainModule
import com.nexpass.passwordmanager.di.networkModule
import com.nexpass.passwordmanager.di.securityModule
import com.nexpass.passwordmanager.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class for NexPass Password Manager
 *
 * Responsibilities:
 * - Initialize Koin dependency injection
 * - Configure security settings
 * - Set up global app configuration
 */
class PasswordManagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin dependency injection
        startKoin {
            // Use Android logger with ERROR level
            androidLogger(Level.ERROR)
            androidContext(this@PasswordManagerApplication)
            modules(
                appModule,
                dataModule,
                domainModule,
                networkModule,
                securityModule,
                viewModelModule,
                autofillModule
            )
        }

        // Initialize security components
        initializeSecurity()
    }

    /**
     * Initialize security-specific configurations
     *
     * This includes:
     * - Disabling screenshots in secure screens (handled per-activity)
     * - Setting up memory management for sensitive data
     * - Configuring secure logging (no sensitive data in logs)
     */
    private fun initializeSecurity() {
        // Security configurations will be handled by individual components:
        // - CryptoManager for encryption
        // - KeystoreManager for key storage
        // - BiometricManager for biometric authentication
        // - SecureLogger for safe logging

        // Note: Screenshot prevention is handled per-activity using
        // WindowManager.LayoutParams.FLAG_SECURE where needed
    }
}
