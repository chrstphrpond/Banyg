package com.banyg.domain.usecase

import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.model.BudgetProgress
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.BudgetRepository
import com.banyg.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Get budget progress use case
 *
 * Retrieves a budget with calculated spent/remaining amounts.
 */
class GetBudgetProgressUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    /**
     * Result of getting budget progress
     */
    sealed class Result {
        data class Success(val progress: BudgetProgress) : Result()
        data class Error(val message: String) : Result()
        data object NotFound : Result()
    }

    /**
     * Get budget progress by budget ID
     *
     * @param budgetId ID of the budget
     */
    suspend operator fun invoke(budgetId: String): Result {
        val budget = budgetRepository.getBudget(budgetId)
            ?: return Result.NotFound

        return calculateProgress(budget)
    }

    /**
     * Get budget progress by category and period
     *
     * @param categoryId Category ID
     * @param period Budget period (defaults to current month)
     */
    suspend operator fun invoke(
        categoryId: String,
        period: BudgetPeriod = BudgetPeriod.current()
    ): Result {
        val budget = budgetRepository.getBudget(categoryId, period)
            ?: return Result.NotFound

        return calculateProgress(budget)
    }

    /**
     * Observe budget progress as a Flow
     * Emits updates when budget or transactions change
     *
     * @param categoryId Category ID
     * @param period Budget period (defaults to current month)
     */
    fun observe(
        categoryId: String,
        period: BudgetPeriod = BudgetPeriod.current()
    ): Flow<Result> {
        return combine(
            budgetRepository.observeBudget(categoryId, period),
            transactionRepository.observeByCategory(categoryId)
        ) { budget, transactions ->
            if (budget == null) {
                Result.NotFound
            } else {
                val spent = calculateSpentForPeriod(budget, transactions)
                Result.Success(BudgetProgress(budget, spent))
            }
        }
    }

    /**
     * Calculate progress for a budget
     */
    private suspend fun calculateProgress(budget: Budget): Result {
        return try {
            val transactions = getTransactionsForBudget(budget)
            val spent = calculateSpentFromTransactions(budget.categoryId, budget.period, transactions)
            Result.Success(BudgetProgress(budget, spent))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to calculate budget progress")
        }
    }

    /**
     * Get all transactions that could affect this budget
     */
    private suspend fun getTransactionsForBudget(budget: Budget): List<Transaction> {
        return transactionRepository.getTransactionsByCategory(budget.categoryId)
    }

    /**
     * Calculate spent amount from transactions for the budget period
     */
    private fun calculateSpentForPeriod(budget: Budget, transactions: List<Transaction>): Long {
        return calculateSpentFromTransactions(budget.categoryId, budget.period, transactions)
    }

    companion object {
        /**
         * Calculate total spent from transactions
         * - Includes only expenses (negative amounts)
         * - Excludes transfers (transferId != null)
         * - Excludes pending transactions (only cleared/reconciled)
         * - Includes split transactions (each split contributes to its category)
         */
        fun calculateSpentFromTransactions(
            categoryId: String,
            period: BudgetPeriod,
            transactions: List<Transaction>
        ): Long {
            var total = 0L

            for (transaction in transactions) {
                // Skip non-cleared transactions
                if (transaction.status != TransactionStatus.CLEARED &&
                    transaction.status != TransactionStatus.RECONCILED
                ) {
                    continue
                }

                // Skip transfers
                if (transaction.transferId != null) continue

                // Check if transaction falls within budget period
                if (transaction.date.isBefore(period.startDate())) continue
                if (transaction.date.isAfter(period.endDate())) continue

                if (transaction.splits.isNotEmpty()) {
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
