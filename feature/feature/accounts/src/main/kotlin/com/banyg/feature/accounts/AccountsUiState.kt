package com.banyg.feature.accounts

import com.banyg.domain.model.Account

sealed interface AccountsUiState {
    data object Loading : AccountsUiState

    data class Success(
        val accounts: List<Account>
    ) : AccountsUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : AccountsUiState
}

sealed interface AccountsUiEvent {
    data object OnRetry : AccountsUiEvent
    data object OnRefresh : AccountsUiEvent
}
