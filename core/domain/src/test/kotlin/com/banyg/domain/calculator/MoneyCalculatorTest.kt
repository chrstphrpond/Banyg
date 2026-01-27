package com.banyg.domain.calculator

import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MoneyCalculator
 * Tests arithmetic, overflow protection, splitting, and rounding
 */
class MoneyCalculatorTest {

    private val php = Currency.PHP
    private val usd = Currency.USD

    // Addition Tests

    @Test
    fun `add positive amounts returns sum`() {
        val a = Money(10000, php)
        val b = Money(5000, php)
        val result = MoneyCalculator.add(a, b)

        assertEquals(15000L, result.minorUnits)
    }

    @Test
    fun `add negative amounts returns sum`() {
        val a = Money(-10000, php)
        val b = Money(-5000, php)
        val result = MoneyCalculator.add(a, b)

        assertEquals(-15000L, result.minorUnits)
    }

    @Test
    fun `add positive and negative amounts`() {
        val income = Money(10000, php)
        val expense = Money(-3000, php)
        val result = MoneyCalculator.add(income, expense)

        assertEquals(7000L, result.minorUnits)
    }

    @Test
    fun `add with zero returns original`() {
        val money = Money(10000, php)
        val zero = Money.zero(php)
        val result = MoneyCalculator.add(money, zero)

        assertEquals(money, result)
    }

    @Test
    fun `add throws on overflow`() {
        val max = Money(Long.MAX_VALUE, php)
        val one = Money(1, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.add(max, one)
        }
    }

