# Banyg Data Model

**Schema Version:** 1
**Last Updated:** 2026-01-26

This document defines the Room database schema for Banyg. All money amounts use `INTEGER` type representing minor units (centavos). See MONEY_RULES.md for details.

## Core Principles

1. **Money as INTEGER**: All amounts stored as `Long` (minor units/centavos)
2. **Timestamps as INTEGER**: Unix epoch milliseconds
3. **TEXT for IDs**: UUIDs as strings
4. **Soft Delete**: Use `deleted_at` timestamp, not hard deletes
5. **Explicit Indices**: Index all foreign keys and query columns

## Tables

### accounts

Represents user's financial accounts (checking, savings, credit cards, cash).

**Columns:**
- `id` (TEXT, PRIMARY KEY) - UUID
- `name` (TEXT, NOT NULL) - User-facing account name (e.g., "Chase Checking")
- `type` (TEXT, NOT NULL) - Account type: "CHECKING", "SAVINGS", "CREDIT_CARD", "CASH"
- `currency` (TEXT, NOT NULL) - ISO 4217 currency code (e.g., "USD", "EUR")
- `opening_balance_minor` (INTEGER, NOT NULL) - Initial balance in minor units
- `archived` (INTEGER, NOT NULL, DEFAULT 0) - 0 = active, 1 = archived
- `created_at` (INTEGER, NOT NULL) - Unix timestamp (millis)
- `updated_at` (INTEGER, NOT NULL) - Unix timestamp (millis)

**Indices:** None (small table, primary key auto-indexed)

**Notes:**
- Current balance calculated by: `opening_balance_minor + sum(transactions.amount_minor)`
- Archived accounts hidden from UI but data preserved

---

### transactions

Represents individual financial transactions.

**Columns:**
- `id` (TEXT, PRIMARY KEY) - UUID
- `account_id` (TEXT, NOT NULL, FOREIGN KEY → accounts.id) - Account this transaction belongs to
- `date` (INTEGER, NOT NULL) - Transaction date as Unix timestamp (millis) or YYYYMMDD
- `amount_minor` (INTEGER, NOT NULL) - Amount in minor units (negative for expenses, positive for income)
- `merchant` (TEXT) - Merchant name (nullable)
- `memo` (TEXT) - User notes (nullable)
- `category_id` (TEXT, FOREIGN KEY → categories.id) - Category (nullable for uncategorized)
- `status` (TEXT, NOT NULL) - "PENDING" or "CLEARED"
- `cleared_at` (INTEGER) - Timestamp when marked cleared (nullable)
- `transfer_id` (TEXT) - Shared ID for linked transfer transactions (nullable)
- `created_at` (INTEGER, NOT NULL) - Unix timestamp (millis)
- `updated_at` (INTEGER, NOT NULL) - Unix timestamp (millis)

**Indices:**
- `account_id` - For queries by account
- `date` - For date range queries
- `category_id` - For category filtering
- `status` - For inbox queries (PENDING transactions)
- `transfer_id` - For finding linked transfers

**Foreign Keys:**
- `account_id` → `accounts.id` ON DELETE CASCADE
- `category_id` → `categories.id` ON DELETE SET NULL

**Notes:**
- Expense transactions: `amount_minor < 0`
- Income transactions: `amount_minor > 0`
- Transfers: Two linked transactions with same `transfer_id`, must sum to zero
- Split transactions: Main transaction + entries in `splits` table

---

### splits

Represents split transactions (one transaction divided across multiple categories).

**Columns:**
- `id` (TEXT, PRIMARY KEY) - UUID
- `transaction_id` (TEXT, NOT NULL, FOREIGN KEY → transactions.id) - Parent transaction
- `line_id` (INTEGER, NOT NULL) - Line number within transaction (0-indexed)
- `category_id` (TEXT, NOT NULL, FOREIGN KEY → categories.id) - Category for this split
- `amount_minor` (INTEGER, NOT NULL) - Amount in minor units for this split

**Indices:**
- `transaction_id` - For loading splits with transaction
- `category_id` - For category queries

**Foreign Keys:**
- `transaction_id` → `transactions.id` ON DELETE CASCADE
- `category_id` → `categories.id` ON DELETE RESTRICT (prevent deletion if split exists)

**Constraints:**
- Validation: `sum(splits.amount_minor WHERE transaction_id = X) == transaction.amount_minor`
- If a transaction has splits, ignore `transaction.category_id` (should be null)

**Notes:**
- `line_id` maintains order of splits within a transaction
- Each split must have a category (category_id NOT NULL)

---

### categories

Represents spending/income categories.

**Columns:**
- `id` (TEXT, PRIMARY KEY) - UUID
- `group_id` (TEXT, FOREIGN KEY → categories.id) - Parent category for grouping (nullable)
- `name` (TEXT, NOT NULL) - Category name (e.g., "Groceries", "Rent")
- `icon` (TEXT) - Icon identifier (nullable, for future use)
- `hidden` (INTEGER, NOT NULL, DEFAULT 0) - 0 = visible, 1 = hidden
- `created_at` (INTEGER, NOT NULL) - Unix timestamp (millis)

