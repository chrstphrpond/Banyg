package com.banyg.data.repository

import com.banyg.data.local.dao.AccountDao
import com.banyg.data.mapper.AccountMapper
import com.banyg.domain.model.Account
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Account Repository Implementation
 *
 * Implements AccountRepository using Room DAO and mapper.
 * Converts between domain models and Room entities.
 */
class AccountRepositoryImpl(
    private val dao: AccountDao,
    private val mapper: AccountMapper = AccountMapper()
) : AccountRepository {

    override fun observeActiveAccounts(): Flow<List<Account>> {
        return dao.observeActiveAccounts()
            .map { mapper.toDomainList(it) }
    }

    override fun observeAllAccounts(): Flow<List<Account>> {
        return dao.observeAllAccounts()
            .map { mapper.toDomainList(it) }
    }

    override fun observeAccountsByCurrency(currency: Currency): Flow<List<Account>> {
        return dao.observeAccountsByCurrency(currency.code)
            .map { mapper.toDomainList(it) }
    }

    override fun observeAccount(id: String): Flow<Account?> {
        return dao.observeAccount(id)
            .map { it?.let { mapper.toDomain(it) } }
    }

    override suspend fun getAccount(id: String): Account? {
        return dao.getAccount(id)?.let { mapper.toDomain(it) }
    }

    override suspend fun getAllAccounts(): List<Account> {
        return mapper.toDomainList(dao.getAllAccounts())
    }

    override suspend fun saveAccount(account: Account) {
        dao.insert(mapper.toEntity(account))
    }

    override suspend fun saveAccounts(accounts: List<Account>) {
        dao.insertAll(mapper.toEntityList(accounts))
    }

    override suspend fun deleteAccount(id: String) {
        dao.deleteById(id)
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        val now = Instant.now().toEpochMilli()
        dao.setArchived(id, archived, now)
    }

    override suspend fun updateBalance(id: String, balance: Money) {
        val now = Instant.now().toEpochMilli()
        dao.updateBalance(id, balance.minorUnits, now)
    }

    override suspend fun getActiveAccountCount(): Int {
        return dao.getActiveAccountCount()
    }
}
