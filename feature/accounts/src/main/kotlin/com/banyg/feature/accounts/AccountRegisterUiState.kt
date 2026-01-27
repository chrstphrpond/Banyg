package com.banyg.feature.accounts

import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency

sealed interface AccountRegisterUiState {
    data object Loading : AccountRegisterUiState

    data class Editing(
        val name: String = "",
        val type: AccountType = AccountType.CHECKING,
        val currency: Currency = Currency.PHP,
        val openingBalanceInput: String = "",
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : AccountRegisterUiState

    data class Saved(
        val accountId: String
    ) : AccountRegisterUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : AccountRegisterUiState
}

sealed interface AccountRegisterUiEvent {
    data class OnNameChanged(val value: String) : AccountRegisterUiEvent
    data class OnTypeSelected(val type: AccountType) : AccountRegisterUiEvent
    data class OnCurrencySelected(val currency: Currency) : AccountRegisterUiEvent
    data class OnOpeningBalanceChanged(val value: String) : AccountRegisterUiEvent
    data object OnSave : AccountRegisterUiEvent
    data object OnRetry : AccountRegisterUiEvent
}
