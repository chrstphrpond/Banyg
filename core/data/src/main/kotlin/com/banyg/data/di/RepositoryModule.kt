package com.banyg.data.di

import com.banyg.data.local.dao.AccountDao
import com.banyg.data.local.dao.CategoryDao
import com.banyg.data.local.dao.SplitDao
import com.banyg.data.local.dao.TransactionDao
import com.banyg.data.mapper.AccountMapper
import com.banyg.data.mapper.CategoryMapper
import com.banyg.data.mapper.SplitMapper
import com.banyg.data.mapper.TransactionMapper
import com.banyg.data.repository.AccountRepositoryImpl
import com.banyg.data.repository.CategoryRepositoryImpl
import com.banyg.data.repository.TransactionRepositoryImpl
import com.banyg.domain.repository.AccountRepository
import com.banyg.domain.repository.CategoryRepository
import com.banyg.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAccountRepository(
        accountDao: AccountDao
    ): AccountRepository = AccountRepositoryImpl(
        dao = accountDao,
        mapper = AccountMapper()
    )

    @Provides
    @Singleton
    fun provideCategoryRepository(
        categoryDao: CategoryDao
    ): CategoryRepository = CategoryRepositoryImpl(
        dao = categoryDao,
        mapper = CategoryMapper()
    )

    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao,
        splitDao: SplitDao
    ): TransactionRepository = TransactionRepositoryImpl(
        transactionDao = transactionDao,
        splitDao = splitDao,
        transactionMapper = TransactionMapper(SplitMapper()),
        splitMapper = SplitMapper()
    )
}
