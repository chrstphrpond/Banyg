package com.banyg.feature.inbox

import com.banyg.domain.model.Category
import com.banyg.domain.model.Transaction

/**
 * UI State for the Inbox screen
 */
sealed interface InboxUiState {
    data object Loading : InboxUiState

    data class Success(
        val transactions: List<TransactionWithCategory>,
        val categories: List<Category>,
        val isRefreshing: Boolean = false
    ) : InboxUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : InboxUiState
}

/**
 * Wrapper for transaction with its assigned category (if any)
 */
data class TransactionWithCategory(
    val transaction: Transaction,
    val category: Category? = null
)

/**
 * UI Events from user interactions in Inbox screen
 */
sealed interface InboxUiEvent {
    data class OnTransactionClick(val transactionId: String) : InboxUiEvent
    data class OnCategorySelect(val transactionId: String, val categoryId: String) : InboxUiEvent
    data class OnMarkCleared(val transactionId: String) : InboxUiEvent
    data class OnDismiss(val transactionId: String) : InboxUiEvent
    data object OnRefresh : InboxUiEvent
    data object OnRetry : InboxUiEvent
    data object OnImportCsv : InboxUiEvent
}
