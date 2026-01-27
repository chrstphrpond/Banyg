package com.banyg.feature.inbox.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banyg.feature.inbox.InboxRoute

const val INBOX_ROUTE = "inbox"

fun NavController.navigateToInbox() {
    navigate(INBOX_ROUTE)
}

fun NavGraphBuilder.inboxScreen(
    onNavigateToTransactionDetail: (String) -> Unit,
    onNavigateToCsvImport: () -> Unit
) {
    composable(route = INBOX_ROUTE) {
        InboxRoute(
            onNavigateToTransactionDetail = onNavigateToTransactionDetail,
            onNavigateToCsvImport = onNavigateToCsvImport
        )
    }
}
