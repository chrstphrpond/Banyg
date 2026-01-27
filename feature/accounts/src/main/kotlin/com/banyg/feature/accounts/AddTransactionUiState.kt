package com.banyg.feature.accounts

import com.banyg.domain.model.Account
import com.banyg.domain.model.Category
import com.banyg.domain.model.Currency
import com.banyg.ui.format.parseToMoney

/**
 * Transaction direction for the form.
 */
enum class TransactionDirection {
    EXPENSE,
    INCOME
}

/**
 * Add Transaction screen UI state.
 */
sealed interface AddTransactionUiState {
    data object Loading : AddTransactionUiState

    data class Editing(
        val accounts: List<Account> = emptyList(),
        val categories: List<Category> = emptyList(),
        val selectedAccountId: String? = null,
        val selectedCategoryId: String? = null,
        val amountInput: String = "",
        val merchantInput: String = "",
        val memoInput: String = "",
        val direction: TransactionDirection = TransactionDirection.EXPENSE,
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : AddTransactionUiState {
        
        /**
         * Check if the form is valid for submission.
         */
        val isValid: Boolean
            get() = selectedAccountId != null &&
                    amountInput.isNotBlank() &&
                    amountInput.parseToMoney(selectedAccount?.currency ?: Currency.PHP) != null &&
                    merchantInput.isNotBlank()

        /**
         * Get the selected account or null.
         */
        val selectedAccount: Account?
            get() = accounts.firstOrNull { it.id == selectedAccountId }

        /**
         * Get the selected category or null.
         */
        val selectedCategory: Category?
            get() = categories.firstOrNull { it.id == selectedCategoryId }
    }

    data class Saved(
        val transactionId: String
    ) : AddTransactionUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : AddTransactionUiState
}

/**
 * Add Transaction screen UI events.
 */
sealed interface AddTransactionUiEvent {
    data class OnAccountSelected(val accountId: String) : AddTransactionUiEvent
    data class OnAmountChanged(val value: String) : AddTransactionUiEvent
    data class OnMerchantChanged(val value: String) : AddTransactionUiEvent
    data class OnMemoChanged(val value: String) : AddTransactionUiEvent
    data class OnDirectionChanged(val direction: TransactionDirection) : AddTransactionUiEvent
    data class OnCategorySelected(val categoryId: String?) : AddTransactionUiEvent
    data object OnSave : AddTransactionUiEvent
    data object OnRetry : AddTransactionUiEvent
    data object OnReset : AddTransactionUiEvent
}
