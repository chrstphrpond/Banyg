# Budget Calculation Rules

This document defines the mathematical formulas and business logic for Banyg's envelope-style budgeting system.

## Budget Model

Banyg uses **envelope-style budgeting** (also known as zero-based budgeting). Each category is like an envelope where you allocate money at the start of the month, then track spending against that allocation.

## Core Formula

```
available = allocated + activity
```

Where:
- **`allocated`**: Amount budgeted for this category this month (always `>= 0`)
- **`activity`**: Sum of all transactions in this category this month
  - Expenses are negative → reduce available
  - Income is positive → increase available
- **`available`**: Remaining budget
  - Positive = money left to spend
  - Negative = overspent
  - Zero = perfectly on budget

## Data Model

From `budget_periods` table (see DATA_MODEL.md):

```kotlin
data class BudgetPeriod(
    val id: String,
    val monthKey: String,           // "YYYY-MM" format
    val categoryId: String,
    val allocatedMinor: Long,       // Amount budgeted (stored)
    val activityMinor: Long,        // Sum of transactions (cached, recalculated)
) {
    // Computed property
    val availableMinor: Long
        get() = Math.addExact(allocatedMinor, activityMinor)
}
```

## Activity Calculation

Activity is the sum of all **cleared** transaction amounts for a category in a given month:

```kotlin
fun calculateActivity(
    categoryId: String,
    monthKey: String,
    transactions: List<Transaction>
): Long {
    return transactions
        .filter { it.categoryId == categoryId }
        .filter { it.monthKey() == monthKey }
        .filter { it.status == TransactionStatus.CLEARED }
        .filter { it.transferId == null }  // Exclude transfers
        .sumOf { it.amountMinor }
}
```

**Inclusions:**
- Expenses (negative amounts)
- Income (positive amounts)
- Split transactions (each split contributes to its category)
- Cleared transactions only (not pending)

