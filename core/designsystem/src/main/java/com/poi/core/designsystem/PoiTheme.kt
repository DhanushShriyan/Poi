package com.poi.core.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PoiGreen = Color(0xFF157A62)
val PoiGreenDark = Color(0xFF0D5544)
val PoiMint = Color(0xFFDFF3EA)
val PoiCoral = Color(0xFFF16F54)
val PoiGold = Color(0xFFF4B942)
val PoiInk = Color(0xFF17221C)
val PoiMuted = Color(0xFF637068)
val PoiCanvas = Color(0xFFF7F5EF)
val PoiSurface = Color(0xFFFFFDF8)

private val LightColors = lightColorScheme(
    primary = PoiGreen,
    onPrimary = Color.White,
    primaryContainer = PoiMint,
    onPrimaryContainer = PoiGreenDark,
    secondary = PoiCoral,
    onSecondary = Color.White,
    tertiary = PoiGold,
    background = PoiCanvas,
    onBackground = PoiInk,
    surface = PoiSurface,
    onSurface = PoiInk,
    surfaceVariant = Color(0xFFECEDE7),
    onSurfaceVariant = PoiMuted,
    outline = Color(0xFFB8BEB8),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF71D6B6),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF07513F),
    onPrimaryContainer = Color(0xFF99F4D3),
    secondary = Color(0xFFFFB5A3),
    onSecondary = Color(0xFF5A190C),
    background = Color(0xFF111712),
    onBackground = Color(0xFFE1E7E0),
    surface = Color(0xFF171D18),
    onSurface = Color(0xFFE1E7E0),
    surfaceVariant = Color(0xFF303832),
    onSurfaceVariant = Color(0xFFBEC8C0),
)

@Composable
fun PoiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.statusBarColor = colors.background.toArgb()
                window.navigationBarColor = colors.surface.toArgb()
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = PoiTypography,
        content = content,
    )
}

