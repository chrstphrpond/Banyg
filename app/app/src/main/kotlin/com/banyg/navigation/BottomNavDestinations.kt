package com.banyg.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.banyg.feature.accounts.navigation.ACCOUNTS_ROUTE
import com.banyg.feature.budget.navigation.BUDGET_ROUTE
import com.banyg.feature.inbox.navigation.INBOX_ROUTE
import com.banyg.feature.reports.navigation.REPORTS_ROUTE

/**
 * Bottom navigation destinations for the main app navigation
 */
enum class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    INBOX(
        route = INBOX_ROUTE,
        label = "Inbox",
        icon = Icons.Default.Inbox
    ),
    ACCOUNTS(
        route = ACCOUNTS_ROUTE,
        label = "Accounts",
        icon = Icons.Default.AccountBalanceWallet
    ),
    BUDGET(
        route = BUDGET_ROUTE,
        label = "Budget",
        icon = Icons.Default.PieChart
    ),
    REPORTS(
        route = REPORTS_ROUTE,
        label = "Reports",
        icon = Icons.Default.Assessment
    );

    companion object {
        val entries = listOf(INBOX, ACCOUNTS, BUDGET, REPORTS)
    }
}
