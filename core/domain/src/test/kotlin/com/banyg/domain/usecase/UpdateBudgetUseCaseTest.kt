package com.banyg.domain.usecase

import com.banyg.domain.model.Budget
import com.banyg.domain.model.BudgetPeriod
import com.banyg.domain.model.Currency
import com.banyg.domain.repository.BudgetRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class UpdateBudgetUseCaseTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val useCase = UpdateBudgetUseCase(budgetRepository)

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

    @Test
    fun `invoke with budget ID and valid amount should update budget`() = runTest {
        // Given
        val existingBudget = createTestBudget(amount = 50_000L)
        coEvery { budgetRepository.getBudget("budget-1") } returns existingBudget

        // When
        val result = useCase(
            budgetId = "budget-1",
            newAmount = 75_000L
        )

        // Then
        assertThat(result).isInstanceOf(UpdateBudgetUseCase.Result.Success::class.java)
        val success = result as UpdateBudgetUseCase.Result.Success
        assertThat(success.budget.amount).isEqualTo(75_000L)
        coVerify { budgetRepository.saveBudget(any()) }
    }

    @Test
    fun `invoke with non-existent budget ID should return error`() = runTest {
        // Given
        coEvery { budgetRepository.getBudget("non-existent") } returns null

        // When
        val result = useCase(
            budgetId = "non-existent",
            newAmount = 75_000L
        )

        // Then
        assertThat(result).isInstanceOf(UpdateBudgetUseCase.Result.Error::class.java)
        val error = result as UpdateBudgetUseCase.Result.Error
        assertThat(error.message).contains("Budget not found")
    }

    @Test
    fun `invoke with negative amount should return error`() = runTest {
        // Given
        val existingBudget = createTestBudget()
        coEvery { budgetRepository.getBudget("budget-1") } returns existingBudget

        // When
        val result = useCase(
            budgetId = "budget-1",
            newAmount = -1_000L
        )

        // Then
        assertThat(result).isInstanceOf(UpdateBudgetUseCase.Result.Error::class.java)
        val error = result as UpdateBudgetUseCase.Result.Error
        assertThat(error.message).contains("cannot be negative")
    }

    @Test
    fun `invoke with same amount should return success without updating`() = runTest {
        // Given
        val existingBudget = createTestBudget(amount = 50_000L)
        coEvery { budgetRepository.getBudget("budget-1") } returns existingBudget

        // When
        val result = useCase(
            budgetId = "budget-1",
            newAmount = 50_000L
        )

        // Then
        assertThat(result).isInstanceOf(UpdateBudgetUseCase.Result.Success::class.java)
        val success = result as UpdateBudgetUseCase.Result.Success
        assertThat(success.budget.amount).isEqualTo(50_000L)
    }

    @Test
    fun `invoke with zero amount should update to zero`() = runTest {
        // Given
        val existingBudget = createTestBudget(amount = 50_000L)
        coEvery { budgetRepository.getBudget("budget-1") } returns existingBudget

        // When
        val result = useCase(
            budgetId = "budget-1",
            newAmount = 0L
        )

        // Then
        assertThat(result).isInstanceOf(UpdateBudgetUseCase.Result.Success::class.java)
        val success = result as UpdateBudgetUseCase.Result.Success
        assertThat(success.budget.amount).isEqualTo(0L)
    }

    @Test
    fun `invoke by category and period should update budget`() = runTest {
        // Given
        val existingBudget = createTestBudget(amount = 50_000L)
        coEvery { budgetRepository.getBudget("cat-1", testPeriod) } returns existingBudget

        // When
        val result = useCase(
            categoryId = "cat-1",
            period = testPeriod,
            newAmount = 75_000L
        )

        // Then
        assertThat(result).isInstanceOf(UpdateBudgetUseCase.Result.Success::class.java)
        val success = result as UpdateBudgetUseCase.Result.Success
        assertThat(success.budget.amount).isEqualTo(75_000L)
    }

    @Test
    fun `invoke should update updatedAt timestamp`() = runTest {
        // Given
        val existingBudget = createTestBudget()
        val newInstant = Instant.parse("2026-01-20T15:30:00Z")
        coEvery { budgetRepository.getBudget("budget-1") } returns existingBudget

        // When
        val result = useCase(
            budgetId = "budget-1",
            newAmount = 75_000L,
            now = newInstant
        )

        // Then
        assertThat(result).isInstanceOf(UpdateBudgetUseCase.Result.Success::class.java)
        val success = result as UpdateBudgetUseCase.Result.Success
        assertThat(success.budget.updatedAt).isEqualTo(newInstant)
        assertThat(success.budget.createdAt).isEqualTo(testInstant) // unchanged
    }

    @Test
    fun `invoke when repository throws should return error`() = runTest {
        // Given
        val existingBudget = createTestBudget()
        coEvery { budgetRepository.getBudget("budget-1") } returns existingBudget
        coEvery { budgetRepository.saveBudget(any()) } throws RuntimeException("DB Error")

        // When
        val result = useCase(
            budgetId = "budget-1",
            newAmount = 75_000L
        )

        // Then
        assertThat(result).isInstanceOf(UpdateBudgetUseCase.Result.Error::class.java)
        val error = result as UpdateBudgetUseCase.Result.Error
        assertThat(error.message).isEqualTo("DB Error")
    }
}
