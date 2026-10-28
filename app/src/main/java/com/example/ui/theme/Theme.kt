package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AnyWatchColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = NeonViolet.copy(alpha = 0.2f),
    onPrimaryContainer = NeonCyanLight,
    secondary = NeonViolet,
    onSecondary = Color.White,
    secondaryContainer = ObsidianCard,
    onSecondaryContainer = NeonPink,
    tertiary = NeonOrange,
    onTertiary = Color.White,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = NeonViolet.copy(alpha = 0.5f),
    outlineVariant = GlassBorder,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun AnyWatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AnyWatchColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AnyWatchTheme(content = content)
}
