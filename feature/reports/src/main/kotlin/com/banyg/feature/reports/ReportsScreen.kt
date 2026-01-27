package com.banyg.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banyg.domain.model.Category
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.ui.components.GradientCard
import com.banyg.ui.components.OutlinedPillButton
import com.banyg.ui.components.PillButton
import com.banyg.ui.format.format
import com.banyg.ui.theme.BanygGradients
import com.banyg.ui.theme.BanygTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ReportsRoute(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-time events
    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ReportsEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ReportsEvent.NavigateToCategoryDetail -> {
                    // Navigation handled by parent
                }
                is ReportsEvent.ExportComplete -> {
                    // Could show a share dialog
                }
            }
        }
    }

    ReportsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsScreen(
    uiState: ReportsUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ReportsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BanygTheme.colors.backgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reports_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = BanygTheme.colors.textPrimary
                    )
                },
                actions = {
                    if (uiState is ReportsUiState.Success) {
                        IconButton(
                            onClick = { onEvent(ReportsUiEvent.OnExportReport) },
                            enabled = !uiState.isExporting
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = BanygTheme.colors.limeGreen,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = stringResource(R.string.reports_export),
                                    tint = BanygTheme.colors.limeGreen
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BanygTheme.colors.backgroundDark,
                    titleContentColor = BanygTheme.colors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is ReportsUiState.Loading -> {
                LoadingContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is ReportsUiState.Success -> {
                SuccessContent(
                    state = uiState,
                    onEvent = onEvent,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is ReportsUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    canRetry = uiState.canRetry,
                    onRetry = { onEvent(ReportsUiEvent.OnRetry) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    state: ReportsUiState.Success,
    onEvent: (ReportsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val reportData = state.reportData

    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(ReportsUiEvent.OnRefresh) },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = BanygTheme.spacing.screenPadding,
                vertical = BanygTheme.spacing.regular
            ),
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
        ) {
            // Period Selector
            item {
                PeriodSelector(
                    selectedPeriod = reportData.period,
                    availablePeriods = state.availablePeriods,
                    onPeriodSelected = { onEvent(ReportsUiEvent.OnPeriodChanged(it)) }
                )
            }

            // Date Range
            item {
                DateRangeDisplay(
                    startDate = reportData.startDate,
                    endDate = reportData.endDate
                )
            }

            // Summary Cards
            item {
                ReportSummaryCard(
                    totalIncome = reportData.totalIncome,
                    totalSpending = reportData.totalSpending,
                    netAmount = reportData.netAmount
                )
            }

            // Category Breakdown Header
            item {
                Text(
                    text = stringResource(R.string.reports_breakdown_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = BanygTheme.colors.textPrimary
                )
            }

            // Empty state or Category spending list
            if (reportData.categorySpending.isEmpty()) {
                item {
                    EmptyBreakdownState()
                }
            } else {
                items(
                    items = reportData.categorySpending,
                    key = { it.category.id }
                ) { spending ->
                    CategorySpendingCard(
                        spending = spending,
                        totalSpending = reportData.totalSpending,
                        onClick = { onEvent(ReportsUiEvent.OnCategoryClick(spending.category.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: ReportPeriod,
    availablePeriods: List<ReportPeriod>,
    onPeriodSelected: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
    ) {
        availablePeriods.forEach { period ->
            val selected = period == selectedPeriod
            val label = when (period) {
                ReportPeriod.WEEKLY -> stringResource(R.string.period_weekly)
                ReportPeriod.MONTHLY -> stringResource(R.string.period_monthly)
                ReportPeriod.QUARTERLY -> stringResource(R.string.period_quarterly)
                ReportPeriod.YEARLY -> stringResource(R.string.period_yearly)
            }

            if (selected) {
                PillButton(
                    text = label,
                    onClick = { onPeriodSelected(period) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                OutlinedPillButton(
                    text = label,
                    onClick = { onPeriodSelected(period) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DateRangeDisplay(
    startDate: LocalDate,
    endDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val formattedRange = "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}"

    Text(
        text = formattedRange,
        style = MaterialTheme.typography.bodyMedium,
        color = BanygTheme.colors.textSecondary,
        modifier = modifier
    )
}

@Composable
private fun ReportSummaryCard(
    totalIncome: Money,
    totalSpending: Money,
    netAmount: Money,
    modifier: Modifier = Modifier
) {
    GradientCard(
        gradient = BanygGradients.DarkOlive,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.medium)
        ) {
            // Income Row
            SummaryRow(
                label = stringResource(R.string.reports_total_income),
                amount = totalIncome.format(),
                color = BanygTheme.colors.successGreen
            )

            HorizontalDivider(color = BanygTheme.colors.surfaceDark)

            // Spending Row
            SummaryRow(
                label = stringResource(R.string.reports_total_spending),
                amount = totalSpending.format(),
                color = BanygTheme.colors.errorRed
            )

            HorizontalDivider(color = BanygTheme.colors.surfaceDark)

            // Net Row
            val netColor = if (netAmount.minorUnits >= 0) {
                BanygTheme.colors.successGreen
            } else {
                BanygTheme.colors.errorRed
            }
            SummaryRow(
                label = stringResource(R.string.reports_net),
                amount = netAmount.format(),
                color = netColor,
                isBold = true
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    isBold: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textSecondary
        )
        Text(
            text = amount,
            style = if (isBold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}

@Composable
private fun CategorySpendingCard(
    spending: CategorySpending,
    totalSpending: Money,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = spending.percentage
    val progressColor = when {
        percentage > 0.5f -> BanygTheme.colors.errorRed
        percentage > 0.3f -> BanygTheme.colors.warningOrange
        else -> BanygTheme.colors.limeGreen
    }

    GradientCard(
        gradient = BanygGradients.SubtleDark,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
                ) {
                    // Category indicator circle
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(progressColor)
                    )

                    Text(
                        text = spending.category.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = BanygTheme.colors.textPrimary
                    )
                }

                Text(
                    text = spending.amount.format(),
                    style = MaterialTheme.typography.titleMedium,
                    color = BanygTheme.colors.textPrimary
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(BanygTheme.shapes.pill),
                color = progressColor,
                trackColor = BanygTheme.colors.surfaceDark,
                strokeCap = StrokeCap.Round
            )

            // Percentage and transaction count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("%.1f%%", percentage * 100),
                    style = MaterialTheme.typography.bodySmall,
                    color = BanygTheme.colors.textSecondary
                )
                Text(
                    text = stringResource(
                        R.string.reports_transaction_count,
                        spending.transactionCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = BanygTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyBreakdownState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = BanygTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
        ) {
            Text(
                text = stringResource(R.string.reports_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                color = BanygTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.reports_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = BanygTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(BanygTheme.spacing.screenPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        Text(
            text = stringResource(R.string.reports_title),
            style = MaterialTheme.typography.headlineLarge,
            color = BanygTheme.colors.textPrimary
        )

        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                BanygTheme.colors.surfaceDarkElevated,
                                BanygTheme.colors.surfaceDark
                            )
                        ),
                        shape = BanygTheme.shapes.card
                    )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = BanygTheme.colors.limeGreen,
                strokeWidth = 2.dp
            )
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodySmall,
                color = BanygTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular),
            modifier = Modifier.padding(BanygTheme.spacing.screenPadding)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = BanygTheme.colors.errorRed
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = BanygTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            if (canRetry) {
                OutlinedPillButton(
                    text = stringResource(R.string.retry),
                    onClick = onRetry
                )
            }
        }
    }
}

// Previews

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun ReportsScreenPreview() {
    val sampleCategories = listOf(
        Category(
            id = "1",
            name = "Food",
            groupName = "Expenses",
            icon = null
        ),
        Category(
            id = "2",
            name = "Transportation",
            groupName = "Expenses",
            icon = null
        ),
        Category(
            id = "3",
            name = "Shopping",
            groupName = "Expenses",
            icon = null
        )
    )

    val reportData = ReportData(
        period = ReportPeriod.MONTHLY,
        startDate = LocalDate.now().withDayOfMonth(1),
        endDate = LocalDate.now(),
        categorySpending = listOf(
            CategorySpending(
                category = sampleCategories[0],
                amount = Money(15_000_00, Currency.PHP),
                percentage = 0.50f,
                transactionCount = 45
            ),
            CategorySpending(
                category = sampleCategories[1],
                amount = Money(9_000_00, Currency.PHP),
                percentage = 0.30f,
                transactionCount = 28
            ),
            CategorySpending(
                category = sampleCategories[2],
                amount = Money(6_000_00, Currency.PHP),
                percentage = 0.20f,
                transactionCount = 12
            )
        ),
        totalSpending = Money(30_000_00, Currency.PHP),
        totalIncome = Money(50_000_00, Currency.PHP),
        netAmount = Money(20_000_00, Currency.PHP),
        currency = Currency.PHP
    )

    BanygTheme {
        ReportsScreen(
            uiState = ReportsUiState.Success(
                reportData = reportData,
                availablePeriods = ReportPeriod.entries
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {}
        )
    }
}
