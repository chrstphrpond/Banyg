# Banyg Finance OS - Development Roadmap

## Current Status: Testing & Quality Phase (Phase 4)

**Completed:**
- ✅ Core domain models (Account, Transaction, Category, Budget)
- ✅ Repository layer with Room database
- ✅ CSV Import use case with duplicate detection
- ✅ Budget use cases (Create, Update, Delete, Progress tracking)
- ✅ ViewModels for all feature modules
- ✅ Dependency injection with Hilt
- ✅ Unit tests for domain layer
- ✅ Build system fixed with desugaring
- ✅ **Inbox Screen** - Full implementation with swipe actions, category chips, pull-to-refresh
- ✅ **Add Transaction Screen** - Full implementation with form validation
- ✅ **Budget Screen** - Full implementation with progress bars, create/edit dialogs
- ✅ **Reports Screen** - Full implementation with spending breakdown, period selector
- ✅ **CSV Import Screen** - Full implementation with file picker, preview, duplicate detection
- ✅ **Navigation** - All screens integrated in NavHost

**Remaining (Phase 4 - Testing):**
- 🔄 ReportsViewModel tests
- 🔄 CsvImportViewModel tests  
- 🔄 InboxViewModel tests
- 🔄 Integration tests for CSV edge cases

**Remaining (Phase 5 - Polish):**
- 🔄 Error handling improvements
- 🔄 State management (process death restoration)
- 🔄 Accessibility (content descriptions, TalkBack)

**Remaining (Phase 6 - Cleanup):**
- 🔄 Remove duplicate folder structures
- 🔄 Documentation updates
- 🔄 Release preparation

---

## Phase 1: Build Stabilization ✅ COMPLETE

**Goal:** Fix compilation errors and ensure stable builds

### 1.1 Fix Build Issues
- [x] Investigate budget repository DI bindings
- [x] Verify database migration v1 → v2 works
- [x] Check BudgetDao injection in DatabaseModule
- [x] Fix any missing imports or dependencies
- [x] Run full build successfully

### 1.2 Verify New Components
- [x] Test BudgetRepositoryImpl compiles
- [x] Test BudgetDao queries
- [x] Verify Migration_1_2 runs correctly
- [x] Check all new use cases compile

**Status:** ✅ `./gradlew build` passes - All modules compile successfully

---

## Phase 2: UI Integration ✅ COMPLETE

**Goal:** Connect ViewModels to UI screens

### 2.1 Inbox Feature ✅ COMPLETE
- [x] Update `InboxScreen.kt` to use `InboxViewModel`
- [x] Implement transaction list with swipe actions
- [x] Add category selection dropdown (chips)
- [x] Add "Mark as Cleared" button (swipe action)
- [x] Add pull-to-refresh
- [x] Add empty state with icon
- [x] Add loading states
- [x] Add error states with retry

### 2.2 Add Transaction Feature ✅ COMPLETE
- [x] Update `AddTransactionScreen.kt` to use `AddTransactionViewModel`
- [x] Add amount input with validation
- [x] Add merchant input
- [x] Add account selection dropdown
- [x] Add expense/income toggle
- [x] Add memo field
- [x] Form validation with error messages
- [x] Loading and error states

### 2.3 Budget Feature ✅ COMPLETE
- [x] Update `BudgetScreen.kt` to use `BudgetViewModel`
- [x] Create budget list with progress bars
- [x] Add "Create Budget" bottom sheet/dialog
- [x] Show budgeted/spent/remaining for each category
- [x] Color-code budgets (green/yellow/red based on progress)
- [x] Add monthly period selector
- [x] Add empty state
- [x] Add loading/error states

### 2.4 Reports Feature ✅ COMPLETE
- [x] Update `ReportsScreen.kt` to use `ReportsViewModel`
- [x] Create spending by category list
- [x] Add period selector (This Month, Last Month, etc.)
- [x] Add percentage bars for category breakdown
- [x] Add income vs expense summary
- [x] Add empty state
- [x] Add loading/error states

**Estimated Effort:** 6-8 hours remaining
**Dependencies:** None
**Success Criteria:** All screens functional with ViewModels

---

## Phase 3: CSV Import UI ✅ COMPLETE

**Goal:** Build user interface for CSV import (MVP critical feature)

### 3.1 Create CsvImport Module ✅
- [x] Create new feature module `feature:csvimport`
- [x] Add to `settings.gradle.kts`
- [x] Add module dependencies (core:domain, core:ui)
- [x] Create `CsvImportViewModel`

### 3.2 File Selection Screen ✅
- [x] Create `CsvImportScreen.kt`
- [x] Add file picker button (using Storage Access Framework)
- [x] Show selected file name
- [x] Validate file is CSV

### 3.3 Column Mapping Screen ✅
- [x] Show CSV preview (first 5 rows)
- [x] Account selection dropdown
- [x] "Auto-detect" button using CsvFormatDetector

### 3.4 Preview Screen ✅
- [x] Show parsed transactions table
- [x] Highlight duplicates with warning icon
- [x] Show transaction count
- [x] Show duplicate count
- [x] Checkboxes to select/deselect transactions
- [x] "Import Selected" button

### 3.5 Import Progress ✅
- [x] Show import progress indicator
- [x] Success message with imported count
- [x] Error handling with skipped rows
- [x] Navigate back to Inbox after import

