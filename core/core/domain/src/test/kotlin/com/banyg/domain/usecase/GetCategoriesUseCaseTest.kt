package com.banyg.domain.usecase

import com.banyg.domain.model.Category
import com.banyg.domain.repository.CategoryRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetCategoriesUseCaseTest {

    private val categoryRepository: CategoryRepository = mockk()
    private val useCase = GetCategoriesUseCase(categoryRepository)

    @Test
    fun `invoke should return visible categories flow`() = runTest {
        // Given
        val categories = listOf(
            Category(id = "1", name = "Food", isHidden = false),
            Category(id = "2", name = "Transport", isHidden = false)
        )
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)

        // When
        val result = useCase().first()

        // Then
        assertThat(result).isEqualTo(categories)
        verify { categoryRepository.observeVisibleCategories() }
    }

    @Test
    fun `observeAll with includeHidden false should return visible categories`() = runTest {
        // Given
        val categories = listOf(
            Category(id = "1", name = "Food", isHidden = false)
        )
        every { categoryRepository.observeVisibleCategories() } returns flowOf(categories)

        // When
        val result = useCase.observeAll(includeHidden = false).first()

        // Then
        assertThat(result).isEqualTo(categories)
    }

    @Test
    fun `observeAll with includeHidden true should return all categories`() = runTest {
        // Given
        val categories = listOf(
            Category(id = "1", name = "Food", isHidden = false),
            Category(id = "2", name = "Old Category", isHidden = true)
        )
        every { categoryRepository.observeAllCategories() } returns flowOf(categories)

        // When
        val result = useCase.observeAll(includeHidden = true).first()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(categories)
    }

    @Test
    fun `observeByGroup should return categories filtered by group`() = runTest {
        // Given
        val groupId = "food-group"
        val categories = listOf(
            Category(id = "1", name = "Groceries", groupId = groupId, isHidden = false),
            Category(id = "2", name = "Dining", groupId = groupId, isHidden = false)
        )
        every { categoryRepository.observeCategoriesByGroup(groupId) } returns flowOf(categories)

        // When
        val result = useCase.observeByGroup(groupId).first()

        // Then
        assertThat(result).isEqualTo(categories)
        verify { categoryRepository.observeCategoriesByGroup(groupId) }
    }
}
