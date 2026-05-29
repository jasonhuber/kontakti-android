package com.kontakti.data.repository

import com.kontakti.data.datastore.TokenStore
import com.kontakti.data.model.GoogleLoginRequest
import com.kontakti.data.model.LoginRequest
import com.kontakti.data.model.LoginResponse
import com.kontakti.data.model.RegisterRequest
import com.kontakti.data.model.UserProfile
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {
    suspend fun login(email: String, password: String): LoginResponse {
        val resp = api.login(LoginRequest(email = email, password = password))
        tokenStore.saveToken(resp.token)
        return resp
    }

    suspend fun register(
        name: String,
        username: String,
        email: String,
        password: String
    ): LoginResponse {
        val resp = api.register(
            RegisterRequest(
                name = name,
                username = username,
                email = email,
                password = password,
                passwordConfirmation = password
            )
        )
        tokenStore.saveToken(resp.token)
        return resp
    }

    suspend fun loginWithGoogle(idToken: String): LoginResponse {
        val resp = api.loginWithGoogle(GoogleLoginRequest(idToken = idToken))
        tokenStore.saveToken(resp.token)
        return resp
    }

    suspend fun me(): UserProfile = api.me()

    suspend fun completeOnboarding(): UserProfile = api.completeOnboarding()

    suspend fun logout() {
        runCatching { api.logout() }
        tokenStore.clearToken()
    }
}
