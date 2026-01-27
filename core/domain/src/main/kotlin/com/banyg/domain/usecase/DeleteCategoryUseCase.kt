package com.banyg.domain.usecase

import com.banyg.domain.repository.CategoryRepository

/**
 * Delete category use case
 *
 * Performs soft delete (marks category as hidden) by default.
 * Hard delete can be requested but should be used with caution.
 */
class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    /**
     * Result of delete operation
     */
    sealed class Result {
        data object Success : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Delete/hide a category
     *
     * @param id Category ID
     * @param hardDelete If true, permanently deletes; if false, marks as hidden (soft delete)
     */
    suspend operator fun invoke(
        id: String,
        hardDelete: Boolean = false
    ): Result {
        // Verify category exists
        val existingCategory = categoryRepository.getCategory(id)
            ?: return Result.Error("Category not found")

        return try {
            if (hardDelete) {
                categoryRepository.deleteCategory(id)
            } else {
                // Soft delete - mark as hidden
                categoryRepository.setHidden(id, true)
            }
            Result.Success
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete category")
        }
    }

    /**
     * Restore a hidden category
     */
    suspend fun restore(id: String): Result {
        val existingCategory = categoryRepository.getCategory(id)
            ?: return Result.Error("Category not found")

        if (!existingCategory.isHidden) {
            return Result.Error("Category is not hidden")
        }

        return try {
            categoryRepository.setHidden(id, false)
            Result.Success
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to restore category")
        }
    }

    /**
     * Toggle hidden state of a category
     */
    suspend fun toggleHidden(id: String): Result {
        val existingCategory = categoryRepository.getCategory(id)
            ?: return Result.Error("Category not found")

        return try {
            categoryRepository.setHidden(id, !existingCategory.isHidden)
            Result.Success
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to toggle category visibility")
        }
    }
}
