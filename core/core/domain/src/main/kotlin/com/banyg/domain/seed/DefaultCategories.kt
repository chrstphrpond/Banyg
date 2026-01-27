package com.banyg.domain.seed

/**
 * Default categories seed data
 *
 * Predefined categories for first-time app setup.
 * Users can modify or hide these categories after creation.
 */
object DefaultCategories {

    /**
     * Represents a default category definition
     *
     * @property id Unique identifier for the category
     * @property name Display name
     * @property groupId Optional parent group ID (categories with null groupId are groups themselves)
     * @property icon Optional icon identifier
     * @property color Optional color hex code
     */
    data class DefaultCategory(
        val id: String,
        val name: String,
        val groupId: String? = null,
        val icon: String? = null,
        val color: String? = null
    )

    /**
     * All default categories including groups and child categories.
     * Groups (categories with groupId = null that serve as parents) are listed first,
     * followed by their child categories.
     */
    val CATEGORIES = listOf(
        // ========== INCOME ==========
        DefaultCategory(
            id = "group_income",
            name = "Income",
            icon = "ic_income",
            color = "#4CAF50"
        ),
        DefaultCategory(
            id = "cat_salary",
            name = "Salary",
            groupId = "group_income",
            icon = "ic_work"
        ),
        DefaultCategory(
            id = "cat_income_other",
            name = "Other Income",
            groupId = "group_income",
            icon = "ic_add_circle"
        ),

        // ========== HOUSING ==========
        DefaultCategory(
            id = "group_housing",
            name = "Housing",
            icon = "ic_home",
            color = "#2196F3"
        ),
        DefaultCategory(
            id = "cat_rent",
            name = "Rent/Mortgage",
            groupId = "group_housing",
            icon = "ic_house"
        ),
        DefaultCategory(
            id = "cat_utilities",
            name = "Utilities",
            groupId = "group_housing",
            icon = "ic_bolt"
        ),

        // ========== FOOD ==========
        DefaultCategory(
            id = "group_food",
            name = "Food & Dining",
            icon = "ic_restaurant",
            color = "#FF9800"
        ),
        DefaultCategory(
            id = "cat_groceries",
            name = "Groceries",
            groupId = "group_food",
            icon = "ic_shopping_cart"
        ),
        DefaultCategory(
            id = "cat_restaurants",
            name = "Restaurants",
            groupId = "group_food",
            icon = "ic_restaurant_menu"
        ),
        DefaultCategory(
            id = "cat_fastfood",
            name = "Fast Food",
            groupId = "group_food",
            icon = "ic_fastfood"
        ),

        // ========== TRANSPORTATION ==========
        DefaultCategory(
            id = "group_transport",
            name = "Transportation",
            icon = "ic_directions_car",
            color = "#9C27B0"
        ),
        DefaultCategory(
            id = "cat_fuel",
            name = "Fuel",
            groupId = "group_transport",
            icon = "ic_local_gas_station"
        ),
        DefaultCategory(
            id = "cat_public_transit",
            name = "Public Transit",
            groupId = "group_transport",
            icon = "ic_train"
        ),
        DefaultCategory(
            id = "cat_rideshare",
            name = "Rideshare",
            groupId = "group_transport",
            icon = "ic_local_taxi"
        ),

        // ========== SHOPPING ==========
        DefaultCategory(
            id = "group_shopping",
            name = "Shopping",
            icon = "ic_shopping_bag",
            color = "#E91E63"
        ),
        DefaultCategory(
            id = "cat_shopping",
            name = "General Shopping",
            groupId = "group_shopping",
            icon = "ic_store"
        ),

        // ========== ENTERTAINMENT ==========
        DefaultCategory(
            id = "group_entertainment",
            name = "Entertainment",
            icon = "ic_movie",
            color = "#673AB7"
        ),
        DefaultCategory(
            id = "cat_entertainment",
            name = "Entertainment",
            groupId = "group_entertainment",
            icon = "ic_theaters"
        ),

        // ========== HEALTH ==========
        DefaultCategory(
            id = "group_health",
            name = "Health & Fitness",
            icon = "ic_fitness_center",
            color = "#F44336"
        ),
        DefaultCategory(
            id = "cat_health",
            name = "Health & Fitness",
            groupId = "group_health",
            icon = "ic_local_hospital"
        ),

        // ========== PERSONAL CARE ==========
        DefaultCategory(
            id = "group_personal",
            name = "Personal Care",
            icon = "ic_spa",
            color = "#00BCD4"
        ),
        DefaultCategory(
            id = "cat_personal",
            name = "Personal Care",
            groupId = "group_personal",
            icon = "ic_content_cut"
        ),

        // ========== EDUCATION ==========
        DefaultCategory(
            id = "group_education",
            name = "Education",
            icon = "ic_school",
            color = "#3F51B5"
        ),
        DefaultCategory(
            id = "cat_education",
            name = "Education",
            groupId = "group_education",
            icon = "ic_menu_book"
        ),

        // ========== GIFTS & DONATIONS ==========
        DefaultCategory(
            id = "group_gifts",
            name = "Gifts & Donations",
            icon = "ic_card_giftcard",
            color = "#FF5722"
        ),
        DefaultCategory(
            id = "cat_gifts",
            name = "Gifts & Donations",
            groupId = "group_gifts",
            icon = "ic_redeem"
        ),

        // ========== BUSINESS ==========
        DefaultCategory(
            id = "group_business",
            name = "Business",
            icon = "ic_business",
            color = "#607D8B"
        ),
        DefaultCategory(
            id = "cat_business",
            name = "Business Expenses",
            groupId = "group_business",
            icon = "ic_work_outline"
        ),

        // ========== TRAVEL ==========
        DefaultCategory(
            id = "group_travel",
            name = "Travel",
            icon = "ic_flight",
            color = "#009688"
        ),
        DefaultCategory(
            id = "cat_travel",
            name = "Travel",
            groupId = "group_travel",
            icon = "ic_luggage"
        ),

        // ========== FINANCIAL ==========
        DefaultCategory(
            id = "group_financial",
            name = "Financial",
            icon = "ic_account_balance",
            color = "#795548"
        ),
        DefaultCategory(
            id = "cat_transfer",
            name = "Transfer",
            groupId = "group_financial",
            icon = "ic_swap_horiz"
        ),
        DefaultCategory(
            id = "cat_fees",
            name = "Fees & Charges",
            groupId = "group_financial",
            icon = "ic_money_off"
        ),

        // ========== UNCATEGORIZED ==========
        DefaultCategory(
            id = "cat_uncategorized",
            name = "Uncategorized",
            icon = "ic_help_outline",
            color = "#9E9E9E"
        )
    )

    /**
     * Categories that are groups (parent categories)
     */
    val GROUPS: List<DefaultCategory>
        get() = CATEGORIES.filter { it.groupId == null }

    /**
     * Categories that have a parent group
     */
    val CHILD_CATEGORIES: List<DefaultCategory>
        get() = CATEGORIES.filter { it.groupId != null }
}
