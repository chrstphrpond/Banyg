package com.banyg.feature.budget.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banyg.feature.budget.BudgetRoute

const val BUDGET_ROUTE = "budget"

fun NavController.navigateToBudget() {
    navigate(BUDGET_ROUTE)
}

fun NavGraphBuilder.budgetScreen() {
    composable(route = BUDGET_ROUTE) {
        BudgetRoute()
    }
}
