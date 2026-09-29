package com.tally.data.repository

import android.content.Context
import com.tally.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [ThemeRepository] on top of plain `SharedPreferences` — a theme preference isn't sensitive data
 * (unlike [com.tally.data.local.auth.EncryptedTokenStorage]), it doesn't need encryption.
 *
 * `SharedPreferences` has no native `Flow` API; [preference] is the single source of truth in
 * memory (updated in [setDarkTheme] before persisting), and the disk read only happens once, at
 * construction — there's no other writer in the process that would justify an
 * `OnSharedPreferenceChangeListener`.
 */
class SharedPreferencesThemeRepository(context: Context) : ThemeRepository {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    private val preference = MutableStateFlow(
        if (preferences.contains(KEY_IS_DARK_THEME)) preferences.getBoolean(KEY_IS_DARK_THEME, false) else null,
    )

    override fun getIsDarkThemeFlow(): StateFlow<Boolean?> = preference

    override suspend fun setDarkTheme(isDarkTheme: Boolean) {
        preferences.edit().putBoolean(KEY_IS_DARK_THEME, isDarkTheme).apply()
        preference.value = isDarkTheme
    }

    private companion object {
        const val PREFERENCES_FILE_NAME = "tally_ui_prefs"
        const val KEY_IS_DARK_THEME = "is_dark_theme"
    }
}
