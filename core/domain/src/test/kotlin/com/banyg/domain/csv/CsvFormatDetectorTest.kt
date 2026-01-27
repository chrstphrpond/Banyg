package com.banyg.domain.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvFormatDetectorTest {

    @Test
    fun `detectColumnMapping finds simple format`() {
        val headers = listOf("Date", "Description", "Amount")

        val mapping = CsvFormatDetector.detectColumnMapping(headers)

        assertNotNull(mapping)
        assertEquals("Date", mapping!!.dateColumn)
        assertEquals("Description", mapping.descriptionColumn)
        assertEquals("Amount", mapping.amountColumn)
        assertNull(mapping.debitColumn)
        assertNull(mapping.creditColumn)
    }

    @Test
    fun `detectColumnMapping finds debit credit format`() {
        val headers = listOf("Date", "Description", "Debit", "Credit")

        val mapping = CsvFormatDetector.detectColumnMapping(headers)

        assertNotNull(mapping)
        assertEquals("Date", mapping!!.dateColumn)
        assertEquals("Description", mapping.descriptionColumn)
        assertNull(mapping.amountColumn)
        assertEquals("Debit", mapping.debitColumn)
        assertEquals("Credit", mapping.creditColumn)
        assertTrue(mapping.usesDebitCreditColumns)
    }

    @Test
    fun `detectColumnMapping handles various date column names`() {
        val dateHeaders = listOf(
            listOf("Date", "Description", "Amount"),
            listOf("Transaction Date", "Description", "Amount"),
            listOf("Posting Date", "Description", "Amount"),
            listOf("Value Date", "Description", "Amount")
        )

        dateHeaders.forEach { headers ->
            val mapping = CsvFormatDetector.detectColumnMapping(headers)
            assertNotNull("Should detect date column in $headers", mapping)
            assertTrue(
                "Date column should contain 'Date'",
                mapping!!.dateColumn.contains("Date", ignoreCase = true)
            )
        }
    }

    @Test
    fun `detectColumnMapping handles various description column names`() {
        val descHeaders = listOf(
            listOf("Date", "Description", "Amount"),
            listOf("Date", "Payee", "Amount"),
            listOf("Date", "Merchant", "Amount"),
            listOf("Date", "Name", "Amount"),
            listOf("Date", "Notes", "Amount")
        )

        descHeaders.forEach { headers ->
            val mapping = CsvFormatDetector.detectColumnMapping(headers)
            assertNotNull("Should detect description column in $headers", mapping)
        }
    }

    @Test
    fun `detectColumnMapping returns null when date column missing`() {
        val headers = listOf("Description", "Amount")

        val mapping = CsvFormatDetector.detectColumnMapping(headers)

        assertNull(mapping)
    }

    @Test
    fun `detectColumnMapping returns null when description column missing`() {
        val headers = listOf("Date", "Amount")

        val mapping = CsvFormatDetector.detectColumnMapping(headers)

        assertNull(mapping)
    }

    @Test
    fun `detectColumnMapping returns null when no amount columns`() {
        val headers = listOf("Date", "Description")

        val mapping = CsvFormatDetector.detectColumnMapping(headers)

        assertNull(mapping)
    }

    @Test
    fun `detectDateFormat finds ISO format`() {
        val sample = listOf("2025-01-15", "2025-01-16", "2025-01-17")

        val format = CsvFormatDetector.detectDateFormat(sample)

        assertEquals("yyyy-MM-dd", format)
    }

    @Test
    fun `detectDateFormat finds US format`() {
        val sample = listOf("01/15/2025", "01/16/2025", "01/17/2025")

        val format = CsvFormatDetector.detectDateFormat(sample)

        assertEquals("MM/dd/yyyy", format)
    }

    @Test
    fun `detectDateFormat finds European format`() {
        val sample = listOf("15/01/2025", "16/01/2025", "17/01/2025")

        val format = CsvFormatDetector.detectDateFormat(sample)

        assertEquals("dd/MM/yyyy", format)
    }

    @Test
    fun `detectDateFormat handles mixed samples with majority`() {
        val sample = listOf("2025-01-15", "2025-01-16", "invalid", "2025-01-17")

        val format = CsvFormatDetector.detectDateFormat(sample)

        // 75% valid ISO dates should match
        assertEquals("yyyy-MM-dd", format)
    }

    @Test
    fun `detectDateFormat returns default for empty list`() {
        val format = CsvFormatDetector.detectDateFormat(emptyList())

        assertEquals("yyyy-MM-dd", format)
    }

    @Test
    fun `detectDelimiter finds comma`() {
        val sample = "Date,Description,Amount\n2025-01-15,Grocery,-50.00"

        val delimiter = CsvFormatDetector.detectDelimiter(sample)

        assertEquals(',', delimiter)
    }

    @Test
    fun `detectDelimiter finds semicolon`() {
        val sample = "Date;Description;Amount\n2025-01-15;Grocery;-50.00"

        val delimiter = CsvFormatDetector.detectDelimiter(sample)

        assertEquals(';', delimiter)
    }

    @Test
    fun `detectDelimiter finds tab`() {
        val sample = "Date\tDescription\tAmount\n2025-01-15\tGrocery\t-50.00"

        val delimiter = CsvFormatDetector.detectDelimiter(sample)

        assertEquals('\t', delimiter)
    }

    @Test
    fun `detectDelimiter defaults to comma for empty sample`() {
        val delimiter = CsvFormatDetector.detectDelimiter("")

        assertEquals(',', delimiter)
    }

    @Test
    fun `isLikelyHeader returns true for header row`() {
        val headerRow = listOf("Date", "Description", "Amount")

        assertTrue(CsvFormatDetector.isLikelyHeader(headerRow))
    }

    @Test
    fun `isLikelyHeader returns true for header with special chars`() {
        val headerRow = listOf("Transaction Date", "Description", "Transaction Amount")

        assertTrue(CsvFormatDetector.isLikelyHeader(headerRow))
    }

    @Test
    fun `isLikelyHeader returns false for data row`() {
        val dataRow = listOf("2025-01-15", "Starbucks", "-5.00")

        assertFalse(CsvFormatDetector.isLikelyHeader(dataRow))
    }

    @Test
    fun `isLikelyHeader returns false for empty row`() {
        assertFalse(CsvFormatDetector.isLikelyHeader(emptyList()))
    }

    @Test
    fun `isLikelyHeader returns false for numeric-only row`() {
        val numericRow = listOf("12345", "67890")

        assertFalse(CsvFormatDetector.isLikelyHeader(numericRow))
    }
}
