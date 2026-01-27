package com.banyg.domain.csv

import com.banyg.domain.model.Currency
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.StringReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Parses CSV content into transactions
 */
class CsvTransactionParser {

    /**
     * Parse CSV content with specified column mapping
     *
     * @param csvContent Raw CSV content
     * @param mapping Column mapping configuration
     * @param currency Currency for all transactions (from account)
     * @return List of successfully parsed transactions
     */
    fun parse(
        csvContent: String,
        mapping: ColumnMapping,
        currency: Currency
    ): List<ParsedTransaction> {
        val format = buildCsvFormat(mapping)

        return StringReader(csvContent).use { reader ->
            CSVParser.parse(reader, format).use { csvParser ->
                csvParser.records.mapIndexedNotNull { index, record ->
                    parseRow(record, mapping, currency, index + 1)
                }
            }
        }
    }

    /**
     * Parse CSV with error reporting
     *
     * @param csvContent Raw CSV content
     * @param mapping Column mapping configuration
     * @param currency Currency for all transactions
     * @return Pair of (successful transactions, import errors)
     */
    fun parseWithErrors(
        csvContent: String,
        mapping: ColumnMapping,
        currency: Currency
    ): Pair<List<ParsedTransaction>, List<ImportError>> {
        val format = buildCsvFormat(mapping)
        val transactions = mutableListOf<ParsedTransaction>()
        val errors = mutableListOf<ImportError>()

        StringReader(csvContent).use { reader ->
            CSVParser.parse(reader, format).use { parser ->
                parser.records.forEachIndexed { index: Int, record: CSVRecord ->
                    val rowIndex = index + 1
                    try {
                        parseRowThrowing(record, mapping, currency, rowIndex)?.let { transactions.add(it) }
                    } catch (e: Exception) {
                        errors.add(
                            ImportError(
                                rowIndex = rowIndex,
                                message = e.message ?: "Unknown parsing error",
                                rawData = record.toMap().toString()
                            )
                        )
                    }
                }
            }
        }

        return transactions to errors
    }

    /**
     * Auto-detect and parse CSV
     *
     * @param csvContent Raw CSV content
     * @param currency Currency for all transactions
     * @return Parse result with detected mapping
     */
    fun parseAutoDetect(
        csvContent: String,
        currency: Currency
    ): AutoParseResult? {
        val delimiter = CsvFormatDetector.detectDelimiter(csvContent)
        val lines = csvContent.lines()
        if (lines.isEmpty()) return null

        // Parse first line to get headers
        val firstLineFormat = CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter)
            .build()

        val headers = StringReader(lines[0]).use { reader ->
            CSVParser.parse(reader, firstLineFormat).use { parser ->
                parser.records.firstOrNull()?.toList()
            }
        } ?: return null

        // Detect column mapping
        val mapping = CsvFormatDetector.detectColumnMapping(headers) ?: return null

        // Detect date format from sample
        val sampleSize = minOf(lines.size - 1, 5)
        val sampleDates = (1..sampleSize).mapNotNull { i ->
            val line = lines.getOrNull(i) ?: return@mapNotNull null
            val record = StringReader(line).use { reader ->
                CSVParser.parse(reader, firstLineFormat).use { parser ->
                    parser.records.firstOrNull()
                }
            } ?: return@mapNotNull null

            val dateIndex = headers.indexOf(mapping.dateColumn)
            if (dateIndex >= 0 && dateIndex < record.size()) {
                record.get(dateIndex)
            } else null
        }

        val detectedDateFormat = if (sampleDates.isNotEmpty()) {
            CsvFormatDetector.detectDateFormat(sampleDates)
        } else {
            mapping.dateFormat
        }

        val finalMapping = mapping.copy(dateFormat = detectedDateFormat, delimiter = delimiter)
        val (transactions, errors) = parseWithErrors(csvContent, finalMapping, currency)

