package com.banyg.domain.model

/**
 * Money value object using Long for minor units (centavos/cents)
 *
 * CRITICAL RULES:
 * - Never use Float or Double for money amounts
 * - All calculations must use Long (minor units)
 * - Sign conventions:
 *   - Negative: Expense/outflow
 *   - Positive: Income/inflow
 *   - Zero: No change
 *
 * Examples:
 * - Money(12345, Currency.PHP) = ₱123.45
 * - Money(-5000, Currency.USD) = -$50.00 (expense)
 * - Money(10000, Currency.USD) = $100.00 (income)
 *
 * @property minorUnits Amount in minor units (centavos/cents). MUST be Long.
 * @property currency The currency of this money amount
 */
data class Money(
    val minorUnits: Long,
    val currency: Currency
) : Comparable<Money> {

    /**
     * True if this is an expense (negative amount)
     */
    val isExpense: Boolean
        get() = minorUnits < 0

    /**
     * True if this is income (positive amount)
     */
    val isIncome: Boolean
        get() = minorUnits > 0

    /**
     * True if amount is zero
     */
    val isZero: Boolean
        get() = minorUnits == 0L

    /**
     * Absolute value of this money amount
     */
    fun abs(): Money = Money(kotlin.math.abs(minorUnits), currency)

    /**
     * Negate this money amount
     */
    operator fun unaryMinus(): Money = Money(-minorUnits, currency)

    /**
     * Compare money amounts (must have same currency)
     */
    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    /**
     * Ensure both money amounts have the same currency
     */
    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Cannot operate on different currencies: ${currency.code} and ${other.currency.code}"
        }
    }

    companion object {
        /**
         * Zero amount for given currency
         */
        fun zero(currency: Currency) = Money(0L, currency)

        /**
         * Create Money from major units (e.g., dollars, pesos)
         * @param majorUnits Amount in major units (can have decimals)
         * @param currency Currency
         * @return Money with correct minor units
         */
        fun fromMajor(majorUnits: Double, currency: Currency): Money {
            val minorUnits = (majorUnits * currency.minorUnitsPerMajor).toLong()
            return Money(minorUnits, currency)
        }

        /**
         * Create Money from major units (Long)
         * @param majorUnits Amount in major units (whole numbers)
         * @param currency Currency
         * @return Money with correct minor units
         */
        fun fromMajor(majorUnits: Long, currency: Currency): Money {
            return Money(majorUnits * currency.minorUnitsPerMajor, currency)
        }
    }
}
