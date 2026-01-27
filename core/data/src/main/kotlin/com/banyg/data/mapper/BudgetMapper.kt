package com.banyg.data.mapper

import com.banyg.data.local.entity.BudgetEntity
import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.model.Currency
import java.time.Instant

/**
 * Budget Mapper
 *
 * Converts between BudgetEntity (Room) and Budget (Domain).
 * Handles currency and period conversions.
 *
 * CRITICAL: Money values are always Long (minor units), never Float/Double.
 */
class BudgetMapper {

    /**
     * Convert Room entity to domain model
     */
    fun toDomain(entity: BudgetEntity): Budget {
        val currency = Currency.fromCode(entity.currencyCode)
            ?: throw IllegalArgumentException("Unknown currency code: ${entity.currencyCode}")
        return Budget(
            id = createBudgetId(entity.categoryId, entity.period),
            categoryId = entity.categoryId,
            amount = entity.amountMinor,
            period = BudgetPeriod.fromMonthKey(entity.period),
            currency = currency,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt)
        )
    }

    /**
     * Convert domain model to Room entity
     */
    fun toEntity(domain: Budget): BudgetEntity {
        return BudgetEntity(
            categoryId = domain.categoryId,
            period = domain.period.monthKey,
            amountMinor = domain.amount,
            currencyCode = domain.currency.code,
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli()
        )
    }

    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<BudgetEntity>): List<Budget> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(domains: List<Budget>): List<BudgetEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * Create budget ID from category and period
     * Used when converting entity to domain
     */
    private fun createBudgetId(categoryId: String, period: String): String {
        return "$categoryId:$period"
    }
}