        return AutoParseResult(
            mapping = finalMapping,
            transactions = transactions,
            errors = errors
        )
    }

    private fun buildCsvFormat(mapping: ColumnMapping): CSVFormat {
        val builder = CSVFormat.DEFAULT.builder()
            .setDelimiter(mapping.delimiter)
            .setIgnoreEmptyLines(true)
            .setTrim(true)

        if (mapping.hasHeader) {
            builder.setHeader().setSkipHeaderRecord(true)
        }

        return builder.build()
    }

    private fun parseRow(
        record: CSVRecord,
        mapping: ColumnMapping,
        currency: Currency,
        rowIndex: Int
    ): ParsedTransaction? {
        return try {
            val date = parseDate(record.get(mapping.dateColumn), mapping.dateFormat)

            val amountMinor = if (mapping.usesDebitCreditColumns) {
                parseDebitCredit(record, mapping)
            } else {
                val amountValue = record.get(mapping.amountColumn!!)
                parseAmountToMinorUnits(amountValue)
            }

            // Skip zero amounts
            if (amountMinor == 0L) return null

            val rawDescription = record.get(mapping.descriptionColumn)
            val merchant = normalizeMerchant(rawDescription)

            ParsedTransaction.fromMinorUnits(
                date = date,
                amountMinor = amountMinor,
                currency = currency,
                merchant = merchant,
                rawDescription = rawDescription,
                rawRowIndex = rowIndex
            )
        } catch (e: Exception) {
            null // Skip malformed rows
        }
    }

    /**
     * Parse a single row, throwing exceptions for errors (used by parseWithErrors)
     */
    private fun parseRowThrowing(
        record: CSVRecord,
        mapping: ColumnMapping,
        currency: Currency,
        rowIndex: Int
    ): ParsedTransaction? {
        val date = parseDate(record.get(mapping.dateColumn), mapping.dateFormat)

        val amountMinor = if (mapping.usesDebitCreditColumns) {
            parseDebitCredit(record, mapping)
        } else {
            val amountValue = record.get(mapping.amountColumn!!)
            parseAmountToMinorUnits(amountValue)
        }

        // Skip zero amounts (not an error, just skip)
        if (amountMinor == 0L) return null

        val rawDescription = record.get(mapping.descriptionColumn)
        val merchant = normalizeMerchant(rawDescription)

        return ParsedTransaction.fromMinorUnits(
            date = date,
            amountMinor = amountMinor,
            currency = currency,
            merchant = merchant,
            rawDescription = rawDescription,
            rawRowIndex = rowIndex
        )
    }

    private fun parseDate(dateStr: String, format: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern(format)
        return try {
            LocalDate.parse(dateStr.trim(), formatter)
        } catch (e: DateTimeParseException) {
            // Try ISO format as fallback
            LocalDate.parse(dateStr.trim())
        }
    }

    /**
     * Parse amount string to minor units (Long)
     *
     * Handles:
     * - Currency symbols ($, ₱, €, £)
     * - Thousands separators (,)
     * - Negative signs (front or parentheses)
     * - Decimal points
     */
    internal fun parseAmountToMinorUnits(amountStr: String): Long {
        val cleaned = amountStr.trim()
            .replace("$", "")
            .replace("₱", "")
            .replace("€", "")
            .replace("£", "")
            .replace(",", "")
            .replace(" ", "")

        // Handle parentheses for negative: (100.00) -> -100.00
        val isNegative = cleaned.startsWith("(") && cleaned.endsWith(")")
        val numericPart = cleaned
            .replace("(", "")
            .replace(")", "")
            .replace("+", "")

        // Parse using BigDecimal for precision, then convert to Long
        val amount = BigDecimal(numericPart)
        val minorUnits = amount.movePointRight(2).setScale(0).toLong()

        return if (isNegative) -minorUnits else minorUnits
    }

    private fun parseDebitCredit(record: CSVRecord, mapping: ColumnMapping): Long {
        val debitStr = mapping.debitColumn?.let { column ->
            try {
                record.get(column)?.trim()
            } catch (e: IllegalArgumentException) {
                null
            }
        } ?: ""

        val creditStr = mapping.creditColumn?.let { column ->
            try {
                record.get(column)?.trim()
            } catch (e: IllegalArgumentException) {
                null
            }
        } ?: ""

        return when {
            debitStr.isNotEmpty() && debitStr != "0" && debitStr != "0.00" -> {
                // Debit is expense (negative)
                -parseAmountToMinorUnits(debitStr)
            }
            creditStr.isNotEmpty() && creditStr != "0" && creditStr != "0.00" -> {
                // Credit is income (positive)
                parseAmountToMinorUnits(creditStr)
            }
            else -> 0L
        }
    }

    /**
     * Normalize merchant name for consistent display and duplicate detection
     */
    internal fun normalizeMerchant(description: String): String {
        return description
            .trim()
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .replace(Regex("\\*+"), "") // Remove asterisks
            .replace(Regex("\\s+#\\s*\\d+"), "") // Remove "#123" location codes first
            .replace(Regex("\\d{4,}"), "") // Remove long numbers (card digits)
            .replace(Regex("\\s+\\d+\\s*$"), "") // Remove trailing numbers
            .trim()
            .titleCase()
    }

    /**
     * Convert string to Title Case
     */
    private fun String.titleCase(): String {
        return this.split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
}

/**
 * Result of auto-detection parsing
 */
data class AutoParseResult(
    val mapping: ColumnMapping,
    val transactions: List<ParsedTransaction>,
    val errors: List<ImportError>
)
