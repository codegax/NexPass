package com.nexpass.passwordmanager.di

import com.nexpass.passwordmanager.ui.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel Module - UI layer ViewModels
 *
 * Provides all ViewModels with their dependencies injected.
 */
val viewModelModule = module {

    // Onboarding ViewModel
    viewModel {
        OnboardingViewModel(
            cryptoManager = get(),
            vaultKeyManager = get(),
            securePreferences = get()
        )
    }

    // Unlock ViewModel
    viewModel {
        UnlockViewModel(
            vaultKeyManager = get(),
            securePreferences = get(),
            biometricManager = get()
        )
    }

    // Vault List ViewModel
    viewModel {
        VaultListViewModel(
            passwordRepository = get(),
            vaultKeyManager = get(),
            folderRepository = get()
        )
    }

    // Password Detail ViewModel
    viewModel {
        PasswordDetailViewModel(
            passwordRepository = get()
        )
    }

    // Password Form ViewModel
    viewModel {
        PasswordFormViewModel(
            passwordRepository = get(),
            folderRepository = get()
        )
    }

    // Settings ViewModel
    viewModel {
        SettingsViewModel(
            securePreferences = get(),
            vaultKeyManager = get(),
            biometricManager = get(),
            syncRepository = get(),
            exportVaultUseCase = get(),
            importVaultUseCase = get()
        )
    }

    // Password Generator ViewModel
    viewModel {
        PasswordGeneratorViewModel()
    }

    // Folder ViewModel
    viewModel {
        FolderViewModel(
            folderRepository = get(),
            syncRepository = get()
        )
    }

    // Tag ViewModel
    viewModel {
        TagViewModel(
            tagRepository = get()
        )
    }
}
