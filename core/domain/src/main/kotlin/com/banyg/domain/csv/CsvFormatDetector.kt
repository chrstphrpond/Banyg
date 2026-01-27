package com.banyg.domain.csv

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Detects CSV format automatically from content
 */
object CsvFormatDetector {

    private val COMMON_DATE_FORMATS = listOf(
        "yyyy-MM-dd",
        "MM/dd/yyyy",
        "dd/MM/yyyy",
        "yyyy/MM/dd",
        "MM-dd-yyyy",
        "dd-MM-yyyy",
        "yyyyMMdd",
        "dd.MM.yyyy",
        "MM.dd.yyyy"
    )

    private val COMMON_DELIMITERS = listOf(',', ';', '\t', '|')

    /**
     * Detect column mapping from CSV headers
     *
     * @param headers CSV header row
     * @return Detected column mapping or null if unable to detect
     */
    fun detectColumnMapping(headers: List<String>): ColumnMapping? {
        val normalizedHeaders = headers.map { it.trim().lowercase() }

        // Detect date column
        val dateColumn = findColumn(normalizedHeaders, headers, DATE_COLUMN_PATTERNS)
            ?: return null

        // Detect description column
        val descriptionColumn = findColumn(normalizedHeaders, headers, DESCRIPTION_COLUMN_PATTERNS)
            ?: return null

        // Detect amount columns - try debit/credit first, then single amount
        val debitColumn = findColumn(normalizedHeaders, headers, DEBIT_COLUMN_PATTERNS)
        val creditColumn = findColumn(normalizedHeaders, headers, CREDIT_COLUMN_PATTERNS)
        val amountColumn = findColumn(normalizedHeaders, headers, AMOUNT_COLUMN_PATTERNS)

        // Must have either amount or both debit/credit
        val finalAmountColumn = when {
            debitColumn != null && creditColumn != null -> null
            amountColumn != null -> amountColumn
            else -> debitColumn ?: creditColumn ?: return null
        }

        return ColumnMapping(
            dateColumn = dateColumn,
            amountColumn = finalAmountColumn,
            descriptionColumn = descriptionColumn,
            debitColumn = if (debitColumn != null && creditColumn != null) debitColumn else null,
            creditColumn = if (debitColumn != null && creditColumn != null) creditColumn else null,
            dateFormat = "yyyy-MM-dd" // Default, will try to detect
        )
    }

    /**
     * Detect date format from sample values
     *
     * @param dateStrings Sample date strings
     * @return Detected date format or default "yyyy-MM-dd"
     */
    fun detectDateFormat(dateStrings: List<String>): String {
        for (format in COMMON_DATE_FORMATS) {
            val formatter = DateTimeFormatter.ofPattern(format)
            val successCount = dateStrings.count { dateStr ->
                try {
                    formatter.parse(dateStr.trim())
                    true
                } catch (e: DateTimeParseException) {
                    false
                }
            }
            // If more than 80% of dates parse successfully, use this format
            if (successCount >= dateStrings.size * 0.8) {
                return format
            }
        }
        return "yyyy-MM-dd" // Default fallback
    }

    /**
     * Detect CSV delimiter from content
     *
     * @param sample First few lines of CSV
     * @return Detected delimiter (defaults to comma)
     */
    fun detectDelimiter(sample: String): Char {
        val lines = sample.lines().take(3) // Check first 3 lines
        if (lines.isEmpty()) return ','

        return COMMON_DELIMITERS.maxByOrNull { delimiter ->
            lines.sumOf { line -> line.count { it == delimiter } }
        } ?: ','
    }

    /**
     * Check if first row appears to be a header
     *
     * @param firstRow First row values
     * @return True if likely a header row
     */
    fun isLikelyHeader(firstRow: List<String>): Boolean {
        if (firstRow.isEmpty()) return false

        // Headers typically contain letters, not just numbers
        val hasLetters = firstRow.any { value ->
            value.any { it.isLetter() }
        }

        // Headers don't look like dates (any value looking like a date suggests data row)
        val hasAnyDate = firstRow.any { value ->
            looksLikeDate(value)
        }

        // Headers don't look like amounts (any value looking like amount suggests data row)
        val hasAnyAmount = firstRow.any { value ->
            looksLikeAmount(value)
        }

        return hasLetters && !hasAnyDate && !hasAnyAmount
    }

    private fun findColumn(
        normalizedHeaders: List<String>,
        originalHeaders: List<String>,
        patterns: List<String>
    ): String? {
        for (pattern in patterns) {
            val index = normalizedHeaders.indexOfFirst { it.matches(Regex(pattern)) }
            if (index >= 0) {
                return originalHeaders[index]
            }
        }
        return null
    }

    private fun looksLikeDate(value: String): Boolean {
        return COMMON_DATE_FORMATS.any { format ->
            try {
                DateTimeFormatter.ofPattern(format).parse(value.trim())
                true
            } catch (e: DateTimeParseException) {
                false
            }
        }
    }

    private fun looksLikeAmount(value: String): Boolean {
        val cleaned = value.trim()
            .replace("$", "")
            .replace(",", "")
            .replace("-", "")
        return cleaned.toDoubleOrNull() != null
    }

    // Column pattern matchers
    private val DATE_COLUMN_PATTERNS = listOf(
        "date",
        "transaction.?date",
        "posting.?date",
        "value.?date",
        "txn.?date"
    )

    private val DESCRIPTION_COLUMN_PATTERNS = listOf(
        "description",
        "desc",
        "narrative",
        "transaction.?description",
        "payee",
        "merchant",
        "name",
        "memo",
        "notes"
    )

    private val AMOUNT_COLUMN_PATTERNS = listOf(
        "amount",
        "transaction.?amount",
        "value",
        "sum",
        "total"
    )

    private val DEBIT_COLUMN_PATTERNS = listOf(
        "debit",
        "debit.?amount",
        "withdrawal",
        "outflow",
        "expense",
        "money.?out",
        "payment"
    )

    private val CREDIT_COLUMN_PATTERNS = listOf(
        "credit",
        "credit.?amount",
        "deposit",
        "inflow",
        "income",
        "money.?in",
        "received"
    )
}
