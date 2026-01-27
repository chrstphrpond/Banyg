package com.banyg.feature.accounts.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banyg.feature.accounts.AccountRegisterRoute
import com.banyg.feature.accounts.AccountsRoute
import com.banyg.feature.accounts.AddTransactionRoute

const val ACCOUNTS_ROUTE = "accounts"
const val ACCOUNT_REGISTER_ROUTE = "accounts/register"
const val ACCOUNT_ADD_TRANSACTION_ROUTE = "accounts/add-transaction"

fun NavController.navigateToAccounts() {
    navigate(ACCOUNTS_ROUTE)
}

fun NavController.navigateToAccountRegister() {
    navigate(ACCOUNT_REGISTER_ROUTE)
}

fun NavController.navigateToAddTransaction() {
    navigate(ACCOUNT_ADD_TRANSACTION_ROUTE)
}

fun NavGraphBuilder.accountsScreen(
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAddTransaction: () -> Unit
) {
    composable(route = ACCOUNTS_ROUTE) {
        AccountsRoute(
            onNavigateToAddAccount = onNavigateToAddAccount,
            onNavigateToAddTransaction = onNavigateToAddTransaction
        )
    }
}

fun NavGraphBuilder.accountRegisterScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = ACCOUNT_REGISTER_ROUTE) {
        AccountRegisterRoute(onNavigateBack = onNavigateBack)
    }
}

fun NavGraphBuilder.addTransactionScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = ACCOUNT_ADD_TRANSACTION_ROUTE) {
        AddTransactionRoute(onNavigateBack = onNavigateBack)
    }
}
