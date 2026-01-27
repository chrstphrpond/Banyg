# Money Type Implementation

Comprehensive Money type implementation using Long minor units (centavos) with overflow-safe arithmetic, sign conventions, and UI formatting.

---

## 📦 Implementation Summary

### **Core Domain** (`core/domain`)

#### 1. **Currency.kt** - Currency Value Object
**Location**: `core/domain/src/main/kotlin/com/banyg/domain/model/Currency.kt`

**Features**:
- Immutable currency representation (ISO 4217)
- Pre-defined currencies: PHP (₱), USD ($), EUR (€), JPY (¥)
- Minor units per major configuration (100 for most, 1 for JPY)
- Validation: 3-character code, positive minor units

```kotlin
val php = Currency.PHP  // ₱ - Philippine Peso
val usd = Currency.USD  // $ - US Dollar
```

---

#### 2. **Money.kt** - Money Value Object
**Location**: `core/domain/src/main/kotlin/com/banyg/domain/model/Money.kt`

**Features**:
- ✅ Uses `Long` for minor units (NEVER Float/Double)
- ✅ Sign conventions enforced:
  - Negative = Expense/outflow
  - Positive = Income/inflow
  - Zero = No change
- ✅ Comparable interface for sorting
- ✅ Absolute value and negation operations
- ✅ Currency validation (cannot mix currencies)

**Properties**:
```kotlin
data class Money(
    val minorUnits: Long,      // ALWAYS Long
    val currency: Currency
)

val isExpense: Boolean         // true if negative
val isIncome: Boolean          // true if positive
val isZero: Boolean            // true if zero
```

**Factory Methods**:
```kotlin
Money.zero(currency)                    // Zero amount
Money.fromMajor(123.45, Currency.PHP)   // From decimal
Money.fromMajor(100L, Currency.PHP)     // From whole number
```

**Examples**:
```kotlin
val expense = Money(-5000, Currency.PHP)  // -₱50.00 (expense)
val income = Money(10000, Currency.USD)   // $100.00 (income)
val zero = Money.zero(Currency.PHP)       // ₱0.00
```

---

#### 3. **MoneyCalculator.kt** - Overflow-Safe Arithmetic
**Location**: `core/domain/src/main/kotlin/com/banyg/domain/calculator/MoneyCalculator.kt`

**All operations use `Math.addExact`, `Math.multiplyExact`, etc. for overflow protection.**

##### Basic Arithmetic
```kotlin
MoneyCalculator.add(a, b)           // Throws on overflow
MoneyCalculator.subtract(a, b)      // Throws on overflow
MoneyCalculator.multiply(m, 3L)     // Integer multiply
MoneyCalculator.multiply(m, 1.5)    // Decimal multiply with rounding
MoneyCalculator.divide(m, 2L)       // Integer divide (truncates)
MoneyCalculator.percentage(m, 15)   // 15% of amount
```

##### Splitting
```kotlin
// Equal split (remainder to first items)
MoneyCalculator.split(Money(100, php), 3)
// Result: [34, 33, 33] centavos

// Percentage split (last item gets remainder)
MoneyCalculator.splitByPercentages(
    Money(10000, php),
    listOf(50, 30, 20)
)
// Result: [5000, 3000, 2000] = ₱50, ₱30, ₱20
```

##### Validation
```kotlin
// Validate transfer nets to zero
MoneyCalculator.validateTransfer(
    from = Money(-10000, php),  // Must be negative
    to = Money(10000, php)      // Must be positive
)

// Validate splits sum to total
MoneyCalculator.validateSplits(total, splits)
```

##### Aggregation
```kotlin
MoneyCalculator.sum(listOf(m1, m2, m3))  // Sum list
MoneyCalculator.roundToMajor(money)       // Round to ₱X.00
```

---

### **UI Layer** (`core/ui`)

#### 4. **MoneyFormatter.kt** - Formatting at UI Edge ONLY
**Location**: `core/ui/src/main/kotlin/com/banyg/ui/format/MoneyFormatter.kt`

**CRITICAL**: Only use in `core/ui` module. Domain and data layers NEVER use formatting.

##### Extension Functions
```kotlin
// Standard formatting
money.format()                        // "₱123.45"
money.format(showSign = false)        // "₱123.45" (no negative sign)
money.formatWithoutSymbol()           // "123.45"

// Compact formatting (for charts)
Money(1_234_567, php).formatCompact() // "₱1.2M"
Money(5_000, php).formatCompact()     // "₱50.00"

// Explicit sign (for income/expense)
income.formatWithExplicitSign()       // "+₱100.00"
expense.formatWithExplicitSign()      // "-₱50.00"

// Percentage of total
part.formatAsPercentageOf(total)      // "25.0%"

// Parse from user input
"123.45".parseToMoney(Currency.PHP)   // Money(12345, PHP)
"₱1,234.56".parseToMoney(Currency.PHP) // Money(123456, PHP)
```

