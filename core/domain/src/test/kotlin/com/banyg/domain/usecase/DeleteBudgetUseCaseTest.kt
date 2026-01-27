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

class DeleteBudgetUseCaseTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val useCase = DeleteBudgetUseCase(budgetRepository)

    private val testPeriod = BudgetPeriod.of(2026, 1)
    private val testInstant = Instant.parse("2026-01-15T10:00:00Z")

    private fun createTestBudget(
        id: String = "budget-1",
        categoryId: String = "cat-1"
    ) = Budget(
        id = id,
        categoryId = categoryId,
        amount = 50_000L,
        period = testPeriod,
        currency = Currency.PHP,
        createdAt = testInstant,
        updatedAt = testInstant
    )

    @Test
    fun `invoke with budget ID should delete budget`() = runTest {
        // Given
        val existingBudget = createTestBudget()
        coEvery { budgetRepository.getBudget("budget-1") } returns existingBudget

        // When
        val result = useCase(budgetId = "budget-1")

        // Then
        assertThat(result).isInstanceOf(DeleteBudgetUseCase.Result.Success::class.java)
        val success = result as DeleteBudgetUseCase.Result.Success
        assertThat(success.deletedId).isEqualTo("budget-1")
        coVerify { budgetRepository.deleteBudget("budget-1") }
    }

    @Test
    fun `invoke with non-existent budget ID should return error`() = runTest {
        // Given
        coEvery { budgetRepository.getBudget("non-existent") } returns null

        // When
        val result = useCase(budgetId = "non-existent")

        // Then
        assertThat(result).isInstanceOf(DeleteBudgetUseCase.Result.Error::class.java)
        val error = result as DeleteBudgetUseCase.Result.Error
        assertThat(error.message).contains("Budget not found")
    }

    @Test
    fun `invoke by category and period should delete budget`() = runTest {
        // Given
        val existingBudget = createTestBudget()
        coEvery { budgetRepository.getBudget("cat-1", testPeriod) } returns existingBudget

        // When
        val result = useCase(
            categoryId = "cat-1",
            period = testPeriod
        )

        // Then
        assertThat(result).isInstanceOf(DeleteBudgetUseCase.Result.Success::class.java)
        val success = result as DeleteBudgetUseCase.Result.Success
        assertThat(success.deletedId).isEqualTo("budget-1")
        coVerify { budgetRepository.deleteBudget("cat-1", testPeriod) }
    }

    @Test
    fun `invoke with non-existent category and period should return error`() = runTest {
        // Given
        coEvery { budgetRepository.getBudget("cat-1", testPeriod) } returns null

        // When
        val result = useCase(
            categoryId = "cat-1",
            period = testPeriod
        )

        // Then
        assertThat(result).isInstanceOf(DeleteBudgetUseCase.Result.Error::class.java)
        val error = result as DeleteBudgetUseCase.Result.Error
        assertThat(error.message).contains("Budget not found")
    }

    @Test
    fun `invoke when repository throws should return error`() = runTest {
        // Given
        val existingBudget = createTestBudget()
        coEvery { budgetRepository.getBudget("budget-1") } returns existingBudget
        coEvery { budgetRepository.deleteBudget("budget-1") } throws RuntimeException("DB Error")

        // When
        val result = useCase(budgetId = "budget-1")

        // Then
        assertThat(result).isInstanceOf(DeleteBudgetUseCase.Result.Error::class.java)
        val error = result as DeleteBudgetUseCase.Result.Error
        assertThat(error.message).isEqualTo("DB Error")
    }
}
