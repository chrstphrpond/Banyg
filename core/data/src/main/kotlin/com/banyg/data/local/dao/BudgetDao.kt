package com.banyg.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.banyg.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Budget DAO
 *
 * Data access object for budgets table.
 * Uses composite primary key: (category_id, period)
 *
 * CRITICAL: All money values use INTEGER (Long) for minor units.
 * Never use REAL/FLOAT for money.
 */
@Dao
interface BudgetDao {

    /**
     * Observe all budgets ordered by period desc, category
     */
    @Query("SELECT * FROM budgets ORDER BY period DESC, category_id ASC")
    fun observeAllBudgets(): Flow<List<BudgetEntity>>

    /**
     * Observe a specific budget by category and period
     */
    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND period = :period")
    fun observeBudget(categoryId: String, period: String): Flow<BudgetEntity?>

    /**
     * Get a specific budget by category and period (one-shot)
     */
    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND period = :period")
    suspend fun getBudget(categoryId: String, period: String): BudgetEntity?

    /**
     * Get all budgets for a specific period (one-shot)
     */
    @Query("SELECT * FROM budgets WHERE period = :period ORDER BY category_id ASC")
    suspend fun getBudgetsForPeriod(period: String): List<BudgetEntity>

    /**
     * Observe all budgets for a specific period
     */
    @Query("SELECT * FROM budgets WHERE period = :period ORDER BY category_id ASC")
    fun observeBudgetsForPeriod(period: String): Flow<List<BudgetEntity>>

    /**
     * Get all budgets for a specific category (one-shot)
     */
    @Query("SELECT * FROM budgets WHERE category_id = :categoryId ORDER BY period DESC")
    suspend fun getBudgetsForCategory(categoryId: String): List<BudgetEntity>

    /**
     * Observe all budgets for a specific category
     */
    @Query("SELECT * FROM budgets WHERE category_id = :categoryId ORDER BY period DESC")
    fun observeBudgetsForCategory(categoryId: String): Flow<List<BudgetEntity>>

    /**
     * Insert or replace a budget
     * OnConflictStrategy.REPLACE for upsert behavior
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    /**
     * Insert multiple budgets
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    /**
     * Update an existing budget
     */
    @Update
    suspend fun update(budget: BudgetEntity)

    /**
     * Delete a specific budget by category and period
     */
    @Query("DELETE FROM budgets WHERE category_id = :categoryId AND period = :period")
    suspend fun delete(categoryId: String, period: String)

    /**
     * Delete all budgets for a specific category
     */
    @Query("DELETE FROM budgets WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: String)

    /**
     * Delete all budgets for a specific period
     */
    @Query("DELETE FROM budgets WHERE period = :period")
    suspend fun deleteByPeriod(period: String)

    /**
     * Delete all budgets
     */
    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    /**
     * Get count of budgets
     */
    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun getCount(): Int

    /**
     * Get count of budgets for a specific period
     */
    @Query("SELECT COUNT(*) FROM budgets WHERE period = :period")
    suspend fun getCountForPeriod(period: String): Int

    /**
     * Check if a budget exists for category and period
     */
    @Query("SELECT EXISTS(SELECT 1 FROM budgets WHERE category_id = :categoryId AND period = :period)")
    suspend fun exists(categoryId: String, period: String): Boolean

    /**
     * Get total budgeted amount for a period
     */
    @Query("SELECT SUM(amount_minor) FROM budgets WHERE period = :period")
    suspend fun getTotalBudgetedForPeriod(period: String): Long?

    /**
     * Get all budgets (one-shot)
     */
    @Query("SELECT * FROM budgets ORDER BY period DESC, category_id ASC")
    suspend fun getAllBudgets(): List<BudgetEntity>
}
