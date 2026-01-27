package com.banyg.feature.accounts

import com.banyg.domain.model.Account

enum class TransactionDirection {
    EXPENSE,
    INCOME
}

sealed interface AddTransactionUiState {
    data object Loading : AddTransactionUiState

    data class Editing(
        val accounts: List<Account> = emptyList(),
        val selectedAccountId: String? = null,
        val amountInput: String = "",
        val merchantInput: String = "",
        val memoInput: String = "",
        val direction: TransactionDirection = TransactionDirection.EXPENSE,
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : AddTransactionUiState

    data class Saved(
        val transactionId: String
    ) : AddTransactionUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : AddTransactionUiState
}

sealed interface AddTransactionUiEvent {
    data class OnAccountSelected(val accountId: String) : AddTransactionUiEvent
    data class OnAmountChanged(val value: String) : AddTransactionUiEvent
    data class OnMerchantChanged(val value: String) : AddTransactionUiEvent
    data class OnMemoChanged(val value: String) : AddTransactionUiEvent
    data class OnDirectionChanged(val direction: TransactionDirection) : AddTransactionUiEvent
    data object OnSave : AddTransactionUiEvent
    data object OnRetry : AddTransactionUiEvent
}
