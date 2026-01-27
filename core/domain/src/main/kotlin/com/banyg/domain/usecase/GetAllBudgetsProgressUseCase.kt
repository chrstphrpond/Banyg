package com.banyg.domain.usecase

import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.model.BudgetProgress
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.repository.BudgetRepository
import com.banyg.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Get all budgets progress use case
 *
 * Retrieves all budgets with calculated progress for a period.
 * Also calculates totals across all budgets.
 */
class GetAllBudgetsProgressUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    /**
     * Result containing all budget progress and totals
     */
    data class Result(
        val budgets: List<BudgetProgress>,
        val totals: Totals,
        val period: BudgetPeriod
    )

    /**
     * Totals across all budgets
     */
    data class Totals(
        val totalBudgeted: Long,
        val totalSpent: Long,
        val totalRemaining: Long,
        val overallPercentage: Int,
        val currency: Currency,
        val overBudgetCount: Int,
        val underBudgetCount: Int,
        val onBudgetCount: Int
    ) {
        /**
         * True if any budget is over budget
         */
        val hasOverBudget: Boolean
            get() = overBudgetCount > 0

        /**
         * True if all budgets are under or on budget
         */
        val allUnderBudget: Boolean
            get() = overBudgetCount == 0 && totalBudgeted > 0

        /**
         * Total budgeted as Money
         */
        fun totalBudgetedAsMoney(): Money = Money(totalBudgeted, currency)

        /**
         * Total spent as Money
         */
        fun totalSpentAsMoney(): Money = Money(totalSpent, currency)

        /**
         * Total remaining as Money
         */
        fun totalRemainingAsMoney(): Money = Money(totalRemaining, currency)
    }

    /**
     * Get all budgets with progress for a period
     *
     * @param period Budget period (defaults to current month)
     * @param currency Currency for totals (defaults to PHP)
     */
    suspend operator fun invoke(
        period: BudgetPeriod = BudgetPeriod.current(),
        currency: Currency = Currency.PHP
    ): Result {
        val budgets = budgetRepository.getBudgetsByPeriod(period)
        return calculateResult(budgets, period, currency)
    }

    /**
     * Observe all budgets with progress as a Flow
     * Emits updates when budgets or transactions change
     *
     * @param period Budget period (defaults to current month)
     * @param currency Currency for totals (defaults to PHP)
     */
    fun observe(
        period: BudgetPeriod = BudgetPeriod.current(),
        currency: Currency = Currency.PHP
    ): Flow<Result> {
        return combine(
            budgetRepository.observeBudgetsByPeriod(period),
            transactionRepository.observeRecentTransactions(limit = 1000)
        ) { budgets, transactions ->
            calculateResult(budgets, period, currency, transactions)
        }
    }

    /**
     * Calculate result with budget progress and totals
     */
    private fun calculateResult(
        budgets: List<Budget>,
        period: BudgetPeriod,
        currency: Currency,
        allTransactions: List<Transaction> = emptyList()
    ): Result {
        val categoryIds = budgets.map { it.categoryId }.toSet()

        // Filter transactions to only those relevant to our categories
        val relevantTransactions = allTransactions.filter { transaction ->
            transaction.categoryId in categoryIds ||
                transaction.splits.any { it.categoryId in categoryIds }
        }

        // Calculate progress for each budget
        val progressList = budgets.map { budget ->
            val spent = if (allTransactions.isEmpty()) {
                // If no transactions provided, we'll calculate without them
                // The repository implementation should handle this properly
                0L
            } else {
                GetBudgetProgressUseCase.calculateSpentFromTransactions(
                    budget.categoryId,
                    period,
                    relevantTransactions
                )
            }
            BudgetProgress(budget, spent)
        }

        // Calculate totals
        val totals = calculateTotals(progressList, currency)

        return Result(
            budgets = progressList,
            totals = totals,
            period = period
        )
    }

    /**
     * Calculate totals across all budget progress entries
     */
    private fun calculateTotals(
        progressList: List<BudgetProgress>,
        currency: Currency
    ): Totals {
        var totalBudgeted = 0L
        var totalSpent = 0L
        var overBudgetCount = 0
        var underBudgetCount = 0
        var onBudgetCount = 0

        for (progress in progressList) {
            totalBudgeted = Math.addExact(totalBudgeted, progress.budgeted)

            // For spent, we use absolute value of expenses only
            val spentAbs = if (progress.spent < 0) kotlin.math.abs(progress.spent) else 0L
            totalSpent = Math.addExact(totalSpent, spentAbs)

            when {
                progress.isOverBudget -> overBudgetCount++
                progress.isOnBudget -> onBudgetCount++
                progress.isUnderBudget -> underBudgetCount++
            }
        }

        val totalRemaining = Math.subtractExact(totalBudgeted, totalSpent)

        val overallPercentage = if (totalBudgeted == 0L) {
            0
        } else {
            ((totalSpent * 100L) / totalBudgeted).toInt().coerceIn(0, 100)
        }

        return Totals(
            totalBudgeted = totalBudgeted,
            totalSpent = totalSpent,
            totalRemaining = totalRemaining,
            overallPercentage = overallPercentage,
            currency = currency,
            overBudgetCount = overBudgetCount,
            underBudgetCount = underBudgetCount,
            onBudgetCount = onBudgetCount
        )
    }
}
