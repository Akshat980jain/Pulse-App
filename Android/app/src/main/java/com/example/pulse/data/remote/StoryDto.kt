package com.example.pulse.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote Data Transfer Object for the `stories` table in Supabase.
 */
@Serializable
data class StoryDto(
    @SerialName("id")         val id: String = "",
    @SerialName("user_id")    val userId: String = "",
    @SerialName("image_url")  val imageUrl: String? = null,
    @SerialName("content")    val content: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("expires_at") val expiresAt: String = ""
)

/**
 * UI-friendly representation of a Story, combining remote story details with
 * the author's cached profile information.
 */
data class StoryUiModel(
    val id: String,
    val userId: String,
    val imageUrl: String?,
    val content: String?,
    val createdAt: String,
    val expiresAt: String,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?
)
