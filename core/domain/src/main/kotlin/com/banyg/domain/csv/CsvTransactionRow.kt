package com.banyg.domain.csv

/**
 * Raw data from a CSV row before parsing
 *
 * @property rowIndex Original row index in CSV (for error reporting)
 * @property columns Map of column name to raw value
 */
data class CsvTransactionRow(
    val rowIndex: Int,
    val columns: Map<String, String>
) {
    /**
     * Get value for column, returns null if column doesn't exist
     */
    fun get(columnName: String): String? {
        return columns[columnName]?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Get value for column or empty string
     */
    fun getOrEmpty(columnName: String): String {
        return get(columnName) ?: ""
    }
}
