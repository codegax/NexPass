package com.nexpass.passwordmanager.util

import android.util.Log
import com.nexpass.passwordmanager.domain.model.AppError
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Retry policy with exponential backoff for network operations.
 *
 * Features:
 * - Exponential backoff with jitter
 * - Configurable max attempts
 * - Configurable base delay
 * - Respects rate limiting
 * - Type-safe error handling with AppError
 */
class RetryPolicy(
    private val maxAttempts: Int = 3,
    private val baseDelayMs: Long = 1000,
    private val maxDelayMs: Long = 32000,
    private val exponentialBase: Double = 2.0,
    private val jitterFactor: Double = 0.1
) {
    companion object {
        private const val TAG = "RetryPolicy"

        /**
         * Default retry policy for network operations
         */
        val DEFAULT = RetryPolicy(
            maxAttempts = 3,
            baseDelayMs = 1000,
            maxDelayMs = 32000
        )

        /**
         * Aggressive retry policy for critical operations
         */
        val AGGRESSIVE = RetryPolicy(
            maxAttempts = 5,
            baseDelayMs = 500,
            maxDelayMs = 16000
        )

        /**
         * Conservative retry policy for non-critical operations
         */
        val CONSERVATIVE = RetryPolicy(
            maxAttempts = 2,
            baseDelayMs = 2000,
            maxDelayMs = 8000
        )
    }

    /**
     * Execute a block with retry logic and exponential backoff.
     *
     * @param block The suspending function to execute
     * @param shouldRetry Predicate to determine if error is retryable (default: check AppError.isRetryable)
     * @return Result of the block execution
     */
    suspend fun <T> execute(
        block: suspend (attempt: Int) -> Result<T>,
        shouldRetry: (AppError) -> Boolean = { it.isRetryable() }
    ): Result<T> {
        var lastError: AppError? = null
        var currentAttempt = 1

        while (currentAttempt <= maxAttempts) {
            try {
                val result = block(currentAttempt)

                if (result.isSuccess) {
                    if (currentAttempt > 1) {
                        Log.d(TAG, "Operation succeeded on attempt $currentAttempt")
                    }
                    return result
                }

                // Extract error from failure
                val exception = result.exceptionOrNull()
                lastError = exception?.let { it as? AppError ?: it.toAppError() }

                // Check if we should retry
                if (lastError != null && !shouldRetry(lastError)) {
                    Log.d(TAG, "Error is not retryable: ${lastError.message}")
                    return Result.failure(lastError)
                }

                // Special handling for rate limiting
                if (lastError is AppError.Network.RateLimited) {
                    val waitTime = lastError.retryAfterSeconds?.let { it * 1000L } ?: calculateBackoff(currentAttempt)
                    Log.d(TAG, "Rate limited. Waiting ${waitTime}ms before retry.")
                    delay(waitTime)
                } else if (currentAttempt < maxAttempts) {
                    val backoffTime = calculateBackoff(currentAttempt)
                    Log.d(TAG, "Attempt $currentAttempt failed: ${lastError?.message}. Retrying in ${backoffTime}ms...")
                    delay(backoffTime)
                }

                currentAttempt++
            } catch (e: Exception) {
                lastError = e.toAppError()

                if (!shouldRetry(lastError)) {
                    Log.e(TAG, "Non-retryable exception on attempt $currentAttempt", e)
                    return Result.failure(lastError)
                }

                if (currentAttempt < maxAttempts) {
                    val backoffTime = calculateBackoff(currentAttempt)
                    Log.e(TAG, "Attempt $currentAttempt failed with exception. Retrying in ${backoffTime}ms...", e)
                    delay(backoffTime)
                }

                currentAttempt++
            }
        }

        // All attempts exhausted
        val finalError = lastError ?: AppError.Unknown("All retry attempts exhausted")
        Log.e(TAG, "All $maxAttempts attempts failed: ${finalError.message}")
        return Result.failure(finalError)
    }

    /**
     * Calculate backoff time with exponential increase and jitter
     */
    private fun calculateBackoff(attempt: Int): Long {
        // Exponential backoff: baseDelay * (exponentialBase ^ (attempt - 1))
        val exponentialDelay = baseDelayMs * exponentialBase.pow(attempt - 1).toLong()

        // Cap at max delay
        val cappedDelay = min(exponentialDelay, maxDelayMs)

        // Add jitter to prevent thundering herd
        val jitter = (cappedDelay * jitterFactor * Math.random()).toLong()

        return cappedDelay + jitter
    }
}

/**
 * Extension function to check if AppError is retryable
 */
fun AppError.isRetryable(): Boolean {
    return when (this) {
        // Network errors are generally retryable
        is AppError.Network.NoConnection -> true
        is AppError.Network.Timeout -> true
        is AppError.Network.ServerUnreachable -> true
        is AppError.Network.RateLimited -> true
        is AppError.Network.Unknown -> true

        // Auth errors - only token expiry is retryable after re-auth
        is AppError.Auth.TokenExpired -> false // Requires re-auth first
        is AppError.Auth.InvalidCredentials -> false
        is AppError.Auth.Unauthorized -> false
        is AppError.Auth.VaultLocked -> false
        is AppError.Auth.BiometricFailed -> false

        // Encryption errors - not retryable
        is AppError.Encryption -> false

        // Storage errors - some are retryable
        is AppError.Storage.ReadFailed -> true
        is AppError.Storage.WriteFailed -> true
        is AppError.Storage.DatabaseCorrupted -> false
        is AppError.Storage.InsufficientStorage -> false

        // Sync errors
        is AppError.Sync.NotConfigured -> false
        is AppError.Sync.ConflictResolutionFailed -> false
        is AppError.Sync.SyncInProgress -> false
        is AppError.Sync.RemoteChangeFailed -> true

        // Autofill errors - not retryable
        is AppError.Autofill -> false

        // Validation errors - not retryable
        is AppError.Validation -> false

        // Unknown errors - conservative approach, allow retry
        is AppError.Unknown -> true
    }
}

/**
 * Extension function to convert Throwable to AppError
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is AppError -> this
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
 * Convenience extension for executing with default retry policy
 */
suspend fun <T> retryWithBackoff(
    policy: RetryPolicy = RetryPolicy.DEFAULT,
    block: suspend (attempt: Int) -> Result<T>
): Result<T> = policy.execute(block)
