package com.example.ui.screens.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.components.LoadingContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.domain.CurrencyFormatter
import com.example.domain.FinancialEngine
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.screens.budget.AddEditBudgetDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    onNavigateToAccountDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Accounts State
    val accountBalances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()

    val loadedQueries by viewModel.loadedQueries.collectAsStateWithLifecycle()
    if (!("allAccounts" in loadedQueries && "transactions" in loadedQueries)) { LoadingContent(); return }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var showCreditCardPaymentDialog by remember { mutableStateOf<AccountWithBalance?>(null) }

    val groupedAccounts = remember(accountBalances) {
        accountBalances.groupBy {
            when (it.account.type) {
                AccountType.CASH -> "Cash"
                AccountType.BANK, AccountType.DEBIT_CARD -> "Banks & Accounts"
                AccountType.CREDIT_CARD -> "Credit Cards"
                AccountType.INVESTMENT, AccountType.FIXED_DEPOSIT, AccountType.REAL_ESTATE, AccountType.SAVINGS -> "Investments & Assets"
                AccountType.LOAN, AccountType.OTHER_LIABILITY -> "Loans & Liabilities"
                else -> "Other"
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accounts & Net Worth",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { showAddAccountDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Account"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                    // Net Worth Summary Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Total Net Worth",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatAmount(netWorth.netWorth, currencyCode),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Assets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(
                                            CurrencyFormatter.formatAmount(netWorth.totalAssets, currencyCode),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = IncomeGreen
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Liabilities", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(
                                            CurrencyFormatter.formatAmount(netWorth.totalLiabilities, currencyCode),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (accountBalances.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No Accounts Added",
                                message = "Add your cash wallet, bank accounts, or credit cards to begin tracking.",
                                actionButton = {
                                    Button(onClick = { showAddAccountDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add First Account")
                                    }
                                }
                            )
                        }
                    }

                    // Account Groups
                    groupedAccounts.forEach { (groupName, accList) ->
                        item {
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        items(accList, key = { it.account.id }) { acc ->
                            val isLiability = FinancialEngine.isLiabilityAccount(acc.account.type)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToAccountDetail(acc.account.id) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (acc.account.type) {
                                                            AccountType.CASH -> IncomeGreen.copy(alpha = 0.15f)
                                                            AccountType.CREDIT_CARD -> ExpenseRed.copy(alpha = 0.15f)
                                                            AccountType.BANK -> TransferTeal.copy(alpha = 0.15f)
                                                            AccountType.INVESTMENT -> TealPrimary.copy(alpha = 0.15f)
                                                            else -> MaterialTheme.colorScheme.primaryContainer
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (acc.account.type) {
                                                        AccountType.CASH -> Icons.Default.AccountBalanceWallet
                                                        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                                        AccountType.BANK -> Icons.Default.AccountBalance
                                                        AccountType.INVESTMENT -> Icons.Default.TrendingUp
                                                        AccountType.LOAN -> Icons.Default.MoneyOff
                                                        else -> Icons.Default.AccountBalance
                                                    },
                                                    contentDescription = null,
                                                    tint = when (acc.account.type) {
                                                        AccountType.CASH -> IncomeGreen
                                                        AccountType.CREDIT_CARD -> ExpenseRed
                                                        AccountType.BANK -> TransferTeal
                                                        AccountType.INVESTMENT -> TealPrimary
                                                        else -> MaterialTheme.colorScheme.primary
                                                    }
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = acc.account.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = acc.account.institution.ifBlank { acc.account.type.name },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (isLiability) "Outstanding" else "Balance",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = CurrencyFormatter.formatAmount(acc.calculatedBalance, currencyCode),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isLiability) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Credit Card Specific Info
                                    if (acc.account.type == AccountType.CREDIT_CARD && acc.account.creditLimit > 0) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Available: ${CurrencyFormatter.formatAmount(acc.availableCredit, currencyCode)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Limit: ${CurrencyFormatter.formatAmount(acc.creditLimit, currencyCode)} (${String.format("%.0f", acc.utilizationPercent)}%)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (acc.utilizationPercent > 50) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { (acc.utilizationPercent / 100f).coerceIn(0f, 1f) },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                color = if (acc.utilizationPercent > 50) ExpenseRed else TealPrimary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Statement: Day ${acc.account.statementDay} · Due: Day ${acc.account.paymentDueDay}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                TextButton(onClick = { showCreditCardPaymentDialog = acc }) {
                                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Pay Bill")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
        }
    }

    // Add / Edit Account Dialog
    if (showAddAccountDialog || accountToEdit != null) {
        AddEditAccountDialog(
            account = accountToEdit,
            currencyCode = currencyCode,
            onSave = { acc ->
                viewModel.saveAccount(acc)
                showAddAccountDialog = false
                accountToEdit = null
            },
            onDismiss = {
                showAddAccountDialog = false
                accountToEdit = null
            }
        )
    }

    // Quick Credit Card Payment Dialog
    if (showCreditCardPaymentDialog != null) {
        val cardAcc = showCreditCardPaymentDialog!!
        val bankAccounts = accountBalances.filter { it.account.type == AccountType.BANK }

        CreditCardPaymentDialog(
            cardAccount = cardAcc,
            bankAccounts = bankAccounts,
            currencyCode = currencyCode,
            onConfirmPayment = { bankId, amountMinor ->
                viewModel.saveTransaction(
                    type = TransactionType.TRANSFER,
                    dateMillis = System.currentTimeMillis(),
                    amount = amountMinor,
                    accountId = bankId,
                    toAccountId = cardAcc.account.id,
                    categoryId = 0L,
                    payee = "Card Payment: ${cardAcc.account.name}",
                    note = "Statement settlement"
                )
                showCreditCardPaymentDialog = null
            },
            onDismiss = { showCreditCardPaymentDialog = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    account: Account?,
    currencyCode: String,
    onSave: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(account) { mutableStateOf(account?.name ?: "") }
    var institution by remember(account) { mutableStateOf(account?.institution ?: "") }
    var selectedType by remember(account) { mutableStateOf(account?.type ?: AccountType.BANK) }
    var initialBalanceText by remember(account) {
        mutableStateOf(if (account != null && account.initialBalance != 0L) CurrencyFormatter.toDecimalString(account.initialBalance) else "")
    }
    var creditLimitText by remember(account) {
        mutableStateOf(if (account != null && account.creditLimit != 0L) CurrencyFormatter.toDecimalString(account.creditLimit) else "")
    }
    var statementDayText by remember(account) {
        mutableStateOf(if (account != null) account.statementDay.toString() else "1")
    }
    var dueDayText by remember(account) {
        mutableStateOf(if (account != null) account.paymentDueDay.toString() else "20")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account != null) "Edit Account" else "Add New Account", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name") },
                        placeholder = { Text("e.g. Salary Account, Pocket Cash, Main Card") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = { Text("Bank / Institution (Optional)") },
                        placeholder = { Text("e.g. HDFC Bank, Chase, SBI") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Account Type", style = MaterialTheme.typography.labelMedium)
                    var expandedType by remember { mutableStateOf(false) }

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedType = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (selectedType) {
                                    AccountType.CASH -> "Cash"
                                    AccountType.BANK -> "Bank Account"
                                    AccountType.CREDIT_CARD -> "Credit Card"
                                    AccountType.DEBIT_CARD -> "Debit Card"
                                    AccountType.INVESTMENT -> "Investment / Stocks"
                                    AccountType.SAVINGS -> "Savings"
                                    AccountType.FIXED_DEPOSIT -> "Fixed Deposit"
                                    AccountType.REAL_ESTATE -> "Real Estate / Property"
                                    AccountType.LOAN -> "Loan / Debt"
                                    AccountType.OTHER_LIABILITY -> "Other Liability"
                                    else -> "Other Asset"
                                },
                                fontWeight = FontWeight.Medium
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        listOf(
                            AccountType.CASH to "Cash",
                            AccountType.BANK to "Bank Account",
                            AccountType.CREDIT_CARD to "Credit Card",
                            AccountType.INVESTMENT to "Investment / Stocks",
                            AccountType.SAVINGS to "Savings",
                            AccountType.FIXED_DEPOSIT to "Fixed Deposit",
                            AccountType.LOAN to "Loan / Debt",
                            AccountType.OTHER_ASSET to "Other Asset"
                        ).forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedType = type
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = { initialBalanceText = it },
                        label = { Text("Starting / Initial Balance") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Text(currencyCode, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (selectedType == AccountType.CREDIT_CARD) {
                    item {
                        OutlinedTextField(
                            value = creditLimitText,
                            onValueChange = { creditLimitText = it },
                            label = { Text("Credit Card Limit") },
                            placeholder = { Text("e.g. 100000") },
                            leadingIcon = { Text(currencyCode, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = statementDayText,
                                onValueChange = { statementDayText = it },
                                label = { Text("Bill Day") },
                                placeholder = { Text("1") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = dueDayText,
                                onValueChange = { dueDayText = it },
                                label = { Text("Due Day") },
                                placeholder = { Text("20") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val initBal = CurrencyFormatter.parseToMinorUnits(initialBalanceText)
                    val credLimit = CurrencyFormatter.parseToMinorUnits(creditLimitText)
                    val stDay = statementDayText.toIntOrNull() ?: 1
                    val dDay = dueDayText.toIntOrNull() ?: 20

                    val newOrUpdated = (account ?: Account(name = "", type = selectedType)).copy(
                        name = name.ifBlank { "Account" },
                        institution = institution,
                        type = selectedType,
                        initialBalance = initBal,
                        creditLimit = credLimit,
                        statementDay = stDay.coerceIn(1, 31),
                        paymentDueDay = dDay.coerceIn(1, 31),
                        currency = currencyCode
                    )
                    onSave(newOrUpdated)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreditCardPaymentDialog(
    cardAccount: AccountWithBalance,
    bankAccounts: List<AccountWithBalance>,
    currencyCode: String,
    onConfirmPayment: (bankId: Long, amountMinor: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBankId by remember(bankAccounts) {
        mutableStateOf(bankAccounts.firstOrNull()?.account?.id ?: 0L)
    }

    var paymentAmountText by remember(cardAccount) {
        mutableStateOf(CurrencyFormatter.toDecimalString(cardAccount.calculatedBalance.coerceAtLeast(0L)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay Credit Card Bill", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Paying bill for ${cardAccount.account.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Outstanding Amount: ${CurrencyFormatter.formatAmount(cardAccount.calculatedBalance, currencyCode)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ExpenseRed
                )

                HorizontalDivider()

                Text("Pay From Bank Account", style = MaterialTheme.typography.labelMedium)

                var expandedBank by remember { mutableStateOf(false) }
                val currentBank = bankAccounts.find { it.account.id == selectedBankId }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedBank = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentBank?.account?.name ?: "Select Bank", fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = expandedBank,
                    onDismissRequest = { expandedBank = false }
                ) {
                    bankAccounts.forEach { b ->
                        DropdownMenuItem(
                            text = { Text("${b.account.name} (${CurrencyFormatter.formatAmount(b.calculatedBalance, currencyCode)})") },
                            onClick = {
                                selectedBankId = b.account.id
                                expandedBank = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it },
                    label = { Text("Payment Amount") },
                    leadingIcon = { Text(currencyCode, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor = CurrencyFormatter.parseToMinorUnits(paymentAmountText)
                    if (amountMinor > 0L && selectedBankId != 0L) {
                        onConfirmPayment(selectedBankId, amountMinor)
                    }
                },
                enabled = selectedBankId != 0L && paymentAmountText.isNotBlank()
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