**Indices:**
- `group_id` - For grouped category queries

**Foreign Keys:**
- `group_id` → `categories.id` ON DELETE SET NULL (self-referencing)

**Notes:**
- System categories (pre-installed) vs user categories
- Hidden categories don't appear in UI but preserve historical data
- Group hierarchy: One level only (category can have parent, but parent can't have parent)

---

### budget_periods

Represents budget allocations per category per month.

**Columns:**
- `id` (TEXT, PRIMARY KEY) - UUID
- `month_key` (TEXT, NOT NULL) - Format: "YYYY-MM" (e.g., "2026-01")
- `category_id` (TEXT, NOT NULL, FOREIGN KEY → categories.id) - Budgeted category
- `allocated_minor` (INTEGER, NOT NULL) - Budgeted amount in minor units
- `activity_minor` (INTEGER, NOT NULL, DEFAULT 0) - Calculated: sum of transactions (cached)
- `created_at` (INTEGER, NOT NULL) - Unix timestamp (millis)
- `updated_at` (INTEGER, NOT NULL) - Unix timestamp (millis)

**Indices:**
- `month_key` - For queries by month
- `category_id` - For queries by category

**Unique Constraint:**
- `(month_key, category_id)` - One budget per category per month

**Foreign Keys:**
- `category_id` → `categories.id` ON DELETE CASCADE

**Calculated Fields:**
- `available_minor = allocated_minor + activity_minor` (computed at read time)
- `activity_minor` updated via trigger or manual recalculation

**Notes:**
- `activity_minor` is sum of all cleared transactions for that category in that month
- Negative activity (expenses) brings down available amount
- See BUDGET_MATH.md for calculation details

---

## Foreign Key Cascade Rules

| Table | Column | References | On Delete |
|-------|--------|------------|-----------|
| transactions | account_id | accounts.id | CASCADE |
| transactions | category_id | categories.id | SET NULL |
| splits | transaction_id | transactions.id | CASCADE |
| splits | category_id | categories.id | RESTRICT |
| categories | group_id | categories.id | SET NULL |
| budget_periods | category_id | categories.id | CASCADE |

## Migration Strategy

When schema changes:
1. Create migration file: `Migration_X_to_Y.kt`
2. Write migration test: `Migration_X_to_Y_Test.kt`
3. Update schema version in `BanygDatabase`
4. Export schema JSON: `./gradlew generateDebugSchema`
5. Update this document (DATA_MODEL.md)

See `room-migration` skill in `.claude/skills/` for templates.

## Open Decisions

TODO: Decide before implementing:

### Attachments Table
**Question**: Support receipt attachments?
```sql
CREATE TABLE attachments (
    id TEXT PRIMARY KEY,
    transaction_id TEXT NOT NULL FOREIGN KEY,
    file_path TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    created_at INTEGER NOT NULL
)
```

### Tags Table
**Question**: Support tags for flexible categorization?
```sql
CREATE TABLE tags (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    color TEXT
);

CREATE TABLE transaction_tags (
    transaction_id TEXT NOT NULL FOREIGN KEY,
    tag_id TEXT NOT NULL FOREIGN KEY,
    PRIMARY KEY (transaction_id, tag_id)
);
```

### Payee Normalization
**Question**: Normalize merchant names into separate table?
```sql
CREATE TABLE payees (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
-- Then transactions.merchant becomes transactions.payee_id
```
**Pros**: Better merchant matching, less duplicate strings
**Cons**: More complex queries, harder to edit inline

### Recurring Transactions
**Question**: Add recurring transaction support?
```sql
CREATE TABLE recurring_transactions (
    id TEXT PRIMARY KEY,
    frequency TEXT NOT NULL, -- "WEEKLY", "MONTHLY", etc.
    next_date INTEGER NOT NULL,
    template_data TEXT NOT NULL -- JSON of transaction fields
);
```

### Date Storage Format
**Question**: Store date as INTEGER (Unix millis) or TEXT (ISO8601)?
- **Current**: INTEGER (Unix millis)
- **Alternative**: TEXT "YYYY-MM-DD" for easier SQL queries
- **Alternative**: INTEGER YYYYMMDD format (e.g., 20260126)

## Verification Queries

Check data integrity:

```sql
-- Verify transfers net to zero
SELECT transfer_id, SUM(amount_minor) as net
FROM transactions
WHERE transfer_id IS NOT NULL
GROUP BY transfer_id
HAVING net != 0;

-- Verify splits sum to transaction amount
SELECT t.id, t.amount_minor, SUM(s.amount_minor) as split_sum
FROM transactions t
JOIN splits s ON s.transaction_id = t.id
GROUP BY t.id
HAVING t.amount_minor != split_sum;

-- Find orphaned splits (transaction deleted)
SELECT * FROM splits
WHERE transaction_id NOT IN (SELECT id FROM transactions);
```
