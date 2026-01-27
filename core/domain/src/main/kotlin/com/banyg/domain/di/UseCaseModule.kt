package com.banyg.domain.di

import com.banyg.domain.repository.CategoryRepository
import com.banyg.domain.usecase.CreateCategoryUseCase
import com.banyg.domain.usecase.DeleteCategoryUseCase
import com.banyg.domain.usecase.GetCategoriesUseCase
import com.banyg.domain.usecase.SeedDefaultCategoriesUseCase
import com.banyg.domain.usecase.UpdateCategoryUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * UseCaseModule provides use case dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetCategoriesUseCase(
        categoryRepository: CategoryRepository
    ): GetCategoriesUseCase {
        return GetCategoriesUseCase(categoryRepository)
    }

    @Provides
    @Singleton
    fun provideCreateCategoryUseCase(
        categoryRepository: CategoryRepository
    ): CreateCategoryUseCase {
        return CreateCategoryUseCase(categoryRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateCategoryUseCase(
        categoryRepository: CategoryRepository
    ): UpdateCategoryUseCase {
        return UpdateCategoryUseCase(categoryRepository)
    }

    @Provides
    @Singleton
    fun provideDeleteCategoryUseCase(
        categoryRepository: CategoryRepository
    ): DeleteCategoryUseCase {
        return DeleteCategoryUseCase(categoryRepository)
    }

    @Provides
    @Singleton
    fun provideSeedDefaultCategoriesUseCase(
        categoryRepository: CategoryRepository
    ): SeedDefaultCategoriesUseCase {
        return SeedDefaultCategoriesUseCase(categoryRepository)
    }
}
