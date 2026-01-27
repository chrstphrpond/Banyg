# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Product Overview

Banyg is a local-first personal finance OS for Android with focus on fast transaction capture, inbox review, budgets, and simple reports. Local-first means offline works by default; cloud sync is optional.

## Tech Stack

- Kotlin, Jetpack Compose (Material 3)
- Room (SQLite) for local database
- DataStore for preferences
- Coroutines + Flow
- Hilt for dependency injection

Before adding new dependencies, propose them with pros/cons and minimal alternatives.

## Architecture

**Module structure and dependencies:**
```
app/
├─ depends on: feature/*, core/*

feature/[feature]/
├─ [feature]Screen.kt         # Composable + Route
├─ [feature]ViewModel.kt      # StateFlow + Events
├─ [feature]UiState.kt        # Sealed UI states
├─ depends on: core/domain, core/ui

core/domain/
├─ model/                     # Money, Account, Transaction
├─ repository/                # Interfaces only
├─ usecase/                   # Business logic
├─ depends on: NOTHING (pure Kotlin)

core/data/
├─ local/entity/              # Room entities
├─ local/dao/                 # Room DAOs
├─ repository/                # Repository implementations
├─ mapper/                    # Entity ↔ Domain
├─ depends on: core/domain

core/ui/
├─ theme/                     # Color, Type, Shape
├─ components/                # Reusable composables
├─ depends on: core/domain (for types only)
```

**Strict rules:**
- `core/domain` has ZERO Android imports
- Repository interfaces in `core/domain/repository`
- Repository implementations in `core/data/repository`
- ViewModels use `core/domain` types, never Room entities
- Composables never import Room or DataStore

## Critical Money Handling Rules

**NEVER use Float or Double for money. Grep checks before commit:**
```bash
# These patterns must NOT exist in production code:
git grep -n "amountMinor.*toDouble\|amountMinor.*toFloat"
git grep -n "amount.*Double\|amount.*Float" -- "*.kt" | grep -v "Test.kt"
```

**Required patterns:**
```kotlin
// ✅ CORRECT - Money type
data class Money(val minorUnits: Long, val currency: Currency)

// ✅ CORRECT - Room column
@ColumnInfo(name = "amount_minor") val amountMinor: Long

// ✅ CORRECT - Arithmetic
fun add(a: Long, b: Long): Long = Math.addExact(a, b)

// ❌ FORBIDDEN - Never do this
val amount: Double = transaction.amount
val total = amount * 1.15
@ColumnInfo(name = "amount") val amount: Float
```

**Sign conventions (enforceable):**
- Expense: `amountMinor < 0`
- Income: `amountMinor > 0`
- Transfers: `fromAmount + toAmount == 0L`

**Formatting (UI layer only):**
```kotlin
// In core/ui only, never in domain/data
fun Long.formatAsCurrency(currency: Currency): String
```

## Domain Model

Core entities (adjust based on actual implementation):
- **Account**: id, name, type, currency, openingBalance, archived
- **Transaction**: id, accountId, date, amountMinor, merchant, memo, categoryId, status, clearedAt
- **Split**: transactionId, lineId, categoryId, amountMinor
- **Transfer**: Two linked transactions with shared transferId
- **Category**: id, groupId, name, hidden
- **BudgetPeriod**: monthKey (YYYY-MM), allocations, activity, available

Schema changes require: migration plan, migration tests, and DATA_MODEL.md update.

## Compose UI Conventions

**Required ViewModel pattern:**
```kotlin
@HiltViewModel
class ScreenViewModel @Inject constructor(
    private val useCase: UseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenUiState>(ScreenUiState.Loading)
    val uiState: StateFlow<ScreenUiState> = _uiState.asStateFlow()

    fun onEvent(event: ScreenUiEvent) { /* handle */ }
}
```

**Required Screen pattern:**
```kotlin
// Route composable - connects to ViewModel
@Composable
fun ScreenRoute(
    onNavigateBack: () -> Unit,
    viewModel: ScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenContent(uiState = uiState, onEvent = viewModel::onEvent)
}

// Stateless content composable
@Composable
private fun ScreenContent(
    uiState: ScreenUiState,
    onEvent: (ScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) { /* UI */ }
```

**Required Preview pattern:**
```kotlin
@Preview(showBackground = true)
@Composable
private fun ScreenPreview() {
    BanygTheme {
        ScreenContent(
            uiState = ScreenUiState.Success(/* sample data */),
            onEvent = {}
        )
    }
}
```

**Enforced rules:**
- ViewModels: `StateFlow`, never `LiveData`
- State collection: `collectAsStateWithLifecycle()`, never `collectAsState()`
- Modifier always a parameter (first or last in optional params)
- No `Context` in composables (pass callbacks instead)

## Branding (Banyg)

Subtle hyperlocal cues without obvious symbols:
- Woven rhythm, soft glow, calm gradients
- Modern sans typography (2-3 weights max)
- Calm, short motion (no bouncy money numbers)

