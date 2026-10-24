package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF00364F),
    primaryContainer = Color(0xFF004D6D),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = IndigoAccent,
    onSecondary = Color(0xFF1E1E60),
    secondaryContainer = Color(0xFF333380),
    onSecondaryContainer = Color(0xFFDFE0FF),
    tertiary = EmeraldAccent,
    onTertiary = Color(0xFF003822),
    tertiaryContainer = Color(0xFF005234),
    onTertiaryContainer = Color(0xFF86F8B6),
    background = IdeBackground,
    onBackground = TextPrimary,
    surface = IdeSurface,
    onSurface = TextPrimary,
    surfaceVariant = IdeSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = IdeBorder,
    error = RoseAccent,
    onError = Color(0xFF600010)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // PocketCode is a dedicated developer IDE with an optimized dark coding aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
