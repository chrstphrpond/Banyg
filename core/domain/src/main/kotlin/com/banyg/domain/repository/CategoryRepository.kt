package com.banyg.domain.repository

import com.banyg.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Category Repository Interface
 *
 * Defines operations for category persistence.
 * Implementation in core/data layer.
 */
interface CategoryRepository {

    /**
     * Observe all visible categories
     */
    fun observeVisibleCategories(): Flow<List<Category>>

    /**
     * Observe all categories (including hidden)
     */
    fun observeAllCategories(): Flow<List<Category>>

    /**
     * Observe categories by group
     */
    fun observeCategoriesByGroup(groupId: String): Flow<List<Category>>

    /**
     * Observe single category by ID
     */
    fun observeCategory(id: String): Flow<Category?>

    /**
     * Get category by ID (one-shot)
     */
    suspend fun getCategory(id: String): Category?

    /**
     * Get all categories (one-shot)
     */
    suspend fun getAllCategories(): List<Category>

    /**
     * Search categories by name
     */
    fun searchCategories(query: String): Flow<List<Category>>

    /**
     * Save category (insert or update)
     */
    suspend fun saveCategory(category: Category)

    /**
     * Save multiple categories
     */
    suspend fun saveCategories(categories: List<Category>)

    /**
     * Delete category
     */
    suspend fun deleteCategory(id: String)

    /**
     * Hide/show category
     */
    suspend fun setHidden(id: String, hidden: Boolean)

    /**
     * Get count of visible categories
     */
    suspend fun getVisibleCategoryCount(): Int
}
