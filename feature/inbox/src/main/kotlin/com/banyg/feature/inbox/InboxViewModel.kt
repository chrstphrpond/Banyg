package com.banyg.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.TransactionRepository
import com.banyg.domain.usecase.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Inbox screen.
 * 
 * Manages pending transactions that need categorization or review.
 * Provides actions to categorize, mark as cleared, or delete transactions.
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    private val _events = Channel<InboxEvent>()
    val events = _events.receiveAsFlow()

    private var observeJob: Job? = null

    init {
        loadInbox()
    }

    /**
     * Load pending transactions and categories.
     */
    fun loadInbox() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = InboxUiState.Loading

            combine(
                transactionRepository.observePendingTransactions(),
                getCategoriesUseCase()
            ) { transactions, categories ->
                // Map transactions with their categories
                val transactionsWithCategories = transactions.map { transaction ->
                    val category = transaction.categoryId?.let { catId ->
                        categories.find { it.id == catId }
                    }
                    TransactionWithCategory(transaction, category)
                }
                InboxUiState.Success(
                    transactions = transactionsWithCategories,
                    categories = categories
                )
            }
                .catch { throwable ->
                    _uiState.value = InboxUiState.Error(
                        message = throwable.message ?: "Failed to load transactions"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    /**
     * Categorize a transaction with the selected category.
     */
    fun categorizeTransaction(transactionId: String, categoryId: String) {
        viewModelScope.launch {
            try {
                val transaction = transactionRepository.getTransaction(transactionId)
                if (transaction != null) {
                    val updatedTransaction = transaction.copy(
                        categoryId = categoryId
                    )
                    transactionRepository.saveTransaction(updatedTransaction)
                    _events.send(InboxEvent.ShowSnackbar("Transaction categorized"))
                }
            } catch (e: Exception) {
                _events.send(InboxEvent.ShowSnackbar("Failed to categorize: ${e.message}"))
            }
        }
    }

    /**
     * Mark a transaction as cleared.
     */
    fun markAsCleared(transactionId: String) {
        viewModelScope.launch {
            try {
                transactionRepository.updateStatus(transactionId, TransactionStatus.CLEARED)
                _events.send(InboxEvent.ShowSnackbar("Transaction marked as cleared"))
            } catch (e: Exception) {
                _events.send(InboxEvent.ShowSnackbar("Failed to clear transaction: ${e.message}"))
            }
        }
    }

    /**
     * Delete a transaction permanently.
     */
    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transactionId)
                _events.send(InboxEvent.ShowSnackbar("Transaction deleted"))
            } catch (e: Exception) {
                _events.send(InboxEvent.ShowSnackbar("Failed to delete: ${e.message}"))
            }
        }
    }

    /**
     * Handle UI events from the screen.
     */
    fun onEvent(event: InboxUiEvent) {
        when (event) {
            is InboxUiEvent.OnTransactionClick -> { /* Navigation handled by UI */ }
            is InboxUiEvent.OnCategorySelect -> categorizeTransaction(event.transactionId, event.categoryId)
            is InboxUiEvent.OnMarkCleared -> markAsCleared(event.transactionId)
            is InboxUiEvent.OnDismiss -> markAsCleared(event.transactionId)
            InboxUiEvent.OnRefresh -> refreshData()
            InboxUiEvent.OnRetry -> loadInbox()
            InboxUiEvent.OnImportCsv -> navigateToCsvImport()
        }
    }

    private fun navigateToCsvImport() {
        viewModelScope.launch {
            _events.send(InboxEvent.NavigateToCsvImport)
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is InboxUiState.Success) {
                _uiState.update { currentState.copy(isRefreshing = true) }
                observeJob?.cancel()
                loadInbox()
            }
        }
    }
}

/**
 * One-time events sent from ViewModel to UI.
 */
sealed class InboxEvent {
    data class ShowSnackbar(val message: String) : InboxEvent()
    data object NavigateToCsvImport : InboxEvent()
}
