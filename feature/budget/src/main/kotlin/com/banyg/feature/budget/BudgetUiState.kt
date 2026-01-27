package com.banyg.feature.budget

import com.banyg.domain.model.Category
import com.banyg.domain.model.Money

/**
 * Budget domain model representing a spending limit for a category.
 */
data class Budget(
    val id: String,
    val categoryId: String,
    val category: Category? = null,
    val amount: Money,
    val period: BudgetPeriod,
    val isActive: Boolean = true
) {
    /**
     * Calculate remaining budget amount.
     */
    fun calculateRemaining(spent: Money): Money {
        return Money(
            minorUnits = amount.minorUnits - kotlin.math.abs(spent.minorUnits),
            currency = amount.currency
        )
    }

    /**
     * Calculate progress percentage (0.0 to 1.0+).
     */
    fun calculateProgress(spent: Money): Float {
        val budgetAmount = kotlin.math.abs(amount.minorUnits)
        val spentAmount = kotlin.math.abs(spent.minorUnits)
        return if (budgetAmount > 0) {
            (spentAmount.toFloat() / budgetAmount.toFloat()).coerceIn(0f, 1.5f)
        } else {
            0f
        }
    }
}

/**
 * Budget period types.
 */
enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * Budget with calculated spending information.
 */
data class BudgetWithProgress(
    val budget: Budget,
    val spent: Money,
    val remaining: Money,
    val progress: Float,
    val isOverBudget: Boolean
)

/**
 * Budget screen UI state.
 */
sealed interface BudgetUiState {
    data object Loading : BudgetUiState

    data class Success(
        val budgets: List<BudgetWithProgress>,
        val categories: List<Category>,
        val totalBudgeted: Money? = null,
        val totalSpent: Money? = null,
        val isRefreshing: Boolean = false
    ) : BudgetUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : BudgetUiState
}

/**
 * Budget form UI state for creating/editing budgets.
 */
data class BudgetFormState(
    val categoryId: String = "",
    val amountInput: String = "",
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() = categoryId.isNotBlank() && 
                amountInput.isNotBlank() && 
                amountInput.toDoubleOrNull() != null &&
                amountInput.toDoubleOrNull() ?: 0.0 > 0
}

/**
 * Budget screen UI events.
 */
sealed interface BudgetUiEvent {
    data object OnRefresh : BudgetUiEvent
    data object OnRetry : BudgetUiEvent
    data object OnAddBudget : BudgetUiEvent
    data class OnEditBudget(val budgetId: String) : BudgetUiEvent
    data class OnDeleteBudget(val budgetId: String) : BudgetUiEvent
    data class OnCategorySelected(val categoryId: String) : BudgetUiEvent
    data class OnAmountChanged(val value: String) : BudgetUiEvent
    data class OnPeriodChanged(val period: BudgetPeriod) : BudgetUiEvent
    data object OnSaveBudget : BudgetUiEvent
    data object OnCancel : BudgetUiEvent
}

/**
 * One-time events sent from ViewModel to UI.
 */
sealed class BudgetEvent {
    data class ShowSnackbar(val message: String) : BudgetEvent()
    data object NavigateToAddBudget : BudgetEvent()
    data class NavigateToEditBudget(val budgetId: String) : BudgetEvent()
    data object NavigateBack : BudgetEvent()
}
