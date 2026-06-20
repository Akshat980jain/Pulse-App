package com.example.pulse.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.local.PostEntity
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.data.remote.StoryUiModel
import com.example.pulse.data.remote.CommentUiModel
import com.example.pulse.data.repository.SupabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: SupabaseRepository
) : ViewModel() {

    private val _hiddenPostIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenPostIds: StateFlow<Set<String>> = _hiddenPostIds.asStateFlow()

    private val _mutedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val mutedUserIds: StateFlow<Set<String>> = _mutedUserIds.asStateFlow()

    // Tab: "following" shows only followed users' posts; "discover" shows all
    private val _feedTab = MutableStateFlow("following")
    val feedTab: StateFlow<String> = _feedTab.asStateFlow()

    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())

    val posts: StateFlow<List<PostEntity>> = combine(
        repository.getPostsFlow(),
        _hiddenPostIds,
        _mutedUserIds,
        _feedTab,
        _followingIds
    ) { postsList, hiddenIds, mutedIds, tab, followingIds ->
        val userId = repository.getCurrentUserId()
        postsList.filter { post ->
            val notHidden  = post.id !in hiddenIds
            val notMuted   = post.userId !in mutedIds
            val inFeedTab  = when (tab) {
                "following" -> post.userId == userId || post.userId in followingIds
                else        -> true  // "discover" shows everything
            }
            notHidden && notMuted && inFeedTab
        }
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** The current auth user's profile — drives the avatar + initials in the TopBar. */
    val currentProfile: StateFlow<ProfileEntity?> = repository
        .getProfileFlow(repository.getCurrentUserId() ?: "")
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _newPostContent = MutableStateFlow("")
    val newPostContent: StateFlow<String> = _newPostContent.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _suggestedProfiles = MutableStateFlow<List<ProfileEntity>>(emptyList())
    val suggestedProfiles: StateFlow<List<ProfileEntity>> = _suggestedProfiles.asStateFlow()

    private val _activeStories = MutableStateFlow<List<StoryUiModel>>(emptyList())
    val activeStories: StateFlow<List<StoryUiModel>> = _activeStories.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentUiModel>>(emptyList())
    val comments: StateFlow<List<CommentUiModel>> = _comments.asStateFlow()

    val currentUserId: String? get() = repository.getCurrentUserId()

    init {
        refreshFeed()
        // Warm up the current user's profile in the local cache
        currentUserId?.let { uid ->
            viewModelScope.launch {
                repository.refreshProfile(uid)
            }
        }
        viewModelScope.launch {
            repository.getMutedUserIds()
                .onSuccess { _mutedUserIds.value = it }
        }
        // Load the list of followed user IDs for feed filtering
        viewModelScope.launch {
            currentUserId?.let { uid ->
                repository.getFollowingIds(uid)
                    .onSuccess { ids -> _followingIds.value = ids.toSet() }
            }
        }
        refreshSuggestedProfiles()
        refreshStories()
    }

    fun setFeedTab(tab: String) {
        _feedTab.value = tab
    }

    fun setNewPostContent(value: String) {
        _newPostContent.value = value
    }

    fun refreshSuggestedProfiles() {
        viewModelScope.launch {
            repository.getSuggestedProfiles()
                .onSuccess {
                    _suggestedProfiles.value = it
                }
        }
    }

    fun refreshStories() {
        viewModelScope.launch {
            repository.getStories()
                .onSuccess {
                    _activeStories.value = it
                }
        }
    }

    fun publishStory(content: String?, mediaBytes: ByteArray?, mimeType: String?) {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.createStory(content, mediaBytes, mimeType)
                .onSuccess {
                    refreshStories()
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to upload story"
                }
            _isRefreshing.value = false
        }
    }

    fun refreshFeed() {
        _isRefreshing.value = true
        _error.value = null
        viewModelScope.launch {
            // Refresh following IDs so new follows/unfollows are reflected
            currentUserId?.let { uid ->
                repository.getFollowingIds(uid)
                    .onSuccess { ids -> _followingIds.value = ids.toSet() }
            }
            repository.refreshPosts()
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to refresh feed"
                }
            repository.getSuggestedProfiles()
                .onSuccess {
                    _suggestedProfiles.value = it
                }
            repository.getStories()
                .onSuccess {
                    _activeStories.value = it
                }
            _isRefreshing.value = false
        }
    }

    fun submitPost() {
        val content = _newPostContent.value.trim()
        if (content.isEmpty()) return
        _newPostContent.value = ""
        viewModelScope.launch {
            repository.createPost(content = content, type = "text")
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to submit post"
                }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to delete post"
                }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId)
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to toggle like"
                }
        }
    }

    fun fetchComments(postId: String) {
        viewModelScope.launch {
            _comments.value = emptyList() // clear first
            repository.getCommentsForPost(postId)
                .onSuccess {
                    _comments.value = it
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to load comments"
                }
        }
    }

    fun submitComment(postId: String, content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addComment(postId, trimmed)
                .onSuccess {
                    fetchComments(postId)
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to submit comment"
                }
        }
    }

    fun editPostCaption(postId: String, newContent: String) {
        viewModelScope.launch {
            repository.updatePostContent(postId, newContent)
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to update caption"
                }
        }
    }

    fun pinPostToProfile(postId: String?, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.pinPostToProfile(postId)
                .onSuccess {
                    currentUserId?.let { uid ->
                        repository.refreshProfile(uid)
                    }
                    onComplete()
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to pin post"
                }
        }
    }

    fun toggleCommentsDisabled(post: PostEntity) {
        viewModelScope.launch {
            val updatedVal = !post.commentsDisabled
            repository.updateCommentsDisabled(post.id, updatedVal)
        }
    }

    fun hidePost(postId: String) {
        _hiddenPostIds.value = _hiddenPostIds.value + postId
    }

    fun unfollowCreator(creatorUserId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.unfollowUser(creatorUserId)
                .onSuccess {
                    onComplete()
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to unfollow creator"
                }
        }
    }

    fun muteCreator(creatorUserId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.muteUser(creatorUserId)
                .onSuccess {
                    _mutedUserIds.value = _mutedUserIds.value + creatorUserId
                    onComplete()
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to mute creator"
                }
        }
    }

    fun reportPost(postId: String, reason: String = "Inappropriate content", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reportPost(postId, reason)
                .onSuccess {
                    onComplete()
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to report post"
                }
        }
    }
}
