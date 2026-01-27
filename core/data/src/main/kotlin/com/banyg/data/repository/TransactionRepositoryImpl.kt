package com.banyg.data.repository

import com.banyg.data.local.dao.SplitDao
import com.banyg.data.local.dao.TransactionDao
import com.banyg.data.mapper.SplitMapper
import com.banyg.data.mapper.TransactionMapper
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/**
 * Transaction Repository Implementation
 *
 * Implements TransactionRepository using Room DAOs and mappers.
 * Handles transactions with optional splits.
 * Converts between domain models and Room entities.
 */
class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val splitDao: SplitDao,
    private val transactionMapper: TransactionMapper = TransactionMapper(),
    private val splitMapper: SplitMapper = SplitMapper()
) : TransactionRepository {

    override fun observeByAccount(accountId: String): Flow<List<Transaction>> {
        return transactionDao.observeByAccount(accountId)
            .map { entities ->
                entities.map { entity ->
                    val splits = splitDao.getSplitsByTransaction(entity.id)
                    transactionMapper.toDomain(entity, splits)
                }
            }
    }

    override fun observeByAccountAndStatus(
        accountId: String,
        status: TransactionStatus
    ): Flow<List<Transaction>> {
        return transactionDao.observeByAccountAndStatus(accountId, status.name)
            .map { entities ->
                entities.map { entity ->
                    val splits = splitDao.getSplitsByTransaction(entity.id)
                    transactionMapper.toDomain(entity, splits)
                }
            }
    }

    override fun observeByDateRange(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Transaction>> {
        val startLong = transactionMapper.dateToLong(startDate)
        val endLong = transactionMapper.dateToLong(endDate)

        return transactionDao.observeByDateRange(accountId, startLong, endLong)
            .map { entities ->
                entities.map { entity ->
                    val splits = splitDao.getSplitsByTransaction(entity.id)
                    transactionMapper.toDomain(entity, splits)
                }
            }
    }

    override fun observeByCategory(categoryId: String): Flow<List<Transaction>> {
        return transactionDao.observeByCategory(categoryId)
            .map { entities ->
                entities.map { entity ->
                    val splits = splitDao.getSplitsByTransaction(entity.id)
                    transactionMapper.toDomain(entity, splits)
                }
            }
    }

    override fun observePendingTransactions(): Flow<List<Transaction>> {
        return transactionDao.observePendingTransactions()
            .map { entities ->
                entities.map { entity ->
                    val splits = splitDao.getSplitsByTransaction(entity.id)
                    transactionMapper.toDomain(entity, splits)
                }
            }
    }

    override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.observeRecentTransactions(limit)
            .map { entities ->
                entities.map { entity ->
                    val splits = splitDao.getSplitsByTransaction(entity.id)
                    transactionMapper.toDomain(entity, splits)
                }
            }
    }

    override fun observeTransaction(id: String): Flow<Transaction?> {
        return transactionDao.observeTransaction(id)
            .map { entity ->
                entity?.let {
                    val splits = splitDao.getSplitsByTransaction(it.id)
                    transactionMapper.toDomain(it, splits)
                }
            }
    }

    override suspend fun getTransaction(id: String): Transaction? {
        val entity = transactionDao.getTransaction(id) ?: return null
        val splits = splitDao.getSplitsByTransaction(id)
        return transactionMapper.toDomain(entity, splits)
    }

    override suspend fun getTransactionsByTransferId(transferId: String): List<Transaction> {
        val entities = transactionDao.getTransactionsByTransferId(transferId)
        return entities.map { entity ->
            val splits = splitDao.getSplitsByTransaction(entity.id)
            transactionMapper.toDomain(entity, splits)
        }
    }

    override suspend fun getTransactionsByAccount(accountId: String): List<Transaction> {
        val entities = transactionDao.getTransactionsByAccount(accountId)
        return entities.map { entity ->
            val splits = splitDao.getSplitsByTransaction(entity.id)
            transactionMapper.toDomain(entity, splits)
        }
    }

    override suspend fun getTransactionsByCategory(categoryId: String): List<Transaction> {
        val entities = transactionDao.getTransactionsByCategory(categoryId)
        return entities.map { entity ->
            val splits = splitDao.getSplitsByTransaction(entity.id)
            transactionMapper.toDomain(entity, splits)
        }
    }

    override suspend fun saveTransaction(transaction: Transaction) {
        // Insert transaction
        transactionDao.insert(transactionMapper.toEntity(transaction))

        // Insert splits if present
        if (transaction.hasSplits) {
            val splitEntities = splitMapper.toEntityList(transaction.splits)
            splitDao.insertTransactionWithSplits(transaction.id, splitEntities)
        } else {
            // Clear any existing splits if transaction no longer has them
            splitDao.deleteByTransaction(transaction.id)
        }
    }

    override suspend fun saveTransactions(transactions: List<Transaction>) {
        transactions.forEach { saveTransaction(it) }
    }

    override suspend fun deleteTransaction(id: String) {
        // Cascade delete will handle splits
        transactionDao.deleteById(id)
    }

    override suspend fun deleteByTransferId(transferId: String) {
        // Cascade delete will handle splits
        transactionDao.deleteByTransferId(transferId)
    }

    override suspend fun updateStatus(id: String, status: TransactionStatus) {
        val clearedAt = if (status == TransactionStatus.CLEARED || status == TransactionStatus.RECONCILED) {
            Instant.now().toEpochMilli()
        } else {
            null
        }
        val updatedAt = Instant.now().toEpochMilli()
        transactionDao.updateStatus(id, status.name, clearedAt, updatedAt)
    }

    override suspend fun getSumByAccount(accountId: String): Money? {
        val sum = transactionDao.getSumByAccount(accountId) ?: return null
        // Get currency from first transaction in account
        val firstTransaction = transactionDao.getTransactionsByAccount(accountId).firstOrNull()
            ?: return null
        val currency = Currency.fromCode(firstTransaction.currencyCode)
            ?: throw IllegalStateException("Unknown currency: ${firstTransaction.currencyCode}")
        return Money(sum, currency)
    }

    override suspend fun getSumByCategoryInRange(
        categoryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Money? {
        val startLong = transactionMapper.dateToLong(startDate)
        val endLong = transactionMapper.dateToLong(endDate)

        // Sum transactions with this category
        val transactionSum = transactionDao.getSumByCategoryInRange(categoryId, startLong, endLong) ?: 0L

        // Sum splits with this category
        val splitSum = splitDao.getSumByCategoryInRange(categoryId, startLong, endLong) ?: 0L

        val total = transactionSum + splitSum
        if (total == 0L) return null

        // Get currency from first transaction or split
        val firstTransaction = transactionDao.getTransactionsByAccount(categoryId).firstOrNull()
        val currency = firstTransaction?.let { Currency.fromCode(it.currencyCode) }
            ?: Currency.PHP // Default fallback

        return Money(total, currency)
    }

    override suspend fun getTransactionCount(accountId: String): Int {
        return transactionDao.getTransactionCount(accountId)
    }

    override fun searchTransactions(query: String, limit: Int): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query, limit)
            .map { entities ->
                entities.map { entity ->
                    val splits = splitDao.getSplitsByTransaction(entity.id)
                    transactionMapper.toDomain(entity, splits)
                }
            }
    }
}
