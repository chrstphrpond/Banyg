package com.banyg.seed

import android.util.Log
import com.banyg.domain.usecase.SeedDefaultCategoriesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-level database seeder.
 *
 * Seeds initial data on app startup in a background coroutine.
 * This is the primary seeding mechanism for the Banyg app.
 *
 * Benefits over RoomDatabase.Callback:
 * - Full access to repositories and use cases
 * - Better error handling and logging
 * - Easier to test and debug
 * - Can be disabled or configured per environment
 */
@Singleton
class AppSeeder @Inject constructor(
    private val seedDefaultCategoriesUseCase: SeedDefaultCategoriesUseCase
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Seeds all default data in background.
     * Should be called from Application.onCreate().
     */
    fun seedOnStartup() {
        scope.launch {
            try {
                Log.d(TAG, "Starting database seeding...")

                // Seed default categories
                val result = seedDefaultCategoriesUseCase()

                result.fold(
                    onSuccess = { count ->
                        when {
                            count > 0 -> Log.i(TAG, "Seeded $count default categories")
                            else -> Log.d(TAG, "Categories already seeded, skipping")
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to seed categories", error)
                    }
                )

                Log.d(TAG, "Database seeding completed")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during seeding", e)
            }
        }
    }

    companion object {
        private const val TAG = "AppSeeder"
    }
}
