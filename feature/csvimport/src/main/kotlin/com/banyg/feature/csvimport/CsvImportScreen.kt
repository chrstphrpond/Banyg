package com.banyg.feature.csvimport

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banyg.domain.csv.ColumnMapping
import com.banyg.domain.csv.DuplicateStatus
import com.banyg.domain.model.Account
import com.banyg.domain.model.AccountType
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
import java.time.LocalDate

@Composable
fun CsvImportRoute(
    viewModel: CsvImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-time events
    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CsvImportEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is CsvImportEvent.LaunchFilePicker -> {
                    // File picker would be launched by the Activity/Fragment
                }
                CsvImportEvent.NavigateBack -> {
                    // Navigation handled by parent
                }
            }
        }
    }

    CsvImportScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvImportScreen(
    uiState: CsvImportUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (CsvImportUiEvent) -> Unit,
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
                        text = stringResource(R.string.csv_import_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = BanygTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(CsvImportUiEvent.OnBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = BanygTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BanygTheme.colors.backgroundDark,
                    titleContentColor = BanygTheme.colors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is CsvImportUiState.Initial -> {
                    InitialContent(
                        onSelectFile = { onEvent(CsvImportUiEvent.OnSelectFile) }
                    )
                }

                is CsvImportUiState.Loading -> {
                    LoadingContent()
                }

                is CsvImportUiState.ColumnMapping -> {
                    ColumnMappingContent(
                        state = uiState,
                        onEvent = onEvent
                    )
                }

                is CsvImportUiState.Preview -> {
                    PreviewContent(
                        state = uiState,
                        onEvent = onEvent
                    )
                }

                is CsvImportUiState.ImportComplete -> {
                    ImportCompleteContent(
                        state = uiState,
                        onDismiss = { onEvent(CsvImportUiEvent.OnDismissComplete) }
                    )
                }

                is CsvImportUiState.Error -> {
                    ErrorContent(
                        message = uiState.message,
                        canRetry = uiState.canRetry,
                        onRetry = { onEvent(CsvImportUiEvent.OnRetry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InitialContent(
    onSelectFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(BanygTheme.spacing.screenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.large)
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BanygTheme.colors.limeGreen
            )

            Text(
                text = stringResource(R.string.csv_import_select_file_title),
                style = MaterialTheme.typography.headlineMedium,
                color = BanygTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.csv_import_select_file_description),
                style = MaterialTheme.typography.bodyMedium,
                color = BanygTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            PillButton(
                text = stringResource(R.string.csv_import_select_file_button),
                onClick = onSelectFile,
                leadingIcon = Icons.Default.Description
            )

            Text(
                text = stringResource(R.string.csv_import_supported_formats),
                style = MaterialTheme.typography.bodySmall,
                color = BanygTheme.colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ColumnMappingContent(
    state: CsvImportUiState.ColumnMapping,
    onEvent: (CsvImportUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(BanygTheme.spacing.screenPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.medium)
    ) {
        // File info
        GradientCard(
            gradient = BanygGradients.DarkOlive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.csv_import_file_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = BanygTheme.colors.textSecondary
                )
                Text(
                    text = state.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = BanygTheme.colors.textPrimary
                )
            }
        }

        // Account Selection
        AccountDropdown(
            accounts = state.accounts,
            selectedAccountId = state.selectedAccountId,
            onAccountSelected = { onEvent(CsvImportUiEvent.OnAccountSelected(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        // Auto-detect button
        PillButton(
            text = if (state.isDetecting) stringResource(R.string.csv_import_detecting) else stringResource(R.string.csv_import_auto_detect),
            onClick = { onEvent(CsvImportUiEvent.OnAutoDetect) },
            enabled = !state.isDetecting,
            modifier = Modifier.fillMaxWidth()
        )

        // CSV Preview
        if (state.csvPreview.isNotEmpty()) {
            Text(
                text = stringResource(R.string.csv_import_preview_title),
                style = MaterialTheme.typography.titleMedium,
                color = BanygTheme.colors.textPrimary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = BanygTheme.colors.surfaceDark
                )
            ) {
                Column(
                    modifier = Modifier.padding(BanygTheme.spacing.medium)
                ) {
                    state.csvPreview.take(5).forEachIndexed { index, row ->
                        Text(
                            text = row.joinToString(", ") { it.take(20) },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (index == 0) BanygTheme.colors.limeGreen else BanygTheme.colors.textSecondary,
                            maxLines = 1
                        )
                        if (index < state.csvPreview.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = BanygTheme.spacing.small),
                                color = BanygTheme.colors.surfaceDarkElevated
                            )
                        }
                    }
                }
            }
        }

        // Error message
        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodyMedium,
                color = BanygTheme.colors.errorRed
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Preview button
        PillButton(
            text = stringResource(R.string.csv_import_preview_button),
            onClick = { onEvent(CsvImportUiEvent.OnPreview) },
            enabled = state.selectedAccountId != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedAccount?.name ?: stringResource(R.string.csv_import_select_account),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.csv_import_account_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text("${account.name} (${account.currency.code})") },
                    onClick = {
                        onAccountSelected(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PreviewContent(
    state: CsvImportUiState.Preview,
    onEvent: (CsvImportUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = BanygTheme.spacing.screenPadding)
    ) {
        // Summary
        GradientCard(
            gradient = BanygGradients.DarkOlive,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = BanygTheme.spacing.regular)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.csv_import_preview_summary),
                    style = MaterialTheme.typography.titleMedium,
                    color = BanygTheme.colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryStat(
                        label = stringResource(R.string.csv_import_new_transactions),
                        value = state.newCount.toString(),
                        color = BanygTheme.colors.limeGreen
                    )
                    SummaryStat(
                        label = stringResource(R.string.csv_import_duplicates),
                        value = state.duplicateCount.toString(),
                        color = BanygTheme.colors.warningOrange
                    )
                    SummaryStat(
                        label = stringResource(R.string.csv_import_errors),
                        value = state.errorCount.toString(),
                        color = BanygTheme.colors.errorRed
                    )
                }
            }
        }

        // Select All
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.csv_import_transactions_title),
                style = MaterialTheme.typography.titleMedium,
                color = BanygTheme.colors.textPrimary
            )

            val allSelected = state.transactions.all { it.isSelected }
            TextButton(
                onClick = { onEvent(CsvImportUiEvent.OnSelectAll(!allSelected)) }
            ) {
                Text(
                    text = if (allSelected) stringResource(R.string.csv_import_deselect_all) else stringResource(R.string.csv_import_select_all),
                    color = BanygTheme.colors.limeGreen
                )
            }
        }

        // Transaction list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
        ) {
            items(
                items = state.transactions,
                key = { it.id }
            ) { transaction ->
                TransactionPreviewCard(
                    transaction = transaction,
                    onToggle = { isSelected ->
                        onEvent(CsvImportUiEvent.OnTransactionSelected(transaction.id, isSelected))
                    }
                )
            }
        }

        // Import button
        val selectedCount = state.transactions.count { it.isSelected }
        PillButton(
            text = if (state.isImporting) {
                stringResource(R.string.csv_import_importing)
            } else {
                stringResource(R.string.csv_import_import_button, selectedCount)
            },
            onClick = { onEvent(CsvImportUiEvent.OnImport) },
            enabled = selectedCount > 0 && !state.isImporting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = BanygTheme.spacing.regular)
        )
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = BanygTheme.colors.textSecondary
        )
    }
}

