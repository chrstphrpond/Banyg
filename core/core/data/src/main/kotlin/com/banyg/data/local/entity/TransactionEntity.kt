package com.banyg.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transaction Room entity
 *
 * Represents a financial transaction (expense, income, or transfer).
 * CRITICAL: Uses amount_minor (Long) for money, never Float/Double.
 *
 * Sign conventions:
 * - Negative amount_minor = Expense/outflow
 * - Positive amount_minor = Income/inflow
 * - Transfers: Two linked transactions with opposite signs
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["date"]),
        Index(value = ["category_id"]),
        Index(value = ["status"]),
        Index(value = ["transfer_id"]),
        Index(value = ["account_id", "date"]),
        Index(value = ["account_id", "status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "account_id")
    val accountId: String,

    /**
     * Transaction date as YYYYMMDD Long
     * Example: 2024-01-27 = 20240127L
     */
    @ColumnInfo(name = "date")
    val date: Long,

    /**
     * Amount in minor units (centavos/cents)
     * CRITICAL: Must be Long, never Float/Double
     * Sign: Negative = expense, Positive = income
     */
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    @ColumnInfo(name = "merchant")
    val merchant: String,

    @ColumnInfo(name = "memo")
    val memo: String? = null,

    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,

    @ColumnInfo(name = "status")
    val status: String = "PENDING",

    /**
     * Timestamp when transaction was cleared (Unix epoch millis)
     */
    @ColumnInfo(name = "cleared_at")
    val clearedAt: Long? = null,

    /**
     * Transfer ID links two transactions (outflow and inflow)
     * If not null, this is part of a transfer
     */
    @ColumnInfo(name = "transfer_id")
    val transferId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
