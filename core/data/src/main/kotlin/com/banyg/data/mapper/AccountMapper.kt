package com.banyg.data.mapper

import com.banyg.data.local.entity.AccountEntity
import com.banyg.domain.model.Account
import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import java.time.Instant

/**
 * Account Mapper
 *
 * Converts between AccountEntity (Room) and Account (Domain).
 * CRITICAL: Preserves Long minor units, never converts to Float/Double.
 */
class AccountMapper {

    /**
     * Convert Room entity to domain model
     */
    fun toDomain(entity: AccountEntity): Account {
        val currency = Currency.fromCode(entity.currencyCode)
            ?: throw IllegalStateException("Unknown currency: ${entity.currencyCode}")

        return Account(
            id = entity.id,
            name = entity.name,
            type = AccountType.valueOf(entity.type),
            currency = currency,
            openingBalance = Money(entity.openingBalanceMinor, currency),
            currentBalance = Money(entity.currentBalanceMinor, currency),
            isArchived = entity.isArchived,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt)
        )
    }

    /**
     * Convert domain model to Room entity
     */
    fun toEntity(domain: Account): AccountEntity {
        return AccountEntity(
            id = domain.id,
            name = domain.name,
            type = domain.type.name,
            currencyCode = domain.currency.code,
            openingBalanceMinor = domain.openingBalance.minorUnits,
            currentBalanceMinor = domain.currentBalance.minorUnits,
            isArchived = domain.isArchived,
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli()
        )
    }

    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<AccountEntity>): List<Account> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(domains: List<Account>): List<AccountEntity> {
        return domains.map { toEntity(it) }
    }
}
