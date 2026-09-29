package com.rateio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * App theme — derives a Material3 `ColorScheme` from the same palette that feeds
 * [LocalRateioColors], so screens can use both the standard Material slots (surfaces, buttons)
 * and the domain-specific semantic tones (balance owed to you/you owe). Mirrors the prototype's
 * two themes (`data-theme="light"`/`"dark"` in `styles.css`); `darkTheme` follows the system
 * theme by default, just like the prototype follows the user's saved preference.
 */
@Composable
fun RateioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val rateioColors = if (darkTheme) DarkRateioColors else LightRateioColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = rateioColors.brandInk,
            onPrimary = rateioColors.onBrand,
            secondary = rateioColors.brand,
            onSecondary = rateioColors.onBrand,
            background = rateioColors.paper,
            onBackground = rateioColors.ink,
            surface = rateioColors.paperRaised,
            onSurface = rateioColors.ink,
            surfaceVariant = rateioColors.paperAlt,
            onSurfaceVariant = rateioColors.inkSoft,
            outline = rateioColors.rule,
            outlineVariant = rateioColors.rule,
            error = rateioColors.danger,
            onError = rateioColors.onBrand,
        )
    } else {
        lightColorScheme(
            primary = rateioColors.brandInk,
            onPrimary = rateioColors.onBrand,
            secondary = rateioColors.brand,
            onSecondary = rateioColors.onBrand,
            background = rateioColors.paper,
            onBackground = rateioColors.ink,
            surface = rateioColors.paperRaised,
            onSurface = rateioColors.ink,
            surfaceVariant = rateioColors.paperAlt,
            onSurfaceVariant = rateioColors.inkSoft,
            outline = rateioColors.rule,
            outlineVariant = rateioColors.rule,
            error = rateioColors.danger,
            onError = rateioColors.onBrand,
        )
    }

    CompositionLocalProvider(LocalRateioColors provides rateioColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
