package com.banyg.di

import com.banyg.domain.repository.AccountRepository
import com.banyg.domain.repository.CategoryRepository
import com.banyg.domain.repository.TransactionRepository
import com.banyg.domain.usecase.AddManualTransactionUseCase
import com.banyg.domain.usecase.CreateAccountUseCase
import com.banyg.domain.usecase.SeedDefaultCategoriesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideCreateAccountUseCase(
        accountRepository: AccountRepository
    ): CreateAccountUseCase = CreateAccountUseCase(accountRepository)

    @Provides
    @Singleton
    fun provideAddManualTransactionUseCase(
        transactionRepository: TransactionRepository,
        accountRepository: AccountRepository
    ): AddManualTransactionUseCase = AddManualTransactionUseCase(
        transactionRepository = transactionRepository,
        accountRepository = accountRepository
    )

    @Provides
    @Singleton
    fun provideSeedDefaultCategoriesUseCase(
        categoryRepository: CategoryRepository
    ): SeedDefaultCategoriesUseCase = SeedDefaultCategoriesUseCase(categoryRepository)
}
