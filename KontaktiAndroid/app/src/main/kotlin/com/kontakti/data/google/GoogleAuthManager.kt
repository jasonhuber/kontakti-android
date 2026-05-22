package com.kontakti.data.google

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
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
            .requestServerAuthCode(
                /* serverClientId — set your OAuth 2.0 web client ID here */
                "YOUR_WEB_CLIENT_ID",
                /* forceCodeForRefreshToken */ true
            )
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** Returns the sign-in Intent to launch via ActivityResultLauncher. */
    fun getSignInIntent() = buildClient().signInIntent

    /**
     * Called with the result Intent from the sign-in flow.
     * Returns the access token on success, null on failure/cancellation.
     */
    suspend fun handleSignInResult(data: android.content.Intent?): String? =
        withContext(Dispatchers.IO) {
            try {
                val account: GoogleSignInAccount =
                    GoogleSignIn.getSignedInAccountFromIntent(data).await()
                account.serverAuthCode // exchange for access token server-side, or use directly
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
