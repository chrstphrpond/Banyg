package com.banyg.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banyg.domain.model.Account
import com.banyg.domain.model.Money
import com.banyg.domain.repository.AccountRepository
import com.banyg.domain.usecase.AddManualTransactionUseCase
import com.banyg.ui.format.parseToMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val addManualTransactionUseCase: AddManualTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddTransactionUiState>(AddTransactionUiState.Loading)
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeAccounts()
    }

    fun onEvent(event: AddTransactionUiEvent) {
        when (event) {
            is AddTransactionUiEvent.OnAccountSelected -> updateEditing {
                it.copy(selectedAccountId = event.accountId, errorMessage = null)
            }

            is AddTransactionUiEvent.OnAmountChanged -> updateEditing {
                it.copy(amountInput = event.value, errorMessage = null)
            }

            is AddTransactionUiEvent.OnMerchantChanged -> updateEditing {
                it.copy(merchantInput = event.value, errorMessage = null)
            }

            is AddTransactionUiEvent.OnMemoChanged -> updateEditing {
                it.copy(memoInput = event.value, errorMessage = null)
            }

            is AddTransactionUiEvent.OnDirectionChanged -> updateEditing {
                it.copy(direction = event.direction, errorMessage = null)
            }

            AddTransactionUiEvent.OnSave -> saveTransaction()
            AddTransactionUiEvent.OnRetry -> observeAccounts()
        }
    }

    private fun observeAccounts() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = AddTransactionUiState.Loading
            accountRepository.observeActiveAccounts()
                .catch { throwable ->
                    _uiState.value = AddTransactionUiState.Error(
                        message = throwable.message ?: "Failed to load accounts"
                    )
                }
                .collect { accounts ->
                    if (accounts.isEmpty()) {
                        _uiState.value = AddTransactionUiState.Error(
                            message = "Create an account before adding transactions",
                            canRetry = true
                        )
                        return@collect
                    }

                    val current = _uiState.value
                    val editing = if (current is AddTransactionUiState.Editing) {
                        current.copy(accounts = accounts)
                    } else {
                        AddTransactionUiState.Editing(accounts = accounts)
                    }

                    val selectedId = editing.selectedAccountId
                        ?.takeIf { id -> accounts.any { it.id == id } }
                        ?: accounts.first().id
                    _uiState.value = editing.copy(selectedAccountId = selectedId)
                }
        }
    }

    private fun updateEditing(update: (AddTransactionUiState.Editing) -> AddTransactionUiState.Editing) {
        val current = _uiState.value
        if (current is AddTransactionUiState.Editing) {
            _uiState.value = update(current)
        }
    }

    private fun saveTransaction() {
        val current = _uiState.value
        if (current !is AddTransactionUiState.Editing || current.isSaving) return

        val account = current.accounts.firstOrNull { it.id == current.selectedAccountId }
        if (account == null) {
            updateEditing { it.copy(errorMessage = "Select an account") }
            return
        }

        val amount = parseAmount(current, account)
        if (amount == null) {
            updateEditing { it.copy(errorMessage = "Enter a valid amount") }
            return
        }

        updateEditing { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val transaction = addManualTransactionUseCase(
                    account = account,
                    amount = amount,
                    merchant = current.merchantInput,
                    memo = current.memoInput
                )
                _uiState.value = AddTransactionUiState.Saved(transactionId = transaction.id)
            } catch (e: Exception) {
                updateEditing {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save transaction"
                    )
                }
            }
        }
    }

    private fun parseAmount(state: AddTransactionUiState.Editing, account: Account): Money? {
        val trimmed = state.amountInput.trim()
        if (trimmed.isBlank()) return null
        val parsed = trimmed.parseToMoney(account.currency) ?: return null
        val absoluteMinor = abs(parsed.minorUnits)
        if (absoluteMinor == 0L) return null
        val signedMinor = if (state.direction == TransactionDirection.EXPENSE) {
            -absoluteMinor
        } else {
            absoluteMinor
        }
        return Money(signedMinor, account.currency)
    }
}