**Exclusions:**
- Transfers (they're account movements, not spending)
- Pending transactions (not yet committed)
- Transactions with `transferId != null`

## Examples

All amounts shown as both dollars (for readability) and centavos (actual stored value).

### Example 1: Simple Case

**Category**: Groceries
**Month**: 2026-01

| Field | Value (USD) | Value (centavos) |
|-------|-------------|------------------|
| Allocated | $500.00 | `50000` |
| Activity | -$320.00 | `-32000` |
| Available | **$180.00** | **`18000`** |

**Transactions:**
- Jan 5: -$120.00 (Whole Foods)
- Jan 12: -$80.00 (Trader Joe's)
- Jan 20: -$120.00 (Safeway)

```kotlin
allocated = 50000L
activity = -12000L + -8000L + -12000L = -32000L
available = 50000L + (-32000L) = 18000L  // $180 left to spend
```

### Example 2: Overspent

**Category**: Dining Out
**Month**: 2026-01

| Field | Value (USD) | Value (centavos) |
|-------|-------------|------------------|
| Allocated | $200.00 | `20000` |
| Activity | -$250.00 | `-25000` |
| Available | **-$50.00** | **`-5000`** |

**Status**: Overspent by $50 (show in red)

```kotlin
allocated = 20000L
activity = -10000L + -8000L + -7000L + -10000L = -25000L
available = 20000L + (-25000L) = -5000L  // $50 over budget
```

### Example 3: Income Category

**Category**: Salary
**Month**: 2026-01

| Field | Value (USD) | Value (centavos) |
|-------|-------------|------------------|
| Allocated | $0.00 | `0` |
| Activity | +$3,000.00 | `300000` |
| Available | **$3,000.00** | **`300000`** |

**Note**: Income categories typically have $0 allocated. Activity is positive (income received).

```kotlin
allocated = 0L
activity = 300000L  // Paycheck
available = 0L + 300000L = 300000L
```

### Example 4: Mixed Income/Expense Category

**Category**: Freelance (user does side gigs and has business expenses)
**Month**: 2026-01

| Field | Value (USD) | Value (centavos) |
|-------|-------------|------------------|
| Allocated | $0.00 | `0` |
| Activity | +$1,200.00 | `120000` |
| Available | **$1,200.00** | **`120000`** |

**Transactions:**
- Jan 10: +$1,500.00 (Client payment - income)
- Jan 15: -$300.00 (Software subscription - expense)

```kotlin
allocated = 0L
activity = 150000L + (-30000L) = 120000L
available = 0L + 120000L = 120000L  // Net income
```

## Split Transaction Handling

When a transaction is split across multiple categories, **each split contributes to its respective category's activity**.

**Transaction**: -$150.00 at Target (split: groceries + household)

**Splits:**
- $100.00 → Groceries category
- $50.00 → Household category

**Impact on budgets:**

| Category | Allocated | Activity Before | Split Amount | Activity After | Available |
|----------|-----------|----------------|--------------|----------------|-----------|
| Groceries | $500 (50000) | -$200 (-20000) | -$100 (-10000) | -$300 (-30000) | $200 (20000) |
| Household | $200 (20000) | -$80 (-8000) | -$50 (-5000) | -$130 (-13000) | $70 (7000) |

**Validation:**
```kotlin
// Splits must sum to transaction total
require(splits.sumOf { it.amountMinor } == transaction.amountMinor) {
    "Split sum ${splits.sumOf { it.amountMinor }} != transaction amount ${transaction.amountMinor}"
}
```

## Transfer Handling

Transfers are **NOT included in budget activity** because they're account movements, not spending or income.

**Example**: Transfer $500 from Checking to Savings

**Transactions created:**
1. Checking: -$500.00 (`amountMinor = -50000`, `transferId = "abc123"`)
2. Savings: +$500.00 (`amountMinor = 50000`, `transferId = "abc123"`)

**Budget impact**: NONE (both transactions excluded from activity calculation)

```kotlin
// Exclude transfers when calculating activity
transactions.filter { it.transferId == null }
```

**Validation:**
```kotlin
// Transfers must net to zero
val transfer1 = Transaction(amountMinor = -50000, transferId = "abc123")
val transfer2 = Transaction(amountMinor = 50000, transferId = "abc123")
require(transfer1.amountMinor + transfer2.amountMinor == 0L)
```

## Month Rollover Strategy

TODO: **Decide before implementing**

When a new month starts, what happens to unused budget amounts?

### Option A: Reset to Zero (Strict)

Each month is independent. Unused budget doesn't carry over.

**Pros:**
- Simple to implement
- Forces spending discipline
- Clear month-to-month boundaries

**Cons:**
- Punishes under-spending
- Doesn't encourage saving within categories

**Implementation:**
```kotlin
// Each month starts fresh
fun createNewMonthBudget(categoryId: String, monthKey: String, allocatedMinor: Long) {
    BudgetPeriod(
        categoryId = categoryId,
        monthKey = monthKey,
        allocatedMinor = allocatedMinor,
        activityMinor = 0L  // Always starts at zero
    )
}
```

### Option B: Carry Over Available Balance (Flexible)

Positive `available` from previous month adds to next month's allocation.

**Pros:**
- Rewards under-spending
- Encourages saving for big purchases
- More flexible budgeting

**Cons:**
- More complex to implement
- Harder to compare month-to-month
- Can accumulate large balances

**Implementation:**
```kotlin
fun createNewMonthBudget(
    categoryId: String,
    monthKey: String,
    allocatedMinor: Long,
    previousPeriod: BudgetPeriod?
): BudgetPeriod {
    val carryOver = previousPeriod?.availableMinor?.coerceAtLeast(0L) ?: 0L
    return BudgetPeriod(
        categoryId = categoryId,
        monthKey = monthKey,
        allocatedMinor = Math.addExact(allocatedMinor, carryOver),
        activityMinor = 0L
    )
}
```

### Option C: Per-Category Setting (Most Complex)

User chooses rollover behavior per category.

**Example:**
- "Groceries" → Reset to zero (strict spending category)
- "Vacation" → Carry over (saving goal category)

**Implementation:**
```kotlin
data class Category(
    val id: String,
    val name: String,
    val rolloverStrategy: RolloverStrategy  // RESET or CARRY_OVER
)

enum class RolloverStrategy {
    RESET,      // Option A
    CARRY_OVER  // Option B
}
```

## Pending Transactions

**Pending transactions are NOT included in activity calculation.**

This prevents double-counting when users:
1. Capture transaction quickly (pending)
2. Review and clear it later (cleared)

Only **cleared** transactions affect budget.

```kotlin
transactions.filter { it.status == TransactionStatus.CLEARED }
```

**UI consideration**: Optionally show pending activity as separate "uncommitted" amount.

## Recalculation Strategy

Activity can be:
1. **Cached** in `budget_periods.activity_minor` (fast, requires updates)
2. **Calculated on-demand** from transactions (slower, always accurate)

**Recommended**: Cache with triggers/updates

```kotlin
// Update activity when transaction changes
suspend fun updateBudgetActivity(
    categoryId: String,
    monthKey: String,
    transactionDao: TransactionDao
) {
    val activity = transactionDao.sumActivity(categoryId, monthKey)
    budgetDao.updateActivity(categoryId, monthKey, activity)
}
```

**Triggers** (if Room supports):
- After INSERT transaction → update budget activity
- After UPDATE transaction.category_id → update old and new budget
- After DELETE transaction → update budget activity

## Edge Cases

### Refunds and Returns

TODO: **Decide before implementing**

**Question**: How to handle refunds?

**Scenario**: User budgets $500 for groceries, spends $400, then gets $50 refund.

**Options:**
1. **Positive transaction in same category**: Activity = -400 + 50 = -350
2. **Void original transaction**: Remove the -50 transaction, activity = -350
3. **Separate "Refund" category**: Activity stays -400, refund doesn't affect budget

**Recommended**: Option 1 (positive transaction in same category)

### Budget Templates

TODO: **Consider for future**

**Question**: Pre-fill budget allocations for new users?

**Options:**
- Default template (e.g., 50/30/20 rule)
- Copy previous month
- Start from zero (current MVP approach)

### Budget vs Goals

TODO: **Consider for future**

**Question**: Are savings goals different from budgets?

**Budget**: Money allocated for spending
**Goal**: Money set aside for future use (not spent this month)

**Current**: Both use same `budget_periods` table
**Future**: May need separate `goals` table with target amount and deadline

## Validation Rules

```kotlin
// Allocated amount must be non-negative
require(allocatedMinor >= 0L) { "Allocated amount cannot be negative" }

// Activity can be any value (expenses negative, income positive)
// No validation needed for activity

// Available is calculated, no validation needed
val availableMinor = Math.addExact(allocatedMinor, activityMinor)
```

## Testing Requirements

Budget calculation tests must cover:

```kotlin
class BudgetCalculatorTest {

    @Test
    fun `calculate available with positive activity`() {
        val budget = BudgetPeriod(allocated = 50000L, activity = 18000L)
        assertEquals(68000L, budget.available)
    }

    @Test
    fun `calculate available with negative activity`() {
        val budget = BudgetPeriod(allocated = 50000L, activity = -32000L)
        assertEquals(18000L, budget.available)
    }

    @Test
    fun `calculate available when overspent`() {
        val budget = BudgetPeriod(allocated = 20000L, activity = -25000L)
        assertEquals(-5000L, budget.available)
    }

    @Test
    fun `activity excludes transfers`() {
        val transactions = listOf(
            Transaction(amount = -10000L, transferId = null),    // Include
            Transaction(amount = -5000L, transferId = "abc123"), // Exclude
            Transaction(amount = 5000L, transferId = "abc123")   // Exclude
        )
        assertEquals(-10000L, calculateActivity(transactions))
    }

    @Test
    fun `activity includes split transactions`() {
        val transaction = Transaction(amount = -15000L)
        val splits = listOf(
            Split(categoryId = "groceries", amount = -10000L),
            Split(categoryId = "household", amount = -5000L)
        )
        // Groceries category activity should be -10000L
        // Household category activity should be -5000L
    }
}
```

## References

- See MONEY_RULES.md for arithmetic rules
- See DATA_MODEL.md for `budget_periods` table schema
- See PRD.md for budget feature requirements
