package com.banyg.domain.calculator

import com.banyg.domain.model.Money

/**
 * Money Calculator - Facade for Money Operations
 *
 * This class provides backward compatibility for existing code.
 * For new code, use the focused classes directly:
 * - [MoneyArithmetic] for arithmetic operations (add, subtract, multiply, divide, etc.)
 * - [MoneySplitter] for splitting operations (split, splitByPercentages)
 * - [MoneyValidator] for validation operations (validateTransfer, validateSplits)
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
 *
 * @deprecated Use MoneyArithmetic, MoneySplitter, or MoneyValidator directly for better code organization
 */
@Deprecated(
    message = "Use MoneyArithmetic, MoneySplitter, or MoneyValidator directly for better code organization",
    replaceWith = ReplaceWith(
        "MoneyArithmetic, MoneySplitter, MoneyValidator",
        "com.banyg.domain.calculator.MoneyArithmetic",
        "com.banyg.domain.calculator.MoneySplitter",
        "com.banyg.domain.calculator.MoneyValidator"
    )
)
object MoneyCalculator {

    /**
     * Add two money amounts
     * @throws ArithmeticException if overflow occurs
     * @throws IllegalArgumentException if currencies don't match
     * @see MoneyArithmetic.add
     */
    fun add(a: Money, b: Money): Money = MoneyArithmetic.add(a, b)

    /**
     * Subtract money amounts (a - b)
     * @throws ArithmeticException if overflow occurs
     * @throws IllegalArgumentException if currencies don't match
     * @see MoneyArithmetic.subtract
     */
    fun subtract(a: Money, b: Money): Money = MoneyArithmetic.subtract(a, b)

    /**
     * Multiply money amount by an integer factor
     * @throws ArithmeticException if overflow occurs
     * @see MoneyArithmetic.multiply
     */
    fun multiply(money: Money, factor: Long): Money = MoneyArithmetic.multiply(money, factor)

    /**
     * Multiply money amount by a decimal factor with rounding
     * Uses banker's rounding (round half to even)
     * @throws IllegalArgumentException if factor is negative
     * @see MoneyArithmetic.multiply
     */
    fun multiply(money: Money, factor: Double): Money = MoneyArithmetic.multiply(money, factor)

    /**
     * Divide money amount by an integer divisor
     * Uses truncation (rounds toward zero)
     * @throws ArithmeticException if divisor is zero
     * @see MoneyArithmetic.divide
     */
    fun divide(money: Money, divisor: Long): Money = MoneyArithmetic.divide(money, divisor)

    /**
     * Calculate percentage of money amount
     * @param percent Percentage as integer (0-100)
     * @throws IllegalArgumentException if percent is out of range
     * @see MoneyArithmetic.percentage
     */
    fun percentage(money: Money, percent: Int): Money = MoneyArithmetic.percentage(money, percent)

    /**
     * Split money amount into N equal parts
     * Handles remainders by distributing them across first items
     *
     * Example: split(Money(100, PHP), 3) = [34, 33, 33]
     * @throws IllegalArgumentException if parts < 1
     * @see MoneySplitter.split
     */
    fun split(money: Money, parts: Int): List<Money> = MoneySplitter.split(money, parts)

    /**
     * Split money by percentages
     * Ensures total equals original amount by adjusting last item for remainder
     *
     * @param money Amount to split
     * @param percentages List of percentages (must sum to 100)
     * @return List of Money amounts matching percentages
     * @throws IllegalArgumentException if percentages don't sum to 100
     * @see MoneySplitter.splitByPercentages
     */
    fun splitByPercentages(money: Money, percentages: List<Int>): List<Money> =
        MoneySplitter.splitByPercentages(money, percentages)

    /**
     * Validate that a transfer nets to zero
     * @param from Outgoing amount (must be negative)
     * @param to Incoming amount (must be positive)
     * @throws IllegalArgumentException if validation fails
     * @see MoneyValidator.validateTransfer
     */
    fun validateTransfer(from: Money, to: Money) = MoneyValidator.validateTransfer(from, to)

    /**
     * Validate that splits sum to total
     * @param total Original amount
     * @param splits Individual split amounts
     * @throws IllegalArgumentException if splits don't sum to total
     * @see MoneyValidator.validateSplits
     */
    fun validateSplits(total: Money, splits: List<Money>) = MoneyValidator.validateSplits(total, splits)

    /**
     * Round money amount to nearest major unit
     * Example: Money(12350, PHP) -> Money(12300, PHP) = ₱123.00
     * @see MoneyArithmetic.roundToMajor
     */
    fun roundToMajor(money: Money): Money = MoneyArithmetic.roundToMajor(money)

    /**
     * Sum list of money amounts
     * @throws IllegalArgumentException if currencies don't match
     * @throws ArithmeticException if overflow occurs
     * @see MoneyArithmetic.sum
     */
    fun sum(amounts: List<Money>): Money = MoneyArithmetic.sum(amounts)
}
