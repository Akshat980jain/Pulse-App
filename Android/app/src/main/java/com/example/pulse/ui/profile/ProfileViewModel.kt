package com.example.pulse.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.local.PostEntity
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.data.repository.SupabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: SupabaseRepository
) : ViewModel() {

    val currentUserId: String? get() = repository.getCurrentUserId()

    // Observable profile flow for the current user
    val profileState: StateFlow<ProfileEntity?> = if (currentUserId != null) {
        repository.getProfileFlow(currentUserId!!)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    } else {
        MutableStateFlow(null)
    }

    // Observable user's posts flow
    val userPosts: StateFlow<List<PostEntity>> = if (currentUserId != null) {
        repository.getUserPostsFlow(currentUserId!!)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else {
        MutableStateFlow(emptyList())
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _editError = MutableStateFlow<String?>(null)
    val editError = _editError.asStateFlow()

    private val _followerCount = MutableStateFlow(0)
    val followerCount: StateFlow<Int> = _followerCount.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    init {
        refreshProfileData()
    }

    fun refreshProfileData() {
        val uid = currentUserId ?: return
        _isRefreshing.value = true
        viewModelScope.launch {
            repository.refreshProfile(uid)
            repository.refreshPosts()
            // Fetch real follower / following counts from the follows table
            repository.getFollowCounts(uid).onSuccess { (followers, following) ->
                _followerCount.value = followers
                _followingCount.value = following
            }
            _isRefreshing.value = false
        }
    }

    fun editProfile(displayName: String?, bio: String?, avatarUrl: String?, onSuccess: () -> Unit) {
        val uid = currentUserId ?: return
        _editError.value = null
        viewModelScope.launch {
            repository.updateProfile(
                displayName = displayName,
                bio = bio,
                avatarUrl = avatarUrl
            ).onSuccess {
                repository.refreshProfile(uid)
                onSuccess()
            }.onFailure {
                _editError.value = it.localizedMessage ?: "Failed to update profile"
            }
        }
    }
}
