package com.banyg.feature.reports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banyg.feature.reports.ReportsRoute

const val REPORTS_ROUTE = "reports"

fun NavController.navigateToReports() {
    navigate(REPORTS_ROUTE)
}

fun NavGraphBuilder.reportsScreen() {
    composable(route = REPORTS_ROUTE) {
        ReportsRoute()
    }
}
