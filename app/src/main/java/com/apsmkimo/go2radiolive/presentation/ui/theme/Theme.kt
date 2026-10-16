package com.apsmkimo.go2radiolive.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = Color(0xFF144B4A),
    onPrimaryContainer = Color(0xFFD4F3F0),
    secondary = OrangeAccent,
    onSecondary = Color.White,
    background = NightBackground,
    surface = NightSurface,
    onBackground = NightOnSurface,
    onSurface = NightOnSurface,
    outline = NightOutline,
    surfaceVariant = Color(0xFF243038),
    onSurfaceVariant = Color(0xFFC5D0D6),
)

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFF003735),
    secondary = OrangeAccent,
    onSecondary = Color.White,
    background = Color(0xFFF4F7F8),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF152026),
    onSurface = Color(0xFF152026),
    outline = Color(0xFF6F7C83),
    surfaceVariant = Color(0xFFE4EEEF),
    onSurfaceVariant = Color(0xFF3D4A52),
)

@Composable
fun Go2RadioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
