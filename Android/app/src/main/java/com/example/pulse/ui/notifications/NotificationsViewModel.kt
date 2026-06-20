package com.example.pulse.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.data.repository.SupabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: SupabaseRepository
) : ViewModel() {

    private val _incomingRequests = MutableStateFlow<List<ProfileEntity>>(emptyList())
    val incomingRequests: StateFlow<List<ProfileEntity>> = _incomingRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refreshRequests()
    }

    fun refreshRequests() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            repository.getIncomingFollowRequests()
                .onSuccess {
                    _incomingRequests.value = it
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to load follow requests"
                }
            _isLoading.value = false
        }
    }

    fun confirmRequest(requesterUserId: String) {
        viewModelScope.launch {
            repository.acceptFollowRequest(requesterUserId)
                .onSuccess {
                    // Remove from list locally
                    _incomingRequests.value = _incomingRequests.value.filter { it.userId != requesterUserId }
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to accept follow request"
                }
        }
    }

    fun deleteRequest(requesterUserId: String) {
        viewModelScope.launch {
            repository.declineFollowRequest(requesterUserId)
                .onSuccess {
                    // Remove from list locally
                    _incomingRequests.value = _incomingRequests.value.filter { it.userId != requesterUserId }
                }
                .onFailure {
                    _error.value = it.localizedMessage ?: "Failed to decline follow request"
                }
        }
    }
}
