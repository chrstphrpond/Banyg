# Banyg Database Schema (Version 1)

Complete database schema documentation for the Banyg finance app.

---

## Overview

**Database**: SQLite via Room
**Version**: 1 (Initial schema)
**Tables**: 4 (accounts, categories, transactions, splits)
**Critical Rule**: All money columns use `INTEGER` (Long), **NEVER** REAL/FLOAT

---

## Tables

### 1. accounts

Financial accounts (bank accounts, cash, credit cards, etc.)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | TEXT | PRIMARY KEY | Unique identifier (UUID) |
| `name` | TEXT | NOT NULL | Account name (e.g., "BDO Savings") |
| `type` | TEXT | NOT NULL | Account type enum |
| `currency_code` | TEXT | NOT NULL | ISO 4217 currency code |
| `opening_balance_minor` | INTEGER | NOT NULL | Opening balance in minor units (centavos) |
| `current_balance_minor` | INTEGER | NOT NULL | Current balance in minor units |
| `is_archived` | INTEGER | NOT NULL, DEFAULT 0 | 0 = active, 1 = archived |
| `created_at` | INTEGER | NOT NULL | Unix epoch milliseconds |
| `updated_at` | INTEGER | NOT NULL | Unix epoch milliseconds |

**Indices:**
- `index_accounts_is_archived` on (`is_archived`)
- `index_accounts_currency_code` on (`currency_code`)
- `index_accounts_type` on (`type`)

**Account Types:**
- CHECKING
- SAVINGS
- CREDIT_CARD
- CASH
- E_WALLET
- INVESTMENT
- LOAN
- OTHER

---

### 2. categories

Transaction categories for budgeting and reporting

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | TEXT | PRIMARY KEY | Unique identifier (UUID) |
| `name` | TEXT | NOT NULL | Category name (e.g., "Groceries") |
| `group_id` | TEXT | NULL | Optional group identifier |
| `group_name` | TEXT | NULL | Optional group name (e.g., "Food") |
| `is_hidden` | INTEGER | NOT NULL, DEFAULT 0 | 0 = visible, 1 = hidden |
| `icon` | TEXT | NULL | Optional icon identifier |
| `color` | TEXT | NULL | Optional color hex code |

**Indices:**
- `index_categories_group_id` on (`group_id`)
- `index_categories_is_hidden` on (`is_hidden`)

**Common Groups:**
- Income
- Food
- Transportation
- Shopping
- Bills & Utilities
- Entertainment
- Health
- Education
- Transfer
- Other

---

### 3. transactions

Financial transactions (expenses, income, transfers)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | TEXT | PRIMARY KEY | Unique identifier (UUID) |
| `account_id` | TEXT | NOT NULL, FK → accounts(id) | Account this belongs to |
| `date` | INTEGER | NOT NULL | Date as YYYYMMDD (e.g., 20240127) |
| `amount_minor` | INTEGER | NOT NULL | **Amount in minor units (centavos)** |
| `currency_code` | TEXT | NOT NULL | ISO 4217 currency code |
| `merchant` | TEXT | NOT NULL | Merchant or payee name |
| `memo` | TEXT | NULL | Optional note/description |
| `category_id` | TEXT | NULL, FK → categories(id) | Category for this transaction |
| `status` | TEXT | NOT NULL, DEFAULT 'PENDING' | Transaction status enum |
| `cleared_at` | INTEGER | NULL | Unix epoch millis when cleared |
| `transfer_id` | TEXT | NULL | Links transfer transactions |
| `created_at` | INTEGER | NOT NULL | Unix epoch milliseconds |
| `updated_at` | INTEGER | NOT NULL | Unix epoch milliseconds |

**Indices:**
- `index_transactions_account_id` on (`account_id`)
- `index_transactions_date` on (`date`)
- `index_transactions_category_id` on (`category_id`)
- `index_transactions_status` on (`status`)
- `index_transactions_transfer_id` on (`transfer_id`)
- `index_transactions_account_id_date` on (`account_id`, `date`) - Compound
- `index_transactions_account_id_status` on (`account_id`, `status`) - Compound

**Foreign Keys:**
- `account_id` → `accounts(id)` ON DELETE CASCADE
- `category_id` → `categories(id)` ON DELETE SET NULL

**Transaction Status:**
- PENDING - Uncleared transaction
- CLEARED - Cleared by bank
- RECONCILED - Reconciled with statement
- VOID - Cancelled transaction

**Sign Conventions:**
- Negative `amount_minor` = Expense/outflow
- Positive `amount_minor` = Income/inflow
- Transfers: Two transactions with same `transfer_id`, opposite signs

---

### 4. splits

Split transactions across multiple categories

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `transaction_id` | TEXT | PRIMARY KEY (composite), FK | Transaction this split belongs to |
| `line_id` | INTEGER | PRIMARY KEY (composite) | Line number (0, 1, 2, ...) |
| `category_id` | TEXT | NOT NULL, FK → categories(id) | Category for this split |
| `amount_minor` | INTEGER | NOT NULL | **Split amount in minor units** |
| `currency_code` | TEXT | NOT NULL | ISO 4217 currency code |
| `memo` | TEXT | NULL | Optional note for this split |

**Composite Primary Key:** (`transaction_id`, `line_id`)

**Indices:**
- `index_splits_transaction_id` on (`transaction_id`)
- `index_splits_category_id` on (`category_id`)

**Foreign Keys:**
- `transaction_id` → `transactions(id)` ON DELETE CASCADE
- `category_id` → `categories(id)` ON DELETE CASCADE

**Validation Rule:**
All splits for a transaction **MUST** sum to transaction's `amount_minor`.

