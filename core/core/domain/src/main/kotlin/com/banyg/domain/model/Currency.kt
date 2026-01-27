package com.banyg.domain.model

/**
 * Currency representation for money values
 *
 * Supports common currencies with their symbols and codes.
 * Immutable value object following domain-driven design.
 */
data class Currency(
    val code: String,
    val symbol: String,
    val name: String,
    val minorUnitsPerMajor: Int = 100
) {
    init {
        require(code.length == 3) { "Currency code must be 3 characters (ISO 4217)" }
        require(minorUnitsPerMajor > 0) { "Minor units per major must be positive" }
    }

    companion object {
        /**
         * Philippine Peso - ₱
         */
        val PHP = Currency(
            code = "PHP",
            symbol = "₱",
            name = "Philippine Peso",
            minorUnitsPerMajor = 100
        )

        /**
         * United States Dollar - $
         */
        val USD = Currency(
            code = "USD",
            symbol = "$",
            name = "US Dollar",
            minorUnitsPerMajor = 100
        )

        /**
         * Euro - €
         */
        val EUR = Currency(
            code = "EUR",
            symbol = "€",
            name = "Euro",
            minorUnitsPerMajor = 100
        )

        /**
         * Japanese Yen - ¥
         * Note: Yen has no minor units (1 yen = 1 unit)
         */
        val JPY = Currency(
            code = "JPY",
            symbol = "¥",
            name = "Japanese Yen",
            minorUnitsPerMajor = 1
        )

        /**
         * Get currency by code
         */
        fun fromCode(code: String): Currency? = when (code.uppercase()) {
            "PHP" -> PHP
            "USD" -> USD
            "EUR" -> EUR
            "JPY" -> JPY
            else -> null
        }

        /**
         * All supported currencies
         */
        val supportedCurrencies = listOf(PHP, USD, EUR, JPY)
    }
}
