package com.banyg.domain.repository

import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Transaction Repository Interface
 *
 * Defines operations for transaction persistence.
 * Implementation in core/data layer.
 */
interface TransactionRepository {

    /**
     * Observe transactions by account
     */
    fun observeByAccount(accountId: String): Flow<List<Transaction>>

    /**
     * Observe transactions by account and status
     */
    fun observeByAccountAndStatus(accountId: String, status: TransactionStatus): Flow<List<Transaction>>

    /**
     * Observe transactions by date range
     */
    fun observeByDateRange(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Transaction>>

    /**
     * Observe transactions by category
     */
    fun observeByCategory(categoryId: String): Flow<List<Transaction>>

    /**
     * Observe pending transactions
     */
    fun observePendingTransactions(): Flow<List<Transaction>>

    /**
     * Observe recent transactions (all accounts)
     */
    fun observeRecentTransactions(limit: Int = 50): Flow<List<Transaction>>

    /**
     * Observe single transaction by ID
     */
    fun observeTransaction(id: String): Flow<Transaction?>

    /**
     * Get transaction by ID (one-shot)
     */
    suspend fun getTransaction(id: String): Transaction?

    /**
     * Get transactions by transfer ID
     */
    suspend fun getTransactionsByTransferId(transferId: String): List<Transaction>

    /**
     * Get transactions by account (one-shot)
     */
    suspend fun getTransactionsByAccount(accountId: String): List<Transaction>

    /**
     * Save transaction (insert or update)
     * Handles splits if transaction has them
     */
    suspend fun saveTransaction(transaction: Transaction)

    /**
     * Save multiple transactions
     */
    suspend fun saveTransactions(transactions: List<Transaction>)

    /**
     * Delete transaction
     */
    suspend fun deleteTransaction(id: String)

    /**
     * Delete transactions by transfer ID
     */
    suspend fun deleteByTransferId(transferId: String)

    /**
     * Update transaction status
     */
    suspend fun updateStatus(id: String, status: TransactionStatus)

    /**
     * Get sum of transactions by account
     */
    suspend fun getSumByAccount(accountId: String): Money?

    /**
     * Get sum of transactions by category in date range
     */
    suspend fun getSumByCategoryInRange(
        categoryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Money?

    /**
     * Get count of transactions for account
     */
    suspend fun getTransactionCount(accountId: String): Int

    /**
     * Search transactions by merchant or memo
     */
    fun searchTransactions(query: String, limit: Int = 50): Flow<List<Transaction>>
}
