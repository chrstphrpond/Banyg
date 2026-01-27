package com.banyg.feature.reports

import com.banyg.domain.model.Category
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import java.time.LocalDate

/**
 * Time period for reports.
 */
enum class ReportPeriod {
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}

/**
 * Spending data for a category.
 */
data class CategorySpending(
    val category: Category,
    val amount: Money,
    val percentage: Float,
    val transactionCount: Int
)

/**
 * Report data containing spending breakdown.
 */
data class ReportData(
    val period: ReportPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val categorySpending: List<CategorySpending>,
    val totalSpending: Money,
    val totalIncome: Money,
    val netAmount: Money,
    val currency: Currency
)

/**
 * Reports screen UI state.
 */
sealed interface ReportsUiState {
    data object Loading : ReportsUiState

    data class Success(
        val reportData: ReportData,
        val availablePeriods: List<ReportPeriod> = ReportPeriod.entries,
        val isExporting: Boolean = false,
        val isRefreshing: Boolean = false
    ) : ReportsUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : ReportsUiState
}

/**
 * Reports screen UI events.
 */
sealed interface ReportsUiEvent {
    data object OnRefresh : ReportsUiEvent
    data object OnRetry : ReportsUiEvent
    data class OnPeriodChanged(val period: ReportPeriod) : ReportsUiEvent
    data class OnDateRangeSelected(val startDate: LocalDate, val endDate: LocalDate) : ReportsUiEvent
    data class OnCategoryClick(val categoryId: String) : ReportsUiEvent
    data object OnExportReport : ReportsUiEvent
    data object OnShareReport : ReportsUiEvent
}

/**
 * One-time events sent from ViewModel to UI.
 */
sealed class ReportsEvent {
    data class ShowSnackbar(val message: String) : ReportsEvent()
    data class NavigateToCategoryDetail(val categoryId: String, val startDate: LocalDate, val endDate: LocalDate) : ReportsEvent()
    data class ExportComplete(val filePath: String) : ReportsEvent()
}
