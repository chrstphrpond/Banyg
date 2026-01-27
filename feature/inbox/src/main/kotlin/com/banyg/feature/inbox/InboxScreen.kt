package com.banyg.feature.inbox

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.pullrefresh.PullRefreshIndicator
import androidx.compose.material3.pullrefresh.pullRefresh
import androidx.compose.material3.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banyg.domain.model.Category
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.ui.components.GradientCard
import com.banyg.ui.components.OutlinedPillButton
import com.banyg.ui.components.PillButton
import com.banyg.ui.format.format
import com.banyg.ui.theme.BanygGradients
import com.banyg.ui.theme.BanygTheme
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun InboxRoute(
    onNavigateToTransactionDetail: (String) -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InboxScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToTransactionDetail = onNavigateToTransactionDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxScreen(
    uiState: InboxUiState,
    onEvent: (InboxUiEvent) -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BanygTheme.colors.backgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.inbox_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = BanygTheme.colors.textPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BanygTheme.colors.backgroundDark,
                    titleContentColor = BanygTheme.colors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is InboxUiState.Loading -> {
                LoadingContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is InboxUiState.Success -> {
                SuccessContent(
                    state = uiState,
                    onEvent = onEvent,
                    onNavigateToTransactionDetail = onNavigateToTransactionDetail,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is InboxUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    canRetry = uiState.canRetry,
                    onRetry = { onEvent(InboxUiEvent.OnRetry) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    state: InboxUiState.Success,
    onEvent: (InboxUiEvent) -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { onEvent(InboxUiEvent.OnRefresh) }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (state.transactions.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = BanygTheme.spacing.screenPadding,
                    vertical = BanygTheme.spacing.regular
                ),
                verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
            ) {
                item {
                    InboxHeader(
                        pendingCount = state.transactions.size
                    )
                }

                items(
                    items = state.transactions,
                    key = { it.transaction.id }
                ) { item ->
                    SwipeableTransactionCard(
                        item = item,
                        categories = state.categories,
                        onClick = { onNavigateToTransactionDetail(item.transaction.id) },
                        onCategorySelect = { categoryId ->
                            onEvent(InboxUiEvent.OnCategorySelect(item.transaction.id, categoryId))
                        },
                        onMarkCleared = {
                            onEvent(InboxUiEvent.OnMarkCleared(item.transaction.id))
                        },
                        onDismiss = {
                            onEvent(InboxUiEvent.OnDismiss(item.transaction.id))
                        }
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = BanygTheme.colors.surfaceDarkElevated,
            contentColor = BanygTheme.colors.limeGreen
        )
    }
}

@Composable
private fun InboxHeader(
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.inbox_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textSecondary
        )

        GradientCard(
            gradient = BanygGradients.DarkOlive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.inbox_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = BanygTheme.colors.textSecondary
                )
                Text(
                    text = stringResource(R.string.transaction_count, pendingCount),
                    style = MaterialTheme.typography.headlineMedium,
                    color = BanygTheme.colors.textPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionCard(
    item: TransactionWithCategory,
    categories: List<Category>,
    onClick: () -> Unit,
    onCategorySelect: (String) -> Unit,
    onMarkCleared: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDismiss()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onMarkCleared()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> BanygTheme.colors.errorRed
                SwipeToDismissBoxValue.StartToEnd -> BanygTheme.colors.successGreen
                SwipeToDismissBoxValue.Settled -> BanygTheme.colors.surfaceDark
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = color,
                        shape = BanygTheme.shapes.card
                    )
                    .padding(horizontal = BanygTheme.spacing.regular),
                contentAlignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                }
            ) {
                Icon(
                    imageVector = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Close
                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                        SwipeToDismissBoxValue.Settled -> Icons.Default.Check
                    },
                    contentDescription = null,
                    tint = BanygTheme.colors.textPrimary
                )
            }
        }
    ) {
        TransactionCard(
            item = item,
            categories = categories,
            onClick = onClick,
            onCategorySelect = onCategorySelect
        )
    }
}

