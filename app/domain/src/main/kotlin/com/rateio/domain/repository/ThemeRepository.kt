package com.rateio.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * The light/dark theme preference manually chosen by the user (the sun/moon button in the
 * `TopAppBar` on every screen — the same global component as `prototype/app.js`'s
 * `initThemeToggle`).
 *
 * `:domain` declares it, `:data` implements it on top of `SharedPreferences` (Dependency
 * Inversion) — same pattern as [NotificationRepository]/[SettlementRepository]: no Android type
 * leaks into this interface.
 */
interface ThemeRepository {

    /**
     * `null` while the user has never touched the button (no preference saved yet) — the consumer
     * decides the default in that case (the app uses the system theme, `isSystemInDarkTheme()`,
     * the same idea as the prototype opening in the saved theme or "light" if nothing was saved).
     * After the first tap, always an explicit value, which then holds for every future app launch
     * until the user taps again.
     */
    fun getIsDarkThemeFlow(): Flow<Boolean?>

    suspend fun setDarkTheme(isDarkTheme: Boolean)
}
