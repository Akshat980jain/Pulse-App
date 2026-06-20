package com.example.pulse.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CommentUiModel(
    val id: String,
    val postId: String,
    val userId: String,
    val content: String,
    val createdAt: String,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?
)
