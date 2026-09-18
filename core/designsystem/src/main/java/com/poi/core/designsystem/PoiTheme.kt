package com.poi.core.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.poi.core.model.PoiVisualTheme

val PoiIndigo = Color(0xFF5B5FEF)
val PoiIndigoDeep = Color(0xFF3538A6)
val PoiLavender = Color(0xFFE8E8FF)
val PoiCoral = Color(0xFFF16F54)
val PoiAqua = Color(0xFF19B6A5)
val PoiInk = Color(0xFF171A2B)
val PoiMuted = Color(0xFF686B7C)
val PoiCanvas = Color(0xFFF7F8FC)
val PoiSurface = Color(0xFFFFFFFF)

val LocalPoiVisualTheme = staticCompositionLocalOf { PoiVisualTheme.CLASSIC }

private val ClassicLightColors = lightColorScheme(
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

private val ClassicDarkColors = darkColorScheme(
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

private val PulseDarkColors = darkColorScheme(
    primary = Color(0xFFE5F35B),
    onPrimary = Color(0xFF20251F),
    primaryContainer = Color(0xFF3B451F),
    onPrimaryContainer = Color(0xFFF2F8BC),
    secondary = Color(0xFF90CEB2),
    onSecondary = Color(0xFF123829),
    secondaryContainer = Color(0xFF244A3A),
    onSecondaryContainer = Color(0xFFCAF3E0),
    tertiary = Color(0xFFF2987C),
    onTertiary = Color(0xFF4E160B),
    background = Color(0xFF181C19),
    onBackground = Color(0xFFF6F2DF),
    surface = Color(0xFF272E29),
    onSurface = Color(0xFFF6F2DF),
    surfaceVariant = Color(0xFF303932),
    onSurfaceVariant = Color(0xFFBBC5B9),
    outline = Color(0xFF7D8E80),
    outlineVariant = Color(0xFF3F4B42),
    error = Color(0xFFFFB4AB),
)

private val PulseLightColors = lightColorScheme(
    primary = Color(0xFF5B6800),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F35B),
    onPrimaryContainer = Color(0xFF20251F),
    secondary = Color(0xFF276B54),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB5EBCF),
    onSecondaryContainer = Color(0xFF123829),
    tertiary = Color(0xFFA64933),
    onTertiary = Color.White,
    background = Color(0xFFF3F2E8),
    onBackground = Color(0xFF20251F),
    surface = Color(0xFFFFFCF0),
    onSurface = Color(0xFF20251F),
    surfaceVariant = Color(0xFFE8E9DB),
    onSurfaceVariant = Color(0xFF4E584F),
    outline = Color(0xFF747D73),
    outlineVariant = Color(0xFFCED3C9),
)

private val RetroLightColors = lightColorScheme(
    primary = Color(0xFF24251F),
    onPrimary = Color(0xFFF2EACE),
    primaryContainer = Color(0xFFE6B83D),
    onPrimaryContainer = Color(0xFF24251F),
    secondary = Color(0xFF2F6974),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8E6ED),
    onSecondaryContainer = Color(0xFF173B42),
    tertiary = Color(0xFFB64C29),
    onTertiary = Color.White,
    background = Color(0xFFF2EACE),
    onBackground = Color(0xFF24251F),
    surface = Color(0xFFFBF5E3),
    onSurface = Color(0xFF24251F),
    surfaceVariant = Color(0xFFE8DFC2),
    onSurfaceVariant = Color(0xFF626151),
    outline = Color(0xFF24251F),
    outlineVariant = Color(0xFFB9AE8C),
)

private val RetroDarkColors = darkColorScheme(
    primary = Color(0xFFE6B83D),
    onPrimary = Color(0xFF24251F),
    primaryContainer = Color(0xFF594A1F),
    onPrimaryContainer = Color(0xFFFFE7A1),
    secondary = Color(0xFF74C2D0),
    onSecondary = Color(0xFF10343B),
    tertiary = Color(0xFFF28B66),
    onTertiary = Color(0xFF4A1608),
    background = Color(0xFF1E211D),
    onBackground = Color(0xFFF2EACE),
    surface = Color(0xFF292D27),
    onSurface = Color(0xFFF8F0D6),
    surfaceVariant = Color(0xFF353930),
    onSurfaceVariant = Color(0xFFD4C9A8),
    outline = Color(0xFFE6DAB4),
    outlineVariant = Color(0xFF625C49),
)

private val ClassicShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val PulseShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val RetroShapes = Shapes(
    extraSmall = CutCornerShape(topEnd = 5.dp, bottomStart = 5.dp),
    small = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp),
    medium = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
    large = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
    extraLarge = CutCornerShape(topEnd = 20.dp, bottomStart = 20.dp),
)

private val PulseTypography = PoiTypography.copy(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 37.sp,
        lineHeight = 39.sp,
        letterSpacing = (-1.3).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 31.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp,
    ),
)

private val RetroTypography = PoiTypography.copy(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 31.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.9).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 27.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.7).sp,
    ),
)

@Composable
fun PoiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    visualTheme: PoiVisualTheme = PoiVisualTheme.CLASSIC,
    content: @Composable () -> Unit,
) {
    val colors = colorScheme(visualTheme, darkTheme)
    val shapes = when (visualTheme) {
        PoiVisualTheme.CLASSIC -> ClassicShapes
        PoiVisualTheme.PULSE -> PulseShapes
        PoiVisualTheme.RETRO -> RetroShapes
    }
    val typography = when (visualTheme) {
        PoiVisualTheme.CLASSIC -> PoiTypography
        PoiVisualTheme.PULSE -> PulseTypography
        PoiVisualTheme.RETRO -> RetroTypography
    }
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

    CompositionLocalProvider(LocalPoiVisualTheme provides visualTheme) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}

private fun colorScheme(theme: PoiVisualTheme, darkTheme: Boolean): ColorScheme = when (theme) {
    PoiVisualTheme.CLASSIC -> if (darkTheme) ClassicDarkColors else ClassicLightColors
    PoiVisualTheme.PULSE -> if (darkTheme) PulseDarkColors else PulseLightColors
    PoiVisualTheme.RETRO -> if (darkTheme) RetroDarkColors else RetroLightColors
}
