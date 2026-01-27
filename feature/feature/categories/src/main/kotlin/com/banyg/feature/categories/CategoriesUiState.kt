package com.banyg.feature.categories

import com.banyg.domain.model.Category

/**
 * Categories screen UI state
 */
sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState

    data class Success(
        val categories: List<Category>,
        val groupedCategories: Map<String?, List<Category>>,
        val hiddenCategories: List<Category>,
        val showHidden: Boolean = false,
        val isRefreshing: Boolean = false
    ) : CategoriesUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : CategoriesUiState
}

/**
 * Categories screen UI events
 */
sealed interface CategoriesUiEvent {
    data object OnRefresh : CategoriesUiEvent
    data object OnRetry : CategoriesUiEvent
    data object OnAddCategory : CategoriesUiEvent
    data object OnToggleShowHidden : CategoriesUiEvent
    data class OnCategoryClick(val categoryId: String) : CategoriesUiEvent
    data class OnEditCategory(val categoryId: String) : CategoriesUiEvent
    data class OnDeleteCategory(val categoryId: String) : CategoriesUiEvent
    data class OnToggleHidden(val categoryId: String) : CategoriesUiEvent
    data class OnRestoreCategory(val categoryId: String) : CategoriesUiEvent
}

/**
 * Category form UI state
 */
sealed interface CategoryFormUiState {
    data object Loading : CategoryFormUiState

    data class Edit(
        val category: Category,
        val name: String,
        val groupId: String?,
        val groupName: String?,
        val icon: String?,
        val color: String?,
        val isHidden: Boolean,
        val isSaving: Boolean = false,
        val error: String? = null
    ) : CategoryFormUiState

    data class Create(
        val name: String = "",
        val groupId: String? = null,
        val groupName: String? = null,
        val icon: String? = null,
        val color: String? = null,
        val isSaving: Boolean = false,
        val error: String? = null
    ) : CategoryFormUiState

    data class Error(val message: String) : CategoryFormUiState
}

/**
 * Category form UI events
 */
sealed interface CategoryFormUiEvent {
    data class OnNameChange(val name: String) : CategoryFormUiEvent
    data class OnGroupChange(val groupId: String?, val groupName: String?) : CategoryFormUiEvent
    data class OnIconChange(val icon: String?) : CategoryFormUiEvent
    data class OnColorChange(val color: String?) : CategoryFormUiEvent
    data object OnSave : CategoryFormUiEvent
    data object OnCancel : CategoryFormUiEvent
}
