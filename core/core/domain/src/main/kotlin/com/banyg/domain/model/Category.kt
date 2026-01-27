package com.banyg.domain.model

/**
 * Category domain model
 *
 * Represents a transaction category for budgeting and reporting.
 * Categories are organized into groups (e.g., "Food" group with "Groceries", "Dining" categories)
 *
 * @property id Unique identifier
 * @property name Category name (e.g., "Groceries", "Transportation")
 * @property groupId Optional group ID for organizing categories
 * @property groupName Optional group name for display
 * @property isHidden True if category should be hidden from selection
 * @property icon Optional icon identifier
 * @property color Optional color hex code
 */
data class Category(
    val id: String,
    val name: String,
    val groupId: String? = null,
    val groupName: String? = null,
    val isHidden: Boolean = false,
    val icon: String? = null,
    val color: String? = null
) {
    init {
        require(name.isNotBlank()) { "Category name cannot be blank" }
    }

    /**
     * Display name including group if available
     * Example: "Food: Groceries" or just "Groceries"
     */
    val displayName: String
        get() = if (groupName != null) "$groupName: $name" else name
}

/**
 * Common category groups
 */
object CategoryGroups {
    const val INCOME = "Income"
    const val FOOD = "Food"
    const val TRANSPORTATION = "Transportation"
    const val SHOPPING = "Shopping"
    const val BILLS = "Bills & Utilities"
    const val ENTERTAINMENT = "Entertainment"
    const val HEALTH = "Health"
    const val EDUCATION = "Education"
    const val TRANSFER = "Transfer"
    const val OTHER = "Other"
}
