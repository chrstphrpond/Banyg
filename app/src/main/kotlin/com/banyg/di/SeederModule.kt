package com.banyg.di

import com.banyg.seed.AppSeeder
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for seeding components.
 *
 * Provides AppSeeder and related dependencies.
 * AppSeeder is constructor-injected, so no explicit @Provides needed.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SeederModule {
    // AppSeeder uses constructor injection, no binding needed
}
