package com.tally.data.local.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tally.domain.model.AuthSession
import com.tally.domain.model.AuthenticatedUser

/**
 * [TokenStorage] on top of `EncryptedSharedPreferences` (Jetpack Security) — `accessToken` and
 * `refreshToken` never in plain `SharedPreferences` nor in logs (T12.2, an explicit deliverable of
 * the task). The master key used to encrypt the file lives in the Android Keystore, managed by
 * [MasterKey] itself; this code never sees or stores the key.
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
        const val PREFERENCES_FILE_NAME = "tally_auth_secure_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
    }
}
