package com.example.pulse.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.local.PostEntity
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.data.repository.SupabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtherUserProfileViewModel @Inject constructor(
    private val repository: SupabaseRepository
) : ViewModel() {

    private val _profile       = MutableStateFlow<ProfileEntity?>(null)
    val profile: StateFlow<ProfileEntity?> = _profile.asStateFlow()

    private val _posts         = MutableStateFlow<List<PostEntity>>(emptyList())
    val posts: StateFlow<List<PostEntity>> = _posts.asStateFlow()

    private val _followerCount = MutableStateFlow(0)
    val followerCount: StateFlow<Int> = _followerCount.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    private val _isFollowedByMe = MutableStateFlow(false)
    val isFollowedByMe: StateFlow<Boolean> = _isFollowedByMe.asStateFlow()

    private val _isFollowingMe = MutableStateFlow(false)
    val isFollowingMe: StateFlow<Boolean> = _isFollowingMe.asStateFlow()

    private val _isFollowRequestPending = MutableStateFlow(false)
    val isFollowRequestPending: StateFlow<Boolean> = _isFollowRequestPending.asStateFlow()

    private val _isLoading     = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error         = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var targetUserId: String = ""

    fun loadUser(userId: String) {
        if (targetUserId == userId && _profile.value != null) return
        targetUserId = userId
        val myId = repository.getCurrentUserId()
        _isLoading.value = true
        _error.value     = null

        viewModelScope.launch {
            // Profile
            repository.refreshProfile(userId)
                .onSuccess { _profile.value = it }
                .onFailure { _error.value = it.localizedMessage ?: "Failed to load profile" }

            // Posts (from Room flow — already populated by refreshPosts)
            repository.getUserPostsFlow(userId).collect { _posts.value = it }
        }

        viewModelScope.launch {
            // Follow counts
            repository.getFollowCounts(userId)
                .onSuccess { (followers, following) ->
                    _followerCount.value  = followers
                    _followingCount.value = following
                }
        }

        viewModelScope.launch {
            // Is current user already following this person?
            if (myId != null) {
                repository.getFollowingIds(myId)
                    .onSuccess { ids -> _isFollowedByMe.value = userId in ids }
            }
        }

        viewModelScope.launch {
            // Does this person follow the current user?
            if (myId != null) {
                repository.getFollowingIds(userId)
                    .onSuccess { ids -> _isFollowingMe.value = myId in ids }
            }
        }

        viewModelScope.launch {
            // Is there a pending follow request?
            repository.isFollowRequestPending(userId)
                .onSuccess { _isFollowRequestPending.value = it }
            
            _isLoading.value = false
        }
    }

    fun toggleFollow() {
        val myId = repository.getCurrentUserId() ?: return
        if (myId == targetUserId) return  // can't follow yourself

        val wasFollowing = _isFollowedByMe.value
        val wasRequested = _isFollowRequestPending.value
        val isPrivate = _profile.value?.isPrivate ?: false

        viewModelScope.launch {
            val result = if (wasFollowing) {
                // Unfollow
                _isFollowedByMe.value = false
                repository.unfollowUser(targetUserId)
            } else if (wasRequested) {
                // Cancel follow request
                _isFollowRequestPending.value = false
                repository.cancelFollowRequest(targetUserId)
            } else {
                // Follow / Follow Back
                if (isPrivate) {
                    _isFollowRequestPending.value = true
                    repository.sendFollowRequest(targetUserId)
                } else {
                    _isFollowedByMe.value = true
                    repository.followUser(targetUserId)
                }
            }

            result.onFailure {
                // Revert on error
                if (wasFollowing) {
                    _isFollowedByMe.value = true
                } else if (wasRequested) {
                    _isFollowRequestPending.value = true
                } else {
                    if (isPrivate) {
                        _isFollowRequestPending.value = false
                    } else {
                        _isFollowedByMe.value = false
                    }
                }
                _error.value = it.localizedMessage ?: "Failed to update follow relationship"
            }

            // Refresh follow counts
            repository.getFollowCounts(targetUserId)
                .onSuccess { (followers, following) ->
                    _followerCount.value  = followers
                    _followingCount.value = following
                }
        }
    }
}
