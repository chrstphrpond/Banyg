package com.banyg.domain.usecase

import com.banyg.domain.model.Category
import com.banyg.domain.model.CategoryGroups
import com.banyg.domain.repository.CategoryRepository
import java.util.UUID

/**
 * Create category use case
 *
 * Creates a new category with validation.
 */
class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    /**
     * Result of category creation
     */
    sealed class Result {
        data class Success(val category: Category) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Create a new category
     *
     * @param name Category name (required, non-blank)
     * @param groupId Optional group ID for organizing categories
     * @param groupName Optional group name for display
     * @param icon Optional icon identifier
     * @param color Optional color hex code
     * @param id Optional ID (generated if not provided)
     */
    suspend operator fun invoke(
        name: String,
        groupId: String? = null,
        groupName: String? = null,
        icon: String? = null,
        color: String? = null,
        id: String = UUID.randomUUID().toString()
    ): Result {
        val trimmedName = name.trim()

        // Validation
        if (trimmedName.isBlank()) {
            return Result.Error("Category name cannot be blank")
        }

        if (trimmedName.length > 50) {
            return Result.Error("Category name must be 50 characters or less")
        }

        // Check for duplicate names in the same group
        val existingCategories = categoryRepository.getAllCategories()
        val isDuplicate = existingCategories.any {
            it.name.equals(trimmedName, ignoreCase = true) &&
            it.groupId == groupId
        }

        if (isDuplicate) {
            return Result.Error(
                if (groupName != null) {
                    "A category named '$trimmedName' already exists in $groupName"
                } else {
                    "A category named '$trimmedName' already exists"
                }
            )
        }

        val category = Category(
            id = id,
            name = trimmedName,
            groupId = groupId,
            groupName = groupName ?: groupId?.let { getDefaultGroupName(it) },
            isHidden = false,
            icon = icon,
            color = color
        )

        return try {
            categoryRepository.saveCategory(category)
            Result.Success(category)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create category")
        }
    }

    private fun getDefaultGroupName(groupId: String): String {
        return when (groupId) {
            CategoryGroups.INCOME -> "Income"
            CategoryGroups.FOOD -> "Food"
            CategoryGroups.TRANSPORTATION -> "Transportation"
            CategoryGroups.SHOPPING -> "Shopping"
            CategoryGroups.BILLS -> "Bills & Utilities"
            CategoryGroups.ENTERTAINMENT -> "Entertainment"
            CategoryGroups.HEALTH -> "Health"
            CategoryGroups.EDUCATION -> "Education"
            CategoryGroups.TRANSFER -> "Transfer"
            CategoryGroups.OTHER -> "Other"
            else -> groupId.replaceFirstChar { it.uppercase() }
        }
    }
}
