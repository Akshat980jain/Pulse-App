package com.example.pulse.data.repository

import com.example.pulse.data.local.MessageEntity
import com.example.pulse.data.local.PostEntity
import com.example.pulse.data.local.ProfileEntity
import kotlinx.coroutines.flow.Flow

import com.example.pulse.data.remote.StoryUiModel
import com.example.pulse.data.remote.CommentUiModel

interface SupabaseRepository {
    // Post Operations
    fun getPostsFlow(): Flow<List<PostEntity>>
    fun getReelsFlow(): Flow<List<PostEntity>>
    suspend fun refreshPosts(): Result<Unit>
    suspend fun createPost(
        content: String,
        type: String,
        imageUrl: String? = null,
        audioUrl: String? = null,
        videoUrl: String? = null
    ): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun updatePostContent(postId: String, content: String): Result<Unit>
    suspend fun pinPostToProfile(postId: String?): Result<Unit>
    suspend fun unfollowUser(targetUserId: String): Result<Unit>
    suspend fun followUser(targetUserId: String): Result<Unit>
    suspend fun muteUser(targetUserId: String): Result<Unit>
    suspend fun getMutedUserIds(): Result<Set<String>>
    suspend fun reportPost(postId: String, reason: String): Result<Unit>
    suspend fun updateCommentsDisabled(postId: String, disabled: Boolean): Result<Unit>

    // Profile Operations
    fun getProfileFlow(userId: String): Flow<ProfileEntity?>
    suspend fun refreshProfile(userId: String): Result<ProfileEntity>
    suspend fun updateProfile(
        displayName: String?,
        bio: String?,
        avatarUrl: String?
    ): Result<Unit>
    suspend fun getSuggestedProfiles(): Result<List<ProfileEntity>>
    suspend fun searchProfiles(query: String): Result<List<ProfileEntity>>

    // Search Operations
    suspend fun searchPosts(query: String): Result<List<PostEntity>>

    // Specific User Posts
    fun getUserPostsFlow(userId: String): Flow<List<PostEntity>>

    // Stories Operations
    suspend fun getStories(): Result<List<StoryUiModel>>
    suspend fun createStory(
        content: String?,
        mediaBytes: ByteArray?,
        mimeType: String?
    ): Result<Unit>

    // Messages Operations
    fun getMessagesFlow(otherUserId: String): Flow<List<MessageEntity>>
    suspend fun refreshMessages(otherUserId: String): Result<Unit>
    suspend fun sendMessage(receiverId: String, content: String): Result<Unit>
    
    // Likes & Comments
    suspend fun toggleLike(postId: String): Result<Unit>
    suspend fun getCommentsForPost(postId: String): Result<List<CommentUiModel>>
    suspend fun addComment(postId: String, content: String): Result<Unit>

    // Auth Helpers
    fun getCurrentUserId(): String?

    // Follow Counts
    suspend fun getFollowCounts(userId: String): Result<Pair<Int, Int>>

    // Get IDs of users the given user follows
    suspend fun getFollowingIds(userId: String): Result<List<String>>

    // Follow Requests operations
    suspend fun sendFollowRequest(targetUserId: String): Result<Unit>
    suspend fun cancelFollowRequest(targetUserId: String): Result<Unit>
    suspend fun getIncomingFollowRequests(): Result<List<ProfileEntity>>
    suspend fun acceptFollowRequest(requesterUserId: String): Result<Unit>
    suspend fun declineFollowRequest(requesterUserId: String): Result<Unit>
    suspend fun isFollowRequestPending(targetUserId: String): Result<Boolean>
}
