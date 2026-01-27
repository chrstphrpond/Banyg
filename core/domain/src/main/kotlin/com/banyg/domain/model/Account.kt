package com.banyg.domain.model

import java.time.Instant

/**
 * Account domain model
 *
 * Represents a financial account (bank account, cash, credit card, etc.)
 * Immutable value object with no Android dependencies.
 *
 * @property id Unique identifier
 * @property name Account name (e.g., "BDO Savings", "Cash Wallet")
 * @property type Account type
 * @property currency Currency for this account
 * @property openingBalance Initial balance in minor units (centavos)
 * @property currentBalance Current balance (calculated from transactions)
 * @property isArchived True if account is archived (hidden from main views)
 * @property createdAt Creation timestamp
 * @property updatedAt Last update timestamp
 */
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: Currency,
    val openingBalance: Money,
    val currentBalance: Money,
    val isArchived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(name.isNotBlank()) { "Account name cannot be blank" }
        require(openingBalance.currency == currency) {
            "Opening balance currency must match account currency"
        }
        require(currentBalance.currency == currency) {
            "Current balance currency must match account currency"
        }
    }
}

/**
 * Account types
 */
enum class AccountType {
    /** Bank checking account */
    CHECKING,

    /** Bank savings account */
    SAVINGS,

    /** Credit card */
    CREDIT_CARD,

    /** Physical cash */
    CASH,

    /** Digital wallet (GCash, PayMaya, etc.) */
    E_WALLET,

    /** Investment account */
    INVESTMENT,

    /** Loan account */
    LOAN,

    /** Other account type */
    OTHER
}
