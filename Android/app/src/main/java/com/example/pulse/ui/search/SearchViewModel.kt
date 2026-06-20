package com.example.pulse.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.local.PostEntity
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.data.repository.SupabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SupabaseRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _userResults = MutableStateFlow<List<ProfileEntity>>(emptyList())
    val userResults = _userResults.asStateFlow()

    private val _postResults = MutableStateFlow<List<PostEntity>>(emptyList())
    val postResults = _postResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.trim().isEmpty()) {
            _userResults.value = emptyList()
            _postResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(500) // Debounce search
            performSearch(newQuery.trim())
        }
    }

    fun performSearch(searchQuery: String = _query.value.trim()) {
        if (searchQuery.isEmpty()) return
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val usersResult = repository.searchProfiles(searchQuery)
                val postsResult = repository.searchPosts(searchQuery)

                usersResult.onSuccess { users ->
                    _userResults.value = users
                }.onFailure {
                    _error.value = it.localizedMessage ?: "Error searching creators"
                }

                postsResult.onSuccess { posts ->
                    _postResults.value = posts
                }.onFailure {
                    _error.value = it.localizedMessage ?: "Error searching posts"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
