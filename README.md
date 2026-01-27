# Banyg Finance OS

Local-first personal finance OS for Android. Track spending, manage budgets, and import bank statements—all offline.

## Features

| Feature | Status | Description |
|---------|--------|-------------|
| ✅ Inbox | Complete | Review and categorize transactions |
| ✅ Add Transaction | Complete | Quick capture (3 taps or less) |
| ✅ Budgets | Complete | Set spending limits by category |
| ✅ Reports | Complete | View spending breakdown by period |
| ✅ CSV Import | Complete | Import bank statements |
| ✅ Offline-First | Complete | Works without internet |
| ✅ Money-Safe | Complete | Uses Long for all calculations |

## Screenshots

*(Coming soon)*

## Architecture

Banyg follows **Clean Architecture** with feature modules:

```
app/                        # Main application module
├── src/main/kotlin/        # Application code, navigation, DI

core/domain/                # Pure Kotlin domain layer
├── model/                  # Domain models (no Android deps)
├── repository/             # Repository interfaces
└── usecase/                # Business logic

core/data/                  # Data layer
├── local/entity/           # Room entities
├── local/dao/              # Room DAOs
├── repository/             # Repository implementations
└── mapper/                 # Entity ↔ Domain mappers

core/ui/                    # Shared UI components
├── theme/                  # Material 3 theme
└── components/             # Reusable composables

feature/inbox/              # Inbox feature module
feature/accounts/           # Accounts feature module
feature/budget/             # Budget feature module
feature/reports/            # Reports feature module
feature/csvimport/          # CSV import feature module
feature/categories/         # Categories feature module
```

### Module Dependencies

```
app
├─ feature:inbox, accounts, budget, reports, csvimport, categories
└─ core:ui
   └─ core:domain

core:data
└─ core:domain
```

**Strict rules:**
- `core/domain` has ZERO Android imports
- Repository interfaces in `core/domain/repository`
- Repository implementations in `core/data/repository`
- ViewModels use `core/domain` types, never Room entities
- Composables never import Room or DataStore

## Tech Stack

- **Kotlin** - Primary language
- **Jetpack Compose** - UI framework (Material 3)
- **Hilt** - Dependency injection
- **Room** - Local database (SQLite)
- **DataStore** - Preferences storage
- **Coroutines & Flow** - Async operations
- **Navigation Compose** - Navigation

## Build Instructions

### Prerequisites

- Android Studio (latest stable)
- JDK 17 or higher
- Android SDK with API 34

### Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run tests for specific module
./gradlew :core:domain:test
./gradlew :core:data:test
./gradlew :feature:inbox:test

# Run linter
./gradlew lint
```

### Running the App

1. Open the project in Android Studio
2. Sync Gradle files
3. Select a device or emulator
4. Click Run (or `./gradlew :app:installDebug`)

## Testing

### Unit Tests

```bash
# Domain layer tests (pure Kotlin)
./gradlew :core:domain:test

# Data layer tests
./gradlew :core:data:test

# Feature module tests
./gradlew :feature:inbox:test
./gradlew :feature:budget:test
```

### Instrumented Tests

```bash
# Room migration tests
./gradlew :core:data:connectedAndroidTest

# Full instrumented test suite
./gradlew connectedAndroidTest
```

### Running Single Test

```bash
./gradlew :core:domain:test --tests "MoneyCalculatorTest"
./gradlew :core:domain:test --tests "*add*"
```

## Money Safety

**Critical:** All money amounts MUST be stored as `Long` (minor units/centavos). Never use `Float` or `Double`.

```kotlin
// ✅ CORRECT
@ColumnInfo(name = "amount_minor") val amountMinor: Long

// ❌ FORBIDDEN
@ColumnInfo(name = "amount") val amount: Double
```

See [MONEY_RULES.md](MONEY_RULES.md) for detailed rules.

## User Guide

See [USER_GUIDE.md](USER_GUIDE.md) for end-user documentation.

## Development Documentation

- [CLAUDE.md](CLAUDE.md) - Development guidelines and patterns
- [PRD.md](PRD.md) - Product requirements
- [DATA_MODEL.md](DATA_MODEL.md) - Database schema
- [MONEY_RULES.md](MONEY_RULES.md) - Money handling rules
- [BUDGET_MATH.md](BUDGET_MATH.md) - Budget calculations
- [ROADMAP.md](ROADMAP.md) - Development roadmap

## Development Workflow

Before implementing:
1. Read [CLAUDE.md](CLAUDE.md) for architecture patterns
2. Follow money safety rules (no Float/Double for amounts)
3. Write tests for domain logic
4. Use Hilt for dependency injection

Pre-commit checks:
```bash
# Check for Float/Double used with money
git grep -n "amount.*Double\|amount.*Float" -- "*.kt" | grep -v "Test.kt"

# Check for missing amount_minor suffix in entities
git grep -n "@ColumnInfo.*amount[^_]" -- "*Entity.kt"
```

## License

Proprietary
