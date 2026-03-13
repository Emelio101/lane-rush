package com.lanerush.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RacingCyan,
    secondary = RacingGold,
    tertiary = RacingRed,
    background = DarkBg0,
    surface = DarkSurface,
    onPrimary = DarkBg0,
    onSecondary = DarkBg0,
    onTertiary = DarkBg0,
    onBackground = OnDarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = OnDarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = RacingCyan,
    secondary = RacingGold,
    tertiary = RacingRed,
    background = LightBg0,
    surface = LightSurface,
    onPrimary = DarkBg0,
    onSecondary = DarkBg0,
    onTertiary = DarkBg0,
    onBackground = OnLightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = OnLightSurfaceVariant
)

@Composable
fun LaneRushTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled by default to keep brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
