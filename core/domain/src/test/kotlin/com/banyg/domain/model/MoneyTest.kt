package com.banyg.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Money domain model
 * Tests sign conventions, comparisons, and basic operations
 */
class MoneyTest {

    private val php = Currency.PHP
    private val usd = Currency.USD

    // Sign Convention Tests

    @Test
    fun `negative amount is expense`() {
        val money = Money(-10000, php)
        assertTrue(money.isExpense)
        assertFalse(money.isIncome)
        assertFalse(money.isZero)
    }

    @Test
    fun `positive amount is income`() {
        val money = Money(10000, php)
        assertTrue(money.isIncome)
        assertFalse(money.isExpense)
        assertFalse(money.isZero)
    }

    @Test
    fun `zero amount is neither expense nor income`() {
        val money = Money(0, php)
        assertTrue(money.isZero)
        assertFalse(money.isExpense)
        assertFalse(money.isIncome)
    }

    // Comparison Tests

    @Test
    fun `compare money with same currency`() {
        val a = Money(10000, php)
        val b = Money(5000, php)
        val c = Money(10000, php)

        assertTrue(a > b)
        assertTrue(b < a)
        assertTrue(a == c)
        assertTrue(a.compareTo(c) == 0)
    }

    @Test
    fun `compare negative amounts`() {
        val expense1 = Money(-10000, php) // -₱100
        val expense2 = Money(-5000, php)  // -₱50

        assertTrue(expense2 > expense1) // -50 > -100
        assertTrue(expense1 < expense2)
    }

    @Test
    fun `cannot compare different currencies`() {
        val phpMoney = Money(10000, php)
        val usdMoney = Money(10000, usd)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            phpMoney.compareTo(usdMoney)
        }
        assertTrue(exception.message!!.contains("different currencies"))
    }

    // Absolute Value Tests

    @Test
    fun `abs returns positive amount`() {
        val expense = Money(-10000, php)
        val absolute = expense.abs()

        assertEquals(10000L, absolute.minorUnits)
        assertFalse(absolute.isExpense)
    }

    @Test
    fun `abs of positive amount returns same amount`() {
        val income = Money(10000, php)
        val absolute = income.abs()

        assertEquals(income, absolute)
    }

    @Test
    fun `abs of zero returns zero`() {
        val zero = Money.zero(php)
        val absolute = zero.abs()

        assertEquals(zero, absolute)
    }

    // Negation Tests

    @Test
    fun `unary minus negates amount`() {
        val income = Money(10000, php)
        val expense = -income

        assertEquals(-10000L, expense.minorUnits)
        assertTrue(expense.isExpense)
    }

    @Test
    fun `double negation returns original`() {
        val original = Money(10000, php)
        val negated = -(-original)

        assertEquals(original, negated)
    }

    // Zero Tests

    @Test
    fun `zero creates zero amount`() {
        val zero = Money.zero(php)

        assertEquals(0L, zero.minorUnits)
        assertTrue(zero.isZero)
    }

    // From Major Tests

    @Test
    fun `fromMajor with double creates correct minor units`() {
        val money = Money.fromMajor(123.45, php)

        assertEquals(12345L, money.minorUnits)
        assertEquals(php, money.currency)
    }

    @Test
    fun `fromMajor with long creates correct minor units`() {
        val money = Money.fromMajor(100L, php)

        assertEquals(10000L, money.minorUnits)
        assertEquals(php, money.currency)
    }

    @Test
    fun `fromMajor handles JPY with no minor units`() {
        val jpy = Currency.JPY
        val money = Money.fromMajor(1000L, jpy)

        assertEquals(1000L, money.minorUnits) // 1:1 for JPY
        assertEquals(jpy, money.currency)
    }

    @Test
    fun `fromMajor handles negative amounts`() {
        val expense = Money.fromMajor(-50.00, usd)

        assertEquals(-5000L, expense.minorUnits)
        assertTrue(expense.isExpense)
    }

    // Equality Tests

    @Test
    fun `money with same values are equal`() {
        val a = Money(10000, php)
        val b = Money(10000, php)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `money with different amounts are not equal`() {
        val a = Money(10000, php)
        val b = Money(5000, php)

        assertNotEquals(a, b)
    }

    @Test
    fun `money with different currencies are not equal`() {
        val a = Money(10000, php)
        val b = Money(10000, usd)

        assertNotEquals(a, b)
    }

    // Edge Cases

    @Test
    fun `money handles max Long value`() {
        val max = Money(Long.MAX_VALUE, php)

        assertEquals(Long.MAX_VALUE, max.minorUnits)
        assertTrue(max.isIncome)
    }

    @Test
    fun `money handles min Long value`() {
        val min = Money(Long.MIN_VALUE, php)

        assertEquals(Long.MIN_VALUE, min.minorUnits)
        assertTrue(min.isExpense)
    }

    @Test
    fun `currency enforces 3 character code`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Currency("US", "$", "US Dollar")
        }
        assertTrue(exception.message!!.contains("3 characters"))
    }

    @Test
    fun `currency requires positive minor units per major`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Currency("XXX", "X", "Test", minorUnitsPerMajor = 0)
        }
        assertTrue(exception.message!!.contains("positive"))
    }
}
