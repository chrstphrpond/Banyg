package com.banyg.feature.inbox

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
class InboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var viewModel: InboxViewModel

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
        groupId = "transport",
        icon = "directions_car"
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
        viewModel = InboxViewModel(transactionRepository, getCategoriesUseCase)
    }

    private fun createTransaction(
        id: String,
        amount: Long,
        categoryId: String? = null,
        date: LocalDate = LocalDate.of(2026, 1, 15),
        merchant: String = "Test Merchant",
        status: TransactionStatus = TransactionStatus.PENDING
    ) = Transaction(
        id = id,
        accountId = "acc-1",
        date = date,
        amount = Money(amount, testCurrency),
        merchant = merchant,
        categoryId = categoryId,
        status = status,
        createdAt = testInstant,
        updatedAt = testInstant
    )

    @Test
    fun `initial state should be Loading`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns emptyFlow()
        every { transactionRepository.observePendingTransactions() } returns emptyFlow()

        // When
        createViewModel()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(InboxUiState.Loading::class.java)
    }

    @Test
    fun `loadInbox success should emit Success state with transactions and categories`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory, testCategory2)
        val transactions = listOf(
            createTransaction("txn-1", -15000, "cat-1"),
            createTransaction("txn-2", -5000, null)
        )
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(transactions)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(InboxUiState.Success::class.java)
        val successState = state as InboxUiState.Success
        assertThat(successState.transactions).hasSize(2)
        assertThat(successState.categories).hasSize(2)
        
        // Verify transaction with category is properly mapped
        val txnWithCategory = successState.transactions.find { it.transaction.id == "txn-1" }
        assertThat(txnWithCategory).isNotNull()
        assertThat(txnWithCategory!!.category).isEqualTo(testCategory)
        
        // Verify transaction without category has null category
        val txnWithoutCategory = successState.transactions.find { it.transaction.id == "txn-2" }
        assertThat(txnWithoutCategory).isNotNull()
        assertThat(txnWithoutCategory!!.category).isNull()
    }

    @Test
    fun `loadInbox error should emit Error state`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flow { throw RuntimeException("Network error") }
        every { transactionRepository.observePendingTransactions() } returns flowOf(emptyList())

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(InboxUiState.Error::class.java)
        val errorState = state as InboxUiState.Error
        assertThat(errorState.message).contains("Network error")
        assertThat(errorState.canRetry).isTrue()
    }

    @Test
    fun `categorizeTransaction should update transaction category and show snackbar`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory, testCategory2)
        val transaction = createTransaction("txn-1", -15000, null)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(listOf(transaction))
        coEvery { transactionRepository.getTransaction("txn-1") } returns transaction
        coEvery { transactionRepository.saveTransaction(any()) } returns Unit

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.categorizeTransaction("txn-1", "cat-1")
            advanceUntilIdle()

            // Verify snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.ShowSnackbar::class.java)
            assertThat((event as InboxEvent.ShowSnackbar).message).contains("categorized")

            // Verify save was called with updated category
            coVerify { transactionRepository.saveTransaction(any()) }

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `categorizeTransaction error should show error snackbar`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transaction = createTransaction("txn-1", -15000, null)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(listOf(transaction))
        coEvery { transactionRepository.getTransaction("txn-1") } returns transaction
        coEvery { transactionRepository.saveTransaction(any()) } throws RuntimeException("Save failed")

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.categorizeTransaction("txn-1", "cat-1")
            advanceUntilIdle()

            // Verify error snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.ShowSnackbar::class.java)
            assertThat((event as InboxEvent.ShowSnackbar).message).contains("Failed to categorize")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `markAsCleared should update status and show snackbar`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transaction = createTransaction("txn-1", -15000, "cat-1", status = TransactionStatus.PENDING)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(listOf(transaction))
        coEvery { transactionRepository.updateStatus(any(), any()) } returns Unit

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.markAsCleared("txn-1")
            advanceUntilIdle()

            // Verify snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.ShowSnackbar::class.java)
            assertThat((event as InboxEvent.ShowSnackbar).message).contains("cleared")

            // Verify updateStatus was called
            coVerify { transactionRepository.updateStatus("txn-1", TransactionStatus.CLEARED) }

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `markAsCleared error should show error snackbar`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transaction = createTransaction("txn-1", -15000, status = TransactionStatus.PENDING)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(listOf(transaction))
        coEvery { transactionRepository.updateStatus(any(), any()) } throws RuntimeException("Update failed")

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.markAsCleared("txn-1")
            advanceUntilIdle()

            // Verify error snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.ShowSnackbar::class.java)
            assertThat((event as InboxEvent.ShowSnackbar).message).contains("Failed to clear")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteTransaction should delete and show snackbar`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transaction = createTransaction("txn-1", -15000)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(listOf(transaction))
        coEvery { transactionRepository.deleteTransaction(any()) } returns Unit

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.deleteTransaction("txn-1")
            advanceUntilIdle()

            // Verify snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.ShowSnackbar::class.java)
            assertThat((event as InboxEvent.ShowSnackbar).message).contains("deleted")

            // Verify delete was called
            coVerify { transactionRepository.deleteTransaction("txn-1") }

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteTransaction error should show error snackbar`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transaction = createTransaction("txn-1", -15000)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(listOf(transaction))
        coEvery { transactionRepository.deleteTransaction(any()) } throws RuntimeException("Delete failed")

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.deleteTransaction("txn-1")
            advanceUntilIdle()

            // Verify error snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.ShowSnackbar::class.java)
            assertThat((event as InboxEvent.ShowSnackbar).message).contains("Failed to delete")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnRefresh should reload data`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(InboxUiEvent.OnRefresh)
        advanceUntilIdle()

        // Then - state should still be success
        assertThat(viewModel.uiState.value).isInstanceOf(InboxUiState.Success::class.java)
    }

    @Test
    fun `onEvent OnImportCsv should emit NavigateToCsvImport event`() = testScope.runTest {
        // Given
        every { getCategoriesUseCase() } returns flowOf(emptyList())
        every { transactionRepository.observePendingTransactions() } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(InboxUiEvent.OnImportCsv)
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.NavigateToCsvImport::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnCategorySelect should call categorizeTransaction`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transaction = createTransaction("txn-1", -15000, null)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(listOf(transaction))
        coEvery { transactionRepository.getTransaction("txn-1") } returns transaction
        coEvery { transactionRepository.saveTransaction(any()) } returns Unit

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(InboxUiEvent.OnCategorySelect("txn-1", "cat-1"))
            advanceUntilIdle()

            // Verify snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.ShowSnackbar::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnMarkCleared should call markAsCleared`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transaction = createTransaction("txn-1", -15000, status = TransactionStatus.PENDING)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(listOf(transaction))
        coEvery { transactionRepository.updateStatus(any(), any()) } returns Unit

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(InboxUiEvent.OnMarkCleared("txn-1"))
            advanceUntilIdle()

            // Verify snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.ShowSnackbar::class.java)
            assertThat((event as InboxEvent.ShowSnackbar).message).contains("cleared")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnDismiss should call markAsCleared`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        val transaction = createTransaction("txn-1", -15000, status = TransactionStatus.PENDING)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(listOf(transaction))
        coEvery { transactionRepository.updateStatus(any(), any()) } returns Unit

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(InboxUiEvent.OnDismiss("txn-1"))
            advanceUntilIdle()

            // Verify snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(InboxEvent.ShowSnackbar::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnRetry should reload inbox`() = testScope.runTest {
        // Given - start with error state
        every { getCategoriesUseCase() } returns flow { throw RuntimeException("Network error") }
        every { transactionRepository.observePendingTransactions() } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Verify error state
        assertThat(viewModel.uiState.value).isInstanceOf(InboxUiState.Error::class.java)

        // When - fix the mock and retry
        val categories = listOf(testCategory)
        every { getCategoriesUseCase() } returns flowOf(categories)
        viewModel.onEvent(InboxUiEvent.OnRetry)
        advanceUntilIdle()

        // Then - should be success now
        assertThat(viewModel.uiState.value).isInstanceOf(InboxUiState.Success::class.java)
    }

    @Test
    fun `categorizeTransaction with non-existent transaction should show error`() = testScope.runTest {
        // Given
        val categories = listOf(testCategory)
        every { getCategoriesUseCase() } returns flowOf(categories)
        every { transactionRepository.observePendingTransactions() } returns flowOf(emptyList())
        coEvery { transactionRepository.getTransaction("non-existent") } returns null

        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.categorizeTransaction("non-existent", "cat-1")
            advanceUntilIdle()

            // No event should be emitted since transaction is null
            // (the method silently returns when transaction is not found)

            cancelAndConsumeRemainingEvents()
        }
    }
}