@Composable
private fun TransactionPreviewCard(
    transaction: ImportTransactionPreview,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDuplicate = transaction.duplicateStatus is DuplicateStatus.DUPLICATE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!transaction.isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (transaction.isSelected) {
                BanygTheme.colors.surfaceDarkElevated
            } else {
                BanygTheme.colors.surfaceDark
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BanygTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = transaction.isSelected,
                onCheckedChange = onToggle
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.extraSmall)
            ) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BanygTheme.colors.textPrimary,
                    maxLines = 1
                )
                Text(
                    text = transaction.date.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = BanygTheme.colors.textSecondary
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = transaction.amount.format(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BanygTheme.colors.textPrimary
                )

                if (isDuplicate) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = BanygTheme.colors.warningOrange
                        )
                        Text(
                            text = stringResource(R.string.csv_import_duplicate),
                            style = MaterialTheme.typography.bodySmall,
                            color = BanygTheme.colors.warningOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportCompleteContent(
    state: CsvImportUiState.ImportComplete,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(BanygTheme.spacing.screenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.large)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BanygTheme.colors.limeGreen
            )

            Text(
                text = stringResource(R.string.csv_import_complete_title),
                style = MaterialTheme.typography.headlineMedium,
                color = BanygTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            // Stats
            Column(
                verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
            ) {
                StatRow(
                    label = stringResource(R.string.csv_import_imported),
                    value = state.importedCount.toString()
                )
                if (state.skippedCount > 0) {
                    StatRow(
                        label = stringResource(R.string.csv_import_skipped),
                        value = state.skippedCount.toString()
                    )
                }
                if (state.duplicateCount > 0) {
                    StatRow(
                        label = stringResource(R.string.csv_import_duplicates_skipped),
                        value = state.duplicateCount.toString()
                    )
                }
            }

            PillButton(
                text = stringResource(R.string.csv_import_done),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = BanygTheme.colors.textPrimary
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
        ) {
            CircularProgressIndicator(
                color = BanygTheme.colors.limeGreen,
                strokeWidth = 2.dp
            )
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = BanygTheme.spacing.small, vertical = BanygTheme.spacing.extraSmall)
    ) {
        content()
    }
}

// Previews

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun CsvImportInitialPreview() {
    BanygTheme {
        CsvImportScreen(
            uiState = CsvImportUiState.Initial,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun CsvImportCompletePreview() {
    BanygTheme {
        CsvImportScreen(
            uiState = CsvImportUiState.ImportComplete(
                importedCount = 25,
                skippedCount = 5,
                duplicateCount = 3
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {}
        )
    }
}
