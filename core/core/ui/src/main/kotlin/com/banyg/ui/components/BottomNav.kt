package com.banyg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.banyg.ui.theme.BanygTheme

/**
 * BottomNav - Bottom navigation bar with pill shape
 */
@Composable
fun BanygBottomNav(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem>
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(BanygTheme.spacing.regular),
        shape = BanygTheme.shapes.bottomNav,
        color = BanygTheme.colors.surfaceDarkElevated,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(
                    horizontal = BanygTheme.spacing.regular,
                    vertical = BanygTheme.spacing.medium
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                BottomNavIconButton(
                    icon = item.icon,
                    selected = selectedIndex == index,
                    onClick = { onItemSelected(index) },
                    contentDescription = item.contentDescription
                )
            }
        }
    }
}

/**
 * BottomNavItem - Data class for bottom nav items
 */
data class BottomNavItem(
    val icon: ImageVector,
    val contentDescription: String? = null
)

/**
 * BottomNavIconButton - Individual icon button in bottom nav
 */
@Composable
private fun BottomNavIconButton(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val backgroundColor = if (selected) {
        BanygTheme.colors.textPrimary
    } else {
        Color.Transparent
    }

    val iconColor = if (selected) {
        BanygTheme.colors.textOnLime
    } else {
        BanygTheme.colors.textSecondary
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = iconColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Previews

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun BottomNavPreview() {
    BanygTheme {
        var selectedIndex by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            BanygBottomNav(
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it },
                items = listOf(
                    BottomNavItem(Icons.Default.Home, "Home"),
                    BottomNavItem(Icons.Default.BarChart, "Stats"),
                    BottomNavItem(Icons.Default.PieChart, "Charts"),
                    BottomNavItem(Icons.Default.Folder, "Documents"),
                    BottomNavItem(Icons.Default.AccountCircle, "Profile")
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun BottomNavSelectedSecondPreview() {
    BanygTheme {
        var selectedIndex by remember { mutableIntStateOf(2) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            BanygBottomNav(
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it },
                items = listOf(
                    BottomNavItem(Icons.Default.Home, "Home"),
                    BottomNavItem(Icons.Default.BarChart, "Stats"),
                    BottomNavItem(Icons.Default.PieChart, "Charts"),
                    BottomNavItem(Icons.Default.Folder, "Documents"),
                    BottomNavItem(Icons.Default.AccountCircle, "Profile")
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun BottomNavFourItemsPreview() {
    BanygTheme {
        var selectedIndex by remember { mutableIntStateOf(1) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            BanygBottomNav(
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it },
                items = listOf(
                    BottomNavItem(Icons.Default.Home, "Home"),
                    BottomNavItem(Icons.Default.BarChart, "Stats"),
                    BottomNavItem(Icons.Default.Folder, "Documents"),
                    BottomNavItem(Icons.Default.AccountCircle, "Profile")
                )
            )
        }
    }
}
