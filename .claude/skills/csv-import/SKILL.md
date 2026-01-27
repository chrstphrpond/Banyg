---
name: csv-import
description: Generate CSV import functionality for bank transaction files with column mapping, duplicate detection, and preview workflow
disable-model-invocation: true
---

# CSV Import Generator

Generate complete CSV import functionality for importing bank transaction files into Banyg.

## Output Components

### 1. CSV Parser
```kotlin
// core/domain/csv/CsvParser.kt

class CsvParser {

    data class CsvColumnMapping(
        val dateColumn: String,
        val amountColumn: String,
        val descriptionColumn: String,
        val debitColumn: String? = null,  // Optional: separate debit/credit columns
        val creditColumn: String? = null,
        val dateFormat: String = "yyyy-MM-dd"
    )

    data class ParsedTransaction(
        val date: LocalDate,
        val amountMinor: Long,  // Negative for expense, positive for income
        val merchant: String,
        val rawDescription: String
    )

    fun parse(
        csvContent: String,
        mapping: CsvColumnMapping
    ): List<ParsedTransaction> {
        val reader = csvContent.reader()
        val csvFormat = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withIgnoreEmptyLines()
            .withTrim()

        return csvFormat.parse(reader).use { records ->
            records.mapNotNull { record ->
                parseRow(record, mapping)
            }
        }
    }

    private fun parseRow(
        record: CSVRecord,
        mapping: CsvColumnMapping
    ): ParsedTransaction? {
        return try {
            val dateStr = record.get(mapping.dateColumn)
            val date = parseDate(dateStr, mapping.dateFormat)

            val amountMinor = when {
                mapping.debitColumn != null && mapping.creditColumn != null -> {
                    parseAmountWithDebitCredit(record, mapping)
                }
                else -> {
                    parseAmount(record.get(mapping.amountColumn))
                }
            }

            val description = record.get(mapping.descriptionColumn)
            val merchant = normalizeMerchant(description)

            ParsedTransaction(
                date = date,
                amountMinor = amountMinor,
                merchant = merchant,
                rawDescription = description
            )
        } catch (e: Exception) {
            null  // Skip malformed rows
        }
    }

    private fun parseDate(dateStr: String, format: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern(format)
        return LocalDate.parse(dateStr.trim(), formatter)
    }

    private fun parseAmount(amountStr: String): Long {
        val cleaned = amountStr
            .replace("$", "")
            .replace(",", "")
            .trim()
        
        // Parse to double first (for decimal), convert to Long
        val amount = cleaned.toDouble()
        return (amount * 100).roundToLong()
    }

    private fun parseAmountWithDebitCredit(
        record: CSVRecord,
        mapping: CsvColumnMapping
    ): Long {
        val debitStr = record.get(mapping.debitColumn)?.trim() ?: ""
        val creditStr = record.get(mapping.creditColumn)?.trim() ?: ""

        return when {
            debitStr.isNotEmpty() -> {
                // Debit is expense (negative)
                -parseAmount(debitStr)
            }
            creditStr.isNotEmpty() -> {
                // Credit is income (positive)
                parseAmount(creditStr)
            }
            else -> 0L
        }
    }

    private fun normalizeMerchant(description: String): String {
        return description
            .trim()
            .replace(Regex("\\s+"), " ")  // Normalize whitespace
            .replace(Regex("\\*+"), "")   // Remove asterisks
            .replace(Regex("\\d{4,}"), "")  // Remove long numbers (card digits)
            .trim()
            .titlecase()  // "STARBUCKS #1234" -> "Starbucks"
    }
}
```

