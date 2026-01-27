package com.banyg.domain.calculator

import com.banyg.domain.model.Money
import kotlin.math.roundToLong

/**
 * Money Arithmetic Operations
 *
 * Provides overflow-safe arithmetic operations for Money values.
 * All operations throw ArithmeticException on overflow.
 *
 * CRITICAL RULES:
 * - Never use Float or Double for money
 * - All operations use Long (minor units)
 * - Overflow protection using Math.addExact, Math.multiplyExact, etc.
 * - Sign conventions enforced:
 *   - Negative = Expense
 *   - Positive = Income
 */
object MoneyArithmetic {

    /**
     * Add two money amounts
     * @throws ArithmeticException if overflow occurs
     * @throws IllegalArgumentException if currencies don't match
     */
    fun add(a: Money, b: Money): Money {
        require(a.currency == b.currency) {
            "Cannot add different currencies: ${a.currency.code} and ${b.currency.code}"
        }
        val result = Math.addExact(a.minorUnits, b.minorUnits)
        return Money(result, a.currency)
    }

    /**
     * Subtract money amounts (a - b)
     * @throws ArithmeticException if overflow occurs
     * @throws IllegalArgumentException if currencies don't match
     */
    fun subtract(a: Money, b: Money): Money {
        require(a.currency == b.currency) {
            "Cannot subtract different currencies: ${a.currency.code} and ${b.currency.code}"
        }
        val result = Math.subtractExact(a.minorUnits, b.minorUnits)
        return Money(result, a.currency)
    }

    /**
     * Multiply money amount by an integer factor
     * @throws ArithmeticException if overflow occurs
     */
    fun multiply(money: Money, factor: Long): Money {
        val result = Math.multiplyExact(money.minorUnits, factor)
        return Money(result, money.currency)
    }

    /**
     * Multiply money amount by a decimal factor with rounding
     * Uses banker's rounding (round half to even)
     * @throws IllegalArgumentException if factor is negative
     */
    fun multiply(money: Money, factor: Double): Money {
        require(factor >= 0.0) { "Factor must be non-negative, got: $factor" }
        val result = (money.minorUnits * factor).roundToLong()
        return Money(result, money.currency)
    }

    /**
     * Divide money amount by an integer divisor
     * Uses truncation (rounds toward zero)
     * @throws ArithmeticException if divisor is zero
     */
    fun divide(money: Money, divisor: Long): Money {
        require(divisor != 0L) { "Cannot divide by zero" }
        val result = money.minorUnits / divisor
        return Money(result, money.currency)
    }

    /**
     * Calculate percentage of money amount
     * @param percent Percentage as integer (0-100)
     * @throws IllegalArgumentException if percent is out of range
     */
    fun percentage(money: Money, percent: Int): Money {
        require(percent in 0..100) { "Percent must be 0-100, got: $percent" }
        val result = (money.minorUnits * percent) / 100
        return Money(result, money.currency)
    }

    /**
     * Round money amount to nearest major unit
     * Example: Money(12350, PHP) -> Money(12300, PHP) = ₱123.00
     */
    fun roundToMajor(money: Money): Money {
        val minorPerMajor = money.currency.minorUnitsPerMajor
        val rounded = (money.minorUnits / minorPerMajor) * minorPerMajor
        return Money(rounded, money.currency)
    }

    /**
     * Sum list of money amounts
     * @throws IllegalArgumentException if currencies don't match
     * @throws ArithmeticException if overflow occurs
     */
    fun sum(amounts: List<Money>): Money {
        require(amounts.isNotEmpty()) { "Cannot sum empty list" }

        val currency = amounts.first().currency
        require(amounts.all { it.currency == currency }) {
            "All amounts must have same currency"
        }

        val total = amounts.fold(0L) { acc, money ->
            Math.addExact(acc, money.minorUnits)
        }

        return Money(total, currency)
    }
}
