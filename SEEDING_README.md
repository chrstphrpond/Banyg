# Database Seeding System

## Overview

The Banyg app includes a comprehensive database seeding system that populates default categories on first app launch.

## Seeding Strategy: Application-Level

We chose **Application-Level Seeding** over RoomDatabase.Callback for the following reasons:

1. **Full DI Access**: Can use repositories and use cases with full dependency injection
2. **Better Error Handling**: Comprehensive logging and error recovery
3. **Easier Testing**: Can be mocked and tested independently
4. **Flexibility**: Can be disabled per environment (debug vs release)
5. **Coroutines Support**: Full Kotlin coroutines support for async operations

## Files Created

### 1. Default Categories Data
**File**: `core/domain/src/main/kotlin/com/banyg/domain/seed/DefaultCategories.kt`

Contains 28 default categories organized into groups:
- 12 Groups (parent categories)
- 15 Child categories
- 1 Uncategorized (standalone)

### 2. Seed Use Case
**File**: `core/domain/src/main/kotlin/com/banyg/domain/usecase/SeedDefaultCategoriesUseCase.kt`

- Checks if categories exist before seeding (idempotent)
- Handles groups first, then children
- Maps default data to domain models
- Returns `Result<Int>` with count of seeded categories

### 3. Application Seeder
**File**: `app/src/main/kotlin/com/banyg/seed/AppSeeder.kt`

- Singleton class that runs on app startup
- Seeds data in background coroutine (IO dispatcher)
- Provides logging for debugging
- Handles errors gracefully

### 4. Database Callback (Alternative)
**File**: `core/data/src/main/kotlin/com/banyg/data/local/BanygDatabaseCallback.kt`

- RoomDatabase.Callback implementation
- Available as backup seeding method
- Contains raw SQL for database-level seeding

### 5. Application Integration
**File**: `app/src/main/kotlin/com/banyg/BanygApplication.kt`

- Injects AppSeeder
- Calls `seedOnStartup()` in `onCreate()`

### 6. DI Configuration
**Files**: 
- `app/src/main/kotlin/com/banyg/di/UseCaseModule.kt`
- `app/src/main/kotlin/com/banyg/di/SeederModule.kt`

Provides SeedDefaultCategoriesUseCase and AppSeeder via Hilt.

### 7. Unit Tests
**File**: `core/domain/src/test/kotlin/com/banyg/domain/usecase/SeedDefaultCategoriesUseCaseTest.kt`

Comprehensive tests for:
- Seeding when database is empty
- Skipping when categories exist
- Error handling
- Correct category properties

## Default Categories

| Group | Categories |
|-------|-----------|
| **Income** | Salary, Other Income |
| **Housing** | Rent/Mortgage, Utilities |
| **Food & Dining** | Groceries, Restaurants, Fast Food |
| **Transportation** | Fuel, Public Transit, Rideshare |
| **Shopping** | General Shopping |
| **Entertainment** | Entertainment |
| **Health & Fitness** | Health & Fitness |
| **Personal Care** | Personal Care |
| **Education** | Education |
| **Gifts & Donations** | Gifts & Donations |
| **Business** | Business Expenses |
| **Travel** | Travel |
| **Financial** | Transfer, Fees & Charges |
| **Uncategorized** | Uncategorized |

## How It Works

1. **App Launch** → `BanygApplication.onCreate()`
2. **Trigger Seeding** → `AppSeeder.seedOnStartup()`
3. **Check Existing** → `SeedDefaultCategoriesUseCase` checks if categories exist
4. **Seed if Empty** → Inserts default categories (groups first, then children)
5. **Return Result** → Success with count (0 if already seeded)

## Testing

### Run Unit Tests
```bash
./gradlew :core:domain:test
```

### Test Seeding in App
1. Fresh install app
2. Check logcat for "Seeded X default categories"
3. Verify categories in UI
4. Kill and restart app
5. Should see "Categories already seeded, skipping"

### Manual Test via Debug
```kotlin
// In debug code, force re-seed
val useCase = SeedDefaultCategoriesUseCase(repository)
useCase() // Returns Result.success(0) if already seeded
```

## Soft Delete Support

Categories support soft delete via the `isHidden` flag:
- Users can hide unwanted default categories
- Hidden categories are excluded from selection lists
- Data is preserved for historical transactions
- Can be unhidden later if needed

## Future Enhancements

Potential improvements:
- Add default accounts (Cash, Bank, Credit Card)
- Add default currencies with exchange rates
- Add sample transactions for onboarding demo
- Add migration to add new categories in future versions
