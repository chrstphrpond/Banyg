---
name: android-test-gen
description: Generate unit tests for Android components (ViewModel, UseCase, Repository)
disable-model-invocation: true
---

# Android Test Generator

Generate comprehensive unit tests for Banyg Android components following project conventions.

## Supported Components

1. **ViewModels** - StateFlow, event handling, state transitions
2. **UseCases** - Business logic, domain rules
3. **Repositories** - Data operations, mapping
4. **Money Calculations** - Overflow, rounding, edge cases
5. **Mappers** - Entity ↔ Domain conversions

## Test Location Rules

```
core/domain/src/test/kotlin/       # Unit tests for domain logic
core/data/src/test/kotlin/         # Repository tests (use fake DAOs)
core/data/src/androidTest/kotlin/  # Room migration tests
feature/*/src/test/kotlin/         # ViewModel tests
```

## Test Patterns

### 1. ViewModel Test Template

```kotlin
// feature/[feature]/src/test/kotlin/com/banyg/[feature]/[Feature]ViewModelTest.kt

package com.banyg.[feature]

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {

    private lateinit var viewModel: FeatureViewModel
    private lateinit var useCase: FeatureUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        viewModel = FeatureViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(FeatureUiState.Loading::class.java)
        }
    }

    @Test
    fun `onEvent loads data successfully`() = runTest {
        // Given
        val mockData = listOf(/* mock data */)
        coEvery { useCase() } returns flowOf(mockData)

        // When
        viewModel.onEvent(FeatureUiEvent.LoadData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(FeatureUiState.Success::class.java)
            assertThat((state as FeatureUiState.Success).data).isEqualTo(mockData)
        }
        coVerify { useCase() }
    }

    @Test
    fun `onEvent handles error`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { useCase() } throws Exception(errorMessage)

        // When
        viewModel.onEvent(FeatureUiEvent.LoadData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(FeatureUiState.Error::class.java)
            assertThat((state as FeatureUiState.Error).message).contains(errorMessage)
        }
    }
}
```

**Required Dependencies**:
- MockK for mocking
- Truth for assertions
- Turbine for Flow testing
- Coroutines test library

---

### 2. UseCase Test Template

```kotlin
// core/domain/src/test/kotlin/com/banyg/domain/usecase/[UseCase]Test.kt

package com.banyg.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CalculateBudgetRemainingUseCaseTest {

    private lateinit var useCase: CalculateBudgetRemainingUseCase
    private lateinit var repository: BudgetRepository

    @Before
    fun setup() {
        repository = mockk()
        useCase = CalculateBudgetRemainingUseCase(repository)
    }

    @Test
    fun `calculate remaining returns correct amount`() = runTest {
        // Given
        val allocated = 50000L  // $500.00
        val spent = 30000L      // $300.00
        val expected = 20000L   // $200.00
        coEvery { repository.getAllocated(any(), any()) } returns allocated
        coEvery { repository.getSpent(any(), any()) } returns spent

        // When
        val result = useCase("category-1", "2024-01")

        // Then
        assertThat(result).isEqualTo(expected)
        coVerify { repository.getAllocated("category-1", "2024-01") }
        coVerify { repository.getSpent("category-1", "2024-01") }
    }

    @Test
    fun `calculate remaining handles zero allocated`() = runTest {
        // Given
        coEvery { repository.getAllocated(any(), any()) } returns 0L
        coEvery { repository.getSpent(any(), any()) } returns 0L

        // When
        val result = useCase("category-1", "2024-01")

        // Then
        assertThat(result).isEqualTo(0L)
    }

    @Test
    fun `calculate remaining handles overspending`() = runTest {
        // Given
        val allocated = 50000L  // $500.00
        val spent = 70000L      // $700.00
        val expected = -20000L  // -$200.00 (overspent)
        coEvery { repository.getAllocated(any(), any()) } returns allocated
        coEvery { repository.getSpent(any(), any()) } returns spent

        // When
        val result = useCase("category-1", "2024-01")

        // Then
        assertThat(result).isEqualTo(expected)
    }
}
```

---

### 3. Repository Test Template (with Fake DAO)

```kotlin
// core/data/src/test/kotlin/com/banyg/data/repository/[Entity]RepositoryImplTest.kt

package com.banyg.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TransactionRepositoryImplTest {

    private lateinit var repository: TransactionRepositoryImpl
    private lateinit var dao: FakeTransactionDao
    private lateinit var mapper: TransactionMapper

    @Before
    fun setup() {
        dao = FakeTransactionDao()
        mapper = TransactionMapper()
        repository = TransactionRepositoryImpl(dao, mapper)
    }

    @Test
    fun `observeByAccount emits transactions for account`() = runTest {
        // Given
        val accountId = "account-1"
        val entities = listOf(
            createTransactionEntity(id = "1", accountId = accountId),
            createTransactionEntity(id = "2", accountId = accountId)
        )
        dao.insertAll(entities)

        // When
        val result = repository.observeByAccount(accountId).first()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo("1")
        assertThat(result[1].id).isEqualTo("2")
    }

    @Test
    fun `save inserts transaction entity`() = runTest {
        // Given
        val transaction = createTransaction(id = "1", amountMinor = 10000L)

        // When
        repository.save(transaction)

        // Then
        val saved = dao.getById("1")
        assertThat(saved).isNotNull()
        assertThat(saved?.amountMinor).isEqualTo(10000L)
    }

    @Test
    fun `mapper converts entity to domain correctly`() {
        // Given
        val entity = createTransactionEntity(
            id = "1",
            amountMinor = -50000L,  // Expense
            merchant = "Coffee Shop"
        )

        // When
        val domain = mapper.toDomain(entity)

        // Then
        assertThat(domain.id).isEqualTo("1")
        assertThat(domain.amountMinor).isEqualTo(-50000L)
        assertThat(domain.merchant).isEqualTo("Coffee Shop")
    }
}

// Fake DAO for testing
class FakeTransactionDao : TransactionDao {
    private val transactions = mutableListOf<TransactionEntity>()

    override fun observeByAccount(accountId: String) = flowOf(
        transactions.filter { it.accountId == accountId }
    )

    override suspend fun getById(id: String) = transactions.find { it.id == id }

    override suspend fun insert(entity: TransactionEntity) {
        transactions.removeIf { it.id == entity.id }
        transactions.add(entity)
    }

    override suspend fun insertAll(entities: List<TransactionEntity>) {
        entities.forEach { insert(it) }
    }

    // ... implement other methods
}
```

