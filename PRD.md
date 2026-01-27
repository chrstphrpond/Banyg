# Banyg Finance OS - Product Requirements (MVP)

## Product Vision

Banyg is a **local-first personal finance OS for Android** that makes money management fast, calm, and offline-ready. Users can capture transactions instantly, review spending in a clean inbox, set budgets that work like envelopes, and see simple reports—all without requiring internet access.

Cloud sync is optional and comes later. The MVP focuses on the core financial tracking workflow that works entirely offline.

## MVP Scope

### 1. Fast Transaction Capture
- **Quick Add**: Capture amount, merchant, optional category from any screen
- **Inbox Flow**: Newly captured transactions land in inbox for later review
- **Minimal Friction**: 3 taps or less to record a transaction
- Fields: amount (required), merchant (optional), category (optional), memo (optional)

### 2. Transaction Review & Categorization
- **Inbox Screen**: Shows uncategorized/unreviewed transactions
- **Categorization**: Assign category, edit merchant, add memo
- **Mark as Cleared**: Move from pending to cleared status
- Support for splits (divide one transaction across multiple categories)

### 3. Account Management
- **Multiple Accounts**: Checking, savings, cash, credit cards
- **Opening Balances**: Set initial balance for each account
- **Account Types**: Different behavior for credit vs debit accounts
- **Transfers**: Move money between accounts (must net to zero)

### 4. Category Management
- **Predefined Categories**: Starter set of common categories
- **Custom Categories**: User can create/edit/hide categories
- **Category Groups**: Optional grouping (e.g., "Housing" contains Rent, Utilities)
- **Income vs Expense**: Categories marked as income or expense

### 5. Budget System (Envelope Style)
- **Monthly Budgets**: Set budget amount per category per month
- **Activity Tracking**: Real-time calculation of spent vs budgeted
- **Overspending Alerts**: Visual indication when over budget
- **Budget Periods**: Month-based (YYYY-MM format)

### 6. Simple Reports
- **Spending by Category**: Show category totals for selected period
- **Time-Based Views**: This month, last month, last 3 months
- **Account Balances**: Current balance per account
- **Budget vs Actual**: Compare budgeted to actual spending

### 7. CSV Import
- **Column Mapping**: Map CSV columns to transaction fields
- **Merchant Normalization**: Trim, standardize merchant names
- **Duplicate Detection**: Fingerprint-based deduping (user-reviewable)
- **Review Before Import**: User approves detected transactions
- Support common bank export formats (date, description, amount, optional debit/credit)

## MVP Non-Goals

These are explicitly **out of scope** for MVP:

- ❌ Cloud sync / multi-device support
- ❌ Shared accounts / multi-user access
- ❌ Investment tracking (stocks, crypto)
- ❌ Bill reminders / recurring transactions
- ❌ Automated categorization (ML/AI)
- ❌ Goals & savings targets
- ❌ Debt payoff calculators
- ❌ Receipt scanning / OCR
- ❌ Bank account linking (Plaid, etc.)
- ❌ Multi-currency with live exchange rates
- ❌ Charts/graphs (simple lists only for MVP)
- ❌ Export to PDF/spreadsheet
- ❌ Search functionality
- ❌ Filters (beyond basic date range)

## Core User Flows

### Flow 1: Capture Transaction
1. User opens app (or uses quick-add widget)
2. Enters amount (e.g., "23.50")
3. Optionally enters merchant (e.g., "Coffee Shop")
4. Taps "Save" → transaction goes to inbox

### Flow 2: Review & Categorize
1. User opens Inbox screen
2. Sees list of uncategorized transactions
3. Taps transaction → edit sheet opens
4. Selects category (e.g., "Food & Dining")
5. Confirms merchant name
6. Marks as cleared → moves out of inbox

### Flow 3: Create Budget
1. User navigates to Budget screen
2. Selects category (e.g., "Groceries")
3. Enters budgeted amount for the month (e.g., "$500")
4. Sees real-time update of available/spent/remaining

### Flow 4: View Report
1. User navigates to Reports screen
2. Selects time period (e.g., "This Month")
3. Sees list of categories with total spent
4. Taps category to see transaction list

### Flow 5: Transfer Between Accounts
1. User creates transaction in "From Account" (negative amount)
2. Marks as Transfer
3. Creates linked transaction in "To Account" (positive amount)
4. System validates: fromAmount + toAmount == 0

## Open Decisions

TODO: Decide on these before implementation:

### Budget Rollover
- **Question**: When a category is under-budget, does the remaining amount carry to next month?
- **Options**:
  - A) Reset to zero each month (stricter)
  - B) Carry over available balance (more flexible)
  - C) Per-category setting (most complex)

### Transaction Status
- **Question**: What statuses do transactions have?
- **Options**:
  - Pending, Cleared
  - Pending, Cleared, Reconciled
  - Add "Void" for deleted transactions?

### Soft Delete
- **Question**: How to handle deleted transactions?
- **Current**: Mark with `deleted_at` timestamp (soft delete)
- **Alternative**: Hard delete but warn user first

### Category Defaults
- **Question**: What categories to create by default?
- **Options**:
  - Minimal set (5-10 common categories)
  - Comprehensive set (30+ categories)
  - None (user creates from scratch)

### Import Deduplication
- **Question**: How aggressive should duplicate detection be?
- **Current**: Exact match on date + amount + merchant
- **Alternative**: Fuzzy match with user approval

## Success Metrics (Post-MVP)

Not measured in MVP, but future considerations:
- Time to capture transaction (target: <5 seconds)
- % of transactions categorized within 24 hours
- Daily active users
- Retention rate (7-day, 30-day)

## Technical Constraints

- **Offline-first**: All features must work without network
- **Fast**: Inbox load time <500ms for 1000 transactions
- **Accurate**: Zero floating-point errors in money calculations
- **Testable**: All money logic has unit tests
- **Minimal Permissions**: No network, camera, or location (for MVP)

## Timeline

MVP is scoped for initial prototype phase. No specific dates set.

**Dependencies**:
- Database schema (see DATA_MODEL.md)
- Money handling rules (see MONEY_RULES.md)
- Budget calculation logic (see BUDGET_MATH.md)
