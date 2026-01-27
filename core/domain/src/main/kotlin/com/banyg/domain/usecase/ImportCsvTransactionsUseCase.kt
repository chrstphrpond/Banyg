package com.banyg.domain.usecase

import com.banyg.domain.csv.AutoParseResult
import com.banyg.domain.csv.ColumnMapping
import com.banyg.domain.csv.CsvTransactionParser
import com.banyg.domain.csv.DuplicateDetector
import com.banyg.domain.csv.DuplicateStatus
import com.banyg.domain.csv.ImportError
import com.banyg.domain.csv.ImportPreview
import com.banyg.domain.csv.ImportResult
import com.banyg.domain.csv.ImportTransactionPreview
import com.banyg.domain.csv.ParsedTransaction
import com.banyg.domain.model.Account
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Use case for importing transactions from CSV files
 *
 * Supports:
 * - Column mapping configuration
 * - Auto-detection of CSV format
 * - Duplicate detection
 * - Preview before import
 * - Batch import with error handling
 */
class ImportCsvTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    private val csvParser = CsvTransactionParser()
    private val duplicateDetector = DuplicateDetector()

    /**
     * Parse CSV and generate import preview with duplicate detection
     *
     * @param csvContent Raw CSV content
     * @param mapping Column mapping configuration
     * @param account Account to import into (for currency and existing transactions)
     * @return Import preview with transactions and duplicate status
     */
    suspend fun generatePreview(
        csvContent: String,
        mapping: ColumnMapping,
        account: Account
    ): ImportPreview {
        val (transactions, errors) = csvParser.parseWithErrors(
            csvContent,
            mapping,
            account.currency
        )

        val existingTransactions = transactionRepository.getTransactionsByAccount(account.id)

        // Check for duplicates
        val checkedTransactions = transactions.map { parsed ->
            val duplicateCheck = duplicateDetector.checkDuplicate(parsed, existingTransactions)

            ImportTransactionPreview(
                id = parsed.id,
                date = parsed.date,
                amount = parsed.amount,
                merchant = parsed.merchant,
                rawDescription = parsed.rawDescription,
                duplicateStatus = if (duplicateCheck.isDuplicate) {
                    DuplicateStatus.DUPLICATE(
                        confidence = duplicateCheck.confidence,
                        existingTransactionId = duplicateCheck.matchedTransactionId!!
                    )
                } else {
                    DuplicateStatus.NEW
                },
                isSelected = !duplicateCheck.isDuplicate, // Uncheck duplicates by default
                categoryId = null,
                rawRowIndex = parsed.rawRowIndex
            )
        }

        val newCount = checkedTransactions.count { it.duplicateStatus is DuplicateStatus.NEW }
        val duplicateCount = checkedTransactions.count { it.duplicateStatus is DuplicateStatus.DUPLICATE }

        return ImportPreview(
            transactions = checkedTransactions,
            newCount = newCount,
            duplicateCount = duplicateCount,
            errorCount = errors.size
        )
    }

    /**
     * Auto-detect CSV format and generate preview
     *
     * @param csvContent Raw CSV content
     * @param account Account to import into
     * @return Triple of (detected mapping, import preview, parse errors) or null if auto-detection failed
     */
    suspend fun autoDetectAndPreview(
        csvContent: String,
        account: Account
    ): Triple<ColumnMapping, ImportPreview, List<ImportError>>? {
        val autoResult = csvParser.parseAutoDetect(csvContent, account.currency)
            ?: return null

        val existingTransactions = transactionRepository.getTransactionsByAccount(account.id)

        val checkedTransactions = autoResult.transactions.map { parsed ->
            val duplicateCheck = duplicateDetector.checkDuplicate(parsed, existingTransactions)

            ImportTransactionPreview(
                id = parsed.id,
                date = parsed.date,
                amount = parsed.amount,
                merchant = parsed.merchant,
                rawDescription = parsed.rawDescription,
                duplicateStatus = if (duplicateCheck.isDuplicate) {
                    DuplicateStatus.DUPLICATE(
                        confidence = duplicateCheck.confidence,
                        existingTransactionId = duplicateCheck.matchedTransactionId!!
                    )
                } else {
                    DuplicateStatus.NEW
                },
                isSelected = !duplicateCheck.isDuplicate,
                categoryId = null,
                rawRowIndex = parsed.rawRowIndex
            )
        }

        val newCount = checkedTransactions.count { it.duplicateStatus is DuplicateStatus.NEW }
        val duplicateCount = checkedTransactions.count { it.duplicateStatus is DuplicateStatus.DUPLICATE }

        val preview = ImportPreview(
            transactions = checkedTransactions,
            newCount = newCount,
            duplicateCount = duplicateCount,
            errorCount = autoResult.errors.size
        )

        return Triple(autoResult.mapping, preview, autoResult.errors)
    }

    /**
     * Import selected transactions from preview
     *
     * @param accountId Account ID to import into
     * @param currency Currency for transactions
     * @param previews List of transaction previews (filtered to only selected)
     * @param now Current timestamp
     * @return Import result with counts
     */
    suspend fun importTransactions(
        accountId: String,
        currency: Currency,
        previews: List<ImportTransactionPreview>,
        now: Instant = Instant.now()
    ): ImportResult {
        val selectedPreviews = previews.filter { it.isSelected }

        if (selectedPreviews.isEmpty()) {
            return ImportResult(importedCount = 0, skippedCount = previews.size)
        }

        val transactions = selectedPreviews.map { preview ->
            Transaction(
                id = UUID.randomUUID().toString(),
                accountId = accountId,
                date = preview.date,
                amount = preview.amount,
                merchant = preview.merchant,
                memo = preview.rawDescription.takeIf { it != preview.merchant },
                categoryId = preview.categoryId,
                status = TransactionStatus.CLEARED,
                clearedAt = now,
                transferId = null,
                splits = emptyList(),
                createdAt = now,
                updatedAt = now
            )
        }

        transactionRepository.saveTransactions(transactions)

        val duplicateCount = selectedPreviews.count {
            it.duplicateStatus is DuplicateStatus.DUPLICATE
        }

        return ImportResult(
            importedCount = transactions.size,
            skippedCount = previews.size - selectedPreviews.size,
            duplicateCount = duplicateCount
        )
    }

    /**
     * Full import workflow: parse, detect duplicates, and import
     *
     * @param csvContent Raw CSV content
     * @param mapping Column mapping configuration
     * @param account Account to import into
     * @param skipDuplicates Whether to skip duplicate transactions (default: true)
     * @return Import result
     */
    suspend fun import(
        csvContent: String,
        mapping: ColumnMapping,
        account: Account,
        skipDuplicates: Boolean = true,
        now: Instant = Instant.now()
    ): ImportResult {
        val (parsedTransactions, parseErrors) = csvParser.parseWithErrors(
            csvContent,
            mapping,
            account.currency
        )

        val existingTransactions = transactionRepository.getTransactionsByAccount(account.id)

        // Filter out duplicates if requested
        val transactionsToImport = if (skipDuplicates) {
            parsedTransactions.filter { parsed ->
                val check = duplicateDetector.checkDuplicate(parsed, existingTransactions)
                !check.isDuplicate
            }
        } else {
            parsedTransactions
        }

        val duplicateCount = parsedTransactions.size - transactionsToImport.size

        // Create domain transactions
        val domainTransactions = transactionsToImport.map { parsed ->
            Transaction(
                id = UUID.randomUUID().toString(),
                accountId = account.id,
                date = parsed.date,
                amount = parsed.amount,
                merchant = parsed.merchant,
                memo = parsed.rawDescription.takeIf { it != parsed.merchant },
                categoryId = null,
                status = TransactionStatus.CLEARED,
                clearedAt = now,
                transferId = null,
                splits = emptyList(),
                createdAt = now,
                updatedAt = now
            )
        }

        transactionRepository.saveTransactions(domainTransactions)

        return ImportResult(
            importedCount = domainTransactions.size,
            skippedCount = 0,
            duplicateCount = duplicateCount,
            errorCount = parseErrors.size,
            errors = parseErrors
        )
    }

    /**
     * Get available pre-configured bank formats
     */
    fun getAvailableFormats(): List<Pair<String, ColumnMapping>> {
        return listOf(
            "Chase" to ColumnMapping.CHASE,
            "Wells Fargo" to ColumnMapping.WELLS_FARGO,
            "Bank of America" to ColumnMapping.BANK_OF_AMERICA,
            "Simple (Date, Description, Amount)" to ColumnMapping.SIMPLE
        )
    }
}
