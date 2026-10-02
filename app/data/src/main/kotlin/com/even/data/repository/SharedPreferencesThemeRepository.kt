package com.even.data.repository

import android.content.Context
import com.even.domain.model.ThemePreference
import com.even.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [ThemeRepository] on top of plain `SharedPreferences` — a theme preference isn't sensitive data
 * (unlike [com.even.data.local.auth.EncryptedTokenStorage]), it doesn't need encryption.
 *
 * `SharedPreferences` has no native `Flow` API; [preference] is the single source of truth in
 * memory (updated in [setThemePreference] before persisting), and the disk read only happens once,
 * at construction — there's no other writer in the process that would justify an
 * `OnSharedPreferenceChangeListener`.
 *
 * The value is stored by [ThemePreference.name] (not a boolean) so that "follow the system" is a
 * first-class, persistable choice instead of "no key saved yet": an unreadable or absent value
 * falls back to [ThemePreference.SYSTEM], the app's default.
 */
class SharedPreferencesThemeRepository(context: Context) : ThemeRepository {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    private val preference = MutableStateFlow(readStoredPreference())

    override fun getThemePreferenceFlow(): StateFlow<ThemePreference> = preference

    override suspend fun setThemePreference(preference: ThemePreference) {
        preferences.edit().putString(KEY_THEME_PREFERENCE, preference.name).apply()
        this.preference.value = preference
    }

    private fun readStoredPreference(): ThemePreference {
        val stored = preferences.getString(KEY_THEME_PREFERENCE, null) ?: return ThemePreference.SYSTEM
        return ThemePreference.entries.firstOrNull { it.name == stored } ?: ThemePreference.SYSTEM
    }

    private companion object {
        const val PREFERENCES_FILE_NAME = "even_ui_prefs"
        const val KEY_THEME_PREFERENCE = "theme_preference"
    }
}
