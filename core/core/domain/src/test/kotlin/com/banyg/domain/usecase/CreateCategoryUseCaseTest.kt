package com.banyg.domain.usecase

import com.banyg.domain.model.Category
import com.banyg.domain.repository.CategoryRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreateCategoryUseCaseTest {

    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val useCase = CreateCategoryUseCase(categoryRepository)

    @Test
    fun `invoke with valid name should create category`() = runTest {
        // Given
        every { categoryRepository.getAllCategories() } returns emptyList()

        // When
        val result = useCase(name = "Food")

        // Then
        assertThat(result).isInstanceOf(CreateCategoryUseCase.Result.Success::class.java)
        val success = result as CreateCategoryUseCase.Result.Success
        assertThat(success.category.name).isEqualTo("Food")
        assertThat(success.category.isHidden).isFalse()
        coVerify { categoryRepository.saveCategory(any()) }
    }

    @Test
    fun `invoke with blank name should return error`() = runTest {
        // When
        val result = useCase(name = "   ")

        // Then
        assertThat(result).isInstanceOf(CreateCategoryUseCase.Result.Error::class.java)
        val error = result as CreateCategoryUseCase.Result.Error
        assertThat(error.message).contains("blank")
    }

    @Test
    fun `invoke with empty name should return error`() = runTest {
        // When
        val result = useCase(name = "")

        // Then
        assertThat(result).isInstanceOf(CreateCategoryUseCase.Result.Error::class.java)
    }

    @Test
    fun `invoke with name too long should return error`() = runTest {
        // Given
        val longName = "A".repeat(51)

        // When
        val result = useCase(name = longName)

        // Then
        assertThat(result).isInstanceOf(CreateCategoryUseCase.Result.Error::class.java)
        val error = result as CreateCategoryUseCase.Result.Error
        assertThat(error.message).contains("50 characters")
    }

    @Test
    fun `invoke with duplicate name in same group should return error`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Food", groupId = "group1")
        every { categoryRepository.getAllCategories() } returns listOf(existingCategory)

        // When
        val result = useCase(name = "Food", groupId = "group1")

        // Then
        assertThat(result).isInstanceOf(CreateCategoryUseCase.Result.Error::class.java)
        val error = result as CreateCategoryUseCase.Result.Error
        assertThat(error.message).contains("already exists")
    }

    @Test
    fun `invoke with duplicate name but different group should succeed`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Food", groupId = "group1")
        every { categoryRepository.getAllCategories() } returns listOf(existingCategory)

        // When
        val result = useCase(name = "Food", groupId = "group2")

        // Then
        assertThat(result).isInstanceOf(CreateCategoryUseCase.Result.Success::class.java)
    }

    @Test
    fun `invoke should trim name`() = runTest {
        // Given
        every { categoryRepository.getAllCategories() } returns emptyList()

        // When
        val result = useCase(name = "  Food  ")

        // Then
        assertThat(result).isInstanceOf(CreateCategoryUseCase.Result.Success::class.java)
        val success = result as CreateCategoryUseCase.Result.Success
        assertThat(success.category.name).isEqualTo("Food")
    }

    @Test
    fun `invoke should save category with all properties`() = runTest {
        // Given
        every { categoryRepository.getAllCategories() } returns emptyList()

        // When
        val result = useCase(
            name = "Food",
            groupId = "food-group",
            groupName = "Food & Dining",
            icon = "restaurant",
            color = "#FF0000"
        )

        // Then
        assertThat(result).isInstanceOf(CreateCategoryUseCase.Result.Success::class.java)
        val success = result as CreateCategoryUseCase.Result.Success
        assertThat(success.category.name).isEqualTo("Food")
        assertThat(success.category.groupId).isEqualTo("food-group")
        assertThat(success.category.groupName).isEqualTo("Food & Dining")
        assertThat(success.category.icon).isEqualTo("restaurant")
        assertThat(success.category.color).isEqualTo("#FF0000")
    }

    @Test
    fun `invoke when repository throws should return error`() = runTest {
        // Given
        every { categoryRepository.getAllCategories() } returns emptyList()
        coEvery { categoryRepository.saveCategory(any()) } throws RuntimeException("DB Error")

        // When
        val result = useCase(name = "Food")

        // Then
        assertThat(result).isInstanceOf(CreateCategoryUseCase.Result.Error::class.java)
        val error = result as CreateCategoryUseCase.Result.Error
        assertThat(error.message).isEqualTo("DB Error")
    }
}
