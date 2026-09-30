package com.tally.app.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.tally.app.BuildConfig

/**
 * Obtains the Google ID token via Credential Manager (`androidx.credentials` + `googleid`) — the
 * current API Google recommends, replacing the old deprecated `GoogleSignIn`. Single
 * responsibility: turning "ask the user to pick a Google account" into an ID token String or an
 * explained failure; exchanging that token for the backend's JWT is
 * [com.tally.domain.repository.AuthRepository]'s job, not this type's — this client doesn't know
 * `POST /auth/google` exists.
 */
class GoogleIdentityClient(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun requestGoogleIdToken(): GoogleIdentityResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption())
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            response.credential.toGoogleIdentityResult()
        } catch (error: GetCredentialException) {
            GoogleIdentityResult.Failure(error.message ?: "Couldn't connect to the Google account.")
        }
    }

    /**
     * `setServerClientId` is the project's **web** OAuth client id on Google Cloud (the backend
     * validates the ID token against that same audience, see `GoogleAuthOptions.ClientId` in
     * `:Tally.Infrastructure`) — still a placeholder client id (see `app/app/build.gradle.kts`).
     */
    private fun googleIdOption(): GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .build()

    private fun Credential.toGoogleIdentityResult(): GoogleIdentityResult {
        if (this !is CustomCredential || type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdentityResult.Failure("The returned credential isn't a Google ID token.")
        }
        return try {
            GoogleIdentityResult.Success(GoogleIdTokenCredential.createFrom(data).idToken)
        } catch (error: GoogleIdTokenParsingException) {
            GoogleIdentityResult.Failure("Couldn't parse the Google credential.")
        }
    }
}

/** Result of [GoogleIdentityClient.requestGoogleIdToken] — no exception crossing into the UI. */
sealed interface GoogleIdentityResult {
    data class Success(val idToken: String) : GoogleIdentityResult
    data class Failure(val reason: String) : GoogleIdentityResult
}
