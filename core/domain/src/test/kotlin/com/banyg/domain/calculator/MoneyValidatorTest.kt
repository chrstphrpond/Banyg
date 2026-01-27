package com.banyg.domain.calculator

import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MoneyValidator
 * Tests validation operations for transfers and splits
 */
class MoneyValidatorTest {

    private val php = Currency.PHP
    private val usd = Currency.USD

    // Transfer Validation Tests

    @Test
    fun `validateTransfer passes for correct transfer`() {
        val from = Money(-10000, php) // Expense
        val to = Money(10000, php)    // Income

        // Should not throw
        MoneyValidator.validateTransfer(from, to)
    }

    @Test
    fun `validateTransfer throws if from is positive`() {
        val from = Money(10000, php)  // Should be negative
        val to = Money(-10000, php)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyValidator.validateTransfer(from, to)
        }
    }

    @Test
    fun `validateTransfer throws if to is negative`() {
        val from = Money(-10000, php)
        val to = Money(-10000, php)   // Should be positive

        assertThrows(IllegalArgumentException::class.java) {
            MoneyValidator.validateTransfer(from, to)
        }
    }

    @Test
    fun `validateTransfer throws if not net zero`() {
        val from = Money(-10000, php)
        val to = Money(5000, php)     // Doesn't net to zero

        assertThrows(IllegalArgumentException::class.java) {
            MoneyValidator.validateTransfer(from, to)
        }
    }

    @Test
    fun `validateTransfer throws on different currencies`() {
        val from = Money(-10000, php)
        val to = Money(10000, usd)

        assertThrows(IllegalArgumentException::class.java) {
            MoneyValidator.validateTransfer(from, to)
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
        MoneyValidator.validateSplits(total, splits)
    }

    @Test
    fun `validateSplits throws when sum doesn't equal total`() {
        val total = Money(10000, php)
        val splits = listOf(
            Money(5000, php),
            Money(3000, php)
        )

        assertThrows(IllegalArgumentException::class.java) {
            MoneyValidator.validateSplits(total, splits)
        }
    }

    @Test
    fun `validateSplits throws on empty list`() {
        val total = Money(10000, php)
        val splits = emptyList<Money>()

        assertThrows(IllegalArgumentException::class.java) {
            MoneyValidator.validateSplits(total, splits)
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
            MoneyValidator.validateSplits(total, splits)
        }
    }
}
