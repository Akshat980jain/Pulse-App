package com.example.pulse.data.repository

import com.example.pulse.data.local.MessageDao
import com.example.pulse.data.local.MessageEntity
import com.example.pulse.data.local.PostDao
import com.example.pulse.data.local.PostEntity
import com.example.pulse.data.local.ProfileDao
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.data.remote.PostDto
import com.example.pulse.data.remote.ProfileDto
import com.example.pulse.data.remote.StoryDto
import com.example.pulse.data.remote.StoryUiModel
import com.example.pulse.data.remote.toEntity
import com.example.pulse.data.remote.CommentUiModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val postDao: PostDao,
    private val profileDao: ProfileDao,
    private val messageDao: MessageDao
) : SupabaseRepository {

    override fun getPostsFlow(): Flow<List<PostEntity>> = postDao.getPostsFlow()

    override fun getReelsFlow(): Flow<List<PostEntity>> = postDao.getReelsFlow()

    /**
     * Two-step feed refresh:
     * 1. Fetch all posts from Supabase (PostDto — posts table columns only)
     * 2. Batch-fetch profiles for every unique user_id in those posts
     * 3. Merge profile display data onto each post entity before Room insert
     *
     * This is necessary because username/display_name/avatar_url live in
     * the `profiles` table, not in `posts`.
     */
    override suspend fun refreshPosts(): Result<Unit> = runCatching {
        val currentUserId = getCurrentUserId()

        // Step 1 — fetch posts
        val remotePosts = supabase.postgrest
            .from("posts")
            .select()
            .decodeList<PostDto>()

        val postIds = remotePosts.map { it.id }

        // Step 2 — Fetch all likes for these posts
        val likesData = if (postIds.isNotEmpty()) {
            supabase.postgrest.from("likes")
                .select {
                    filter {
                        isIn("post_id", postIds)
                    }
                }
                .decodeList<LikeDto>()
        } else {
            emptyList()
        }

        // Step 3 — Fetch all comments for these posts
        val commentsData = if (postIds.isNotEmpty()) {
            supabase.postgrest.from("comments")
                .select {
                    filter {
                        isIn("post_id", postIds)
                    }
                }
                .decodeList<CommentDto>()
        } else {
            emptyList()
        }

        // Step 4 — collect unique user IDs and batch-fetch their profiles
        val userIds = remotePosts.map { it.userId }.distinct()
        val profilesByUserId: Map<String, ProfileDto> = if (userIds.isNotEmpty()) {
            supabase.postgrest
                .from("profiles")
                .select {
                    filter {
                        isIn("user_id", userIds)
                    }
                }
                .decodeList<ProfileDto>()
                .associateBy { it.userId }   // keyed by auth user_id
        } else {
            emptyMap()
        }

        // Group counts
        val likesByPost = likesData.groupBy { it.postId }
        val commentsByPost = commentsData.groupBy { it.postId }
        val userLikedPostIds = likesData.filter { it.userId == currentUserId }.map { it.postId }.toSet()

        // Step 5 — merge profile data and engagement counts into each PostEntity
        val entities = remotePosts.map { post ->
            val profile = profilesByUserId[post.userId]
            val likeCount = likesByPost[post.id]?.size ?: 0
            val commentCount = commentsByPost[post.id]?.size ?: 0
            val likedByMe = userLikedPostIds.contains(post.id)
            val existing = postDao.getPostById(post.id)
            val commentsDisabled = existing?.commentsDisabled ?: false
            post.toEntity(profile).copy(
                likeCount = likeCount,
                commentCount = commentCount,
                likedByMe = likedByMe,
                commentsDisabled = commentsDisabled
            )
        }

        postDao.clearAllPosts()
        postDao.insertPosts(entities)
    }

    override suspend fun createPost(
        content: String,
        type: String,
        imageUrl: String?,
        audioUrl: String?,
        videoUrl: String?
    ): Result<Unit> = runCatching {
        val userId = getCurrentUserId() ?: throw Exception("User not authenticated")

        // Build a minimal insert payload (only columns that exist in the posts table)
        val post = PostEntity(
            id           = UUID.randomUUID().toString(),
            userId       = userId,
            content      = content,
            type         = type,
            imageUrl     = imageUrl,
            audioUrl     = audioUrl,
            videoUrl     = videoUrl,
            quotedPostId = null,
            isFlagged    = false,
            createdAt    = Instant.now().toString(),
            scheduledAt  = null,
            commentsDisabled = false
        )

        // Push to remote — Room entity only has post columns at this point
        supabase.postgrest.from("posts").insert(PostDto(
            id           = post.id,
            userId       = post.userId,
            content      = post.content,
            type         = post.type,
            imageUrl     = post.imageUrl,
            audioUrl     = post.audioUrl,
            videoUrl     = post.videoUrl,
            quotedPostId = post.quotedPostId,
            isFlagged    = post.isFlagged,
            createdAt    = post.createdAt,
            scheduledAt  = post.scheduledAt,
        ))

        // Enrich with current user's cached profile for instant UI feedback
        val myProfile = profileDao.getProfile(userId)
        val enrichedPost = post.copy(
            username    = myProfile?.username,
            displayName = myProfile?.displayName ?: myProfile?.fullName,
            avatarUrl   = myProfile?.avatarUrl,
            isVerified  = myProfile?.isVerified ?: false,
        )
        postDao.insertPost(enrichedPost)
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        supabase.postgrest.from("posts").delete {
            filter { eq("id", postId) }
        }
        postDao.deletePost(postId)
    }

    override suspend fun updatePostContent(postId: String, content: String): Result<Unit> = runCatching {
        supabase.postgrest.from("posts").update(
            mapOf("content" to content)
        ) {
            filter { eq("id", postId) }
        }
        postDao.updatePostContent(postId, content)
    }

    override suspend fun pinPostToProfile(postId: String?): Result<Unit> = runCatching {
        val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
        supabase.postgrest.from("profiles").update(
            mapOf("pinned_post_id" to postId)
        ) {
            filter { eq("user_id", userId) }
        }
        val current = profileDao.getProfile(userId)
        if (current != null) {
            val updated = current.copy(pinnedPostId = postId)
            profileDao.insertProfile(updated)
        }
    }

    override suspend fun unfollowUser(targetUserId: String): Result<Unit> = runCatching {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        supabase.postgrest.from("follows").delete {
            filter {
                eq("follower_id", currentUserId)
                eq("following_id", targetUserId)
            }
        }
    }

    override suspend fun followUser(targetUserId: String): Result<Unit> = runCatching {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        supabase.postgrest.from("follows").insert(
            mapOf(
                "follower_id"  to currentUserId,
                "following_id" to targetUserId
            )
        )
    }

    override suspend fun muteUser(targetUserId: String): Result<Unit> = runCatching {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        supabase.postgrest.from("mutes").insert(
            mapOf(
                "muter_id" to currentUserId,
                "muted_id" to targetUserId
            )
        )
    }

    override suspend fun getMutedUserIds(): Result<Set<String>> = runCatching {
        val currentUserId = getCurrentUserId() ?: return@runCatching emptySet<String>()
        @Serializable
        data class MutedUserDto(
            @SerialName("muted_id") val mutedId: String
        )
        val response = supabase.postgrest.from("mutes")
            .select {
                filter { eq("muter_id", currentUserId) }
            }
            .decodeList<MutedUserDto>()
        response.map { it.mutedId }.toSet()
    }

    override suspend fun reportPost(postId: String, reason: String): Result<Unit> = runCatching {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        supabase.postgrest.from("reports").insert(
            mapOf(
                "reporter_id" to currentUserId,
                "post_id" to postId,
                "reason" to reason
            )
        )
    }

    override suspend fun updateCommentsDisabled(postId: String, disabled: Boolean): Result<Unit> = runCatching {
        postDao.updateCommentsDisabled(postId, disabled)
    }

    // ── Profile ──────────────────────────────────────────────────────────────

    override fun getProfileFlow(userId: String): Flow<ProfileEntity?> =
        profileDao.getProfileFlow(userId)

    /**
     * Fetch a full profile from Supabase and cache it locally.
     * Uses ProfileDto for deserialization (snake_case @SerialName mappings)
     * then maps to the Room ProfileEntity.
     */
    override suspend fun refreshProfile(userId: String): Result<ProfileEntity> = runCatching {
        val dto = supabase.postgrest
            .from("profiles")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeSingle<ProfileDto>()

        val entity = ProfileEntity(
            id           = dto.id,
            userId       = dto.userId,
            username     = dto.username,
            displayName  = dto.displayName,
            fullName     = dto.fullName,
            avatarUrl    = dto.avatarUrl,
            coverUrl     = null,
            bio          = dto.bio,
            isPrivate    = dto.isPrivate,
            isVerified   = dto.isVerified,
            dateOfBirth  = null,
            gender       = null,
            phone        = null,
            pinnedPostId = dto.pinnedPostId,
            createdAt    = Instant.now().toString(),
            updatedAt    = Instant.now().toString(),
        )
        profileDao.insertProfile(entity)
        entity
    }

    override suspend fun getSuggestedProfiles(): Result<List<ProfileEntity>> = runCatching {
        val currentUserId = getCurrentUserId()
        val dtos = supabase.postgrest
            .from("profiles")
            .select {
                limit(15)
            }
            .decodeList<ProfileDto>()

        val entities = dtos
            .filter { it.userId != currentUserId }
            .map { dto ->
                ProfileEntity(
                    id           = dto.id,
                    userId       = dto.userId,
                    username     = dto.username,
                    displayName  = dto.displayName,
                    fullName     = dto.fullName,
                    avatarUrl    = dto.avatarUrl,
                    coverUrl     = null,
                    bio          = dto.bio,
                    isPrivate    = dto.isPrivate,
                    isVerified   = dto.isVerified,
                    dateOfBirth  = null,
                    gender       = null,
                    phone        = null,
                    pinnedPostId = dto.pinnedPostId,
                    createdAt    = "",
                    updatedAt    = "",
                )
            }
        profileDao.insertProfiles(entities)
        entities
    }

    override suspend fun searchProfiles(query: String): Result<List<ProfileEntity>> = runCatching {
        val dtos = supabase.postgrest
            .from("profiles")
            .select {
                filter {
                    or {
                        ilike("username", "%$query%")
                        ilike("display_name", "%$query%")
                    }
                }
                limit(20)
            }
            .decodeList<ProfileDto>()

        dtos.map { dto ->
            ProfileEntity(
                id           = dto.id,
                userId       = dto.userId,
                username     = dto.username,
                displayName  = dto.displayName,
                fullName     = dto.fullName,
                avatarUrl    = dto.avatarUrl,
                coverUrl     = null,
                bio          = dto.bio,
                isPrivate    = dto.isPrivate,
                isVerified   = dto.isVerified,
                dateOfBirth  = null,
                gender       = null,
                phone        = null,
                pinnedPostId = dto.pinnedPostId,
                createdAt    = "",
                updatedAt    = "",
            )
        }
    }

    override suspend fun searchPosts(query: String): Result<List<PostEntity>> = runCatching {
        val currentUserId = getCurrentUserId()

        val remotePosts = supabase.postgrest
            .from("posts")
            .select {
                filter {
                    ilike("content", "%$query%")
                }
                limit(30)
            }
            .decodeList<PostDto>()

        val postIds = remotePosts.map { it.id }

        // Fetch likes
        val likesData = if (postIds.isNotEmpty()) {
            supabase.postgrest.from("likes")
                .select {
                    filter {
                        isIn("post_id", postIds)
                    }
                }
                .decodeList<LikeDto>()
        } else {
            emptyList()
        }

        // Fetch comments
        val commentsData = if (postIds.isNotEmpty()) {
            supabase.postgrest.from("comments")
                .select {
                    filter {
                        isIn("post_id", postIds)
                    }
                }
                .decodeList<CommentDto>()
        } else {
            emptyList()
        }

        val userIds = remotePosts.map { it.userId }.distinct()
        val profilesByUserId = if (userIds.isNotEmpty()) {
            supabase.postgrest
                .from("profiles")
                .select {
                    filter {
                        isIn("user_id", userIds)
                    }
                }
                .decodeList<ProfileDto>()
                .associateBy { it.userId }
        } else {
            emptyMap()
        }

        val likesByPost = likesData.groupBy { it.postId }
        val commentsByPost = commentsData.groupBy { it.postId }
        val userLikedPostIds = likesData.filter { it.userId == currentUserId }.map { it.postId }.toSet()

        remotePosts.map { post ->
            val profile = profilesByUserId[post.userId]
            val likeCount = likesByPost[post.id]?.size ?: 0
            val commentCount = commentsByPost[post.id]?.size ?: 0
            val likedByMe = userLikedPostIds.contains(post.id)
            post.toEntity(profile).copy(
                likeCount = likeCount,
                commentCount = commentCount,
                likedByMe = likedByMe
            )
        }
    }

    override fun getUserPostsFlow(userId: String): Flow<List<PostEntity>> =
        postDao.getUserPostsFlow(userId)

    override suspend fun updateProfile(
        displayName: String?,
        bio: String?,
        avatarUrl: String?
    ): Result<Unit> = runCatching {
        val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
        val current = profileDao.getProfile(userId) ?: throw Exception("Profile not found locally")

        val updated = current.copy(
            displayName = displayName ?: current.displayName,
            bio         = bio ?: current.bio,
            avatarUrl   = avatarUrl ?: current.avatarUrl,
            updatedAt   = Instant.now().toString()
        )

        // Only send the columns that actually exist in the profiles table
        supabase.postgrest.from("profiles").update(
            mapOf(
                "display_name" to updated.displayName,
                "bio"          to updated.bio,
                "avatar_url"   to updated.avatarUrl,
                "updated_at"   to updated.updatedAt,
            )
        ) {
            filter { eq("user_id", userId) }
        }
        profileDao.insertProfile(updated)
    }

    // ── Messages ─────────────────────────────────────────────────────────────

    override fun getMessagesFlow(otherUserId: String): Flow<List<MessageEntity>> {
        val myId = getCurrentUserId() ?: ""
        return messageDao.getMessagesFlow(myId, otherUserId)
    }

    override suspend fun refreshMessages(otherUserId: String): Result<Unit> = runCatching {
        val myId = getCurrentUserId() ?: throw Exception("User not authenticated")
        val remoteMessages = supabase.postgrest
            .from("messages")
            .select()
            .decodeList<MessageEntity>()
            .filter {
                (it.senderId == myId && it.receiverId == otherUserId) ||
                (it.senderId == otherUserId && it.receiverId == myId)
            }
        messageDao.insertMessages(remoteMessages)
    }

    override suspend fun sendMessage(receiverId: String, content: String): Result<Unit> = runCatching {
        val myId = getCurrentUserId() ?: throw Exception("User not authenticated")
        val message = MessageEntity(
            id         = UUID.randomUUID().toString(),
            senderId   = myId,
            receiverId = receiverId,
            content    = content,
            read       = false,
            createdAt  = Instant.now().toString()
        )
        supabase.postgrest.from("messages").insert(message)
        messageDao.insertMessage(message)
    }

    // ── Stories ──────────────────────────────────────────────────────────────

    override suspend fun getStories(): Result<List<StoryUiModel>> = runCatching {
        val nowIso = Instant.now().toString()
        val dtos = supabase.postgrest
            .from("stories")
            .select {
                filter {
                    gte("expires_at", nowIso)
                }
            }
            .decodeList<StoryDto>()

        val userIds = dtos.map { it.userId }.distinct()
        val profilesByUserId = if (userIds.isNotEmpty()) {
            supabase.postgrest
                .from("profiles")
                .select {
                    filter {
                        isIn("user_id", userIds)
                    }
                }
                .decodeList<ProfileDto>()
                .associateBy { it.userId }
        } else {
            emptyMap()
        }

        dtos.map { story ->
            val profile = profilesByUserId[story.userId]
            StoryUiModel(
                id = story.id,
                userId = story.userId,
                imageUrl = story.imageUrl,
                content = story.content,
                createdAt = story.createdAt,
                expiresAt = story.expiresAt,
                username = profile?.username,
                displayName = profile?.displayName ?: profile?.fullName,
                avatarUrl = profile?.avatarUrl
            )
        }
    }

    override suspend fun createStory(
        content: String?,
        mediaBytes: ByteArray?,
        mimeType: String?
    ): Result<Unit> = runCatching {
        val userId = getCurrentUserId() ?: throw Exception("User not authenticated")

        var imageUrl: String? = null
        if (mediaBytes != null) {
            val extension = when {
                mimeType?.contains("video") == true -> "mp4"
                else -> "jpg"
            }
            val fileName = "$userId/${System.currentTimeMillis()}.$extension"
            val bucket = supabase.storage.from("post-images")
            bucket.upload(fileName, mediaBytes)
            imageUrl = bucket.publicUrl(fileName)
        }

        val insertPayload = mapOf(
            "user_id" to userId,
            "content" to content,
            "image_url" to imageUrl
        )
        supabase.postgrest.from("stories").insert(insertPayload)
    }

    // ── Auth ─────────────────────────────────────────────────────────────────
 
    override fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    override suspend fun getFollowCounts(userId: String): Result<Pair<Int, Int>> = runCatching {
        @Serializable
        data class FollowDto(
            @SerialName("follower_id")  val followerId: String  = "",
            @SerialName("following_id") val followingId: String = ""
        )

        // Followers = rows where following_id == userId
        val followers = supabase.postgrest
            .from("follows")
            .select {
                filter { eq("following_id", userId) }
            }
            .decodeList<FollowDto>()

        // Following = rows where follower_id == userId
        val following = supabase.postgrest
            .from("follows")
            .select {
                filter { eq("follower_id", userId) }
            }
            .decodeList<FollowDto>()

        Pair(followers.size, following.size)
    }

    override suspend fun getFollowingIds(userId: String): Result<List<String>> = runCatching {
        @Serializable
        data class FollowingDto(@SerialName("following_id") val followingId: String = "")

        supabase.postgrest
            .from("follows")
            .select {
                filter { eq("follower_id", userId) }
            }
            .decodeList<FollowingDto>()
            .map { it.followingId }
    }

    // Likes & Comments
    override suspend fun toggleLike(postId: String): Result<Unit> = runCatching {
        val userId = getCurrentUserId() ?: throw IllegalStateException("Not authenticated")
        val post = postDao.getPostById(postId) ?: throw IllegalArgumentException("Post not found")
        
        val isLiked = post.likedByMe
        if (isLiked) {
            supabase.postgrest.from("likes")
                .delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
            
            val updatedPost = post.copy(
                likedByMe = false,
                likeCount = (post.likeCount - 1).coerceAtLeast(0)
            )
            postDao.insertPost(updatedPost)
        } else {
            supabase.postgrest.from("likes").insert(LikeDto(postId = postId, userId = userId))
            
            val updatedPost = post.copy(
                likedByMe = true,
                likeCount = post.likeCount + 1
            )
            postDao.insertPost(updatedPost)
        }
    }

    override suspend fun getCommentsForPost(postId: String): Result<List<CommentUiModel>> = runCatching {
        val remoteComments = supabase.postgrest
            .from("comments")
            .select {
                filter {
                    eq("post_id", postId)
                }
            }
            .decodeList<CommentDto>()

        val userIds = remoteComments.map { it.userId }.distinct()
        val profilesByUserId = if (userIds.isNotEmpty()) {
            supabase.postgrest
                .from("profiles")
                .select {
                    filter {
                        isIn("user_id", userIds)
                    }
                }
                .decodeList<ProfileDto>()
                .associateBy { it.userId }
        } else {
            emptyMap()
        }

        remoteComments.map { comment ->
            val profile = profilesByUserId[comment.userId]
            CommentUiModel(
                id = comment.id,
                postId = comment.postId,
                userId = comment.userId,
                content = comment.content,
                createdAt = comment.createdAt,
                username = profile?.username,
                displayName = profile?.displayName ?: profile?.fullName,
                avatarUrl = profile?.avatarUrl
            )
        }
    }

    override suspend fun addComment(postId: String, content: String): Result<Unit> = runCatching {
        val userId = getCurrentUserId() ?: throw IllegalStateException("Not authenticated")
        
        supabase.postgrest.from("comments").insert(
            CommentDto(
                postId = postId,
                userId = userId,
                content = content
            )
        )
        
        val post = postDao.getPostById(postId)
        if (post != null) {
            val updatedPost = post.copy(commentCount = post.commentCount + 1)
            postDao.insertPost(updatedPost)
        }
    }

    override suspend fun sendFollowRequest(targetUserId: String): Result<Unit> = runCatching {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        supabase.postgrest.from("follow_requests").insert(
            mapOf(
                "requester_id" to currentUserId,
                "target_id"    to targetUserId,
                "status"       to "pending"
            )
        )
    }

    override suspend fun cancelFollowRequest(targetUserId: String): Result<Unit> = runCatching {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        supabase.postgrest.from("follow_requests").delete {
            filter {
                eq("requester_id", currentUserId)
                eq("target_id", targetUserId)
            }
        }
    }

    override suspend fun getIncomingFollowRequests(): Result<List<ProfileEntity>> = runCatching {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        
        @Serializable
        data class FollowRequestDto(
            @SerialName("requester_id") val requesterId: String = ""
        )

        val requests = supabase.postgrest.from("follow_requests")
            .select {
                filter {
                    eq("target_id", currentUserId)
                    eq("status", "pending")
                }
            }
            .decodeList<FollowRequestDto>()

        val requesterIds = requests.map { it.requesterId }.distinct()
        if (requesterIds.isEmpty()) return@runCatching emptyList()

        val dtos = supabase.postgrest.from("profiles")
            .select {
                filter {
                    isIn("user_id", requesterIds)
                }
            }
            .decodeList<ProfileDto>()

        dtos.map { dto ->
            ProfileEntity(
                id           = dto.id,
                userId       = dto.userId,
                username     = dto.username,
                displayName  = dto.displayName,
                fullName     = dto.fullName,
                avatarUrl    = dto.avatarUrl,
                coverUrl     = null,
                bio          = dto.bio,
                isPrivate    = dto.isPrivate,
                isVerified   = dto.isVerified,
                dateOfBirth  = null,
                gender       = null,
                phone        = null,
                pinnedPostId = dto.pinnedPostId,
                createdAt    = "",
                updatedAt    = "",
            )
        }
    }

    override suspend fun acceptFollowRequest(requesterUserId: String): Result<Unit> = runCatching {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        
        // 1. Delete request
        supabase.postgrest.from("follow_requests").delete {
            filter {
                eq("requester_id", requesterUserId)
                eq("target_id", currentUserId)
            }
        }

        // 2. Insert into follows (requester follows current user)
        supabase.postgrest.from("follows").insert(
            mapOf(
                "follower_id"  to requesterUserId,
                "following_id" to currentUserId
            )
        )
    }

    override suspend fun declineFollowRequest(requesterUserId: String): Result<Unit> = runCatching {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        supabase.postgrest.from("follow_requests").delete {
            filter {
                eq("requester_id", requesterUserId)
                eq("target_id", currentUserId)
            }
        }
    }

    override suspend fun isFollowRequestPending(targetUserId: String): Result<Boolean> = runCatching {
        val currentUserId = getCurrentUserId() ?: return@runCatching false
        
        @Serializable
        data class FollowRequestDtoId(
            @SerialName("id") val id: Int = 0
        )

        val requests = supabase.postgrest.from("follow_requests")
            .select {
                filter {
                    eq("requester_id", currentUserId)
                    eq("target_id", targetUserId)
                    eq("status", "pending")
                }
            }
            .decodeList<FollowRequestDtoId>()

        requests.isNotEmpty()
    }
}

@Serializable
data class LikeDto(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
data class CommentDto(
    @SerialName("id") val id: String = "",
    @SerialName("post_id") val postId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("content") val content: String = "",
    @SerialName("created_at") val createdAt: String = ""
)
