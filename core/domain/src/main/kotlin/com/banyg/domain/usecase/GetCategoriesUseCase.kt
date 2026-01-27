package com.banyg.domain.usecase

import com.banyg.domain.model.Category
import com.banyg.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Get all categories use case
 *
 * Provides access to categories with optional filtering.
 */
class GetCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    /**
     * Observe all visible categories (not hidden)
     * Returns Flow for reactive updates
     */
    operator fun invoke(): Flow<List<Category>> {
        return categoryRepository.observeVisibleCategories()
    }

    /**
     * Observe all categories including hidden
     */
    fun observeAll(includeHidden: Boolean = false): Flow<List<Category>> {
        return if (includeHidden) {
            categoryRepository.observeAllCategories()
        } else {
            categoryRepository.observeVisibleCategories()
        }
    }

    /**
     * Observe categories by group
     */
    fun observeByGroup(groupId: String): Flow<List<Category>> {
        return categoryRepository.observeCategoriesByGroup(groupId)
    }

    /**
     * Get single category by ID (one-shot)
     */
    suspend fun getById(id: String): Category? {
        return categoryRepository.getCategory(id)
    }

    /**
     * Observe single category by ID
     */
    fun observeById(id: String): Flow<Category?> {
        return categoryRepository.observeCategory(id)
    }
}
