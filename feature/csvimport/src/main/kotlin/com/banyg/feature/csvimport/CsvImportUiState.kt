package com.banyg.feature.csvimport

import com.banyg.domain.csv.ColumnMapping
import com.banyg.domain.csv.DuplicateStatus
import com.banyg.domain.model.Account
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import java.time.LocalDate

/**
 * CSV Import screen UI state.
 */
sealed interface CsvImportUiState {
    data object Initial : CsvImportUiState
    data object Loading : CsvImportUiState

    data class ColumnMapping(
        val fileName: String,
        val csvContent: String,
        val accounts: List<Account>,
        val selectedAccountId: String? = null,
        val columnMapping: com.banyg.domain.csv.ColumnMapping? = null,
        val detectedMapping: com.banyg.domain.csv.ColumnMapping? = null,
        val csvPreview: List<List<String>> = emptyList(),
        val isDetecting: Boolean = false,
        val error: String? = null
    ) : CsvImportUiState

    data class Preview(
        val fileName: String,
        val account: Account,
        val transactions: List<ImportTransactionPreview>,
        val newCount: Int,
        val duplicateCount: Int,
        val errorCount: Int,
        val isImporting: Boolean = false
    ) : CsvImportUiState

    data class ImportComplete(
        val importedCount: Int,
        val skippedCount: Int,
        val duplicateCount: Int
    ) : CsvImportUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : CsvImportUiState
}

/**
 * Transaction preview for import.
 */
data class ImportTransactionPreview(
    val id: String,
    val date: LocalDate,
    val amount: Money,
    val merchant: String,
    val rawDescription: String,
    val duplicateStatus: DuplicateStatus,
    val isSelected: Boolean,
    val categoryId: String? = null,
    val rawRowIndex: Int
)

/**
 * CSV Import screen UI events.
 */
sealed interface CsvImportUiEvent {
    data object OnSelectFile : CsvImportUiEvent
    data class OnFileSelected(val fileName: String, val csvContent: String) : CsvImportUiEvent
    data class OnAccountSelected(val accountId: String) : CsvImportUiEvent
    data object OnAutoDetect : CsvImportUiEvent
    data class OnColumnMappingChanged(val mapping: ColumnMapping) : CsvImportUiEvent
    data object OnPreview : CsvImportUiEvent
    data class OnTransactionSelected(val transactionId: String, val isSelected: Boolean) : CsvImportUiEvent
    data class OnSelectAll(val isSelected: Boolean) : CsvImportUiEvent
    data object OnImport : CsvImportUiEvent
    data object OnBack : CsvImportUiEvent
    data object OnRetry : CsvImportUiEvent
    data object OnDismissComplete : CsvImportUiEvent
}

/**
 * One-time events sent from ViewModel to UI.
 */
sealed class CsvImportEvent {
    data class ShowSnackbar(val message: String) : CsvImportEvent()
    data class LaunchFilePicker(val mimeType: String = "text/csv") : CsvImportEvent()
    data object NavigateBack : CsvImportEvent()
}
