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

class UpdateCategoryUseCaseTest {

    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val useCase = UpdateCategoryUseCase(categoryRepository)

    @Test
    fun `invoke with valid update should succeed`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Old Name", isHidden = false)
        coEvery { categoryRepository.getCategory("1") } returns existingCategory
        coEvery { categoryRepository.getAllCategories() } returns listOf(existingCategory)

        // When
        val result = useCase(id = "1", name = "New Name")

        // Then
        assertThat(result).isInstanceOf(UpdateCategoryUseCase.Result.Success::class.java)
        val success = result as UpdateCategoryUseCase.Result.Success
        assertThat(success.category.name).isEqualTo("New Name")
        coVerify { categoryRepository.saveCategory(any()) }
    }

    @Test
    fun `invoke with nonexistent category should return error`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("1") } returns null

        // When
        val result = useCase(id = "1", name = "New Name")

        // Then
        assertThat(result).isInstanceOf(UpdateCategoryUseCase.Result.Error::class.java)
        val error = result as UpdateCategoryUseCase.Result.Error
        assertThat(error.message).contains("not found")
    }

    @Test
    fun `invoke with blank name should return error`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Old Name", isHidden = false)
        coEvery { categoryRepository.getCategory("1") } returns existingCategory

        // When
        val result = useCase(id = "1", name = "   ")

        // Then
        assertThat(result).isInstanceOf(UpdateCategoryUseCase.Result.Error::class.java)
        val error = result as UpdateCategoryUseCase.Result.Error
        assertThat(error.message).contains("blank")
    }

    @Test
    fun `invoke with name too long should return error`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Old Name", isHidden = false)
        coEvery { categoryRepository.getCategory("1") } returns existingCategory
        val longName = "A".repeat(51)

        // When
        val result = useCase(id = "1", name = longName)

        // Then
        assertThat(result).isInstanceOf(UpdateCategoryUseCase.Result.Error::class.java)
    }

    @Test
    fun `invoke with duplicate name should return error`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Food", groupId = "group1")
        val otherCategory = Category(id = "2", name = "Snacks", groupId = "group1")
        coEvery { categoryRepository.getCategory("1") } returns existingCategory
        coEvery { categoryRepository.getAllCategories() } returns listOf(existingCategory, otherCategory)

        // When - try to rename category 1 to "Snacks" which is the name of category 2
        val result = useCase(id = "1", name = "Snacks")

        // Then
        assertThat(result).isInstanceOf(UpdateCategoryUseCase.Result.Error::class.java)
        val error = result as UpdateCategoryUseCase.Result.Error
        assertThat(error.message).contains("already exists")
    }

    @Test
    fun `invoke should keep existing values when not provided`() = runTest {
        // Given
        val existingCategory = Category(
            id = "1",
            name = "Food",
            groupId = "food-group",
            groupName = "Food Group",
            icon = "restaurant",
            color = "#FF0000",
            isHidden = false
        )
        coEvery { categoryRepository.getCategory("1") } returns existingCategory
        coEvery { categoryRepository.getAllCategories() } returns listOf(existingCategory)

        // When - only update name
        val result = useCase(id = "1", name = "Updated Food")

        // Then
        assertThat(result).isInstanceOf(UpdateCategoryUseCase.Result.Success::class.java)
        val success = result as UpdateCategoryUseCase.Result.Success
        assertThat(success.category.name).isEqualTo("Updated Food")
        assertThat(success.category.groupId).isEqualTo("food-group")
        assertThat(success.category.icon).isEqualTo("restaurant")
        assertThat(success.category.color).isEqualTo("#FF0000")
    }

    @Test
    fun `invoke should update all provided values`() = runTest {
        // Given
        val existingCategory = Category(
            id = "1",
            name = "Old Name",
            groupId = "old-group",
            groupName = "Old Group",
            icon = "old-icon",
            color = "#000000",
            isHidden = false
        )
        coEvery { categoryRepository.getCategory("1") } returns existingCategory
        coEvery { categoryRepository.getAllCategories() } returns listOf(existingCategory)

        // When
        val result = useCase(
            id = "1",
            name = "New Name",
            groupId = "new-group",
            groupName = "New Group",
            icon = "new-icon",
            color = "#FFFFFF",
            isHidden = true
        )

        // Then
        assertThat(result).isInstanceOf(UpdateCategoryUseCase.Result.Success::class.java)
        val success = result as UpdateCategoryUseCase.Result.Success
        assertThat(success.category.name).isEqualTo("New Name")
        assertThat(success.category.groupId).isEqualTo("new-group")
        assertThat(success.category.groupName).isEqualTo("New Group")
        assertThat(success.category.icon).isEqualTo("new-icon")
        assertThat(success.category.color).isEqualTo("#FFFFFF")
        assertThat(success.category.isHidden).isTrue()
    }

    @Test
    fun `invoke when repository throws should return error`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Old Name", isHidden = false)
        coEvery { categoryRepository.getCategory("1") } returns existingCategory
        coEvery { categoryRepository.getAllCategories() } returns listOf(existingCategory)
        coEvery { categoryRepository.saveCategory(any()) } throws RuntimeException("DB Error")

        // When
        val result = useCase(id = "1", name = "New Name")

        // Then
        assertThat(result).isInstanceOf(UpdateCategoryUseCase.Result.Error::class.java)
        val error = result as UpdateCategoryUseCase.Result.Error
        assertThat(error.message).isEqualTo("DB Error")
    }
}
