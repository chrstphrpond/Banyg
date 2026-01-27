package com.banyg.domain.repository

import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import kotlinx.coroutines.flow.Flow

/**
 * Budget Repository Interface
 *
 * Defines operations for budget persistence.
 * Implementation in core/data layer.
 */
interface BudgetRepository {

    /**
     * Observe all budgets
     */
    fun observeAllBudgets(): Flow<List<Budget>>

    /**
     * Observe budgets for a specific period
     */
    fun observeBudgetsByPeriod(period: BudgetPeriod): Flow<List<Budget>>

    /**
     * Observe budget for a specific category and period
     * Returns null if no budget exists
     */
    fun observeBudget(categoryId: String, period: BudgetPeriod): Flow<Budget?>

    /**
     * Observe budgets for a specific category across all periods
     */
    fun observeBudgetsByCategory(categoryId: String): Flow<List<Budget>>

    /**
     * Get budget by ID (one-shot)
     */
    suspend fun getBudget(id: String): Budget?

    /**
     * Get budget for category and period (one-shot)
     * Returns null if no budget exists
     */
    suspend fun getBudget(categoryId: String, period: BudgetPeriod): Budget?

    /**
     * Get all budgets (one-shot)
     */
    suspend fun getAllBudgets(): List<Budget>

    /**
     * Get budgets for a specific period (one-shot)
     */
    suspend fun getBudgetsByPeriod(period: BudgetPeriod): List<Budget>

    /**
     * Save budget (insert or update)
     * If budget with same category and period exists, it will be updated
     */
    suspend fun saveBudget(budget: Budget)

    /**
     * Save multiple budgets
     */
    suspend fun saveBudgets(budgets: List<Budget>)

    /**
     * Delete budget by ID
     */
    suspend fun deleteBudget(id: String)

    /**
     * Delete budget for category and period
     */
    suspend fun deleteBudget(categoryId: String, period: BudgetPeriod)

    /**
     * Delete all budgets for a period
     */
    suspend fun deleteBudgetsForPeriod(period: BudgetPeriod)

    /**
     * Check if budget exists for category and period
     */
    suspend fun budgetExists(categoryId: String, period: BudgetPeriod): Boolean

    /**
     * Get count of budgets for a period
     */
    suspend fun getBudgetCount(period: BudgetPeriod): Int

    /**
     * Get total budgeted amount for a period
     */
    suspend fun getTotalBudgeted(period: BudgetPeriod): Long
}
