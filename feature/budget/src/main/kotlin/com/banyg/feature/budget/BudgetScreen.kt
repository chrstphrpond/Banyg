package com.banyg.feature.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
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
import com.banyg.ui.components.GradientCard
import com.banyg.ui.components.OutlinedPillButton
import com.banyg.ui.components.PillButton
import com.banyg.ui.format.format
import com.banyg.ui.theme.BanygGradients
import com.banyg.ui.theme.BanygTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant

@Composable
fun BudgetRoute(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingBudgetId by remember { mutableStateOf<String?>(null) }
    var budgetToDelete by remember { mutableStateOf<BudgetWithProgress?>(null) }

    // Handle one-time events
    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BudgetEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                BudgetEvent.NavigateToAddBudget -> {
                    showAddDialog = true
                }
                is BudgetEvent.NavigateToEditBudget -> {
                    editingBudgetId = event.budgetId
                    showEditDialog = true
                }
                BudgetEvent.NavigateBack -> {
                    showAddDialog = false
                    showEditDialog = false
                    editingBudgetId = null
                }
            }
        }
    }

    // Reset dialog states when form is reset
    LaunchedEffect(formState.isSaving) {
        if (!formState.isSaving && formState.categoryId.isEmpty()) {
            showAddDialog = false
            showEditDialog = false
            editingBudgetId = null
        }
    }

    BudgetScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onAddBudget = { viewModel.onEvent(BudgetUiEvent.OnAddBudget) },
        onEditBudget = { budgetId ->
            viewModel.onEvent(BudgetUiEvent.OnEditBudget(budgetId))
        },
        onDeleteBudget = { budget ->
            budgetToDelete = budget
        }
    )

    // Add Budget Dialog
    if (showAddDialog && uiState is BudgetUiState.Success) {
        val successState = uiState as BudgetUiState.Success
        BudgetFormDialog(
            title = stringResource(R.string.budget_add_title),
            categories = successState.categories,
            formState = formState,
            onCategorySelected = { viewModel.onEvent(BudgetUiEvent.OnCategorySelected(it)) },
            onAmountChanged = { viewModel.onEvent(BudgetUiEvent.OnAmountChanged(it)) },
            onPeriodChanged = { viewModel.onEvent(BudgetUiEvent.OnPeriodChanged(it)) },
            onSave = { viewModel.onEvent(BudgetUiEvent.OnSaveBudget) },
            onDismiss = { viewModel.onEvent(BudgetUiEvent.OnCancel) }
        )
    }

    // Edit Budget Dialog
    if (showEditDialog && editingBudgetId != null) {
        val successState = uiState as? BudgetUiState.Success
        // For edit, show all categories including the one currently selected
        val allCategories = successState?.budgets?.map { it.budget.category }?.filterNotNull().orEmpty() +
                successState?.categories.orEmpty()
        BudgetFormDialog(
            title = stringResource(R.string.budget_edit_title),
            categories = allCategories.distinctBy { it.id },
            formState = formState,
            onCategorySelected = { viewModel.onEvent(BudgetUiEvent.OnCategorySelected(it)) },
            onAmountChanged = { viewModel.onEvent(BudgetUiEvent.OnAmountChanged(it)) },
            onPeriodChanged = { viewModel.onEvent(BudgetUiEvent.OnPeriodChanged(it)) },
            onSave = { viewModel.onEvent(BudgetUiEvent.OnSaveBudget) },
            onDismiss = { viewModel.onEvent(BudgetUiEvent.OnCancel) }
        )
    }

    // Delete Confirmation Dialog
    budgetToDelete?.let { budget ->
        DeleteBudgetDialog(
            budget = budget,
            onConfirm = {
                viewModel.onEvent(BudgetUiEvent.OnDeleteBudget(budget.budget.id))
                budgetToDelete = null
            },
            onDismiss = { budgetToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetScreen(
    uiState: BudgetUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (BudgetUiEvent) -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (String) -> Unit,
    onDeleteBudget: (BudgetWithProgress) -> Unit,
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
                        text = stringResource(R.string.budget_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = BanygTheme.colors.textPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BanygTheme.colors.backgroundDark,
                    titleContentColor = BanygTheme.colors.textPrimary
                )
            )
        },
        floatingActionButton = {
            if (uiState is BudgetUiState.Success && uiState.categories.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onAddBudget,
                    containerColor = BanygTheme.colors.limeGreen,
                    contentColor = BanygTheme.colors.textOnLime
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.budget_add_fab))
                }
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is BudgetUiState.Loading -> {
                LoadingContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is BudgetUiState.Success -> {
                SuccessContent(
                    state = uiState,
                    onEvent = onEvent,
                    onEditBudget = onEditBudget,
                    onDeleteBudget = onDeleteBudget,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is BudgetUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    canRetry = uiState.canRetry,
                    onRetry = { onEvent(BudgetUiEvent.OnRetry) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    state: BudgetUiState.Success,
    onEvent: (BudgetUiEvent) -> Unit,
    onEditBudget: (String) -> Unit,
    onDeleteBudget: (BudgetWithProgress) -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(BudgetUiEvent.OnRefresh) },
        modifier = modifier.fillMaxSize()
    ) {
        if (state.budgets.isEmpty()) {
            EmptyState(
                onAddBudget = { onEvent(BudgetUiEvent.OnAddBudget) },
                hasCategories = state.categories.isNotEmpty()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = BanygTheme.spacing.screenPadding,
                    vertical = BanygTheme.spacing.regular
                ),
                verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
            ) {
                // Summary Card
                item {
                    BudgetSummaryCard(
                        totalBudgeted = state.totalBudgeted,
                        totalSpent = state.totalSpent
                    )
                }

                // Budget Items
                items(
                    items = state.budgets,
                    key = { it.budget.id }
                ) { budget ->
                    BudgetCard(
                        budget = budget,
                        onEdit = { onEditBudget(budget.budget.id) },
                        onDelete = { onDeleteBudget(budget) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    totalBudgeted: Money?,
    totalSpent: Money?,
    modifier: Modifier = Modifier
) {
    if (totalBudgeted == null || totalSpent == null) return

    val remaining = Money(
        totalBudgeted.minorUnits - totalSpent.minorUnits,
        totalBudgeted.currency
    )
    val progress = if (totalBudgeted.minorUnits > 0) {
        (totalSpent.minorUnits.toFloat() / totalBudgeted.minorUnits.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val progressColor = when {
        progress > 0.9f -> BanygTheme.colors.errorRed
        progress > 0.75f -> BanygTheme.colors.warningOrange
        else -> BanygTheme.colors.limeGreen
    }

    GradientCard(
        gradient = BanygGradients.DarkOlive,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.budget_summary_title),
                style = MaterialTheme.typography.titleMedium,
                color = BanygTheme.colors.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = stringResource(R.string.budget_total_budgeted),
                    amount = totalBudgeted.format(),
                    color = BanygTheme.colors.textPrimary
                )
                SummaryItem(
                    label = stringResource(R.string.budget_total_spent),
                    amount = totalSpent.format(),
                    color = BanygTheme.colors.errorRed
                )
                SummaryItem(
                    label = stringResource(R.string.budget_remaining),
                    amount = remaining.format(),
                    color = if (remaining.minorUnits < 0) BanygTheme.colors.errorRed else BanygTheme.colors.limeGreen
                )
            }

            // Overall progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(BanygTheme.shapes.pill),
                color = progressColor,
                trackColor = BanygTheme.colors.surfaceDark,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = BanygTheme.colors.textSecondary
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}

@Composable
private fun BudgetCard(
    budget: BudgetWithProgress,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        budget.isOverBudget -> BanygTheme.colors.errorRed
        budget.progress > 0.9f -> BanygTheme.colors.errorRed
        budget.progress > 0.75f -> BanygTheme.colors.warningOrange
        else -> BanygTheme.colors.limeGreen
    }

    GradientCard(
        gradient = BanygGradients.SubtleDark,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.medium)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.budget.category?.name ?: stringResource(R.string.budget_uncategorized),
                        style = MaterialTheme.typography.titleMedium,
                        color = BanygTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = budget.budget.period.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = BanygTheme.colors.textSecondary
                    )
                }

                // Actions
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.budget_edit),
                            tint = BanygTheme.colors.textSecondary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.budget_delete),
                            tint = BanygTheme.colors.errorRed
                        )
                    }
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { budget.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(BanygTheme.shapes.pill),
                color = progressColor,
                trackColor = BanygTheme.colors.surfaceDark,
                strokeCap = StrokeCap.Round
            )

            // Amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BudgetAmountItem(
                    label = stringResource(R.string.budget_spent),
                    amount = budget.spent.format()
                )
                BudgetAmountItem(
                    label = stringResource(R.string.budget_remaining),
                    amount = budget.remaining.format(),
                    isWarning = budget.isOverBudget
                )
                BudgetAmountItem(
                    label = stringResource(R.string.budget_limit),
                    amount = budget.budget.amount.format()
                )
            }

            // Over budget warning
            if (budget.isOverBudget) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = BanygTheme.colors.errorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.budget_over_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = BanygTheme.colors.errorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetAmountItem(
    label: String,
    amount: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = BanygTheme.colors.textSecondary
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isWarning) BanygTheme.colors.errorRed else BanygTheme.colors.textPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetFormDialog(
    title: String,
    categories: List<Category>,
    formState: BudgetFormState,
    onCategorySelected: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onPeriodChanged: (BudgetPeriod) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = BanygTheme.colors.textPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.medium)
            ) {
                // Category Dropdown
                CategoryDropdown(
                    categories = categories,
                    selectedCategoryId = formState.categoryId,
                    onCategorySelected = onCategorySelected,
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount Input
                OutlinedTextField(
                    value = formState.amountInput,
                    onValueChange = onAmountChanged,
                    label = { Text(stringResource(R.string.budget_amount_label)) },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = formState.error != null
                )

                // Period Selector
                PeriodSelector(
                    selectedPeriod = formState.period,
                    onPeriodSelected = onPeriodChanged,
                    modifier = Modifier.fillMaxWidth()
                )

                // Error Message
                if (formState.error != null) {
                    Text(
                        text = formState.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = BanygTheme.colors.errorRed
                    )
                }
            }
        },
        confirmButton = {
            PillButton(
                text = if (formState.isSaving) stringResource(R.string.saving) else stringResource(R.string.save),
                onClick = onSave,
                enabled = formState.isValid && !formState.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            OutlinedPillButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = BanygTheme.colors.surfaceDark
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory?.name ?: stringResource(R.string.select_category),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: BudgetPeriod,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.budget_period_label),
            style = MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textPrimary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
        ) {
            BudgetPeriod.entries.forEach { period ->
                val selected = period == selectedPeriod
                if (selected) {
                    PillButton(
                        text = period.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { onPeriodSelected(period) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    OutlinedPillButton(
                        text = period.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { onPeriodSelected(period) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteBudgetDialog(
    budget: BudgetWithProgress,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.budget_delete_title),
                style = MaterialTheme.typography.headlineSmall,
                color = BanygTheme.colors.textPrimary
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.budget_delete_message,
                    budget.budget.category?.name ?: stringResource(R.string.budget_uncategorized)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = BanygTheme.colors.textSecondary
            )
        },
        confirmButton = {
            PillButton(
                text = stringResource(R.string.delete),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            OutlinedPillButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = BanygTheme.colors.surfaceDark
    )
}

@Composable
private fun EmptyState(
    onAddBudget: () -> Unit,
    hasCategories: Boolean,
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
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = BanygTheme.colors.limeGreen
            )

            Text(
                text = stringResource(R.string.budget_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = BanygTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (hasCategories) {
                    stringResource(R.string.budget_empty_description)
                } else {
                    stringResource(R.string.budget_empty_no_categories)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = BanygTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            if (hasCategories) {
                Spacer(modifier = Modifier.height(BanygTheme.spacing.regular))
                PillButton(
                    text = stringResource(R.string.budget_add_first),
                    onClick = onAddBudget,
                    leadingIcon = Icons.Default.Add
                )
            }
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
            text = stringResource(R.string.budget_title),
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
private fun BudgetScreenPreview() {
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
        )
    )

    val sampleBudgets = listOf(
        BudgetWithProgress(
            budget = Budget(
                id = "1",
                categoryId = "1",
                category = sampleCategories[0],
                amount = Money(10_000_00, Currency.PHP),
                period = BudgetPeriod.MONTHLY
            ),
            spent = Money(7_500_00, Currency.PHP),
            remaining = Money(2_500_00, Currency.PHP),
            progress = 0.75f,
            isOverBudget = false
        ),
        BudgetWithProgress(
            budget = Budget(
                id = "2",
                categoryId = "2",
                category = sampleCategories[1],
                amount = Money(5_000_00, Currency.PHP),
                period = BudgetPeriod.MONTHLY
            ),
            spent = Money(6_200_00, Currency.PHP),
            remaining = Money(-1_200_00, Currency.PHP),
            progress = 1.24f,
            isOverBudget = true
        )
    )

    BanygTheme {
        BudgetScreen(
            uiState = BudgetUiState.Success(
                budgets = sampleBudgets,
                categories = emptyList(),
                totalBudgeted = Money(15_000_00, Currency.PHP),
                totalSpent = Money(13_700_00, Currency.PHP)
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onAddBudget = {},
            onEditBudget = {},
            onDeleteBudget = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun BudgetEmptyPreview() {
    BanygTheme {
        EmptyState(
            onAddBudget = {},
            hasCategories = true
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun BudgetFormDialogPreview() {
    BanygTheme {
        BudgetFormDialog(
            title = "Add Budget",
            categories = listOf(
                Category("1", "Food", "Expenses", null),
                Category("2", "Transport", "Expenses", null)
            ),
            formState = BudgetFormState(
                categoryId = "1",
                amountInput = "10000",
                period = BudgetPeriod.MONTHLY
            ),
            onCategorySelected = {},
            onAmountChanged = {},
            onPeriodChanged = {},
            onSave = {},
            onDismiss = {}
        )
    }
}
