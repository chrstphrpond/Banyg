package com.banyg.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.banyg.data.local.entity.CategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Room Database Callback for seeding initial data.
 *
 * This callback is invoked on database creation to seed default categories.
 * Uses a coroutine scope for async database operations.
 *
 * Note: This approach seeds directly via DAO to avoid circular dependencies.
 * For more complex seeding logic, use the application-level seeder instead.
 */
class BanygDatabaseCallback : RoomDatabase.Callback() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Database-level seeding can be done here via raw SQL if needed
        // For category seeding, we use the application-level seeder
        // which provides more flexibility and access to repositories
    }

    companion object {
        /**
         * Raw SQL seed data for categories.
         * Can be used for database-level seeding if needed.
         */
        val CATEGORY_SEED_SQL = listOf(
            // Income Group
            "INSERT OR IGNORE INTO categories (id, name, is_hidden) VALUES ('group_income', 'Income', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_salary', 'Salary', 'group_income', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_income_other', 'Other Income', 'group_income', 0)",

            // Housing Group
            "INSERT OR IGNORE INTO categories (id, name, is_hidden) VALUES ('group_housing', 'Housing', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_rent', 'Rent/Mortgage', 'group_housing', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_utilities', 'Utilities', 'group_housing', 0)",

            // Food Group
            "INSERT OR IGNORE INTO categories (id, name, is_hidden) VALUES ('group_food', 'Food & Dining', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_groceries', 'Groceries', 'group_food', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_restaurants', 'Restaurants', 'group_food', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_fastfood', 'Fast Food', 'group_food', 0)",

            // Transportation Group
            "INSERT OR IGNORE INTO categories (id, name, is_hidden) VALUES ('group_transport', 'Transportation', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_fuel', 'Fuel', 'group_transport', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_public_transit', 'Public Transit', 'group_transport', 0)",
            "INSERT OR IGNORE INTO categories (id, name, group_id, is_hidden) VALUES ('cat_rideshare', 'Rideshare', 'group_transport', 0)"
        )
    }
}
