package com.banyg.data.repository

import com.banyg.data.local.dao.AccountDao
import com.banyg.data.local.entity.AccountEntity
import com.banyg.data.mapper.AccountMapper
import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountRepositoryImplTest {

    @Test
    fun `observeActiveAccounts maps entities to domain`() = runTest {
        val dao = FakeAccountDao()
        val repository = AccountRepositoryImpl(dao, AccountMapper())

        val entity = AccountEntity(
            id = "acc-1",
            name = "Primary",
            type = AccountType.CHECKING.name,
            currencyCode = Currency.PHP.code,
            openingBalanceMinor = 10_000L,
            currentBalanceMinor = 12_500L,
            isArchived = false,
            createdAt = 1_700_000_000_000,
            updatedAt = 1_700_000_000_000
        )

        dao.insert(entity)

        val accounts = repository.observeActiveAccounts().first()

        assertEquals(1, accounts.size)
        assertEquals("acc-1", accounts.first().id)
        assertEquals(Money(12_500L, Currency.PHP), accounts.first().currentBalance)
    }
}

private class FakeAccountDao : AccountDao {
    private val allAccounts = mutableListOf<AccountEntity>()
    private val allFlow = MutableStateFlow<List<AccountEntity>>(emptyList())
    private val activeFlow = MutableStateFlow<List<AccountEntity>>(emptyList())

    override fun observeActiveAccounts(): Flow<List<AccountEntity>> = activeFlow

    override fun observeAllAccounts(): Flow<List<AccountEntity>> = allFlow

    override fun observeAccountsByCurrency(currencyCode: String): Flow<List<AccountEntity>> {
        return activeFlow.map { accounts -> accounts.filter { it.currencyCode == currencyCode } }
    }

    override fun observeAccount(id: String): Flow<AccountEntity?> {
        return allFlow.map { accounts -> accounts.firstOrNull { it.id == id } }
    }

    override suspend fun getAccount(id: String): AccountEntity? {
        return allAccounts.firstOrNull { it.id == id }
    }

    override suspend fun getAllAccounts(): List<AccountEntity> = allAccounts.toList()

    override suspend fun insert(account: AccountEntity) {
        allAccounts.removeAll { it.id == account.id }
        allAccounts.add(account)
        emit()
    }

    override suspend fun insertAll(accounts: List<AccountEntity>) {
        accounts.forEach { insert(it) }
    }

    override suspend fun update(account: AccountEntity) {
        insert(account)
    }

    override suspend fun updateBalance(id: String, balanceMinor: Long, updatedAt: Long) {
        val current = allAccounts.firstOrNull { it.id == id } ?: return
        insert(
            current.copy(
                currentBalanceMinor = balanceMinor,
                updatedAt = updatedAt
            )
        )
    }

    override suspend fun delete(account: AccountEntity) {
        deleteById(account.id)
    }

    override suspend fun deleteById(id: String) {
        allAccounts.removeAll { it.id == id }
        emit()
    }

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long) {
        val current = allAccounts.firstOrNull { it.id == id } ?: return
        insert(
            current.copy(
                isArchived = archived,
                updatedAt = updatedAt
            )
        )
    }

    override suspend fun getActiveAccountCount(): Int {
        return allAccounts.count { !it.isArchived }
    }

    private fun emit() {
        allFlow.value = allAccounts.toList()
        activeFlow.value = allAccounts.filter { !it.isArchived }
    }
}
