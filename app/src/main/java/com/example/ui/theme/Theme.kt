package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MobiDarkNavy,
    onPrimary = Color.White,
    primaryContainer = MobiDarkNavy,
    onPrimaryContainer = Color.White,
    secondary = MobiTeal,
    onSecondary = Color.White,
    secondaryContainer = MobiTealContainer,
    onSecondaryContainer = Color(0xFF006F66),
    tertiary = MobiAmberWarning,
    onTertiary = Color.White,
    tertiaryContainer = MobiAmberContainer,
    onTertiaryContainer = MobiAmberWarning,
    background = MobiSurface,
    onBackground = MobiOnSurface,
    surface = MobiSurface,
    onSurface = MobiOnSurface,
    surfaceVariant = MobiSurfaceVariant,
    onSurfaceVariant = MobiOnSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = MobiSurfaceLow,
    surfaceContainer = MobiSurfaceContainer,
    surfaceContainerHigh = MobiSurfaceHigh,
    surfaceContainerHighest = MobiSurfaceHighest,
    outline = MobiOutline,
    outlineVariant = MobiOutlineVariant,
    error = MobiError,
    onError = Color.White,
    errorContainer = MobiErrorContainer,
    onErrorContainer = MobiOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = MobiDarkNavy,
    primaryContainer = MobiInverseSurface,
    onPrimaryContainer = Color(0xFFDAE2FD),
    secondary = MobiTealFixed,
    onSecondary = Color(0xFF00201D),
    secondaryContainer = Color(0xFF005049),
    onSecondaryContainer = MobiTealFixed,
    tertiary = Color(0xFFFFB77D),
    onTertiary = Color(0xFF2F1500),
    background = Color(0xFF0B1C30),
    onBackground = Color(0xFFEAF1FF),
    surface = Color(0xFF0B1C30),
    onSurface = Color(0xFFEAF1FF),
    surfaceVariant = Color(0xFF213145),
    onSurfaceVariant = Color(0xFFC6C6CD),
    outline = Color(0xFF8E9099),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun MobiWaveTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MobiWaveTheme(darkTheme = darkTheme, content = content)
}
