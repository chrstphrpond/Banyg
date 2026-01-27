package com.banyg.domain.calculator

import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MoneyArithmetic
 * Tests arithmetic operations, overflow protection, and rounding
 */
class MoneyArithmeticTest {

    private val php = Currency.PHP
    private val usd = Currency.USD

    // Addition Tests

    @Test
    fun `add positive amounts returns sum`() {
        val a = Money(10000, php)
        val b = Money(5000, php)
        val result = MoneyArithmetic.add(a, b)

        assertEquals(15000L, result.minorUnits)
    }

    @Test
    fun `add negative amounts returns sum`() {
        val a = Money(-10000, php)
        val b = Money(-5000, php)
        val result = MoneyArithmetic.add(a, b)

        assertEquals(-15000L, result.minorUnits)
    }

    @Test
    fun `add positive and negative amounts`() {
        val income = Money(10000, php)
        val expense = Money(-3000, php)
        val result = MoneyArithmetic.add(income, expense)

        assertEquals(7000L, result.minorUnits)
    }

    @Test
    fun `add with zero returns original`() {
        val money = Money(10000, php)
        val zero = Money.zero(php)
        val result = MoneyArithmetic.add(money, zero)

        assertEquals(money, result)
    }

    @Test
    fun `add throws on overflow`() {
        val max = Money(Long.MAX_VALUE, php)
        val one = Money(1, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyArithmetic.add(max, one)
        }
    }