    @Test
    fun `add throws on different currencies`() {
        val phpMoney = Money(10000, php)
        val usdMoney = Money(10000, usd)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.add(phpMoney, usdMoney)
        }
        assertTrue(exception.message!!.contains("different currencies"))
    }

    // Subtraction Tests

    @Test
    fun `subtract returns difference`() {
        val a = Money(10000, php)
        val b = Money(3000, php)
        val result = MoneyCalculator.subtract(a, b)

        assertEquals(7000L, result.minorUnits)
    }

    @Test
    fun `subtract resulting in negative`() {
        val a = Money(5000, php)
        val b = Money(10000, php)
        val result = MoneyCalculator.subtract(a, b)

        assertEquals(-5000L, result.minorUnits)
        assertTrue(result.isExpense)
    }

    @Test
    fun `subtract throws on overflow`() {
        val min = Money(Long.MIN_VALUE, php)
        val one = Money(1, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.subtract(min, one)
        }
    }

    // Multiplication Tests

    @Test
    fun `multiply by integer factor`() {
        val money = Money(10000, php) // ₱100
        val result = MoneyCalculator.multiply(money, 3L)

        assertEquals(30000L, result.minorUnits) // ₱300
    }

    @Test
    fun `multiply by zero returns zero`() {
        val money = Money(10000, php)
        val result = MoneyCalculator.multiply(money, 0L)

        assertEquals(0L, result.minorUnits)
    }

    @Test
    fun `multiply negative amount`() {
        val expense = Money(-10000, php)
        val result = MoneyCalculator.multiply(expense, 2L)

        assertEquals(-20000L, result.minorUnits)
    }

    @Test
    fun `multiply by decimal factor with rounding`() {
        val money = Money(10000, php) // ₱100
        val result = MoneyCalculator.multiply(money, 1.5)

        assertEquals(15000L, result.minorUnits) // ₱150
    }

    @Test
    fun `multiply by decimal rounds correctly`() {
        val money = Money(10001, php) // ₱100.01
        val result = MoneyCalculator.multiply(money, 0.5)

        assertEquals(5001L, result.minorUnits) // ₱50.01 (rounds up)
    }

    @Test
    fun `multiply throws on negative factor`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.multiply(money, -1.0)
        }
    }

    @Test
    fun `multiply throws on overflow`() {
        val max = Money(Long.MAX_VALUE, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.multiply(max, 2L)
        }
    }

    // Division Tests

    @Test
    fun `divide by integer divisor`() {
        val money = Money(10000, php) // ₱100
        val result = MoneyCalculator.divide(money, 2L)

        assertEquals(5000L, result.minorUnits) // ₱50
    }

    @Test
    fun `divide truncates toward zero`() {
        val money = Money(10001, php) // ₱100.01
        val result = MoneyCalculator.divide(money, 2L)

        assertEquals(5000L, result.minorUnits) // ₱50.00 (truncates 0.5 centavo)
    }

    @Test
    fun `divide negative amount`() {
        val expense = Money(-10000, php)
        val result = MoneyCalculator.divide(expense, 2L)

        assertEquals(-5000L, result.minorUnits)
    }

    @Test
    fun `divide throws on zero divisor`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.divide(money, 0L)
        }
    }

    // Percentage Tests

    @Test
    fun `percentage calculates correctly`() {
        val money = Money(10000, php) // ₱100
        val result = MoneyCalculator.percentage(money, 15)

        assertEquals(1500L, result.minorUnits) // ₱15 (15%)
    }

    @Test
    fun `percentage of zero returns zero`() {
        val zero = Money.zero(php)
        val result = MoneyCalculator.percentage(zero, 50)

        assertEquals(0L, result.minorUnits)
    }

    @Test
    fun `percentage 100 returns original`() {
        val money = Money(10000, php)
        val result = MoneyCalculator.percentage(money, 100)

        assertEquals(money, result)
    }

    @Test
    fun `percentage 0 returns zero`() {
        val money = Money(10000, php)
        val result = MoneyCalculator.percentage(money, 0)

        assertEquals(0L, result.minorUnits)
    }

    @Test
    fun `percentage throws on invalid value`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.percentage(money, -1)
        }

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.percentage(money, 101)
        }
    }

    // Split Tests

    @Test
    fun `split into equal parts`() {
        val money = Money(10000, php) // ₱100
        val splits = MoneyCalculator.split(money, 2)

        assertEquals(2, splits.size)
        assertEquals(5000L, splits[0].minorUnits) // ₱50
        assertEquals(5000L, splits[1].minorUnits) // ₱50
    }

    @Test
    fun `split with remainder distributes to first items`() {
        val money = Money(100, php) // ₱1.00
        val splits = MoneyCalculator.split(money, 3)

        assertEquals(3, splits.size)
        assertEquals(34L, splits[0].minorUnits) // Gets extra centavo
        assertEquals(33L, splits[1].minorUnits)
        assertEquals(33L, splits[2].minorUnits)

        // Verify sum equals original
        val sum = splits.sumOf { it.minorUnits }
        assertEquals(100L, sum)
    }

    @Test
    fun `split into 1 part returns original`() {
        val money = Money(10000, php)
        val splits = MoneyCalculator.split(money, 1)

        assertEquals(1, splits.size)
        assertEquals(money, splits[0])
    }

    @Test
    fun `split negative amount`() {
        val expense = Money(-10000, php)
        val splits = MoneyCalculator.split(expense, 2)

        assertEquals(2, splits.size)
        assertEquals(-5000L, splits[0].minorUnits)
        assertEquals(-5000L, splits[1].minorUnits)
    }

    @Test
    fun `split throws on zero or negative parts`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.split(money, 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.split(money, -1)
        }
    }

    // Split by Percentages Tests

    @Test
    fun `splitByPercentages divides correctly`() {
        val money = Money(10000, php) // ₱100
        val splits = MoneyCalculator.splitByPercentages(money, listOf(50, 30, 20))

        assertEquals(3, splits.size)
        assertEquals(5000L, splits[0].minorUnits) // 50%
        assertEquals(3000L, splits[1].minorUnits) // 30%
        assertEquals(2000L, splits[2].minorUnits) // 20%
    }

    @Test
    fun `splitByPercentages handles remainder in last item`() {
        val money = Money(10000, php) // ₱100
        val splits = MoneyCalculator.splitByPercentages(money, listOf(33, 33, 34))

        assertEquals(3, splits.size)
        assertEquals(3300L, splits[0].minorUnits) // 33%
        assertEquals(3300L, splits[1].minorUnits) // 33%
        assertEquals(3400L, splits[2].minorUnits) // 34% gets remainder

        // Verify sum equals original
        val sum = splits.sumOf { it.minorUnits }
        assertEquals(10000L, sum)
    }

    @Test
    fun `splitByPercentages throws if not 100`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.splitByPercentages(money, listOf(50, 30))
        }

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.splitByPercentages(money, listOf(50, 30, 30))
        }
    }

    @Test
    fun `splitByPercentages throws on negative percentage`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.splitByPercentages(money, listOf(50, 60, -10))
        }
    }

    // Transfer Validation Tests

    @Test
    fun `validateTransfer passes for correct transfer`() {
        val from = Money(-10000, php) // Expense
        val to = Money(10000, php)    // Income

        // Should not throw
        MoneyCalculator.validateTransfer(from, to)
    }

    @Test
    fun `validateTransfer throws if from is positive`() {
        val from = Money(10000, php)  // Should be negative
        val to = Money(-10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.validateTransfer(from, to)
        }
    }

    @Test
    fun `validateTransfer throws if to is negative`() {
        val from = Money(-10000, php)
        val to = Money(-10000, php)   // Should be positive

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.validateTransfer(from, to)
        }
    }

    @Test
    fun `validateTransfer throws if not net zero`() {
        val from = Money(-10000, php)
        val to = Money(5000, php)     // Doesn't net to zero

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.validateTransfer(from, to)
        }
    }

    @Test
    fun `validateTransfer throws on different currencies`() {
        val from = Money(-10000, php)
        val to = Money(10000, usd)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.validateTransfer(from, to)
        }
    }

    // Split Validation Tests

    @Test
    fun `validateSplits passes when sum equals total`() {
        val total = Money(10000, php)
        val splits = listOf(
            Money(5000, php),
            Money(3000, php),
            Money(2000, php)
        )

        // Should not throw
        MoneyCalculator.validateSplits(total, splits)
    }

    @Test
    fun `validateSplits throws when sum doesn't equal total`() {
        val total = Money(10000, php)
        val splits = listOf(
            Money(5000, php),
            Money(3000, php)
        )

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.validateSplits(total, splits)
        }
    }

    @Test
    fun `validateSplits throws on empty list`() {
        val total = Money(10000, php)
        val splits = emptyList<Money>()

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.validateSplits(total, splits)
        }
    }

    @Test
    fun `validateSplits throws on currency mismatch`() {
        val total = Money(10000, php)
        val splits = listOf(
            Money(5000, php),
            Money(5000, usd)  // Wrong currency
        )

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.validateSplits(total, splits)
        }
    }

    // Rounding Tests

    @Test
    fun `roundToMajor rounds to nearest major unit`() {
        val money = Money(12350, php) // ₱123.50
        val rounded = MoneyCalculator.roundToMajor(money)

        assertEquals(12300L, rounded.minorUnits) // ₱123.00
    }

    @Test
    fun `roundToMajor handles exact major units`() {
        val money = Money(10000, php) // ₱100.00
        val rounded = MoneyCalculator.roundToMajor(money)

        assertEquals(10000L, rounded.minorUnits)
    }

    @Test
    fun `roundToMajor handles negative amounts`() {
        val expense = Money(-12350, php) // -₱123.50
        val rounded = MoneyCalculator.roundToMajor(expense)

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
        val total = MoneyCalculator.sum(amounts)

        assertEquals(18000L, total.minorUnits)
    }

    @Test
    fun `sum handles negative amounts`() {
        val amounts = listOf(
            Money(10000, php),
            Money(-3000, php),
            Money(5000, php)
        )
        val total = MoneyCalculator.sum(amounts)

        assertEquals(12000L, total.minorUnits)
    }

    @Test
    fun `sum of single item returns item`() {
        val amounts = listOf(Money(10000, php))
        val total = MoneyCalculator.sum(amounts)

        assertEquals(10000L, total.minorUnits)
    }

    @Test
    fun `sum throws on empty list`() {
        val amounts = emptyList<Money>()

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.sum(amounts)
        }
    }

    @Test
    fun `sum throws on mixed currencies`() {
        val amounts = listOf(
            Money(10000, php),
            Money(10000, usd)
        )

        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.sum(amounts)
        }
    }

    @Test
    fun `sum throws on overflow`() {
        val amounts = listOf(
            Money(Long.MAX_VALUE, php),
            Money(1, php)
        )

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.sum(amounts)
        }
    }

    // ========================================
    // Overflow Protection Tests
    // ========================================
    // Comprehensive tests to ensure arithmetic operations throw ArithmeticException
    // on overflow instead of silently corrupting financial data

    @Test
    fun `add throws ArithmeticException on negative overflow`() {
        val a = Money(Long.MIN_VALUE, php)
        val b = Money(-1L, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.add(a, b)
        }
    }

    @Test
    fun `subtract throws ArithmeticException on positive overflow`() {
        val a = Money(Long.MAX_VALUE, php)
        val b = Money(-1L, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.subtract(a, b)
        }
    }

    @Test
    fun `subtract throws ArithmeticException on negative underflow`() {
        val a = Money(Long.MIN_VALUE, php)
        val b = Money(Long.MAX_VALUE, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.subtract(a, b)
        }
    }

    @Test
    fun `multiply with Long factor throws ArithmeticException on positive overflow`() {
        val money = Money(Long.MAX_VALUE / 2 + 1, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.multiply(money, 2L)
        }
    }

    @Test
    fun `multiply with negative factor throws ArithmeticException on negative overflow`() {
        val money = Money(Long.MIN_VALUE / 2 - 1, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.multiply(money, 2L)
        }
    }

    @Test
    fun `multiply with large factor throws ArithmeticException on overflow`() {
        val money = Money(Long.MAX_VALUE / 10, php)

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.multiply(money, 11L)
        }
    }

    @Test
    fun `sum throws ArithmeticException on overflow with multiple items`() {
        val amounts = listOf(
            Money(Long.MAX_VALUE / 2, php),
            Money(Long.MAX_VALUE / 2, php),
            Money(Long.MAX_VALUE / 2, php)
        )

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.sum(amounts)
        }
    }

    @Test
    fun `sum throws ArithmeticException on negative overflow`() {
        val amounts = listOf(
            Money(Long.MIN_VALUE / 2, php),
            Money(Long.MIN_VALUE / 2, php),
            Money(Long.MIN_VALUE / 2, php)
        )

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.sum(amounts)
        }
    }

    @Test
    fun `validateSplits throws ArithmeticException on overflow during sum`() {
        val total = Money(100L, php)
        val splits = listOf(
            Money(Long.MAX_VALUE, php),
            Money(1L, php)
        )

        assertThrows(ArithmeticException::class.java) {
            MoneyCalculator.validateSplits(total, splits)
        }
    }

    @Test
    fun `add at boundary does not overflow`() {
        // Test that we can add at the edge without overflow
        val a = Money(Long.MAX_VALUE - 1, php)
        val b = Money(1L, php)
        val result = MoneyCalculator.add(a, b)

        assertEquals(Long.MAX_VALUE, result.minorUnits)
    }

    @Test
    fun `subtract at boundary does not overflow`() {
        // Test that we can subtract at the edge without overflow
        val a = Money(Long.MIN_VALUE + 1, php)
        val b = Money(1L, php)
        val result = MoneyCalculator.subtract(a, b)

        assertEquals(Long.MIN_VALUE, result.minorUnits)
    }

    @Test
    fun `multiply at boundary does not overflow`() {
        // Test that we can multiply at the edge without overflow
        val money = Money(Long.MAX_VALUE / 2, php)
        val result = MoneyCalculator.multiply(money, 2L)

        assertEquals(Long.MAX_VALUE - 1, result.minorUnits) // Due to truncation
    }
}
