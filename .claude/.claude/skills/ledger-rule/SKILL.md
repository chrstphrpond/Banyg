---
name: ledger-rule
description: Generate money-safe domain logic with unit tests (enforces Long for cents, no Float/Double)
---

# Ledger Rule Generator

Generate domain logic for money operations with integer-safe arithmetic and comprehensive tests.

## Supported Operations

- Money arithmetic (add, subtract, multiply, divide)
- Split calculations
- Transfer validations
- Budget calculations
- Reconciliation checks
- Balance calculations
- Percentage/proration logic

## Critical Rules

**ENFORCED:**
1. Never use `Float` or `Double` for money
2. Always use `Long` for minor units (centavos/cents)
3. All calculations must be integer-safe
4. Every domain function must have unit tests
5. Sign conventions:
   - Expense: negative
   - Income: positive
   - Transfers: net to zero overall

## Output Structure

### 1. Domain Class/Function
```kotlin
// core/domain/[feature]/[Operation].kt

/**
 * [Description of the business rule]
 *
 * @param amountMinor Amount in minor units (cents/centavos)
 * @return Result in minor units
 */
fun calculateAmount(
    amountMinor: Long,
    // ... other parameters
): Long {
    require(amountMinor >= 0) { "Amount cannot be negative" }

    // Integer-safe calculation
    return result
}
```

### 2. Data Classes (if needed)
```kotlin
data class Split(
    val categoryId: String,
    val amountMinor: Long,
    val description: String? = null
) {
    init {
        require(amountMinor != 0L) { "Split amount cannot be zero" }
    }
}
```

### 3. Use Case (if applicable)
```kotlin
class CalculateBudgetRemainingUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(
        categoryId: String,
        monthKey: String
    ): Long {
        // Implementation
    }
}
```

### 4. Unit Tests (Required)
```kotlin
// core/domain/[feature]/[Operation]Test.kt

class CalculateAmountTest {

    @Test
    fun `positive amount returns correct result`() {
        val result = calculateAmount(amountMinor = 1000L)
        assertEquals(expected, result)
    }

    @Test
    fun `zero amount is handled correctly`() {
        val result = calculateAmount(amountMinor = 0L)
        assertEquals(0L, result)
    }

    @Test
    fun `negative amount throws exception`() {
        assertThrows<IllegalArgumentException> {
            calculateAmount(amountMinor = -100L)
        }
    }

    @Test
    fun `large amounts don't overflow`() {
        val result = calculateAmount(amountMinor = Long.MAX_VALUE / 2)
        assertTrue(result > 0)
    }

    // Edge cases, rounding, overflow tests
}
```

## Common Patterns

### Money Arithmetic
```kotlin
object MoneyCalculator {
    /**
     * Adds two money amounts.
     * @throws ArithmeticException if overflow occurs
     */
    fun add(a: Long, b: Long): Long {
        return Math.addExact(a, b)
    }

    /**
     * Multiplies amount by factor, rounds to nearest cent.
     */
    fun multiply(amountMinor: Long, factor: Double): Long {
        require(factor >= 0.0) { "Factor must be non-negative" }
        return (amountMinor * factor).roundToLong()
    }

    /**
     * Calculates percentage of amount.
     */
    fun percentage(amountMinor: Long, percent: Int): Long {
        require(percent in 0..100) { "Percent must be 0-100" }
        return (amountMinor * percent) / 100
    }
}
```

### Split Validation
```kotlin
fun validateSplits(
    totalAmountMinor: Long,
    splits: List<Split>
): ValidationResult {
    val splitSum = splits.sumOf { it.amountMinor }

    return when {
        splitSum != totalAmountMinor ->
            ValidationResult.Error("Splits must sum to total amount")
        splits.any { it.amountMinor == 0L } ->
            ValidationResult.Error("Split amounts cannot be zero")
        else ->
            ValidationResult.Success
    }
}
```

### Transfer Invariants
```kotlin
fun validateTransfer(
    fromTransaction: Transaction,
    toTransaction: Transaction
): ValidationResult {
    return when {
        fromTransaction.amountMinor >= 0 ->
            ValidationResult.Error("From amount must be negative")
        toTransaction.amountMinor <= 0 ->
            ValidationResult.Error("To amount must be positive")
        Math.addExact(fromTransaction.amountMinor, toTransaction.amountMinor) != 0L ->
            ValidationResult.Error("Transfer must net to zero")
        fromTransaction.transferId != toTransaction.transferId ->
            ValidationResult.Error("Transfer IDs must match")
        else ->
            ValidationResult.Success
    }
}
```

## Test Requirements

For each domain function, generate tests for:
1. Happy path (typical valid inputs)
2. Edge cases (zero, boundaries, max values)
3. Invalid inputs (negative where not allowed, nulls)
4. Overflow scenarios
5. Rounding behavior (if applicable)
6. Business rule violations

## Workflow

1. Understand the business rule
2. Design the function signature (Long for amounts)
3. Implement integer-safe calculation
4. Add precondition checks (`require`)
5. Handle overflow with `Math.addExact`, `Math.multiplyExact`
6. Generate comprehensive unit tests
7. Document assumptions and constraints
8. Verify no Float/Double in money logic

## Anti-Patterns to Reject

```kotlin
// ❌ NEVER DO THIS
fun calculateAmount(amount: Double): Double {
    return amount * 0.15
}

// ❌ NEVER DO THIS
val total = transaction.amount * 1.15f

// ✅ CORRECT
fun calculateAmount(amountMinor: Long, percentBasisPoints: Int): Long {
    return (amountMinor * percentBasisPoints) / 10000
}
```
