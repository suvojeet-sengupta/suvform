package com.suvojeetsengupta.suvform.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** User-selectable theme mode. SYSTEM follows the device setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColorScheme = lightColorScheme(
    primary = Ink, onPrimary = CardWhite,
    primaryContainer = Ink, onPrimaryContainer = CardWhite,
    secondary = Accent, onSecondary = CardWhite,
    secondaryContainer = AccentSoft, onSecondaryContainer = AccentDeep,
    tertiary = Ok, onTertiary = CardWhite,
    tertiaryContainer = OkSoft, onTertiaryContainer = Ok,
    background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = Paper2, onSurfaceVariant = Muted,
    surfaceContainerLowest = CardWhite, surfaceContainerLow = CardWhite,
    surfaceContainer = Paper2, surfaceContainerHigh = Paper2, surfaceContainerHighest = Paper2,
    outline = Line, outlineVariant = Line2,
    error = Accent, onError = CardWhite, errorContainer = AccentSoft, onErrorContainer = AccentDeep,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkInk, onPrimary = DarkPaper,
    primaryContainer = DarkInk, onPrimaryContainer = DarkPaper,
    secondary = DarkAccent, onSecondary = CardWhite,
    secondaryContainer = DarkAccentSoft, onSecondaryContainer = DarkAccentDeep,
    tertiary = DarkOk, onTertiary = DarkPaper,
    tertiaryContainer = DarkOkSoft, onTertiaryContainer = DarkOk,
    background = DarkPaper, onBackground = DarkInk,
    surface = DarkPaper, onSurface = DarkInk,
    surfaceVariant = DarkPaper2, onSurfaceVariant = DarkMuted,
    surfaceContainerLowest = DarkCard, surfaceContainerLow = DarkCard,
    surfaceContainer = DarkPaper2, surfaceContainerHigh = DarkPaper2, surfaceContainerHighest = DarkPaper2,
    outline = DarkLine, outlineVariant = DarkLine2,
    error = DarkAccent, onError = CardWhite, errorContainer = DarkAccentSoft, onErrorContainer = DarkAccentDeep,
)

@Composable
fun SuvFormTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val suvColors = if (darkTheme) DarkSuvColors else LightSuvColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = suvColors.paper.toArgb()
            window.navigationBarColor = suvColors.paper.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalSuvColors provides suvColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
