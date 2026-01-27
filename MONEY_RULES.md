# Money Handling Rules

**NEVER use Float or Double for money amounts in Banyg.**

This document defines strict rules for handling money in the codebase to ensure accuracy, prevent floating-point errors, and maintain data integrity.

## Core Type

**All money amounts MUST be stored as `Long` representing minor units (centavos/cents).**

```kotlin
// ✅ CORRECT - Domain model
data class Money(
    val minorUnits: Long,
    val currency: Currency
)

// ✅ CORRECT - Room entity
@Entity(tableName = "transactions")
data class TransactionEntity(
    @ColumnInfo(name = "amount_minor") val amountMinor: Long
)

// ✅ CORRECT - Function parameter
fun calculateTotal(amountMinor: Long): Long

// ❌ FORBIDDEN - Never do this
data class Money(val amount: Double)
val price: Float = 19.99f
@ColumnInfo(name = "amount") val amount: Double
```

## Sign Conventions

Sign indicates transaction direction:

| Type | Sign | Example |
|------|------|---------|
| **Expense** | Negative | `-1250` (-$12.50) |
| **Income** | Positive | `300000` ($3,000.00) |
| **Transfer Out** | Negative | `-10000` (-$100.00) |
| **Transfer In** | Positive | `10000` ($100.00) |

**Enforced invariant for transfers:**
```kotlin
// Two linked transactions must sum to zero
require(fromTransaction.amountMinor + toTransaction.amountMinor == 0L) {
    "Transfer must net to zero"
}
```

## Arithmetic Operations

### Addition / Subtraction

Use `Math.addExact()` and `Math.subtractExact()` to detect overflow:

```kotlin
// ✅ CORRECT - Detects overflow
fun add(a: Long, b: Long): Long = Math.addExact(a, b)

fun subtract(a: Long, b: Long): Long = Math.subtractExact(a, b)

// ❌ WRONG - Silent overflow
fun add(a: Long, b: Long): Long = a + b  // Can overflow without warning
```

### Multiplication

Use `Math.multiplyExact()` for integer multiplication:

```kotlin
// ✅ CORRECT - Safe multiplication
fun multiply(amountMinor: Long, quantity: Int): Long {
    return Math.multiplyExact(amountMinor, quantity.toLong())
}

// ❌ WRONG - Can overflow
fun multiply(amountMinor: Long, quantity: Int): Long = amountMinor * quantity
```

### Division and Percentages

Order matters for precision. Multiply before dividing:

```kotlin
// ✅ CORRECT - Calculate percentage
fun percentage(amountMinor: Long, percent: Int): Long {
    require(percent in 0..100) { "Percent must be 0-100" }
    return (amountMinor * percent) / 100
}

// ✅ CORRECT - Calculate tax (e.g., 8.5%)
fun calculateTax(amountMinor: Long, taxRateBasisPoints: Int): Long {
    // Basis points: 850 = 8.5%
    return (amountMinor * taxRateBasisPoints) / 10000
}

// ✅ CORRECT - Prorate amount
fun prorate(totalMinor: Long, numerator: Int, denominator: Int): Long {
    require(denominator > 0) { "Denominator must be positive" }
    return (totalMinor * numerator) / denominator
}

// ❌ WRONG - Division before multiplication loses precision
fun percentage(amountMinor: Long, percent: Int): Long {
    return (amountMinor / 100) * percent  // Loses fractional cents
}
```

### Rounding

Only use `Double` when absolutely necessary (e.g., external APIs). Convert back to `Long` immediately:

```kotlin
// ✅ CORRECT - Round to nearest cent
fun fromDouble(amount: Double): Long {
    return (amount * 100).roundToLong()
}

// ✅ CORRECT - Handle half-cents consistently
fun roundHalfUp(amountMinor: Long, divisor: Int): Long {
    val quotient = amountMinor / divisor
    val remainder = amountMinor % divisor
    return if (remainder >= divisor / 2) quotient + 1 else quotient
}

// ❌ WRONG - Storing or passing Double
fun calculate(amount: Double): Double = amount * 1.15
```

## Formatting (UI Layer Only)

**Money formatting is ONLY allowed in the `core/ui` module.**

```kotlin
// ✅ CORRECT - Extension function in core/ui/util/MoneyFormat.kt
fun Long.formatAsCurrency(currency: Currency): String {
    val formatter = NumberFormat.getCurrencyInstance()
    formatter.currency = currency
    return formatter.format(this / 100.0)
}

// Usage in composable
@Composable
fun TransactionRow(amountMinor: Long) {
    Text(text = amountMinor.formatAsCurrency(Currency.getInstance("USD")))
}

// ❌ FORBIDDEN - Formatting in domain or data layer
// Never do this in core/domain or core/data
class Transaction {
    fun getFormattedAmount(): String = "$${amountMinor / 100.0}"
}
```

## Validation Rules

### Split Transaction Validation