@Composable
private fun TransactionCard(
    item: TransactionWithCategory,
    categories: List<Category>,
    onClick: () -> Unit,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val transaction = item.transaction
    val currentCategory = item.category

    GradientCard(
        gradient = BanygGradients.SubtleDark,
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.medium)
        ) {
            // Transaction header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        color = BanygTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = BanygTheme.colors.textSecondary
                    )
                }

                Text(
                    text = transaction.amount.format(),
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        transaction.amount.minorUnits < 0 -> BanygTheme.colors.errorRed
                        transaction.amount.minorUnits > 0 -> BanygTheme.colors.successGreen
                        else -> BanygTheme.colors.textSecondary
                    }
                )
            }

            // Category chips
            CategoryChips(
                categories = categories,
                selectedCategoryId = transaction.categoryId,
                onCategorySelect = onCategorySelect
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small),
        contentPadding = PaddingValues(vertical = BanygTheme.spacing.extraSmall)
    ) {
        // Uncategorized option
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { /* Already uncategorized, do nothing */ },
                label = {
                    Text(
                        text = stringResource(R.string.transaction_uncategorized),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BanygTheme.colors.surfaceDarkElevated,
                    selectedLabelColor = BanygTheme.colors.textSecondary,
                    containerColor = BanygTheme.colors.surfaceDark,
                    labelColor = BanygTheme.colors.textTertiary
                )
            )
        }

        items(categories) { category ->
            FilterChip(
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelect(category.id) },
                label = {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BanygTheme.colors.limeGreen,
                    selectedLabelColor = BanygTheme.colors.textOnLime,
                    containerColor = BanygTheme.colors.surfaceDarkElevated,
                    labelColor = BanygTheme.colors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
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
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = BanygTheme.colors.limeGreen
            )

            Text(
                text = stringResource(R.string.inbox_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = BanygTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.inbox_empty_description),
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
            text = stringResource(R.string.inbox_title),
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
                text = stringResource(R.string.inbox_loading),
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
                    text = stringResource(R.string.inbox_retry),
                    onClick = onRetry
                )
            }
        }
    }
}

private fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}

// Previews

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun InboxLoadingPreview() {
    BanygTheme {
        InboxScreen(
            uiState = InboxUiState.Loading,
            onEvent = {},
            onNavigateToTransactionDetail = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun InboxEmptyPreview() {
    BanygTheme {
        InboxScreen(
            uiState = InboxUiState.Success(
                transactions = emptyList(),
                categories = emptyList()
            ),
            onEvent = {},
            onNavigateToTransactionDetail = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun InboxSuccessPreview() {
    BanygTheme {
        val sampleCategories = listOf(
            Category(
                id = "1",
                name = "Food",
                groupName = "Expenses"
            ),
            Category(
                id = "2",
                name = "Transportation",
                groupName = "Expenses"
            ),
            Category(
                id = "3",
                name = "Shopping",
                groupName = "Expenses"
            )
        )

        val sampleTransactions = listOf(
            TransactionWithCategory(
                transaction = Transaction(
                    id = "1",
                    accountId = "acc1",
                    date = LocalDate.now(),
                    amount = Money(-250_00, Currency.PHP),
                    merchant = "Jollibee",
                    memo = "Lunch",
                    categoryId = null,
                    status = TransactionStatus.PENDING,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                ),
                category = null
            ),
            TransactionWithCategory(
                transaction = Transaction(
                    id = "2",
                    accountId = "acc1",
                    date = LocalDate.now().minusDays(1),
                    amount = Money(-1_500_00, Currency.PHP),
                    merchant = "Shell",
                    memo = "Gas",
                    categoryId = "2",
                    status = TransactionStatus.PENDING,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                ),
                category = sampleCategories[1]
            ),
            TransactionWithCategory(
                transaction = Transaction(
                    id = "3",
                    accountId = "acc1",
                    date = LocalDate.now().minusDays(2),
                    amount = Money(-3_450_00, Currency.PHP),
                    merchant = "SM Supermarket",
                    status = TransactionStatus.PENDING,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                ),
                category = null
            )
        )

        InboxScreen(
            uiState = InboxUiState.Success(
                transactions = sampleTransactions,
                categories = sampleCategories
            ),
            onEvent = {},
            onNavigateToTransactionDetail = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun InboxErrorPreview() {
    BanygTheme {
        InboxScreen(
            uiState = InboxUiState.Error(
                message = "Unable to load transactions. Please check your connection.",
                canRetry = true
            ),
            onEvent = {},
            onNavigateToTransactionDetail = {}
        )
    }
}
