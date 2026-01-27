package com.banyg.domain.csv

import com.banyg.domain.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvTransactionParserTest {

    private val parser = CsvTransactionParser()
    private val php = Currency.PHP

    @Test
    fun `parse simple CSV format with signed amounts`() {
        val csv = """
            Date,Description,Amount
            2025-01-15,Grocery Store,-50.00
            2025-01-16,Salary,5000.00
            2025-01-17,Coffee,-5.50
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        val transactions = parser.parse(csv, mapping, php)

        assertEquals(3, transactions.size)

        // First transaction - expense
        with(transactions[0]) {
            assertEquals(LocalDate.of(2025, 1, 15), date)
            assertEquals("Grocery Store", merchant)
            assertEquals(-5000L, amount.minorUnits) // -50.00
            assertTrue(isExpense)
        }

        // Second transaction - income
        with(transactions[1]) {
            assertEquals(LocalDate.of(2025, 1, 16), date)
            assertEquals("Salary", merchant)
            assertEquals(500000L, amount.minorUnits) // 5000.00
            assertTrue(isIncome)
        }

        // Third transaction
        with(transactions[2]) {
            assertEquals(LocalDate.of(2025, 1, 17), date)
            assertEquals("Coffee", merchant)
            assertEquals(-550L, amount.minorUnits) // -5.50
        }
    }

    @Test
    fun `parse CSV with debit and credit columns`() {
        val csv = """
            Date,Description,Debit,Credit
            2025-01-15,Grocery Store,50.00,
            2025-01-16,Salary,,5000.00
            2025-01-17,Coffee,5.50,
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            descriptionColumn = "Description",
            debitColumn = "Debit",
            creditColumn = "Credit"
        )

        val transactions = parser.parse(csv, mapping, php)

        assertEquals(3, transactions.size)

        // Debit transaction (expense)
        with(transactions[0]) {
            assertEquals(LocalDate.of(2025, 1, 15), date)
            assertEquals("Grocery Store", merchant)
            assertEquals(-5000L, amount.minorUnits) // -50.00
            assertTrue(isExpense)
        }

        // Credit transaction (income)
        with(transactions[1]) {
            assertEquals(LocalDate.of(2025, 1, 16), date)
            assertEquals("Salary", merchant)
            assertEquals(500000L, amount.minorUnits) // 5000.00
            assertTrue(isIncome)
        }

        // Another debit
        with(transactions[2]) {
            assertEquals(LocalDate.of(2025, 1, 17), date)
            assertEquals(-550L, amount.minorUnits) // -5.50
        }
    }

    @Test
    fun `parse CSV with different date format`() {
        val csv = """
            Date,Description,Amount
            01/15/2025,Grocery Store,-50.00
            01/16/2025,Salary,5000.00
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description",
            dateFormat = "MM/dd/yyyy"
        )

        val transactions = parser.parse(csv, mapping, php)

        assertEquals(2, transactions.size)
        assertEquals(LocalDate.of(2025, 1, 15), transactions[0].date)
        assertEquals(LocalDate.of(2025, 1, 16), transactions[1].date)
    }

    @Test
    fun `parse amount with currency symbols and commas`() {
        val csv = """
            Date,Description,Amount
            2025-01-15,Expensive Item,"-$1,234.56"
            2025-01-16,Salary,"$5,000.00"
            2025-01-17,Coffee,₱150.00
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        val transactions = parser.parse(csv, mapping, php)

        assertEquals(3, transactions.size)
        assertEquals(-123456L, transactions[0].amount.minorUnits)
        assertEquals(500000L, transactions[1].amount.minorUnits)
        assertEquals(15000L, transactions[2].amount.minorUnits)
    }

    @Test
    fun `parse amount with parentheses for negative`() {
        val csv = """
            Date,Description,Amount
            2025-01-15,Grocery Store,(50.00)
            2025-01-16,Salary,100.00
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        val transactions = parser.parse(csv, mapping, php)

        assertEquals(2, transactions.size)
        assertEquals(-5000L, transactions[0].amount.minorUnits)
        assertEquals(10000L, transactions[1].amount.minorUnits)
    }

    @Test
    fun `normalize merchant names correctly`() {
        val csv = """
            Date,Description,Amount
            2025-01-15,STARBUCKS #1234,-5.00
            2025-01-16,  Multiple   Spaces   ,-10.00
            2025-01-17,***SPECIAL***,-15.00
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        val transactions = parser.parse(csv, mapping, php)

        assertEquals(3, transactions.size)
        assertEquals("Starbucks", transactions[0].merchant)
        assertEquals("Multiple Spaces", transactions[1].merchant)
        assertEquals("Special", transactions[2].merchant)
    }

    @Test
    fun `skip rows with zero amounts`() {
        val csv = """
            Date,Description,Amount
            2025-01-15,No Change,0.00
            2025-01-16,Valid Transaction,-50.00
            2025-01-17,Also Zero,0
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        val transactions = parser.parse(csv, mapping, php)

        assertEquals(1, transactions.size)
        assertEquals("Valid Transaction", transactions[0].merchant)
    }

    @Test
    fun `skip malformed rows without failing entire import`() {
        val csv = """
            Date,Description,Amount
            2025-01-15,Valid,-50.00
            invalid-date-row,Invalid,
            2025-01-17,Also Valid,-25.00
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        val transactions = parser.parse(csv, mapping, php)

        assertEquals(2, transactions.size)
        assertEquals("Valid", transactions[0].merchant)
        assertEquals("Also Valid", transactions[1].merchant)
    }

    @Test
    fun `parseWithErrors returns errors for malformed rows`() {
        val csv = """
            Date,Description,Amount
            2025-01-15,Valid,-50.00
            invalid-date,Invalid,100
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        val (transactions, errors) = parser.parseWithErrors(csv, mapping, php)

        assertEquals(1, transactions.size)
        assertEquals(1, errors.size)
        assertEquals(2, errors[0].rowIndex) // Row 2 (0-indexed + header)
    }

    @Test
    fun `parse empty CSV returns empty list`() {
        val csv = "Date,Description,Amount"

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description"
        )

        val transactions = parser.parse(csv, mapping, php)

        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `parse CSV with semicolon delimiter`() {
        val csv = """
            Date;Description;Amount
            2025-01-15;Grocery Store;-50.00
            2025-01-16;Salary;5000.00
        """.trimIndent()

        val mapping = ColumnMapping(
            dateColumn = "Date",
            amountColumn = "Amount",
            descriptionColumn = "Description",
            delimiter = ';'
        )

        val transactions = parser.parse(csv, mapping, php)

        assertEquals(2, transactions.size)
        assertEquals("Grocery Store", transactions[0].merchant)
    }

    @Test
    fun `auto detect parses simple format correctly`() {
        val csv = """
            Date,Description,Amount
            2025-01-15,Grocery Store,-50.00
            2025-01-16,Salary,5000.00
        """.trimIndent()

        val result = parser.parseAutoDetect(csv, php)

        assertNotNull(result)
        assertEquals(2, result!!.transactions.size)
        assertEquals("Date", result.mapping.dateColumn)
        assertEquals("Description", result.mapping.descriptionColumn)
    }

    @Test
    fun `auto detect parses debit credit format`() {
        val csv = """
            Date,Description,Debit,Credit
            2025-01-15,Grocery Store,50.00,
            2025-01-16,Salary,,5000.00
        """.trimIndent()

        val result = parser.parseAutoDetect(csv, php)

        assertNotNull(result)
        assertEquals(2, result!!.transactions.size)
        assertTrue(result.mapping.usesDebitCreditColumns)
        assertEquals(-5000L, result.transactions[0].amount.minorUnits)
        assertEquals(500000L, result.transactions[1].amount.minorUnits)
    }

    @Test
    fun `parseAmountToMinorUnits handles various formats`() {
        // Test via the parser's internal method (made internal for testing)
        assertEquals(12345L, parser.parseAmountToMinorUnits("123.45"))
        assertEquals(12345L, parser.parseAmountToMinorUnits("$123.45"))
        assertEquals(12345L, parser.parseAmountToMinorUnits("₱123.45"))
        assertEquals(12345L, parser.parseAmountToMinorUnits("1,234.56") / 10)
        assertEquals(-12345L, parser.parseAmountToMinorUnits("-123.45"))
        assertEquals(-12345L, parser.parseAmountToMinorUnits("(123.45)"))
        assertEquals(0L, parser.parseAmountToMinorUnits("0"))
        assertEquals(0L, parser.parseAmountToMinorUnits("0.00"))
    }

    @Test
    fun `normalizeMerchant handles various formats`() {
        assertEquals("Starbucks", parser.normalizeMerchant("STARBUCKS #1234"))
        assertEquals("Multiple Spaces", parser.normalizeMerchant("  Multiple   Spaces  "))
        assertEquals("Special", parser.normalizeMerchant("***SPECIAL***"))
        assertEquals("Merchant Name", parser.normalizeMerchant("MERCHANT NAME 123456789012345"))
        assertEquals("Store Location", parser.normalizeMerchant("Store #123 Location"))
    }
}
