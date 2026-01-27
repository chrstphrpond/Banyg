package com.banyg.domain.usecase

import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.repository.BudgetRepository

/**
 * Delete budget use case
 *
 * Deletes/reset a budget for a category.
 */
class DeleteBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    /**
     * Result of budget deletion
     */
    sealed class Result {
        data class Success(val deletedId: String) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Delete budget by ID
     *
     * @param budgetId ID of the budget to delete
     */
    suspend operator fun invoke(budgetId: String): Result {
        // Validate budget exists
        val existingBudget = budgetRepository.getBudget(budgetId)
            ?: return Result.Error("Budget not found: $budgetId")

        return try {
            budgetRepository.deleteBudget(budgetId)
            Result.Success(existingBudget.id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete budget")
        }
    }

    /**
     * Delete budget by category and period
     *
     * @param categoryId Category ID
     * @param period Budget period
     */
    suspend operator fun invoke(
        categoryId: String,
        period: BudgetPeriod
    ): Result {
        // Validate budget exists
        val existingBudget = budgetRepository.getBudget(categoryId, period)
            ?: return Result.Error("Budget not found for category $categoryId in ${period.monthKey}")

        return try {
            budgetRepository.deleteBudget(categoryId, period)
            Result.Success(existingBudget.id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete budget")
        }
    }
}
