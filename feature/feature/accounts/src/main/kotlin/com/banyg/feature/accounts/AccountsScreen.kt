package com.banyg.feature.accounts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banyg.domain.model.Account
import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.ui.components.GradientCard
import com.banyg.ui.components.OutlinedPillButton
import com.banyg.ui.components.PillButton
import com.banyg.ui.components.SubtleGradientCard
import com.banyg.ui.format.format
import com.banyg.ui.theme.BanygGradients
import com.banyg.ui.theme.BanygTheme
import java.time.Instant

@Composable
fun AccountsRoute(
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AccountsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToAddAccount = onNavigateToAddAccount,
        onNavigateToAddTransaction = onNavigateToAddTransaction
    )
}

@Composable
private fun AccountsScreen(
    uiState: AccountsUiState,
    onEvent: (AccountsUiEvent) -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BanygTheme.colors.backgroundDark
    ) { paddingValues ->
        when (uiState) {
            AccountsUiState.Loading -> AccountsLoading(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is AccountsUiState.Error -> AccountsError(
                message = uiState.message,
                canRetry = uiState.canRetry,
                onRetry = { onEvent(AccountsUiEvent.OnRetry) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is AccountsUiState.Success -> AccountsContent(
                accounts = uiState.accounts,
                onNavigateToAddAccount = onNavigateToAddAccount,
                onNavigateToAddTransaction = onNavigateToAddTransaction,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun AccountsContent(
    accounts: List<Account>,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = BanygTheme.spacing.screenPadding,
            vertical = BanygTheme.spacing.regular
        ),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        item {
            AccountsHeader(
                accountCount = accounts.size,
                onNavigateToAddAccount = onNavigateToAddAccount,
                onNavigateToAddTransaction = onNavigateToAddTransaction
            )
        }

        if (accounts.isEmpty()) {
            item {
                AccountsEmptyState(
                    onNavigateToAddAccount = onNavigateToAddAccount
                )
            }
        } else {
            items(accounts, key = { it.id }) { account ->
                AccountCard(account = account)
            }
        }
    }
}

@Composable
private fun AccountsHeader(
    accountCount: Int,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        Text(
            text = "Accounts",
            style = MaterialTheme.typography.headlineLarge,
            color = BanygTheme.colors.textPrimary
        )

        Text(
            text = "Capture fast and review in Inbox when ready.",
            style = MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textSecondary
        )

        GradientCard(
            gradient = BanygGradients.DarkDepth,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)) {
                Text(
                    text = "Active accounts",
                    style = MaterialTheme.typography.titleMedium,
                    color = BanygTheme.colors.textSecondary
                )
                Text(
                    text = if (accountCount == 0) "No accounts yet" else "$accountCount accounts",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BanygTheme.colors.textPrimary
                )
                Text(
                    text = "New transactions land in Inbox status",
                    style = MaterialTheme.typography.bodySmall,
                    color = BanygTheme.colors.textTertiary
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedPillButton(
                text = "Add account",
                onClick = onNavigateToAddAccount,
                modifier = Modifier.weight(1f)
            )
            PillButton(
                text = "Add transaction",
                onClick = onNavigateToAddTransaction,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Default.Add
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: Account,
    modifier: Modifier = Modifier
) {
    SubtleGradientCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                color = BanygTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${accountTypeLabel(account.type)} - ${account.currency.code}",
                style = MaterialTheme.typography.bodySmall,
                color = BanygTheme.colors.textSecondary
            )
            Text(
                text = account.currentBalance.format(),
                style = MaterialTheme.typography.headlineMedium,
                color = BanygTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun AccountsEmptyState(
    onNavigateToAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    GradientCard(
        gradient = BanygGradients.SubtleDark,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)) {
            Text(
                text = "Start with your first account",
                style = MaterialTheme.typography.titleMedium,
                color = BanygTheme.colors.textPrimary
            )
            Text(
                text = "Add a checking, cash, or card account to begin tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = BanygTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(BanygTheme.spacing.small))
            PillButton(
                text = "Create account",
                onClick = onNavigateToAddAccount,
                leadingIcon = Icons.Default.Add
            )
        }
    }
}

@Composable
private fun AccountsLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(BanygTheme.spacing.screenPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        Text(
            text = "Accounts",
            style = MaterialTheme.typography.headlineLarge,
            color = BanygTheme.colors.textPrimary
        )

        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
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
                text = "Loading accounts",
                style = MaterialTheme.typography.bodySmall,
                color = BanygTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun AccountsError(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular),
            modifier = Modifier.padding(BanygTheme.spacing.screenPadding)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = BanygTheme.colors.textSecondary
            )
            if (canRetry) {
                OutlinedPillButton(
                    text = "Retry",
                    onClick = onRetry
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
private fun AccountsLoadingPreview() {
    BanygTheme {
        AccountsScreen(
            uiState = AccountsUiState.Loading,
            onEvent = {},
            onNavigateToAddAccount = {},
            onNavigateToAddTransaction = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun AccountsEmptyPreview() {
    BanygTheme {
        AccountsScreen(
            uiState = AccountsUiState.Success(accounts = emptyList()),
            onEvent = {},
            onNavigateToAddAccount = {},
            onNavigateToAddTransaction = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun AccountsSuccessPreview() {
    BanygTheme {
        AccountsScreen(
            uiState = AccountsUiState.Success(
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
                    ),
                    Account(
                        id = "2",
                        name = "Daily Cash",
                        type = AccountType.CASH,
                        currency = Currency.PHP,
                        openingBalance = Money(5_000, Currency.PHP),
                        currentBalance = Money(3_200, Currency.PHP),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                )
            ),
            onEvent = {},
            onNavigateToAddAccount = {},
            onNavigateToAddTransaction = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun AccountsErrorPreview() {
    BanygTheme {
        AccountsScreen(
            uiState = AccountsUiState.Error(message = "Unable to load accounts"),
            onEvent = {},
            onNavigateToAddAccount = {},
            onNavigateToAddTransaction = {}
        )
    }
}
