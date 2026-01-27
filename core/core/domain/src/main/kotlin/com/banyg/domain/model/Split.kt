package com.banyg.domain.model

/**
 * Split domain model
 *
 * Represents a split of a transaction amount across multiple categories.
 * Example: A ₱500 grocery transaction split into:
 * - ₱300 for Food
 * - ₱200 for Household items
 *
 * IMPORTANT: All splits for a transaction must sum to transaction amount.
 *
 * @property transactionId Transaction this split belongs to
 * @property lineId Unique line identifier within transaction (0, 1, 2, ...)
 * @property categoryId Category for this split
 * @property amount Split amount in minor units
 * @property memo Optional note for this split
 */
data class Split(
    val transactionId: String,
    val lineId: Int,
    val categoryId: String,
    val amount: Money,
    val memo: String? = null
) {
    init {
        require(lineId >= 0) { "Line ID must be non-negative" }
        require(!amount.isZero) { "Split amount cannot be zero" }
    }

    /**
     * True if this is an expense split (negative amount)
     */
    val isExpense: Boolean
        get() = amount.isExpense

    /**
     * True if this is an income split (positive amount)
     */
    val isIncome: Boolean
        get() = amount.isIncome
}
