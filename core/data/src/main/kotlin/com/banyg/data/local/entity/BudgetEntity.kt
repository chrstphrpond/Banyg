package com.banyg.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Budget Room entity
 *
 * Represents a budget allocation for a specific category and period.
 * Uses composite primary key: (category_id, period)
 *
 * CRITICAL: Money stored as INTEGER (Long) in minor units (cents/centavos).
 * Never use REAL/FLOAT for money amounts.
 */
@Entity(
    tableName = "budgets",
    primaryKeys = ["category_id", "period"],
    indices = [
        Index(value = ["period"]),
        Index(value = ["category_id"])
    ]
)
data class BudgetEntity(
    @ColumnInfo(name = "category_id")
    val categoryId: String,

    @ColumnInfo(name = "period")
    val period: String, // YYYY-MM format

    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long, // Budget amount in minor units (cents), always >= 0

    @ColumnInfo(name = "currency_code")
    val currencyCode: String, // ISO 4217 currency code (e.g., "PHP", "USD")

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Unix timestamp in milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long // Unix timestamp in milliseconds
) {
    init {
        require(categoryId.isNotBlank()) { "Category ID cannot be blank" }
        require(period.matches(Regex("\\d{4}-\\d{2}"))) { "Period must be in YYYY-MM format, got: $period" }
        require(amountMinor >= 0L) { "Budget amount cannot be negative, got: $amountMinor" }
        require(currencyCode.isNotBlank()) { "Currency code cannot be blank" }
    }
}
