package com.banyg.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banyg.domain.model.Category
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.repository.CategoryRepository
import com.banyg.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * ViewModel for the Reports screen.
 * 
 * Manages spending reports by category with different time periods.
 * Calculates spending totals per category and handles report export.
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportsUiState>(ReportsUiState.Loading)
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReportsEvent>()
    val events = _events.receiveAsFlow()

    private var observeJob: Job? = null
    private var currentPeriod: ReportPeriod = ReportPeriod.MONTHLY

    init {
        loadReport()
    }

    /**
     * Load spending report for the current period.
     */
    fun loadReport() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = ReportsUiState.Loading

            val (startDate, endDate) = calculateDateRange(currentPeriod)

            // Combine transactions with categories
            combine(
                transactionRepository.observeRecentTransactions(limit = 5000),
                categoryRepository.observeVisibleCategories()
            ) { transactions, categories ->
                buildReportData(
                    transactions = transactions,
                    categories = categories,
                    period = currentPeriod,
                    startDate = startDate,
                    endDate = endDate
                )
            }
                .catch { throwable ->
                    _uiState.value = ReportsUiState.Error(
                        message = throwable.message ?: "Failed to load report"
                    )
                }
                .collect { reportData ->
                    _uiState.value = ReportsUiState.Success(
                        reportData = reportData,
                        availablePeriods = ReportPeriod.entries
                    )
                }
        }
    }

    /**
     * Change the report time period.
     */
    fun changePeriod(period: ReportPeriod) {
        currentPeriod = period
        loadReport()
    }

    /**
     * Export the current report to a file.
     */
    fun exportReport() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is ReportsUiState.Success) return@launch

            _uiState.update { 
                currentState.copy(isExporting = true) 
            }

            try {
                // Generate CSV content
                val csvContent = generateCsvReport(currentState.reportData)
                
                // In a real implementation, this would save to a file
                // For now, we just simulate the export
                val fileName = "banyg_report_${currentState.reportData.startDate}_${currentState.reportData.period}.csv"
                
                _uiState.update { 
                    currentState.copy(isExporting = false) 
                }
                
                _events.send(ReportsEvent.ExportComplete(fileName))
                _events.send(ReportsEvent.ShowSnackbar("Report exported to $fileName"))
            } catch (e: Exception) {
                _uiState.update { 
                    currentState.copy(isExporting = false) 
                }
                _events.send(ReportsEvent.ShowSnackbar("Export failed: ${e.message}"))
            }
        }
    }

    /**
     * Calculate spending totals per category.
     */
    private fun calculateSpendingByCategory(
        transactions: List<Transaction>,
        categories: List<Category>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CategorySpending> {
        // Filter transactions by date
        val filteredTransactions = transactions.filter { 
            it.date in startDate..endDate 
        }

        // Group transactions by category
        val transactionsByCategory = filteredTransactions
            .filter { it.categoryId != null && it.amount.isExpense }
            .groupBy { it.categoryId!! }

        // Calculate total spending for percentage calculation
        val totalSpendingMinor = transactionsByCategory.values
            .flatten()
            .sumOf { kotlin.math.abs(it.amount.minorUnits) }

        // Build category spending list
        return categories.map { category ->
            val categoryTransactions = transactionsByCategory[category.id] ?: emptyList()
            val amount = if (categoryTransactions.isNotEmpty()) {
                val currency = categoryTransactions.first().amount.currency
                Money(
                    minorUnits = categoryTransactions.sumOf { kotlin.math.abs(it.amount.minorUnits) },
                    currency = currency
                )
            } else {
                Money.zero(Currency.PHP)
            }

            val percentage = if (totalSpendingMinor > 0) {
                (kotlin.math.abs(amount.minorUnits).toFloat() / totalSpendingMinor.toFloat())
            } else {
                0f
            }

            CategorySpending(
                category = category,
                amount = amount,
                percentage = percentage,
                transactionCount = categoryTransactions.size
            )
        }
            .filter { it.amount.minorUnits > 0 }
            .sortedByDescending { it.amount.minorUnits }
    }

    /**
     * Calculate income for the period.
     */
    private fun calculateIncome(
        transactions: List<Transaction>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Money {
        val incomeTransactions = transactions.filter { 
            it.date in startDate..endDate && it.amount.isIncome 
        }
        
        return if (incomeTransactions.isNotEmpty()) {
            val currency = incomeTransactions.first().amount.currency
            Money(
                minorUnits = incomeTransactions.sumOf { it.amount.minorUnits },
                currency = currency
            )
        } else {
            Money.zero(Currency.PHP)
        }
    }

    /**
     * Calculate total spending (expenses only).
     */
    private fun calculateTotalSpending(
        transactions: List<Transaction>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Money {
        val expenseTransactions = transactions.filter { 
            it.date in startDate..endDate && it.amount.isExpense 
        }
        
        return if (expenseTransactions.isNotEmpty()) {
            val currency = expenseTransactions.first().amount.currency
            Money(
                minorUnits = expenseTransactions.sumOf { kotlin.math.abs(it.amount.minorUnits) },
                currency = currency
            )
        } else {
            Money.zero(Currency.PHP)
        }
    }

    /**
     * Build complete report data.
     */
    private fun buildReportData(
        transactions: List<Transaction>,
        categories: List<Category>,
        period: ReportPeriod,
        startDate: LocalDate,
        endDate: LocalDate
    ): ReportData {
        val categorySpending = calculateSpendingByCategory(
            transactions, categories, startDate, endDate
        )
        val totalSpending = calculateTotalSpending(transactions, startDate, endDate)
        val totalIncome = calculateIncome(transactions, startDate, endDate)
        val currency = categorySpending.firstOrNull()?.amount?.currency ?: Currency.PHP

        return ReportData(
            period = period,
            startDate = startDate,
            endDate = endDate,
            categorySpending = categorySpending,
            totalSpending = totalSpending,
            totalIncome = totalIncome,
            netAmount = Money(totalIncome.minorUnits - totalSpending.minorUnits, currency),
            currency = currency
        )
    }

    /**
     * Calculate date range based on period.
     */
    private fun calculateDateRange(period: ReportPeriod): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (period) {
            ReportPeriod.WEEKLY -> {
                val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = startOfWeek.plusDays(6)
                startOfWeek to endOfWeek
            }
            ReportPeriod.MONTHLY -> {
                val startOfMonth = now.withDayOfMonth(1)
                val endOfMonth = startOfMonth.plusMonths(1).minusDays(1)
                startOfMonth to endOfMonth
            }
            ReportPeriod.QUARTERLY -> {
                val quarter = (now.monthValue - 1) / 3
                val startOfQuarter = LocalDate.of(now.year, quarter * 3 + 1, 1)
                val endOfQuarter = startOfQuarter.plusMonths(3).minusDays(1)
                startOfQuarter to endOfQuarter
            }
            ReportPeriod.YEARLY -> {
                val startOfYear = LocalDate.of(now.year, 1, 1)
                val endOfYear = LocalDate.of(now.year, 12, 31)
                startOfYear to endOfYear
            }
        }
    }

    /**
     * Generate CSV content from report data.
     */
    private fun generateCsvReport(reportData: ReportData): String {
        val sb = StringBuilder()
        sb.appendLine("Category,Amount,Percentage,Transaction Count")
        
        reportData.categorySpending.forEach { spending ->
            val amount = kotlin.math.abs(spending.amount.minorUnits) / 100.0
            sb.appendLine("${spending.category.name},$amount,${spending.percentage},${spending.transactionCount}")
        }
        
        sb.appendLine()
        sb.appendLine("Total Income,${reportData.totalIncome.minorUnits / 100.0}")
        sb.appendLine("Total Spending,${reportData.totalSpending.minorUnits / 100.0}")
        sb.appendLine("Net,${reportData.netAmount.minorUnits / 100.0}")
        
        return sb.toString()
    }

    /**
     * Handle UI events from the screen.
     */
    fun onEvent(event: ReportsUiEvent) {
        when (event) {
            ReportsUiEvent.OnRefresh -> {
                val currentState = _uiState.value
                if (currentState is ReportsUiState.Success) {
                    _uiState.update { currentState.copy(isRefreshing = true) }
                }
                loadReport()
            }
            ReportsUiEvent.OnRetry -> loadReport()
            is ReportsUiEvent.OnPeriodChanged -> changePeriod(event.period)
            is ReportsUiEvent.OnDateRangeSelected -> {
                // Custom date range handling would go here
                loadReport()
            }
            is ReportsUiEvent.OnCategoryClick -> viewModelScope.launch {
                val currentState = _uiState.value
                if (currentState is ReportsUiState.Success) {
                    _events.send(
                        ReportsEvent.NavigateToCategoryDetail(
                            categoryId = event.categoryId,
                            startDate = currentState.reportData.startDate,
                            endDate = currentState.reportData.endDate
                        )
                    )
                }
            }
            ReportsUiEvent.OnExportReport -> exportReport()
            ReportsUiEvent.OnShareReport -> {
                // Share functionality would be implemented here
                viewModelScope.launch {
                    _events.send(ReportsEvent.ShowSnackbar("Share feature coming soon"))
                }
            }
        }
    }
}
