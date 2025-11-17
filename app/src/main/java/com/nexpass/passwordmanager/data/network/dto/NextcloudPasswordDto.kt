package com.nexpass.passwordmanager.data.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO for Nextcloud Passwords API password object.
 *
 * Based on Nextcloud Passwords app API v1.0
 * https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api
 */
@Serializable
data class NextcloudPasswordDto(
    val id: String,
    val label: String,
    val username: String = "",
    val password: String,  // Encrypted by us before sending
    val url: String = "",
    val notes: String = "",
    val customFields: String = "",
    val status: Int = 0,
    val statusCode: String = "GOOD",
    val hash: String = "",
    val folder: String = "00000000-0000-0000-0000-000000000000",  // Default folder
    val revision: String = "",
    val share: String? = null,
    val shared: Boolean = false,
    val cseType: String = "none",  // Client-side encryption type (we handle our own)
    val cseKey: String = "",
    val sseType: String = "SSEv2r1",  // Server-side encryption type
    val hidden: Boolean = false,
    val trashed: Boolean = false,
    val favorite: Boolean = false,
    val editable: Boolean = true,
    val edited: Long,  // Unix timestamp in seconds
    val created: Long,  // Unix timestamp in seconds
    val updated: Long,  // Unix timestamp in seconds
    val tags: List<String> = emptyList()
)

/**
 * Response wrapper for password list endpoint.
 */
@Serializable
data class NextcloudPasswordListResponse(
    val passwords: List<NextcloudPasswordDto>? = null
)

/**
 * Response wrapper for single password endpoints.
 */
@Serializable
data class NextcloudPasswordResponse(
    val id: String,
    val label: String,
    val username: String = "",
    val password: String,
    val url: String = "",
    val notes: String = "",
    val customFields: String = "",
    val status: Int = 0,
    val statusCode: String = "GOOD",
    val hash: String = "",
    val folder: String = "00000000-0000-0000-0000-000000000000",
    val revision: String = "",
    val share: String? = null,
    val shared: Boolean = false,
    val cseType: String = "none",
    val cseKey: String = "",
    val sseType: String = "SSEv2r1",
    val hidden: Boolean = false,
    val trashed: Boolean = false,
    val favorite: Boolean = false,
    val editable: Boolean = true,
    val edited: Long,
    val created: Long,
    val updated: Long,
    val tags: List<String> = emptyList()
)

/**
 * Request body for creating a password.
 */
@Serializable
data class CreatePasswordRequest(
    val password: String,
    val label: String,
    val username: String = "",
    val url: String = "",
    val notes: String = "",
    val folder: String = "00000000-0000-0000-0000-000000000000",
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val hidden: Boolean = false,
    val customFields: String = ""
)

/**
 * Request body for updating a password.
 */
@Serializable
data class UpdatePasswordRequest(
    val id: String,
    val password: String,
    val label: String,
    val username: String = "",
    val url: String = "",
    val notes: String = "",
    val folder: String = "00000000-0000-0000-0000-000000000000",
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val hidden: Boolean = false,
    val customFields: String = ""
)

/**
 * Generic API error response.
 */
@Serializable
data class NextcloudErrorResponse(
    val status: String? = null,
    val message: String? = null
)
