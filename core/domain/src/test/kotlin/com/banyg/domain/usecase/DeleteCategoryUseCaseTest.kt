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

class DeleteCategoryUseCaseTest {

    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val useCase = DeleteCategoryUseCase(categoryRepository)

    @Test
    fun `invoke with soft delete should hide category`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Food", isHidden = false)
        coEvery { categoryRepository.getCategory("1") } returns existingCategory

        // When
        val result = useCase(id = "1", hardDelete = false)

        // Then
        assertThat(result).isEqualTo(DeleteCategoryUseCase.Result.Success)
        coVerify { categoryRepository.setHidden("1", true) }
    }

    @Test
    fun `invoke with hard delete should delete category`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Food", isHidden = false)
        coEvery { categoryRepository.getCategory("1") } returns existingCategory

        // When
        val result = useCase(id = "1", hardDelete = true)

        // Then
        assertThat(result).isEqualTo(DeleteCategoryUseCase.Result.Success)
        coVerify { categoryRepository.deleteCategory("1") }
    }

    @Test
    fun `invoke with nonexistent category should return error`() = runTest {
        // Given
        coEvery { categoryRepository.getCategory("1") } returns null

        // When
        val result = useCase(id = "1")

        // Then
        assertThat(result).isInstanceOf(DeleteCategoryUseCase.Result.Error::class.java)
        val error = result as DeleteCategoryUseCase.Result.Error
        assertThat(error.message).contains("not found")
    }

    @Test
    fun `restore hidden category should succeed`() = runTest {
        // Given
        val hiddenCategory = Category(id = "1", name = "Food", isHidden = true)
        coEvery { categoryRepository.getCategory("1") } returns hiddenCategory

        // When
        val result = useCase.restore("1")

        // Then
        assertThat(result).isEqualTo(DeleteCategoryUseCase.Result.Success)
        coVerify { categoryRepository.setHidden("1", false) }
    }

    @Test
    fun `restore visible category should return error`() = runTest {
        // Given
        val visibleCategory = Category(id = "1", name = "Food", isHidden = false)
        coEvery { categoryRepository.getCategory("1") } returns visibleCategory

        // When
        val result = useCase.restore("1")

        // Then
        assertThat(result).isInstanceOf(DeleteCategoryUseCase.Result.Error::class.java)
        val error = result as DeleteCategoryUseCase.Result.Error
        assertThat(error.message).contains("not hidden")
    }

    @Test
    fun `toggleHidden on visible category should hide it`() = runTest {
        // Given
        val visibleCategory = Category(id = "1", name = "Food", isHidden = false)
        coEvery { categoryRepository.getCategory("1") } returns visibleCategory

        // When
        val result = useCase.toggleHidden("1")

        // Then
        assertThat(result).isEqualTo(DeleteCategoryUseCase.Result.Success)
        coVerify { categoryRepository.setHidden("1", true) }
    }

    @Test
    fun `toggleHidden on hidden category should show it`() = runTest {
        // Given
        val hiddenCategory = Category(id = "1", name = "Food", isHidden = true)
        coEvery { categoryRepository.getCategory("1") } returns hiddenCategory

        // When
        val result = useCase.toggleHidden("1")

        // Then
        assertThat(result).isEqualTo(DeleteCategoryUseCase.Result.Success)
        coVerify { categoryRepository.setHidden("1", false) }
    }

    @Test
    fun `invoke when setHidden throws should return error`() = runTest {
        // Given
        val existingCategory = Category(id = "1", name = "Food", isHidden = false)
        coEvery { categoryRepository.getCategory("1") } returns existingCategory
        coEvery { categoryRepository.setHidden(any(), any()) } throws RuntimeException("DB Error")

        // When
        val result = useCase(id = "1")

        // Then
        assertThat(result).isInstanceOf(DeleteCategoryUseCase.Result.Error::class.java)
    }
}
