package com.banyg.feature.reports

import app.cash.turbine.test
import com.banyg.domain.model.Category
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.CategoryRepository
import com.banyg.domain.repository.TransactionRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: ReportsViewModel

    private val testInstant = Instant.parse("2026-01-15T10:00:00Z")
    private val testCurrency = Currency.PHP

    private val testCategory = Category(
        id = "cat-1",
        name = "Groceries",
        groupId = "food",
        icon = "shopping_cart"
    )

    private val testCategory2 = Category(
        id = "cat-2",
        name = "Transportation",
        groupId = "transport"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ReportsViewModel(transactionRepository, categoryRepository)
    }

    private fun createTransaction(
        id: String,
        amount: Long,
        categoryId: String? = "cat-1",
        date: LocalDate = LocalDate.of(2026, 1, 15),
        merchant: String = "Test Merchant"
    ) = Transaction(
        id = id,
        accountId = "acc-1",
        date = date,
        amount = Money(amount, testCurrency),
        merchant = merchant,
        categoryId = categoryId,
        status = TransactionStatus.CLEARED,
        createdAt = testInstant,
        updatedAt = testInstant
    )

    @Test
    fun `initial state should be Loading`() = testScope.runTest {
        // Given
        every { categoryRepository.observeVisibleCategories() } returns emptyFlow()
        every { transactionRepository.observeRecentTransactions(any()) } returns emptyFlow()

        // When
        createViewModel()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(ReportsUiState.Loading::class.java)
    }

    @Test
    fun `loadReport success should emit Success state with report data`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory, testCategory2)
        val transactions = listOf(
            createTransaction("txn-1", -2000L, "cat-1"), // 20.00 expense
            createTransaction("txn-2", -3000L, "cat-1"), // 30.00 expense
            createTransaction("txn-3", 10000L, null, merchant = "Salary") // 100.00 income
        )
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ReportsUiState.Success::class.java)
        val successState = state as ReportsUiState.Success
        assertThat(successState.reportData.categorySpending).hasSize(1)
        assertThat(successState.reportData.categorySpending.first().category.id).isEqualTo("cat-1")
        assertThat(successState.reportData.categorySpending.first().amount.minorUnits).isEqualTo(5000L) // 50.00 total
        assertThat(successState.reportData.totalSpending.minorUnits).isEqualTo(5000L) // 50.00
        assertThat(successState.reportData.totalIncome.minorUnits).isEqualTo(10000L) // 100.00
        assertThat(successState.availablePeriods).isEqualTo(ReportPeriod.entries)
        assertThat(successState.isExporting).isFalse()
        assertThat(successState.isRefreshing).isFalse()
    }

    @Test
    fun `loadReport error should emit Error state`() = testScope.runTest {
        // Given
        every { categoryRepository.observeVisibleCategories() } returns flow { throw RuntimeException("Network error") }
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ReportsUiState.Error::class.java)
        val errorState = state as ReportsUiState.Error
        assertThat(errorState.message).contains("Network error")
        assertThat(errorState.canRetry).isTrue()
    }

    @Test
    fun `changePeriod should reload report with new period`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Verify initial state
        val initialState = viewModel.uiState.value as ReportsUiState.Success
        assertThat(initialState.reportData.period).isEqualTo(ReportPeriod.MONTHLY)

        // When - change to weekly
        viewModel.changePeriod(ReportPeriod.WEEKLY)
        advanceUntilIdle()

        // Then
        val newState = viewModel.uiState.value as ReportsUiState.Success
        assertThat(newState.reportData.period).isEqualTo(ReportPeriod.WEEKLY)
    }

    @Test
    fun `exportReport should generate CSV and emit ExportComplete event`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transactions = listOf(
            createTransaction("txn-1", -2000L, "cat-1")
        )
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)
        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.exportReport()
            advanceUntilIdle()

            // First event should be ExportComplete
            val firstEvent = awaitItem()
            assertThat(firstEvent).isInstanceOf(ReportsEvent.ExportComplete::class.java)
            val exportEvent = firstEvent as ReportsEvent.ExportComplete
            assertThat(exportEvent.filePath).contains("banyg_report_")
            assertThat(exportEvent.filePath).contains(".csv")

            // Second event should be ShowSnackbar
            val secondEvent = awaitItem()
            assertThat(secondEvent).isInstanceOf(ReportsEvent.ShowSnackbar::class.java)
            assertThat((secondEvent as ReportsEvent.ShowSnackbar).message).contains("exported to")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `exportReport should complete with isExporting false`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When - launch export and wait for completion
        viewModel.exportReport()
        advanceUntilIdle()

        // Then - isExporting should be false after completion
        val finalState = viewModel.uiState.value as ReportsUiState.Success
        assertThat(finalState.isExporting).isFalse()
    }

    @Test
    fun `exportReport should not run when state is not Success`() = testScope.runTest {
        // Given - start with Loading state by not advancing
        every { categoryRepository.observeVisibleCategories() } returns emptyFlow()
        every { transactionRepository.observeRecentTransactions(any()) } returns emptyFlow()
        createViewModel()

        // When
        viewModel.exportReport()
        advanceUntilIdle()

        // Then - state should still be Loading (no crash, no change)
        assertThat(viewModel.uiState.value).isInstanceOf(ReportsUiState.Loading::class.java)
    }

    @Test
    fun `onEvent OnRefresh should reload report`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Verify initial state
        val initialState = viewModel.uiState.value as ReportsUiState.Success
        assertThat(initialState.isRefreshing).isFalse()

        // When
        viewModel.onEvent(ReportsUiEvent.OnRefresh)
        
        // Check refreshing state is set
        val refreshingState = viewModel.uiState.value as ReportsUiState.Success
        assertThat(refreshingState.isRefreshing).isTrue()
        
        advanceUntilIdle()

        // Then - should still be success after refresh
        val finalState = viewModel.uiState.value as ReportsUiState.Success
        assertThat(finalState.isRefreshing).isFalse()
    }

    @Test
    fun `onEvent OnPeriodChanged should change period and reload`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Verify initial period
        val initialState = viewModel.uiState.value as ReportsUiState.Success
        assertThat(initialState.reportData.period).isEqualTo(ReportPeriod.MONTHLY)

        // When
        viewModel.onEvent(ReportsUiEvent.OnPeriodChanged(ReportPeriod.YEARLY))
        advanceUntilIdle()

        // Then
        val newState = viewModel.uiState.value as ReportsUiState.Success
        assertThat(newState.reportData.period).isEqualTo(ReportPeriod.YEARLY)
    }

    @Test
    fun `onEvent OnCategoryClick should emit NavigateToCategoryDetail event`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(ReportsUiEvent.OnCategoryClick("cat-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isInstanceOf(ReportsEvent.NavigateToCategoryDetail::class.java)
            val navigateEvent = event as ReportsEvent.NavigateToCategoryDetail
            assertThat(navigateEvent.categoryId).isEqualTo("cat-1")
            assertThat(navigateEvent.startDate).isNotNull()
            assertThat(navigateEvent.endDate).isNotNull()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnCategoryClick should not emit event when not in Success state`() = testScope.runTest {
        // Given - start with Loading state
        every { categoryRepository.observeVisibleCategories() } returns emptyFlow()
        every { transactionRepository.observeRecentTransactions(any()) } returns emptyFlow()
        createViewModel()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(ReportsUiEvent.OnCategoryClick("cat-1"))
            advanceUntilIdle()

            // No events should be emitted
            expectNoEvents()
        }
    }

    @Test
    fun `onEvent OnRetry should reload report`() = testScope.runTest {
        // Given - start with error state
        every { categoryRepository.observeVisibleCategories() } returns flow { throw RuntimeException("Error") }
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Verify error state
        assertThat(viewModel.uiState.value).isInstanceOf(ReportsUiState.Error::class.java)

        // Fix the repository to return success
        val categories = listOf(testCategory)
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)

        // When
        viewModel.onEvent(ReportsUiEvent.OnRetry)
        advanceUntilIdle()

        // Then - should be success now
        assertThat(viewModel.uiState.value).isInstanceOf(ReportsUiState.Success::class.java)
    }

    @Test
    fun `onEvent OnExportReport should call exportReport`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transactions = listOf(createTransaction("txn-1", -1000L, "cat-1"))
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)
        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(ReportsUiEvent.OnExportReport)
            advanceUntilIdle()

            // Should emit ExportComplete event
            val event = awaitItem()
            assertThat(event).isInstanceOf(ReportsEvent.ExportComplete::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnShareReport should emit ShowSnackbar event`() = testScope.runTest {
        // Given
        every { categoryRepository.observeVisibleCategories() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(ReportsUiEvent.OnShareReport)
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isInstanceOf(ReportsEvent.ShowSnackbar::class.java)
            assertThat((event as ReportsEvent.ShowSnackbar).message).contains("Share feature coming soon")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loadReport should calculate spending percentages correctly`() = testScope.runTest {
        // Given - transactions in two categories
        val categories = listOf(testCategory, testCategory2)
        val transactions = listOf(
            createTransaction("txn-1", -2000L, "cat-1"), // 20.00
            createTransaction("txn-2", -3000L, "cat-1"), // 30.00 (cat-1 total: 50.00)
            createTransaction("txn-3", -5000L, "cat-2")  // 50.00 (cat-2 total: 50.00)
        )
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as ReportsUiState.Success
        val categorySpending = state.reportData.categorySpending
        assertThat(categorySpending).hasSize(2)
        
        // Both categories should have 50% (0.5f) since they both spent 50.00
        assertThat(categorySpending[0].percentage).isEqualTo(0.5f)
        assertThat(categorySpending[1].percentage).isEqualTo(0.5f)
        
        // Total spending should be 100.00
        assertThat(state.reportData.totalSpending.minorUnits).isEqualTo(10000L)
    }

    @Test
    fun `loadReport should sort categories by spending descending`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory, testCategory2)
        val transactions = listOf(
            createTransaction("txn-1", -1000L, "cat-1"), // 10.00
            createTransaction("txn-2", -5000L, "cat-2")  // 50.00 (higher)
        )
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then - cat-2 should be first (higher spending)
        val state = viewModel.uiState.value as ReportsUiState.Success
        val categorySpending = state.reportData.categorySpending
        assertThat(categorySpending[0].category.id).isEqualTo("cat-2")
        assertThat(categorySpending[1].category.id).isEqualTo("cat-1")
    }

    @Test
    fun `loadReport should filter transactions outside date range`() = testScope.runTest {
        // Given - transactions with different dates
        val categories = listOf(testCategory)
        val transactions = listOf(
            createTransaction("txn-1", -2000L, "cat-1", date = LocalDate.of(2026, 1, 15)), // In current month
            createTransaction("txn-2", -3000L, "cat-1", date = LocalDate.of(2025, 12, 1))  // Previous month
        )
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then - only January transaction should count
        val state = viewModel.uiState.value as ReportsUiState.Success
        assertThat(state.reportData.categorySpending).hasSize(1)
        assertThat(state.reportData.categorySpending[0].amount.minorUnits).isEqualTo(2000L)
        assertThat(state.reportData.categorySpending[0].transactionCount).isEqualTo(1)
    }

    @Test
    fun `loadReport should handle empty transactions`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as ReportsUiState.Success
        assertThat(state.reportData.categorySpending).isEmpty()
        assertThat(state.reportData.totalSpending.minorUnits).isEqualTo(0L)
        assertThat(state.reportData.totalIncome.minorUnits).isEqualTo(0L)
        assertThat(state.reportData.netAmount.minorUnits).isEqualTo(0L)
    }

    @Test
    fun `loadReport should handle empty categories`() = testScope.runTest {
        // Given
        val transactions = listOf(createTransaction("txn-1", -2000L, "cat-1"))
        every { categoryRepository.observeVisibleCategories() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then - no category spending since no categories exist
        val state = viewModel.uiState.value as ReportsUiState.Success
        assertThat(state.reportData.categorySpending).isEmpty()
    }

    @Test
    fun `loadReport should filter out income transactions from spending`() = testScope.runTest {
        // Given - mix of income and expense
        val categories = listOf(testCategory)
        val transactions = listOf(
            createTransaction("txn-1", -2000L, "cat-1"), // Expense (negative)
            createTransaction("txn-2", 5000L, "cat-1")    // Income (positive) - should not be counted as spending
        )
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as ReportsUiState.Success
        assertThat(state.reportData.totalSpending.minorUnits).isEqualTo(2000L) // Only expense
        assertThat(state.reportData.totalIncome.minorUnits).isEqualTo(5000L)   // Only income
        assertThat(state.reportData.netAmount.minorUnits).isEqualTo(3000L)     // Income - Spending
    }

    @Test
    fun `loadReport should filter out uncategorized transactions`() = testScope.runTest {
        // Given - transaction with no category
        val categories = listOf(testCategory)
        val transactions = listOf(
            createTransaction("txn-1", -2000L, "cat-1"),
            createTransaction("txn-2", -3000L, null) // Uncategorized
        )
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then - only categorized transaction should count in category spending
        val state = viewModel.uiState.value as ReportsUiState.Success
        assertThat(state.reportData.categorySpending).hasSize(1)
        assertThat(state.reportData.categorySpending[0].transactionCount).isEqualTo(1)
        // Total spending includes both transactions (5000L)
        assertThat(state.reportData.totalSpending.minorUnits).isEqualTo(5000L)
    }

    @Test
    fun `onEvent OnDateRangeSelected should reload report`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(ReportsUiEvent.OnDateRangeSelected(
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 31)
        ))
        advanceUntilIdle()

        // Then - should still be success after reload
        assertThat(viewModel.uiState.value).isInstanceOf(ReportsUiState.Success::class.java)
    }
}
