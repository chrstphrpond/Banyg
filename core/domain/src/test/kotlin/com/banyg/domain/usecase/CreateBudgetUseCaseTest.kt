package com.banyg.domain.usecase

import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.model.Category
import com.banyg.domain.model.Currency
import com.banyg.domain.repository.BudgetRepository
import com.banyg.domain.repository.CategoryRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class CreateBudgetUseCaseTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val useCase = CreateBudgetUseCase(budgetRepository, categoryRepository)

    private val testCategory = Category(
        id = "cat-1",
        name = "Groceries",
        groupId = "food"
    )

    private val testPeriod = BudgetPeriod.of(2026, 1)

    @Test
    fun `invoke with valid data should create budget`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("cat-1") } returns testCategory
        coEvery { budgetRepository.getBudget("cat-1", testPeriod) } returns null

        // When
        val result = useCase(
            categoryId = "cat-1",
            amount = 50_000L, // ₱500.00
            period = testPeriod
        )

        // Then
        assertThat(result).isInstanceOf(CreateBudgetUseCase.Result.Success::class.java)
        val success = result as CreateBudgetUseCase.Result.Success
        assertThat(success.budget.categoryId).isEqualTo("cat-1")
        assertThat(success.budget.amount).isEqualTo(50_000L)
        assertThat(success.budget.period).isEqualTo(testPeriod)
        coVerify { budgetRepository.saveBudget(any()) }
    }

    @Test
    fun `invoke with non-existent category should return error`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("non-existent") } returns null

        // When
        val result = useCase(
            categoryId = "non-existent",
            amount = 50_000L
        )

        // Then
        assertThat(result).isInstanceOf(CreateBudgetUseCase.Result.Error::class.java)
        val error = result as CreateBudgetUseCase.Result.Error
        assertThat(error.message).contains("Category not found")
    }

    @Test
    fun `invoke with zero amount should return error`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("cat-1") } returns testCategory

        // When
        val result = useCase(
            categoryId = "cat-1",
            amount = 0L
        )

        // Then
        assertThat(result).isInstanceOf(CreateBudgetUseCase.Result.Error::class.java)
        val error = result as CreateBudgetUseCase.Result.Error
        assertThat(error.message).contains("greater than zero")
    }

    @Test
    fun `invoke with negative amount should return error`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("cat-1") } returns testCategory

        // When
        val result = useCase(
            categoryId = "cat-1",
            amount = -1_000L
        )

        // Then
        assertThat(result).isInstanceOf(CreateBudgetUseCase.Result.Error::class.java)
        val error = result as CreateBudgetUseCase.Result.Error
        assertThat(error.message).contains("greater than zero")
    }

    @Test
    fun `invoke with duplicate budget should return error`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("cat-1") } returns testCategory
        val existingBudget = Budget(
            id = "existing",
            categoryId = "cat-1",
            amount = 30_000L,
            period = testPeriod,
            currency = Currency.PHP,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { budgetRepository.getBudget("cat-1", testPeriod) } returns existingBudget

        // When
        val result = useCase(
            categoryId = "cat-1",
            amount = 50_000L,
            period = testPeriod
        )

        // Then
        assertThat(result).isInstanceOf(CreateBudgetUseCase.Result.Error::class.java)
        val error = result as CreateBudgetUseCase.Result.Error
        assertThat(error.message).contains("already exists")
    }

    @Test
    fun `invoke should use default period as current month`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("cat-1") } returns testCategory
        coEvery { budgetRepository.getBudget("cat-1", any()) } returns null

        // When
        val result = useCase(
            categoryId = "cat-1",
            amount = 50_000L
        )

        // Then
        assertThat(result).isInstanceOf(CreateBudgetUseCase.Result.Success::class.java)
        val success = result as CreateBudgetUseCase.Result.Success
        assertThat(success.budget.period.monthKey).isNotEmpty()
    }

    @Test
    fun `invoke should use provided currency`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("cat-1") } returns testCategory
        coEvery { budgetRepository.getBudget("cat-1", testPeriod) } returns null

        // When
        val result = useCase(
            categoryId = "cat-1",
            amount = 50_000L,
            period = testPeriod,
            currency = Currency.USD
        )

        // Then
        assertThat(result).isInstanceOf(CreateBudgetUseCase.Result.Success::class.java)
        val success = result as CreateBudgetUseCase.Result.Success
        assertThat(success.budget.currency).isEqualTo(Currency.USD)
    }

    @Test
    fun `invoke when repository throws should return error`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("cat-1") } returns testCategory
        coEvery { budgetRepository.getBudget("cat-1", testPeriod) } returns null
        coEvery { budgetRepository.saveBudget(any()) } throws RuntimeException("DB Error")

        // When
        val result = useCase(
            categoryId = "cat-1",
            amount = 50_000L,
            period = testPeriod
        )

        // Then
        assertThat(result).isInstanceOf(CreateBudgetUseCase.Result.Error::class.java)
        val error = result as CreateBudgetUseCase.Result.Error
        assertThat(error.message).isEqualTo("DB Error")
    }

    @Test
    fun `invoke should create budget with correct timestamps`() = runTest {
        // Given
        val now = Instant.parse("2026-01-15T10:00:00Z")
        coEvery { categoryRepository.getCategory("cat-1") } returns testCategory
        coEvery { budgetRepository.getBudget("cat-1", testPeriod) } returns null

        // When
        val result = useCase(
            categoryId = "cat-1",
            amount = 50_000L,
            period = testPeriod,
            now = now
        )

        // Then
        assertThat(result).isInstanceOf(CreateBudgetUseCase.Result.Success::class.java)
        val success = result as CreateBudgetUseCase.Result.Success
        assertThat(success.budget.createdAt).isEqualTo(now)
        assertThat(success.budget.updatedAt).isEqualTo(now)
    }
}
