package com.banyg.domain.csv

import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DuplicateDetectorTest {

    private val detector = DuplicateDetector()
    private val php = Currency.PHP
    private val now = Instant.now()

    private fun createParsedTransaction(
        date: LocalDate,
        amountMinor: Long,
        merchant: String,
        description: String = merchant
    ): ParsedTransaction {
        return ParsedTransaction.fromMinorUnits(
            date = date,
            amountMinor = amountMinor,
            currency = php,
            merchant = merchant,
            rawDescription = description,
            rawRowIndex = 1
        )
    }

    private fun createExistingTransaction(
        id: String,
        date: LocalDate,
        amountMinor: Long,
        merchant: String
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "acc-1",
            date = date,
            amount = Money(amountMinor, php),
            merchant = merchant,
            memo = null,
            categoryId = null,
            status = TransactionStatus.CLEARED,
            clearedAt = now,
            transferId = null,
            splits = emptyList(),
            createdAt = now,
            updatedAt = now
        )
    }

    @Test
    fun `exact match returns duplicate with full confidence`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val existing = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val result = detector.checkDuplicate(parsed, listOf(existing))

        assertTrue(result.isDuplicate)
        assertEquals(1.0f, result.confidence, 0.01f)
        assertEquals("txn-1", result.matchedTransactionId)
    }

    @Test
    fun `case insensitive merchant match`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "STARBUCKS"
        )

        val existing = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "starbucks"
        )

        val result = detector.checkDuplicate(parsed, listOf(existing))

        assertTrue(result.isDuplicate)
        assertEquals(1.0f, result.confidence, 0.01f)
    }

    @Test
    fun `different date is not exact match but may be fuzzy`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val existing = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 16), // One day off
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val result = detector.checkDuplicate(parsed, listOf(existing))

        // Should be detected as duplicate with high confidence
        assertTrue(result.isDuplicate)
        assertTrue(result.confidence > 0.8f)
    }

    @Test
    fun `different amount is not duplicate`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val existing = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5500L, // Different amount
            merchant = "Starbucks"
        )

        val result = detector.checkDuplicate(parsed, listOf(existing))

        // Same date and merchant but different amount - low confidence
        assertFalse(result.isDuplicate)
    }

    @Test
    fun `different merchant with same date and amount is potential duplicate`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val existing = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Coffee Bean" // Different merchant
        )

        val result = detector.checkDuplicate(parsed, listOf(existing))

        // Same date (0.4) + same amount (0.4) = 0.8, which exceeds threshold
        // Even with different merchants, this is flagged as potential duplicate
        assertTrue(result.isDuplicate)
    }

    @Test
    fun `check multiple transactions returns correct results`() {
        val parsed1 = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val parsed2 = createParsedTransaction(
            date = LocalDate.of(2025, 1, 16),
            amountMinor = -10000L,
            merchant = "Grocery Store"
        )

        val existing = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val results = detector.checkDuplicates(listOf(parsed1, parsed2), listOf(existing))

        assertEquals(2, results.size)
        assertTrue(results[0].second.isDuplicate) // First is duplicate
        assertFalse(results[1].second.isDuplicate) // Second is new
    }

    @Test
    fun `find internal duplicates within parsed batch`() {
        val parsed1 = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val parsed2 = createParsedTransaction(
            date = LocalDate.of(2025, 1, 16),
            amountMinor = -10000L,
            merchant = "Grocery Store"
        )

        val parsed3 = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks" // Same as parsed1
        )

        val duplicates = detector.findInternalDuplicates(listOf(parsed1, parsed2, parsed3))

        assertEquals(1, duplicates.size)
        assertTrue(duplicates.contains(parsed3.id))
    }

    @Test
    fun `no internal duplicates returns empty set`() {
        val parsed1 = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val parsed2 = createParsedTransaction(
            date = LocalDate.of(2025, 1, 16),
            amountMinor = -10000L,
            merchant = "Grocery Store"
        )

        val duplicates = detector.findInternalDuplicates(listOf(parsed1, parsed2))

        assertTrue(duplicates.isEmpty())
    }

    @Test
    fun `fuzzy match with similar merchant name`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks Coffee"
        )

        val existing = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks" // Similar but not exact
        )

        val result = detector.checkDuplicate(parsed, listOf(existing))

        // Same date and amount, similar merchant should match
        assertTrue(result.isDuplicate)
        assertTrue(result.confidence > 0.8f)
    }

    @Test
    fun `date within tolerance days with same amount and merchant is potential duplicate`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val existing = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 16), // 1 day difference
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val result = detector.checkDuplicate(parsed, listOf(existing))

        // 1 day diff (0.27) + same amount (0.4) + same merchant (0.2) = 0.87 > 0.75
        assertTrue(result.isDuplicate)
        assertTrue(result.confidence > 0.75f)
    }

    @Test
    fun `date outside tolerance is not fuzzy match`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val existing = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 20), // 5 days difference
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val result = detector.checkDuplicate(parsed, listOf(existing))

        // Outside tolerance, different date matters
        assertFalse(result.isDuplicate)
    }

    @Test
    fun `empty existing transactions returns no duplicate`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val result = detector.checkDuplicate(parsed, emptyList())

        assertFalse(result.isDuplicate)
        assertEquals(0f, result.confidence, 0.01f)
        assertNull(result.matchedTransactionId)
    }

    @Test
    fun `check multiple existing transactions finds best match`() {
        val parsed = createParsedTransaction(
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val existing1 = createExistingTransaction(
            id = "txn-1",
            date = LocalDate.of(2025, 1, 10),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val existing2 = createExistingTransaction(
            id = "txn-2",
            date = LocalDate.of(2025, 1, 15),
            amountMinor = -5000L,
            merchant = "Starbucks"
        )

        val result = detector.checkDuplicate(parsed, listOf(existing1, existing2))

        assertTrue(result.isDuplicate)
        assertEquals("txn-2", result.matchedTransactionId) // Should match the exact one
        assertEquals(1.0f, result.confidence, 0.01f)
    }
}
