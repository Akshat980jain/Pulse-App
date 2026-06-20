package com.example.pulse.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    fun getProfileFlow(userId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileEntity>)
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE type = 'reels' ORDER BY createdAt DESC")
    fun getReelsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserPostsFlow(userId: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deletePost(id: String)

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: String): PostEntity?

    @Query("UPDATE posts SET content = :content WHERE id = :id")
    suspend fun updatePostContent(id: String, content: String)

    @Query("UPDATE posts SET commentsDisabled = :disabled WHERE id = :id")
    suspend fun updateCommentsDisabled(id: String, disabled: Boolean)

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE (senderId = :myId AND receiverId = :otherId) OR (senderId = :otherId AND receiverId = :myId) ORDER BY createdAt ASC")
    fun getMessagesFlow(myId: String, otherId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun getDraftsFlow(): Flow<List<DraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE localId = :localId")
    suspend fun deleteDraft(localId: Int)
}
