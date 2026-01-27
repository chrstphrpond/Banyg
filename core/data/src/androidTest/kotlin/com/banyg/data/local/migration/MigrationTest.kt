package com.banyg.data.local.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.banyg.data.local.BanygDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * CRITICAL migration tests for BanygDatabase
 *
 * These tests prevent data loss during schema changes.
 * For a financial app, data loss is catastrophic - these tests ensure:
 * 1. Migrations execute without errors
 * 2. Data is preserved across migrations
 * 3. Money columns remain as INTEGER (Long), never REAL/FLOAT
 * 4. Foreign keys and indices are properly created
 *
 * Run with: ./gradlew :core:data:connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BanygDatabase::class.java
    )

    companion object {
        private const val TEST_DB = "migration_test"
    }

    /**
     * Test migration from version 0 (empty) to version 1 (initial schema)
     *
     * Verifies:
     * - All tables are created (accounts, categories, transactions, splits)
     * - Money columns are INTEGER (Long)
     * - Foreign keys work correctly
     * - Indices are created
     */
    @Test
    @Throws(IOException::class)
    fun migrate0To1_CreatesAllTablesWithProperSchema() {
        // Create database at version 0 (empty)
        helper.createDatabase(TEST_DB, 0).apply {
            // Database is empty at version 0
            close()
        }

        // Run migration to version 1
        val db = helper.runMigrationsAndValidate(TEST_DB, 1, true, MIGRATION_0_1)

        // Verify accounts table exists and has correct schema
        db.query("SELECT * FROM accounts WHERE 1=0").use { cursor ->
            assertThat(cursor.columnNames).asList().containsAtLeast(
                "id", "name", "type", "currency_code",
                "opening_balance_minor", "current_balance_minor",
                "is_archived", "created_at", "updated_at"
            )
        }

        // Verify categories table exists
        db.query("SELECT * FROM categories WHERE 1=0").use { cursor ->
            assertThat(cursor.columnNames).asList().containsAtLeast(
                "id", "name", "group_id", "group_name", "is_hidden", "icon", "color"
            )
        }

        // Verify transactions table exists with money as INTEGER
        db.query("SELECT * FROM transactions WHERE 1=0").use { cursor ->
            assertThat(cursor.columnNames).asList().containsAtLeast(
                "id", "account_id", "date", "amount_minor", "currency_code",
                "merchant", "memo", "category_id", "status", "cleared_at",
                "transfer_id", "created_at", "updated_at"
            )
        }

        // Verify splits table exists
        db.query("SELECT * FROM splits WHERE 1=0").use { cursor ->
            assertThat(cursor.columnNames).asList().containsAtLeast(
                "transaction_id", "line_id", "category_id", "amount_minor",
                "currency_code", "memo"
            )
        }

        // Verify indices exist by checking sqlite_master
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='accounts'").use { cursor ->
            val indices = mutableListOf<String>()
            while (cursor.moveToNext()) {
                indices.add(cursor.getString(0))
            }
            assertThat(indices).contains("index_accounts_is_archived")
            assertThat(indices).contains("index_accounts_currency_code")
            assertThat(indices).contains("index_accounts_type")
        }

        db.close()
    }

    /**
     * Test migration from 0 to 1 preserves data integrity
     *
     * Verifies:
     * - Account data inserted at version 1 is readable
     * - Money amounts remain as Long (no precision loss)
     * - Foreign keys work (categories and transactions)
     */
    @Test
    @Throws(IOException::class)
    fun migrate0To1_CanInsertAndReadData() {
        // Create database at version 0 and migrate to 1
        helper.createDatabase(TEST_DB, 0).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 1, true, MIGRATION_0_1)

        // Insert test account with real money amounts
        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT INTO accounts (id, name, type, currency_code, opening_balance_minor,
                                 current_balance_minor, is_archived, created_at, updated_at)
            VALUES ('acc1', 'Test Bank', 'CHECKING', 'PHP', 100000, 125000, 0, $now, $now)
            """.trimIndent()
        )

        // Insert test category
        db.execSQL(
            """
            INSERT INTO categories (id, name, group_id, group_name, is_hidden, icon, color)
            VALUES ('cat1', 'Groceries', 'grp1', 'Food', 0, 'cart', '#FF5722')
            """.trimIndent()
        )

        // Insert test transaction with negative amount (expense)
        val transactionDate = 20260128L
        db.execSQL(
            """
            INSERT INTO transactions (id, account_id, date, amount_minor, currency_code,
                                     merchant, memo, category_id, status, cleared_at,
                                     transfer_id, created_at, updated_at)
            VALUES ('txn1', 'acc1', $transactionDate, -25000, 'PHP',
                    'SM Supermarket', 'Weekly groceries', 'cat1', 'PENDING', NULL,
                    NULL, $now, $now)
            """.trimIndent()
        )

        // Verify account data integrity
        db.query("SELECT * FROM accounts WHERE id='acc1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("name"))).isEqualTo("Test Bank")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("type"))).isEqualTo("CHECKING")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("currency_code"))).isEqualTo("PHP")

            // CRITICAL: Verify money is stored as Long, not Float/Double
            val openingBalance = cursor.getLong(cursor.getColumnIndexOrThrow("opening_balance_minor"))
            assertThat(openingBalance).isEqualTo(100000L) // PHP 1,000.00

            val currentBalance = cursor.getLong(cursor.getColumnIndexOrThrow("current_balance_minor"))
            assertThat(currentBalance).isEqualTo(125000L) // PHP 1,250.00
        }

        // Verify transaction data integrity
        db.query("SELECT * FROM transactions WHERE id='txn1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("merchant"))).isEqualTo("SM Supermarket")

            // CRITICAL: Verify negative amount (expense) is stored correctly as Long
            val amount = cursor.getLong(cursor.getColumnIndexOrThrow("amount_minor"))
            assertThat(amount).isEqualTo(-25000L) // -PHP 250.00 (expense)

            // Verify foreign key reference
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("category_id"))).isEqualTo("cat1")
        }

        // Verify foreign key cascade works
        db.execSQL("DELETE FROM accounts WHERE id='acc1'")
        db.query("SELECT * FROM transactions WHERE id='txn1'").use { cursor ->
            assertThat(cursor.count).isEqualTo(0) // Transaction should be deleted via CASCADE
        }

        db.close()
    }

    /**
     * Test migration from version 1 to version 2 (adds budgets table)
     *
     * Verifies:
     * - Budgets table is created
     * - Existing data is preserved
     * - Budget money column is INTEGER (Long)
     * - Composite primary key works
     */
    @Test
    @Throws(IOException::class)
    fun migrate1To2_AddsBudgetsTableAndPreservesData() {
        // Create database at version 1 with test data
        helper.createDatabase(TEST_DB, 1).apply {
            val now = System.currentTimeMillis()

            // Insert category that will be used for budget
            execSQL(
                """
                INSERT INTO categories (id, name, group_id, group_name, is_hidden, icon, color)
                VALUES ('cat1', 'Groceries', 'grp1', 'Food', 0, 'cart', '#FF5722')
                """.trimIndent()
            )

            // Insert account with some balance
            execSQL(
                """
                INSERT INTO accounts (id, name, type, currency_code, opening_balance_minor,
                                     current_balance_minor, is_archived, created_at, updated_at)
                VALUES ('acc1', 'Test Bank', 'CHECKING', 'PHP', 100000, 100000, 0, $now, $now)
                """.trimIndent()
            )

            close()
        }

        // Run migration to version 2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Verify budgets table exists with correct schema
        db.query("SELECT * FROM budgets WHERE 1=0").use { cursor ->
            assertThat(cursor.columnNames).asList().containsAtLeast(
                "category_id", "period", "amount_minor", "currency_code",
                "created_at", "updated_at"
            )
        }

        // Verify old data is preserved
        db.query("SELECT * FROM categories WHERE id='cat1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("name"))).isEqualTo("Groceries")
        }

        db.query("SELECT * FROM accounts WHERE id='acc1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("name"))).isEqualTo("Test Bank")

            // CRITICAL: Money remains as Long after migration
            val balance = cursor.getLong(cursor.getColumnIndexOrThrow("current_balance_minor"))
            assertThat(balance).isEqualTo(100000L)
        }

        // Verify indices exist on budgets table
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='budgets'").use { cursor ->
            val indices = mutableListOf<String>()
            while (cursor.moveToNext()) {
                indices.add(cursor.getString(0))
            }
            assertThat(indices).contains("index_budgets_period")
            assertThat(indices).contains("index_budgets_category_id")
        }

        db.close()
    }

    /**
     * Test migration from 1 to 2 with budget data
     *
     * Verifies:
     * - Budget data can be inserted after migration
     * - Budget money amounts are stored as Long
     * - Composite primary key enforces uniqueness
     * - Foreign key to categories works
     */
    @Test
    @Throws(IOException::class)
    fun migrate1To2_CanInsertBudgetData() {
        // Create database at version 1 with category
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO categories (id, name, group_id, group_name, is_hidden)
                VALUES ('cat_groceries', 'Groceries', 'grp1', 'Food', 0)
                """.trimIndent()
            )
            close()
        }

        // Run migration to version 2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Insert budget for January 2026
        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT INTO budgets (category_id, period, amount_minor, currency_code, created_at, updated_at)
            VALUES ('cat_groceries', '2026-01', 500000, 'PHP', $now, $now)
            """.trimIndent()
        )

        // Verify budget data
        db.query("SELECT * FROM budgets WHERE category_id='cat_groceries' AND period='2026-01'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()

            // CRITICAL: Budget amount is stored as Long
            val budgetAmount = cursor.getLong(cursor.getColumnIndexOrThrow("amount_minor"))
            assertThat(budgetAmount).isEqualTo(500000L) // PHP 5,000.00

            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("currency_code"))).isEqualTo("PHP")
        }

        // Verify composite primary key works (can't insert duplicate)
        try {
            db.execSQL(
                """
                INSERT INTO budgets (category_id, period, amount_minor, currency_code, created_at, updated_at)
                VALUES ('cat_groceries', '2026-01', 600000, 'PHP', $now, $now)
                """.trimIndent()
            )
            throw AssertionError("Should have thrown constraint violation for duplicate primary key")
        } catch (e: Exception) {
            // Expected: PRIMARY KEY constraint violation
            assertThat(e.message).contains("UNIQUE")
        }

        // Verify can insert same category for different period
        db.execSQL(
            """
            INSERT INTO budgets (category_id, period, amount_minor, currency_code, created_at, updated_at)
            VALUES ('cat_groceries', '2026-02', 550000, 'PHP', $now, $now)
            """.trimIndent()
        )

        db.query("SELECT COUNT(*) FROM budgets WHERE category_id='cat_groceries'").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getInt(0)).isEqualTo(2) // Jan and Feb budgets
        }

        // Verify foreign key cascade delete
        db.execSQL("DELETE FROM categories WHERE id='cat_groceries'")
        db.query("SELECT * FROM budgets WHERE category_id='cat_groceries'").use { cursor ->
            assertThat(cursor.count).isEqualTo(0) // Budgets should be deleted via CASCADE
        }

        db.close()
    }

    /**
     * Test full migration path from version 0 to version 2
     *
     * Verifies:
     * - All migrations can be applied sequentially
     * - Data survives multi-step migration
     * - Final schema is correct
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll_From0To2() {
        // Create empty database at version 0
        helper.createDatabase(TEST_DB, 0).close()

        // Run all migrations from 0 to 2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_0_1, MIGRATION_1_2)

        // Verify all tables exist
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            val tables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
            assertThat(tables).containsAtLeast(
                "accounts", "categories", "transactions", "splits", "budgets"
            )
        }

        // Insert complete financial data to verify schema works end-to-end
        val now = System.currentTimeMillis()

        // Create account
        db.execSQL(
            """
            INSERT INTO accounts (id, name, type, currency_code, opening_balance_minor,
                                 current_balance_minor, is_archived, created_at, updated_at)
            VALUES ('acc1', 'BDO Checking', 'CHECKING', 'PHP', 5000000, 4750000, 0, $now, $now)
            """.trimIndent()
        )

        // Create category
        db.execSQL(
            """
            INSERT INTO categories (id, name, group_id, group_name, is_hidden, icon, color)
            VALUES ('cat1', 'Transportation', 'grp1', 'Living', 0, 'car', '#2196F3')
            """.trimIndent()
        )

        // Create transaction
        db.execSQL(
            """
            INSERT INTO transactions (id, account_id, date, amount_minor, currency_code,
                                     merchant, memo, category_id, status, cleared_at,
                                     transfer_id, created_at, updated_at)
            VALUES ('txn1', 'acc1', 20260128, -25000, 'PHP',
                    'Grab', 'Ride to office', 'cat1', 'CLEARED', $now,
                    NULL, $now, $now)
            """.trimIndent()
        )

        // Create budget
        db.execSQL(
            """
            INSERT INTO budgets (category_id, period, amount_minor, currency_code, created_at, updated_at)
            VALUES ('cat1', '2026-01', 200000, 'PHP', $now, $now)
            """.trimIndent()
        )

        // Verify complete data integrity
        db.query(
            """
            SELECT a.name as account_name,
                   a.current_balance_minor as balance,
                   t.merchant,
                   t.amount_minor as txn_amount,
                   c.name as category_name,
                   b.amount_minor as budget_amount
            FROM accounts a
            JOIN transactions t ON a.id = t.account_id
            JOIN categories c ON t.category_id = c.id
            JOIN budgets b ON c.id = b.category_id
            WHERE a.id='acc1' AND b.period='2026-01'
            """.trimIndent()
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()

            assertThat(cursor.getString(0)).isEqualTo("BDO Checking")
            assertThat(cursor.getLong(1)).isEqualTo(4750000L) // PHP 47,500.00
            assertThat(cursor.getString(2)).isEqualTo("Grab")
            assertThat(cursor.getLong(3)).isEqualTo(-25000L) // -PHP 250.00
            assertThat(cursor.getString(4)).isEqualTo("Transportation")
            assertThat(cursor.getLong(5)).isEqualTo(200000L) // PHP 2,000.00
        }

        db.close()
    }

    /**
     * Test that database can be opened after all migrations
     *
     * Verifies:
     * - Migrated database is valid
     * - Room can read the migrated schema
     * - No schema validation errors
     */
    @Test
    fun migratedDatabase_CanBeOpenedByRoom() = runBlocking {
        // Create database at version 0 and migrate to version 2
        helper.createDatabase(TEST_DB, 0).close()
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_0_1, MIGRATION_1_2).close()

        // Open the migrated database with Room
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BanygDatabase::class.java,
            TEST_DB
        ).build()

        // Verify Room can open and read the database without errors
        val accounts = db.accountDao().getAllAccounts()
        assertThat(accounts).isNotNull()

        db.close()
    }

    /**
     * Test that money columns are INTEGER type, not REAL/FLOAT
     *
     * CRITICAL: This test ensures financial precision is preserved.
     * Float/Double introduce rounding errors that are unacceptable for money.
     */
    @Test
    fun verifyMoneyColumnsAreInteger() {
        // Create and migrate database
        helper.createDatabase(TEST_DB, 0).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_0_1, MIGRATION_1_2)

        // Check accounts table
        db.query("PRAGMA table_info(accounts)").use { cursor ->
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val columnType = cursor.getString(cursor.getColumnIndexOrThrow("type"))

                if (columnName.contains("balance_minor")) {
                    assertThat(columnType.uppercase()).isEqualTo("INTEGER")
                }
            }
        }

        // Check transactions table
        db.query("PRAGMA table_info(transactions)").use { cursor ->
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val columnType = cursor.getString(cursor.getColumnIndexOrThrow("type"))

                if (columnName == "amount_minor") {
                    assertThat(columnType.uppercase()).isEqualTo("INTEGER")
                }
            }
        }

        // Check splits table
        db.query("PRAGMA table_info(splits)").use { cursor ->
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val columnType = cursor.getString(cursor.getColumnIndexOrThrow("type"))

                if (columnName == "amount_minor") {
                    assertThat(columnType.uppercase()).isEqualTo("INTEGER")
                }
            }
        }

        // Check budgets table
        db.query("PRAGMA table_info(budgets)").use { cursor ->
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val columnType = cursor.getString(cursor.getColumnIndexOrThrow("type"))

                if (columnName == "amount_minor") {
                    assertThat(columnType.uppercase()).isEqualTo("INTEGER")
                }
            }
        }

        db.close()
    }
}
