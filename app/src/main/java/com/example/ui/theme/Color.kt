package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian & Cyberpunk Surfaces (Rich Indigo-Violet Undertones)
val ObsidianBlack = Color(0xFF090812)
val ObsidianSurface = Color(0xFF100E20)
val ObsidianCard = Color(0xFF16132C)
val ObsidianGlass = Color(0xEE0E0C22)
val GlassBorder = Color(0x44A855F7)

// Neon Accents
val NeonViolet = Color(0xFFA855F7)
val NeonPurple = Color(0xFF8B5CF6)
val NeonCyan = Color(0xFF00F5FF)
val NeonCyanLight = Color(0xFF22D3EE)
val NeonPink = Color(0xFFFF007F)
val NeonMagenta = Color(0xFFEC4899)
val NeonOrange = Color(0xFFFF6B2B)
val AlertRed = Color(0xFFEF4444)
val UnblockPink = Color(0xFFFF2A6D)
val TelegramBlue = Color(0xFF229ED9)

val TextPrimary = Color(0xFFF8F9FA)
val TextSecondary = Color(0xFFA1A1C2)
val TextMuted = Color(0xFF6B7280)

// Vibrant Cyberpunk & Neon Gradients
val PurpleToPinkBrush = Brush.horizontalGradient(
    listOf(Color(0xFF9D4EDD), Color(0xFFFF007F))
)

val CyanToBlueBrush = Brush.horizontalGradient(
    listOf(Color(0xFF00F5D4), Color(0xFF00BBF9))
)

val NeonSunsetBrush = Brush.horizontalGradient(
    listOf(Color(0xFFFF007F), Color(0xFFFF9E00))
)

val AdminGlowBrush = Brush.horizontalGradient(
    listOf(Color(0xFFFF0055), Color(0xFFFF5252))
)

val CyberpunkHeaderGlow = Brush.horizontalGradient(
    listOf(
        Color(0x0000F5D4),
        Color(0xFF00F5D4),
        Color(0xFF9D4EDD),
        Color(0xFFFF007F),
        Color(0x00FF007F)
    )
)
