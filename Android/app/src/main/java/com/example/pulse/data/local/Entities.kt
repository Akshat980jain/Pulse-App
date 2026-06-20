package com.example.pulse.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String?,
    val displayName: String?,
    val fullName: String?,
    val avatarUrl: String?,
    val coverUrl: String?,
    val bio: String?,
    val isPrivate: Boolean = false,
    val isVerified: Boolean = false,
    val dateOfBirth: String?,
    val gender: String?,
    val phone: String?,
    val pinnedPostId: String?,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val content: String,
    val type: String, // "text", "image", "audio", "video", "reels"
    val imageUrl: String?,
    val audioUrl: String?,
    val videoUrl: String?,
    val quotedPostId: String?,
    val isFlagged: Boolean = false,
    val createdAt: String,
    val scheduledAt: String?,
    // Cached from profile join — avoids extra DB lookups in the feed
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedByMe: Boolean = false,
    val commentsDisabled: Boolean = false
)

@Serializable
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val read: Boolean = false,
    val createdAt: String
)

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val content: String,
    val mediaPaths: String, // JSON Array string for local media files
    val pollQuestion: String?,
    val pollOptions: String?, // JSON Array string
    val updatedAt: Long
)
