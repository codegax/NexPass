package com.nexpass.passwordmanager.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nexpass.passwordmanager.ui.components.*
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import com.nexpass.passwordmanager.ui.viewmodel.OnboardingViewModel
import com.nexpass.passwordmanager.ui.viewmodel.OnboardingUiState
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding Screen
 *
 * First-time setup flow for new users:
 * 1. Welcome message
 * 2. Create master password
 * 3. Confirm master password
 * 4. Warning about password recovery
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    var step by remember { mutableIntStateOf(1) }
    var masterPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is OnboardingUiState.Success -> onComplete()
            is OnboardingUiState.Error -> {
                passwordError = (uiState as OnboardingUiState.Error).message
            }
            else -> {}
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (step) {
                1 -> WelcomeStep(onNext = { step = 2 })
                2 -> CreatePasswordStep(
                    password = masterPassword,
                    onPasswordChange = {
                        masterPassword = it
                        passwordError = null
                    },
                    onNext = {
                        if (masterPassword.length >= 8) {
                            step = 3
                        } else {
                            passwordError = "Password must be at least 8 characters"
                        }
                    },
                    errorMessage = passwordError
                )
                3 -> ConfirmPasswordStep(
                    password = masterPassword,
                    confirmPassword = confirmPassword,
                    onConfirmChange = {
                        confirmPassword = it
                        passwordError = null
                    },
                    onNext = {
                        if (confirmPassword == masterPassword) {
                            step = 4
                        } else {
                            passwordError = "Passwords do not match"
                        }
                    },
                    onBack = { step = 2 },
                    errorMessage = passwordError
                )
                4 -> WarningStep(
                    onAccept = {
                        viewModel.initializeVault(masterPassword)
                    },
                    onBack = { step = 3 },
                    isLoading = uiState is OnboardingUiState.Loading
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Welcome to NexPass",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your secure, offline-first password manager with zero-knowledge encryption",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        NexPassPrimaryButton(
            text = "Get Started",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CreatePasswordStep(
    password: String,
    onPasswordChange: (String) -> Unit,
    onNext: () -> Unit,
    errorMessage: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create Master Password",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "This password will encrypt all your data. Choose a strong, memorable password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        NexPassPasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Master Password",
            placeholder = "Enter your master password",
            isError = errorMessage != null,
            errorMessage = errorMessage,
            imeAction = ImeAction.Next
        )

        if (password.isNotEmpty()) {
            PasswordStrengthIndicator(password = password)

            Spacer(modifier = Modifier.height(8.dp))

            PasswordRequirements(password = password)
        }

        Spacer(modifier = Modifier.weight(1f))

        NexPassPrimaryButton(
            text = "Next",
            onClick = onNext,
            enabled = password.length >= 8,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConfirmPasswordStep(
    password: String,
    confirmPassword: String,
    onConfirmChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    errorMessage: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Confirm Master Password",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Re-enter your master password to confirm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        NexPassPasswordField(
            value = confirmPassword,
            onValueChange = onConfirmChange,
            label = "Confirm Password",
            placeholder = "Re-enter your master password",
            isError = errorMessage != null,
            errorMessage = errorMessage,
            imeAction = ImeAction.Done
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NexPassPrimaryButton(
                text = "Next",
                onClick = onNext,
                enabled = confirmPassword.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )

            NexPassSecondaryButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WarningStep(
    onAccept: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚠️ Important",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Your master password cannot be recovered!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = "• We use zero-knowledge encryption\n" +
                           "• Your password is never sent to our servers\n" +
                           "• If you forget it, your data is permanently lost\n" +
                           "• There is no password reset option",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Text(
            text = "Please write down your master password and store it in a safe place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NexPassPrimaryButton(
                text = "I Understand, Create Vault",
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                loading = isLoading
            )

            NexPassSecondaryButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    NexPassTheme {
        OnboardingScreen(onComplete = {})
    }
}