---

## Money Safety Rules

### ✅ ALWAYS
- Use `INTEGER` column type for money (stores Long)
- Column name: `amount_minor` or `*_minor` (indicates minor units)
- Sign convention: negative = expense, positive = income
- Overflow protection in calculations (Math.addExact)

### ❌ NEVER
- Use `REAL` or `FLOAT` column types for money
- Name column just `amount` (must be `amount_minor`)
- Convert to Double/Float for calculations
- Store decimal values directly in database

---

## Date Storage

### Transactions Date
**Format**: YYYYMMDD as INTEGER
**Example**: 2024-01-27 → 20240127

**Advantages:**
- Efficient indexing for date range queries
- No timezone issues
- Easy to sort chronologically

### Timestamps
**Format**: Unix epoch milliseconds as INTEGER
**Used for**: `created_at`, `updated_at`, `cleared_at`

---

## Query Patterns

### Common Queries

**Transactions for account in date range:**
```sql
SELECT * FROM transactions
WHERE account_id = ?
  AND date BETWEEN ? AND ?
  AND status != 'VOID'
ORDER BY date DESC
```

**Sum by category in month:**
```sql
-- From transactions
SELECT SUM(amount_minor) FROM transactions
WHERE category_id = ?
  AND date BETWEEN 20240101 AND 20240131
  AND status != 'VOID'

-- From splits (for split transactions)
SELECT SUM(s.amount_minor)
FROM splits s
INNER JOIN transactions t ON s.transaction_id = t.id
WHERE s.category_id = ?
  AND t.date BETWEEN 20240101 AND 20240131
  AND t.status != 'VOID'
```

**Pending transactions:**
```sql
SELECT * FROM transactions
WHERE status = 'PENDING'
ORDER BY date DESC
```

---

## Migrations

### Version 0 → 1 (Initial Schema)
**Migration**: `MIGRATION_0_1`
**File**: `core/data/src/main/kotlin/com/banyg/data/local/migration/Migration_0_1.kt`

**Creates:**
- All 4 tables
- All indices
- All foreign key constraints

**Strategy:**
- Since v0 is empty, simply CREATE TABLE for all tables
- Create indices immediately after table creation
- No data migration needed

---

## Index Strategy

### Why These Indices?

**accounts:**
- `is_archived` - Filter active/archived accounts
- `currency_code` - Group by currency
- `type` - Filter by account type

**categories:**
- `group_id` - Group categories by parent
- `is_hidden` - Filter visible categories

**transactions:**
- `account_id` - All transactions for account (most common query)
- `date` - Date range queries, sorting
- `category_id` - Filter by category
- `status` - Filter pending/cleared
- `transfer_id` - Find linked transfers
- `(account_id, date)` - Compound for efficient account+date filtering
- `(account_id, status)` - Compound for account inbox view

**splits:**
- `transaction_id` - Get all splits for transaction
- `category_id` - Sum splits by category

---

## Foreign Key Cascades

### CASCADE (Delete children)
- `transactions.account_id` → ON DELETE CASCADE
  *When account deleted, delete all its transactions*

- `splits.transaction_id` → ON DELETE CASCADE
  *When transaction deleted, delete all its splits*

- `splits.category_id` → ON DELETE CASCADE
  *When category deleted, delete splits using it*

### SET NULL (Preserve data)
- `transactions.category_id` → ON DELETE SET NULL
  *When category deleted, keep transaction but clear category*

---

## Schema Export

Room exports schema JSON for version control:

**Location**: `core/data/schemas/`

**Files:**
- `1.json` - Version 1 schema
- `2.json` - Version 2 schema (future)
- etc.

**Enable in build.gradle.kts:**
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

---

## Testing Strategy

### Migration Tests
Test each migration to ensure:
- Schema is created correctly
- Data is preserved
- Indices exist
- Foreign keys work

**Location**: `core/data/src/androidTest/kotlin/com/banyg/data/local/migration/`

**Template:**
```kotlin
@RunWith(AndroidJUnit4::class)
class Migration0to1Test {
    @get:Rule
    val helper = MigrationTestHelper(...)

    @Test
    fun migrate0to1() {
        // Create v0 (empty)
        helper.createDatabase(TEST_DB, 0).close()

        // Run migration
        helper.runMigrationsAndValidate(TEST_DB, 1, true, MIGRATION_0_1)

        // Verify schema
        // ... assertions
    }
}
```

---

## Architecture Compliance

✅ **Separations:**
- Entities in `core/data/local/entity/`
- DAOs in `core/data/local/dao/`
- Domain models in `core/domain/model/`
- Repository interfaces in `core/domain/repository/`
- Repository implementations in `core/data/repository/`
- Mappers in `core/data/mapper/`

✅ **Money Safety:**
- All money as `INTEGER` (Long)
- Column names: `*_minor`
- No Float/Double in database

✅ **Domain Independence:**
- Domain models have no Android imports
- Domain models use java.time classes
- Entities use Long for dates/timestamps

---

## Future Schema Changes

### Planning Checklist

Before modifying schema:
1. [ ] Create new migration file
2. [ ] Update database version
3. [ ] Add migration to BanygDatabase
4. [ ] Write migration test
5. [ ] Update this documentation
6. [ ] Consider backward compatibility
7. [ ] Plan data migration path

### Potential Additions

**Version 2 might include:**
- `budgets` table for budget allocations
- `recurring_transactions` table for recurring entries
- `tags` table for flexible categorization
- `attachments` table for receipts
- `sync_metadata` table for cloud sync

---

**Generated**: 2026-01-27
**For**: Banyg - Local-first Android Finance App
**Database Version**: 1
