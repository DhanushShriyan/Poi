package com.dhanushshriyan.poi.retrolab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Cream = Color(0xFFF2EACE)
val LightCream = Color(0xFFFBF5E3)
val Ink = Color(0xFF24251F)
val MutedInk = Color(0xFF626151)
val Gold = Color(0xFFE6B83D)
val Teal = Color(0xFF2F6974)
val Rust = Color(0xFFB64C29)

@Composable
fun RetroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink, onPrimary = Cream,
            secondary = Teal, onSecondary = Color.White,
            tertiary = Rust, onTertiary = Color.White,
            background = Cream, onBackground = Ink,
            surface = LightCream, onSurface = Ink,
            surfaceVariant = Cream, onSurfaceVariant = MutedInk,
            outline = Ink,
        ),
        typography = MaterialTheme.typography.copy(
            displayMedium = TextStyle(
                fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black,
                fontSize = 38.sp, lineHeight = 37.sp, letterSpacing = (-1.4).sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black,
                fontSize = 29.sp, lineHeight = 30.sp, letterSpacing = (-0.8).sp,
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
                fontSize = 24.sp, lineHeight = 26.sp, letterSpacing = (-0.5).sp,
            ),
            titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 21.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
        ),
        content = content,
    )
}

