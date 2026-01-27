package com.banyg.domain.repository

import com.banyg.domain.model.Account
import com.banyg.domain.model.Currency
import kotlinx.coroutines.flow.Flow

/**
 * Account Repository Interface
 *
 * Defines operations for account persistence.
 * Implementation in core/data layer.
 */
interface AccountRepository {

    /**
     * Observe all active accounts (excluding archived)
     */
    fun observeActiveAccounts(): Flow<List<Account>>

    /**
     * Observe all accounts (including archived)
     */
    fun observeAllAccounts(): Flow<List<Account>>

    /**
     * Observe accounts by currency
     */
    fun observeAccountsByCurrency(currency: Currency): Flow<List<Account>>

    /**
     * Observe single account by ID
     */
    fun observeAccount(id: String): Flow<Account?>

    /**
     * Get account by ID (one-shot)
     */
    suspend fun getAccount(id: String): Account?

    /**
     * Get all accounts (one-shot)
     */
    suspend fun getAllAccounts(): List<Account>

    /**
     * Save account (insert or update)
     */
    suspend fun saveAccount(account: Account)

    /**
     * Save multiple accounts
     */
    suspend fun saveAccounts(accounts: List<Account>)

    /**
     * Delete account
     */
    suspend fun deleteAccount(id: String)

    /**
     * Archive/unarchive account
     */
    suspend fun setArchived(id: String, archived: Boolean)

    /**
     * Update account balance
     */
    suspend fun updateBalance(id: String, balance: com.banyg.domain.model.Money)

    /**
     * Get count of active accounts
     */
    suspend fun getActiveAccountCount(): Int
}
