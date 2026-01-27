package com.banyg.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banyg.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountsUiState>(AccountsUiState.Loading)
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeAccounts()
    }

    fun onEvent(event: AccountsUiEvent) {
        when (event) {
            AccountsUiEvent.OnRefresh -> observeAccounts()
            AccountsUiEvent.OnRetry -> observeAccounts()
        }
    }

    private fun observeAccounts() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = AccountsUiState.Loading
            accountRepository.observeActiveAccounts()
                .catch { throwable ->
                    _uiState.value = AccountsUiState.Error(
                        message = throwable.message ?: "Failed to load accounts"
                    )
                }
                .collect { accounts ->
                    _uiState.value = AccountsUiState.Success(accounts = accounts)
                }
        }
    }
}
