---
name: use-case
description: Generate Clean Architecture use cases following Banyg domain layer patterns with Result types and coroutines
disable-model-invocation: true
---

# Use Case Generator

Generate Clean Architecture use cases following Banyg domain layer patterns.

## Output Structure

### 1. Basic Use Case
```kotlin
// core/domain/usecase/[Action][Entity]UseCase.kt

/**
 * [Description of what this use case does]
 */
class ActionEntityUseCase @Inject constructor(
    private val repository: EntityRepository
) {
    /**
     * [Detailed description of the operation]
     *
     * @param param1 Description
     * @param param2 Description
     * @return Result containing [ReturnType] on success, or exception on failure
     */
    suspend operator fun invoke(
        param1: Param1Type,
        param2: Param2Type
    ): Result<ReturnType> {
        return try {
            // Validate inputs
            require(param1.isNotBlank()) { "Param1 cannot be blank" }
            
            // Business logic
            val result = repository.someOperation(param1, param2)
            
            Result.success(result)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 2. Flow-Based Use Case (for observing data)
```kotlin
// core/domain/usecase/ObserveEntitiesUseCase.kt

/**
 * Observes [entities] as a stream of updates.
 */
class ObserveEntitiesUseCase @Inject constructor(
    private val repository: EntityRepository
) {
    /**
     * Returns a Flow of entities that emits whenever data changes.
     *
     * @param filter Optional filter criteria
     * @return Flow of entity list
     */
    operator fun invoke(filter: EntityFilter? = null): Flow<List<Entity>> {
        return repository.observeAll()
            .map { entities ->
                filter?.let { f ->
                    entities.filter { f.matches(it) }
                } ?: entities
            }
            .flowOn(Dispatchers.Default)
    }
}
```

### 3. Money-Safe Use Case
```kotlin
// core/domain/usecase/CalculateBudgetRemainingUseCase.kt

/**
 * Calculates remaining budget amount for a category in a given month.
 * Follows money safety rules (Long for cents, no Float/Double arithmetic).
 */
class CalculateBudgetRemainingUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    data class Result(
        val budgetedMinor: Long,
        val spentMinor: Long,
        val remainingMinor: Long,
        val isOverBudget: Boolean
    )

    suspend operator fun invoke(
        categoryId: String,
        monthKey: String  // Format: "yyyy-MM"
    ): Result {
        val budget = budgetRepository.getByCategoryAndMonth(categoryId, monthKey)
        val budgetedMinor = budget?.amountMinor ?: 0L

        val spentMinor = transactionRepository
            .getByCategoryAndMonth(categoryId, monthKey)
            .sumOf { transaction ->
                // Only count expenses (negative amounts)
                if (transaction.amountMinor < 0) {
                    kotlin.math.abs(transaction.amountMinor)
                } else 0L
            }

        val remainingMinor = budgetedMinor - spentMinor

        return Result(
            budgetedMinor = budgetedMinor,
            spentMinor = spentMinor,
            remainingMinor = remainingMinor,
            isOverBudget = remainingMinor < 0
        )
    }
}
```

### 4. Validation Use Case
```kotlin
// core/domain/usecase/ValidateTransferUseCase.kt

/**
 * Validates a transfer between two accounts.
 * Ensures transfer invariants are maintained.
 */
class ValidateTransferUseCase @Inject constructor() {
    
    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
    }

    operator fun invoke(
        fromAmountMinor: Long,
        toAmountMinor: Long
    ): ValidationResult {
        return when {
            fromAmountMinor >= 0 -> 
                ValidationResult.Invalid("From amount must be negative (outflow)")
            
            toAmountMinor <= 0 -> 
                ValidationResult.Invalid("To amount must be positive (inflow)")
            
            fromAmountMinor + toAmountMinor != 0L -> 
                ValidationResult.Invalid("Transfer must net to zero")
            
            else -> ValidationResult.Valid
        }
    }
}
```

### 5. Composite Use Case
```kotlin
// core/domain/usecase/GetDashboardDataUseCase.kt

/**
 * Aggregates data for the dashboard screen.
 * Combines multiple data sources into a single dashboard state.
 */
class GetDashboardDataUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) {
    data class DashboardData(
        val totalBalance: Long,
        val accounts: List<AccountSummary>,
        val recentTransactions: List<Transaction>,
        val budgetStatus: List<BudgetStatus>
    )

    data class AccountSummary(
        val account: Account,
        val currentBalance: Long
    )

    data class BudgetStatus(
        val category: Category,
        val budgeted: Long,
        val spent: Long,
        val remaining: Long
    )

    suspend operator fun invoke(): DashboardData {
        val accounts = accountRepository.getAll()
        val totalBalance = accounts.sumOf { it.currentBalanceMinor }

        val recentTransactions = transactionRepository
            .getRecent(limit = 10)

        val budgetStatus = budgetRepository
            .getCurrentMonthBudgets()
            .map { budget ->
                val spent = transactionRepository
                    .getSpentByCategory(budget.categoryId, budget.monthKey)
                
                BudgetStatus(
                    category = budget.category,
                    budgeted = budget.amountMinor,
                    spent = spent,
                    remaining = budget.amountMinor - spent
                )
            }

        return DashboardData(
            totalBalance = totalBalance,
            accounts = accounts.map { AccountSummary(it, it.currentBalanceMinor) },
            recentTransactions = recentTransactions,
            budgetStatus = budgetStatus
        )
    }
}
```

### 6. Unit Tests
```kotlin
// core/domain/usecase/ActionEntityUseCaseTest.kt

@ExperimentalCoroutinesApi
class ActionEntityUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: EntityRepository
    private lateinit var useCase: ActionEntityUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = ActionEntityUseCase(repository)
    }

    @Test
    fun `invoke with valid params returns success`() = runTest {
        // Given
        val expected = Entity(/* ... */)
        coEvery { repository.someOperation(any(), any()) } returns expected

        // When
        val result = useCase(param1 = "valid", param2 = 123)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `invoke with blank param1 returns failure`() = runTest {
        // When
        val result = useCase(param1 = "", param2 = 123)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke when repository throws returns failure`() = runTest {
        // Given
        coEvery { repository.someOperation(any(), any()) } 
            throws RuntimeException("DB error")

        // When
        val result = useCase(param1 = "valid", param2 = 123)

        // Then
        assertTrue(result.isFailure)
    }
}
```

## Naming Conventions

**Pattern:** `[Action][Entity]UseCase`

| Action | When to Use |
|--------|-------------|
| `Get` | Retrieve single entity by ID |
| `GetAll` | Retrieve all entities |
| `Observe` | Returns Flow for reactive updates |
| `Create` / `Add` | Create new entity |
| `Update` | Modify existing entity |
| `Delete` / `Remove` | Remove entity |
| `Validate` | Validation only, no data changes |
| `Calculate` | Computation/aggregation |
| `Import` | Bulk import operations |
| `Sync` | Synchronization operations |

## Requirements

**Always include:**
- `@Inject` constructor for dependency injection
- `operator fun invoke()` for callable syntax
- `suspend` for async operations (unless returning Flow)
- `Result<T>` return type for single operations
- Input validation with meaningful error messages
- Proper exception handling
- Comprehensive unit tests

**For money operations:**
- Use `Long` for amounts (minor units)
- Never use `Float` or `Double`
- Document sign conventions (negative = expense)

## Workflow

1. Identify the single responsibility of the use case
2. Determine dependencies (repositories, other use cases)
3. Define input parameters
4. Define return type (Result<T> or Flow<T>)
5. Implement validation logic
6. Implement business logic
7. Handle errors appropriately
8. Write unit tests

## Anti-Patterns to Avoid

```kotlin
// ❌ DON'T: Mix concerns
class BadUseCase(
    private val repo1: Repo1,
    private val repo2: Repo2,
    private val repo3: Repo3
) {
    suspend fun doEverything() {
        // Too many responsibilities
    }
}

// ❌ DON'T: Return nullable instead of Result
class BadUseCase {
    suspend fun execute(): Entity?  // Caller can't tell why it's null
}

// ❌ DON'T: Expose implementation details
class BadUseCase {
    suspend fun execute(): Pair<Boolean, String>  // Unclear what this means
}

// ✅ DO: Single responsibility, clear types
class GoodUseCase {
    suspend operator fun invoke(): Result<Entity>  // Clear success/failure
}
```
