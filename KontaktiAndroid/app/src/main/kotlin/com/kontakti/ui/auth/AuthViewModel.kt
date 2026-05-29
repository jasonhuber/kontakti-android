package com.kontakti.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.datastore.TokenStore
import com.kontakti.data.model.UserProfile
import com.kontakti.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(val user: UserProfile?) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    tokenStore: TokenStore
) : ViewModel() {

    /** True while the initial token check is happening. */
    private val _bootstrapping = MutableStateFlow(true)

    /** Surface auth state derived from the persisted token plus an in-memory user. */
    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user.asStateFlow()

    val state: StateFlow<AuthState> = tokenStore.tokenFlow
        .map { token ->
            if (_bootstrapping.value && token == null) AuthState.Loading
            else if (token.isNullOrBlank()) AuthState.SignedOut
            else AuthState.SignedIn(_user.value)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val profile = runCatching { repo.me() }.getOrNull()
            _user.value = profile
            _bootstrapping.value = false
        }
    }

    fun clearError() { _errorMessage.value = null }

    suspend fun login(email: String, password: String): Boolean {
        _errorMessage.value = null
        return runCatching {
            val resp = repo.login(email, password)
            _user.value = resp.user
            true
        }.getOrElse {
            _errorMessage.value = it.message ?: "Sign-in failed."
            false
        }
    }

    suspend fun register(name: String, username: String, email: String, password: String): Boolean {
        _errorMessage.value = null
        return runCatching {
            val resp = repo.register(name, username, email, password)
            _user.value = resp.user
            true
        }.getOrElse {
            _errorMessage.value = it.message ?: "Sign-up failed."
            false
        }
    }

    suspend fun loginWithGoogle(idToken: String): Boolean {
        _errorMessage.value = null
        return runCatching {
            val resp = repo.loginWithGoogle(idToken)
            _user.value = resp.user
            true
        }.getOrElse {
            _errorMessage.value = it.message ?: "Google sign-in failed."
            false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _user.value = null
        }
    }
}
