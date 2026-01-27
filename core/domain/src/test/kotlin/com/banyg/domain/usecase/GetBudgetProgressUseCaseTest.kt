package com.banyg.domain.usecase

import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Split
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.BudgetRepository
import com.banyg.domain.repository.TransactionRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GetBudgetProgressUseCaseTest {

    private val budgetRepository: BudgetRepository = mockk()
    private val transactionRepository: TransactionRepository = mockk()
    private val useCase = GetBudgetProgressUseCase(budgetRepository, transactionRepository)

    private val testPeriod = BudgetPeriod.of(2026, 1)
    private val testInstant = Instant.parse("2026-01-15T10:00:00Z")

    private fun createTestBudget(
        id: String = "budget-1",
        amount: Long = 50_000L
    ) = Budget(
        id = id,
        categoryId = "cat-1",
        amount = amount,
        period = testPeriod,
        currency = Currency.PHP,
        createdAt = testInstant,
        updatedAt = testInstant
    )

    private fun createTransaction(
        id: String,
        amount: Long,
        categoryId: String = "cat-1",
        date: LocalDate = LocalDate.of(2026, 1, 15),
        status: TransactionStatus = TransactionStatus.CLEARED,
        transferId: String? = null,
        splits: List<Split> = emptyList()
    ) = Transaction(
        id = id,
        accountId = "acc-1",
        date = date,
        amount = Money(amount, Currency.PHP),
        merchant = "Test Merchant",
        categoryId = categoryId,
        status = status,
        transferId = transferId,
        splits = splits,
        createdAt = testInstant,
        updatedAt = testInstant
    )

    @Test
    fun `invoke with budget ID should return budget with progress`() = runTest {
        // Given
        val budget = createTestBudget(amount = 50_000L)
        coEvery { budgetRepository.getBudget("budget-1") } returns budget
        coEvery { transactionRepository.getTransactionsByCategory("cat-1") } returns emptyList()

        // When
        val result = useCase(budgetId = "budget-1")

        // Then
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.Success::class.java)
        val success = result as GetBudgetProgressUseCase.Result.Success
        assertThat(success.progress.budgeted).isEqualTo(50_000L)
        assertThat(success.progress.spent).isEqualTo(0L)
        assertThat(success.progress.remaining).isEqualTo(50_000L)
        assertThat(success.progress.percentage).isEqualTo(0)
    }

    @Test
    fun `invoke with non-existent budget ID should return not found`() = runTest {
        // Given
        coEvery { budgetRepository.getBudget("non-existent") } returns null

        // When
        val result = useCase(budgetId = "non-existent")

        // Then
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.NotFound::class.java)
    }

    @Test
    fun `invoke by category and period should return budget with progress`() = runTest {
        // Given
        val budget = createTestBudget()
        coEvery { budgetRepository.getBudget("cat-1", testPeriod) } returns budget
        coEvery { transactionRepository.getTransactionsByCategory("cat-1") } returns emptyList()

        // When
        val result = useCase(categoryId = "cat-1", period = testPeriod)

        // Then
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.Success::class.java)
    }

    @Test
    fun `invoke should calculate spent from transactions`() = runTest {
        // Given
        val budget = createTestBudget(amount = 50_000L)
        val transaction = createTransaction(
            id = "txn-1",
            amount = -10_000L, // ₱100.00 expense
            categoryId = "cat-1"
        )
        coEvery { budgetRepository.getBudget("budget-1") } returns budget
        coEvery { transactionRepository.getTransactionsByCategory("cat-1") } returns listOf(transaction)

        // When
        val result = useCase(budgetId = "budget-1")

        // Then
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.Success::class.java)
        val success = result as GetBudgetProgressUseCase.Result.Success
        assertThat(success.progress.spent).isEqualTo(-10_000L)
        assertThat(success.progress.remaining).isEqualTo(40_000L)
        assertThat(success.progress.percentage).isEqualTo(20)
    }

    @Test
    fun `invoke should exclude pending transactions`() = runTest {
        // Given
        val budget = createTestBudget(amount = 50_000L)
        val clearedTransaction = createTransaction(
            id = "txn-1",
            amount = -10_000L,
            status = TransactionStatus.CLEARED
        )
        val pendingTransaction = createTransaction(
            id = "txn-2",
            amount = -20_000L,
            status = TransactionStatus.PENDING
        )
        coEvery { budgetRepository.getBudget("budget-1") } returns budget
        coEvery { transactionRepository.getTransactionsByCategory("cat-1") } returns listOf(
            clearedTransaction,
            pendingTransaction
        )

        // When
        val result = useCase(budgetId = "budget-1")

        // Then - only cleared transaction should be counted
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.Success::class.java)
        val success = result as GetBudgetProgressUseCase.Result.Success
        assertThat(success.progress.spent).isEqualTo(-10_000L)
    }

    @Test
    fun `invoke should exclude transfers`() = runTest {
        // Given
        val budget = createTestBudget(amount = 50_000L)
        val regularTransaction = createTransaction(
            id = "txn-1",
            amount = -10_000L,
            transferId = null
        )
        val transferTransaction = createTransaction(
            id = "txn-2",
            amount = -20_000L,
            transferId = "transfer-1"
        )
        coEvery { budgetRepository.getBudget("budget-1") } returns budget
        coEvery { transactionRepository.getTransactionsByCategory("cat-1") } returns listOf(
            regularTransaction,
            transferTransaction
        )

        // When
        val result = useCase(budgetId = "budget-1")

        // Then - only non-transfer transaction should be counted
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.Success::class.java)
        val success = result as GetBudgetProgressUseCase.Result.Success
        assertThat(success.progress.spent).isEqualTo(-10_000L)
    }

    @Test
    fun `invoke should exclude transactions outside period`() = runTest {
        // Given
        val budget = createTestBudget(amount = 50_000L)
        val inPeriodTransaction = createTransaction(
            id = "txn-1",
            amount = -10_000L,
            date = LocalDate.of(2026, 1, 15) // January
        )
        val outOfPeriodTransaction = createTransaction(
            id = "txn-2",
            amount = -20_000L,
            date = LocalDate.of(2026, 2, 15) // February
        )
        coEvery { budgetRepository.getBudget("budget-1") } returns budget
        coEvery { transactionRepository.getTransactionsByCategory("cat-1") } returns listOf(
            inPeriodTransaction,
            outOfPeriodTransaction
        )

        // When
        val result = useCase(budgetId = "budget-1")

        // Then - only January transaction should be counted
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.Success::class.java)
        val success = result as GetBudgetProgressUseCase.Result.Success
        assertThat(success.progress.spent).isEqualTo(-10_000L)
    }

    @Test
    fun `invoke should calculate spent from split transactions`() = runTest {
        // Given
        val budget = createTestBudget(amount = 50_000L)
        val split = Split(
            transactionId = "txn-1",
            lineId = 0,
            categoryId = "cat-1",
            amount = Money(-15_000L, Currency.PHP)
        )
        val transaction = createTransaction(
            id = "txn-1",
            amount = -30_000L,
            splits = listOf(split)
        )
        coEvery { budgetRepository.getBudget("budget-1") } returns budget
        coEvery { transactionRepository.getTransactionsByCategory("cat-1") } returns listOf(transaction)

        // When
        val result = useCase(budgetId = "budget-1")

        // Then - split amount should be counted
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.Success::class.java)
        val success = result as GetBudgetProgressUseCase.Result.Success
        assertThat(success.progress.spent).isEqualTo(-15_000L)
    }

    @Test
    fun `invoke should detect over budget`() = runTest {
        // Given
        val budget = createTestBudget(amount = 50_000L)
        val transaction = createTransaction(
            id = "txn-1",
            amount = -60_000L // More than budget
        )
        coEvery { budgetRepository.getBudget("budget-1") } returns budget
        coEvery { transactionRepository.getTransactionsByCategory("cat-1") } returns listOf(transaction)

        // When
        val result = useCase(budgetId = "budget-1")

        // Then
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.Success::class.java)
        val success = result as GetBudgetProgressUseCase.Result.Success
        assertThat(success.progress.isOverBudget).isTrue()
        assertThat(success.progress.remaining).isEqualTo(-10_000L)
        assertThat(success.progress.percentage).isEqualTo(100) // capped at 100
    }

    @Test
    fun `observe should emit budget progress updates`() = runTest {
        // Given
        val budget = createTestBudget()
        every { budgetRepository.observeBudget("cat-1", testPeriod) } returns flowOf(budget)
        every { transactionRepository.observeByCategory("cat-1") } returns flowOf(emptyList())

        // When
        val result = useCase.observe(categoryId = "cat-1", period = testPeriod).first()

        // Then
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.Success::class.java)
    }

    @Test
    fun `observe should emit not found when budget does not exist`() = runTest {
        // Given
        every { budgetRepository.observeBudget("cat-1", testPeriod) } returns flowOf(null)
        every { transactionRepository.observeByCategory("cat-1") } returns flowOf(emptyList())

        // When
        val result = useCase.observe(categoryId = "cat-1", period = testPeriod).first()

        // Then
        assertThat(result).isInstanceOf(GetBudgetProgressUseCase.Result.NotFound::class.java)
    }

    @Test
    fun `calculate spent should return 0 when no matching transactions`() {
        // Given
        val transactions = emptyList<Transaction>()

        // When
        val spent = GetBudgetProgressUseCase.calculateSpentFromTransactions(
            "cat-1",
            testPeriod,
            transactions
        )

        // Then
        assertThat(spent).isEqualTo(0L)
    }

    @Test
    fun `calculate spent should include reconciled transactions`() {
        // Given
        val transaction = createTransaction(
            id = "txn-1",
            amount = -10_000L,
            status = TransactionStatus.RECONCILED
        )

        // When
        val spent = GetBudgetProgressUseCase.calculateSpentFromTransactions(
            "cat-1",
            testPeriod,
            listOf(transaction)
        )

        // Then
        assertThat(spent).isEqualTo(-10_000L)
    }

    @Test
    fun `calculate spent should sum multiple transactions`() {
        // Given
        val transactions = listOf(
            createTransaction(id = "txn-1", amount = -10_000L),
            createTransaction(id = "txn-2", amount = -20_000L),
            createTransaction(id = "txn-3", amount = -5_000L)
        )

        // When
        val spent = GetBudgetProgressUseCase.calculateSpentFromTransactions(
            "cat-1",
            testPeriod,
            transactions
        )

        // Then
        assertThat(spent).isEqualTo(-35_000L)
    }
}
