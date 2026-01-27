package com.banyg.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.banyg.ui.theme.BanygTheme

@Composable
fun ReportsRoute() {
    ReportsScreen()
}

@Composable
private fun ReportsScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = BanygTheme.spacing.screenPadding,
            vertical = BanygTheme.spacing.regular
        ),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        item {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.headlineLarge,
                color = BanygTheme.colors.textPrimary
            )
        }

        item {
            ReportsPlaceholder()
        }
    }
}

@Composable
private fun ReportsPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
        ) {
            Text(
                text = "Reports feature coming soon",
                style = MaterialTheme.typography.titleMedium,
                color = BanygTheme.colors.textSecondary
            )
            Text(
                text = "Visualize your spending patterns and trends",
                style = MaterialTheme.typography.bodyMedium,
                color = BanygTheme.colors.textTertiary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun ReportsScreenPreview() {
    BanygTheme {
        ReportsScreen()
    }
}
