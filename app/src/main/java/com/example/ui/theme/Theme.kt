package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimaryDark,
    secondary = PurpleSecondaryDark,
    tertiary = PurpleTertiaryDark,
    background = HighDensityBackgroundDark,
    surface = HighDensitySurfaceDark,
    surfaceVariant = HighDensitySurfaceVariantDark,
    onPrimary = HighDensityBackgroundDark,
    onSecondary = HighDensityBackgroundDark,
    onBackground = HighDensityBackground,
    onSurface = HighDensityBackground,
    primaryContainer = HighDensityAccentContainerDark,
    secondaryContainer = HighDensityAccentContainerDark,
    outline = HighDensityBorderDark,
    error = HighDensityError
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    tertiary = PurpleTertiary,
    background = HighDensityBackground,
    surface = HighDensitySurface,
    surfaceVariant = HighDensitySurfaceVariant,
    onPrimary = HighDensitySurface,
    onSecondary = HighDensitySurface,
    onBackground = HighDensityBackgroundDark,
    onSurface = HighDensityBackgroundDark,
    primaryContainer = HighDensityTagContainer,
    onPrimaryContainer = HighDensityOnTagContainer,
    secondaryContainer = HighDensityAccentContainer,
    onSecondaryContainer = HighDensityOnAccentContainer,
    outline = HighDensityBorder,
    error = HighDensityError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var currentContext = view.context
            var activity: Activity? = null
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is Activity) {
                    activity = currentContext
                    break
                }
                currentContext = currentContext.baseContext
            }
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val isLight = !darkTheme
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = isLight
                    isAppearanceLightNavigationBars = isLight
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
