package com.banyg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.banyg.ui.theme.BanygTheme

/**
 * PillButton - Primary button with pill shape
 * Filled variant with lime-green background
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = BanygTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = BanygTheme.colors.limeGreen,
            contentColor = BanygTheme.colors.textOnLime,
            disabledContainerColor = BanygTheme.colors.limeGreen.copy(alpha = 0.3f),
            disabledContentColor = BanygTheme.colors.textOnLime.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(
            horizontal = BanygTheme.spacing.large,
            vertical = BanygTheme.spacing.medium
        )
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(BanygTheme.spacing.small))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )

        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(BanygTheme.spacing.small))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null
            )
        }
    }
}

/**
 * PillButton - Outlined variant
 * Transparent background with white border
 */
@Composable
fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = BanygTheme.shapes.pill,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BanygTheme.colors.textPrimary,
            disabledContentColor = BanygTheme.colors.textPrimary.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) BanygTheme.colors.textPrimary else BanygTheme.colors.textPrimary.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(
            horizontal = BanygTheme.spacing.large,
            vertical = BanygTheme.spacing.medium
        )
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(BanygTheme.spacing.small))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )

        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(BanygTheme.spacing.small))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null
            )
        }
    }
}

/**
 * PillButton - Secondary variant
 * Dark background variant
 */
@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = BanygTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = BanygTheme.colors.surfaceDarkElevated,
            contentColor = BanygTheme.colors.textPrimary,
            disabledContainerColor = BanygTheme.colors.surfaceDarkElevated.copy(alpha = 0.5f),
            disabledContentColor = BanygTheme.colors.textPrimary.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(
            horizontal = BanygTheme.spacing.large,
            vertical = BanygTheme.spacing.medium
        )
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(BanygTheme.spacing.small))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )

        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(BanygTheme.spacing.small))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null
            )
        }
    }
}

// Previews

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun PillButtonPreview() {
    BanygTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillButton(
                text = "Buy Assets",
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            PillButton(
                text = "Withdraw",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun OutlinedPillButtonPreview() {
    BanygTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedPillButton(
                text = "Sell Assets",
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedPillButton(
                text = "Deposit",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun SecondaryPillButtonPreview() {
    BanygTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryPillButton(
                text = "Market Stats",
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun PillButtonRowPreview() {
    BanygTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedPillButton(
                text = "Sell Assets",
                onClick = {},
                modifier = Modifier.weight(1f)
            )

            PillButton(
                text = "Buy Assets",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}
