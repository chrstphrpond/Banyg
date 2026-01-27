package com.banyg.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Transaction domain model
 *
 * Represents a financial transaction (expense, income, or transfer).
 * Sign conventions:
 * - Negative amount = Expense/outflow
 * - Positive amount = Income/inflow
 * - Transfers: Two linked transactions with opposite signs
 *
 * @property id Unique identifier
 * @property accountId Account this transaction belongs to
 * @property date Transaction date
 * @property amount Money amount (negative for expense, positive for income)
 * @property merchant Merchant or payee name
 * @property memo Optional note/description
 * @property categoryId Category for this transaction
 * @property status Transaction status (pending, cleared, reconciled)
 * @property clearedAt Timestamp when transaction was cleared
 * @property transferId Optional ID linking transfer transactions
 * @property splits Optional list of splits if transaction is split across categories
 * @property createdAt Creation timestamp
 * @property updatedAt Last update timestamp
 */
data class Transaction(
    val id: String,
    val accountId: String,
    val date: LocalDate,
    val amount: Money,
    val merchant: String,
    val memo: String? = null,
    val categoryId: String? = null,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val clearedAt: Instant? = null,
    val transferId: String? = null,
    val splits: List<Split> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(merchant.isNotBlank()) { "Merchant cannot be blank" }
    }

    /**
     * True if this is an expense (negative amount)
     */
    val isExpense: Boolean
        get() = amount.isExpense

    /**
     * True if this is income (positive amount)
     */
    val isIncome: Boolean
        get() = amount.isIncome

    /**
     * True if this is part of a transfer
     */
    val isTransfer: Boolean
        get() = transferId != null

    /**
     * True if transaction has splits across multiple categories
     */
    val hasSplits: Boolean
        get() = splits.isNotEmpty()

    /**
     * True if transaction is cleared
     */
    val isCleared: Boolean
        get() = status == TransactionStatus.CLEARED || status == TransactionStatus.RECONCILED
}

/**
 * Transaction status
 */
enum class TransactionStatus {
    /** Pending/uncleared transaction */
    PENDING,

    /** Cleared by bank */
    CLEARED,

    /** Reconciled with bank statement */
    RECONCILED,

    /** Voided/cancelled transaction */
    VOID
}