##### Direct Long Formatting
```kotlin
12345L.formatAsMinorUnits(Currency.PHP) // "₱123.45"
```

---

## 🧪 Test Coverage

### **MoneyTest.kt** - Money Domain Model Tests
**Location**: `core/domain/src/test/kotlin/com/banyg/domain/model/MoneyTest.kt`

**Test Categories**:
- ✅ Sign conventions (expense/income/zero)
- ✅ Comparisons (same currency only)
- ✅ Absolute value operations
- ✅ Negation (unary minus)
- ✅ Factory methods (fromMajor)
- ✅ Equality and hash code
- ✅ Edge cases (Long.MAX_VALUE, Long.MIN_VALUE)
- ✅ Currency validation

**Total Tests**: 24

---

### **MoneyCalculatorTest.kt** - Arithmetic Tests
**Location**: `core/domain/src/test/kotlin/com/banyg/domain/calculator/MoneyCalculatorTest.kt`

**Test Categories**:
1. **Addition** (6 tests)
   - Positive amounts
   - Negative amounts
   - Mixed signs
   - Zero handling
   - Overflow protection
   - Currency mismatch

2. **Subtraction** (3 tests)
   - Basic subtraction
   - Resulting in negative
   - Overflow protection

3. **Multiplication** (6 tests)
   - Integer factors
   - Decimal factors
   - Rounding behavior
   - Negative amounts
   - Zero
   - Overflow protection

4. **Division** (4 tests)
   - Integer division
   - Truncation behavior
   - Negative amounts
   - Zero divisor protection

5. **Percentage** (5 tests)
   - Basic calculation
   - Edge cases (0%, 100%)
   - Invalid values

6. **Splitting** (5 tests)
   - Equal splits
   - Remainder distribution
   - Single part
   - Negative amounts
   - Invalid parts

7. **Split by Percentages** (4 tests)
   - Correct division
   - Remainder handling
   - Sum validation
   - Negative percentages

8. **Transfer Validation** (5 tests)
   - Correct transfers
   - Sign validation
   - Net zero validation
   - Currency matching

9. **Split Validation** (4 tests)
   - Correct sums
   - Mismatched sums
   - Empty lists
   - Currency mismatch

10. **Rounding** (3 tests)
    - Round to major
    - Exact amounts
    - Negative amounts

11. **Sum** (6 tests)
    - List totals
    - Mixed signs
    - Single item
    - Empty list
    - Currency mismatch
    - Overflow protection

**Total Tests**: 51

---

## 🎯 Sign Conventions

### Expenses (Negative)
```kotlin
val coffeeExpense = Money(-450, Currency.PHP)  // -₱4.50
val groceries = Money(-15000, Currency.PHP)    // -₱150.00

assertTrue(coffeeExpense.isExpense)
assertFalse(coffeeExpense.isIncome)
```

### Income (Positive)
```kotlin
val salary = Money(500000, Currency.PHP)  // ₱5,000.00
val refund = Money(2500, Currency.PHP)    // ₱25.00

assertTrue(salary.isIncome)
assertFalse(salary.isExpense)
```

### Transfers (Must Net to Zero)
```kotlin
val fromAccount = Money(-10000, Currency.PHP)  // Outflow (expense)
val toAccount = Money(10000, Currency.PHP)     // Inflow (income)

MoneyCalculator.validateTransfer(fromAccount, toAccount)
// ✅ Valid: -10000 + 10000 = 0
```

---

## 🔒 Safety Guarantees

### 1. **No Float/Double**
```kotlin
// ❌ FORBIDDEN - Will not compile
val amount: Double = transaction.amount
val total = amount * 1.15

// ✅ CORRECT - Uses Long
val amount: Long = transaction.amountMinor
val total = MoneyCalculator.multiply(Money(amount, php), 1.15)
```

### 2. **Overflow Protection**
```kotlin
val max = Money(Long.MAX_VALUE, Currency.PHP)
val one = Money(1, Currency.PHP)

// Throws ArithmeticException (prevents silent overflow)
MoneyCalculator.add(max, one)
```

### 3. **Currency Validation**
```kotlin
val php = Money(10000, Currency.PHP)
val usd = Money(10000, Currency.USD)

// Throws IllegalArgumentException
MoneyCalculator.add(php, usd)  // Cannot mix currencies
```

