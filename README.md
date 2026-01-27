# Banyg Finance OS

Local-first personal finance OS for Android.

## Project Structure

```
app/                        # Main application module
├── src/main/kotlin/        # Application code

core/domain/                # Pure Kotlin domain layer (no Android dependencies)
├── src/main/kotlin/        # Domain models, use cases, repository interfaces
├── src/test/kotlin/        # Unit tests

core/data/                  # Data layer (Room, DataStore, repositories)
├── src/main/kotlin/        # Room entities, DAOs, repository implementations
├── src/test/kotlin/        # Unit tests
├── src/androidTest/kotlin/ # Instrumented tests (DAO, migrations)
├── schemas/                # Room schema exports

core/ui/                    # Shared UI components and theme
├── src/main/kotlin/        # Composables, theme, design system

feature/inbox/              # Inbox feature module
feature/accounts/           # Accounts feature module
feature/budget/             # Budget feature module
feature/reports/            # Reports feature module
```

## Tech Stack

- **Kotlin** - Primary language
- **Jetpack Compose** - UI framework (Material 3)
- **Hilt** - Dependency injection
- **Room** - Local database
- **DataStore** - Preferences storage
- **Coroutines & Flow** - Async operations
- **Navigation Compose** - Navigation

## Build Commands

```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run tests for specific module
./gradlew :core:domain:test
./gradlew :core:data:test

# Build debug APK
./gradlew :app:assembleDebug

# Run linter
./gradlew lint
```

## Module Dependencies

```
app
├─ feature:inbox
├─ feature:accounts
├─ feature:budget
├─ feature:reports
└─ core:ui
   └─ core:domain

core:data
└─ core:domain
```

## Money Safety

**Critical:** All money amounts MUST be stored as `Long` (minor units/centavos). Never use `Float` or `Double`.

See [MONEY_RULES.md](MONEY_RULES.md) for detailed rules.

## Documentation

- [PRD.md](PRD.md) - Product requirements
- [DATA_MODEL.md](DATA_MODEL.md) - Database schema
- [MONEY_RULES.md](MONEY_RULES.md) - Money handling rules
- [BUDGET_MATH.md](BUDGET_MATH.md) - Budget calculations
- [CLAUDE.md](CLAUDE.md) - Development guidelines

## Development

Before implementing:
1. Read CLAUDE.md for architecture patterns
2. Follow money safety rules (no Float/Double for amounts)
3. Write tests for domain logic
4. Use Hilt for dependency injection

## License

Proprietary
