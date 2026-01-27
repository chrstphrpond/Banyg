package com.banyg.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.ui.components.PillButton
import com.banyg.ui.components.OutlinedPillButton
import com.banyg.ui.theme.BanygTheme

@Composable
fun AccountRegisterRoute(
    onNavigateBack: () -> Unit,
    viewModel: AccountRegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AccountRegisterUiState.Saved) {
            onNavigateBack()
        }
    }

    AccountRegisterScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountRegisterScreen(
    uiState: AccountRegisterUiState,
    onEvent: (AccountRegisterUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BanygTheme.colors.backgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New account",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
            AccountRegisterUiState.Loading -> AccountRegisterLoading(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is AccountRegisterUiState.Editing -> AccountRegisterForm(
                state = uiState,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is AccountRegisterUiState.Error -> AccountRegisterError(
                message = uiState.message,
                canRetry = uiState.canRetry,
                onRetry = { onEvent(AccountRegisterUiEvent.OnRetry) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is AccountRegisterUiState.Saved -> {
                Spacer(modifier = Modifier.padding(paddingValues))
            }
        }
    }
}

@Composable
private fun AccountRegisterForm(
    state: AccountRegisterUiState.Editing,
    onEvent: (AccountRegisterUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(PaddingValues(BanygTheme.spacing.screenPadding))
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        Text(
            text = "Set up your account",
            style = MaterialTheme.typography.headlineSmall,
            color = BanygTheme.colors.textPrimary
        )

        Text(
            text = "Opening balance helps calibrate your starting point.",
            style = MaterialTheme.typography.bodySmall,
            color = BanygTheme.colors.textSecondary
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(AccountRegisterUiEvent.OnNameChanged(it)) },
            label = { Text("Account name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        AccountTypeDropdown(
            selected = state.type,
            onSelected = { onEvent(AccountRegisterUiEvent.OnTypeSelected(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        CurrencyDropdown(
            selected = state.currency,
            onSelected = { onEvent(AccountRegisterUiEvent.OnCurrencySelected(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.openingBalanceInput,
            onValueChange = { onEvent(AccountRegisterUiEvent.OnOpeningBalanceChanged(it)) },
            label = { Text("Opening balance") },
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            text = if (state.isSaving) "Saving..." else "Create account",
            onClick = { onEvent(AccountRegisterUiEvent.OnSave) },
            enabled = state.name.isNotBlank() && !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AccountRegisterLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(BanygTheme.spacing.screenPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        Text(
            text = "Preparing account form",
            style = MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textSecondary
        )
    }
}

@Composable
private fun AccountRegisterError(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTypeDropdown(
    selected: AccountType,
    onSelected: (AccountType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = accountTypeLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text("Account type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AccountType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(accountTypeLabel(type)) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: Currency,
    onSelected: (Currency) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${selected.code} - ${selected.symbol}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Currency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Currency.supportedCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text("${currency.code} - ${currency.symbol}") },
                    onClick = {
                        onSelected(currency)
                        expanded = false
                    }
                )
            }
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
private fun AccountRegisterPreview() {
    BanygTheme {
        AccountRegisterScreen(
            uiState = AccountRegisterUiState.Editing(),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
