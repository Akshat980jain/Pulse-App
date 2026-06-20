package com.example.pulse.data.remote

import com.example.pulse.data.local.PostEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote Data Transfer Object for the `posts` table in Supabase.
 * All fields match Supabase's snake_case column names via @SerialName.
 * Nullable/default values prevent deserialization crashes when fields are absent.
 *
 * Profile fields (username, avatar_url, etc.) are NOT on the posts table —
 * they are fetched separately via ProfileDto and merged in the repository.
 */
@Serializable
data class PostDto(
    @SerialName("id")             val id: String = "",
    @SerialName("user_id")        val userId: String = "",
    @SerialName("content")        val content: String = "",
    @SerialName("type")           val type: String = "text",
    @SerialName("image_url")      val imageUrl: String? = null,
    @SerialName("audio_url")      val audioUrl: String? = null,
    @SerialName("video_url")      val videoUrl: String? = null,
    @SerialName("quoted_post_id") val quotedPostId: String? = null,
    @SerialName("is_flagged")     val isFlagged: Boolean = false,
    @SerialName("created_at")     val createdAt: String = "",
    @SerialName("scheduled_at")   val scheduledAt: String? = null,
    // Engagement counts — populated if your DB has these columns/views
    @SerialName("like_count")     val likeCount: Int = 0,
    @SerialName("comment_count")  val commentCount: Int = 0,
)

/**
 * Map PostDto + optional ProfileDto → local Room PostEntity.
 * The [profile] param carries the author's display info fetched in a
 * separate batch query by the repository.
 */
fun PostDto.toEntity(profile: ProfileDto? = null): PostEntity = PostEntity(
    id            = id,
    userId        = userId,
    content       = content,
    type          = type,
    imageUrl      = imageUrl,
    audioUrl      = audioUrl,
    videoUrl      = videoUrl,
    quotedPostId  = quotedPostId,
    isFlagged     = isFlagged,
    createdAt     = createdAt,
    scheduledAt   = scheduledAt,
    likeCount     = likeCount,
    commentCount  = commentCount,
    // Profile display fields merged from the profiles table
    username      = profile?.username,
    displayName   = profile?.displayName ?: profile?.fullName,
    avatarUrl     = profile?.avatarUrl,
    isVerified    = profile?.isVerified ?: false,
    commentsDisabled = false
)
