package com.banyg.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Account Room entity
 *
 * Represents a financial account in the database.
 * CRITICAL: Uses amount_minor (Long) for money, never Float/Double.
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["is_archived"]),
        Index(value = ["currency_code"]),
        Index(value = ["type"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    /**
     * Opening balance in minor units (centavos/cents)
     * CRITICAL: Must be Long, never Float/Double
     */
    @ColumnInfo(name = "opening_balance_minor")
    val openingBalanceMinor: Long,

    /**
     * Current balance in minor units (centavos/cents)
     * CRITICAL: Must be Long, never Float/Double
     */
    @ColumnInfo(name = "current_balance_minor")
    val currentBalanceMinor: Long,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
