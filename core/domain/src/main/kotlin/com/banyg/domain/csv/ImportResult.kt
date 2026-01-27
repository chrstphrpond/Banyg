package com.banyg.domain.csv

/**
 * Result of a CSV import operation
 *
 * @property importedCount Number of transactions successfully imported
 * @property skippedCount Number of transactions skipped (user unchecked)
 * @property duplicateCount Number of transactions that were duplicates
 * @property errorCount Number of rows that failed to parse
 * @property errors List of specific errors encountered
 */
data class ImportResult(
    val importedCount: Int,
    val skippedCount: Int = 0,
    val duplicateCount: Int = 0,
    val errorCount: Int = 0,
    val errors: List<ImportError> = emptyList()
) {
    /**
     * Total number of rows processed
     */
    val totalRows: Int
        get() = importedCount + skippedCount + duplicateCount + errorCount

    /**
     * True if import was completely successful (no errors)
     */
    val isSuccess: Boolean
        get() = errorCount == 0

    /**
     * Summary message for display
     */
    fun summary(): String {
        return buildString {
            append("Imported: $importedCount")
            if (duplicateCount > 0) append(", Duplicates: $duplicateCount")
            if (skippedCount > 0) append(", Skipped: $skippedCount")
            if (errorCount > 0) append(", Errors: $errorCount")
        }
    }
}

/**
 * Import error details
 *
 * @property rowIndex Row index in CSV where error occurred
 * @property message Human-readable error message
 * @property rawData Optional raw data that caused the error
 */
data class ImportError(
    val rowIndex: Int,
    val message: String,
    val rawData: String? = null
)

/**
 * Preview of import before committing
 *
 * @property transactions List of parsed transactions with duplicate status
 * @property newCount Number of new (non-duplicate) transactions
 * @property duplicateCount Number of potential duplicates
 * @property errorCount Number of parse errors
 */
data class ImportPreview(
    val transactions: List<ImportTransactionPreview>,
    val newCount: Int,
    val duplicateCount: Int,
    val errorCount: Int = 0
) {
    /**
     * Transactions that are selected for import
     */
    val selectedTransactions: List<ImportTransactionPreview>
        get() = transactions.filter { it.isSelected }

    /**
     * Transactions marked as duplicates
     */
    val duplicateTransactions: List<ImportTransactionPreview>
        get() = transactions.filter { it.duplicateStatus is DuplicateStatus.DUPLICATE }
}

/**
 * Transaction preview item with import metadata
 *
 * @property id Unique identifier
 * @property date Transaction date
 * @property amount Money amount
 * @property merchant Normalized merchant name
 * @property rawDescription Original description
 * @property duplicateStatus Duplicate detection result
 * @property isSelected Whether user selected this for import
 * @property categoryId Optional category assignment
 */
data class ImportTransactionPreview(
    val id: String,
    val date: java.time.LocalDate,
    val amount: com.banyg.domain.model.Money,
    val merchant: String,
    val rawDescription: String,
    val duplicateStatus: DuplicateStatus = DuplicateStatus.NEW,
    val isSelected: Boolean = true,
    val categoryId: String? = null,
    val rawRowIndex: Int
)

/**
 * Duplicate detection status
 */
sealed interface DuplicateStatus {
    /**
     * New transaction (not a duplicate)
     */
    data object NEW : DuplicateStatus

    /**
     * Potential duplicate detected
     *
     * @property confidence Match confidence (0.0 - 1.0)
     * @property existingTransactionId ID of matching existing transaction
     */
    data class DUPLICATE(
        val confidence: Float,
        val existingTransactionId: String
    ) : DuplicateStatus
}
