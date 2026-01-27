package com.banyg.domain.model

import com.banyg.domain.calculator.MoneyCalculator
import java.time.LocalDate

/**
 * Budget progress with calculated spent/remaining values
 *
 * Represents the current state of a budget including:
 * - Budgeted amount (allocated)
 * - Spent amount (activity)
 * - Remaining/available amount
 * - Percentage used
 * - Over budget indicator
 *
 * @property budget The budget entity
 * @param spent Spent amount in minor units (cents) - can be positive (income) or negative (expenses)
 */
data class BudgetProgress(
    val budget: Budget,
    val spent: Long
) {
    /**
     * Budgeted amount in minor units (always positive)
     */
    val budgeted: Long
        get() = budget.amount

    /**
     * Remaining amount in minor units
     * Positive = money left to spend
     * Negative = overspent
     */
    val remaining: Long
        get() = Math.subtractExact(budgeted, spentAbs)

    /**
     * Absolute value of spent (always positive for percentage calculation)
     * Expenses are negative in transaction model, so we negate for spending
     */
    private val spentAbs: Long
        get() = if (spent < 0) kotlin.math.abs(spent) else 0L

    /**
     * Percentage of budget spent (0-100)
     * Returns 0 if budget is zero
     */
    val percentage: Int
        get() = if (budgeted == 0L) {
            0
        } else {
            val pct = (spentAbs * 100L) / budgeted
            pct.coerceIn(0L, 100L).toInt()
        }

    /**
     * True if spent amount exceeds budget (over budget)
     */
    val isOverBudget: Boolean
        get() = spentAbs > budgeted

    /**
     * True if exactly on budget (spent equals budget)
     */
    val isOnBudget: Boolean
        get() = spentAbs == budgeted && budgeted > 0

    /**
     * True if under budget (spent less than budget)
     */
    val isUnderBudget: Boolean
        get() = spentAbs < budgeted

    /**
     * True if no budget set (budget amount is zero)
     */
    val hasNoBudget: Boolean
        get() = budgeted == 0L

    /**
     * Get Money representation of spent amount
     */
    fun spentAsMoney(): Money = Money(spent, budget.currency)

    /**
     * Get Money representation of remaining amount
     */
    fun remainingAsMoney(): Money = Money(remaining, budget.currency)

    /**
     * Get the over-budget amount (0 if not over budget)
     */
    fun overBudgetAmount(): Long = if (isOverBudget) spentAbs - budgeted else 0L

    companion object {
        /**
         * Create BudgetProgress from budget and list of transactions
         * Calculates spent from transactions for the category and period
         *
         * Note: Only includes expenses (negative amounts). Income is not counted as "spending".
         */
        fun fromTransactions(
            budget: Budget,
            transactions: List<Transaction>
        ): BudgetProgress {
            val spent = calculateSpent(budget.categoryId, budget.period, transactions)
            return BudgetProgress(budget, spent)
        }

        /**
         * Calculate total spent from transactions
         * - Includes only expenses (negative amounts)
         * - Excludes transfers (transferId != null)
         * - Excludes pending transactions (only cleared/reconciled)
         * - Includes split transactions (each split contributes to its category)
         */
        private fun calculateSpent(
            categoryId: String,
            period: BudgetPeriod,
            transactions: List<Transaction>
        ): Long {
            var total = 0L

            for (transaction in transactions) {
                // Skip non-cleared transactions
                if (!transaction.isCleared) continue

                // Skip transfers
                if (transaction.isTransfer) continue

                // Check if transaction falls within budget period
                if (!period.contains(transaction.date)) continue

                if (transaction.hasSplits) {
                    // Process splits - each split contributes to its category
                    for (split in transaction.splits) {
                        if (split.categoryId == categoryId) {
                            total = Math.addExact(total, split.amount.minorUnits)
                        }
                    }
                } else if (transaction.categoryId == categoryId) {
                    // Direct transaction to this category
                    total = Math.addExact(total, transaction.amount.minorUnits)
                }
            }

            return total
        }
    }
}

/**
 * Check if a date falls within this budget period
 */
private fun BudgetPeriod.contains(date: LocalDate): Boolean {
    return !date.isBefore(startDate()) && !date.isAfter(endDate())
}
