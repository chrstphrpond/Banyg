# Banyg Finance OS - User Guide

Complete guide to using Banyg for personal finance management.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Adding Transactions](#adding-transactions)
3. [Inbox - Review & Categorize](#inbox---review--categorize)
4. [Managing Budgets](#managing-budgets)
5. [Viewing Reports](#viewing-reports)
6. [Importing CSV Bank Statements](#importing-csv-bank-statements)
7. [Managing Accounts](#managing-accounts)
8. [Tips & Best Practices](#tips--best-practices)

---

## Getting Started

### First Launch

When you first open Banyg:

1. **Default accounts** are created (Cash, Checking)
2. **Default categories** are set up (Food, Transport, etc.)
3. You're ready to start tracking!

### Home Screen

The app has a bottom navigation with four main sections:

| Tab | Purpose |
|-----|---------|
| **Inbox** | Review and categorize new transactions |
| **Budgets** | View and manage spending limits |
| **Reports** | See spending breakdowns |
| **Accounts** | Manage your accounts |

---

## Adding Transactions

### Quick Add

The fastest way to record spending:

1. Tap the **+** button on any screen
2. Enter the **amount** (required)
3. Enter **merchant** name (optional but recommended)
4. Select **account** (e.g., Checking, Cash)
5. Select **category** (optional - can be done later in Inbox)
6. Add **memo** for notes (optional)
7. Tap **Save**

**Goal:** Record a transaction in 3 taps or less (amount → merchant → save).

### Income vs Expense

- **Expense** (default): Money going out (negative amount)
- **Income**: Money coming in (positive amount)
  - Tap the toggle to switch to Income mode

### Transfer Between Accounts

To move money between accounts:

1. Create an expense transaction in the "From" account
2. Create an income transaction in the "To" account
3. Use the same memo to link them mentally

---

## Inbox - Review & Categorize

The Inbox is where uncategorized transactions live. Think of it like email—items sit here until you process them.

### What's in the Inbox?

- Newly added transactions without a category
- Imported transactions from CSV files
- Any transaction you marked as "needs review"

### Processing Transactions

#### Method 1: Quick Categorize (Swipe)

1. **Swipe left** on a transaction → Quick actions appear
2. Tap a **category chip** to assign
3. Transaction moves out of Inbox

#### Method 2: Full Edit (Tap)

1. **Tap** the transaction to open detail view
2. Edit any field:
   - Amount
   - Merchant
   - Category
   - Memo
   - Date
3. Tap **Save** or **Mark as Cleared**

### Bulk Actions

- **Pull down** to refresh the list
- **Long press** to select multiple transactions (if available)

### Transaction Status

| Status | Meaning |
|--------|---------|
| **Pending** | Recently added, needs review |
| **Cleared** | Reviewed and categorized |

---

## Managing Budgets

Budgets help you control spending by category. Banyg uses an "envelope-style" system.

### How Budgets Work

- Set a monthly spending limit per category
- Track spending in real-time
- See remaining budget at a glance

### Creating a Budget

1. Go to **Budgets** tab
2. Tap **+ Add Budget**
3. Select a **category** (e.g., "Groceries")
4. Enter **budget amount** for the month (e.g., $500)
5. Tap **Save**

### Understanding Budget Display

Each budget shows:

```
Groceries
$350 spent / $500 budgeted
[##############------] 70%
$150 remaining
```

| Color | Meaning |
|-------|---------|
| 🟢 Green | On track (< 80% spent) |
| 🟡 Yellow | Getting close (80-100% spent) |
| 🔴 Red | Over budget (> 100% spent) |

### Monthly Rollover

- Budgets are **monthly** (resets each month)
- Spending activity is tracked per month
- Unspent amounts don't automatically roll over (by design)

### Editing or Deleting Budgets

1. Tap the budget in the list
2. Adjust the amount or delete
3. Changes apply immediately

---

## Viewing Reports

Reports help you understand where your money goes.

### Spending by Category

1. Go to **Reports** tab
2. Select a **time period**:
   - This Month
   - Last Month
   - Last 3 Months
3. See categories ranked by spending amount

Each category shows:
- Total spent
- Percentage of total spending
- Visual bar for comparison

### Income vs Expense

View a summary of:
- Total income
- Total expenses
- Net (income - expenses)

### Account Balances

View current balance for each account:
- Calculated from opening balance + all transactions
- Updates in real-time

### Drilling Down

Tap any category in the report to see:
- List of transactions in that category
- For the selected time period

---

## Importing CSV Bank Statements

Import transactions from your bank to save manual entry.

### Supported Banks

Banyg supports standard CSV formats. Most banks export compatible CSV files.

### How to Import

#### Step 1: Export from Your Bank

1. Log into your bank's website or app
2. Find "Download" or "Export" transactions
3. Select CSV format
4. Choose date range (e.g., last 30 days)
5. Download the file

#### Step 2: Import to Banyg

1. In Banyg, go to **Inbox** → Tap **Import CSV**
2. Tap **Select File**
3. Choose your downloaded CSV file
4. Tap **Next**

#### Step 3: Column Mapping

Banyg will try to auto-detect columns. Verify:

| Field | Required | Typical CSV Column |
|-------|----------|-------------------|
| Date | Yes | Date, Transaction Date |
| Description | Yes | Description, Merchant, Payee |
| Amount | Yes | Amount, Transaction Amount |
| Account | Select in UI | (choose your account) |

If auto-detect fails:
1. Tap each dropdown
2. Select the matching column from your CSV
3. Preview updates automatically

#### Step 4: Review & Import

Before final import, review:

- **Transaction count**: How many rows detected
- **Duplicates**: Highlighted in yellow (already exist in Banyg)
- **Data preview**: First few rows shown

Actions:
- **Uncheck** any transactions you don't want to import
- **Review duplicates** carefully (usually skip these)
- Tap **Import Selected** when ready

#### Step 5: Categorize Imported Transactions

After import:
1. New transactions appear in **Inbox**
2. Categorize them as usual
3. Mark as cleared when done

### Duplicate Detection

Banyg detects duplicates using:
- Date
- Amount
- Merchant/Description

**Tip:** Always review flagged duplicates before importing to avoid double-counting.

### Troubleshooting Import Issues

| Problem | Solution |
|---------|----------|
| "No valid transactions found" | Check CSV has headers and data rows |
| Wrong date format | Ensure dates are recognized (DD/MM/YYYY or MM/DD/YYYY) |
| Amounts look wrong | Verify debit/credit column mapping |
| Special characters garbled | Save CSV as UTF-8 encoding |

---

## Managing Accounts

### Account Types

| Type | Use For |
|------|---------|
| **Checking** | Primary bank account |
| **Savings** | Savings accounts |
| **Credit Card** | Credit card tracking |
| **Cash** | Cash expenses |

### Adding an Account

1. Go to **Accounts** tab
2. Tap **+ Add Account**
3. Enter:
   - Account name (e.g., "Chase Checking")
   - Account type
   - Opening balance (current balance today)
   - Currency
4. Tap **Save**

### Archiving Accounts

To hide an account without deleting history:

1. Tap the account
2. Tap **Archive**
3. Account hidden from main list but data preserved

### Editing Accounts

1. Tap the account
2. Edit name, type, or opening balance
3. Tap **Save**

---

## Tips & Best Practices

### Daily Workflow

1. **Capture immediately** - Add transactions as they happen (3 taps!)
2. **Weekly review** - Process Inbox once a week
3. **Monthly budget review** - Check budget progress mid-month

### Categorization Tips

- Be **consistent** with merchant names ("Starbucks" not "Starbucks #4521")
- Use **memos** for context ("Lunch with Sarah")
- Create **custom categories** if needed

### Budgeting Tips

- Start with **3-5 key categories** (don't overcomplicate)
- Review after first month and **adjust amounts**
- Be realistic—budgets should be achievable

### Data Safety

- Banyg stores data **locally** on your device
- **No cloud sync** in MVP (coming later)
- Consider **exporting** data periodically as backup

### Keyboard Shortcuts

*(When using external keyboard)*

- `Tab` - Move between fields
- `Enter` - Save/Confirm
- `ESC` - Cancel/Go back

---

## FAQ

**Q: Does Banyg work offline?**  
A: Yes! Everything works without internet. Data is stored locally.

**Q: Can I sync between devices?**  
A: Not in the current version. Cloud sync is planned for a future release.

**Q: Is my data secure?**  
A: Yes. Data never leaves your device unless you explicitly export it.

**Q: Can I export my data?**  
A: Export functionality is planned. For now, data stays in the local database.

**Q: What currencies are supported?**  
A: All currencies. Each account has its own currency setting.

**Q: Can I split a transaction across categories?**  
A: Yes, when editing a transaction, use the split feature to divide across multiple categories.

---

## Getting Help

If you encounter issues:

1. Check this guide first
2. Review [ROADMAP.md](ROADMAP.md) for known limitations
3. Check [CLAUDE.md](CLAUDE.md) for technical details (developers)

---

*Last updated: 2026-01-28*
