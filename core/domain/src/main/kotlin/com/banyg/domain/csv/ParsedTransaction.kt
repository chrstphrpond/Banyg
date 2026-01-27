package com.banyg.domain.csv

import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import java.time.LocalDate
import java.util.UUID

/**
 * A parsed transaction ready for import
 *
 * @property id Unique identifier for this parsed transaction
 * @property date Transaction date
 * @property amount Money amount (negative for expense, positive for income)
 * @property merchant Normalized merchant name
 * @property rawDescription Original description from CSV
 * @property rawRowIndex Original row index in CSV file (for reference)
 */
data class ParsedTransaction(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val amount: Money,
    val merchant: String,
    val rawDescription: String,
    val rawRowIndex: Int
) {
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
     * Generate fingerprint for duplicate detection
     * Format: "yyyy-MM-dd|amount|normalized_merchant"
     */
    fun fingerprint(): String {
        return "${date}|${amount.minorUnits}|${merchant.lowercase().trim()}"
    }

    companion object {
        /**
         * Create ParsedTransaction with Long amount (minor units)
         */
        fun fromMinorUnits(
            date: LocalDate,
            amountMinor: Long,
            currency: Currency,
            merchant: String,
            rawDescription: String,
            rawRowIndex: Int
        ): ParsedTransaction {
            return ParsedTransaction(
                date = date,
                amount = Money(amountMinor, currency),
                merchant = merchant,
                rawDescription = rawDescription,
                rawRowIndex = rawRowIndex
            )
        }
    }
}
