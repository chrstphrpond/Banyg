package com.banyg.feature.csvimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banyg.domain.csv.ColumnMapping
import com.banyg.domain.csv.CsvTransactionParser
import com.banyg.domain.csv.DuplicateStatus
import com.banyg.domain.model.Account
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.AccountRepository
import com.banyg.domain.repository.TransactionRepository
import com.banyg.domain.usecase.ImportCsvTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the CSV Import screen.
 *
 * Manages the CSV import workflow:
 * 1. File selection
 * 2. Column mapping configuration
 * 3. Preview with duplicate detection
 * 4. Import execution
 */
@HiltViewModel
class CsvImportViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CsvImportUiState>(CsvImportUiState.Initial)
    val uiState: StateFlow<CsvImportUiState> = _uiState.asStateFlow()

    private val _events = Channel<CsvImportEvent>()
    val events = _events.receiveAsFlow()

    private val importUseCase = ImportCsvTransactionsUseCase(transactionRepository)
    private val csvParser = CsvTransactionParser()

    private var currentFileName: String = ""
    private var currentCsvContent: String = ""

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = CsvImportUiState.Loading
            try {
                val accounts = accountRepository.observeActiveAccounts().first()
                _uiState.value = CsvImportUiState.Initial
            } catch (e: Exception) {
                _uiState.value = CsvImportUiState.Error(
                    message = e.message ?: "Failed to load accounts"
                )
            }
        }
    }

    /**
     * Handle file selection from the file picker.
     */
    fun onFileSelected(fileName: String, csvContent: String) {
        viewModelScope.launch {
            currentFileName = fileName
            currentCsvContent = csvContent

            // Get first few lines for preview (raw CSV lines)
            val previewRows = csvContent.lines()
                .take(6)
                .map { line -> listOf(line) } // Keep as single column for display

            val accounts = accountRepository.observeActiveAccounts().first()

            _uiState.value = CsvImportUiState.ColumnMapping(
                fileName = fileName,
                csvContent = csvContent,
                accounts = accounts,
                csvPreview = previewRows
            )
        }
    }

    /**
     * Auto-detect CSV format from the content.
     */
    fun autoDetectFormat() {
        val currentState = _uiState.value as? CsvImportUiState.ColumnMapping ?: return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isDetecting = true, error = null)

            try {
                // Use a default account for detection (currency)
                val currency = Currency.PHP
                val result = csvParser.parseAutoDetect(currentState.csvContent, currency)

                if (result != null) {
                    _uiState.value = currentState.copy(
                        detectedMapping = result.mapping,
                        columnMapping = result.mapping,
                        isDetecting = false
                    )
                    _events.send(CsvImportEvent.ShowSnackbar("Format detected successfully"))
                } else {
                    _uiState.value = currentState.copy(
                        isDetecting = false,
                        error = "Could not auto-detect format. Please configure manually."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isDetecting = false,
                    error = "Detection failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Generate preview with duplicate detection.
     */
    fun generatePreview() {
        val currentState = _uiState.value as? CsvImportUiState.ColumnMapping ?: return

        val accountId = currentState.selectedAccountId
        val mapping = currentState.columnMapping

        if (accountId == null || mapping == null) {
            _uiState.value = currentState.copy(error = "Please select account and configure columns")
            return
        }

        viewModelScope.launch {
            _uiState.value = CsvImportUiState.Loading

            try {
                val accounts = accountRepository.observeActiveAccounts().first()
                val account = accounts.find { it.id == accountId }
                    ?: throw IllegalStateException("Account not found")

                val preview = importUseCase.generatePreview(
                    csvContent = currentState.csvContent,
                    mapping = mapping,
                    account = account
                )

                // Map domain preview to UI model
                val transactions = preview.transactions.map { tx ->
                    ImportTransactionPreview(
                        id = tx.id,
                        date = tx.date,
                        amount = tx.amount,
                        merchant = tx.merchant,
                        rawDescription = tx.rawDescription,
                        duplicateStatus = tx.duplicateStatus,
                        isSelected = tx.isSelected,
                        categoryId = tx.categoryId,
                        rawRowIndex = tx.rawRowIndex
                    )
                }

                _uiState.value = CsvImportUiState.Preview(
                    fileName = currentState.fileName,
                    account = account,
                    transactions = transactions,
                    newCount = preview.newCount,
                    duplicateCount = preview.duplicateCount,
                    errorCount = preview.errorCount
                )
            } catch (e: Exception) {
                _uiState.value = CsvImportUiState.Error(
                    message = "Failed to generate preview: ${e.message}"
                )
            }
        }
    }

    /**
     * Import selected transactions.
     */
    fun importTransactions() {
        val currentState = _uiState.value as? CsvImportUiState.Preview ?: return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isImporting = true)

            try {
                val selectedTransactions = currentState.transactions.filter { it.isSelected }

                val result = importUseCase.importTransactions(
                    accountId = currentState.account.id,
                    currency = currentState.account.currency,
                    previews = selectedTransactions.map { tx ->
                        com.banyg.domain.csv.ImportTransactionPreview(
                            id = tx.id,
                            date = tx.date,
                            amount = tx.amount,
                            merchant = tx.merchant,
                            rawDescription = tx.rawDescription,
                            duplicateStatus = tx.duplicateStatus,
                            isSelected = tx.isSelected,
                            categoryId = tx.categoryId,
                            rawRowIndex = tx.rawRowIndex
                        )
                    }
                )

                _uiState.value = CsvImportUiState.ImportComplete(
                    importedCount = result.importedCount,
                    skippedCount = result.skippedCount,
                    duplicateCount = result.duplicateCount
                )

                _events.send(
                    CsvImportEvent.ShowSnackbar(
                        "Imported ${result.importedCount} transactions"
                    )
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(isImporting = false)
                _events.send(
                    CsvImportEvent.ShowSnackbar("Import failed: ${e.message}")
                )
            }
        }
    }

    /**
     * Toggle transaction selection.
     */
    fun toggleTransactionSelection(transactionId: String, isSelected: Boolean) {
        val currentState = _uiState.value as? CsvImportUiState.Preview ?: return

        val updatedTransactions = currentState.transactions.map { tx ->
            if (tx.id == transactionId) tx.copy(isSelected = isSelected) else tx
        }

        val newCount = updatedTransactions.count { it.isSelected && it.duplicateStatus is DuplicateStatus.NEW }
        val duplicateCount = updatedTransactions.count { it.isSelected && it.duplicateStatus is DuplicateStatus.DUPLICATE }

        _uiState.value = currentState.copy(
            transactions = updatedTransactions,
            newCount = newCount,
            duplicateCount = duplicateCount
        )
    }

    /**
     * Select or deselect all transactions.
     */
    fun selectAll(isSelected: Boolean) {
        val currentState = _uiState.value as? CsvImportUiState.Preview ?: return

        val updatedTransactions = currentState.transactions.map { tx ->
            tx.copy(isSelected = isSelected)
        }

        val newCount = updatedTransactions.count { it.isSelected && it.duplicateStatus is DuplicateStatus.NEW }
        val duplicateCount = updatedTransactions.count { it.isSelected && it.duplicateStatus is DuplicateStatus.DUPLICATE }

        _uiState.value = currentState.copy(
            transactions = updatedTransactions,
            newCount = newCount,
            duplicateCount = duplicateCount
        )
    }

    /**
     * Update account selection.
     */
    fun selectAccount(accountId: String) {
        val currentState = _uiState.value as? CsvImportUiState.ColumnMapping ?: return
        _uiState.value = currentState.copy(
            selectedAccountId = accountId,
            error = null
        )
    }

    /**
     * Update column mapping.
     */
    fun updateColumnMapping(mapping: ColumnMapping) {
        val currentState = _uiState.value as? CsvImportUiState.ColumnMapping ?: return
        _uiState.value = currentState.copy(
            columnMapping = mapping,
            error = null
        )
    }

    /**
     * Handle UI events from the screen.
     */
    fun onEvent(event: CsvImportUiEvent) {
        when (event) {
            CsvImportUiEvent.OnSelectFile -> viewModelScope.launch {
                _events.send(CsvImportEvent.LaunchFilePicker())
            }
            is CsvImportUiEvent.OnFileSelected -> onFileSelected(event.fileName, event.csvContent)
            is CsvImportUiEvent.OnAccountSelected -> selectAccount(event.accountId)
            CsvImportUiEvent.OnAutoDetect -> autoDetectFormat()
            is CsvImportUiEvent.OnColumnMappingChanged -> updateColumnMapping(event.mapping)
            CsvImportUiEvent.OnPreview -> generatePreview()
            is CsvImportUiEvent.OnTransactionSelected -> toggleTransactionSelection(event.transactionId, event.isSelected)
            is CsvImportUiEvent.OnSelectAll -> selectAll(event.isSelected)
            CsvImportUiEvent.OnImport -> importTransactions()
            CsvImportUiEvent.OnBack -> viewModelScope.launch {
                _events.send(CsvImportEvent.NavigateBack)
            }
            CsvImportUiEvent.OnRetry -> loadAccounts()
            CsvImportUiEvent.OnDismissComplete -> viewModelScope.launch {
                _events.send(CsvImportEvent.NavigateBack)
            }
        }
    }
}
