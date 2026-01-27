package com.banyg.domain.usecase

import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
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

class GetAllBudgetsProgressUseCaseTest {

    private val budgetRepository: BudgetRepository = mockk()
    private val transactionRepository: TransactionRepository = mockk()
    private val useCase = GetAllBudgetsProgressUseCase(budgetRepository, transactionRepository)

    private val testPeriod = BudgetPeriod.of(2026, 1)
    private val testInstant = Instant.parse("2026-01-15T10:00:00Z")

    private fun createTestBudget(
        id: String,
        categoryId: String,
        amount: Long
    ) = Budget(
        id = id,
        categoryId = categoryId,
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
        status: TransactionStatus = TransactionStatus.CLEARED
    ) = Transaction(
        id = id,
        accountId = "acc-1",
        date = date,
        amount = Money(amount, Currency.PHP),
        merchant = "Test",
        categoryId = categoryId,
        status = status,
        createdAt = testInstant,
        updatedAt = testInstant
    )

    @Test
    fun `invoke should return all budgets with progress`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 50_000L),
            createTestBudget("budget-2", "cat-2", 30_000L)
        )
        coEvery { budgetRepository.getBudgetsByPeriod(testPeriod) } returns budgets

        // When
        val result = useCase(period = testPeriod)

        // Then
        assertThat(result.budgets).hasSize(2)
        assertThat(result.period).isEqualTo(testPeriod)
    }

    @Test
    fun `invoke should calculate totals correctly`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 50_000L),
            createTestBudget("budget-2", "cat-2", 30_000L)
        )
        coEvery { budgetRepository.getBudgetsByPeriod(testPeriod) } returns budgets

        // When
        val result = useCase(period = testPeriod)

        // Then
        assertThat(result.totals.totalBudgeted).isEqualTo(80_000L)
        assertThat(result.totals.totalSpent).isEqualTo(0L)
        assertThat(result.totals.totalRemaining).isEqualTo(80_000L)
        assertThat(result.totals.overallPercentage).isEqualTo(0)
    }

    @Test
    fun `invoke should count budget statuses correctly`() = runTest {
        // Given - 3 budgets: one over, one on, one under
        // We'll simulate this by setting up transactions later
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 50_000L),
            createTestBudget("budget-2", "cat-2", 30_000L),
            createTestBudget("budget-3", "cat-3", 20_000L)
        )
        coEvery { budgetRepository.getBudgetsByPeriod(testPeriod) } returns budgets

        // When
        val result = useCase(period = testPeriod)

        // Then - all under budget when no transactions
        assertThat(result.totals.underBudgetCount).isEqualTo(3)
        assertThat(result.totals.onBudgetCount).isEqualTo(0)
        assertThat(result.totals.overBudgetCount).isEqualTo(0)
    }

    @Test
    fun `invoke with transactions should calculate spent`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 50_000L),
            createTestBudget("budget-2", "cat-2", 30_000L)
        )
        val transactions = listOf(
            createTransaction("txn-1", -20_000L, "cat-1"),
            createTransaction("txn-2", -10_000L, "cat-2")
        )
        coEvery { budgetRepository.getBudgetsByPeriod(testPeriod) } returns budgets
        coEvery { transactionRepository.observeRecentTransactions(1000) } returns flowOf(transactions)

        // When - using observe to include transactions
        val result = useCase.observe(period = testPeriod).first()

        // Then
        assertThat(result.totals.totalBudgeted).isEqualTo(80_000L)
        assertThat(result.totals.totalSpent).isEqualTo(30_000L)
        assertThat(result.totals.totalRemaining).isEqualTo(50_000L)
    }

    @Test
    fun `invoke should handle empty budget list`() = runTest {
        // Given
        coEvery { budgetRepository.getBudgetsByPeriod(testPeriod) } returns emptyList()

        // When
        val result = useCase(period = testPeriod)

        // Then
        assertThat(result.budgets).isEmpty()
        assertThat(result.totals.totalBudgeted).isEqualTo(0L)
        assertThat(result.totals.totalSpent).isEqualTo(0L)
        assertThat(result.totals.totalRemaining).isEqualTo(0L)
    }

    @Test
    fun `invoke should use provided currency`() = runTest {
        // Given
        coEvery { budgetRepository.getBudgetsByPeriod(testPeriod) } returns emptyList()

        // When
        val result = useCase(period = testPeriod, currency = Currency.USD)

        // Then
        assertThat(result.totals.currency).isEqualTo(Currency.USD)
    }

    @Test
    fun `observe should emit updates when budgets change`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 50_000L)
        )
        every { budgetRepository.observeBudgetsByPeriod(testPeriod) } returns flowOf(budgets)
        every { transactionRepository.observeRecentTransactions(1000) } returns flowOf(emptyList())

        // When
        val result = useCase.observe(period = testPeriod).first()

        // Then
        assertThat(result.budgets).hasSize(1)
        assertThat(result.budgets[0].budgeted).isEqualTo(50_000L)
    }

    @Test
    fun `observe should emit updates when transactions change`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 50_000L)
        )
        val transactions = listOf(
            createTransaction("txn-1", -20_000L, "cat-1")
        )
        every { budgetRepository.observeBudgetsByPeriod(testPeriod) } returns flowOf(budgets)
        every { transactionRepository.observeRecentTransactions(1000) } returns flowOf(transactions)

        // When
        val result = useCase.observe(period = testPeriod).first()

        // Then
        assertThat(result.budgets[0].spent).isEqualTo(-20_000L)
        assertThat(result.budgets[0].remaining).isEqualTo(30_000L)
    }

    @Test
    fun `totals hasOverBudget should be true when any budget over`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 50_000L),
            createTestBudget("budget-2", "cat-2", 30_000L)
        )
        // Budget 1 over by 10k, budget 2 under
        val transactions = listOf(
            createTransaction("txn-1", -60_000L, "cat-1"),
            createTransaction("txn-2", -10_000L, "cat-2")
        )
        every { budgetRepository.observeBudgetsByPeriod(testPeriod) } returns flowOf(budgets)
        every { transactionRepository.observeRecentTransactions(1000) } returns flowOf(transactions)

        // When
        val result = useCase.observe(period = testPeriod).first()

        // Then
        assertThat(result.totals.hasOverBudget).isTrue()
        assertThat(result.totals.overBudgetCount).isEqualTo(1)
    }

    @Test
    fun `totals allUnderBudget should be true when no budgets over`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 50_000L),
            createTestBudget("budget-2", "cat-2", 30_000L)
        )
        // Both under budget
        val transactions = listOf(
            createTransaction("txn-1", -20_000L, "cat-1"),
            createTransaction("txn-2", -10_000L, "cat-2")
        )
        every { budgetRepository.observeBudgetsByPeriod(testPeriod) } returns flowOf(budgets)
        every { transactionRepository.observeRecentTransactions(1000) } returns flowOf(transactions)

        // When
        val result = useCase.observe(period = testPeriod).first()

        // Then
        assertThat(result.totals.allUnderBudget).isTrue()
        assertThat(result.totals.overBudgetCount).isEqualTo(0)
    }

    @Test
    fun `totals should calculate percentage correctly`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 100_000L)
        )
        val transactions = listOf(
            createTransaction("txn-1", -50_000L, "cat-1")
        )
        every { budgetRepository.observeBudgetsByPeriod(testPeriod) } returns flowOf(budgets)
        every { transactionRepository.observeRecentTransactions(1000) } returns flowOf(transactions)

        // When
        val result = useCase.observe(period = testPeriod).first()

        // Then
        assertThat(result.totals.overallPercentage).isEqualTo(50)
    }

    @Test
    fun `totals should cap percentage at 100`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 50_000L)
        )
        val transactions = listOf(
            createTransaction("txn-1", -80_000L, "cat-1") // Over budget
        )
        every { budgetRepository.observeBudgetsByPeriod(testPeriod) } returns flowOf(budgets)
        every { transactionRepository.observeRecentTransactions(1000) } returns flowOf(transactions)

        // When
        val result = useCase.observe(period = testPeriod).first()

        // Then
        assertThat(result.totals.overallPercentage).isEqualTo(100)
    }

    @Test
    fun `totals Money conversions should work correctly`() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget("budget-1", "cat-1", 100_000L)
        )
        val transactions = listOf(
            createTransaction("txn-1", -60_000L, "cat-1")
        )
        every { budgetRepository.observeBudgetsByPeriod(testPeriod) } returns flowOf(budgets)
        every { transactionRepository.observeRecentTransactions(1000) } returns flowOf(transactions)

        // When
        val result = useCase.observe(period = testPeriod).first()

        // Then
        assertThat(result.totals.totalBudgetedAsMoney().minorUnits).isEqualTo(100_000L)
        assertThat(result.totals.totalSpentAsMoney().minorUnits).isEqualTo(60_000L)
        assertThat(result.totals.totalRemainingAsMoney().minorUnits).isEqualTo(40_000L)
    }
}
