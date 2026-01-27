package com.banyg.domain.usecase

import com.banyg.domain.model.Category
import com.banyg.domain.repository.CategoryRepository

/**
 * Update category use case
 *
 * Updates an existing category with validation.
 */
class UpdateCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    /**
     * Result of category update
     */
    sealed class Result {
        data class Success(val category: Category) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Update an existing category
     *
     * @param id Category ID (required)
     * @param name New name (optional, keeps current if null)
     * @param groupId New group ID (optional, keeps current if null)
     * @param groupName New group name (optional, keeps current if null)
     * @param icon New icon (optional, keeps current if null)
     * @param color New color (optional, keeps current if null)
     * @param isHidden New hidden state (optional, keeps current if null)
     */
    suspend operator fun invoke(
        id: String,
        name: String? = null,
        groupId: String? = null,
        groupName: String? = null,
        icon: String? = null,
        color: String? = null,
        isHidden: Boolean? = null
    ): Result {
        // Get existing category
        val existingCategory = categoryRepository.getCategory(id)
            ?: return Result.Error("Category not found")

        // Validate new name if provided
        val trimmedName = name?.trim()
        if (trimmedName != null) {
            if (trimmedName.isBlank()) {
                return Result.Error("Category name cannot be blank")
            }

            if (trimmedName.length > 50) {
                return Result.Error("Category name must be 50 characters or less")
            }

            // Check for duplicate names (excluding self)
            val newGroupId = groupId ?: existingCategory.groupId
            val existingCategories = categoryRepository.getAllCategories()
            val isDuplicate = existingCategories.any {
                it.id != id &&
                it.name.equals(trimmedName, ignoreCase = true) &&
                it.groupId == newGroupId
            }

            if (isDuplicate) {
                val displayGroup = groupName ?: existingCategory.groupName
                return Result.Error(
                    if (displayGroup != null) {
                        "A category named '$trimmedName' already exists in $displayGroup"
                    } else {
                        "A category named '$trimmedName' already exists"
                    }
                )
            }
        }

        val updatedCategory = existingCategory.copy(
            name = trimmedName ?: existingCategory.name,
            groupId = groupId ?: existingCategory.groupId,
            groupName = groupName ?: existingCategory.groupName,
            icon = icon ?: existingCategory.icon,
            color = color ?: existingCategory.color,
            isHidden = isHidden ?: existingCategory.isHidden
        )

        return try {
            categoryRepository.saveCategory(updatedCategory)
            Result.Success(updatedCategory)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update category")
        }
    }
}
