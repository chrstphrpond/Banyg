package com.banyg.data.repository

import com.banyg.data.local.dao.SplitDao
import com.banyg.data.local.dao.TransactionDao
import com.banyg.data.local.entity.SplitEntity
import com.banyg.data.local.entity.TransactionEntity
import com.banyg.data.mapper.SplitMapper
import com.banyg.data.mapper.TransactionMapper
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TransactionRepositoryImplTest {

    @Test
    fun `saveTransaction inserts and clears splits when no splits`() = runTest {
        val transactionDao = FakeTransactionDao()
        val splitDao = FakeSplitDao()
        val repository = TransactionRepositoryImpl(
            transactionDao = transactionDao,
            splitDao = splitDao,
            transactionMapper = TransactionMapper(),
            splitMapper = SplitMapper()
        )

        val transaction = Transaction(
            id = "txn-1",
            accountId = "acc-1",
            date = LocalDate.parse("2026-01-27"),
            amount = Money(-2_500L, Currency.PHP),
            merchant = "Coffee",
            memo = null,
            categoryId = null,
            status = TransactionStatus.PENDING,
            clearedAt = null,
            transferId = null,
            splits = emptyList(),
            createdAt = Instant.parse("2026-01-27T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-27T00:00:00Z")
        )

        repository.saveTransaction(transaction)

        assertEquals(1, transactionDao.transactions.size)
        assertEquals(listOf("txn-1"), splitDao.deletedTransactions)
    }

    @Test
    fun `observePendingTransactions maps pending entities`() = runTest {
        val transactionDao = FakeTransactionDao()
        val splitDao = FakeSplitDao()
        val repository = TransactionRepositoryImpl(
            transactionDao = transactionDao,
            splitDao = splitDao,
            transactionMapper = TransactionMapper(),
            splitMapper = SplitMapper()
        )

        transactionDao.insert(
            TransactionEntity(
                id = "txn-1",
                accountId = "acc-1",
                date = 20260127L,
                amountMinor = -5000L,
                currencyCode = Currency.PHP.code,
                merchant = "Lunch",
                memo = null,
                categoryId = null,
                status = TransactionStatus.PENDING.name,
                clearedAt = null,
                transferId = null,
                createdAt = 1_700_000_000_000,
                updatedAt = 1_700_000_000_000
            )
        )
        transactionDao.insert(
            TransactionEntity(
                id = "txn-2",
                accountId = "acc-1",
                date = 20260126L,
                amountMinor = -7000L,
                currencyCode = Currency.PHP.code,
                merchant = "Groceries",
                memo = null,
                categoryId = null,
                status = TransactionStatus.CLEARED.name,
                clearedAt = 1_700_000_000_000,
                transferId = null,
                createdAt = 1_700_000_000_000,
                updatedAt = 1_700_000_000_000
            )
        )

        val pending = repository.observePendingTransactions().first()

        assertEquals(1, pending.size)
        assertEquals("txn-1", pending.first().id)
        assertEquals(TransactionStatus.PENDING, pending.first().status)
    }
}

private class FakeTransactionDao : TransactionDao {
    val transactions = mutableListOf<TransactionEntity>()
    private val pendingFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

    override fun observeByAccount(accountId: String): Flow<List<TransactionEntity>> {
        return pendingFlow.map { list -> list.filter { it.accountId == accountId } }
    }

    override fun observeByAccountAndStatus(accountId: String, status: String): Flow<List<TransactionEntity>> {
        return pendingFlow.map { list -> list.filter { it.accountId == accountId && it.status == status } }
    }

    override fun observeByDateRange(accountId: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>> {
        return pendingFlow.map { list -> list.filter { it.accountId == accountId } }
    }

    override fun observeByCategory(categoryId: String): Flow<List<TransactionEntity>> {
        return pendingFlow.map { list -> list.filter { it.categoryId == categoryId } }
    }

    override fun observePendingTransactions(): Flow<List<TransactionEntity>> = pendingFlow

    override fun observeRecentTransactions(limit: Int): Flow<List<TransactionEntity>> {
        return pendingFlow.map { list -> list.take(limit) }
    }

    override fun observeTransaction(id: String): Flow<TransactionEntity?> {
        return pendingFlow.map { list -> list.firstOrNull { it.id == id } }
    }

    override suspend fun getTransaction(id: String): TransactionEntity? {
        return transactions.firstOrNull { it.id == id }
    }

    override suspend fun getTransactionsByTransferId(transferId: String): List<TransactionEntity> {
        return transactions.filter { it.transferId == transferId }
    }

    override suspend fun getTransactionsByAccount(accountId: String): List<TransactionEntity> {
        return transactions.filter { it.accountId == accountId }
    }

    override suspend fun insert(transaction: TransactionEntity) {
        transactions.removeAll { it.id == transaction.id }
        transactions.add(transaction)
        emit()
    }

    override suspend fun insertAll(transactions: List<TransactionEntity>) {
        transactions.forEach { insert(it) }
    }

    override suspend fun update(transaction: TransactionEntity) {
        insert(transaction)
    }

    override suspend fun delete(transaction: TransactionEntity) {
        deleteById(transaction.id)
    }

    override suspend fun deleteById(id: String) {
        transactions.removeAll { it.id == id }
        emit()
    }

    override suspend fun deleteByTransferId(transferId: String) {
        transactions.removeAll { it.transferId == transferId }
        emit()
    }

    override suspend fun updateStatus(id: String, status: String, clearedAt: Long?, updatedAt: Long) {
        val current = transactions.firstOrNull { it.id == id } ?: return
        insert(current.copy(status = status, clearedAt = clearedAt, updatedAt = updatedAt))
    }

    override suspend fun getSumByAccount(accountId: String): Long? {
        return transactions.filter { it.accountId == accountId }.sumOf { it.amountMinor }
    }

    override suspend fun getSumByCategoryInRange(categoryId: String, startDate: Long, endDate: Long): Long? {
        return transactions.filter { it.categoryId == categoryId }.sumOf { it.amountMinor }
    }

    override suspend fun getTransactionCount(accountId: String): Int {
        return transactions.count { it.accountId == accountId }
    }

    override fun searchTransactions(query: String, limit: Int): Flow<List<TransactionEntity>> {
        return pendingFlow.map { list -> list.filter { it.merchant.contains(query, ignoreCase = true) }.take(limit) }
    }

    private fun emit() {
        pendingFlow.value = transactions.filter { it.status == TransactionStatus.PENDING.name }
    }
}

private class FakeSplitDao : SplitDao {
    val deletedTransactions = mutableListOf<String>()

    override fun observeSplitsByTransaction(transactionId: String): Flow<List<SplitEntity>> {
        return MutableStateFlow(emptyList())
    }

    override fun observeSplitsByCategory(categoryId: String): Flow<List<SplitEntity>> {
        return MutableStateFlow(emptyList())
    }

    override suspend fun getSplitsByTransaction(transactionId: String): List<SplitEntity> = emptyList()

    override suspend fun getSplit(transactionId: String, lineId: Int): SplitEntity? = null

    override suspend fun insert(split: SplitEntity) = Unit

    override suspend fun insertAll(splits: List<SplitEntity>) = Unit

    override suspend fun update(split: SplitEntity) = Unit

    override suspend fun delete(split: SplitEntity) = Unit

    override suspend fun deleteByTransaction(transactionId: String) {
        deletedTransactions.add(transactionId)
    }

    override suspend fun getSumByCategoryInRange(categoryId: String, startDate: Long, endDate: Long): Long? = 0L

    override suspend fun getSplitsSum(transactionId: String): Long? = 0L
}
