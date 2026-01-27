package com.banyg.domain.calculator

import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MoneySplitter
 * Tests splitting operations and remainder distribution
 */
class MoneySplitterTest {

    private val php = Currency.PHP

    // Split Tests

    @Test
    fun `split into equal parts`() {
        val money = Money(10000, php) // ₱100
        val splits = MoneySplitter.split(money, 2)

        assertEquals(2, splits.size)
        assertEquals(5000L, splits[0].minorUnits) // ₱50
        assertEquals(5000L, splits[1].minorUnits) // ₱50
    }

    @Test
    fun `split with remainder distributes to first items`() {
        val money = Money(100, php) // ₱1.00
        val splits = MoneySplitter.split(money, 3)

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
        val splits = MoneySplitter.split(money, 1)

        assertEquals(1, splits.size)
        assertEquals(money, splits[0])
    }

    @Test
    fun `split negative amount`() {
        val expense = Money(-10000, php)
        val splits = MoneySplitter.split(expense, 2)

        assertEquals(2, splits.size)
        assertEquals(-5000L, splits[0].minorUnits)
        assertEquals(-5000L, splits[1].minorUnits)
    }

    @Test
    fun `split throws on zero or negative parts`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneySplitter.split(money, 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            MoneySplitter.split(money, -1)
        }
    }

    // Split by Percentages Tests

    @Test
    fun `splitByPercentages divides correctly`() {
        val money = Money(10000, php) // ₱100
        val splits = MoneySplitter.splitByPercentages(money, listOf(50, 30, 20))

        assertEquals(3, splits.size)
        assertEquals(5000L, splits[0].minorUnits) // 50%
        assertEquals(3000L, splits[1].minorUnits) // 30%
        assertEquals(2000L, splits[2].minorUnits) // 20%
    }

    @Test
    fun `splitByPercentages handles remainder in last item`() {
        val money = Money(10000, php) // ₱100
        val splits = MoneySplitter.splitByPercentages(money, listOf(33, 33, 34))

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
            MoneySplitter.splitByPercentages(money, listOf(50, 30))
        }

        assertThrows(IllegalArgumentException::class.java) {
            MoneySplitter.splitByPercentages(money, listOf(50, 30, 30))
        }
    }

    @Test
    fun `splitByPercentages throws on negative percentage`() {
        val money = Money(10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneySplitter.splitByPercentages(money, listOf(50, 60, -10))
        }
    }
}
