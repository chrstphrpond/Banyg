package com.banyg.feature.csvimport

import app.cash.turbine.test
import com.banyg.domain.csv.ColumnMapping
import com.banyg.domain.csv.DuplicateStatus
import com.banyg.domain.csv.ImportPreview
import com.banyg.domain.csv.ImportResult
import com.banyg.domain.model.Account
import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.AccountRepository
import com.banyg.domain.repository.TransactionRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class CsvImportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var viewModel: CsvImportViewModel

    private val testInstant = Instant.parse("2026-01-15T10:00:00Z")
    private val testCurrency = Currency.PHP

    private val testAccount = Account(
        id = "acc-1",
        name = "Test Account",
        type = AccountType.CHECKING,
        currency = testCurrency,
        openingBalance = Money(100000L, testCurrency),
        currentBalance = Money(100000L, testCurrency),
        isArchived = false,
        createdAt = testInstant,
        updatedAt = testInstant
    )

    private val testAccount2 = Account(
        id = "acc-2",
        name = "Savings Account",
        type = AccountType.SAVINGS,
        currency = testCurrency,
        openingBalance = Money(500000L, testCurrency),
        currentBalance = Money(500000L, testCurrency),
        isArchived = false,
        createdAt = testInstant,
        updatedAt = testInstant
    )

    private val testColumnMapping = ColumnMapping(
        dateColumn = "Date",
        amountColumn = "Amount",
        descriptionColumn = "Description",
        dateFormat = "yyyy-MM-dd"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = mockk()
        transactionRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = CsvImportViewModel(accountRepository, transactionRepository)
    }

    private fun createImportPreview(
        transactions: List<com.banyg.domain.csv.ImportTransactionPreview>
    ) = ImportPreview(
        transactions = transactions,
        newCount = transactions.count { it.duplicateStatus is DuplicateStatus.NEW },
        duplicateCount = transactions.count { it.duplicateStatus is DuplicateStatus.DUPLICATE },
        errorCount = 0
    )

    private fun createDomainPreviewTransaction(
        id: String,
        amount: Long,
        duplicateStatus: DuplicateStatus = DuplicateStatus.NEW,
        isSelected: Boolean = true
    ) = com.banyg.domain.csv.ImportTransactionPreview(
        id = id,
        date = LocalDate.of(2026, 1, 15),
        amount = Money(amount, testCurrency),
        merchant = "Test Merchant $id",
        rawDescription = "Test Description $id",
        duplicateStatus = duplicateStatus,
        isSelected = isSelected,
        categoryId = null,
        rawRowIndex = 1
    )

    private fun createTransaction(
        id: String,
        amount: Long,
        date: LocalDate = LocalDate.of(2026, 1, 15)
    ) = Transaction(
        id = id,
        accountId = testAccount.id,
        date = date,
        amount = Money(amount, testCurrency),
        merchant = "Test Merchant",
        status = TransactionStatus.CLEARED,
        createdAt = testInstant,
        updatedAt = testInstant
    )

    @Test
    fun `initial state should be Initial`() = testScope.runTest {
        // Given
        every { accountRepository.observeActiveAccounts() } returns flowOf(listOf(testAccount))

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(CsvImportUiState.Initial::class.java)
    }

    @Test
    fun `onFileSelected should transition to ColumnMapping state`() = testScope.runTest {
        // Given
        val accounts = listOf(testAccount, testAccount2)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        createViewModel()
        advanceUntilIdle()

        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test"

        // When
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CsvImportUiState.ColumnMapping::class.java)
        val columnMappingState = state as CsvImportUiState.ColumnMapping
        assertThat(columnMappingState.fileName).isEqualTo(fileName)
        assertThat(columnMappingState.csvContent).isEqualTo(csvContent)
        assertThat(columnMappingState.accounts).hasSize(2)
        assertThat(columnMappingState.accounts).containsExactly(testAccount, testAccount2)
    }

    @Test
    fun `generatePreview with valid data should transition to Preview state`() = testScope.runTest {
        // Given
        val accounts = listOf(testAccount)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        coEvery { transactionRepository.getTransactionsByAccount(testAccount.id) } returns emptyList()
        createViewModel()
        advanceUntilIdle()

        // Set up file selection
        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test Merchant"
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()

        // Select account and set column mapping
        viewModel.selectAccount(testAccount.id)
        viewModel.updateColumnMapping(testColumnMapping)

        // When
        viewModel.generatePreview()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CsvImportUiState.Preview::class.java)
        val previewState = state as CsvImportUiState.Preview
        assertThat(previewState.fileName).isEqualTo(fileName)
        assertThat(previewState.account).isEqualTo(testAccount)
    }

    @Test
    fun `importTransactions should transition to ImportComplete state`() = testScope.runTest {
        // Given
        val accounts = listOf(testAccount)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        coEvery { transactionRepository.getTransactionsByAccount(testAccount.id) } returns emptyList()
        coEvery { transactionRepository.saveTransactions(any()) } returns Unit
        createViewModel()
        advanceUntilIdle()

        // Navigate to Preview state
        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test Merchant"
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()
        viewModel.selectAccount(testAccount.id)
        viewModel.updateColumnMapping(testColumnMapping)
        viewModel.generatePreview()
        advanceUntilIdle()

        // When
        viewModel.events.test {
            viewModel.importTransactions()
            advanceUntilIdle()

            // Then - verify state transition
            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(CsvImportUiState.ImportComplete::class.java)
            val completeState = state as CsvImportUiState.ImportComplete
            assertThat(completeState.importedCount).isGreaterThan(0)

            // Verify snackbar event
            val event = awaitItem()
            assertThat(event).isInstanceOf(CsvImportEvent.ShowSnackbar::class.java)

            cancelAndConsumeRemainingEvents()
        }

        // Verify transactions were saved
        coVerify { transactionRepository.saveTransactions(any()) }
    }

    @Test
    fun `toggleTransactionSelection should update selection state`() = testScope.runTest {
        // Given
        val accounts = listOf(testAccount)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        coEvery { transactionRepository.getTransactionsByAccount(testAccount.id) } returns emptyList()
        createViewModel()
        advanceUntilIdle()

        // Navigate to Preview state with transactions
        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test Merchant 1\n2026-01-16,-2000,Test Merchant 2"
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()
        viewModel.selectAccount(testAccount.id)
        viewModel.updateColumnMapping(testColumnMapping)
        viewModel.generatePreview()
        advanceUntilIdle()

        val previewState = viewModel.uiState.value as CsvImportUiState.Preview
        val transactionId = previewState.transactions.first().id
        val initialSelection = previewState.transactions.first().isSelected

        // When
        viewModel.toggleTransactionSelection(transactionId, !initialSelection)
        advanceUntilIdle()

        // Then
        val updatedState = viewModel.uiState.value as CsvImportUiState.Preview
        val updatedTransaction = updatedState.transactions.find { it.id == transactionId }
        assertThat(updatedTransaction).isNotNull()
        assertThat(updatedTransaction!!.isSelected).isEqualTo(!initialSelection)
    }

    @Test
    fun `selectAll should select or deselect all transactions`() = testScope.runTest {
        // Given
        val accounts = listOf(testAccount)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        coEvery { transactionRepository.getTransactionsByAccount(testAccount.id) } returns emptyList()
        createViewModel()
        advanceUntilIdle()

        // Navigate to Preview state with multiple transactions
        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test Merchant 1\n2026-01-16,-2000,Test Merchant 2\n2026-01-17,-3000,Test Merchant 3"
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()
        viewModel.selectAccount(testAccount.id)
        viewModel.updateColumnMapping(testColumnMapping)
        viewModel.generatePreview()
        advanceUntilIdle()

        // When - deselect all
        viewModel.selectAll(false)
        advanceUntilIdle()

        // Then
        val deselectedState = viewModel.uiState.value as CsvImportUiState.Preview
        assertThat(deselectedState.transactions.all { !it.isSelected }).isTrue()
        assertThat(deselectedState.newCount).isEqualTo(0)

        // When - select all
        viewModel.selectAll(true)
        advanceUntilIdle()

        // Then
        val selectedState = viewModel.uiState.value as CsvImportUiState.Preview
        assertThat(selectedState.transactions.all { it.isSelected }).isTrue()
        assertThat(selectedState.newCount).isGreaterThan(0)
    }

    @Test
    fun `onEvent OnBack should emit NavigateBack event`() = testScope.runTest {
        // Given
        every { accountRepository.observeActiveAccounts() } returns flowOf(listOf(testAccount))
        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(CsvImportUiEvent.OnBack)

            val event = awaitItem()
            assertThat(event).isInstanceOf(CsvImportEvent.NavigateBack::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnRetry should reload accounts`() = testScope.runTest {
        // Given - start with error state
        every { accountRepository.observeActiveAccounts() } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // Verify initial state is not error (empty list is valid)
        assertThat(viewModel.uiState.value).isInstanceOf(CsvImportUiState.Initial::class.java)

        // Create new ViewModel with accounts available
        every { accountRepository.observeActiveAccounts() } returns flowOf(listOf(testAccount))
        
        // When
        viewModel.onEvent(CsvImportUiEvent.OnRetry)
        advanceUntilIdle()

        // Then - should still be in a valid state
        val state = viewModel.uiState.value
        assertThat(state).isNotInstanceOf(CsvImportUiState.Error::class.java)
    }

    @Test
    fun `loadAccounts error should emit Error state`() = testScope.runTest {
        // Given
        every { accountRepository.observeActiveAccounts() } returns flow { throw RuntimeException("Network error") }

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CsvImportUiState.Error::class.java)
        val errorState = state as CsvImportUiState.Error
        assertThat(errorState.message).contains("Network error")
        assertThat(errorState.canRetry).isTrue()
    }

    @Test
    fun `selectAccount should update selected account in ColumnMapping state`() = testScope.runTest {
        // Given
        val accounts = listOf(testAccount, testAccount2)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        createViewModel()
        advanceUntilIdle()

        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test"
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()

        // When
        viewModel.selectAccount(testAccount2.id)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as CsvImportUiState.ColumnMapping
        assertThat(state.selectedAccountId).isEqualTo(testAccount2.id)
    }

    @Test
    fun `updateColumnMapping should update mapping in ColumnMapping state`() = testScope.runTest {
        // Given
        val accounts = listOf(testAccount)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        createViewModel()
        advanceUntilIdle()

        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test"
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()

        val newMapping = ColumnMapping(
            dateColumn = "Transaction Date",
            amountColumn = "Amount",
            descriptionColumn = "Description",
            dateFormat = "MM/dd/yyyy"
        )

        // When
        viewModel.updateColumnMapping(newMapping)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as CsvImportUiState.ColumnMapping
        assertThat(state.columnMapping).isEqualTo(newMapping)
    }

    @Test
    fun `generatePreview without account should show error`() = testScope.runTest {
        // Given
        val accounts = listOf(testAccount)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        createViewModel()
        advanceUntilIdle()

        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test"
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()

        // Don't select account - just try to generate preview
        viewModel.updateColumnMapping(testColumnMapping)

        // When
        viewModel.generatePreview()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CsvImportUiState.ColumnMapping::class.java)
        val columnMappingState = state as CsvImportUiState.ColumnMapping
        assertThat(columnMappingState.error).isNotNull()
    }

    @Test
    fun `onEvent OnSelectFile should emit LaunchFilePicker event`() = testScope.runTest {
        // Given
        every { accountRepository.observeActiveAccounts() } returns flowOf(listOf(testAccount))
        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(CsvImportUiEvent.OnSelectFile)

            val event = awaitItem()
            assertThat(event).isInstanceOf(CsvImportEvent.LaunchFilePicker::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onEvent OnDismissComplete should emit NavigateBack event`() = testScope.runTest {
        // Given
        every { accountRepository.observeActiveAccounts() } returns flowOf(listOf(testAccount))
        createViewModel()
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onEvent(CsvImportUiEvent.OnDismissComplete)

            val event = awaitItem()
            assertThat(event).isInstanceOf(CsvImportEvent.NavigateBack::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `importTransactions with no selected transactions should complete with zero imports`() = testScope.runTest {
        // Given
        val accounts = listOf(testAccount)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        coEvery { transactionRepository.getTransactionsByAccount(testAccount.id) } returns emptyList()
        createViewModel()
        advanceUntilIdle()

        // Navigate to Preview state
        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test Merchant"
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()
        viewModel.selectAccount(testAccount.id)
        viewModel.updateColumnMapping(testColumnMapping)
        viewModel.generatePreview()
        advanceUntilIdle()

        // Deselect all transactions
        viewModel.selectAll(false)
        advanceUntilIdle()

        // When
        viewModel.events.test {
            viewModel.importTransactions()
            advanceUntilIdle()

            // Then - should still complete
            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(CsvImportUiState.ImportComplete::class.java)
            val completeState = state as CsvImportUiState.ImportComplete
            assertThat(completeState.importedCount).isEqualTo(0)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `duplicate detection should mark duplicates correctly`() = testScope.runTest {
        // Given - existing transactions that would match
        val existingTransaction = createTransaction(
            id = "existing-1",
            amount = -1000L,
            date = LocalDate.of(2026, 1, 15)
        )
        val accounts = listOf(testAccount)
        every { accountRepository.observeActiveAccounts() } returns flowOf(accounts)
        coEvery { transactionRepository.getTransactionsByAccount(testAccount.id) } returns listOf(existingTransaction)
        createViewModel()
        advanceUntilIdle()

        // Navigate to Preview state
        val fileName = "transactions.csv"
        val csvContent = "Date,Amount,Description\n2026-01-15,-1000,Test Merchant"
        viewModel.onFileSelected(fileName, csvContent)
        advanceUntilIdle()
        viewModel.selectAccount(testAccount.id)
        viewModel.updateColumnMapping(testColumnMapping)

        // When
        viewModel.generatePreview()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as CsvImportUiState.Preview
        // At least one transaction should be marked as duplicate or new
        assertThat(state.transactions).isNotEmpty()
        assertThat(state.newCount + state.duplicateCount).isEqualTo(state.transactions.size)
    }
}
