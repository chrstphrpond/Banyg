package com.banyg.data.local.dao

import androidx.room.*
import com.banyg.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Category DAO
 *
 * Data access object for categories table.
 * Uses Flow for reactive queries, suspend for one-shot operations.
 */
@Dao
interface CategoryDao {

    /**
     * Observe all visible categories
     */
    @Query("SELECT * FROM categories WHERE is_hidden = 0 ORDER BY group_name ASC, name ASC")
    fun observeVisibleCategories(): Flow<List<CategoryEntity>>

    /**
     * Observe all categories (including hidden)
     */
    @Query("SELECT * FROM categories ORDER BY group_name ASC, name ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    /**
     * Observe categories by group
     */
    @Query("SELECT * FROM categories WHERE group_id = :groupId AND is_hidden = 0 ORDER BY name ASC")
    fun observeCategoriesByGroup(groupId: String): Flow<List<CategoryEntity>>

    /**
     * Observe single category by ID
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    fun observeCategory(id: String): Flow<CategoryEntity?>

    /**
     * Get category by ID (one-shot)
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: String): CategoryEntity?

    /**
     * Get all categories (one-shot)
     */
    @Query("SELECT * FROM categories ORDER BY group_name ASC, name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    /**
     * Search categories by name
     */
    @Query("SELECT * FROM categories WHERE name LIKE '%' || :query || '%' AND is_hidden = 0 ORDER BY name ASC")
    fun searchCategories(query: String): Flow<List<CategoryEntity>>

    /**
     * Insert category
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    /**
     * Insert multiple categories
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    /**
     * Update category
     */
    @Update
    suspend fun update(category: CategoryEntity)

    /**
     * Delete category
     */
    @Delete
    suspend fun delete(category: CategoryEntity)

    /**
     * Delete category by ID
     */
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Hide/show category
     */
    @Query("UPDATE categories SET is_hidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean)

    /**
     * Get count of categories
     */
    @Query("SELECT COUNT(*) FROM categories WHERE is_hidden = 0")
    suspend fun getVisibleCategoryCount(): Int
}
