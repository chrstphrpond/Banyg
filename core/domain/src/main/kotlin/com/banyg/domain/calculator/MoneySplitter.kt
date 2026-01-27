package com.banyg.domain.calculator

import com.banyg.domain.model.Money

/**
 * Money Splitting Operations
 *
 * Provides functions to split Money amounts into multiple parts
 * with proper remainder handling.
 *
 * CRITICAL RULES:
 * - Never use Float or Double for money
 * - All operations use Long (minor units)
 * - Remainders are distributed fairly across splits
 * - Split sums always equal original amount (no precision loss)
 */
object MoneySplitter {

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
}
