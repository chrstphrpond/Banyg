package com.banyg.ui.format

import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Money Formatter - UI Edge Only
 *
 * CRITICAL: Only use in core/ui module for display purposes.
 * Domain and data layers must NEVER use these formatters.
 * All domain logic operates on Long minor units.
 *
 * Formatting rules:
 * - Always format with currency symbol
 * - Use locale-appropriate number formatting
 * - Show sign for expenses (negative)
 * - Handle zero specially
 */

/**
 * Format Money as string with currency symbol
 * Example: Money(12345, PHP) -> "₱123.45"
 * Example: Money(-5000, USD) -> "-$50.00"
 */
fun Money.format(
    showSign: Boolean = true,
    locale: Locale = Locale.getDefault()
): String {
    val isNegative = minorUnits < 0
    val absoluteMinor = kotlin.math.abs(minorUnits)

    // Convert to major units
    val majorUnits = absoluteMinor.toDouble() / currency.minorUnitsPerMajor

    // Format with decimal places based on currency
    val formatted = when {
        currency.minorUnitsPerMajor == 1 -> {
            // Currencies with no minor units (like JPY)
            DecimalFormat("#,##0").format(majorUnits)
        }
        else -> {
            // Currencies with minor units (2 decimal places)
            DecimalFormat("#,##0.00").format(majorUnits)
        }
    }

    // Build final string
    val sign = if (isNegative && showSign) "-" else ""
    return "$sign${currency.symbol}$formatted"
}

/**
 * Format Money without currency symbol
 * Example: Money(12345, PHP) -> "123.45"
 * Example: Money(-5000, USD) -> "-50.00"
 */
fun Money.formatWithoutSymbol(
    showSign: Boolean = true
): String {
    val isNegative = minorUnits < 0
    val absoluteMinor = kotlin.math.abs(minorUnits)

    // Convert to major units
    val majorUnits = absoluteMinor.toDouble() / currency.minorUnitsPerMajor

    // Format with decimal places based on currency
    val formatted = when {
        currency.minorUnitsPerMajor == 1 -> {
            DecimalFormat("#,##0").format(majorUnits)
        }
        else -> {
            DecimalFormat("#,##0.00").format(majorUnits)
        }
    }

    val sign = if (isNegative && showSign) "-" else ""
    return "$sign$formatted"
}

/**
 * Format Money for compact display (e.g., "₱1.2K", "₱1.5M")
 * Used in charts and summaries where space is limited
 */
fun Money.formatCompact(): String {
    val isNegative = minorUnits < 0
    val absoluteMinor = kotlin.math.abs(minorUnits)
    val majorUnits = absoluteMinor.toDouble() / currency.minorUnitsPerMajor

    val formatted = when {
        majorUnits >= 1_000_000_000 -> {
            String.format("%.1fB", majorUnits / 1_000_000_000)
        }
        majorUnits >= 1_000_000 -> {
            String.format("%.1fM", majorUnits / 1_000_000)
        }
        majorUnits >= 1_000 -> {
            String.format("%.1fK", majorUnits / 1_000)
        }
        else -> {
            when {
                currency.minorUnitsPerMajor == 1 -> {
                    String.format("%.0f", majorUnits)
                }
                else -> {
                    String.format("%.2f", majorUnits)
                }
            }
        }
    }

    val sign = if (isNegative) "-" else ""
    return "$sign${currency.symbol}$formatted"
}

/**
 * Format Money as percentage of total
 * Example: Money(5000, PHP) of Money(10000, PHP) -> "50.0%"
 */
fun Money.formatAsPercentageOf(total: Money): String {
    require(currency == total.currency) {
        "Currencies must match: ${currency.code} and ${total.currency.code}"
    }

    if (total.minorUnits == 0L) return "0.0%"

    val percentage = (minorUnits.toDouble() / total.minorUnits.toDouble()) * 100
    return String.format("%.1f%%", percentage)
}

/**
 * Extension function for Long minor units formatting
 * Convert Long minor units directly to formatted string
 */
fun Long.formatAsMinorUnits(currency: Currency): String {
    return Money(this, currency).format()
}

/**
 * Parse formatted money string back to Money (for input fields)
 * Handles: "123.45", "1,234.56", "₱123.45", "-50.00"
 *
 * Returns null if parsing fails.
 * This is ONLY for UI input parsing, not domain logic.
 */
fun String.parseToMoney(currency: Currency): Money? {
    return try {
        // Remove currency symbol, commas, spaces
        val cleaned = this
            .replace(currency.symbol, "")
            .replace(",", "")
            .replace(" ", "")
            .trim()

        if (cleaned.isEmpty()) return Money.zero(currency)

        // Parse as double to handle decimals
        val value = cleaned.toDoubleOrNull() ?: return null

        // Convert to minor units
        val minorUnits = (value * currency.minorUnitsPerMajor).toLong()

        Money(minorUnits, currency)
    } catch (e: Exception) {
        null
    }
}

/**
 * Format Money with explicit sign (always show + or -)
 * Used for income/expense display
 */
fun Money.formatWithExplicitSign(): String {
    val formatted = format(showSign = false)
    return when {
        minorUnits > 0 -> "+$formatted"
        minorUnits < 0 -> "-$formatted"
        else -> formatted
    }
}

/**
 * Format Money colored by sign (for Compose Text color selection)
 * Returns pair of (formattedString, isNegative)
 */
fun Money.formatWithSignIndicator(): Pair<String, Boolean> {
    return Pair(format(), minorUnits < 0)
}

/**
 * Get major units as Double (for internal UI calculations ONLY)
 * WARNING: Only use for display calculations, never for domain logic
 */
@Deprecated(
    message = "Only use for UI calculations, never for domain logic",
    level = DeprecationLevel.WARNING
)
fun Money.toMajorUnitsDouble(): Double {
    return minorUnits.toDouble() / currency.minorUnitsPerMajor
}
