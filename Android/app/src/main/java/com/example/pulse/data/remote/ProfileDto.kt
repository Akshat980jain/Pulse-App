package com.example.pulse.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lightweight remote DTO for the `profiles` table.
 * Used when batch-joining profiles onto posts during feed refresh.
 * Only contains the fields needed to display a post author.
 */
@Serializable
data class ProfileDto(
    @SerialName("id")           val id: String = "",
    @SerialName("user_id")      val userId: String = "",
    @SerialName("username")     val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("full_name")    val fullName: String? = null,
    @SerialName("avatar_url")   val avatarUrl: String? = null,
    @SerialName("is_verified")  val isVerified: Boolean = false,
    @SerialName("is_private")   val isPrivate: Boolean = false,
    @SerialName("bio")          val bio: String? = null,
    @SerialName("pinned_post_id") val pinnedPostId: String? = null,
)
