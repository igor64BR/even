package com.tally.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * App theme — derives a Material3 `ColorScheme` from the same palette that feeds
 * [LocalTallyColors], so screens can use both the standard Material slots (surfaces, buttons)
 * and the domain-specific semantic tones (balance owed to you/you owe). Mirrors the prototype's
 * two themes (`data-theme="light"`/`"dark"` in `styles.css`); `darkTheme` follows the system
 * theme by default, just like the prototype follows the user's saved preference.
 */
@Composable
fun TallyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tallyColors = if (darkTheme) DarkTallyColors else LightTallyColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = tallyColors.brandInk,
            onPrimary = tallyColors.onBrand,
            secondary = tallyColors.brand,
            onSecondary = tallyColors.onBrand,
            background = tallyColors.paper,
            onBackground = tallyColors.ink,
            surface = tallyColors.paperRaised,
            onSurface = tallyColors.ink,
            surfaceVariant = tallyColors.paperAlt,
            onSurfaceVariant = tallyColors.inkSoft,
            outline = tallyColors.rule,
            outlineVariant = tallyColors.rule,
            error = tallyColors.danger,
            onError = tallyColors.onBrand,
        )
    } else {
        lightColorScheme(
            primary = tallyColors.brandInk,
            onPrimary = tallyColors.onBrand,
            secondary = tallyColors.brand,
            onSecondary = tallyColors.onBrand,
            background = tallyColors.paper,
            onBackground = tallyColors.ink,
            surface = tallyColors.paperRaised,
            onSurface = tallyColors.ink,
            surfaceVariant = tallyColors.paperAlt,
            onSurfaceVariant = tallyColors.inkSoft,
            outline = tallyColors.rule,
            outlineVariant = tallyColors.rule,
            error = tallyColors.danger,
            onError = tallyColors.onBrand,
        )
    }

    CompositionLocalProvider(LocalTallyColors provides tallyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
