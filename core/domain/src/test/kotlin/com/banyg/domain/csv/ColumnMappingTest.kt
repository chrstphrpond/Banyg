package com.banyg.domain.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColumnMappingTest {

    @Test
    fun `valid mapping with amount column`() {
        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        assertEquals("Date", mapping.dateColumn)
        assertEquals("Amount", mapping.amountColumn)
        assertEquals("Description", mapping.descriptionColumn)
        assertFalse(mapping.usesDebitCreditColumns)
    }

    @Test
    fun `valid mapping with debit credit columns`() {
        val mapping = ColumnMapping(
            dateColumn = "Date",
            descriptionColumn = "Description",
            debitColumn = "Debit",
            creditColumn = "Credit"
        )

        assertNull(mapping.amountColumn)
        assertEquals("Debit", mapping.debitColumn)
        assertEquals("Credit", mapping.creditColumn)
        assertTrue(mapping.usesDebitCreditColumns)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty date column throws exception`() {
        ColumnMapping(
            dateColumn = "",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty description column throws exception`() {
        ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = ""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing amount and debit credit columns throws exception`() {
        ColumnMapping(
            dateColumn = "Date",
            amountColumn = null,
            descriptionColumn = "Description",
            debitColumn = null,
            creditColumn = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `only debit column without credit throws exception`() {
        ColumnMapping(
            dateColumn = "Date",
            amountColumn = null,
            descriptionColumn = "Description",
            debitColumn = "Debit",
            creditColumn = null
        )
    }

    @Test
    fun `chase preset has correct values`() {
        val mapping = ColumnMapping.CHASE

        assertEquals("Transaction Date", mapping.dateColumn)
        assertEquals("Amount", mapping.amountColumn)
        assertEquals("Description", mapping.descriptionColumn)
        assertEquals("MM/dd/yyyy", mapping.dateFormat)
    }

    @Test
    fun `wells fargo preset has correct values`() {
        val mapping = ColumnMapping.WELLS_FARGO

        assertEquals("Date", mapping.dateColumn)
        assertEquals("Amount", mapping.amountColumn)
        assertEquals("Description", mapping.descriptionColumn)
        assertEquals("MM/dd/yyyy", mapping.dateFormat)
    }

    @Test
    fun `bank of america preset uses debit credit columns`() {
        val mapping = ColumnMapping.BANK_OF_AMERICA

        assertEquals("Date", mapping.dateColumn)
        assertNull(mapping.amountColumn)
        assertEquals("Description", mapping.descriptionColumn)
        assertEquals("Debit", mapping.debitColumn)
        assertEquals("Credit", mapping.creditColumn)
        assertTrue(mapping.usesDebitCreditColumns)
        assertEquals("MM/dd/yyyy", mapping.dateFormat)
    }

    @Test
    fun `simple preset uses default date format`() {
        val mapping = ColumnMapping.SIMPLE

        assertEquals("Date", mapping.dateColumn)
        assertEquals("Amount", mapping.amountColumn)
        assertEquals("Description", mapping.descriptionColumn)
        assertEquals("yyyy-MM-dd", mapping.dateFormat)
    }

    @Test
    fun `custom delimiter and header settings`() {
        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description",
            delimiter = ';',
            hasHeader = false
        )

        assertEquals(';', mapping.delimiter)
        assertFalse(mapping.hasHeader)
    }

    @Test
    fun `default delimiter is comma`() {
        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        assertEquals(',', mapping.delimiter)
        assertTrue(mapping.hasHeader)
    }
}
