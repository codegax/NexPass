package com.nexpass.passwordmanager.data.network

import android.util.Base64
import android.util.Log
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.data.network.dto.*
import com.nexpass.passwordmanager.domain.model.AppError
import com.nexpass.passwordmanager.util.RetryPolicy
import com.nexpass.passwordmanager.util.toAppError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * HTTP client for Nextcloud Passwords API.
 *
 * Implements the Nextcloud Passwords app API v1.0
 * Authentication: Basic Auth with username and app password
 * Base URL: https://<server>/index.php/apps/passwords/api/1.0
 *
 * Features:
 * - Network error handling with AppError types
 * - HTTP status code mapping to AppError
 */
class NextcloudApiClient(
    private val securePreferences: SecurePreferences
) {
    companion object {
        private const val TAG = "NextcloudApiClient"
        private const val API_BASE_PATH = "/index.php/apps/passwords/api/1.0"
        private const val TIMEOUT_MS = 30_000L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(this@NextcloudApiClient.json)
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(TAG, message)
                }
            }
            level = LogLevel.INFO
        }

        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MS
            connectTimeoutMillis = TIMEOUT_MS
            socketTimeoutMillis = TIMEOUT_MS
        }

        defaultRequest {
            // Add Basic Auth header
            val username = securePreferences.getNextcloudUsername() ?: ""
            val password = securePreferences.getNextcloudAppPassword() ?: ""
            val credentials = "$username:$password"
            val encodedCredentials = Base64.encodeToString(
                credentials.toByteArray(),
                Base64.NO_WRAP
            )
            header("Authorization", "Basic $encodedCredentials")

            // Set content type
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }

    /**
     * Get the full API URL.
     */
    private fun getApiUrl(endpoint: String): String {
        val serverUrl = securePreferences.getNextcloudServerUrl()
            ?: throw IllegalStateException("Nextcloud server URL not configured")
        return "$serverUrl$API_BASE_PATH$endpoint"
    }

    /**
     * Test the connection to the Nextcloud server.
     * Returns true if connection is successful and credentials are valid.
     */
    suspend fun testConnection(): Result<Boolean> {
        return try {
            Log.d(TAG, "Testing connection")
            val response = httpClient.get(getApiUrl("/password/list"))
            handleHttpResponseBoolean(response)
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * List all passwords from Nextcloud.
     */
    suspend fun listPasswords(): Result<List<NextcloudPasswordDto>> {
        return try {
            Log.d(TAG, "Listing passwords")
            val response = httpClient.get(getApiUrl("/password/list"))
            handleHttpResponse<List<NextcloudPasswordDto>>(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list passwords", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * Get a single password by ID.
     */
    suspend fun getPassword(id: String): Result<NextcloudPasswordDto> {
        return try {
            Log.d(TAG, "Getting password $id")
            val response = httpClient.get(getApiUrl("/password/show")) {
                parameter("id", id)
            }
            handleHttpResponse<NextcloudPasswordDto>(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get password $id", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * Create a new password on Nextcloud.
     */
    suspend fun createPassword(request: CreatePasswordRequest): Result<NextcloudPasswordDto> {
        return try {
            Log.d(TAG, "Creating password")
            val response = httpClient.post(getApiUrl("/password/create")) {
                setBody(request)
            }
            handleHttpResponse<NextcloudPasswordDto>(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create password", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * Update an existing password on Nextcloud.
     */
    suspend fun updatePassword(request: UpdatePasswordRequest): Result<NextcloudPasswordDto> {
        return try {
            Log.d(TAG, "Updating password ${request.id}")
            val response = httpClient.patch(getApiUrl("/password/update")) {
                setBody(request)
            }
            handleHttpResponse<NextcloudPasswordDto>(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update password", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * Delete a password from Nextcloud.
     */
    suspend fun deletePassword(id: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Deleting password $id")
            val response = httpClient.delete(getApiUrl("/password/delete")) {
                parameter("id", id)
            }
            handleHttpResponseBoolean(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete password $id", e)
            Result.failure(e.toAppError())
        }
    }

    // ========== Folder API Methods ==========

    /**
     * List all folders from Nextcloud.
     */
    suspend fun listFolders(): Result<List<NextcloudFolderDto>> {
        return try {
            Log.d(TAG, "Listing folders")
            val response = httpClient.get(getApiUrl("/folder/list"))
            handleHttpResponse<List<NextcloudFolderDto>>(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list folders", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * Get a single folder by ID.
     */
    suspend fun getFolder(id: String): Result<NextcloudFolderDto> {
        return try {
            Log.d(TAG, "Getting folder $id")
            val response = httpClient.get(getApiUrl("/folder/show")) {
                parameter("id", id)
            }
            handleHttpResponse<NextcloudFolderDto>(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get folder $id", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * Create a new folder on Nextcloud.
     */
    suspend fun createFolder(request: CreateFolderRequest): Result<NextcloudFolderDto> {
        return try {
            Log.d(TAG, "Creating folder")
            val response = httpClient.post(getApiUrl("/folder/create")) {
                setBody(request)
            }
            handleHttpResponse<NextcloudFolderDto>(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create folder", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * Update an existing folder on Nextcloud.
     */
    suspend fun updateFolder(request: UpdateFolderRequest): Result<NextcloudFolderDto> {
        return try {
            Log.d(TAG, "Updating folder ${request.id}")
            val response = httpClient.patch(getApiUrl("/folder/update")) {
                setBody(request)
            }
            handleHttpResponse<NextcloudFolderDto>(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update folder", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * Delete a folder from Nextcloud.
     */
    suspend fun deleteFolder(id: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Deleting folder $id")
            val response = httpClient.delete(getApiUrl("/folder/delete")) {
                parameter("id", id)
            }
            handleHttpResponseBoolean(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete folder $id", e)
            Result.failure(e.toAppError())
        }
    }

    /**
     * Handle HTTP response and map status codes to AppError (for password responses)
     */
    private suspend inline fun <reified T> handleHttpResponse(
        response: HttpResponse
    ): Result<T> {
        return when (response.status.value) {
            in 200..299 -> {
                val data: T = response.body()
                Result.success(data)
            }
            401 -> Result.failure(AppError.Auth.InvalidCredentials())
            403 -> Result.failure(AppError.Auth.Unauthorized())
            404 -> Result.failure(AppError.Network.ServerUnreachable(
                serverUrl = securePreferences.getNextcloudServerUrl() ?: "unknown",
                cause = Exception("Server returned 404")
            ))
            429 -> {
                val retryAfter = response.headers["Retry-After"]?.toIntOrNull()
                Result.failure(AppError.Network.RateLimited(retryAfter))
            }
            in 500..599 -> Result.failure(AppError.Network.Unknown(
                message = "Server error: ${response.status}",
                cause = Exception("HTTP ${response.status}")
            ))
            else -> Result.failure(AppError.Network.Unknown(
                message = "Unexpected response: ${response.status}",
                cause = Exception("HTTP ${response.status}")
            ))
        }
    }

    /**
     * Handle HTTP response for boolean results (for delete/test)
     */
    private fun handleHttpResponseBoolean(response: HttpResponse): Result<Boolean> {
        return when (response.status.value) {
            in 200..299 -> Result.success(response.status.isSuccess())
            401 -> Result.failure(AppError.Auth.InvalidCredentials())
            403 -> Result.failure(AppError.Auth.Unauthorized())
            404 -> Result.failure(AppError.Network.ServerUnreachable(
                serverUrl = securePreferences.getNextcloudServerUrl() ?: "unknown",
                cause = Exception("Server returned 404")
            ))
            429 -> {
                val retryAfter = response.headers["Retry-After"]?.toIntOrNull()
                Result.failure(AppError.Network.RateLimited(retryAfter))
            }
            in 500..599 -> Result.failure(AppError.Network.Unknown(
                message = "Server error: ${response.status}",
                cause = Exception("HTTP ${response.status}")
            ))
            else -> Result.failure(AppError.Network.Unknown(
                message = "Unexpected response: ${response.status}",
                cause = Exception("HTTP ${response.status}")
            ))
        }
    }

    /**
     * Close the HTTP client.
     */
    fun close() {
        httpClient.close()
    }
}
