package com.nexpass.passwordmanager.ui.screens.unlock

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nexpass.passwordmanager.ui.components.*
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import com.nexpass.passwordmanager.ui.viewmodel.UnlockViewModel
import com.nexpass.passwordmanager.ui.viewmodel.UnlockUiState
import org.koin.androidx.compose.koinViewModel

@Composable
fun UnlockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnlockViewModel = koinViewModel()
) {
    var password by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is UnlockUiState.Success -> onUnlocked()
            else -> {}
        }
    }

    val errorMessage = when (val state = uiState) {
        is UnlockUiState.Error -> state.message
        else -> null
    }

    val isLoading = uiState is UnlockUiState.Loading
    val isBiometricAvailable = (uiState as? UnlockUiState.Idle)?.isBiometricAvailable ?: false

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Unlock Vault",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            NexPassPasswordField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.resetError()
                },
                label = "Master Password",
                placeholder = "Enter your master password",
                isError = errorMessage != null,
                errorMessage = errorMessage
            )

            Spacer(modifier = Modifier.height(24.dp))

            NexPassPrimaryButton(
                text = "Unlock",
                onClick = {
                    viewModel.unlockWithPassword(password)
                },
                enabled = password.isNotEmpty() && !isLoading,
                loading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isBiometricAvailable) {
                NexPassSecondaryButton(
                    text = "Use Biometric",
                    onClick = {
                        viewModel.unlockWithBiometric()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UnlockScreenPreview() {
    NexPassTheme {
        UnlockScreen(onUnlocked = {})
    }
}
