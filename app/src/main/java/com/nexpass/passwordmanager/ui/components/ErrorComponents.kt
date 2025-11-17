package com.nexpass.passwordmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexpass.passwordmanager.domain.model.AppError
import com.nexpass.passwordmanager.domain.model.RecoveryAction

/**
 * Reusable error display card.
 *
 * Shows error message with icon and optional action button.
 */
@Composable
fun ErrorCard(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Error icon
            Icon(
                imageVector = getErrorIcon(error),
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )

            // Error message
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            // Action buttons based on recovery action
            when (error.recoveryAction) {
                is RecoveryAction.Retry -> {
                    if (onRetry != null) {
                        Button(onClick = onRetry) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
                is RecoveryAction.CheckConnection -> {
                    Text(
                        text = "Please check your internet connection and try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    if (onRetry != null) {
                        OutlinedButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
                is RecoveryAction.UnlockVault -> {
                    Text(
                        text = "Please unlock your vault to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // Generic dismiss button
                    if (onDismiss != null) {
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inline error message (less prominent than ErrorCard)
 */
@Composable
fun ErrorMessage(
    error: AppError,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )

        if (onDismiss != null) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Full-screen error state with illustration
 */
@Composable
fun ErrorState(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Large error icon
            Icon(
                imageVector = getErrorIcon(error),
                contentDescription = "Error",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            )

            // Error title
            Text(
                text = getErrorTitle(error),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Error message
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Retry button if retryable
            if (error.recoveryAction is RecoveryAction.Retry && onRetry != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again")
                }
            }
        }
    }
}

/**
 * Get appropriate icon for error type
 */
private fun getErrorIcon(error: AppError): androidx.compose.ui.graphics.vector.ImageVector {
    return when (error) {
        is AppError.Network -> Icons.Default.CloudOff
        is AppError.Auth -> Icons.Default.Lock
        is AppError.Encryption -> Icons.Default.Security
        is AppError.Storage -> Icons.Default.Storage
        is AppError.Sync -> Icons.Default.SyncProblem
        is AppError.Autofill -> Icons.Default.AutoMode
        is AppError.Validation -> Icons.Default.Warning
        is AppError.Unknown -> Icons.Default.ErrorOutline
    }
}

/**
 * Get user-friendly error title
 */
private fun getErrorTitle(error: AppError): String {
    return when (error) {
        is AppError.Network.NoConnection -> "No Internet Connection"
        is AppError.Network.Timeout -> "Connection Timeout"
        is AppError.Network.ServerUnreachable -> "Server Unreachable"
        is AppError.Network.RateLimited -> "Too Many Requests"
        is AppError.Network.Unknown -> "Network Error"

        is AppError.Auth.InvalidCredentials -> "Invalid Credentials"
        is AppError.Auth.TokenExpired -> "Session Expired"
        is AppError.Auth.Unauthorized -> "Access Denied"
        is AppError.Auth.VaultLocked -> "Vault Locked"
        is AppError.Auth.BiometricFailed -> "Authentication Failed"

        is AppError.Encryption.DecryptionFailed -> "Decryption Failed"
        is AppError.Encryption.EncryptionFailed -> "Encryption Failed"
        is AppError.Encryption.KeyDerivationFailed -> "Key Derivation Failed"
        is AppError.Encryption.KeystoreError -> "Keystore Error"

        is AppError.Storage.DatabaseCorrupted -> "Database Corrupted"
        is AppError.Storage.InsufficientStorage -> "Storage Full"
        is AppError.Storage.ReadFailed -> "Read Error"
        is AppError.Storage.WriteFailed -> "Write Error"

        is AppError.Sync.NotConfigured -> "Sync Not Configured"
        is AppError.Sync.ConflictResolutionFailed -> "Sync Conflict"
        is AppError.Sync.SyncInProgress -> "Sync In Progress"
        is AppError.Sync.RemoteChangeFailed -> "Sync Failed"

        is AppError.Autofill.ServiceUnavailable -> "Autofill Unavailable"
        is AppError.Autofill.FieldDetectionFailed -> "Field Detection Failed"
        is AppError.Autofill.NoMatchesFound -> "No Matches"

        is AppError.Validation -> "Validation Error"
        is AppError.Unknown -> "Unexpected Error"
    }
}
