package com.banyg.feature.csvimport.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banyg.feature.csvimport.CsvImportRoute

const val CSV_IMPORT_ROUTE = "csv-import"

fun NavController.navigateToCsvImport() {
    navigate(CSV_IMPORT_ROUTE)
}

fun NavGraphBuilder.csvImportScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = CSV_IMPORT_ROUTE) {
        CsvImportRoute()
    }
}
