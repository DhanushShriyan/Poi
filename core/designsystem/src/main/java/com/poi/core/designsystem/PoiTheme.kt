package com.poi.core.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val PoiIndigo = Color(0xFF5B5FEF)
val PoiIndigoDeep = Color(0xFF3538A6)
val PoiLavender = Color(0xFFE8E8FF)
val PoiCoral = Color(0xFFF16F54)
val PoiAqua = Color(0xFF19B6A5)
val PoiInk = Color(0xFF171A2B)
val PoiMuted = Color(0xFF686B7C)
val PoiCanvas = Color(0xFFF7F8FC)
val PoiSurface = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = PoiIndigo,
    onPrimary = Color.White,
    primaryContainer = PoiLavender,
    onPrimaryContainer = PoiIndigoDeep,
    secondary = PoiCoral,
    onSecondary = Color.White,
    tertiary = PoiAqua,
    background = PoiCanvas,
    onBackground = PoiInk,
    surface = PoiSurface,
    onSurface = PoiInk,
    surfaceVariant = Color(0xFFF0F1F7),
    onSurfaceVariant = PoiMuted,
    outline = Color(0xFFB8BBCB),
    outlineVariant = Color(0xFFE1E3EC),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBFC2FF),
    onPrimary = Color(0xFF252875),
    primaryContainer = Color(0xFF3D408E),
    onPrimaryContainer = Color(0xFFE1E2FF),
    secondary = Color(0xFFFFB5A3),
    onSecondary = Color(0xFF5A190C),
    tertiary = Color(0xFF63D8C8),
    background = Color(0xFF0E1020),
    onBackground = Color(0xFFF0F0F8),
    surface = Color(0xFF17192B),
    onSurface = Color(0xFFF0F0F8),
    surfaceVariant = Color(0xFF22253A),
    onSurfaceVariant = Color(0xFFC5C6D2),
    outline = Color(0xFF8E90A2),
    outlineVariant = Color(0xFF34364A),
)

private val PoiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
        shapes = PoiShapes,
        content = content,
    )
}
