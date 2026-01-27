package com.banyg.data.repository

import com.banyg.data.local.dao.CategoryDao
import com.banyg.data.mapper.CategoryMapper
import com.banyg.domain.model.Category
import com.banyg.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Category Repository Implementation
 *
 * Implements CategoryRepository using Room DAO and mapper.
 * Converts between domain models and Room entities.
 */
class CategoryRepositoryImpl(
    private val dao: CategoryDao,
    private val mapper: CategoryMapper = CategoryMapper()
) : CategoryRepository {

    override fun observeVisibleCategories(): Flow<List<Category>> {
        return dao.observeVisibleCategories()
            .map { mapper.toDomainList(it) }
    }

    override fun observeAllCategories(): Flow<List<Category>> {
        return dao.observeAllCategories()
            .map { mapper.toDomainList(it) }
    }

    override fun observeCategoriesByGroup(groupId: String): Flow<List<Category>> {
        return dao.observeCategoriesByGroup(groupId)
            .map { mapper.toDomainList(it) }
    }

    override fun observeCategory(id: String): Flow<Category?> {
        return dao.observeCategory(id)
            .map { it?.let { mapper.toDomain(it) } }
    }

    override suspend fun getCategory(id: String): Category? {
        return dao.getCategory(id)?.let { mapper.toDomain(it) }
    }

    override suspend fun getAllCategories(): List<Category> {
        return mapper.toDomainList(dao.getAllCategories())
    }

    override fun searchCategories(query: String): Flow<List<Category>> {
        return dao.searchCategories(query)
            .map { mapper.toDomainList(it) }
    }

    override suspend fun saveCategory(category: Category) {
        dao.insert(mapper.toEntity(category))
    }

    override suspend fun saveCategories(categories: List<Category>) {
        dao.insertAll(mapper.toEntityList(categories))
    }

    override suspend fun deleteCategory(id: String) {
        dao.deleteById(id)
    }

    override suspend fun setHidden(id: String, hidden: Boolean) {
        dao.setHidden(id, hidden)
    }

    override suspend fun getVisibleCategoryCount(): Int {
        return dao.getVisibleCategoryCount()
    }
}
