package com.banyg.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Banyg Dark Color Scheme
 * Primary theme with lime-green accent
 */
private val DarkColorScheme = darkColorScheme(
    primary = LimeGreen,
    onPrimary = TextOnLime,
    primaryContainer = LimeGreenVariant,
    onPrimaryContainer = TextOnLime,

    secondary = CardDarkElevated,
    onSecondary = TextPrimary,
    secondaryContainer = CardDark,
    onSecondaryContainer = TextSecondary,

    tertiary = SurfaceDarkElevated,
    onTertiary = TextPrimary,

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = TextPrimary,

    outline = TextTertiary,
    outlineVariant = TextSecondary,

    scrim = Overlay
)

/**
 * Local composition for custom theme values
 */
data class BanygColors(
    val limeGreen: androidx.compose.ui.graphics.Color = LimeGreen,
    val limeGreenVariant: androidx.compose.ui.graphics.Color = LimeGreenVariant,
    val backgroundDark: androidx.compose.ui.graphics.Color = BackgroundDark,
    val surfaceDark: androidx.compose.ui.graphics.Color = SurfaceDark,
    val surfaceDarkElevated: androidx.compose.ui.graphics.Color = SurfaceDarkElevated,
    val cardDark: androidx.compose.ui.graphics.Color = CardDark,
    val cardDarkElevated: androidx.compose.ui.graphics.Color = CardDarkElevated,
    val textPrimary: androidx.compose.ui.graphics.Color = TextPrimary,
    val textSecondary: androidx.compose.ui.graphics.Color = TextSecondary,
    val textTertiary: androidx.compose.ui.graphics.Color = TextTertiary,
    val textOnLime: androidx.compose.ui.graphics.Color = TextOnLime,
    val successGreen: androidx.compose.ui.graphics.Color = SuccessGreen,
    val errorRed: androidx.compose.ui.graphics.Color = ErrorRed,
    val warningOrange: androidx.compose.ui.graphics.Color = WarningOrange,
    val chartLime: androidx.compose.ui.graphics.Color = ChartLime,
    val chartWhite: androidx.compose.ui.graphics.Color = ChartWhite,
    val chartGreen: androidx.compose.ui.graphics.Color = ChartGreen,
    val chartRed: androidx.compose.ui.graphics.Color = ChartRed
)

val LocalBanygColors = staticCompositionLocalOf { BanygColors() }
val LocalBanygSpacing = staticCompositionLocalOf { BanygSpacing }
val LocalBanygShapes = staticCompositionLocalOf { BanygCustomShapes }

/**
 * Banyg Theme
 * Dark-first personal finance OS theme
 */
@Composable
fun BanygTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    CompositionLocalProvider(
        LocalBanygColors provides BanygColors(),
        LocalBanygSpacing provides BanygSpacing,
        LocalBanygShapes provides BanygCustomShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BanygTypography,
            shapes = BanygShapes,
            content = content
        )
    }
}

/**
 * Object to access Banyg theme values
 */
object BanygTheme {
    val colors: BanygColors
        @Composable
        get() = LocalBanygColors.current

    val spacing: BanygSpacing
        @Composable
        get() = LocalBanygSpacing.current

    val shapes: BanygCustomShapes
        @Composable
        get() = LocalBanygShapes.current

    val gradients = BanygGradients
}
