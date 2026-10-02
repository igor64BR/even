package com.even.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The Even / Tô Quite palette — **cool slate** neutrals, an **indigo** brand (hue ~232: primary
 * buttons, FAB, active nav, focus rings, avatars, selected chips) and an **amber** accent (hue ~36:
 * money that is changing hands — settle-up actions, settlement amounts, the expense money tile).
 * Semantics keep their meaning: `owed` = coral, `credit` = green, `danger` = red.
 *
 * Every value here is an **exact hex replica** of a custom property in `prototype/styles.css`
 * (`:root` for light, `:root[data-theme="dark"]` for dark) — that stylesheet is the source of truth
 * for the palette. No re-derivation, no "close enough" Material defaults, no dynamic/Monet colour
 * (see [EvenTheme]).
 *
 * The prototype-chrome tokens (`--stage`, `--device-border`, `--device-shadow`, `--chrome-*`) are
 * deliberately absent: they paint the fake phone frame and the navigation bar *around* the
 * prototype, which have no Android counterpart. `--radius` is not a colour and lives with the
 * shapes it describes, not here.
 *
 * Material3's [androidx.compose.material3.ColorScheme] has no slot for `owed`/`credit`/`neutral`,
 * for indigo-on-paper (`brandInk`) or for the hover/pressed pairs, so the whole palette is carried
 * here and exposed through [LocalEvenColors]; [EvenTheme] maps the subset that does have an M3 slot.
 */
data class EvenColors(
    // surfaces and ink
    val paper: Color,
    val paperAlt: Color,
    val paperRaised: Color,
    val ink: Color,
    val inkSoft: Color,
    val rule: Color,
    val controlBorder: Color,
    // brand — indigo
    val brand: Color,
    val brandHover: Color,
    val brandInk: Color,
    val brandBg: Color,
    val onBrand: Color,
    val brandShadow: Color,
    // accent — amber
    val accent: Color,
    val accentHover: Color,
    val accentInk: Color,
    val accentBg: Color,
    val onAccent: Color,
    // semantics
    val owed: Color,
    val owedBg: Color,
    val credit: Color,
    val creditBg: Color,
    val neutral: Color,
    val danger: Color,
    // elevation, overlays, feedback
    val shadowRaised: Color,
    val overlayScrim: Color,
    val toastBg: Color,
    val onToast: Color,
    val toastOk: Color,
    // Google sign-in surface
    val googleBg: Color,
    val googleBgHover: Color,
    val googleInk: Color,
)

/** `:root` in `prototype/styles.css` — the light theme. */
val LightEvenColors = EvenColors(
    paper = Color(0xFFF7F8FC),
    paperAlt = Color(0xFFEBEEF6),
    paperRaised = Color(0xFFFFFFFF),
    ink = Color(0xFF151823),
    inkSoft = Color(0xFF565E73),
    rule = Color(0xFFD4DAE7),
    controlBorder = Color(0xFF848EA3),
    brand = Color(0xFF3246C8),
    brandHover = Color(0xFF26339C),
    brandInk = Color(0xFF2B3CAD),
    brandBg = Color(0xFFE7EAFB),
    onBrand = Color(0xFFFFFFFF),
    brandShadow = Color(0x6126339C), // rgba(38, 51, 156, 0.38)
    accent = Color(0xFFC97C06),
    accentHover = Color(0xFFB56D03),
    accentInk = Color(0xFF8A5600),
    accentBg = Color(0xFFFDF0D8),
    onAccent = Color(0xFF171003),
    owed = Color(0xFFC0342A),
    owedBg = Color(0xFFFBE4E0),
    credit = Color(0xFF15714D),
    creditBg = Color(0xFFDDF2E6),
    neutral = Color(0xFF636B7C),
    danger = Color(0xFFC0261C),
    shadowRaised = Color(0x24151823), // rgba(21, 24, 35, 0.14)
    overlayScrim = Color(0xE6F7F8FC), // rgba(247, 248, 252, 0.90)
    toastBg = Color(0xFF1B2231),
    onToast = Color(0xFFF2F5FB),
    toastOk = Color(0xFF5FCF98),
    googleBg = Color(0xFFFFFFFF),
    googleBgHover = Color(0xFFEBEEF6),
    googleInk = Color(0xFF1F2430),
)

/** `:root[data-theme="dark"]` in `prototype/styles.css` — the dark theme, not a tinted copy of light. */
val DarkEvenColors = EvenColors(
    paper = Color(0xFF141924),
    paperAlt = Color(0xFF1D2331),
    paperRaised = Color(0xFF252C3C),
    ink = Color(0xFFE9EDF6),
    inkSoft = Color(0xFF9AA4B8),
    rule = Color(0xFF313A4C),
    controlBorder = Color(0xFF6E798F),
    brand = Color(0xFF7B8CF7),
    brandHover = Color(0xFF93A2FF),
    brandInk = Color(0xFFA3B1FF),
    brandBg = Color(0xFF1E2648),
    onBrand = Color(0xFF0F1535),
    brandShadow = Color(0x8C000000), // rgba(0, 0, 0, 0.55)
    accent = Color(0xFFF0B046),
    accentHover = Color(0xFFF7BD5C),
    accentInk = Color(0xFFF0BC5E),
    accentBg = Color(0xFF3A2B10),
    onAccent = Color(0xFF171003),
    owed = Color(0xFFFF8F76),
    owedBg = Color(0xFF3E211A),
    credit = Color(0xFF5FCF98),
    creditBg = Color(0xFF153024),
    neutral = Color(0xFF8E99AD),
    danger = Color(0xFFFF8A7A),
    shadowRaised = Color(0x80000000), // rgba(0, 0, 0, 0.50)
    overlayScrim = Color(0xE60B0E15), // rgba(11, 14, 21, 0.90)
    toastBg = Color(0xFF2B3447),
    onToast = Color(0xFFEEF1F8),
    toastOk = Color(0xFF5FCF98),
    googleBg = Color(0xFF1B202C),
    googleBgHover = Color(0xFF232A38),
    googleInk = Color(0xFFE9EDF6),
)

val LocalEvenColors = staticCompositionLocalOf { LightEvenColors }
