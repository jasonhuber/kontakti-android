package com.kontakti.data.google

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.kontakti.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val GMAIL_SCOPE = Scope("https://www.googleapis.com/auth/gmail.readonly")
    private val CONTACTS_SCOPE = Scope("https://www.googleapis.com/auth/contacts.readonly")

    private fun buildClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(GMAIL_SCOPE, CONTACTS_SCOPE)
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** Returns the sign-in Intent to launch via ActivityResultLauncher. */
    fun getSignInIntent() = buildClient().signInIntent

    /**
     * Resolves the sign-in result, then exchanges the bound Google account for
     * a real OAuth 2.0 access token via [GoogleAuthUtil]. The returned token is
     * usable directly against the People / Gmail APIs.
     *
     * Returns null on failure/cancellation. The exchange happens off the main
     * thread.
     */
    suspend fun handleSignInResult(data: android.content.Intent?): String? =
        withContext(Dispatchers.IO) {
            try {
                val signed: GoogleSignInAccount =
                    GoogleSignIn.getSignedInAccountFromIntent(data).await()
                val account = signed.account ?: return@withContext null
                val scopeString =
                    "oauth2:${CONTACTS_SCOPE.scopeUri} ${GMAIL_SCOPE.scopeUri}"
                GoogleAuthUtil.getToken(context, account, scopeString)
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Variant configured for backend identity verification: requests an
     * `id_token` minted by the Google OAuth web client whose ID is supplied at
     * build time via `kontakti.google.web_client_id` in `local.properties`.
     * The backend `/auth/google` route verifies that token; we do not need
     * email/contacts scopes for login itself.
     */
    private fun buildLoginClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun getLoginSignInIntent() = buildLoginClient().signInIntent

    /** Returns the Google id_token, or null on failure/cancellation. */
    suspend fun handleLoginSignInResult(data: android.content.Intent?): String? =
        withContext(Dispatchers.IO) {
            try {
                val account: GoogleSignInAccount =
                    GoogleSignIn.getSignedInAccountFromIntent(data).await()
                account.idToken
            } catch (_: Exception) {
                null
            }
        }

    /** Returns a valid signed-in account if the user is already authenticated. */
    fun getLastSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try { buildClient().signOut().await() } catch (_: Exception) { }
    }
}
