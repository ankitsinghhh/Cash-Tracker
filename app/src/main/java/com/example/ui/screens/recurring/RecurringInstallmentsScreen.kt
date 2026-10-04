package com.example.ui.screens.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.MainViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.components.LoadingContent
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class RecurringTab {
    RECURRING_SUBSCRIPTIONS,
    INSTALLMENTS_EMI
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringInstallmentsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by rememberSaveable { mutableStateOf(RecurringTab.RECURRING_SUBSCRIPTIONS) }
    val recurringList by viewModel.allRecurring.collectAsStateWithLifecycle()
    val installmentsList by viewModel.allInstallments.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()

    val loadedQueries by viewModel.loadedQueries.collectAsStateWithLifecycle()
    if ("recurring" !in loadedQueries || "installments" !in loadedQueries) { LoadingContent(); return }

    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showAddInstallmentDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = { Text("Subscriptions & EMIs", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = {
                            if (activeTab == RecurringTab.RECURRING_SUBSCRIPTIONS) showAddRecurringDialog = true
                            else showAddInstallmentDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == RecurringTab.RECURRING_SUBSCRIPTIONS,
                        onClick = { activeTab = RecurringTab.RECURRING_SUBSCRIPTIONS },
                        text = { Text("Recurring (${recurringList.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = activeTab == RecurringTab.INSTALLMENTS_EMI,
                        onClick = { activeTab = RecurringTab.INSTALLMENTS_EMI },
                        text = { Text("Installments & EMIs (${installmentsList.size})", fontWeight = FontWeight.SemiBold) }
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
            if (activeTab == RecurringTab.RECURRING_SUBSCRIPTIONS) {
                if (recurringList.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No Recurring Subscriptions",
                            message = "Automate bills, Netflix/Spotify subscriptions, gym fees, or recurring salary.",
                            actionButton = {
                                Button(onClick = { showAddRecurringDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Subscription")
                                }
                            }
                        )
                    }
                } else {
                    items(recurringList, key = { it.id }) { item ->
                        val acc = accounts.find { it.id == item.accountId }
                        val cat = categories.find { it.id == item.categoryId }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = item.payee.ifBlank { cat?.name ?: "Recurring Item" },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${item.frequency.name} · ${acc?.name ?: "Account"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = CurrencyFormatter.formatAmount(item.amount, currencyCode),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (item.type == TransactionType.EXPENSE) FinancialColors.expense else FinancialColors.income
                                        )
                                        Text(
                                            text = "Next: ${dateFormat.format(Date(item.nextDueDateMillis))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = item.isActive,
                                            onCheckedChange = { active ->
                                                viewModel.saveRecurring(item.copy(isActive = active))
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (item.isActive) "Active" else "Paused", style = MaterialTheme.typography.labelMedium)
                                    }

                                    IconButton(onClick = { viewModel.deleteRecurring(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FinancialColors.expense)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Installments / EMI Tab
                if (installmentsList.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No Installment Plans",
                            message = "Track credit card EMI purchases, gadget loans, and no-cost EMIs with remaining counts.",
                            actionButton = {
                                Button(onClick = { showAddInstallmentDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add EMI Plan")
                                }
                            }
                        )
                    }
                } else {
                    items(installmentsList, key = { it.id }) { plan ->
                        val acc = accounts.find { it.id == plan.accountId }
                        val progress = plan.paidInstallments.toFloat() / plan.totalInstallments.toFloat()
                        val remainingAmount = plan.monthlyAmount * (plan.totalInstallments - plan.paidInstallments)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(plan.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            "Total: ${CurrencyFormatter.formatAmount(plan.totalAmount, currencyCode)} · ${acc?.name ?: "Account"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${CurrencyFormatter.formatAmount(plan.monthlyAmount, currencyCode)}/mo",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = FinancialColors.expense
                                        )
                                        Text(
                                            "Paid ${plan.paidInstallments} of ${plan.totalInstallments}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = FinancialColors.income,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Remaining: ${CurrencyFormatter.formatAmount(remainingAmount, currencyCode)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row {
                                        if (plan.paidInstallments < plan.totalInstallments) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.saveInstallment(
                                                        plan.copy(
                                                            paidInstallments = plan.paidInstallments + 1,
                                                            isCompleted = (plan.paidInstallments + 1 >= plan.totalInstallments)
                                                        )
                                                    )
                                                }
                                            ) {
                                                Text("Pay EMI (+1)")
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteInstallment(plan) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FinancialColors.expense)
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

    // Add Recurring Dialog
    if (showAddRecurringDialog) {
        AddRecurringDialog(
            accounts = accounts,
            categories = categories,
            currencyCode = currencyCode,
            onSave = { rec ->
                viewModel.saveRecurring(rec)
                showAddRecurringDialog = false
            },
            onDismiss = { showAddRecurringDialog = false }
        )
    }

    // Add Installment Dialog
    if (showAddInstallmentDialog) {
        AddInstallmentDialog(
            accounts = accounts,
            categories = categories,
            currencyCode = currencyCode,
            onSave = { inst ->
                viewModel.saveInstallment(inst)
                showAddInstallmentDialog = false
            },
            onDismiss = { showAddInstallmentDialog = false }
        )
    }
}

@Composable
fun AddRecurringDialog(
    accounts: List<Account>,
    categories: List<Category>,
    currencyCode: String,
    onSave: (RecurringTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Recurring Subscription", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name / Payee") },
                    placeholder = { Text("e.g. Netflix, Rent, WiFi") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (${CurrencyFormatter.getCurrencySymbol(currencyCode).trim()})") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Frequency", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(RecurringFrequency.DAILY, RecurringFrequency.WEEKLY, RecurringFrequency.MONTHLY, RecurringFrequency.YEARLY).forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.name.take(3)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = CurrencyFormatter.parseToMinorUnits(amountText)
                    if (name.isNotBlank() && minor > 0) {
                        onSave(
                            RecurringTransaction(
                                name = name,
                                type = TransactionType.EXPENSE,
                                amount = minor,
                                accountId = selectedAccountId,
                                categoryId = selectedCategoryId,
                                payee = name,
                                frequency = frequency,
                                startDateMillis = System.currentTimeMillis(),
                                nextDueDateMillis = System.currentTimeMillis() + 86400000L * 30L
                            )
                        )
                    }
                }
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
fun AddInstallmentDialog(
    accounts: List<Account>,
    categories: List<Category>,
    currencyCode: String,
    onSave: (InstallmentPlan) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var totalAmountText by remember { mutableStateOf("") }
    var monthsText by remember { mutableStateOf("6") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New EMI Installment Plan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product / Plan Name") },
                    placeholder = { Text("e.g. iPhone 15 EMI, Laptop") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it },
                    label = { Text("Total Amount (${CurrencyFormatter.getCurrencySymbol(currencyCode).trim()})") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = monthsText,
                    onValueChange = { monthsText = it },
                    label = { Text("Tenure (Number of Months)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalMinor = CurrencyFormatter.parseToMinorUnits(totalAmountText)
                    val months = monthsText.toIntOrNull() ?: 6
                    if (name.isNotBlank() && totalMinor > 0 && months > 0) {
                        val monthlyMinor = totalMinor / months
                        onSave(
                            InstallmentPlan(
                                name = name,
                                totalAmount = totalMinor,
                                monthlyAmount = monthlyMinor,
                                totalInstallments = months,
                                paidInstallments = 0,
                                accountId = selectedAccountId,
                                categoryId = selectedCategoryId,
                                startDateMillis = System.currentTimeMillis(),
                                nextDueDateMillis = System.currentTimeMillis() + 86400000L * 30L
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
