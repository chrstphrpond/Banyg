package com.banyg.data.di

import android.content.Context
import com.banyg.data.local.BanygDatabase
import com.banyg.data.local.dao.AccountDao
import com.banyg.data.local.dao.BudgetDao
import com.banyg.data.local.dao.CategoryDao
import com.banyg.data.local.dao.SplitDao
import com.banyg.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): BanygDatabase = BanygDatabase.getInstance(context)

    @Provides
    fun provideAccountDao(database: BanygDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: BanygDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: BanygDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideSplitDao(database: BanygDatabase): SplitDao = database.splitDao()

    @Provides
    fun provideBudgetDao(database: BanygDatabase): BudgetDao = database.budgetDao()
}
