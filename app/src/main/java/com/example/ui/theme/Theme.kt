package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = AccentWhite,
    onPrimary = DarkBg,
    primaryContainer = DarkSurfaceHighlight,
    onPrimaryContainer = TextPrimary,
    secondary = AccentSlate,
    onSecondary = DarkBg,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentMuted,
    onTertiary = TextPrimary,
    tertiaryContainer = DarkSurfaceElevated,
    onTertiaryContainer = TextPrimary,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MonochromeDarkColorScheme,
        typography = Typography,
        content = content
    )
}
