package com.banyg.domain.calculator

import com.banyg.domain.model.Money
import kotlin.math.roundToLong

/**
 * Money Calculator - Overflow-safe arithmetic operations
 *
 * CRITICAL RULES:
 * - Never use Float or Double for money
 * - All operations use Long (minor units)
 * - Overflow protection using Math.addExact, Math.multiplyExact, etc.
 * - Sign conventions enforced:
 *   - Negative = Expense
 *   - Positive = Income
 *   - Transfers must net to zero
 *
 * All operations throw ArithmeticException on overflow.
 */
object MoneyCalculator {

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
     * Split money amount into N equal parts
     * Handles remainders by distributing them across first items
     *
     * Example: split(Money(100, PHP), 3) = [34, 33, 33]
     * @throws IllegalArgumentException if parts < 1
     */
    fun split(money: Money, parts: Int): List<Money> {
        require(parts > 0) { "Parts must be positive, got: $parts" }

        if (parts == 1) {
            return listOf(money)
        }

        val baseAmount = money.minorUnits / parts
        val remainder = money.minorUnits % parts

        return List(parts) { index ->
            val amount = if (index < remainder.toInt()) {
                baseAmount + 1
            } else {
                baseAmount
            }
            Money(amount, money.currency)
        }
    }

    /**
     * Split money by percentages
     * Ensures total equals original amount by adjusting last item for remainder
     *
     * @param money Amount to split
     * @param percentages List of percentages (must sum to 100)
     * @return List of Money amounts matching percentages
     * @throws IllegalArgumentException if percentages don't sum to 100
     */
    fun splitByPercentages(money: Money, percentages: List<Int>): List<Money> {
        val sum = percentages.sum()
        require(sum == 100) { "Percentages must sum to 100, got: $sum" }
        require(percentages.all { it >= 0 }) { "Percentages must be non-negative" }

        val splits = mutableListOf<Money>()
        var allocated = 0L

        for (i in percentages.indices) {
            val split = if (i == percentages.lastIndex) {
                // Last split gets remainder to ensure total matches
                Money(money.minorUnits - allocated, money.currency)
            } else {
                val amount = (money.minorUnits * percentages[i]) / 100
                allocated += amount
                Money(amount, money.currency)
            }
            splits.add(split)
        }

        return splits
    }

    /**
     * Validate that a transfer nets to zero
     * @param from Outgoing amount (must be negative)
     * @param to Incoming amount (must be positive)
     * @throws IllegalArgumentException if validation fails
     */
    fun validateTransfer(from: Money, to: Money) {
        require(from.currency == to.currency) {
            "Transfer currencies must match: ${from.currency.code} and ${to.currency.code}"
        }
        require(from.minorUnits < 0) {
            "From amount must be negative (expense), got: ${from.minorUnits}"
        }
        require(to.minorUnits > 0) {
            "To amount must be positive (income), got: ${to.minorUnits}"
        }

        val sum = Math.addExact(from.minorUnits, to.minorUnits)
        require(sum == 0L) {
            "Transfer must net to zero, got: $sum (${from.minorUnits} + ${to.minorUnits})"
        }
    }

    /**
     * Validate that splits sum to total
     * @param total Original amount
     * @param splits Individual split amounts
     * @throws IllegalArgumentException if splits don't sum to total
     */
    fun validateSplits(total: Money, splits: List<Money>) {
        require(splits.isNotEmpty()) { "Splits cannot be empty" }
        require(splits.all { it.currency == total.currency }) {
            "All splits must have same currency as total"
        }

        val sum = splits.fold(0L) { acc, split ->
            Math.addExact(acc, split.minorUnits)
        }

        require(sum == total.minorUnits) {
            "Splits must sum to total. Expected: ${total.minorUnits}, got: $sum"
        }
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
