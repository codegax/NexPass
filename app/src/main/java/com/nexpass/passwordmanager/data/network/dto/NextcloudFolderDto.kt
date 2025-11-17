package com.nexpass.passwordmanager.data.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO for Nextcloud Passwords API folder object.
 *
 * Based on Nextcloud Passwords app API v1.0
 * https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Folder-Api
 */
@Serializable
data class NextcloudFolderDto(
    val id: String,
    val label: String,
    val parent: String = "00000000-0000-0000-0000-000000000000",  // Parent folder ID (default = root)
    val revision: String = "",
    val cseType: String = "none",  // Client-side encryption type
    val cseKey: String = "",
    val sseType: String = "SSEv2r1",  // Server-side encryption type
    val hidden: Boolean = false,
    val trashed: Boolean = false,
    val editable: Boolean = true,
    val edited: Long,  // Unix timestamp in seconds
    val created: Long,  // Unix timestamp in seconds
    val updated: Long   // Unix timestamp in seconds
)

/**
 * Request body for creating a folder.
 */
@Serializable
data class CreateFolderRequest(
    val label: String,
    val parent: String = "00000000-0000-0000-0000-000000000000",
    val hidden: Boolean = false
)

/**
 * Request body for updating a folder.
 */
@Serializable
data class UpdateFolderRequest(
    val id: String,
    val label: String,
    val parent: String = "00000000-0000-0000-0000-000000000000",
    val hidden: Boolean = false
)
