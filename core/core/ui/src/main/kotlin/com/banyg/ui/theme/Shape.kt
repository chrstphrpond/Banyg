package com.banyg.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Banyg Shape System
 * Rounded corners with pill-shaped buttons
 */
val BanygShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Custom shapes for specific components
 */
object BanygCustomShapes {
    val pill = RoundedCornerShape(percent = 50)
    val card = RoundedCornerShape(16.dp)
    val cardLarge = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(12.dp)
    val bottomNav = RoundedCornerShape(24.dp)
    val iconButton = RoundedCornerShape(percent = 50)
}
