package com.banyg.data.mapper

import com.banyg.data.local.entity.SplitEntity
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Split

/**
 * Split Mapper
 *
 * Converts between SplitEntity (Room) and Split (Domain).
 * CRITICAL: Preserves Long minor units, never converts to Float/Double.
 */
class SplitMapper {

    /**
     * Convert Room entity to domain model
     */
    fun toDomain(entity: SplitEntity): Split {
        val currency = Currency.fromCode(entity.currencyCode)
            ?: throw IllegalStateException("Unknown currency: ${entity.currencyCode}")

        return Split(
            transactionId = entity.transactionId,
            lineId = entity.lineId,
            categoryId = entity.categoryId,
            amount = Money(entity.amountMinor, currency),
            memo = entity.memo
        )
    }

    /**
     * Convert domain model to Room entity
     */
    fun toEntity(domain: Split): SplitEntity {
        return SplitEntity(
            transactionId = domain.transactionId,
            lineId = domain.lineId,
            categoryId = domain.categoryId,
            amountMinor = domain.amount.minorUnits,
            currencyCode = domain.amount.currency.code,
            memo = domain.memo
        )
    }

    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<SplitEntity>): List<Split> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(domains: List<Split>): List<SplitEntity> {
        return domains.map { toEntity(it) }
    }
}
