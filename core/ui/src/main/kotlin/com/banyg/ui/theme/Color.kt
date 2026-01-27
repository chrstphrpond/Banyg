package com.banyg.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Banyg Dark Theme Color Palette
 * Based on lime-green accent with dark backgrounds
 */

// Primary Colors
val LimeGreen = Color(0xFFCDFF00)
val LimeGreenVariant = Color(0xFFB8E600)

// Background Colors
val BackgroundDark = Color(0xFF0F0F0F)
val SurfaceDark = Color(0xFF1A1A1A)
val SurfaceDarkElevated = Color(0xFF252525)

// Card Surface Colors
val CardDark = Color(0xFF1F1F1F)
val CardDarkElevated = Color(0xFF2A2A2A)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0B0)
val TextTertiary = Color(0xFF808080)
val TextOnLime = Color(0xFF000000)

// Status Colors
val SuccessGreen = Color(0xFF00C853)
val ErrorRed = Color(0xFFFF5252)
val WarningOrange = Color(0xFFFFAB00)

// Chart Colors
val ChartLime = Color(0xFFCDFF00)
val ChartWhite = Color(0xFFFFFFFF)
val ChartGreen = Color(0xFF4CAF50)
val ChartRed = Color(0xFFFF5252)

// Overlay Colors
val Overlay = Color(0x80000000)
val OverlayLight = Color(0x40000000)

/**
 * Gradient Helpers
 */
object BanygGradients {

    /**
     * Dark olive gradient for cards
     * Used on asset cards and feature highlights
     */
    val DarkOlive = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2A3A1A),
            Color(0xFF1A2510)
        )
    )

    /**
     * Dark olive horizontal gradient
     */
    val DarkOliveHorizontal = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF2A3A1A),
            Color(0xFF1A2510)
        )
    )

    /**
     * Lime gradient for highlighted elements
     */
    val LimeAccent = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDFF33),
            Color(0xFFCDFF00)
        )
    )

    /**
     * Subtle dark gradient for background cards
     */
    val SubtleDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF252525),
            Color(0xFF1A1A1A)
        )
    )

    /**
     * Dark to darker gradient for depth
     */
    val DarkDepth = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1F1F1F),
            Color(0xFF0F0F0F)
        )
    )

    /**
     * Radial gradient for glowing effects
     */
    fun radialGlow(
        centerColor: Color = LimeGreen.copy(alpha = 0.3f),
        edgeColor: Color = Color.Transparent
    ) = Brush.radialGradient(
        colors = listOf(centerColor, edgeColor)
    )

    /**
     * Custom vertical gradient
     */
    fun customVertical(
        colors: List<Color>
    ) = Brush.verticalGradient(colors = colors)

    /**
     * Custom horizontal gradient
     */
    fun customHorizontal(
        colors: List<Color>
    ) = Brush.horizontalGradient(colors = colors)
}
