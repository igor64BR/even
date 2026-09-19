package com.rateio.app.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.rateio.app.BuildConfig

/**
 * Obtém o ID token do Google via Credential Manager (`androidx.credentials` + `googleid`) — a API
 * atual recomendada pelo Google, substitui o antigo `GoogleSignIn` deprecated (T12.1). Única
 * responsabilidade: transformar "pedir pro usuário escolher uma conta Google" numa String de ID
 * token ou numa falha explicada; trocar esse token pelo JWT do backend é trabalho do
 * [com.rateio.domain.repository.AuthRepository], não deste tipo — este client não sabe que
 * `POST /auth/google` existe.
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
            GoogleIdentityResult.Failure(error.message ?: "Não foi possível conectar à conta Google.")
        }
    }

    /**
     * `setServerClientId` é o client id OAuth **web** do projeto no Google Cloud (o backend valida
     * o ID token contra essa mesma audience, ver `GoogleAuthOptions.ClientId` em `:Rateio.Infrastructure`)
     * — client id ainda placeholder (ver `app/app/build.gradle.kts`), documentado em T12.
     */
    private fun googleIdOption(): GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .build()

    private fun Credential.toGoogleIdentityResult(): GoogleIdentityResult {
        if (this !is CustomCredential || type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdentityResult.Failure("Credencial retornada não é um ID token do Google.")
        }
        return try {
            GoogleIdentityResult.Success(GoogleIdTokenCredential.createFrom(data).idToken)
        } catch (error: GoogleIdTokenParsingException) {
            GoogleIdentityResult.Failure("Não foi possível interpretar a credencial do Google.")
        }
    }
}

/** Resultado de [GoogleIdentityClient.requestGoogleIdToken] — sem exceção cruzando pra UI. */
sealed interface GoogleIdentityResult {
    data class Success(val idToken: String) : GoogleIdentityResult
    data class Failure(val reason: String) : GoogleIdentityResult
}
