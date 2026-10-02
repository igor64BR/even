package com.even.domain.repository

import com.even.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/**
 * The theme preference: follow the system (the default) or the light/dark theme manually chosen
 * by the user (the sun/moon button in the `TopAppBar` on every screen — the same global component
 * as `prototype/app.js`'s `initThemeToggle`).
 *
 * `:domain` declares it, `:data` implements it on top of `SharedPreferences` (Dependency
 * Inversion) — same pattern as [NotificationRepository]/[SettlementRepository]: no Android type
 * leaks into this interface.
 */
interface ThemeRepository {

    /**
     * [ThemePreference.SYSTEM] while the user has never touched the button (nothing saved yet) —
     * the consumer resolves it against the device setting (`isSystemInDarkTheme()`). After the
     * first tap, always an explicit [ThemePreference.LIGHT]/[ThemePreference.DARK], which then
     * holds for every future app launch until the user taps again.
     */
    fun getThemePreferenceFlow(): Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)
}
