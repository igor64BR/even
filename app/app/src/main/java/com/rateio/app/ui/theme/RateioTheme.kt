package com.rateio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Tema do app — deriva um `ColorScheme` do Material3 da mesma paleta que alimenta
 * [LocalRateioColors], para telas usarem tanto os slots padrão do Material (superfícies, botões)
 * quanto os tons semânticos específicos do domínio (saldo a receber/a dever). Réplica dos dois
 * temas do protótipo (`data-theme="light"`/`"dark"` em `styles.css`); `darkTheme` segue o tema do
 * sistema por padrão, como o protótipo segue a preferência salva do usuário.
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
