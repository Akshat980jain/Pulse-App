package com.example.pulse.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject

// Minimal DTO for username uniqueness check
@Serializable
private data class UsernameCheckDto(
    @SerialName("username") val username: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _isSignUp = MutableStateFlow(false)
    val isSignUp: StateFlow<Boolean> = _isSignUp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val sessionStatus: StateFlow<SessionStatus> = supabase.auth.sessionStatus

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                _isAuthenticated.value = status is SessionStatus.Authenticated
            }
        }
    }

    fun setEmail(value: String)           { _email.value = value }
    fun setPassword(value: String)        { _password.value = value }
    fun setDisplayName(value: String)     { _displayName.value = value }
    fun setUsername(value: String)        { _username.value = value }
    fun setConfirmPassword(value: String) { _confirmPassword.value = value }

    fun toggleAuthMode() {
        _isSignUp.value = !_isSignUp.value
        _error.value = null
        _username.value = ""
        _confirmPassword.value = ""
        _displayName.value = ""
        _email.value = ""
        _password.value = ""
    }

    fun authenticate() {
        val mail = _email.value.trim()
        val pass = _password.value.trim()

        // ── Shared validation ──────────────────────────────────────────────
        if (mail.isEmpty() || pass.isEmpty()) {
            _error.value = "Email and password cannot be empty"
            return
        }

        if (_isSignUp.value) {
            val userVal    = _username.value.trim()
            val displayVal = _displayName.value.trim()
            val confirmVal = _confirmPassword.value.trim()

            if (displayVal.isEmpty()) {
                _error.value = "Display name cannot be empty"
                return
            }
            if (userVal.length < 3) {
                _error.value = "Username must be at least 3 characters"
                return
            }
            if (!userVal.matches(Regex("^[a-zA-Z0-9_]+$"))) {
                _error.value = "Username may only contain letters, numbers, and underscores"
                return
            }
            if (pass.length < 6) {
                _error.value = "Password must be at least 6 characters"
                return
            }
            if (pass != confirmVal) {
                _error.value = "Passwords do not match"
                return
            }
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                if (_isSignUp.value) {
                    val usernameToSave    = _username.value.trim()
                    val displayNameToSave = _displayName.value.trim()

                    // Step 1 — Username uniqueness check
                    val existingCount = supabase.postgrest
                        .from("profiles")
                        .select {
                            filter {
                                eq("username", usernameToSave)
                            }
                        }
                        .decodeList<UsernameCheckDto>()

                    if (existingCount.isNotEmpty()) {
                        _error.value = "Username \"$usernameToSave\" is already taken"
                        _isLoading.value = false
                        return@launch
                    }

                    // Step 2 — Create the auth user in Supabase
                    supabase.auth.signUpWith(Email) {
                        this.email    = mail
                        this.password = pass
                        this.data     = buildJsonObject {
                            put("display_name", displayNameToSave)
                            put("username",     usernameToSave)
                        }
                    }

                    // Step 3 — Sign in immediately to get a live session + userId
                    supabase.auth.signInWith(Email) {
                        this.email    = mail
                        this.password = pass
                    }

                    val userId = supabase.auth.currentUserOrNull()?.id
                    if (userId != null) {
                        // Step 4 — Write the profile row to the `profiles` table
                        val now = Instant.now().toString()
                        supabase.postgrest
                            .from("profiles")
                            .upsert(
                                buildJsonObject {
                                    put("user_id",      userId)
                                    put("username",     usernameToSave)
                                    put("display_name", displayNameToSave)
                                    put("full_name",    displayNameToSave)
                                    put("is_verified",  false)
                                    put("is_private",   false)
                                    put("created_at",   now)
                                    put("updated_at",   now)
                                }
                            )
                        // Navigate to the main app
                        _isAuthenticated.value = true
                    } else {
                        // Supabase project has email confirmation enabled
                        _error.value = "Account created! Please check your email to confirm, then sign in."
                        _isSignUp.value = false
                        clearFields()
                    }

                } else {
                    // Sign-in flow
                    supabase.auth.signInWith(Email) {
                        this.email    = mail
                        this.password = pass
                    }
                    _isAuthenticated.value = true
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Authentication failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                _isAuthenticated.value = false
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    private fun clearFields() {
        _username.value      = ""
        _confirmPassword.value = ""
        _displayName.value   = ""
        _email.value         = ""
        _password.value      = ""
    }
}
