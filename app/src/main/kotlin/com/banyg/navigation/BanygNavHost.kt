package com.banyg.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.banyg.feature.accounts.navigation.ACCOUNTS_ROUTE
import com.banyg.feature.accounts.navigation.accountRegisterScreen
import com.banyg.feature.accounts.navigation.accountsScreen
import com.banyg.feature.accounts.navigation.addTransactionScreen
import com.banyg.feature.accounts.navigation.navigateToAccountRegister
import com.banyg.feature.accounts.navigation.navigateToAddTransaction
import com.banyg.feature.budget.navigation.BUDGET_ROUTE
import com.banyg.feature.budget.navigation.budgetScreen
import com.banyg.feature.inbox.navigation.INBOX_ROUTE
import com.banyg.feature.inbox.navigation.inboxScreen
import com.banyg.feature.reports.navigation.REPORTS_ROUTE
import com.banyg.feature.reports.navigation.reportsScreen

/**
 * Main navigation host for Banyg app
 * Defines the top-level navigation graph with bottom nav destinations
 */
@Composable
fun BanygNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = INBOX_ROUTE,
        modifier = modifier
    ) {
        // Inbox feature (start destination)
        inboxScreen(
            onNavigateToTransactionDetail = { transactionId ->
                // TODO: Navigate to transaction detail screen when implemented
            }
        )

        // Accounts feature with nested navigation
        accountsScreen(
            onNavigateToAddAccount = { navController.navigateToAccountRegister() },
            onNavigateToAddTransaction = { navController.navigateToAddTransaction() }
        )
        accountRegisterScreen(
            onNavigateBack = { navController.popBackStack() }
        )
        addTransactionScreen(
            onNavigateBack = { navController.popBackStack() }
        )

        // Budget feature
        budgetScreen()

        // Reports feature
        reportsScreen()
    }
}
