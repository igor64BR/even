package com.rateio.data.local.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rateio.domain.model.AuthSession
import com.rateio.domain.model.AuthenticatedUser

/**
 * [TokenStorage] sobre `EncryptedSharedPreferences` (Jetpack Security) — `accessToken` e
 * `refreshToken` nunca em `SharedPreferences` plano nem em log (T12.2, entregável explícito da
 * task). A chave mestra usada para cifrar o arquivo vive no Android Keystore, gerenciada pelo
 * próprio [MasterKey]; este código nunca vê nem guarda a chave.
 */
class EncryptedTokenStorage(context: Context) : TokenStorage {

    private val preferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFERENCES_FILE_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun read(): AuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userName = preferences.getString(KEY_USER_NAME, null) ?: return null
        val userEmail = preferences.getString(KEY_USER_EMAIL, null) ?: return null
        return AuthSession(accessToken, refreshToken, AuthenticatedUser(userName, userEmail))
    }

    override fun save(session: AuthSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_NAME, session.user.name)
            .putString(KEY_USER_EMAIL, session.user.email)
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_FILE_NAME = "rateio_auth_secure_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
    }
}
