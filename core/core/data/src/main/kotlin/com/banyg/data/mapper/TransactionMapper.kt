package com.banyg.data.mapper

import com.banyg.data.local.entity.SplitEntity
import com.banyg.data.local.entity.TransactionEntity
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Split
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Transaction Mapper
 *
 * Converts between TransactionEntity (Room) and Transaction (Domain).
 * CRITICAL: Preserves Long minor units, never converts to Float/Double.
 *
 * Handles date conversion:
 * - Database: YYYYMMDD as Long (e.g., 20240127)
 * - Domain: LocalDate
 */
class TransactionMapper(
    private val splitMapper: SplitMapper = SplitMapper()
) {

    /**
     * Convert Room entity to domain model
     */
    fun toDomain(
        entity: TransactionEntity,
        splits: List<SplitEntity> = emptyList()
    ): Transaction {
        val currency = Currency.fromCode(entity.currencyCode)
            ?: throw IllegalStateException("Unknown currency: ${entity.currencyCode}")

        return Transaction(
            id = entity.id,
            accountId = entity.accountId,
            date = dateFromLong(entity.date),
            amount = Money(entity.amountMinor, currency),
            merchant = entity.merchant,
            memo = entity.memo,
            categoryId = entity.categoryId,
            status = TransactionStatus.valueOf(entity.status),
            clearedAt = entity.clearedAt?.let { Instant.ofEpochMilli(it) },
            transferId = entity.transferId,
            splits = splitMapper.toDomainList(splits),
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt)
        )
    }

    /**
     * Convert domain model to Room entity
     */
    fun toEntity(domain: Transaction): TransactionEntity {
        return TransactionEntity(
            id = domain.id,
            accountId = domain.accountId,
            date = dateToLong(domain.date),
            amountMinor = domain.amount.minorUnits,
            currencyCode = domain.amount.currency.code,
            merchant = domain.merchant,
            memo = domain.memo,
            categoryId = domain.categoryId,
            status = domain.status.name,
            clearedAt = domain.clearedAt?.toEpochMilli(),
            transferId = domain.transferId,
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli()
        )
    }

    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<TransactionEntity>): List<Transaction> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(domains: List<Transaction>): List<TransactionEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * Convert LocalDate to YYYYMMDD Long
     * Example: 2024-01-27 -> 20240127L
     */
    fun dateToLong(date: LocalDate): Long {
        return date.format(DateTimeFormatter.BASIC_ISO_DATE).toLong()
    }

    /**
     * Convert YYYYMMDD Long to LocalDate
     * Example: 20240127L -> 2024-01-27
     */
    fun dateFromLong(dateLong: Long): LocalDate {
        val dateString = dateLong.toString().padStart(8, '0')
        return LocalDate.parse(dateString, DateTimeFormatter.BASIC_ISO_DATE)
    }
}
