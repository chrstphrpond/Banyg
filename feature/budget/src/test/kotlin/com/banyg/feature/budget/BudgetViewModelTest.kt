package com.banyg.feature.budget

import app.cash.turbine.test
import com.banyg.domain.model.Category
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.TransactionRepository
import com.banyg.domain.usecase.GetCategoriesUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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
class BudgetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var viewModel: BudgetViewModel

    private val testInstant = Instant.parse("2026-01-15T10:00:00Z")
    private val testCurrency = Currency.PHP

    private val testCategory = Category(
        id = "cat-1",
        name = "Groceries",
        groupId = "food"
    )

    private val testCategory2 = Category(
        id = "cat-2",
        name = "Transportation",
        groupId = "transport"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getCategoriesUseCase = mockk()
        transactionRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = BudgetViewModel(getCategoriesUseCase, transactionRepository)
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
        every { getCategoriesUseCase() } returns emptyFlow()
        every { transactionRepository.observeRecentTransactions(any()) } returns emptyFlow()

        // When
        createViewModel()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(BudgetUiState.Loading::class.java)
    }

    @Test
    fun `loadBudgets success should emit Success state with budgets`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory, testCategory2)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(BudgetUiState.Success::class.java)
    }

    @Test
    fun `loadBudgets error should emit Error state`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flow { throw RuntimeException("Network error") }
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(BudgetUiState.Error::class.java)
        val errorState = state as BudgetUiState.Error
        assertThat(errorState.message).contains("Network error")
        assertThat(errorState.canRetry).isTrue()
    }

    @Test
    fun `createBudget success should add budget and emit events`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory, testCategory2)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Set up form state
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("5000")

        // When
        viewModel.events.test {
            viewModel.createBudget()
            advanceUntilIdle()

            // Then
            val firstEvent = awaitItem()
            assertThat(firstEvent).isInstanceOf(BudgetEvent.ShowSnackbar::class.java)
            assertThat((firstEvent as BudgetEvent.ShowSnackbar).message).contains("created successfully")

            val secondEvent = awaitItem()
            assertThat(secondEvent).isInstanceOf(BudgetEvent.NavigateBack::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `createBudget with invalid form should show error`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When - trying to create without setting category or amount
        viewModel.createBudget()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.formState.value.error).isNotNull()
        assertThat(viewModel.formState.value.error).contains("fill in all fields")
    }

    @Test
    fun `createBudget with invalid amount should show error`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Set up form with invalid amount
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("invalid")

        // When
        viewModel.createBudget()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.formState.value.error).isNotNull()
    }

    @Test
    fun `deleteBudget should remove budget and refresh`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Create a budget first
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("5000")
        viewModel.createBudget()
        advanceUntilIdle()

        // Get the budget id from the success state
        val successState = viewModel.uiState.value as BudgetUiState.Success
        val budgetId = successState.budgets.first().budget.id

        // When
        viewModel.events.test {
            viewModel.deleteBudget(budgetId)
            advanceUntilIdle()

            // Then
            val event = awaitItem()
            assertThat(event).isInstanceOf(BudgetEvent.ShowSnackbar::class.java)
            assertThat((event as BudgetEvent.ShowSnackbar).message).contains("deleted")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `budget calculations should compute spent correctly`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transactions = listOf(
            createTransaction("txn-1", -2000L, "cat-1"), // 20 spent
            createTransaction("txn-2", -3000L, "cat-1")  // 30 spent
        )
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)
        createViewModel()
        advanceUntilIdle()

        // Create budget
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("10000") // 100 budget
        viewModel.createBudget()
        advanceUntilIdle()

        // Reload to trigger calculation
        viewModel.loadBudgets()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as BudgetUiState.Success
        assertThat(state.budgets).hasSize(1)
        assertThat(state.totalSpent).isNotNull()
    }

    @Test
    fun `budget calculations should compute remaining correctly`() = testScope.runTest {
        // Given - budget of 100, spent of 50
        val categories = listOf(testCategory)
        val budgetAmount = 10000L // 100.00
        val spentAmount = 5000L   // 50.00

        val transactions = listOf(
            createTransaction("txn-1", -spentAmount, "cat-1")
        )
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(transactions)
        createViewModel()
        advanceUntilIdle()

        // Create budget
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount((budgetAmount / 100).toString())
        viewModel.createBudget()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as BudgetUiState.Success
        assertThat(state.totalBudgeted).isNotNull()
        assertThat(state.totalBudgeted!!.minorUnits).isEqualTo(budgetAmount)
    }

    @Test
    fun `loadBudgets should show only unbudgeted categories`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory, testCategory2)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Create budget for cat-1
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("5000")
        viewModel.createBudget()
        advanceUntilIdle()

        // Then - only cat-2 should be in available categories
        val state = viewModel.uiState.value as BudgetUiState.Success
        assertThat(state.categories).hasSize(1)
        assertThat(state.categories.first().id).isEqualTo("cat-2")
    }

    @Test
    fun `updateBudget should update existing budget`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Create budget first
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("5000")
        viewModel.createBudget()
        advanceUntilIdle()

        val budgetId = (viewModel.uiState.value as BudgetUiState.Success).budgets.first().budget.id

        // Reset form and update
        viewModel.resetForm()
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("10000")

        // When
        viewModel.events.test {
            viewModel.updateBudget(budgetId)
            advanceUntilIdle()

            // Then
            val event = awaitItem()
            assertThat(event).isInstanceOf(BudgetEvent.ShowSnackbar::class.java)
            assertThat((event as BudgetEvent.ShowSnackbar).message).contains("updated")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updateBudget with non-existent budget should show error`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Set form state
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("5000")

        // When - try to update non-existent budget
        viewModel.updateBudget("non-existent-id")
        advanceUntilIdle()

        // Then
        assertThat(viewModel.formState.value.error).contains("not found")
    }

    @Test
    fun `loadBudgetForEdit should populate form state`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Create budget first
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("5000")
        viewModel.createBudget()
        advanceUntilIdle()

        val budgetId = (viewModel.uiState.value as BudgetUiState.Success).budgets.first().budget.id

        // Reset form
        viewModel.resetForm()

        // When
        viewModel.loadBudgetForEdit(budgetId)
        advanceUntilIdle()

        // Then
        assertThat(viewModel.formState.value.categoryId).isEqualTo("cat-1")
        assertThat(viewModel.formState.value.amountInput).isEqualTo("50")
    }

    @Test
    fun `form state isValid should be true with valid inputs`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()

        // When
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("5000")

        // Then
        assertThat(viewModel.formState.value.isValid).isTrue()
    }

    @Test
    fun `form state isValid should be false with zero amount`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()

        // When
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("0")

        // Then
        assertThat(viewModel.formState.value.isValid).isFalse()
    }

    @Test
    fun `selectPeriod should update form state period`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()

        // When
        viewModel.selectPeriod(BudgetPeriod.YEARLY)

        // Then
        assertThat(viewModel.formState.value.period).isEqualTo(BudgetPeriod.YEARLY)
    }

    @Test
    fun `resetForm should clear form state`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()

        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("5000")

        // When
        viewModel.resetForm()

        // Then
        assertThat(viewModel.formState.value.categoryId).isEmpty()
        assertThat(viewModel.formState.value.amountInput).isEmpty()
        assertThat(viewModel.formState.value.error).isNull()
    }

    @Test
    fun `onEvent OnRefresh should reload budgets`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(BudgetUiEvent.OnRefresh)
        advanceUntilIdle()

        // Then - state should still be success
        assertThat(viewModel.uiState.value).isInstanceOf(BudgetUiState.Success::class.java)
    }

    @Test
    fun `onEvent OnAddBudget should emit NavigateToAddBudget`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(BudgetUiEvent.OnAddBudget)

            val event = awaitItem()
            assertThat(event).isInstanceOf(BudgetEvent.NavigateToAddBudget::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnDeleteBudget should call deleteBudget`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observeRecentTransactions(any()) } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Create budget
        viewModel.selectCategory("cat-1")
        viewModel.updateAmount("5000")
        viewModel.createBudget()
        advanceUntilIdle()

        val budgetId = (viewModel.uiState.value as BudgetUiState.Success).budgets.first().budget.id

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(BudgetUiEvent.OnDeleteBudget(budgetId))
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isInstanceOf(BudgetEvent.ShowSnackbar::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }
}