### 4. **Split Consistency**
```kotlin
val total = Money(100, Currency.PHP)
val splits = MoneyCalculator.split(total, 3)

// Always sums back to original
val sum = splits.sumOf { it.minorUnits }
assertEquals(100L, sum)  // [34, 33, 33] = 100
```

---

## 📐 Architecture Rules

### ✅ **Domain Layer** (`core/domain`)
- Pure Kotlin, no Android imports
- All money amounts as `Long`
- No formatting logic
- Overflow-safe arithmetic only

### ✅ **Data Layer** (`core/data`)
- Room entities use `amountMinor: Long`
- Column name: `amount_minor` (NOT `amount`)
- No Float/Double columns

### ✅ **UI Layer** (`core/ui`)
- Formatting ONLY at UI edge
- Extension functions for display
- Parsing for user input
- Color coding by sign

---

## 🚀 Usage Examples

### Creating Money
```kotlin
// From minor units (preferred in domain)
val money = Money(12345, Currency.PHP)  // ₱123.45

// From major units (for user input)
val money = Money.fromMajor(123.45, Currency.PHP)

// Zero
val zero = Money.zero(Currency.PHP)
```

### Arithmetic
```kotlin
val balance = Money(50000, Currency.PHP)  // ₱500
val expense = Money(-15000, Currency.PHP) // -₱150

val newBalance = MoneyCalculator.add(balance, expense)
// Result: ₱350.00
```

### Formatting (UI Only)
```kotlin
val money = Money(12345, Currency.PHP)

// In Composable
Text(text = money.format())  // "₱123.45"
Text(
    text = money.format(),
    color = if (money.isExpense) {
        MaterialTheme.colorScheme.error
    } else {
        Color.Green
    }
)
```

### Transaction Display
```kotlin
@Composable
fun TransactionItem(transaction: Transaction) {
    val money = Money(transaction.amountMinor, Currency.PHP)

    Row {
        Text(transaction.merchant)
        Spacer(Modifier.weight(1f))
        Text(
            text = money.format(),
            color = if (money.isExpense) {
                MaterialTheme.colorScheme.error
            } else {
                SuccessGreen
            }
        )
    }
}
```

---

## 🔍 Verification Commands

### Run Tests
```bash
# All domain tests
./gradlew :core:domain:test

# Specific test classes
./gradlew :core:domain:test --tests "MoneyTest"
./gradlew :core:domain:test --tests "MoneyCalculatorTest"

# Single test
./gradlew :core:domain:test --tests "*add positive amounts*"
```

### Verify Money Safety (Pre-commit)
```bash
# Check for Float/Double in money context
git grep -n "amount.*Double\|amount.*Float" -- "*.kt" | grep -v "Test.kt"

# Check for wrong column names in Room entities
git grep -n "@ColumnInfo.*amount" -- "*Entity.kt" | grep -v "amount_minor"
```

---

## 📊 Implementation Stats

| Component | Lines of Code | Test Cases |
|-----------|---------------|------------|
| Currency.kt | 73 | Included in MoneyTest |
| Money.kt | 89 | 24 |
| MoneyCalculator.kt | 242 | 51 |
| MoneyFormatter.kt | 180 | Manual testing |
| **Total** | **584** | **75+** |

---

## ✅ Completion Checklist

- [x] Money type with Long minor units
- [x] Currency value object with validation
- [x] Sign conventions (expense/income/zero)
- [x] Overflow-safe arithmetic operations
- [x] Splitting with remainder distribution
- [x] Percentage calculations
- [x] Transfer validation (nets to zero)
- [x] Split validation (sums match)
- [x] Rounding behavior
- [x] Formatting at UI edge only
- [x] Compact formatting for charts
- [x] Explicit sign formatting
- [x] Input parsing (UI only)
- [x] Comprehensive unit tests (75+ tests)
- [x] Edge case handling (overflow, max/min Long)
- [x] Currency mismatch protection
- [x] Documentation and examples

---

## 🎓 Key Learnings

1. **Long is Non-Negotiable**: Float/Double cause rounding errors that compound over time
2. **Overflow Protection**: Math.addExact catches overflow immediately instead of silent wraparound
3. **Sign Conventions**: Consistent negative=expense, positive=income simplifies logic
4. **Formatting Isolation**: Keep formatting at UI edge prevents domain contamination
5. **Split Consistency**: Remainder distribution ensures sum always equals original
6. **Transfer Validation**: Enforcing net-zero prevents money from appearing/disappearing

---

**Generated**: 2026-01-27
**For**: Banyg - Local-first Android Finance App
**Status**: ✅ Complete and Tested
