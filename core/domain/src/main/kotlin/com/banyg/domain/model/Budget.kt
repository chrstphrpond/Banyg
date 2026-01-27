package com.banyg.domain.model

import java.time.Instant

/**
 * Budget domain model
 *
 * Represents a budget allocation for a specific category and period.
 * Uses envelope-style budgeting where money is allocated at the start of the period.
 *
 * @property id Unique identifier
 * @property categoryId Category this budget is for
 * @property amount Budgeted amount in minor units (cents) - always positive
 * @property period The budget period (month/year)
 * @property currency Currency for this budget
 * @property createdAt Creation timestamp
 * @property updatedAt Last update timestamp
 */
data class Budget(
    val id: String,
    val categoryId: String,
    val amount: Long,
    val period: BudgetPeriod,
    val currency: Currency,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(categoryId.isNotBlank()) { "Category ID cannot be blank" }
        require(amount >= 0L) { "Budget amount cannot be negative, got: $amount" }
    }

    /**
     * True if budget amount is zero
     */
    val isZero: Boolean
        get() = amount == 0L

    /**
     * Get Money representation of budget amount
     */
    fun asMoney(): Money = Money(amount, currency)
}
