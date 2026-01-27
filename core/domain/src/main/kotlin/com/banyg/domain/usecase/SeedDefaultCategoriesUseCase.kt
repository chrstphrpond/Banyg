package com.banyg.domain.usecase

import com.banyg.domain.model.Category
import com.banyg.domain.model.CategoryGroups
import com.banyg.domain.repository.CategoryRepository

/**
 * Seed default categories use case
 *
 * Creates default categories if none exist.
 * Safe to call multiple times - skips existing categories.
 */
class SeedDefaultCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    /**
     * Result of seed operation
     */
    sealed class Result {
        data class Success(val created: Int, val skipped: Int) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Seed default categories if they don't exist
     */
    suspend operator fun invoke(): Result {
        return try {
            val existingCategories = categoryRepository.getAllCategories()
            val existingNames = existingCategories.map { it.name.lowercase() }.toSet()

            val defaultCategories = getDefaultCategories()
            val categoriesToCreate = defaultCategories.filter {
                it.name.lowercase() !in existingNames
            }

            if (categoriesToCreate.isNotEmpty()) {
                categoryRepository.saveCategories(categoriesToCreate)
            }

            Result.Success(
                created = categoriesToCreate.size,
                skipped = defaultCategories.size - categoriesToCreate.size
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to seed categories")
        }
    }

    /**
     * Get the list of default categories
     */
    fun getDefaultCategories(): List<Category> {
        return listOf(
            // Income
            Category(
                id = "cat_income_salary",
                name = "Salary",
                groupId = CategoryGroups.INCOME,
                groupName = CategoryGroups.INCOME,
                icon = "income",
                color = "#4CAF50"
            ),
            Category(
                id = "cat_income_freelance",
                name = "Freelance",
                groupId = CategoryGroups.INCOME,
                groupName = CategoryGroups.INCOME,
                icon = "income",
                color = "#4CAF50"
            ),
            Category(
                id = "cat_income_investment",
                name = "Investment",
                groupId = CategoryGroups.INCOME,
                groupName = CategoryGroups.INCOME,
                icon = "income",
                color = "#4CAF50"
            ),

            // Food & Dining
            Category(
                id = "cat_food_groceries",
                name = "Groceries",
                groupId = CategoryGroups.FOOD,
                groupName = CategoryGroups.FOOD,
                icon = "restaurant",
                color = "#FF9800"
            ),
            Category(
                id = "cat_food_dining",
                name = "Dining Out",
                groupId = CategoryGroups.FOOD,
                groupName = CategoryGroups.FOOD,
                icon = "restaurant",
                color = "#FF9800"
            ),
            Category(
                id = "cat_food_snacks",
                name = "Snacks",
                groupId = CategoryGroups.FOOD,
                groupName = CategoryGroups.FOOD,
                icon = "restaurant",
                color = "#FF9800"
            ),

            // Transportation
            Category(
                id = "cat_transport_fuel",
                name = "Fuel",
                groupId = CategoryGroups.TRANSPORTATION,
                groupName = CategoryGroups.TRANSPORTATION,
                icon = "transport",
                color = "#2196F3"
            ),
            Category(
                id = "cat_transport_public",
                name = "Public Transit",
                groupId = CategoryGroups.TRANSPORTATION,
                groupName = CategoryGroups.TRANSPORTATION,
                icon = "transport",
                color = "#2196F3"
            ),
            Category(
                id = "cat_transport_parking",
                name = "Parking",
                groupId = CategoryGroups.TRANSPORTATION,
                groupName = CategoryGroups.TRANSPORTATION,
                icon = "transport",
                color = "#2196F3"
            ),

            // Shopping
            Category(
                id = "cat_shop_clothing",
                name = "Clothing",
                groupId = CategoryGroups.SHOPPING,
                groupName = CategoryGroups.SHOPPING,
                icon = "shopping",
                color = "#9C27B0"
            ),
            Category(
                id = "cat_shop_electronics",
                name = "Electronics",
                groupId = CategoryGroups.SHOPPING,
                groupName = CategoryGroups.SHOPPING,
                icon = "shopping",
                color = "#9C27B0"
            ),
            Category(
                id = "cat_shop_personal",
                name = "Personal Care",
                groupId = CategoryGroups.SHOPPING,
                groupName = CategoryGroups.SHOPPING,
                icon = "shopping",
                color = "#9C27B0"
            ),

            // Bills & Utilities
            Category(
                id = "cat_bills_electric",
                name = "Electricity",
                groupId = CategoryGroups.BILLS,
                groupName = CategoryGroups.BILLS,
                icon = "receipt",
                color = "#F44336"
            ),
            Category(
                id = "cat_bills_water",
                name = "Water",
                groupId = CategoryGroups.BILLS,
                groupName = CategoryGroups.BILLS,
                icon = "receipt",
                color = "#F44336"
            ),
            Category(
                id = "cat_bills_internet",
                name = "Internet",
                groupId = CategoryGroups.BILLS,
                groupName = CategoryGroups.BILLS,
                icon = "receipt",
                color = "#F44336"
            ),
            Category(
                id = "cat_bills_mobile",
                name = "Mobile",
                groupId = CategoryGroups.BILLS,
                groupName = CategoryGroups.BILLS,
                icon = "receipt",
                color = "#F44336"
            ),

            // Entertainment
            Category(
                id = "cat_ent_movies",
                name = "Movies & Shows",
                groupId = CategoryGroups.ENTERTAINMENT,
                groupName = CategoryGroups.ENTERTAINMENT,
                icon = "entertainment",
                color = "#E91E63"
            ),
            Category(
                id = "cat_ent_games",
                name = "Games",
                groupId = CategoryGroups.ENTERTAINMENT,
                groupName = CategoryGroups.ENTERTAINMENT,
                icon = "entertainment",
                color = "#E91E63"
            ),
            Category(
                id = "cat_ent_hobbies",
                name = "Hobbies",
                groupId = CategoryGroups.ENTERTAINMENT,
                groupName = CategoryGroups.ENTERTAINMENT,
                icon = "entertainment",
                color = "#E91E63"
            ),

            // Health & Fitness
            Category(
                id = "cat_health_medical",
                name = "Medical",
                groupId = CategoryGroups.HEALTH,
                groupName = CategoryGroups.HEALTH,
                icon = "health",
                color = "#00BCD4"
            ),
            Category(
                id = "cat_health_fitness",
                name = "Fitness",
                groupId = CategoryGroups.HEALTH,
                groupName = CategoryGroups.HEALTH,
                icon = "health",
                color = "#00BCD4"
            ),
            Category(
                id = "cat_health_pharmacy",
                name = "Pharmacy",
                groupId = CategoryGroups.HEALTH,
                groupName = CategoryGroups.HEALTH,
                icon = "health",
                color = "#00BCD4"
            ),

            // Travel
            Category(
                id = "cat_travel_transport",
                name = "Transport",
                groupId = "Travel",
                groupName = "Travel",
                icon = "flight",
                color = "#3F51B5"
            ),
            Category(
                id = "cat_travel_lodging",
                name = "Lodging",
                groupId = "Travel",
                groupName = "Travel",
                icon = "flight",
                color = "#3F51B5"
            ),
            Category(
                id = "cat_travel_food",
                name = "Travel Food",
                groupId = "Travel",
                groupName = "Travel",
                icon = "flight",
                color = "#3F51B5"
            ),

            // Transfer
            Category(
                id = "cat_transfer",
                name = "Transfer",
                groupId = CategoryGroups.TRANSFER,
                groupName = CategoryGroups.TRANSFER,
                icon = "swap",
                color = "#607D8B"
            ),

            // Other
            Category(
                id = "cat_other_misc",
                name = "Miscellaneous",
                groupId = CategoryGroups.OTHER,
                groupName = CategoryGroups.OTHER,
                icon = "category",
                color = "#9E9E9E"
            )
        )
    }
}
