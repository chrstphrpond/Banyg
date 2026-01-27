package com.banyg.data.local.dao

import androidx.room.*
import com.banyg.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Account DAO
 *
 * Data access object for accounts table.
 * Uses Flow for reactive queries, suspend for one-shot operations.
 */
@Dao
interface AccountDao {

    /**
     * Observe all accounts (excluding archived)
     */
    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY name ASC")
    fun observeActiveAccounts(): Flow<List<AccountEntity>>

    /**
     * Observe all accounts (including archived)
     */
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun observeAllAccounts(): Flow<List<AccountEntity>>

    /**
     * Observe accounts by currency
     */
    @Query("SELECT * FROM accounts WHERE currency_code = :currencyCode AND is_archived = 0 ORDER BY name ASC")
    fun observeAccountsByCurrency(currencyCode: String): Flow<List<AccountEntity>>

    /**
     * Observe single account by ID
     */
    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeAccount(id: String): Flow<AccountEntity?>

    /**
     * Get account by ID (one-shot)
     */
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccount(id: String): AccountEntity?

    /**
     * Get all accounts (one-shot)
     */
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    suspend fun getAllAccounts(): List<AccountEntity>

    /**
     * Insert account
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    /**
     * Insert multiple accounts
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    /**
     * Update account
     */
    @Update
    suspend fun update(account: AccountEntity)

    /**
     * Update account balance (for efficient balance updates)
     */
    @Query("UPDATE accounts SET current_balance_minor = :balanceMinor, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: String, balanceMinor: Long, updatedAt: Long)

    /**
     * Delete account
     */
    @Delete
    suspend fun delete(account: AccountEntity)

    /**
     * Delete account by ID
     */
    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Archive account
     */
    @Query("UPDATE accounts SET is_archived = :archived, updated_at = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)

    /**
     * Get count of accounts
     */
    @Query("SELECT COUNT(*) FROM accounts WHERE is_archived = 0")
    suspend fun getActiveAccountCount(): Int
}
