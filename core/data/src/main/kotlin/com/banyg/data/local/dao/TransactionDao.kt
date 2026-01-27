package com.banyg.data.local.dao

import androidx.room.*
import com.banyg.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Transaction DAO
 *
 * Data access object for transactions table.
 * Uses Flow for reactive queries, suspend for one-shot operations.
 */
@Dao
interface TransactionDao {

    /**
     * Observe transactions by account
     */
    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY date DESC, created_at DESC")
    fun observeByAccount(accountId: String): Flow<List<TransactionEntity>>

    /**
     * Observe transactions by account and status
     */
    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND status = :status ORDER BY date DESC")
    fun observeByAccountAndStatus(accountId: String, status: String): Flow<List<TransactionEntity>>

    /**
     * Observe transactions by date range
     */
    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun observeByDateRange(accountId: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    /**
     * Observe transactions by category
     */
    @Query("SELECT * FROM transactions WHERE category_id = :categoryId ORDER BY date DESC")
    fun observeByCategory(categoryId: String): Flow<List<TransactionEntity>>

    /**
     * Observe pending transactions
     */
    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY date DESC")
    fun observePendingTransactions(): Flow<List<TransactionEntity>>

    /**
     * Observe recent transactions (all accounts)
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int = 50): Flow<List<TransactionEntity>>

    /**
     * Observe single transaction by ID
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeTransaction(id: String): Flow<TransactionEntity?>

    /**
     * Get transaction by ID (one-shot)
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: String): TransactionEntity?

    /**
     * Get transactions by transfer ID
     */
    @Query("SELECT * FROM transactions WHERE transfer_id = :transferId")
    suspend fun getTransactionsByTransferId(transferId: String): List<TransactionEntity>

    /**
     * Get transactions by account (one-shot)
     */
    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY date DESC")
    suspend fun getTransactionsByAccount(accountId: String): List<TransactionEntity>

    /**
     * Get transactions by category (one-shot)
     */
    @Query("SELECT * FROM transactions WHERE category_id = :categoryId ORDER BY date DESC")
    suspend fun getTransactionsByCategory(categoryId: String): List<TransactionEntity>

    /**
     * Insert transaction
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    /**
     * Insert multiple transactions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    /**
     * Update transaction
     */
    @Update
    suspend fun update(transaction: TransactionEntity)

    /**
     * Delete transaction
     */
    @Delete
    suspend fun delete(transaction: TransactionEntity)

    /**
     * Delete transaction by ID
     */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete transactions by transfer ID (for canceling transfers)
     */
    @Query("DELETE FROM transactions WHERE transfer_id = :transferId")
    suspend fun deleteByTransferId(transferId: String)

    /**
     * Update transaction status
     */
    @Query("UPDATE transactions SET status = :status, cleared_at = :clearedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, clearedAt: Long?, updatedAt: Long)

    /**
     * Get sum of transactions by account
     */
    @Query("SELECT SUM(amount_minor) FROM transactions WHERE account_id = :accountId AND status != 'VOID'")
    suspend fun getSumByAccount(accountId: String): Long?

    /**
     * Get sum of transactions by category in date range
     */
    @Query("SELECT SUM(amount_minor) FROM transactions WHERE category_id = :categoryId AND date BETWEEN :startDate AND :endDate AND status != 'VOID'")
    suspend fun getSumByCategoryInRange(categoryId: String, startDate: Long, endDate: Long): Long?

    /**
     * Get count of transactions
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId")
    suspend fun getTransactionCount(accountId: String): Int

    /**
     * Search transactions by merchant
     */
    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :query || '%' OR memo LIKE '%' || :query || '%' ORDER BY date DESC LIMIT :limit")
    fun searchTransactions(query: String, limit: Int = 50): Flow<List<TransactionEntity>>
}
