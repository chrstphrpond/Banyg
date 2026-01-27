---
name: architecture-verify
description: Verify Banyg architecture rules (module dependencies, import restrictions)
---

# Architecture Verification

Verify that code follows Banyg's strict architecture rules defined in CLAUDE.md.

## Rules to Check

### 1. core/domain has ZERO Android imports
**Rule**: `core/domain` module must not import any Android framework classes.
**Why**: Domain layer must be pure Kotlin for testability and platform independence.

**Check**:
```bash
find core/domain/src -name "*.kt" -exec grep -l "import android\." {} \;
```

**Expected**: No files found (empty output)
**Violation**: Any file importing android.* classes

---

### 2. Repository Separation
**Rule**:
- Repository **interfaces** in `core/domain/repository`
- Repository **implementations** in `core/data/repository`

**Check**:
```bash
# Find repository implementations in wrong place
find core/domain -name "*RepositoryImpl.kt"
# Find repository interfaces in wrong place
find core/data -name "*Repository.kt" ! -name "*RepositoryImpl.kt"
```

**Expected**: No files found for either check
**Violation**: Implementation in domain or interface in data

---

### 3. ViewModels Never Import Room or DataStore
**Rule**: ViewModels should only use domain types, never Room entities or DataStore.

**Check**:
```bash
find feature app -name "*ViewModel.kt" -exec grep -l "import androidx.room\|import androidx.datastore" {} \;
```

**Expected**: No files found
**Violation**: ViewModel importing Room or DataStore

---

### 4. Composables Never Import Room or DataStore
**Rule**: Composable functions should only use domain types via ViewModels.

**Check**:
```bash
find feature app core/ui -name "*Screen.kt" -o -name "*Composable.kt" | xargs grep -l "import androidx.room\|import androidx.datastore" 2>/dev/null
```

**Expected**: No files found
**Violation**: Composable importing Room or DataStore

---

### 5. Module Dependency Direction
**Rule**: Dependencies flow in this direction only:
- `app` → `feature/*` → `core/domain`
- `core/data` → `core/domain`
- `feature/*` → `core/ui`
- `core/ui` → `core/domain` (types only)

**Check**: Verify build.gradle.kts files don't violate dependency rules
```bash
# core/domain should depend on NOTHING
grep "implementation(project" core/domain/build.gradle.kts

# core/ui should only depend on core/domain
grep "implementation(project" core/ui/build.gradle.kts | grep -v "core:domain"

# core/data should only depend on core/domain
grep "implementation(project" core/data/build.gradle.kts | grep -v "core:domain"
```

**Expected**: All commands return empty or only allowed dependencies
**Violation**: Wrong dependency direction

---

### 6. Money Type Safety
**Rule**: Money amounts must use `Long` type (amountMinor), never `Float` or `Double`.

**Check**:
```bash
# Check for Float/Double in money context (excluding test files)
git grep -n "amount.*Double\|amount.*Float" -- "*.kt" | grep -v "Test.kt" | grep -v "format"

# Check for wrong column names in Room entities
git grep -n "@ColumnInfo.*amount" -- "*Entity.kt" | grep -v "amount_minor"
```

**Expected**: No matches
**Violation**: Float/Double used for money or wrong column naming

---

### 7. StateFlow Over LiveData
**Rule**: ViewModels must use `StateFlow`, never `LiveData`.

**Check**:
```bash
find feature app -name "*ViewModel.kt" -exec grep -l "LiveData\|MutableLiveData" {} \;
```

**Expected**: No files found
**Violation**: ViewModel using LiveData

---

### 8. collectAsStateWithLifecycle Over collectAsState
**Rule**: Composables must use `collectAsStateWithLifecycle()` for Flow collection.

**Check**:
```bash
find feature app -name "*Screen.kt" -exec grep -n "\.collectAsState()" {} + | grep -v "collectAsStateWithLifecycle"
```

**Expected**: No matches
**Violation**: Using collectAsState() instead of collectAsStateWithLifecycle()

---

## Execution

When invoked, run all checks and report:

### Output Format

```markdown
## Architecture Verification Report

### ✅ Passed Rules
- Rule 1: core/domain has no Android imports
- Rule 2: Repository separation correct
...

### ❌ Failed Rules

#### Rule 3: ViewModels Import Room/DataStore
**Severity**: CRITICAL
**Violations**:
- feature/inbox/InboxViewModel.kt:15 imports androidx.room.Database
- feature/budget/BudgetViewModel.kt:8 imports androidx.datastore.core

**Fix**: Remove direct Room/DataStore usage. Use repository interfaces from core/domain instead.

---

### Summary
- **Total Rules**: 8
- **Passed**: 6
- **Failed**: 2
- **Critical Violations**: 2
```

## Usage

```bash
# Run architecture verification
/architecture-verify

# Or let Claude invoke automatically when making architectural changes
```

## Implementation Notes

- Run all bash commands from project root
- Use `grep -l` to list files only (no line content)
- Use `find` with filters to search specific module directories
- Exit early if critical violations found
- Suggest fixes for each violation type