### 2. Duplicate Detector
```kotlin
// core/domain/csv/DuplicateDetector.kt

class DuplicateDetector {

    data class DuplicateCheckResult(
        val isDuplicate: Boolean,
        val confidence: Float,  // 0.0 - 1.0
        val matchedTransactionId: String? = null
    )

    fun checkDuplicate(
        parsed: CsvParser.ParsedTransaction,
        existingTransactions: List<Transaction>
    ): DuplicateCheckResult {
        // Exact match: date + amount + normalized merchant
        val exactMatch = existingTransactions.find { existing ->
            isExactMatch(parsed, existing)
        }

        if (exactMatch != null) {
            return DuplicateCheckResult(
                isDuplicate = true,
                confidence = 1.0f,
                matchedTransactionId = exactMatch.id
            )
        }

        // Fuzzy match: check similarity
        val fuzzyMatches = existingTransactions.map { existing ->
            existing to calculateSimilarity(parsed, existing)
        }.filter { it.second > 0.8f }

        return if (fuzzyMatches.isNotEmpty()) {
            val bestMatch = fuzzyMatches.maxBy { it.second }
            DuplicateCheckResult(
                isDuplicate = true,
                confidence = bestMatch.second,
                matchedTransactionId = bestMatch.first.id
            )
        } else {
            DuplicateCheckResult(isDuplicate = false, confidence = 0f)
        }
    }

    private fun isExactMatch(
        parsed: CsvParser.ParsedTransaction,
        existing: Transaction
    ): Boolean {
        return parsed.date.toEpochDay() == existing.date.toEpochDay() &&
               parsed.amountMinor == existing.amountMinor &&
               parsed.merchant.equals(existing.merchant, ignoreCase = true)
    }

    private fun calculateSimilarity(
        parsed: CsvParser.ParsedTransaction,
        existing: Transaction
    ): Float {
        var score = 0f

        // Date similarity (within 3 days)
        val daysDiff = kotlin.math.abs(
            parsed.date.toEpochDay() - existing.date.toEpochDay()
        )
        if (daysDiff <= 3) {
            score += 0.4f * (1 - daysDiff / 3f)
        }

        // Amount similarity
        if (parsed.amountMinor == existing.amountMinor) {
            score += 0.4f
        }

        // Merchant similarity (simple string similarity)
        val merchantScore = stringSimilarity(
            parsed.merchant.lowercase(),
            existing.merchant.lowercase()
        )
        score += 0.2f * merchantScore

        return score
    }

    private fun stringSimilarity(s1: String, s2: String): Float {
        // Simple Levenshtein-based similarity
        val distance = levenshteinDistance(s1, s2)
        val maxLen = kotlin.math.max(s1.length, s2.length)
        return if (maxLen == 0) 1f else 1f - (distance.toFloat() / maxLen)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        // Implementation of Levenshtein distance algorithm
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    kotlin.math.min(
                        kotlin.math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    ) + 1
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
}
```

### 3. Import Preview UI State
```kotlin
// feature/import/ImportPreviewUiState.kt

sealed interface ImportPreviewUiState {
    data object Loading : ImportPreviewUiState
    
    data class Preview(
        val transactions: List<ImportTransactionPreview>,
        val duplicatesCount: Int,
        val newCount: Int,
        val selectedAccount: Account? = null
    ) : ImportPreviewUiState
    
    data class Error(val message: String) : ImportPreviewUiState
}

data class ImportTransactionPreview(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val amountMinor: Long,
    val merchant: String,
    val rawDescription: String,
    val category: Category? = null,
    val duplicateStatus: DuplicateStatus = DuplicateStatus.NEW,
    val isSelected: Boolean = true  // User can uncheck to skip
)

sealed interface DuplicateStatus {
    data object NEW : DuplicateStatus
    data class DUPLICATE(
        val confidence: Float,
        val existingTransactionId: String
    ) : DuplicateStatus
}
```

### 4. Import Use Case
```kotlin
// core/domain/usecase/ImportTransactionsUseCase.kt

class ImportTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val csvParser: CsvParser,
    private val duplicateDetector: DuplicateDetector
) {
    data class ImportResult(
        val importedCount: Int,
        val skippedCount: Int,
        val duplicateCount: Int
    )

    suspend operator fun invoke(
        csvContent: String,
        columnMapping: CsvColumnMapping,
        accountId: String,
        importPreviews: List<ImportTransactionPreview>
    ): Result<ImportResult> {
        return try {
            val selectedPreviews = importPreviews.filter { it.isSelected }
            
            val transactionsToImport = selectedPreviews.map { preview ->
                Transaction(
                    id = generateId(),
                    accountId = accountId,
                    amountMinor = preview.amountMinor,
                    merchant = preview.merchant,
                    date = preview.date,
                    categoryId = preview.category?.id,
                    memo = preview.rawDescription,
                    status = TransactionStatus.CLEARED,
                    createdAt = System.currentTimeMillis()
                )
            }

            transactionRepository.insertAll(transactionsToImport)

            Result.success(
                ImportResult(
                    importedCount = transactionsToImport.size,
                    skippedCount = importPreviews.size - selectedPreviews.size,
                    duplicateCount = selectedPreviews.count { 
                        it.duplicateStatus is DuplicateStatus.DUPLICATE 
                    }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 5. Import Preview ViewModel
```kotlin
// feature/import/ImportPreviewViewModel.kt

