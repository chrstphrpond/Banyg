package com.banyg.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banyg.domain.model.Account
import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.ui.components.OutlinedPillButton
import com.banyg.ui.components.PillButton
import com.banyg.ui.theme.BanygTheme
import java.time.Instant

@Composable
fun AddTransactionRoute(
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AddTransactionUiState.Saved) {
            onNavigateBack()
        }
    }

    AddTransactionScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun AddTransactionScreen(
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BanygTheme.colors.backgroundDark,
        topBar = {
            TopAppBar(
                title = { Text(text = "New transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BanygTheme.colors.backgroundDark,
                    titleContentColor = BanygTheme.colors.textPrimary,
                    navigationIconContentColor = BanygTheme.colors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            AddTransactionUiState.Loading -> AddTransactionLoading(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is AddTransactionUiState.Editing -> AddTransactionForm(
                state = uiState,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is AddTransactionUiState.Error -> AddTransactionError(
                message = uiState.message,
                canRetry = uiState.canRetry,
                onRetry = { onEvent(AddTransactionUiEvent.OnRetry) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is AddTransactionUiState.Saved -> {
                Spacer(modifier = Modifier.padding(paddingValues))
            }
        }
    }
}

@Composable
private fun AddTransactionForm(
    state: AddTransactionUiState.Editing,
    onEvent: (AddTransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(PaddingValues(BanygTheme.spacing.screenPadding))
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        Text(
            text = "Capture it now, review later",
            style = MaterialTheme.typography.headlineSmall,
            color = BanygTheme.colors.textPrimary
        )

        Text(
            text = "This transaction lands in Inbox status until you clear it.",
            style = MaterialTheme.typography.bodySmall,
            color = BanygTheme.colors.textSecondary
        )

        AccountDropdown(
            accounts = state.accounts,
            selectedAccountId = state.selectedAccountId,
            onSelected = { onEvent(AddTransactionUiEvent.OnAccountSelected(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        DirectionToggle(
            selected = state.direction,
            onSelected = { onEvent(AddTransactionUiEvent.OnDirectionChanged(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.amountInput,
            onValueChange = { onEvent(AddTransactionUiEvent.OnAmountChanged(it)) },
            label = { Text("Amount") },
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.merchantInput,
            onValueChange = { onEvent(AddTransactionUiEvent.OnMerchantChanged(it)) },
            label = { Text("Merchant (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.memoInput,
            onValueChange = { onEvent(AddTransactionUiEvent.OnMemoChanged(it)) },
            label = { Text("Memo (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = BanygTheme.colors.errorRed
            )
        }

        PillButton(
            text = if (state.isSaving) "Saving..." else "Save to Inbox",
            onClick = { onEvent(AddTransactionUiEvent.OnSave) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedAccountId }
    val label = selected?.let { "${it.name} - ${accountTypeLabel(it.type)}" } ?: "Select account"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Account") },
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
                    text = { Text("${account.name} - ${account.currency.code}") },
                    onClick = {
                        onSelected(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DirectionToggle(
    selected: TransactionDirection,
    onSelected: (TransactionDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
    ) {
        val expenseSelected = selected == TransactionDirection.EXPENSE
        val incomeSelected = selected == TransactionDirection.INCOME

        if (expenseSelected) {
            PillButton(
                text = "Expense",
                onClick = { onSelected(TransactionDirection.EXPENSE) },
                modifier = Modifier.weight(1f)
            )
        } else {
            OutlinedPillButton(
                text = "Expense",
                onClick = { onSelected(TransactionDirection.EXPENSE) },
                modifier = Modifier.weight(1f)
            )
        }

        if (incomeSelected) {
            PillButton(
                text = "Income",
                onClick = { onSelected(TransactionDirection.INCOME) },
                modifier = Modifier.weight(1f)
            )
        } else {
            OutlinedPillButton(
                text = "Income",
                onClick = { onSelected(TransactionDirection.INCOME) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddTransactionLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(BanygTheme.spacing.screenPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        Text(
            text = "Preparing transaction form",
            style = MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textSecondary
        )
    }
}

@Composable
private fun AddTransactionError(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(BanygTheme.spacing.screenPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textSecondary
        )
        if (canRetry) {
            OutlinedPillButton(text = "Retry", onClick = onRetry)
        }
    }
}

private fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.CHECKING -> "Checking"
    AccountType.SAVINGS -> "Savings"
    AccountType.CREDIT_CARD -> "Credit card"
    AccountType.CASH -> "Cash"
    AccountType.E_WALLET -> "E-wallet"
    AccountType.INVESTMENT -> "Investment"
    AccountType.LOAN -> "Loan"
    AccountType.OTHER -> "Other"
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun AddTransactionPreview() {
    BanygTheme {
        AddTransactionScreen(
            uiState = AddTransactionUiState.Editing(
                accounts = listOf(
                    Account(
                        id = "1",
                        name = "City Savings",
                        type = AccountType.SAVINGS,
                        currency = Currency.PHP,
                        openingBalance = Money(250_000, Currency.PHP),
                        currentBalance = Money(312_500, Currency.PHP),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                ),
                selectedAccountId = "1"
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
