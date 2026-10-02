package com.even.app.ui.theme

import com.even.domain.model.ThemePreference

/**
 * Turns the saved [ThemePreference] into the single boolean [EvenTheme] consumes.
 *
 * [ThemePreference.SYSTEM] — the default, while the user has never touched the sun/moon button —
 * defers to [systemInDarkTheme] (`isSystemInDarkTheme()` at the call site, which recomposes when
 * the device switches between light and dark); an explicit [ThemePreference.LIGHT]/
 * [ThemePreference.DARK] ignores the system setting entirely.
 *
 * `internal` (instead of `private` in `MainActivity`) just to be directly testable — the same
 * convention as [com.even.app.extractInviteCode].
 */
internal fun ThemePreference.resolveIsDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemePreference.SYSTEM -> systemInDarkTheme
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}
