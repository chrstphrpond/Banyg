package com.banyg.domain.usecase

import com.banyg.domain.model.Category
import com.banyg.domain.model.CategoryGroups
import com.banyg.domain.repository.CategoryRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SeedDefaultCategoriesUseCaseTest {

    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val useCase = SeedDefaultCategoriesUseCase(categoryRepository)

    @Test
    fun `invoke with empty database should create all default categories`() = runTest {
        // Given
        coEvery { categoryRepository.getAllCategories() } returns emptyList()

        // When
        val result = useCase()

        // Then
        assertThat(result).isInstanceOf(SeedDefaultCategoriesUseCase.Result.Success::class.java)
        val success = result as SeedDefaultCategoriesUseCase.Result.Success
        assertThat(success.created).isGreaterThan(0)
        assertThat(success.skipped).isEqualTo(0)
        coVerify { categoryRepository.saveCategories(any()) }
    }

    @Test
    fun `invoke with existing categories should skip duplicates`() = runTest {
        // Given
        val existingCategories = listOf(
            Category(id = "1", name = "Salary", groupId = CategoryGroups.INCOME),
            Category(id = "2", name = "Groceries", groupId = CategoryGroups.FOOD)
        )
        coEvery { categoryRepository.getAllCategories() } returns existingCategories

        // When
        val result = useCase()

        // Then
        assertThat(result).isInstanceOf(SeedDefaultCategoriesUseCase.Result.Success::class.java)
        val success = result as SeedDefaultCategoriesUseCase.Result.Success
        assertThat(success.skipped).isEqualTo(2)
        assertThat(success.created).isGreaterThan(0)
    }

    @Test
    fun `invoke with all existing categories should skip all`() = runTest {
        // Given - all default categories already exist
        val existingCategories = useCase.getDefaultCategories()
        coEvery { categoryRepository.getAllCategories() } returns existingCategories

        // When
        val result = useCase()

        // Then
        assertThat(result).isInstanceOf(SeedDefaultCategoriesUseCase.Result.Success::class.java)
        val success = result as SeedDefaultCategoriesUseCase.Result.Success
        assertThat(success.created).isEqualTo(0)
        assertThat(success.skipped).isEqualTo(existingCategories.size)
    }

    @Test
    fun `invoke with case insensitive matching should skip duplicates`() = runTest {
        // Given - existing with different case
        val existingCategories = listOf(
            Category(id = "1", name = "SALARY", groupId = CategoryGroups.INCOME),
            Category(id = "2", name = "groceries", groupId = CategoryGroups.FOOD)
        )
        coEvery { categoryRepository.getAllCategories() } returns existingCategories

        // When
        val result = useCase()

        // Then
        assertThat(result).isInstanceOf(SeedDefaultCategoriesUseCase.Result.Success::class.java)
        val success = result as SeedDefaultCategoriesUseCase.Result.Success
        assertThat(success.skipped).isEqualTo(2)
    }

    @Test
    fun `getDefaultCategories should return predefined list`() = runTest {
        // When
        val categories = useCase.getDefaultCategories()

        // Then
        assertThat(categories).isNotEmpty()

        // Check for specific groups
        val groupNames = categories.mapNotNull { it.groupName }.distinct()
        assertThat(groupNames).contains("Income")
        assertThat(groupNames).contains("Food")
        assertThat(groupNames).contains("Transportation")
        assertThat(groupNames).contains("Shopping")
        assertThat(groupNames).contains("Bills & Utilities")
        assertThat(groupNames).contains("Entertainment")

        // All categories should have names
        assertThat(categories.all { it.name.isNotBlank() }).isTrue()

        // All categories should have IDs
        assertThat(categories.all { it.id.isNotBlank() }).isTrue()
    }

    @Test
    fun `invoke when repository throws should return error`() = runTest {
        // Given
        coEvery { categoryRepository.getAllCategories() } throws RuntimeException("DB Error")

        // When
        val result = useCase()

        // Then
        assertThat(result).isInstanceOf(SeedDefaultCategoriesUseCase.Result.Error::class.java)
        val error = result as SeedDefaultCategoriesUseCase.Result.Error
        assertThat(error.message).isEqualTo("DB Error")
    }
}
