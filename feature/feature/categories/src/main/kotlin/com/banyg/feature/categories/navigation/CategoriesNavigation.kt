package com.banyg.feature.categories.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.banyg.feature.categories.CategoriesRoute
import com.banyg.feature.categories.CategoryFormRoute

const val CATEGORIES_ROUTE = "categories"
const val CATEGORY_FORM_ROUTE = "categories/form"
const val CATEGORY_EDIT_ROUTE = "categories/edit/{categoryId}"

fun NavController.navigateToCategories() {
    navigate(CATEGORIES_ROUTE)
}

fun NavController.navigateToCategoryForm() {
    navigate(CATEGORY_FORM_ROUTE)
}

fun NavController.navigateToCategoryEdit(categoryId: String) {
    navigate("categories/edit/$categoryId")
}

fun NavGraphBuilder.categoriesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddCategory: () -> Unit,
    onNavigateToEditCategory: (String) -> Unit
) {
    composable(route = CATEGORIES_ROUTE) {
        CategoriesRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToAddCategory = onNavigateToAddCategory,
            onNavigateToEditCategory = onNavigateToEditCategory
        )
    }
}

fun NavGraphBuilder.categoryFormScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = CATEGORY_FORM_ROUTE) {
        CategoryFormRoute(
            categoryId = null,
            onNavigateBack = onNavigateBack
        )
    }
}

fun NavGraphBuilder.categoryEditScreen(
    onNavigateBack: () -> Unit
) {
    composable(
        route = CATEGORY_EDIT_ROUTE,
        arguments = listOf(
            navArgument("categoryId") {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId")
        CategoryFormRoute(
            categoryId = categoryId,
            onNavigateBack = onNavigateBack
        )
    }
}