---

### 4. Money Calculator Test Template

```kotlin
// core/domain/src/test/kotlin/com/banyg/domain/calculator/MoneyCalculatorTest.kt

package com.banyg.domain.calculator

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

class MoneyCalculatorTest {

    @Test
    fun `add positive amounts returns sum`() {
        val result = MoneyCalculator.add(100L, 200L)
        assertThat(result).isEqualTo(300L)
    }

    @Test
    fun `add negative amounts returns sum`() {
        val result = MoneyCalculator.add(-100L, -200L)
        assertThat(result).isEqualTo(-300L)
    }

    @Test
    fun `add with zero returns original amount`() {
        val result = MoneyCalculator.add(500L, 0L)
        assertThat(result).isEqualTo(500L)
    }

    @Test
    fun `add overflow throws ArithmeticException`() {
        assertFailsWith<ArithmeticException> {
            MoneyCalculator.add(Long.MAX_VALUE, 1L)
        }
    }

    @Test
    fun `multiply by factor rounds correctly`() {
        val result = MoneyCalculator.multiply(10000L, 1.5)  // $100 * 1.5
        assertThat(result).isEqualTo(15000L)  // $150
    }

    @Test
    fun `multiply by factor rounds half up`() {
        val result = MoneyCalculator.multiply(10001L, 0.5)  // $100.01 * 0.5
        assertThat(result).isEqualTo(5001L)  // $50.01 (rounds up)
    }

    @Test
    fun `percentage calculates correctly`() {
        val result = MoneyCalculator.percentage(10000L, 15)  // 15% of $100
        assertThat(result).isEqualTo(1500L)  // $15
    }

    @Test
    fun `percentage of zero returns zero`() {
        val result = MoneyCalculator.percentage(0L, 50)
        assertThat(result).isEqualTo(0L)
    }

    @Test
    fun `percentage with invalid percent throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            MoneyCalculator.percentage(10000L, 101)
        }
    }

    @Test
    fun `large amounts don't overflow in multiplication`() {
        val amount = Long.MAX_VALUE / 10
        val result = MoneyCalculator.multiply(amount, 0.5)
        assertThat(result).isGreaterThan(0L)
    }
}
```

---

## Test Requirements Checklist

For each component, ensure tests cover:

- [ ] **Happy path** - Typical valid inputs
- [ ] **Edge cases** - Zero, boundaries, max values
- [ ] **Invalid inputs** - Negative where not allowed, nulls
- [ ] **Overflow scenarios** - Math.addExact, Math.multiplyExact
- [ ] **Rounding behavior** - Money calculations
- [ ] **Business rule violations** - Domain invariants
- [ ] **Error handling** - Exception cases
- [ ] **Async operations** - Coroutines, Flow emissions

## Money-Specific Test Cases

Always include these for money operations:

```kotlin
@Test
fun `handles zero amount`()

@Test
fun `handles negative amount (expense)`()

@Test
fun `handles positive amount (income)`()

@Test
fun `handles maximum Long value without overflow`()

@Test
fun `throws on overflow`()

@Test
fun `rounds to nearest cent correctly`()
```

## Naming Conventions

- Test file: `[ClassName]Test.kt`
- Test function: `` `description with spaces` ``
- Use explicit `assertEquals(expected, actual)`
- Never use snapshot tests for money

## Dependencies to Include

Add to module's `build.gradle.kts`:

```kotlin
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.truth)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)  // For Flow testing
```

## Workflow

1. Identify component type (ViewModel, UseCase, Repository, Calculator)
2. Choose appropriate template
3. Generate test class with all required tests
4. Ensure money safety tests if applicable
5. Add edge cases specific to the component
6. Run tests: `./gradlew :[module]:test`
7. Verify coverage of critical paths

## Usage

```bash
# Invoke the skill
/android-test-gen

# Then specify:
# - Component type (ViewModel, UseCase, Repository, etc.)
# - Component name
# - Specific methods/functions to test
```

## Output

The skill generates:
1. Complete test file with package and imports
2. Setup and teardown methods
3. All required test cases
4. Helper methods (fakes, builders)
5. Comments explaining assertions
