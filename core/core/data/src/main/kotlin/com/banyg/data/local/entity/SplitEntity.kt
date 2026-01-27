package com.banyg.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Split Room entity
 *
 * Represents a split of a transaction amount across multiple categories.
 * Composite primary key: (transaction_id, line_id)
 *
 * CRITICAL: Uses amount_minor (Long) for money, never Float/Double.
 * IMPORTANT: All splits for a transaction must sum to transaction amount.
 */
@Entity(
    tableName = "splits",
    primaryKeys = ["transaction_id", "line_id"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["category_id"])
    ]
)
data class SplitEntity(
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    /**
     * Line number within transaction (0, 1, 2, ...)
     */
    @ColumnInfo(name = "line_id")
    val lineId: Int,

    @ColumnInfo(name = "category_id")
    val categoryId: String,

    /**
     * Split amount in minor units (centavos/cents)
     * CRITICAL: Must be Long, never Float/Double
     * Sign: Negative = expense split, Positive = income split
     */
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    @ColumnInfo(name = "memo")
    val memo: String? = null
)
