package com.banyg.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Budget period representing a month/year combination
 *
 * Uses ISO-8601 year-month format (YYYY-MM) for storage and comparison.
 * Example: "2026-01" for January 2026
 *
 * @property yearMonth The year-month value
 */
data class BudgetPeriod(
    val yearMonth: YearMonth
) : Comparable<BudgetPeriod> {

    /**
     * Month key in YYYY-MM format for storage
     */
    val monthKey: String
        get() = yearMonth.format(MONTH_KEY_FORMATTER)

    /**
     * Year component
     */
    val year: Int
        get() = yearMonth.year

    /**
     * Month component (1-12)
     */
    val month: Int
        get() = yearMonth.monthValue

    /**
     * Get the first day of this period
     */
    fun startDate(): LocalDate = yearMonth.atDay(1)

    /**
     * Get the last day of this period
     */
    fun endDate(): LocalDate = yearMonth.atEndOfMonth()

    /**
     * Get the previous period (previous month)
     */
    fun previous(): BudgetPeriod = BudgetPeriod(yearMonth.minusMonths(1))

    /**
     * Get the next period (next month)
     */
    fun next(): BudgetPeriod = BudgetPeriod(yearMonth.plusMonths(1))

    override fun compareTo(other: BudgetPeriod): Int {
        return yearMonth.compareTo(other.yearMonth)
    }

    companion object {
        private val MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")

        /**
         * Create from year and month
         */
        fun of(year: Int, month: Int): BudgetPeriod {
            return BudgetPeriod(YearMonth.of(year, month))
        }

        /**
         * Create from month key string (YYYY-MM format)
         * @throws IllegalArgumentException if format is invalid
         */
        fun fromMonthKey(monthKey: String): BudgetPeriod {
            return try {
                BudgetPeriod(YearMonth.parse(monthKey, MONTH_KEY_FORMATTER))
            } catch (e: DateTimeParseException) {
                throw IllegalArgumentException(
                    "Invalid month key format: '$monthKey'. Expected: YYYY-MM",
                    e
                )
            }
        }

        /**
         * Create for current month
         */
        fun current(): BudgetPeriod {
            return BudgetPeriod(YearMonth.now())
        }

        /**
         * Create from LocalDate (uses year and month only)
         */
        fun fromDate(date: LocalDate): BudgetPeriod {
            return BudgetPeriod(YearMonth.from(date))
        }
    }
}
