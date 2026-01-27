package com.banyg.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.banyg.data.local.dao.AccountDao
import com.banyg.data.local.dao.BudgetDao
import com.banyg.data.local.dao.CategoryDao
import com.banyg.data.local.dao.SplitDao
import com.banyg.data.local.dao.TransactionDao
import com.banyg.data.local.entity.AccountEntity
import com.banyg.data.local.entity.BudgetEntity
import com.banyg.data.local.entity.CategoryEntity
import com.banyg.data.local.entity.SplitEntity
import com.banyg.data.local.entity.TransactionEntity
import com.banyg.data.local.migration.MIGRATION_0_1
import com.banyg.data.local.migration.MIGRATION_1_2
import com.banyg.data.local.migration.MigrationBackupCallback

/**
 * Banyg Room Database
 *
 * Local-first finance database with offline support.
 * Version 2: Added budgets table for envelope-style budgeting.
 *
 * CRITICAL: All money columns use INTEGER (Long), never REAL/FLOAT.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        SplitEntity::class,
        BudgetEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class BanygDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun splitDao(): SplitDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        private const val DATABASE_NAME = "banyg_database"

        @Volatile
        private var INSTANCE: BanygDatabase? = null

        /**
         * Get database instance (singleton)
         */
        fun getInstance(context: Context): BanygDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Build database with migrations
         *
         * IMPORTANT: fallbackToDestructiveMigration() is NOT enabled to prevent
         * catastrophic data loss. If a migration fails, the app will crash rather
         * than silently delete all user financial data. This is the correct behavior
         * for a personal finance app.
         *
         * Automatic backups are created before migrations via MigrationBackupCallback.
         */
        private fun buildDatabase(context: Context): BanygDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BanygDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(
                    MIGRATION_0_1,
                    MIGRATION_1_2
                )
                .addCallback(MigrationBackupCallback(context.applicationContext))
                .build()
        }

        /**
         * Build database with seeding callback.
         * Alternative entry point when seeding via callback is preferred.
         *
         * IMPORTANT: fallbackToDestructiveMigration() is NOT enabled to prevent
         * catastrophic data loss. If a migration fails, the app will crash rather
         * than silently delete all user financial data. This is the correct behavior
         * for a personal finance app.
         *
         * Automatic backups are created before migrations via MigrationBackupCallback.
         */
        fun buildDatabaseWithCallback(context: Context): BanygDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BanygDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_0_1, MIGRATION_1_2)
                .addCallback(MigrationBackupCallback(context.applicationContext))
                .addCallback(BanygDatabaseCallback())
                .build()
        }

        /**
         * Create in-memory database for testing
         */
        fun createInMemory(context: Context): BanygDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                BanygDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
