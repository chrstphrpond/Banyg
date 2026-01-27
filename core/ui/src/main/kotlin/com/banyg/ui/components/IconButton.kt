package com.banyg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.banyg.ui.theme.BanygTheme

/**
 * BanygIconButton - Circular icon button with white background
 */
@Composable
fun BanygIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 48.dp,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = BanygTheme.colors.textPrimary,
            contentColor = BanygTheme.colors.textOnLime,
            disabledContainerColor = BanygTheme.colors.textPrimary.copy(alpha = 0.3f),
            disabledContentColor = BanygTheme.colors.textOnLime.copy(alpha = 0.5f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * BanygIconButton - Lime variant with lime-green background
 */
@Composable
fun LimeIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 48.dp,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = BanygTheme.colors.limeGreen,
            contentColor = BanygTheme.colors.textOnLime,
            disabledContainerColor = BanygTheme.colors.limeGreen.copy(alpha = 0.3f),
            disabledContentColor = BanygTheme.colors.textOnLime.copy(alpha = 0.5f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * BanygIconButton - Dark variant with dark surface background
 */
@Composable
fun DarkIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 48.dp,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = BanygTheme.colors.surfaceDarkElevated,
            contentColor = BanygTheme.colors.textPrimary,
            disabledContainerColor = BanygTheme.colors.surfaceDarkElevated.copy(alpha = 0.5f),
            disabledContentColor = BanygTheme.colors.textPrimary.copy(alpha = 0.5f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * IconButtonWithLabel - Icon button with label below
 */
@Composable
fun IconButtonWithLabel(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    buttonSize: Dp = 48.dp,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BanygIconButton(
            icon = icon,
            onClick = onClick,
            contentDescription = contentDescription,
            size = buttonSize,
            enabled = enabled
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = BanygTheme.colors.textSecondary
        )
    }
}

// Previews

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun IconButtonPreview() {
    BanygTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BanygIconButton(
                icon = Icons.Default.ArrowBack,
                onClick = {}
            )

            BanygIconButton(
                icon = Icons.Default.Notifications,
                onClick = {}
            )

            BanygIconButton(
                icon = Icons.Default.Menu,
                onClick = {},
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun LimeIconButtonPreview() {
    BanygTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LimeIconButton(
                icon = Icons.Default.Add,
                onClick = {}
            )

            LimeIconButton(
                icon = Icons.Default.Add,
                onClick = {},
                size = 56.dp
            )

            LimeIconButton(
                icon = Icons.Default.Add,
                onClick = {},
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun DarkIconButtonPreview() {
    BanygTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DarkIconButton(
                icon = Icons.Default.Menu,
                onClick = {}
            )

            DarkIconButton(
                icon = Icons.Default.Notifications,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun IconButtonWithLabelPreview() {
    BanygTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButtonWithLabel(
                icon = Icons.Default.Add,
                label = "Deposit",
                onClick = {}
            )

            IconButtonWithLabel(
                icon = Icons.Default.Menu,
                label = "Transfer",
                onClick = {}
            )

            IconButtonWithLabel(
                icon = Icons.Default.ArrowBack,
                label = "Withdraw",
                onClick = {}
            )

            IconButtonWithLabel(
                icon = Icons.Default.Menu,
                label = "More",
                onClick = {},
                enabled = false
            )
        }
    }
}
