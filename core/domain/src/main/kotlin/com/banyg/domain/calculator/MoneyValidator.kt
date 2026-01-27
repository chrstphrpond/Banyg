package com.banyg.domain.calculator

import com.banyg.domain.model.Money

/**
 * Money Validation Operations
 *
 * Provides validation functions for Money operations like
 * transfers and splits.
 *
 * CRITICAL RULES:
 * - Never use Float or Double for money
 * - All operations use Long (minor units)
 * - Sign conventions enforced:
 *   - Transfer from: negative (expense)
 *   - Transfer to: positive (income)
 *   - Transfers must net to zero
 */
object MoneyValidator {

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
}
