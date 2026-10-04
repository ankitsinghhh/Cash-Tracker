package com.example.ui.screens.accounts
import com.example.ui.theme.FinancialColors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Account
import com.example.data.model.TransactionType
import com.example.domain.CurrencyFormatter
import com.example.domain.FinancialEngine
import com.example.ui.MainViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.ui.components.LoadingContent
import com.example.ui.components.ErrorContent
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TransactionItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: MainViewModel,
    accountId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val loaded by viewModel.accountsLoaded.collectAsStateWithLifecycle()
    val accountTransactions = remember(accountId) { viewModel.accountPages(accountId) }.collectAsLazyPagingItems()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val accountBalances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()

    val account = remember(accountId, allAccounts) {
        allAccounts.find { it.id == accountId }
    }

    val accountBalanceInfo = remember(accountId, accountBalances) {
        accountBalances.find { it.account.id == accountId }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (account == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!loaded) LoadingContent() else Text("Account not found")
        }
        return
    }

    val isLiability = FinancialEngine.isLiabilityAccount(account.type)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(account.name, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Account")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = FinancialColors.expense)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Account Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isLiability) "Current Outstanding" else "Current Balance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = CurrencyFormatter.formatAmount(accountBalanceInfo?.calculatedBalance ?: 0L, currencyCode),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isLiability) FinancialColors.expense else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = account.type.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Breakdown Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total In", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                val inAmount = (accountBalanceInfo?.totalIncome ?: 0L) + (accountBalanceInfo?.totalTransferIn ?: 0L)
                                Text(
                                    "+${CurrencyFormatter.formatAmount(inAmount, currencyCode, showDecimals = false)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = FinancialColors.income
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                val outAmount = (accountBalanceInfo?.totalExpense ?: 0L) + (accountBalanceInfo?.totalTransferOut ?: 0L)
                                Text(
                                    "-${CurrencyFormatter.formatAmount(outAmount, currencyCode, showDecimals = false)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = FinancialColors.expense
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Opening Bal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                Text(
                                    CurrencyFormatter.formatAmount(account.initialBalance, currencyCode, showDecimals = false),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction History (${accountTransactions.itemCount} loaded)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            if (accountTransactions.loadState.refresh is LoadState.Loading) {
                item { LoadingContent() }
            } else if (accountTransactions.loadState.refresh is LoadState.Error) {
                item { ErrorContent("Could not load transactions", accountTransactions::retry) }
            } else if (accountTransactions.itemCount == 0) {
                item {
                    EmptyStateView(
                        title = "No Transactions Found",
                        message = "This account doesn't have any recorded transactions yet."
                    )
                }
            } else {
                items(accountTransactions.itemCount, key = accountTransactions.itemKey { it.transaction.id }, contentType = { "transaction" }) { index ->
                    val item = accountTransactions[index] ?: return@items
                    TransactionItemRow(
                        item = item,
                        currencyCode = currencyCode,
                        onClick = { onNavigateToEditTransaction(item.transaction.id) }
                    )
                }
            }
            if (accountTransactions.loadState.append is LoadState.Loading) item { LoadingContent() }
            if (accountTransactions.loadState.append is LoadState.Error) item { ErrorContent("Could not load more", accountTransactions::retry) }
        }
    }

    if (showEditDialog) {
        AddEditAccountDialog(
            account = account,
            currencyCode = currencyCode,
            onSave = { updated ->
                viewModel.saveAccount(updated)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("Are you sure you want to delete '${account.name}'? Existing transactions linked to this account may lose account association.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(account)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinancialColors.expense)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
