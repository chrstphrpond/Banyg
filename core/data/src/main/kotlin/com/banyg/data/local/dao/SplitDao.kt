package com.banyg.data.local.dao

import androidx.room.*
import com.banyg.data.local.entity.SplitEntity
import kotlinx.coroutines.flow.Flow

/**
 * Split DAO
 *
 * Data access object for splits table.
 * Uses Flow for reactive queries, suspend for one-shot operations.
 */
@Dao
interface SplitDao {

    /**
     * Observe splits by transaction
     */
    @Query("SELECT * FROM splits WHERE transaction_id = :transactionId ORDER BY line_id ASC")
    fun observeSplitsByTransaction(transactionId: String): Flow<List<SplitEntity>>

    /**
     * Observe splits by category
     */
    @Query("SELECT * FROM splits WHERE category_id = :categoryId")
    fun observeSplitsByCategory(categoryId: String): Flow<List<SplitEntity>>

    /**
     * Get splits by transaction (one-shot)
     */
    @Query("SELECT * FROM splits WHERE transaction_id = :transactionId ORDER BY line_id ASC")
    suspend fun getSplitsByTransaction(transactionId: String): List<SplitEntity>

    /**
     * Get single split
     */
    @Query("SELECT * FROM splits WHERE transaction_id = :transactionId AND line_id = :lineId")
    suspend fun getSplit(transactionId: String, lineId: Int): SplitEntity?

    /**
     * Insert split
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(split: SplitEntity)

    /**
     * Insert multiple splits
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<SplitEntity>)

    /**
     * Update split
     */
    @Update
    suspend fun update(split: SplitEntity)

    /**
     * Delete split
     */
    @Delete
    suspend fun delete(split: SplitEntity)

    /**
     * Delete splits by transaction
     */
    @Query("DELETE FROM splits WHERE transaction_id = :transactionId")
    suspend fun deleteByTransaction(transactionId: String)

    /**
     * Insert transaction with splits atomically
     */
    @Transaction
    suspend fun insertTransactionWithSplits(
        transactionId: String,
        splits: List<SplitEntity>
    ) {
        // First delete any existing splits
        deleteByTransaction(transactionId)
        // Then insert new splits
        if (splits.isNotEmpty()) {
            insertAll(splits)
        }
    }

    /**
     * Get sum of splits by category in date range
     * Joins with transactions to filter by date
     */
    @Query("""
        SELECT SUM(s.amount_minor)
        FROM splits s
        INNER JOIN transactions t ON s.transaction_id = t.id
        WHERE s.category_id = :categoryId
        AND t.date BETWEEN :startDate AND :endDate
        AND t.status != 'VOID'
    """)
    suspend fun getSumByCategoryInRange(categoryId: String, startDate: Long, endDate: Long): Long?

    /**
     * Validate splits sum to transaction amount
     */
    @Query("""
        SELECT SUM(amount_minor)
        FROM splits
        WHERE transaction_id = :transactionId
    """)
    suspend fun getSplitsSum(transactionId: String): Long?
}
