package com.rateio.data.repository

import android.content.Context
import com.rateio.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [ThemeRepository] sobre `SharedPreferences` plano — preferência de tema não é dado sensível
 * (diferente de [com.rateio.data.local.auth.EncryptedTokenStorage]), não precisa de cifra.
 *
 * `SharedPreferences` não tem API de `Flow` nativa; [preference] é a única fonte de verdade em
 * memória (atualizada em [setDarkTheme] antes de persistir), e a leitura em disco só acontece uma
 * vez, na construção — não há outro escritor no processo que justifique um
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
        const val PREFERENCES_FILE_NAME = "rateio_ui_prefs"
        const val KEY_IS_DARK_THEME = "is_dark_theme"
    }
}
