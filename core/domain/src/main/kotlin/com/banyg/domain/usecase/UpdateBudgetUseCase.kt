package com.banyg.domain.usecase

import com.banyg.domain.model.Budget
import com.banyg.domain.repository.BudgetRepository
import java.time.Instant

/**
 * Update budget use case
 *
 * Updates an existing budget's amount.
 */
class UpdateBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    /**
     * Result of budget update
     */
    sealed class Result {
        data class Success(val budget: Budget) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Update an existing budget
     *
     * @param budgetId ID of the budget to update (required)
     * @param newAmount New budget amount in minor units/cents (required, must be >= 0)
     * @param now Current timestamp for updatedAt
     */
    suspend operator fun invoke(
        budgetId: String,
        newAmount: Long,
        now: Instant = Instant.now()
    ): Result {
        // Validate budget exists
        val existingBudget = budgetRepository.getBudget(budgetId)
            ?: return Result.Error("Budget not found: $budgetId")

        // Validate new amount is non-negative
        if (newAmount < 0L) {
            return Result.Error("Budget amount cannot be negative")
        }

        // Check if amount is actually changing
        if (existingBudget.amount == newAmount) {
            return Result.Success(existingBudget)
        }

        val updatedBudget = existingBudget.copy(
            amount = newAmount,
            updatedAt = now
        )

        return try {
            budgetRepository.saveBudget(updatedBudget)
            Result.Success(updatedBudget)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update budget")
        }
    }

    /**
     * Update budget by category and period
     *
     * @param categoryId Category ID
     * @param period Budget period
     * @param newAmount New budget amount in minor units/cents
     * @param now Current timestamp for updatedAt
     */
    suspend operator fun invoke(
        categoryId: String,
        period: com.banyg.domain.model.BudgetPeriod,
        newAmount: Long,
        now: Instant = Instant.now()
    ): Result {
        // Validate budget exists
        val existingBudget = budgetRepository.getBudget(categoryId, period)
            ?: return Result.Error("Budget not found for category $categoryId in ${period.monthKey}")

        // Validate new amount is non-negative
        if (newAmount < 0L) {
            return Result.Error("Budget amount cannot be negative")
        }

        // Check if amount is actually changing
        if (existingBudget.amount == newAmount) {
            return Result.Success(existingBudget)
        }

        val updatedBudget = existingBudget.copy(
            amount = newAmount,
            updatedAt = now
        )

        return try {
            budgetRepository.saveBudget(updatedBudget)
            Result.Success(updatedBudget)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update budget")
        }
    }
}
