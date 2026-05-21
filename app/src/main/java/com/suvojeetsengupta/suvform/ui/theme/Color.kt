package com.suvojeetsengupta.suvform.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---- Light "paper" swatches ----
val Paper = Color(0xFFF4F1EA)
val Paper2 = Color(0xFFEBE7DD)
val Ink = Color(0xFF0F0F10)
val Muted = Color(0xFF6E6B62)
val Muted2 = Color(0xFFA8A49A)
val Line = Color(0xFFDDD6C7)
val Line2 = Color(0xFFE8E2D2)
val CardWhite = Color(0xFFFFFFFF)

val Accent = Color(0xFFE94221)        // sharp editorial red-orange
val AccentSoft = Color(0xFFFBE3DC)
val AccentDeep = Color(0xFF7A1C0A)
val Ok = Color(0xFF1F7A4D)
val OkSoft = Color(0xFFDAEEDE)
val Warn = Color(0xFFB26B00)
val WarnSoft = Color(0xFFF2E3C0)

// ---- Dark "ink paper" swatches (warm, editorial) ----
val DarkPaper = Color(0xFF14120E)
val DarkPaper2 = Color(0xFF211E18)
val DarkCard = Color(0xFF1C1A15)
val DarkInk = Color(0xFFF1EDE4)
val DarkMuted = Color(0xFF9C968A)
val DarkMuted2 = Color(0xFF6E6A60)
val DarkLine = Color(0xFF322E26)
val DarkLine2 = Color(0xFF2A2720)
val DarkAccent = Color(0xFFFF5A3C)
val DarkAccentSoft = Color(0xFF3A201A)
val DarkAccentDeep = Color(0xFFFFB6A6)
val DarkOk = Color(0xFF5FB98A)
val DarkOkSoft = Color(0xFF1E3A2C)
val DarkWarn = Color(0xFFD9A24E)
val DarkWarnSoft = Color(0xFF3A2E18)

/**
 * Semantic editorial color roles. Resolved per-theme via [LocalSuvColors].
 * `feature`/`onFeature` are the high-contrast hero/featured surfaces — dark-on-light
 * in light mode and light-on-dark in dark mode, so they always pop against the page.
 */
@Immutable
data class SuvColors(
    val paper: Color,
    val paper2: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val muted2: Color,
    val line: Color,
    val line2: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentDeep: Color,
    val ok: Color,
    val okSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val feature: Color,
    val onFeature: Color,
    val isDark: Boolean,
)

val LightSuvColors = SuvColors(
    paper = Paper, paper2 = Paper2, card = CardWhite, ink = Ink,
    muted = Muted, muted2 = Muted2, line = Line, line2 = Line2,
    accent = Accent, onAccent = Color.White, accentSoft = AccentSoft, accentDeep = AccentDeep,
    ok = Ok, okSoft = OkSoft, warn = Warn, warnSoft = WarnSoft,
    feature = Ink, onFeature = Paper, isDark = false,
)

val DarkSuvColors = SuvColors(
    paper = DarkPaper, paper2 = DarkPaper2, card = DarkCard, ink = DarkInk,
    muted = DarkMuted, muted2 = DarkMuted2, line = DarkLine, line2 = DarkLine2,
    accent = DarkAccent, onAccent = Color.White, accentSoft = DarkAccentSoft, accentDeep = DarkAccentDeep,
    ok = DarkOk, okSoft = DarkOkSoft, warn = DarkWarn, warnSoft = DarkWarnSoft,
    feature = Color(0xFFEDE9E0), onFeature = Color(0xFF15130F), isDark = true,
)

val LocalSuvColors = staticCompositionLocalOf { LightSuvColors }

/** Access editorial colors: `SuvTheme.colors.ink`. */
object SuvTheme {
    val colors: SuvColors
        @Composable @ReadOnlyComposable get() = LocalSuvColors.current
}
