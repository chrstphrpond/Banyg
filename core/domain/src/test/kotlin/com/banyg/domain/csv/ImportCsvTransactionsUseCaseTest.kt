package com.banyg.domain.csv

import com.banyg.domain.model.Account
import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.TransactionRepository
import com.banyg.domain.usecase.ImportCsvTransactionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ImportCsvTransactionsUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val useCase = ImportCsvTransactionsUseCase(transactionRepository)

    private val php = Currency.PHP
    private val now = Instant.parse("2026-01-27T00:00:00Z")

    private val testAccount = Account(
        id = "acc-1",
        name = "Checking",
        type = AccountType.CHECKING,
        currency = php,
        openingBalance = Money(0L, php),
        currentBalance = Money(0L, php),
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `generatePreview parses CSV and detects no duplicates when empty`() = runTest {
        val csv = """
            Date,Description,Amount
            2025-01-15,Grocery Store,-50.00
            2025-01-16,Salary,5000.00
        """.trimIndent()

        val mapping = ColumnMapping.SIMPLE

        coEvery { transactionRepository.getTransactionsByAccount("acc-1") } returns emptyList()

        val preview = useCase.generatePreview(csv, mapping, testAccount)

        assertEquals(2, preview.transactions.size)
        assertEquals(2, preview.newCount)
        assertEquals(0, preview.duplicateCount)
        assertEquals(0, preview.errorCount)

        // All should be selected (no duplicates)
        assertTrue(preview.transactions.all { it.isSelected })
        assertTrue(preview.transactions.all { it.duplicateStatus is DuplicateStatus.NEW })
    }

    @Test
    fun `generatePreview detects duplicates from existing transactions`() = runTest {
        val csv = """
            Date,Description,Amount
            2025-01-15,Starbucks,-150.00
            2025-01-16,Grocery Store,-500.00
        """.trimIndent()

        val existingTransaction = Transaction(
            id = "txn-1",
            accountId = "acc-1",
            date = LocalDate.of(2025, 1, 15),
            amount = Money(-15000L, php),
            merchant = "Starbucks",
            memo = null,
            categoryId = null,
            status = TransactionStatus.CLEARED,
            clearedAt = now,
            transferId = null,
            splits = emptyList(),
            createdAt = now,
            updatedAt = now
        )

        coEvery { transactionRepository.getTransactionsByAccount("acc-1") } returns listOf(existingTransaction)

        val preview = useCase.generatePreview(csv, ColumnMapping.SIMPLE, testAccount)

        assertEquals(2, preview.transactions.size)
        assertEquals(1, preview.newCount)
        assertEquals(1, preview.duplicateCount)

        // Duplicate should be unselected by default
        val duplicate = preview.transactions.find { it.merchant == "Starbucks" }!!
        assertFalse(duplicate.isSelected)
        assertTrue(duplicate.duplicateStatus is DuplicateStatus.DUPLICATE)

        // New transaction should be selected
        val newTransaction = preview.transactions.find { it.merchant == "Grocery Store" }!!
        assertTrue(newTransaction.isSelected)
        assertTrue(newTransaction.duplicateStatus is DuplicateStatus.NEW)
    }

    @Test
    fun `importTransactions saves selected transactions`() = runTest {
        val previews = listOf(
            ImportTransactionPreview(
                id = "preview-1",
                date = LocalDate.of(2025, 1, 15),
                amount = Money(-5000L, php),
                merchant = "Starbucks",
                rawDescription = "STARBUCKS #1234",
                duplicateStatus = DuplicateStatus.NEW,
                isSelected = true,
                categoryId = null,
                rawRowIndex = 1
            ),
            ImportTransactionPreview(
                id = "preview-2",
                date = LocalDate.of(2025, 1, 16),
                amount = Money(-10000L, php),
                merchant = "Grocery Store",
                rawDescription = "WHOLE FOODS MARKET",
                duplicateStatus = DuplicateStatus.NEW,
                isSelected = true,
                categoryId = "cat-1",
                rawRowIndex = 2
            )
        )

        val result = useCase.importTransactions("acc-1", php, previews, now)

        assertEquals(2, result.importedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(0, result.duplicateCount)

        coVerify {
            transactionRepository.saveTransactions(match { transactions ->
                transactions.size == 2 &&
                transactions.all { it.accountId == "acc-1" } &&
                transactions.all { it.status == TransactionStatus.CLEARED }
            })
        }
    }

    @Test
    fun `importTransactions skips unselected transactions`() = runTest {
        val previews = listOf(
            ImportTransactionPreview(
                id = "preview-1",
                date = LocalDate.of(2025, 1, 15),
                amount = Money(-5000L, php),
                merchant = "Starbucks",
                rawDescription = "STARBUCKS",
                duplicateStatus = DuplicateStatus.NEW,
                isSelected = true,
                rawRowIndex = 1
            ),
            ImportTransactionPreview(
                id = "preview-2",
                date = LocalDate.of(2025, 1, 16),
                amount = Money(-10000L, php),
                merchant = "Grocery Store",
                rawDescription = "GROCERY",
                duplicateStatus = DuplicateStatus.NEW,
                isSelected = false, // Unselected
                rawRowIndex = 2
            )
        )

        val result = useCase.importTransactions("acc-1", php, previews, now)

        assertEquals(1, result.importedCount)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun `importTransactions counts duplicates in result`() = runTest {
        val previews = listOf(
            ImportTransactionPreview(
                id = "preview-1",
                date = LocalDate.of(2025, 1, 15),
                amount = Money(-5000L, php),
                merchant = "Starbucks",
                rawDescription = "STARBUCKS",
                duplicateStatus = DuplicateStatus.DUPLICATE(1.0f, "txn-1"),
                isSelected = true, // User chose to import anyway
                rawRowIndex = 1
            )
        )

        val result = useCase.importTransactions("acc-1", php, previews, now)

        assertEquals(1, result.importedCount)
        assertEquals(1, result.duplicateCount)
    }

    @Test
    fun `import with skipDuplicates true skips duplicates`() = runTest {
        val csv = """
            Date,Description,Amount
            2025-01-15,Starbucks,-150.00
        """.trimIndent()

        val existingTransaction = Transaction(
            id = "txn-1",
            accountId = "acc-1",
            date = LocalDate.of(2025, 1, 15),
            amount = Money(-15000L, php),
            merchant = "Starbucks",
            memo = null,
            categoryId = null,
            status = TransactionStatus.CLEARED,
            clearedAt = now,
            transferId = null,
            splits = emptyList(),
            createdAt = now,
            updatedAt = now
        )

        coEvery { transactionRepository.getTransactionsByAccount("acc-1") } returns listOf(existingTransaction)

        val result = useCase.import(csv, ColumnMapping.SIMPLE, testAccount, skipDuplicates = true, now = now)

        assertEquals(0, result.importedCount)
        assertEquals(1, result.duplicateCount)
        assertTrue(result.isSuccess)

        // Should not save any transactions
        coVerify(exactly = 0) { transactionRepository.saveTransactions(any()) }
    }

    @Test
    fun `import with skipDuplicates false imports duplicates`() = runTest {
        val csv = """
            Date,Description,Amount
            2025-01-15,Starbucks,-150.00
        """.trimIndent()

        val existingTransaction = Transaction(
            id = "txn-1",
            accountId = "acc-1",
            date = LocalDate.of(2025, 1, 15),
            amount = Money(-15000L, php),
            merchant = "Starbucks",
            memo = null,
            categoryId = null,
            status = TransactionStatus.CLEARED,
            clearedAt = now,
            transferId = null,
            splits = emptyList(),
            createdAt = now,
            updatedAt = now
        )

        coEvery { transactionRepository.getTransactionsByAccount("acc-1") } returns listOf(existingTransaction)

        val result = useCase.import(csv, ColumnMapping.SIMPLE, testAccount, skipDuplicates = false, now = now)

        assertEquals(1, result.importedCount)
        assertEquals(0, result.duplicateCount) // Still counts as imported

        coVerify { transactionRepository.saveTransactions(any()) }
    }

    @Test
    fun `import reports parse errors`() = runTest {
        val csv = """
            Date,Description,Amount
            2025-01-15,Valid,-50.00
            invalid-date,Invalid,100
        """.trimIndent()

        coEvery { transactionRepository.getTransactionsByAccount("acc-1") } returns emptyList()

        val result = useCase.import(csv, ColumnMapping.SIMPLE, testAccount, now = now)

        assertEquals(1, result.importedCount)
        assertEquals(1, result.errorCount)
        assertFalse(result.isSuccess)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `autoDetectAndPreview returns null for unrecognizable format`() = runTest {
        val csv = """
            Some,Random,Data,Columns
            a,b,c,d
            1,2,3,4
        """.trimIndent()

        val result = useCase.autoDetectAndPreview(csv, testAccount)

        assertNull(result)
    }

    @Test
    fun `autoDetectAndPreview returns detected mapping and preview`() = runTest {
        val csv = """
            Date,Description,Amount
            2025-01-15,Grocery Store,-50.00
            2025-01-16,Salary,5000.00
        """.trimIndent()

        coEvery { transactionRepository.getTransactionsByAccount("acc-1") } returns emptyList()

        val result = useCase.autoDetectAndPreview(csv, testAccount)

        assertNotNull(result)
        assertEquals("Date", result!!.first.dateColumn)
        assertEquals(2, result.second.transactions.size)
        assertEquals(0, result.third.size) // No parse errors
    }

    @Test
    fun `getAvailableFormats returns predefined formats`() {
        val formats = useCase.getAvailableFormats()

        assertEquals(4, formats.size)

        val formatNames = formats.map { it.first }
        assertTrue(formatNames.contains("Chase"))
        assertTrue(formatNames.contains("Wells Fargo"))
        assertTrue(formatNames.contains("Bank of America"))
        assertTrue(formatNames.contains("Simple (Date, Description, Amount)"))
    }

    @Test
    fun `import with empty selection returns zero counts`() = runTest {
        val previews = emptyList<ImportTransactionPreview>()

        val result = useCase.importTransactions("acc-1", php, previews, now)

        assertEquals(0, result.importedCount)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `ImportResult summary formats correctly`() {
        val result1 = ImportResult(importedCount = 10)
        assertEquals("Imported: 10", result1.summary())

        val result2 = ImportResult(importedCount = 10, duplicateCount = 2)
        assertEquals("Imported: 10, Duplicates: 2", result2.summary())

        val result3 = ImportResult(importedCount = 10, skippedCount = 3, duplicateCount = 2)
        assertEquals("Imported: 10, Duplicates: 2, Skipped: 3", result3.summary())

        val result4 = ImportResult(importedCount = 10, errorCount = 1)
        assertEquals("Imported: 10, Errors: 1", result4.summary())
    }
}
