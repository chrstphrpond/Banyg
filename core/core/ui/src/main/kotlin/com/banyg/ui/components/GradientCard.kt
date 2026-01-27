package com.banyg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.banyg.ui.theme.BanygGradients
import com.banyg.ui.theme.BanygTheme

/**
 * GradientCard - Card with gradient background
 * Used for asset cards and feature highlights
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush = BanygGradients.DarkOlive,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = BanygTheme.shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(BanygTheme.spacing.cardPadding)
        ) {
            content()
        }
    }
}

/**
 * GradientCard variant with lime accent
 */
@Composable
fun LimeGradientCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    GradientCard(
        modifier = modifier,
        gradient = BanygGradients.LimeAccent,
        elevation = elevation,
        content = content
    )
}

/**
 * GradientCard variant with subtle dark gradient
 */
@Composable
fun SubtleGradientCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    GradientCard(
        modifier = modifier,
        gradient = BanygGradients.SubtleDark,
        elevation = elevation,
        content = content
    )
}

// Previews

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun GradientCardPreview() {
    BanygTheme {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            GradientCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Paypeople Inc · PYPL",
                        style = MaterialTheme.typography.titleMedium,
                        color = BanygTheme.colors.textSecondary
                    )
                    Text(
                        text = "$73.26",
                        style = MaterialTheme.typography.headlineLarge,
                        color = BanygTheme.colors.textPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Portfolio",
                        style = MaterialTheme.typography.bodySmall,
                        color = BanygTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun LimeGradientCardPreview() {
    BanygTheme {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            LimeGradientCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Shophaly Inc · SHOP",
                        style = MaterialTheme.typography.titleMedium,
                        color = BanygTheme.colors.textOnLime
                    )
                    Text(
                        text = "$84.92",
                        style = MaterialTheme.typography.headlineLarge,
                        color = BanygTheme.colors.textOnLime,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Highlighted Asset",
                        style = MaterialTheme.typography.bodySmall,
                        color = BanygTheme.colors.textOnLime.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun SubtleGradientCardPreview() {
    BanygTheme {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            SubtleGradientCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Open Price",
                        style = MaterialTheme.typography.bodySmall,
                        color = BanygTheme.colors.textSecondary
                    )
                    Text(
                        text = "2,139.68",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BanygTheme.colors.textPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