### 3.6 Navigation ✅
- [x] Add CSV Import to navigation graph
- [x] Add menu item/button in Inbox screen

**Estimated Effort:** 6-8 hours
**Dependencies:** Phase 2 UI patterns established
**Success Criteria:** User can import CSV from bank statement

---

## Phase 4: Testing & Quality 🟡 IN PROGRESS

**Goal:** Comprehensive testing and bug fixes

### 4.1 Unit Tests
- [x] Add tests for BudgetViewModel
- [ ] Add tests for ReportsViewModel
- [ ] Add tests for CsvImportViewModel
- [ ] Add tests for InboxViewModel
- [ ] Add tests for CSV edge cases (malformed data, empty files)
- [ ] Add tests for budget calculations
- [ ] Add tests for duplicate detection accuracy

### 4.2 Integration Tests
- [ ] Test full transaction flow (add → categorize → budget update)
- [ ] Test CSV import with real bank statements
- [ ] Test database migrations
- [ ] Test backup/restore (if implemented)

### 4.3 UI Tests
- [ ] Test navigation flows
- [ ] Test form validation
- [ ] Test error states
- [ ] Test screen rotations

### 4.4 Performance Testing
- [ ] Test with 1000+ transactions
- [ ] Test CSV import with large files (500+ rows)
- [ ] Verify inbox loads in <500ms (per PRD)
- [ ] Memory profiling

**Estimated Effort:** 6-8 hours
**Dependencies:** Phases 2-3 complete
**Success Criteria:** 80%+ test coverage, no critical bugs

---

## Phase 5: Polish & Hardening 🟢 MEDIUM

**Goal:** Production-ready polish

### 5.1 Error Handling
- [ ] Add global error handler
- [ ] Add retry mechanisms for DB operations
- [ ] Add user-friendly error messages
- [ ] Add crash reporting (Firebase Crashlytics - optional)

### 5.2 State Management
- [ ] Handle process death/restoration
- [ ] Add loading states to all async operations
- [ ] Add empty states for all lists
- [ ] Add error states for all screens

### 5.3 Accessibility
- [ ] Add content descriptions to icons
- [ ] Test with TalkBack
- [ ] Ensure proper contrast ratios
- [ ] Add accessibility labels to forms

### 5.4 Edge Cases
- [ ] Handle empty database (first launch)
- [ ] Handle large amounts (overflow protection)
- [ ] Handle special characters in merchant names
- [ ] Handle timezone issues with dates

**Estimated Effort:** 4-6 hours
**Dependencies:** Phases 1-4 complete
**Success Criteria:** App handles all edge cases gracefully

---

## Phase 6: Cleanup & Tech Debt 🟢 LOW

**Goal:** Clean up project structure

### 6.1 Remove Duplicate Folders
- [ ] Remove `core/core/` duplicate folder structure
- [ ] Remove `feature/feature/` duplicate folder structure
- [ ] Verify no code is lost in cleanup
- [ ] Ensure build still passes

### 6.2 Documentation
- [ ] Update README.md with features
- [ ] Add user guide for CSV import
- [ ] Document budget system
- [ ] Add troubleshooting guide

### 6.3 Release Preparation
- [ ] Create app icon (all densities)
- [ ] Write app store description
- [ ] Create screenshots
- [ ] Set up signing configuration
- [ ] Configure ProGuard/R8 rules

**Estimated Effort:** 2-4 hours
**Dependencies:** Phases 1-5 complete

---

## Summary Timeline

| Phase | Effort | Status |
|-------|--------|--------|
| Phase 1: Build Stabilization | 2-4 hrs | ✅ COMPLETE |
| Phase 2: UI Integration | 6-8 hrs | ✅ COMPLETE |
| Phase 3: CSV Import UI | 6-8 hrs | ✅ COMPLETE |
| Phase 4: Testing | 6-8 hrs | 🟡 IN PROGRESS |
| Phase 5: Polish | 4-6 hrs | 🟢 PENDING |
| Phase 6: Cleanup | 2-4 hrs | 🟢 PENDING |

**Total Remaining Effort:** 12-18 hours (2-3 days of focused work)

---

## Immediate Priorities

### This Week
1. **ViewModel Tests** (Phase 4.1) - Reports, CsvImport, Inbox
2. **Integration Tests** (Phase 4.2) - CSV edge cases

### Next Week
3. **Polish** (Phase 5) - Error handling, state management, accessibility
4. **Cleanup** (Phase 6) - Documentation, release prep

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| CSV formats vary widely | Medium | Test with real bank statements |
| Performance with large data | Medium | Add pagination, lazy loading |
| Scope creep | High | Stick to MVP features only |

---

## Definition of Done (MVP)

- [x] User can add transaction (3 taps or less)
- [x] User can categorize transactions in inbox
- [x] User can set monthly budgets per category
- [x] User can see budget progress
- [x] User can import CSV bank statements
- [x] User can see spending reports
- [x] App works offline completely
- [x] All money calculations use Long (no Float/Double)
- [ ] Build passes with tests
- [ ] No critical bugs

---

## Notes

- **Money Safety:** All financial calculations must use `Long` (cents/minor units). Never use Float or Double.
- **Offline First:** All features must work without internet.
- **Performance:** Inbox load <500ms for 1000 transactions.
- **Test Coverage:** Maintain >70% unit test coverage.
