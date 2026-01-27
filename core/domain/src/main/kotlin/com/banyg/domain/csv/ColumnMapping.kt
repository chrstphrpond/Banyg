package com.banyg.domain.csv

/**
 * Column mapping configuration for CSV import
 *
 * Maps CSV column names to transaction fields.
 * Supports both single amount column and separate debit/credit columns.
 *
 * @property dateColumn Name of the date column in CSV
 * @property amountColumn Name of the amount column (used when debit/credit not specified)
 * @property descriptionColumn Name of the description/merchant column
 * @property debitColumn Optional: name of debit column (expenses)
 * @property creditColumn Optional: name of credit column (income)
 * @property dateFormat Date format pattern (e.g., "yyyy-MM-dd", "MM/dd/yyyy")
 * @property delimiter CSV delimiter character (default: comma)
 * @property hasHeader Whether CSV has header row (default: true)
 */
data class ColumnMapping(
    val dateColumn: String,
    val amountColumn: String? = null,
    val descriptionColumn: String,
    val debitColumn: String? = null,
    val creditColumn: String? = null,
    val dateFormat: String = "yyyy-MM-dd",
    val delimiter: Char = ',',
    val hasHeader: Boolean = true
) {
    init {
        require(dateColumn.isNotBlank()) { "Date column cannot be blank" }
        require(descriptionColumn.isNotBlank()) { "Description column cannot be blank" }
        require(amountColumn != null || (debitColumn != null && creditColumn != null)) {
            "Must specify either amountColumn OR both debitColumn and creditColumn"
        }
    }

    /**
     * True if using separate debit/credit columns
     */
    val usesDebitCreditColumns: Boolean
        get() = debitColumn != null && creditColumn != null

    companion object {
        /**
         * Pre-configured mapping for Chase bank format
         */
        val CHASE = ColumnMapping(
            dateColumn = "Transaction Date",
            amountColumn = "Amount",
            descriptionColumn = "Description",
            dateFormat = "MM/dd/yyyy"
        )

        /**
         * Pre-configured mapping for Wells Fargo format
         */
        val WELLS_FARGO = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description",
            dateFormat = "MM/dd/yyyy"
        )

        /**
         * Pre-configured mapping for Bank of America format (debit/credit columns)
         */
        val BANK_OF_AMERICA = ColumnMapping(
            dateColumn = "Date",
            descriptionColumn = "Description",
            debitColumn = "Debit",
            creditColumn = "Credit",
            dateFormat = "MM/dd/yyyy"
        )

        /**
         * Simple format: Date, Description, Amount
         */
        val SIMPLE = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )
    }
}
