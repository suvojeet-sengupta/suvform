package com.suvojeetsengupta.suvform.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Editorial light scheme — mapped onto the paper palette. No dynamic color, no dark variant:
// the design is a single, deliberate light identity.
private val EditorialColors = lightColorScheme(
    primary = Ink,
    onPrimary = CardWhite,
    primaryContainer = Ink,
    onPrimaryContainer = CardWhite,
    secondary = Accent,
    onSecondary = CardWhite,
    secondaryContainer = AccentSoft,
    onSecondaryContainer = AccentDeep,
    tertiary = Ok,
    onTertiary = CardWhite,
    tertiaryContainer = OkSoft,
    onTertiaryContainer = Ok,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Paper2,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = CardWhite,
    surfaceContainerLow = CardWhite,
    surfaceContainer = Paper2,
    surfaceContainerHigh = Paper2,
    surfaceContainerHighest = Paper2,
    outline = Line,
    outlineVariant = Line2,
    error = Accent,
    onError = CardWhite,
    errorContainer = AccentSoft,
    onErrorContainer = AccentDeep,
)

@Composable
fun SuvFormTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Paper.toArgb()
            window.navigationBarColor = Paper.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = EditorialColors,
        typography = Typography,
        content = content,
    )
}
