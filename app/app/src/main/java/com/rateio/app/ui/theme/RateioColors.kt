package com.rateio.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta "caderno de contas" do protótipo (`prototype/styles.css`, `:root` e
 * `:root[data-theme="dark"]`) — papel/tinta/acento âmbar, réplica em hex exata das custom
 * properties CSS. Material3 [androidx.compose.material3.ColorScheme] não tem slots nomeados para
 * "a receber"/"a dever"/"quitado", então esses tons semânticos vivem aqui e são expostos via
 * [LocalRateioColors] além de alimentar o [androidx.compose.material3.ColorScheme] derivado em
 * `Theme.kt`.
 */
data class RateioColors(
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

/** `:root` — tema claro (styles.css linhas 8-31). */
val LightRateioColors = RateioColors(
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

/** `:root[data-theme="dark"]` — tema escuro (styles.css linhas 33-55). */
val DarkRateioColors = RateioColors(
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

val LocalRateioColors = staticCompositionLocalOf { LightRateioColors }
