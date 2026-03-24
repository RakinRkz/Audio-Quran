package com.audioquran.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = White,
    primaryContainer = Green100,
    onPrimaryContainer = Green900,
    secondary = Gold,
    onSecondary = Black,
    secondaryContainer = GoldDark,
    onSecondaryContainer = Black,
    tertiary = Green500,
    onTertiary = White,
    background = White,
    onBackground = Gray900,
    surface = Surface,
    onSurface = Gray900,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Gray700,
    outline = Gray400,
    error = androidx.compose.ui.graphics.Color(0xFFB00020),
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = Green500,
    onPrimary = Black,
    primaryContainer = Green800,
    onPrimaryContainer = Green100,
    secondary = Gold,
    onSecondary = Black,
    secondaryContainer = GoldDark,
    onSecondaryContainer = Black,
    tertiary = Green200,
    onTertiary = Black,
    background = SurfaceDark,
    onBackground = White,
    surface = Gray900,
    onSurface = White,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,
    outline = Gray600,
    error = androidx.compose.ui.graphics.Color(0xFFCF6679),
    onError = Black
)

@Composable
fun AudioQuranTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