    @Test
    fun `add throws on different currencies`() {
        val phpMoney = Money(10000, php)
        val usdMoney = Money(10000, usd)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            MoneyArithmetic.add(phpMoney, usdMoney)
        }
        assertTrue(exception.message!!.contains("different currencies"))
    }

    // Subtraction Tests

    @Test
    fun `subtract returns difference`() {
        val a = Money(10000, php)
        val b = Money(3000, php)
        val result = MoneyArithmetic.subtract(a, b)

        assertEquals(7000L, result.minorUnits)
    }

    @Test
    fun `subtract resulting in negative`() {
        val a = Money(5000, php)
        val b = Money(10000, php)
        val result = MoneyArithmetic.subtract(a, b)

        assertEquals(-5000L, result.minorUnits)
        assertTrue(result.isExpense)
    }

    @Test
    fun `subtract throws on overflow`() {
        val min = Money(Long.MIN_VALUE, php)
        val one = Money(1, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyArithmetic.subtract(min, one)
        }
    }

    // Multiplication Tests

    @Test
    fun `multiply by integer factor`() {
        val money = Money(10000, php) // ₱100
        val result = MoneyArithmetic.multiply(money, 3L)

        assertEquals(30000L, result.minorUnits) // ₱300
    }

    @Test
    fun `multiply by zero returns zero`() {
        val money = Money(10000, php)
        val result = MoneyArithmetic.multiply(money, 0L)

        assertEquals(0L, result.minorUnits)
    }

    @Test
    fun `multiply negative amount`() {
        val expense = Money(-10000, php)
        val result = MoneyArithmetic.multiply(expense, 2L)

        assertEquals(-20000L, result.minorUnits)
    }

    @Test
    fun `multiply by decimal factor with rounding`() {
        val money = Money(10000, php) // ₱100
        val result = MoneyArithmetic.multiply(money, 1.5)

        assertEquals(15000L, result.minorUnits) // ₱150
    }

    @Test
    fun `multiply by decimal rounds correctly`() {
        val money = Money(10001, php) // ₱100.01
        val result = MoneyArithmetic.multiply(money, 0.5)

        assertEquals(5001L, result.minorUnits) // ₱50.01 (rounds up)
    }

    @Test
    fun `multiply throws on negative factor`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyArithmetic.multiply(money, -1.0)
        }
    }

    @Test
    fun `multiply throws on overflow`() {
        val max = Money(Long.MAX_VALUE, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyArithmetic.multiply(max, 2L)
        }
    }

    // Division Tests

    @Test
    fun `divide by integer divisor`() {
        val money = Money(10000, php) // ₱100
        val result = MoneyArithmetic.divide(money, 2L)

        assertEquals(5000L, result.minorUnits) // ₱50
    }

    @Test
    fun `divide truncates toward zero`() {
        val money = Money(10001, php) // ₱100.01
        val result = MoneyArithmetic.divide(money, 2L)

        assertEquals(5000L, result.minorUnits) // ₱50.00 (truncates 0.5 centavo)
    }

    @Test
    fun `divide negative amount`() {
        val expense = Money(-10000, php)
        val result = MoneyArithmetic.divide(expense, 2L)

        assertEquals(-5000L, result.minorUnits)
    }

    @Test
    fun `divide throws on zero divisor`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyArithmetic.divide(money, 0L)
        }
    }

    // Percentage Tests

    @Test
    fun `percentage calculates correctly`() {
        val money = Money(10000, php) // ₱100
        val result = MoneyArithmetic.percentage(money, 15)

        assertEquals(1500L, result.minorUnits) // ₱15 (15%)
    }

    @Test
    fun `percentage of zero returns zero`() {
        val zero = Money.zero(php)
        val result = MoneyArithmetic.percentage(zero, 50)

        assertEquals(0L, result.minorUnits)
    }

    @Test
    fun `percentage 100 returns original`() {
        val money = Money(10000, php)
        val result = MoneyArithmetic.percentage(money, 100)

        assertEquals(money, result)
    }

    @Test
    fun `percentage 0 returns zero`() {
        val money = Money(10000, php)
        val result = MoneyArithmetic.percentage(money, 0)

        assertEquals(0L, result.minorUnits)
    }

    @Test
    fun `percentage throws on invalid value`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyArithmetic.percentage(money, -1)
        }

        assertThrows(IllegalArgumentException::class.java) {
            MoneyArithmetic.percentage(money, 101)
        }
    }

    // Rounding Tests

    @Test
    fun `roundToMajor rounds to nearest major unit`() {
        val money = Money(12350, php) // ₱123.50
        val rounded = MoneyArithmetic.roundToMajor(money)

        assertEquals(12300L, rounded.minorUnits) // ₱123.00
    }

    @Test
    fun `roundToMajor handles exact major units`() {
        val money = Money(10000, php) // ₱100.00
        val rounded = MoneyArithmetic.roundToMajor(money)

        assertEquals(10000L, rounded.minorUnits)
    }

    @Test
    fun `roundToMajor handles negative amounts`() {
        val expense = Money(-12350, php) // -₱123.50
        val rounded = MoneyArithmetic.roundToMajor(expense)

        assertEquals(-12300L, rounded.minorUnits) // -₱123.00
    }

    // Sum Tests

    @Test
    fun `sum calculates total of list`() {
        val amounts = listOf(
            Money(10000, php),
            Money(5000, php),
            Money(3000, php)
        )
        val total = MoneyArithmetic.sum(amounts)

        assertEquals(18000L, total.minorUnits)
    }

    @Test
    fun `sum handles negative amounts`() {
        val amounts = listOf(
            Money(10000, php),
            Money(-3000, php),
            Money(5000, php)
        )
        val total = MoneyArithmetic.sum(amounts)

        assertEquals(12000L, total.minorUnits)
    }

    @Test
    fun `sum of single item returns item`() {
        val amounts = listOf(Money(10000, php))
        val total = MoneyArithmetic.sum(amounts)

        assertEquals(10000L, total.minorUnits)
    }

    @Test
    fun `sum throws on empty list`() {
        val amounts = emptyList<Money>()

        assertThrows(IllegalArgumentException::class.java) {
            MoneyArithmetic.sum(amounts)
        }
    }

    @Test
    fun `sum throws on mixed currencies`() {
        val amounts = listOf(
            Money(10000, php),
            Money(10000, usd)
        )

        assertThrows(IllegalArgumentException::class.java) {
            MoneyArithmetic.sum(amounts)
        }
    }

    @Test
    fun `sum throws on overflow`() {
        val amounts = listOf(
            Money(Long.MAX_VALUE, php),
            Money(1, php)
        )

        assertThrows(ArithmeticException::class.java) {
            MoneyArithmetic.sum(amounts)
        }
    }
}
