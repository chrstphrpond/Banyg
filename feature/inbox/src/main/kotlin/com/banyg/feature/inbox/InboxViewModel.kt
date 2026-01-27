package com.banyg.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.CategoryRepository
import com.banyg.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadData()
    }

    fun onEvent(event: InboxUiEvent) {
        when (event) {
            is InboxUiEvent.OnTransactionClick -> handleTransactionClick(event.transactionId)
            is InboxUiEvent.OnCategorySelect -> handleCategorySelect(event.transactionId, event.categoryId)
            is InboxUiEvent.OnMarkCleared -> handleMarkCleared(event.transactionId)
            is InboxUiEvent.OnDismiss -> handleDismiss(event.transactionId)
            InboxUiEvent.OnRefresh -> refreshData()
            InboxUiEvent.OnRetry -> loadData()
        }
    }

    private fun loadData() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = InboxUiState.Loading

            combine(
                transactionRepository.observePendingTransactions(),
                categoryRepository.observeVisibleCategories()
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

    private fun refreshData() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is InboxUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
                // Re-collect will automatically update with fresh data
                observeJob?.cancel()
                loadData()
            }
        }
    }

    private fun handleTransactionClick(transactionId: String) {
        // Navigation to transaction detail will be handled by the caller
    }

    private fun handleCategorySelect(transactionId: String, categoryId: String) {
        viewModelScope.launch {
            try {
                val transaction = transactionRepository.getTransaction(transactionId)
                if (transaction != null) {
                    val updatedTransaction = transaction.copy(
                        categoryId = categoryId
                    )
                    transactionRepository.saveTransaction(updatedTransaction)
                }
            } catch (e: Exception) {
                // Error handling - could show a snackbar or toast
                // For now, the flow will automatically refresh on next emission
            }
        }
    }

    private fun handleMarkCleared(transactionId: String) {
        viewModelScope.launch {
            try {
                transactionRepository.updateStatus(transactionId, TransactionStatus.CLEARED)
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    private fun handleDismiss(transactionId: String) {
        viewModelScope.launch {
            try {
                // For now, dismiss marks the transaction as cleared
                // This behavior can be customized based on requirements
                transactionRepository.updateStatus(transactionId, TransactionStatus.CLEARED)
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
}