@HiltViewModel
class ImportPreviewViewModel @Inject constructor(
    private val csvParser: CsvParser,
    private val duplicateDetector: DuplicateDetector,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val importTransactionsUseCase: ImportTransactionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportPreviewUiState>(ImportPreviewUiState.Loading)
    val uiState: StateFlow<ImportPreviewUiState> = _uiState.asStateFlow()

    private var parsedTransactions: List<ImportTransactionPreview> = emptyList()

    fun loadPreview(csvContent: String, columnMapping: CsvColumnMapping) {
        viewModelScope.launch {
            _uiState.value = ImportPreviewUiState.Loading

            try {
                val parsed = csvParser.parse(csvContent, columnMapping)
                val existingTransactions = getTransactionsUseCase().first()

                val previews = parsed.map { transaction ->
                    val duplicateCheck = duplicateDetector.checkDuplicate(
                        transaction,
                        existingTransactions
                    )

                    ImportTransactionPreview(
                        date = transaction.date,
                        amountMinor = transaction.amountMinor,
                        merchant = transaction.merchant,
                        rawDescription = transaction.rawDescription,
                        duplicateStatus = if (duplicateCheck.isDuplicate) {
                            DuplicateStatus.DUPLICATE(
                                confidence = duplicateCheck.confidence,
                                existingTransactionId = duplicateCheck.matchedTransactionId!!
                            )
                        } else {
                            DuplicateStatus.NEW
                        },
                        isSelected = !duplicateCheck.isDuplicate  // Uncheck duplicates by default
                    )
                }

                parsedTransactions = previews

                _uiState.value = ImportPreviewUiState.Preview(
                    transactions = previews,
                    duplicatesCount = previews.count { 
                        it.duplicateStatus is DuplicateStatus.DUPLICATE 
                    },
                    newCount = previews.count { it.duplicateStatus is DuplicateStatus.NEW }
                )
            } catch (e: Exception) {
                _uiState.value = ImportPreviewUiState.Error(
                    message = e.message ?: "Failed to parse CSV"
                )
            }
        }
    }

    fun toggleTransactionSelection(id: String) {
        val current = _uiState.value as? ImportPreviewUiState.Preview ?: return
        
        val updated = current.transactions.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }

        _uiState.value = current.copy(
            transactions = updated,
            newCount = updated.count { it.isSelected && it.duplicateStatus is DuplicateStatus.NEW }
        )
    }

    fun setCategory(transactionId: String, category: Category?) {
        val current = _uiState.value as? ImportPreviewUiState.Preview ?: return
        
        val updated = current.transactions.map {
            if (it.id == transactionId) it.copy(category = category) else it
        }

        _uiState.value = current.copy(transactions = updated)
    }

    fun import(accountId: String, onComplete: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val current = _uiState.value as? ImportPreviewUiState.Preview ?: return@launch

            importTransactionsUseCase(
                csvContent = "",  // Pass actual content from args
                columnMapping = CsvColumnMapping("", "", ""),  // Pass actual mapping
                accountId = accountId,
                importPreviews = current.transactions
            ).onSuccess { result ->
                onComplete(result)
            }.onFailure { error ->
                _uiState.value = ImportPreviewUiState.Error(
                    message = error.message ?: "Import failed"
                )
            }
        }
    }
}
```

## Common Bank CSV Formats

Pre-configured mappings for common banks:

```kotlin
// core/domain/csv/BankFormats.kt

object BankFormats {
    
    val CHASE = CsvColumnMapping(
        dateColumn = "Transaction Date",
        amountColumn = "Amount",
        descriptionColumn = "Description",
        dateFormat = "MM/dd/yyyy"
    )

    val WELLS_FARGO = CsvColumnMapping(
        dateColumn = "Date",
        amountColumn = "Amount",
        descriptionColumn = "Description",
        dateFormat = "MM/dd/yyyy"
    )

    val BANK_OF_AMERICA = CsvColumnMapping(
        dateColumn = "Date",
        debitColumn = "Debit",
        creditColumn = "Credit",
        descriptionColumn = "Description",
        dateFormat = "MM/dd/yyyy"
    )
}
```

## Requirements

**Always include:**
- Column mapping configuration (date, amount, description)
- Support for both single amount column and separate debit/credit columns
- Configurable date formats
- Merchant name normalization (trim, title-case, remove numbers)
- Duplicate detection (exact + fuzzy matching)
- Preview screen before import
- Ability to uncheck transactions to skip
- Category assignment during preview
- Import result summary

**Follow Banyg patterns:**
- Use Long for money amounts (amountMinor)
- Negative for expenses, positive for income
- Proper error handling for malformed CSV rows
- Skip unparseable rows (don't fail entire import)

## Workflow

1. User selects CSV file
2. Detect or manually configure column mapping
3. Parse CSV into preview items
4. Run duplicate detection against existing transactions
5. Show preview screen with:
   - List of transactions
   - Duplicate indicators
   - Checkboxes to include/exclude
   - Category dropdowns
6. User reviews and confirms
7. Import selected transactions
8. Show result summary

## Dependencies

Add to `core/domain/build.gradle.kts`:
```kotlin
dependencies {
    implementation("org.apache.commons:commons-csv:1.10.0")
}
```
