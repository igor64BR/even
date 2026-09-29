package com.tally.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The prototype's "expense notebook" palette (`prototype/styles.css`, `:root` and
 * `:root[data-theme="dark"]`) — paper/ink/amber accent, an exact hex replica of the CSS custom
 * properties. Material3's [androidx.compose.material3.ColorScheme] has no named slots for
 * "to receive"/"to owe"/"settled", so these semantic tones live here and are exposed via
 * [LocalTallyColors] as well as feeding the [androidx.compose.material3.ColorScheme] derived in
 * `Theme.kt`.
 */
data class TallyColors(
    val paper: Color,
    val paperAlt: Color,
    val paperRaised: Color,
    val ink: Color,
    val inkSoft: Color,
    val rule: Color,
    val owed: Color,
    val owedBg: Color,
    val credit: Color,
    val creditBg: Color,
    val neutral: Color,
    val brand: Color,
    val brandInk: Color,
    val onBrand: Color,
    val danger: Color,
)

/** `:root` — light theme (styles.css lines 8-31). */
val LightTallyColors = TallyColors(
    paper = Color(0xFFF5F2EA),
    paperAlt = Color(0xFFECE6D8),
    paperRaised = Color(0xFFFFFFFF),
    ink = Color(0xFF211F1B),
    inkSoft = Color(0xFF6B6558),
    rule = Color(0xFFD9D1BD),
    owed = Color(0xFFA84A34),
    owedBg = Color(0xFFF4E2DC),
    credit = Color(0xFF33604F),
    creditBg = Color(0xFFDFE9DE),
    neutral = Color(0xFF8A8474),
    brand = Color(0xFFB7842A),
    brandInk = Color(0xFF3A2C0F),
    onBrand = Color(0xFFFFFFFF),
    danger = Color(0xFFA3352B),
)

/** `:root[data-theme="dark"]` — dark theme (styles.css lines 33-55). */
val DarkTallyColors = TallyColors(
    paper = Color(0xFF1E1B17),
    paperAlt = Color(0xFF2A251E),
    paperRaised = Color(0xFF332D24),
    ink = Color(0xFFF3EEE2),
    inkSoft = Color(0xFFA89F8D),
    rule = Color(0xFF3D372C),
    owed = Color(0xFFE08361),
    owedBg = Color(0xFF3A241C),
    credit = Color(0xFF7AB89A),
    creditBg = Color(0xFF1F332A),
    neutral = Color(0xFF948C78),
    brand = Color(0xFFCF9C3F),
    brandInk = Color(0xFFD9A441),
    onBrand = Color(0xFF241A08),
    danger = Color(0xFFE2735F),
)

val LocalTallyColors = staticCompositionLocalOf { LightTallyColors }
