package com.example.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BudgetProgress
import com.example.data.model.Category
import com.example.domain.CurrencyFormatter
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonthPickerDialog
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val budgetProgressList by viewModel.currentMonthBudgetsProgress.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val monthlyBudgetGoal by viewModel.monthlyBudgetGoal.collectAsStateWithLifecycle()

    val monthStr = remember(currentMonth) {
        SimpleDateFormat("yyyy-MM", Locale.US).format(currentMonth.time)
    }

    val totalCategoryBudget = remember(budgetProgressList) { budgetProgressList.sumOf { it.budget.amount } }
    val totalSpent = remember(budgetProgressList) { budgetProgressList.sumOf { it.spentAmount } }

    // Use monthly target goal if set, otherwise fallback to sum of category budgets
    val effectiveBudget = if (monthlyBudgetGoal > 0L) monthlyBudgetGoal else totalCategoryBudget
    val totalRemaining = effectiveBudget - totalSpent
    val overallProgress = if (effectiveBudget > 0L) (totalSpent.toFloat() / effectiveBudget.toFloat()) else 0f

    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetProgress?>(null) }
    var showEditTargetGoalDialog by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    MonthSelectorHeader(
                        currentMonthDate = currentMonth,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onMonthClick = { showMonthPicker = true }
                    )
                }
                IconButton(
                    onClick = { showAddBudgetDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Budget")
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
            // Target Monthly Budget & Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (monthlyBudgetGoal > 0L) "Target Monthly Budget" else "Total Monthly Budget",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatAmount(effectiveBudget, currencyCode),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            FilledTonalButton(
                                onClick = { showEditTargetGoalDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (monthlyBudgetGoal > 0L) "Edit Goal" else "Set Target Goal",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { overallProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (overallProgress > 1f) ExpenseRed else if (overallProgress > 0.8f) WarningAmber else TealPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyFormatter.formatAmount(totalSpent, currencyCode),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (overallProgress > 1f) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Usage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", overallProgress * 100)}%",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (overallProgress > 1f) ExpenseRed else MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyFormatter.formatAmount(totalRemaining, currencyCode),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (totalRemaining >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }

                        if (totalCategoryBudget > 0L && monthlyBudgetGoal > 0L && totalCategoryBudget != monthlyBudgetGoal) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Category limits total: ${CurrencyFormatter.formatAmount(totalCategoryBudget, currencyCode)} (${budgetProgressList.size} categories)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Category Budgets Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Budgets (${budgetProgressList.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { showAddBudgetDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Limit")
                    }
                }
            }

            if (budgetProgressList.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No Category Budgets Set",
                        message = "Set monthly spending limits for categories like Food, Shopping, or Bills to control your expenses.",
                        actionButton = {
                            Button(onClick = { showAddBudgetDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Category Budget")
                            }
                        }
                    )
                }
            } else {
                items(budgetProgressList, key = { it.budget.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { budgetToEdit = item },
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
                                    CategoryIconBadge(
                                        iconName = item.category?.iconName ?: "category",
                                        colorHex = item.category?.colorHex ?: "#0D9488",
                                        size = 38.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = item.category?.name ?: "All Expenses",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (item.isOverBudget) "Over budget by ${CurrencyFormatter.formatAmount(-item.remainingAmount, currencyCode)}"
                                            else "Left: ${CurrencyFormatter.formatAmount(item.remainingAmount, currencyCode)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (item.isOverBudget) ExpenseRed else IncomeGreen
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", item.utilizationPercent)}%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (item.isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatAmount(item.spentAmount, currencyCode, showDecimals = false)} / ${CurrencyFormatter.formatAmount(item.budget.amount, currencyCode, showDecimals = false)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { (item.utilizationPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (item.isOverBudget) ExpenseRed else if (item.utilizationPercent > 80f) WarningAmber else GeoPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Month Picker Dialog
    if (showMonthPicker) {
        MonthPickerDialog(
            currentCalendar = currentMonth,
            onMonthPicked = { year, month ->
                viewModel.setMonth(year, month)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    // Edit Target Monthly Budget Goal Dialog
    if (showEditTargetGoalDialog) {
        var budgetInput by remember {
            mutableStateOf(if (monthlyBudgetGoal > 0L) (monthlyBudgetGoal / 100).toString() else "")
        }

        AlertDialog(
            onDismissRequest = { showEditTargetGoalDialog = false },
            title = { Text("Target Monthly Budget Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Set an overall monthly spending ceiling to guide your overall finances and savings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Target Monthly Budget (${CurrencyFormatter.getCurrencySymbol(currencyCode).trim()})") },
                        placeholder = { Text("e.g. 50000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = CurrencyFormatter.parseToMinorUnits(budgetInput)
                        viewModel.setMonthlyBudgetGoal(amount)
                        showEditTargetGoalDialog = false
                    }
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                Row {
                    if (monthlyBudgetGoal > 0L) {
                        TextButton(
                            onClick = {
                                viewModel.setMonthlyBudgetGoal(0L)
                                showEditTargetGoalDialog = false
                            }
                        ) {
                            Text("Clear", color = ExpenseRed)
                        }
                    }
                    TextButton(onClick = { showEditTargetGoalDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Add / Edit Category Budget Dialog
    if (showAddBudgetDialog || budgetToEdit != null) {
        val existing = budgetToEdit
        AddEditBudgetDialog(
            existingBudget = existing,
            categories = categories.filter { it.type == com.example.data.model.TransactionType.EXPENSE && it.parentId == null },
            monthStr = monthStr,
            currencyCode = currencyCode,
            onSave = { catId, amountMinor ->
                viewModel.saveBudget(catId, monthStr, amountMinor)
                showAddBudgetDialog = false
                budgetToEdit = null
            },
            onDelete = {
                if (existing != null) {
                    viewModel.deleteBudget(existing.budget)
                }
                showAddBudgetDialog = false
                budgetToEdit = null
            },
            onDismiss = {
                showAddBudgetDialog = false
                budgetToEdit = null
            }
        )
    }
}

@Composable
fun AddEditBudgetDialog(
    existingBudget: BudgetProgress?,
    categories: List<Category>,
    monthStr: String,
    currencyCode: String,
    onSave: (categoryId: Long, amountMinor: Long) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by remember(existingBudget, categories) {
        mutableStateOf(existingBudget?.budget?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }

    var amountText by remember(existingBudget) {
        mutableStateOf(if (existingBudget != null) CurrencyFormatter.toDecimalString(existingBudget.budget.amount) else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingBudget != null) "Edit Category Budget" else "New Category Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Category", style = MaterialTheme.typography.labelMedium)

                var expandedCategory by remember { mutableStateOf(false) }
                val currentCategory = categories.find { it.id == selectedCategoryId }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedCategory = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentCategory?.name ?: "All Expenses (Overall)")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Expenses (Overall)") },
                        onClick = {
                            selectedCategoryId = 0L
                            expandedCategory = false
                        }
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                selectedCategoryId = cat.id
                                expandedCategory = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monthly Budget Limit (${CurrencyFormatter.getCurrencySymbol(currencyCode).trim()})") },
                    placeholder = { Text("e.g. 15000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = CurrencyFormatter.parseToMinorUnits(amountText)
                    if (minor > 0) {
                        onSave(selectedCategoryId, minor)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (existingBudget != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = ExpenseRed)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
