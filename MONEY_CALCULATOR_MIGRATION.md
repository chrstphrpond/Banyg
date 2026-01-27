# MoneyCalculator Refactoring - Migration Guide

## Summary

MoneyCalculator has been refactored into three focused classes following the Single Responsibility Principle:

1. **MoneyArithmetic** - Basic arithmetic operations (add, subtract, multiply, divide, percentage, roundToMajor, sum)
2. **MoneySplitter** - Splitting operations (split, splitByPercentages)
3. **MoneyValidator** - Validation operations (validateTransfer, validateSplits)

The original `MoneyCalculator` object has been kept as a **facade** for backward compatibility, but is marked as `@Deprecated`.

## Files Created

### New Implementation Classes
- `C:\coding\Banyg\core\domain\src\main\kotlin\com\banyg\domain\calculator\MoneyArithmetic.kt`
- `C:\coding\Banyg\core\domain\src\main\kotlin\com\banyg\domain\calculator\MoneySplitter.kt`
- `C:\coding\Banyg\core\domain\src\main\kotlin\com\banyg\domain\calculator\MoneyValidator.kt`

### New Test Files
- `C:\coding\Banyg\core\domain\src\test\kotlin\com\banyg\domain\calculator\MoneyArithmeticTest.kt`
- `C:\coding\Banyg\core\domain\src\test\kotlin\com\banyg\domain\calculator\MoneySplitterTest.kt`
- `C:\coding\Banyg\core\domain\src\test\kotlin\com\banyg\domain\calculator\MoneyValidatorTest.kt`

### Modified Files
- `C:\coding\Banyg\core\domain\src\main\kotlin\com\banyg\domain\calculator\MoneyCalculator.kt` - Now a facade that delegates to new classes

### Unchanged Files
- `C:\coding\Banyg\core\domain\src\test\kotlin\com\banyg\domain\calculator\MoneyCalculatorTest.kt` - All tests still pass via facade

## Function Distribution

### MoneyArithmetic (8 functions)
- `add(a: Money, b: Money): Money`
- `subtract(a: Money, b: Money): Money`
- `multiply(money: Money, factor: Long): Money`
- `multiply(money: Money, factor: Double): Money`
- `divide(money: Money, divisor: Long): Money`
- `percentage(money: Money, percent: Int): Money`
- `roundToMajor(money: Money): Money`
- `sum(amounts: List<Money>): Money`

### MoneySplitter (2 functions)
- `split(money: Money, parts: Int): List<Money>`
- `splitByPercentages(money: Money, percentages: List<Int>): List<Money>`

### MoneyValidator (2 functions)
- `validateTransfer(from: Money, to: Money)`
- `validateSplits(total: Money, splits: List<Money>)`

## Migration Examples

### Before (using MoneyCalculator)
```kotlin
import com.banyg.domain.calculator.MoneyCalculator

val total = MoneyCalculator.add(amount1, amount2)
val splits = MoneyCalculator.split(total, 3)
MoneyCalculator.validateSplits(total, splits)
```

### After (using focused classes)
```kotlin
import com.banyg.domain.calculator.MoneyArithmetic
import com.banyg.domain.calculator.MoneySplitter
import com.banyg.domain.calculator.MoneyValidator

val total = MoneyArithmetic.add(amount1, amount2)
val splits = MoneySplitter.split(total, 3)
MoneyValidator.validateSplits(total, splits)
```

## Current Usages in Codebase

### Active Usage (needs migration)
1. **File**: `C:\coding\Banyg\core\domain\src\main\kotlin\com\banyg\domain\usecase\AddManualTransactionUseCase.kt`
   - **Line 53**: `val newBalance = MoneyCalculator.add(account.currentBalance, amount)`
   - **Migration**: Replace with `MoneyArithmetic.add(account.currentBalance, amount)`
   - **Status**: Compiles with deprecation warning, works correctly

### Unused Import (should be removed)
2. **File**: `C:\coding\Banyg\core\domain\src\main\kotlin\com\banyg\domain\model\BudgetProgress.kt`
   - **Line 3**: `import com.banyg.domain.calculator.MoneyCalculator`
   - **Status**: Import not used in code
   - **Action**: Remove unused import

## Migration Steps for Developers

### For New Code
Use the focused classes directly:
- For arithmetic: `MoneyArithmetic`
- For splitting: `MoneySplitter`
- For validation: `MoneyValidator`

### For Existing Code
**Option 1: Keep using MoneyCalculator (recommended for now)**
- No code changes required
- Compiles with deprecation warnings
- Fully backward compatible

**Option 2: Migrate to new classes (for active development)**
1. Identify which operations you're using
2. Replace imports:
   ```kotlin
   // Old
   import com.banyg.domain.calculator.MoneyCalculator

   // New
   import com.banyg.domain.calculator.MoneyArithmetic
   import com.banyg.domain.calculator.MoneySplitter
   import com.banyg.domain.calculator.MoneyValidator
   ```
3. Replace usage:
   ```kotlin
   // Old
   MoneyCalculator.add(a, b)

   // New
   MoneyArithmetic.add(a, b)
   ```

## Test Results

All tests pass successfully:

### New Test Classes
- **MoneyArithmeticTest**: All 31 tests pass ✓
- **MoneySplitterTest**: All 8 tests pass ✓
- **MoneyValidatorTest**: All 9 tests pass ✓

### Legacy Tests (via Facade)
- **MoneyCalculatorTest**: All 58 tests pass ✓

Total: **106 tests passing**

## Benefits of This Refactoring

1. **Single Responsibility** - Each class has one clear purpose
2. **Better Organization** - Easy to find the right class for your needs
3. **Improved Testability** - Tests are focused on specific functionality
4. **Backward Compatible** - Existing code continues to work
5. **Clear Migration Path** - Deprecation warnings guide developers
6. **No Logic Changes** - All behavior preserved exactly

## Safety Guarantees

✅ **All overflow protection maintained** - Math.addExact, Math.multiplyExact, etc.
✅ **All validation maintained** - Currency matching, sign conventions
✅ **All money rules enforced** - Long type, no Float/Double
✅ **All tests passing** - Original + new test suites
✅ **No breaking changes** - Facade provides full compatibility

## Timeline

- **Phase 1 (Current)**: MoneyCalculator facade available, new classes ready
- **Phase 2 (Future)**: Gradually migrate active code to new classes
- **Phase 3 (Future)**: Remove MoneyCalculator facade after all migrations complete

## Questions?

- See original code: `MoneyCalculator.kt` (now a facade)
- See implementations: `MoneyArithmetic.kt`, `MoneySplitter.kt`, `MoneyValidator.kt`
- See tests: `*Test.kt` files in same directory
- See rules: `CLAUDE.md` section on "Critical Money Handling Rules"
