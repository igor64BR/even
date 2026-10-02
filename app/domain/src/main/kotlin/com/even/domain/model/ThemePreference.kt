package com.even.domain.model

/**
 * How the app picks between the light and the dark theme.
 *
 * [SYSTEM] is the default (and the only value that isn't a manual choice): the app follows the
 * device's light/dark setting, so it changes along with it — including while the app is open.
 * [LIGHT]/[DARK] are what the sun/moon button in the `TopAppBar` saves, and they then hold on
 * every future launch, regardless of the system setting, until the user taps again.
 */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}
