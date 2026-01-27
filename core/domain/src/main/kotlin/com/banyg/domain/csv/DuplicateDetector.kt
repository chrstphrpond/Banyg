package com.banyg.domain.csv

import com.banyg.domain.model.Transaction
import java.time.LocalDate

/**
 * Detects duplicate transactions between parsed CSV transactions and existing transactions
 */
class DuplicateDetector {

    /**
     * Result of duplicate check
     *
     * @property isDuplicate True if this is a potential duplicate
     * @property confidence Match confidence (0.0 - 1.0)
     * @property matchedTransactionId ID of matching existing transaction
     */
    data class DuplicateCheckResult(
        val isDuplicate: Boolean,
        val confidence: Float,
        val matchedTransactionId: String? = null
    )

    /**
     * Check if a parsed transaction is a duplicate of any existing transaction
     *
     * @param parsed The parsed transaction from CSV
     * @param existingTransactions List of existing transactions to check against
     * @return Duplicate check result
     */
    fun checkDuplicate(
        parsed: ParsedTransaction,
        existingTransactions: List<Transaction>
    ): DuplicateCheckResult {
        // First check for exact match
        val exactMatch = existingTransactions.find { existing ->
            isExactMatch(parsed, existing)
        }

        if (exactMatch != null) {
            return DuplicateCheckResult(
                isDuplicate = true,
                confidence = 1.0f,
                matchedTransactionId = exactMatch.id
            )
        }

        // Check for fuzzy matches
        val fuzzyMatches = existingTransactions.mapNotNull { existing ->
            val similarity = calculateSimilarity(parsed, existing)
            if (similarity >= DUPLICATE_THRESHOLD) {
                existing to similarity
            } else null
        }

        return if (fuzzyMatches.isNotEmpty()) {
            val bestMatch = fuzzyMatches.maxByOrNull { it.second }!!
            DuplicateCheckResult(
                isDuplicate = true,
                confidence = bestMatch.second,
                matchedTransactionId = bestMatch.first.id
            )
        } else {
            DuplicateCheckResult(isDuplicate = false, confidence = 0f)
        }
    }

    /**
     * Check multiple parsed transactions against existing ones
     *
     * @param parsedTransactions List of parsed transactions from CSV
     * @param existingTransactions List of existing transactions in database
     * @return List of duplicate check results (same order as parsedTransactions)
     */
    fun checkDuplicates(
        parsedTransactions: List<ParsedTransaction>,
        existingTransactions: List<Transaction>
    ): List<Pair<ParsedTransaction, DuplicateCheckResult>> {
        return parsedTransactions.map { parsed ->
            parsed to checkDuplicate(parsed, existingTransactions)
        }
    }

    /**
     * Find duplicates within a batch of parsed transactions (e.g., duplicate rows in CSV)
     *
     * @param parsedTransactions List of parsed transactions
     * @return List of transaction IDs that are duplicates within the batch
     */
    fun findInternalDuplicates(parsedTransactions: List<ParsedTransaction>): Set<String> {
        val seen = mutableSetOf<String>() // fingerprints
        val duplicates = mutableSetOf<String>()

        parsedTransactions.forEach { transaction ->
            val fingerprint = transaction.fingerprint()
            if (fingerprint in seen) {
                duplicates.add(transaction.id)
            } else {
                seen.add(fingerprint)
            }
        }

        return duplicates
    }

    /**
     * Exact match: same date, same amount, same merchant (case-insensitive)
     */
    private fun isExactMatch(parsed: ParsedTransaction, existing: Transaction): Boolean {
        return parsed.date == existing.date &&
               parsed.amount.minorUnits == existing.amount.minorUnits &&
               parsed.merchant.equals(existing.merchant, ignoreCase = true)
    }

    /**
     * Calculate similarity score between parsed and existing transaction
     *
     * Score components:
     * - Date similarity (within 3 days): up to 0.4
     * - Amount exact match: 0.4
     * - Merchant string similarity: up to 0.2
     */
    private fun calculateSimilarity(parsed: ParsedTransaction, existing: Transaction): Float {
        var score = 0f

        // Date similarity (within 3 days)
        val daysDiff = kotlin.math.abs(
            parsed.date.toEpochDay() - existing.date.toEpochDay()
        )
        if (daysDiff <= DATE_TOLERANCE_DAYS) {
            score += 0.4f * (1 - daysDiff.toFloat() / DATE_TOLERANCE_DAYS)
        }

        // Amount similarity
        if (parsed.amount.minorUnits == existing.amount.minorUnits) {
            score += 0.4f
        }

        // Merchant similarity
        val merchantScore = stringSimilarity(
            parsed.merchant.lowercase(),
            existing.merchant.lowercase()
        )
        score += 0.2f * merchantScore

        return score
    }

    /**
     * Calculate string similarity using Levenshtein distance
     *
     * @return Similarity score between 0.0 and 1.0
     */
    private fun stringSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val distance = levenshteinDistance(s1, s2)
        val maxLen = kotlin.math.max(s1.length, s2.length)
        return 1f - (distance.toFloat() / maxLen)
    }

    /**
     * Calculate Levenshtein edit distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    kotlin.math.min(
                        kotlin.math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    ) + 1
                }
            }
        }

        return dp[s1.length][s2.length]
    }

    companion object {
        // Minimum confidence to consider as duplicate
        private const val DUPLICATE_THRESHOLD = 0.75f

        // Date tolerance for fuzzy matching (days)
        private const val DATE_TOLERANCE_DAYS = 3
    }
}
