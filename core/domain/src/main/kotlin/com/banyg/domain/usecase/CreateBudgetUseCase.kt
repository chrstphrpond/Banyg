package com.banyg.domain.usecase

import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.model.Currency
import com.banyg.domain.repository.BudgetRepository
import com.banyg.domain.repository.CategoryRepository
import java.time.Instant
import java.util.UUID

/**
 * Create budget use case
 *
 * Creates a new monthly budget for a category with validation.
 */
class CreateBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {
    /**
     * Result of budget creation
     */
    sealed class Result {
        data class Success(val budget: Budget) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Create a new budget
     *
     * @param categoryId Category to budget for (required)
     * @param amount Budgeted amount in minor units/cents (required, must be > 0)
     * @param period Budget period (defaults to current month)
     * @param currency Currency for the budget (defaults to PHP)
     * @param now Current timestamp for createdAt/updatedAt
     * @param id Optional ID (generated if not provided)
     */
    suspend operator fun invoke(
        categoryId: String,
        amount: Long,
        period: BudgetPeriod = BudgetPeriod.current(),
        currency: Currency = Currency.PHP,
        now: Instant = Instant.now(),
        id: String = UUID.randomUUID().toString()
    ): Result {
        // Validate category exists
        val category = categoryRepository.getCategory(categoryId)
            ?: return Result.Error("Category not found: $categoryId")

        // Validate amount is positive
        if (amount <= 0L) {
            return Result.Error("Budget amount must be greater than zero")
        }

        // Check for duplicate budget for this category and period
        val existingBudget = budgetRepository.getBudget(categoryId, period)
        if (existingBudget != null) {
            return Result.Error(
                "Budget already exists for ${category.name} in ${period.monthKey}"
            )
        }

        val budget = Budget(
            id = id,
            categoryId = categoryId,
            amount = amount,
            period = period,
            currency = currency,
            createdAt = now,
            updatedAt = now
        )

        return try {
            budgetRepository.saveBudget(budget)
            Result.Success(budget)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create budget")
        }
    }
}
