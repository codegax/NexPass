package com.nexpass.passwordmanager.domain.model

/**
 * Comprehensive error type system for the entire application.
 *
 * This sealed class hierarchy provides type-safe error handling with:
 * - User-friendly error messages
 * - Suggested recovery actions
 * - Error categorization for logging and analytics
 * - Support for retry logic
 */
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null,
    open val recoveryAction: RecoveryAction? = null
) : Throwable(message, cause) {
    /**
     * Network-related errors
     */
    sealed class Network(
        override val message: String,
        override val cause: Throwable? = null,
        override val recoveryAction: RecoveryAction? = RecoveryAction.Retry
    ) : AppError(message, cause, recoveryAction) {

        data class NoConnection(
            override val cause: Throwable? = null
        ) : Network(
            message = "No internet connection. Please check your network settings.",
            cause = cause,
            recoveryAction = RecoveryAction.CheckConnection
        )

        data class Timeout(
            override val cause: Throwable? = null
        ) : Network(
            message = "Request timed out. Please try again.",
            cause = cause,
            recoveryAction = RecoveryAction.Retry
        )

        data class ServerUnreachable(
            val serverUrl: String,
            override val cause: Throwable? = null
        ) : Network(
            message = "Cannot reach server at $serverUrl. Please check the URL and try again.",
            cause = cause,
            recoveryAction = RecoveryAction.CheckServerUrl
        )

        data class RateLimited(
            val retryAfterSeconds: Int? = null,
            override val cause: Throwable? = null
        ) : Network(
            message = retryAfterSeconds?.let {
                "Too many requests. Please try again in $it seconds."
            } ?: "Too many requests. Please try again later.",
            cause = cause,
            recoveryAction = RecoveryAction.WaitAndRetry(retryAfterSeconds)
        )

        data class Unknown(
            override val message: String = "Network error occurred.",
            override val cause: Throwable? = null
        ) : Network(message, cause)
    }

    /**
     * Authentication and authorization errors
     */
    sealed class Auth(
        override val message: String,
        override val cause: Throwable? = null,
        override val recoveryAction: RecoveryAction? = null
    ) : AppError(message, cause, recoveryAction) {

        data class InvalidCredentials(
            override val cause: Throwable? = null
        ) : Auth(
            message = "Invalid username or app password. Please check your credentials.",
            cause = cause,
            recoveryAction = RecoveryAction.ReconfigureAuth
        )

        data class TokenExpired(
            override val cause: Throwable? = null
        ) : Auth(
            message = "Your session has expired. Please sign in again.",
            cause = cause,
            recoveryAction = RecoveryAction.Reauthenticate
        )

        data class Unauthorized(
            override val cause: Throwable? = null
        ) : Auth(
            message = "You don't have permission to perform this action.",
            cause = cause,
            recoveryAction = RecoveryAction.CheckPermissions
        )

        data class VaultLocked(
            override val cause: Throwable? = null
        ) : Auth(
            message = "Vault is locked. Please unlock to continue.",
            cause = cause,
            recoveryAction = RecoveryAction.UnlockVault
        )

        data class BiometricFailed(
            override val message: String = "Biometric authentication failed.",
            override val cause: Throwable? = null
        ) : Auth(message, cause, RecoveryAction.UseMasterPassword)
    }

    /**
     * Encryption and decryption errors
     */
    sealed class Encryption(
        override val message: String,
        override val cause: Throwable? = null,
        override val recoveryAction: RecoveryAction? = null
    ) : AppError(message, cause, recoveryAction) {

        data class DecryptionFailed(
            val entryId: String,
            override val cause: Throwable? = null
        ) : Encryption(
            message = "Failed to decrypt password entry. The data may be corrupted.",
            cause = cause,
            recoveryAction = RecoveryAction.QuarantineEntry(entryId)
        )

        data class EncryptionFailed(
            override val cause: Throwable? = null
        ) : Encryption(
            message = "Failed to encrypt data. Please try again.",
            cause = cause,
            recoveryAction = RecoveryAction.Retry
        )

        data class KeyDerivationFailed(
            override val cause: Throwable? = null
        ) : Encryption(
            message = "Failed to derive encryption key. Please check your master password.",
            cause = cause
        )

        data class KeystoreError(
            override val message: String = "Android Keystore error occurred.",
            override val cause: Throwable? = null
        ) : Encryption(message, cause, RecoveryAction.RestartApp)
    }

    /**
     * Database and storage errors
     */
    sealed class Storage(
        override val message: String,
        override val cause: Throwable? = null,
        override val recoveryAction: RecoveryAction? = null
    ) : AppError(message, cause, recoveryAction) {

        data class DatabaseCorrupted(
            override val cause: Throwable? = null
        ) : Storage(
            message = "Database is corrupted. You may need to restore from backup.",
            cause = cause,
            recoveryAction = RecoveryAction.RestoreBackup
        )

        data class InsufficientStorage(
            override val cause: Throwable? = null
        ) : Storage(
            message = "Insufficient storage space. Please free up space and try again.",
            cause = cause,
            recoveryAction = RecoveryAction.FreeSpace
        )

        data class ReadFailed(
            override val message: String = "Failed to read data from storage.",
            override val cause: Throwable? = null
        ) : Storage(message, cause, RecoveryAction.Retry)

        data class WriteFailed(
            override val message: String = "Failed to write data to storage.",
            override val cause: Throwable? = null
        ) : Storage(message, cause, RecoveryAction.Retry)
    }

    /**
     * Sync-specific errors
     */
    sealed class Sync(
        override val message: String,
        override val cause: Throwable? = null,
        override val recoveryAction: RecoveryAction? = null
    ) : AppError(message, cause, recoveryAction) {

        data class NotConfigured(
            override val cause: Throwable? = null
        ) : Sync(
            message = "Nextcloud sync is not configured. Please configure in settings.",
            cause = cause,
            recoveryAction = RecoveryAction.ConfigureSync
        )

        data class ConflictResolutionFailed(
            val entryTitle: String,
            override val cause: Throwable? = null
        ) : Sync(
            message = "Failed to resolve conflict for '$entryTitle'.",
            cause = cause,
            recoveryAction = RecoveryAction.ManualResolve
        )

        data class SyncInProgress(
            override val cause: Throwable? = null
        ) : Sync(
            message = "Sync is already in progress.",
            cause = cause
        )

        data class RemoteChangeFailed(
            val operationType: String,
            override val cause: Throwable? = null
        ) : Sync(
            message = "Failed to $operationType on server. Changes saved locally.",
            cause = cause,
            recoveryAction = RecoveryAction.RetrySync
        )
    }

    /**
     * Autofill-specific errors
     */
    sealed class Autofill(
        override val message: String,
        override val cause: Throwable? = null,
        override val recoveryAction: RecoveryAction? = null
    ) : AppError(message, cause, recoveryAction) {

        data class ServiceUnavailable(
            override val cause: Throwable? = null
        ) : Autofill(
            message = "Autofill service is not available.",
            cause = cause,
            recoveryAction = RecoveryAction.EnableAutofill
        )

        data class FieldDetectionFailed(
            override val cause: Throwable? = null
        ) : Autofill(
            message = "Failed to detect login fields.",
            cause = cause
        )

        data class NoMatchesFound(
            val domain: String?,
            override val cause: Throwable? = null
        ) : Autofill(
            message = domain?.let { "No passwords found for $it." }
                ?: "No matching passwords found.",
            cause = cause,
            recoveryAction = RecoveryAction.CreateEntry
        )
    }

    /**
     * Validation errors
     */
    sealed class Validation(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause, null) {

        data class InvalidInput(
            val fieldName: String,
            val reason: String
        ) : Validation(
            message = "$fieldName is invalid: $reason"
        )

        data class WeakPassword(
            override val message: String = "Password is too weak. Please use a stronger password."
        ) : Validation(message)

        data class RequiredFieldMissing(
            val fieldName: String
        ) : Validation(
            message = "$fieldName is required."
        )

        data class InvalidUrl(
            val url: String
        ) : Validation(
            message = "'$url' is not a valid URL."
        )
    }

    /**
     * Generic/unknown errors
     */
    data class Unknown(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : AppError(message, cause, RecoveryAction.Retry)
}

/**
 * Suggested recovery actions for errors
 */
sealed class RecoveryAction {
    object Retry : RecoveryAction()
    object CheckConnection : RecoveryAction()
    object CheckServerUrl : RecoveryAction()
    data class WaitAndRetry(val seconds: Int?) : RecoveryAction()
    object ReconfigureAuth : RecoveryAction()
    object Reauthenticate : RecoveryAction()
    object CheckPermissions : RecoveryAction()
    object UnlockVault : RecoveryAction()
    object UseMasterPassword : RecoveryAction()
    data class QuarantineEntry(val entryId: String) : RecoveryAction()
    object RestartApp : RecoveryAction()
    object RestoreBackup : RecoveryAction()
    object FreeSpace : RecoveryAction()
    object ConfigureSync : RecoveryAction()
    object ManualResolve : RecoveryAction()
    object RetrySync : RecoveryAction()
    object EnableAutofill : RecoveryAction()
    object CreateEntry : RecoveryAction()
}

/**
 * Result type that wraps success or error
 */
sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Error(val error: AppError) : DataResult<Nothing>()
    object Loading : DataResult<Nothing>()
}

/**
 * Extension function to convert Throwable to AppError
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is java.net.UnknownHostException -> AppError.Network.NoConnection(this)
        is java.net.SocketTimeoutException -> AppError.Network.Timeout(this)
        is java.net.ConnectException -> AppError.Network.NoConnection(this)
        is javax.crypto.AEADBadTagException -> AppError.Encryption.DecryptionFailed("unknown", this)
        is javax.crypto.BadPaddingException -> AppError.Encryption.DecryptionFailed("unknown", this)
        is java.io.IOException -> AppError.Storage.ReadFailed(this.message ?: "IO error", this)
        else -> AppError.Unknown(this.message ?: "Unknown error", this)
    }
}

/**
 * Extension function to extract user-friendly message from AppError
 */
fun AppError.getUserMessage(): String = this.message

/**
 * Extension function to check if error is retryable
 */
fun AppError.isRetryable(): Boolean = this.recoveryAction is RecoveryAction.Retry
    || this.recoveryAction is RecoveryAction.WaitAndRetry
    || this.recoveryAction is RecoveryAction.RetrySync
