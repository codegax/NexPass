package com.nexpass.passwordmanager.di

import com.nexpass.passwordmanager.autofill.matcher.AutofillMatcher
import com.nexpass.passwordmanager.autofill.matcher.AutofillMatcherImpl
import com.nexpass.passwordmanager.autofill.service.AutofillResponseBuilder
import com.nexpass.passwordmanager.autofill.ui.AutofillPromptViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Autofill Module - Provides autofill-related dependencies
 *
 * Includes AutofillMatcher, AutofillResponseBuilder, and AutofillPromptViewModel.
 */
val autofillModule = module {

    // AutofillMatcher - for matching password entries to autofill contexts
    single<AutofillMatcher> {
        AutofillMatcherImpl(
            passwordRepository = get()
        )
    }

    // AutofillResponseBuilder - for building autofill responses
    factory {
        AutofillResponseBuilder(
            context = androidContext()
        )
    }

    // AutofillPromptViewModel - for the unlock prompt activity
    viewModel {
        AutofillPromptViewModel(
            vaultKeyManager = get(),
            securePreferences = get(),
            biometricManager = get(),
            autofillMatcher = get(),
            autofillResponseBuilder = get()
        )
    }
}
