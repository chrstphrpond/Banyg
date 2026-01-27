package com.banyg.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 1 to version 2
 *
 * Adds budgets table for envelope-style budgeting.
 * Each budget represents an allocation for a category in a specific period (month).
 *
 * CRITICAL: All money columns are INTEGER (Long), never REAL/FLOAT.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create budgets table with composite primary key (category_id, period)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budgets` (
                `category_id` TEXT NOT NULL,
                `period` TEXT NOT NULL,
                `amount_minor` INTEGER NOT NULL,
                `currency_code` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`category_id`, `period`),
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Create indices for budgets table
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_period` ON `budgets` (`period`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_category_id` ON `budgets` (`category_id`)")
    }
}