All new UI must use the design system layer.

## Data Layer Conventions

**Room DAO pattern:**
```kotlin
@Dao
interface TransactionDao {
    // Observe: Flow for reactive updates
    @Query("SELECT * FROM transactions WHERE account_id = :accountId")
    fun observeByAccount(accountId: String): Flow<List<TransactionEntity>>

    // Single read: suspend for one-shot queries
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    // Write: suspend, use @Transaction for complex ops
    @Transaction
    suspend fun insertWithSplits(transaction: TransactionEntity, splits: List<SplitEntity>)
}
```

**Repository pattern:**
```kotlin
// Interface in core/domain/repository
interface TransactionRepository {
    fun observeByAccount(accountId: String): Flow<List<Transaction>>
    suspend fun save(transaction: Transaction)
}

// Implementation in core/data/repository
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val mapper: TransactionMapper
) : TransactionRepository {
    override fun observeByAccount(accountId: String): Flow<List<Transaction>> =
        dao.observeByAccount(accountId).map { it.map(mapper::toDomain) }
}
```

**Required indices:**
```kotlin
@Entity(
    indices = [
        Index(value = ["account_id"]),     // FK queries
        Index(value = ["date"]),           // Date range queries
        Index(value = ["category_id"]),    // Category filtering
        Index(value = ["status"])          // Status filtering
    ]
)
```

**Multi-write pattern:**
```kotlin
@Transaction  // Ensures atomicity
suspend fun createTransfer(from: Transaction, to: Transaction) {
    require(from.amountMinor + to.amountMinor == 0L)
    dao.insert(mapper.toEntity(from))
    dao.insert(mapper.toEntity(to))
}
```

## CSV Import (MVP)

- Support column mapping: date, description, amount, optional debit/credit/balance
- Normalize merchant strings (trim, collapse spaces)
- Deduping: use deterministic fingerprint, make user-reviewable
- Never silently delete data (prefer soft-delete or mark duplicates)

## Testing Requirements

**Test locations:**
```
core/domain/src/test/kotlin/       # Unit tests for domain logic
core/data/src/test/kotlin/         # Repository tests (use fake DAOs)
core/data/src/androidTest/kotlin/  # Room migration tests
feature/*/src/test/kotlin/         # ViewModel tests
```

**Required tests:**
```kotlin
// core/domain/calculator/MoneyCalculatorTest.kt
class MoneyCalculatorTest {
    @Test
    fun `add positive amounts returns sum`() {
        val result = MoneyCalculator.add(100L, 200L)
        assertEquals(300L, result)
    }

    @Test
    fun `add overflow throws ArithmeticException`() {
        assertThrows<ArithmeticException> {
            MoneyCalculator.add(Long.MAX_VALUE, 1L)
        }
    }
}

// core/data/local/migration/MigrationTest.kt
@RunWith(AndroidJUnit4::class)
class Migration1to2Test {
    @get:Rule
    val helper = MigrationTestHelper(...)

    @Test
    fun migrate1to2() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
    }
}
```

**Naming convention:**
- Test files: `[ClassName]Test.kt`
- Test functions: `` `description with spaces` ``
- Use explicit `assertEquals(expected, actual)`, never snapshot tests for money

## Development Commands

**Run specific module tests:**
```bash
./gradlew :core:domain:test                    # Domain logic tests
./gradlew :core:data:test                      # Repository tests
./gradlew :core:data:connectedAndroidTest      # Room migration tests
./gradlew :feature:inbox:test                  # Feature tests
```

**Run single test:**
```bash
./gradlew :core:domain:test --tests "MoneyCalculatorTest"
./gradlew :core:domain:test --tests "*add*"
```

**Verify money safety (run before commit):**
```bash
# Check for Float/Double in money context
git grep -n "amount.*Double\|amount.*Float" -- "*.kt" | grep -v "Test.kt"

# Check for missing amountMinor suffix in Room entities
git grep -n "@ColumnInfo.*amount" -- "*Entity.kt" | grep -v "amount_minor"
```

**Build and lint:**
```bash
./gradlew build                    # Full build
./gradlew :app:assembleDebug       # Build debug APK
./gradlew lint                     # Run all linters
./gradlew detekt                   # Static analysis (if configured)
```

## Working Style

**Before implementing:**
1. Check if money is involved → verify Long type, not Float/Double
2. Check if database schema changes → need migration
3. Check if new dependency → propose with alternatives

**Implementation steps:**
1. State goal in one sentence
2. List exact files to create/modify
3. Make minimal, focused changes
4. Run tests: `./gradlew :[module]:test`
5. Verify money safety checks pass
6. Summarize what changed

**Always ask before:**
- Adding dependencies (especially for common tasks)
- Changing database schema (need migration + tests)
- Renaming public APIs in domain layer

## Security

Never read or modify:
- `.env`, `*.keystore`, `google-services.json`, `local.properties`, or any credentials
- Ask for redacted samples if needed
