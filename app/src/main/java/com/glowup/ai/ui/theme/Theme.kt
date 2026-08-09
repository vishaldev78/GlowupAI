package com.glowup.ai.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = Violet100,
    onPrimaryContainer = Primary,
    secondary = Secondary,
    onSecondary = White,
    tertiary = Accent,
    background = White,
    surface = White,
    onBackground = Gray900,
    onSurface = Gray900,
    surfaceVariant = Gray50,
    onSurfaceVariant = Gray600,
    outline = Gray200,
    error = Error
)

@Composable
fun GlowUpAITheme(
    darkTheme: Boolean = false, // Force Light Theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = White.toArgb()
            window.navigationBarColor = White.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
