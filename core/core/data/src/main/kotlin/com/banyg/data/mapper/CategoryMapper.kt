package com.banyg.data.mapper

import com.banyg.data.local.entity.CategoryEntity
import com.banyg.domain.model.Category

/**
 * Category Mapper
 *
 * Converts between CategoryEntity (Room) and Category (Domain).
 */
class CategoryMapper {

    /**
     * Convert Room entity to domain model
     */
    fun toDomain(entity: CategoryEntity): Category {
        return Category(
            id = entity.id,
            name = entity.name,
            groupId = entity.groupId,
            groupName = entity.groupName,
            isHidden = entity.isHidden,
            icon = entity.icon,
            color = entity.color
        )
    }

    /**
     * Convert domain model to Room entity
     */
    fun toEntity(domain: Category): CategoryEntity {
        return CategoryEntity(
            id = domain.id,
            name = domain.name,
            groupId = domain.groupId,
            groupName = domain.groupName,
            isHidden = domain.isHidden,
            icon = domain.icon,
            color = domain.color
        )
    }

    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<CategoryEntity>): List<Category> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(domains: List<Category>): List<CategoryEntity> {
        return domains.map { toEntity(it) }
    }
}
