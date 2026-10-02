package com.even.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * App theme — derives a Material3 `ColorScheme` from the same palette that feeds [LocalEvenColors],
 * so screens can use both the standard Material slots (surfaces, buttons) and the tokens M3 has no
 * slot for (`brandInk`, `owed`/`credit`/`neutral`, the hover pairs). Mirrors the prototype's two
 * themes (`:root` / `:root[data-theme="dark"]` in `prototype/styles.css`); `darkTheme` follows the
 * system by default, exactly like the prototype follows `prefers-color-scheme` until the user picks
 * a theme explicitly.
 *
 * Material You / dynamic colour is deliberately **not** used: the palette is fixed (it is defined in
 * `prototype/styles.css`), so a device's wallpaper can never repaint the brand.
 */
@Composable
fun EvenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val evenColors = if (darkTheme) DarkEvenColors else LightEvenColors
    val colorScheme = if (darkTheme) {
        darkColorScheme().withEvenColors(evenColors, onSemanticFill = evenColors.onAccent)
    } else {
        lightColorScheme().withEvenColors(evenColors, onSemanticFill = Color.White)
    }

    CompositionLocalProvider(LocalEvenColors provides evenColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/**
 * Maps the palette onto the M3 slots, applied on top of the Material baseline so both themes go
 * through exactly the same code — the only difference between light and dark is the [EvenColors]
 * instance.
 *
 * Two slots deserve a note:
 *
 * - `secondary*` has no token of its own. It is mirrored from the indigo brand rather than left at
 *   the Material baseline, because the baseline `secondaryContainer` is lavender and components we
 *   use without explicit colours (`FilterChip`, `InputChip`) render their selected state from it —
 *   which would smuggle purple into a UI that only has indigo and amber. Mirroring stays inside the
 *   indigo family, so it doesn't introduce a third colour either.
 * - `onError` has no token of its own: [onSemanticFill] is the same rule used for text on *any*
 *   filled semantic surface (`--danger`, `--owed`, `--credit`) — white in light, `--on-accent` in
 *   dark.
 */
private fun ColorScheme.withEvenColors(colors: EvenColors, onSemanticFill: Color): ColorScheme = copy(
    primary = colors.brand,
    onPrimary = colors.onBrand,
    primaryContainer = colors.brandBg,
    onPrimaryContainer = colors.brandInk,
    secondary = colors.brand,
    onSecondary = colors.onBrand,
    secondaryContainer = colors.brandBg,
    onSecondaryContainer = colors.brandInk,
    tertiary = colors.accent,
    onTertiary = colors.onAccent,
    tertiaryContainer = colors.accentBg,
    onTertiaryContainer = colors.accentInk,
    background = colors.paper,
    onBackground = colors.ink,
    surface = colors.paper,
    onSurface = colors.ink,
    surfaceVariant = colors.paperAlt,
    onSurfaceVariant = colors.inkSoft,
    surfaceContainerHighest = colors.paperRaised,
    outline = colors.controlBorder,
    outlineVariant = colors.rule,
    error = colors.danger,
    onError = onSemanticFill,
    errorContainer = colors.owedBg,
    onErrorContainer = colors.owed,
    inverseSurface = colors.toastBg,
    inverseOnSurface = colors.onToast,
    scrim = colors.overlayScrim,
    surfaceTint = colors.brand,
)
