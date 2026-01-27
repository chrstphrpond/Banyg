package com.banyg.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Banyg Spacing System
 * 8dp base grid with common spacing values
 */
object BanygSpacing {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val regular: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 32.dp
    val huge: Dp = 48.dp
    val massive: Dp = 64.dp

    // Semantic spacing
    val cardPadding: Dp = regular
    val screenPadding: Dp = regular
    val sectionSpacing: Dp = large
    val itemSpacing: Dp = small
    val contentSpacing: Dp = medium
}
