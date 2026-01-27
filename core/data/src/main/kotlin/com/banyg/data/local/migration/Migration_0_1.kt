package com.banyg.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 0 (empty) to version 1 (initial schema)
 *
 * Creates all tables with proper indices and foreign keys.
 * CRITICAL: All money columns are INTEGER (Long), never REAL/FLOAT.
 */
val MIGRATION_0_1 = object : Migration(0, 1) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create accounts table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `accounts` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `currency_code` TEXT NOT NULL,
                `opening_balance_minor` INTEGER NOT NULL,
                `current_balance_minor` INTEGER NOT NULL,
                `is_archived` INTEGER NOT NULL DEFAULT 0,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create indices for accounts
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_is_archived` ON `accounts` (`is_archived`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_currency_code` ON `accounts` (`currency_code`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_type` ON `accounts` (`type`)")

        // Create categories table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `group_id` TEXT,
                `group_name` TEXT,
                `is_hidden` INTEGER NOT NULL DEFAULT 0,
                `icon` TEXT,
                `color` TEXT
            )
            """.trimIndent()
        )

        // Create indices for categories
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_group_id` ON `categories` (`group_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_is_hidden` ON `categories` (`is_hidden`)")

        // Create transactions table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `account_id` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                `amount_minor` INTEGER NOT NULL,
                `currency_code` TEXT NOT NULL,
                `merchant` TEXT NOT NULL,
                `memo` TEXT,
                `category_id` TEXT,
                `status` TEXT NOT NULL DEFAULT 'PENDING',
                `cleared_at` INTEGER,
                `transfer_id` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        // Create indices for transactions
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_account_id` ON `transactions` (`account_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_status` ON `transactions` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transfer_id` ON `transactions` (`transfer_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_account_id_date` ON `transactions` (`account_id`, `date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_account_id_status` ON `transactions` (`account_id`, `status`)")

        // Create splits table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `splits` (
                `transaction_id` TEXT NOT NULL,
                `line_id` INTEGER NOT NULL,
                `category_id` TEXT NOT NULL,
                `amount_minor` INTEGER NOT NULL,
                `currency_code` TEXT NOT NULL,
                `memo` TEXT,
                PRIMARY KEY(`transaction_id`, `line_id`),
                FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Create indices for splits
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_splits_transaction_id` ON `splits` (`transaction_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_splits_category_id` ON `splits` (`category_id`)")
    }
}
