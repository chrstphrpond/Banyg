package com.banyg.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banyg.domain.model.Money
import com.banyg.domain.usecase.CreateAccountUseCase
import com.banyg.ui.format.parseToMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountRegisterViewModel @Inject constructor(
    private val createAccountUseCase: CreateAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountRegisterUiState>(AccountRegisterUiState.Editing())
    val uiState: StateFlow<AccountRegisterUiState> = _uiState.asStateFlow()

    fun onEvent(event: AccountRegisterUiEvent) {
        when (event) {
            is AccountRegisterUiEvent.OnNameChanged -> updateEditing {
                it.copy(name = event.value, errorMessage = null)
            }

            is AccountRegisterUiEvent.OnTypeSelected -> updateEditing {
                it.copy(type = event.type, errorMessage = null)
            }

            is AccountRegisterUiEvent.OnCurrencySelected -> updateEditing {
                it.copy(currency = event.currency, errorMessage = null)
            }

            is AccountRegisterUiEvent.OnOpeningBalanceChanged -> updateEditing {
                it.copy(openingBalanceInput = event.value, errorMessage = null)
            }

            AccountRegisterUiEvent.OnSave -> saveAccount()
            AccountRegisterUiEvent.OnRetry -> resetForm()
        }
    }

    private fun updateEditing(update: (AccountRegisterUiState.Editing) -> AccountRegisterUiState.Editing) {
        val current = _uiState.value
        if (current is AccountRegisterUiState.Editing) {
            _uiState.value = update(current)
        }
    }

    private fun resetForm() {
        _uiState.value = AccountRegisterUiState.Editing()
    }

    private fun saveAccount() {
        val current = _uiState.value
        if (current !is AccountRegisterUiState.Editing || current.isSaving) return

        val trimmedName = current.name.trim()
        if (trimmedName.isBlank()) {
            updateEditing { it.copy(errorMessage = "Account name is required") }
            return
        }

        val openingBalance = parseOpeningBalance(current)
        if (openingBalance == null) {
            updateEditing { it.copy(errorMessage = "Enter a valid opening balance") }
            return
        }

        updateEditing { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val account = createAccountUseCase(
                    name = trimmedName,
                    type = current.type,
                    currency = current.currency,
                    openingBalance = openingBalance
                )
                _uiState.value = AccountRegisterUiState.Saved(accountId = account.id)
            } catch (e: Exception) {
                updateEditing {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to create account"
                    )
                }
            }
        }
    }

    private fun parseOpeningBalance(state: AccountRegisterUiState.Editing): Money? {
        val trimmed = state.openingBalanceInput.trim()
        if (trimmed.isBlank()) {
            return Money.zero(state.currency)
        }
        return trimmed.parseToMoney(state.currency)
    }
}