```kotlin
fun validateSplits(
    transactionAmountMinor: Long,
    splits: List<Split>
): Result<Unit> {
    val splitSum = splits.sumOf { it.amountMinor }

    return when {
        splits.isEmpty() ->
            Result.failure(IllegalArgumentException("Splits cannot be empty"))

        splits.any { it.amountMinor == 0L } ->
            Result.failure(IllegalArgumentException("Split amounts cannot be zero"))

        splitSum != transactionAmountMinor ->
            Result.failure(IllegalArgumentException(
                "Splits must sum to transaction amount. Expected: $transactionAmountMinor, Got: $splitSum"
            ))

        else -> Result.success(Unit)
    }
}
```

### Transfer Validation

```kotlin
fun validateTransfer(
    fromTransaction: Transaction,
    toTransaction: Transaction
): Result<Unit> {
    return when {
        fromTransaction.amountMinor >= 0 ->
            Result.failure(IllegalArgumentException("From amount must be negative"))

        toTransaction.amountMinor <= 0 ->
            Result.failure(IllegalArgumentException("To amount must be positive"))

        fromTransaction.transferId != toTransaction.transferId ->
            Result.failure(IllegalArgumentException("Transfer IDs must match"))

        Math.addExact(fromTransaction.amountMinor, toTransaction.amountMinor) != 0L ->
            Result.failure(IllegalArgumentException("Transfer must net to zero"))

        else -> Result.success(Unit)
    }
}
```

### Budget Activity Validation

```kotlin
fun validateBudgetActivity(
    allocatedMinor: Long,
    activityMinor: Long
): Result<Long> {
    require(allocatedMinor >= 0) { "Allocated amount must be non-negative" }

    val availableMinor = Math.addExact(allocatedMinor, activityMinor)
    return Result.success(availableMinor)
}
```

## Testing Requirements

Every money operation MUST have unit tests covering:

1. **Happy path** (typical valid values)
2. **Zero values**
3. **Negative values** (where applicable)
4. **Overflow scenarios**
5. **Rounding behavior**
6. **Edge cases** (Long.MIN_VALUE, Long.MAX_VALUE)

```kotlin
// Example test suite
class MoneyCalculatorTest {

    @Test
    fun `add positive amounts returns correct sum`() {
        assertEquals(300L, MoneyCalculator.add(100L, 200L))
    }

    @Test
    fun `add with zero returns original amount`() {
        assertEquals(100L, MoneyCalculator.add(100L, 0L))
    }

    @Test
    fun `add overflow throws ArithmeticException`() {
        assertThrows<ArithmeticException> {
            MoneyCalculator.add(Long.MAX_VALUE, 1L)
        }
    }

    @Test
    fun `percentage calculation rounds correctly`() {
        // 10% of $12.50 = $1.25
        assertEquals(125L, MoneyCalculator.percentage(1250L, 10))
    }

    @Test
    fun `percentage handles rounding half-cents`() {
        // 15% of $10.01 = $1.5015 → rounds to $1.50
        assertEquals(150L, MoneyCalculator.percentage(1001L, 15))
    }
}
```

## Enforcement Checks

Run these checks before committing code:

```bash
# Check for Float/Double used with money
git grep -n "amount.*Double\|amount.*Float" -- "*.kt" | grep -v "Test.kt"

# Check for missing amountMinor suffix in entities
git grep -n "@ColumnInfo.*amount[^_]" -- "*Entity.kt"

# Check for toDouble/toFloat on money values
git grep -n "amountMinor.*toDouble\|amountMinor.*toFloat" -- "*.kt"
```

If any of these return results, fix before committing.

## Open Decisions

TODO: Decide before implementing:

### Multi-Currency Conversion

**Question**: How to handle currency conversion?

**Options**:
1. **No conversion** (MVP) - Each account has one currency, no conversion
2. **Manual conversion** - User enters equivalent amount when transferring between currencies
3. **Live rates** - Fetch from API (requires internet, not local-first)

**Implications**: If supporting multiple currencies:
- Add `exchange_rate` field to transactions?
- Store base currency for reporting?
- How to aggregate cross-currency totals?

### Currencies with 3 Decimal Places

**Question**: Some currencies have 3 decimal places (e.g., KWD, BHD).

**Current approach**: Minor units = 2 decimal places (cents)

**Options**:
1. Store multiplier per currency (100 for USD, 1000 for KWD)
2. Use smallest common unit (1000 for all currencies)
3. Don't support 3-decimal currencies in MVP

### Negative Income

**Question**: Can income be negative (e.g., refund of income)?

**Current**: Income is positive (`amountMinor > 0`)

**Consideration**: If user gets a chargeback or income reversal, how to represent?

### Half-Cent Rounding

**Question**: When calculations produce half-cents, how to round?

**Current**: `roundToLong()` uses "round half to even" (banker's rounding)

**Alternatives**:
- Always round up (favorable to user)
- Always round down (conservative)
- Round half to even (statistical bias reduction)

## References

- See `ledger-rule` skill in `.claude/skills/` for money-safe code generation
- See DATA_MODEL.md for database column types
- See BUDGET_MATH.md for budget calculation examples
