package com.banyg.data.repository

import com.banyg.data.local.dao.BudgetDao
import com.banyg.data.mapper.BudgetMapper
import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Budget Repository Implementation
 *
 * Implements BudgetRepository using Room DAO and mapper.
 * Converts between domain models and Room entities.
 *
 * CRITICAL: All money operations use Long (minor units), never Float/Double.
 */
class BudgetRepositoryImpl(
    private val dao: BudgetDao,
    private val mapper: BudgetMapper = BudgetMapper()
) : BudgetRepository {

    override fun observeAllBudgets(): Flow<List<Budget>> {
        return dao.observeAllBudgets()
            .map { mapper.toDomainList(it) }
    }

    override fun observeBudgetsByPeriod(period: BudgetPeriod): Flow<List<Budget>> {
        return dao.observeBudgetsForPeriod(period.monthKey)
            .map { mapper.toDomainList(it) }
    }

    override fun observeBudget(categoryId: String, period: BudgetPeriod): Flow<Budget?> {
        return dao.observeBudget(categoryId, period.monthKey)
            .map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeBudgetsByCategory(categoryId: String): Flow<List<Budget>> {
        return dao.observeBudgetsForCategory(categoryId)
            .map { mapper.toDomainList(it) }
    }

    override suspend fun getBudget(id: String): Budget? {
        // Parse composite ID: "categoryId:period"
        val (categoryId, period) = parseBudgetId(id)
        return dao.getBudget(categoryId, period)?.let { mapper.toDomain(it) }
    }

    override suspend fun getBudget(categoryId: String, period: BudgetPeriod): Budget? {
        return dao.getBudget(categoryId, period.monthKey)?.let { mapper.toDomain(it) }
    }

    override suspend fun getAllBudgets(): List<Budget> {
        return mapper.toDomainList(dao.getAllBudgets())
    }

    override suspend fun getBudgetsByPeriod(period: BudgetPeriod): List<Budget> {
        return mapper.toDomainList(dao.getBudgetsForPeriod(period.monthKey))
    }

    override suspend fun saveBudget(budget: Budget) {
        val entity = mapper.toEntity(budget)
        dao.insert(entity)
    }

    override suspend fun saveBudgets(budgets: List<Budget>) {
        val entities = mapper.toEntityList(budgets)
        dao.insertAll(entities)
    }

    override suspend fun deleteBudget(id: String) {
        // Parse composite ID: "categoryId:period"
        val (categoryId, period) = parseBudgetId(id)
        dao.delete(categoryId, period)
    }

    override suspend fun deleteBudget(categoryId: String, period: BudgetPeriod) {
        dao.delete(categoryId, period.monthKey)
    }

    override suspend fun deleteBudgetsForPeriod(period: BudgetPeriod) {
        dao.deleteByPeriod(period.monthKey)
    }

    override suspend fun budgetExists(categoryId: String, period: BudgetPeriod): Boolean {
        return dao.exists(categoryId, period.monthKey)
    }

    override suspend fun getBudgetCount(period: BudgetPeriod): Int {
        return dao.getCountForPeriod(period.monthKey)
    }

    override suspend fun getTotalBudgeted(period: BudgetPeriod): Long {
        return dao.getTotalBudgetedForPeriod(period.monthKey) ?: 0L
    }

    /**
     * Parse budget ID into categoryId and period
     * Budget ID format: "categoryId:period" (e.g., "cat123:2026-01")
     */
    private fun parseBudgetId(id: String): Pair<String, String> {
        val parts = id.split(":", limit = 2)
        require(parts.size == 2) { "Invalid budget ID format: $id. Expected: categoryId:period" }
        return parts[0] to parts[1]
    }
}
