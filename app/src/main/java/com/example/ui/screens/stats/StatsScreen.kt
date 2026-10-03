package com.example.ui.screens.stats

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
import com.example.ui.components.*
import com.example.ui.theme.*

enum class StatsViewType {
    CATEGORY_BREAKDOWN,
    SPENDING_TREND,
    PERIOD_COMPARISON,
    CASH_FLOW_TREND,
    MERCHANT_ANALYTICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onNavigateToTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val periodSummary by viewModel.monthPeriodSummary.collectAsStateWithLifecycle()
    val categorySpendings by viewModel.categorySpendings.collectAsStateWithLifecycle()
    val categoryIncomes by viewModel.categoryIncomes.collectAsStateWithLifecycle()
    val merchantStats by viewModel.merchantStats.collectAsStateWithLifecycle()
    val monthlyGroups by viewModel.monthlyHistoricalGroups.collectAsStateWithLifecycle()
    val dailyTrendData by viewModel.dailySpendingTrend.collectAsStateWithLifecycle()
    val dayOfWeekHabits by viewModel.dayOfWeekDistribution.collectAsStateWithLifecycle()
    val categoryComparisons by viewModel.monthOverMonthCategoryComparison.collectAsStateWithLifecycle()
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()

    var selectedViewType by remember { mutableStateOf(StatsViewType.CATEGORY_BREAKDOWN) }
    var selectedTransactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedDrilldownCategory by remember { mutableStateOf<CategorySpending?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val activeCategoryItems = if (selectedTransactionType == TransactionType.EXPENSE) categorySpendings else categoryIncomes
    val totalAmount = if (selectedTransactionType == TransactionType.EXPENSE) periodSummary.totalExpense else periodSummary.totalIncome

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Compact Header Row matching Home Transaction screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonthSelectorHeader(
                        currentMonthDate = currentMonth,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onMonthClick = { showMonthPicker = true }
                    )
                    Text(
                        text = "Stats",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                // Stats Section Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedViewType.ordinal,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
                ) {
                    StatsViewType.values().forEach { viewType ->
                        Tab(
                            selected = selectedViewType == viewType,
                            onClick = {
                                selectedViewType = viewType
                                selectedDrilldownCategory = null
                            },
                            text = {
                                Text(
                                    text = when (viewType) {
                                        StatsViewType.CATEGORY_BREAKDOWN -> "Categories"
                                        StatsViewType.SPENDING_TREND -> "Daily Trend"
                                        StatsViewType.PERIOD_COMPARISON -> "Comparison"
                                        StatsViewType.CASH_FLOW_TREND -> "Cash Flow"
                                        StatsViewType.MERCHANT_ANALYTICS -> "Merchants"
                                    },
                                    fontWeight = if (selectedViewType == viewType) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
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
            when (selectedViewType) {
                StatsViewType.CATEGORY_BREAKDOWN -> {
                    // Type toggle (Expense vs Income)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                                val isSelected = selectedTransactionType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) (if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen) else Color.Transparent)
                                        .clickable {
                                            selectedTransactionType = type
                                            selectedDrilldownCategory = null
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (activeCategoryItems.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No ${selectedTransactionType.name.lowercase().capitalize()} Data",
                                message = "No records found for the selected month."
                            )
                        }
                    } else {
                        // Donut Chart
                        item {
                            DonutPieChart(
                                items = activeCategoryItems,
                                totalAmount = totalAmount,
                                currencyCode = currencyCode,
                                onCategorySelected = { selectedDrilldownCategory = it }
                            )
                        }

                        // Ranked Category Horizontal Bars with drilldown
                        item {
                            RankedCategoryBarChart(
                                items = activeCategoryItems,
                                currencyCode = currencyCode,
                                onCategorySelected = { selectedDrilldownCategory = it }
                            )
                        }

                        // Drilldown info if category selected
                        if (selectedDrilldownCategory != null) {
                            val cat = selectedDrilldownCategory!!
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CategoryIconBadge(iconName = cat.category.iconName, colorHex = cat.category.colorHex, size = 36.dp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(cat.category.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                                    Text("${cat.transactionCount} transactions · ${String.format("%.1f", cat.percentage)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text(
                                                CurrencyFormatter.formatAmount(cat.totalAmount, currencyCode),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (cat.subcategoryBreakdown.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Subcategories", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            cat.subcategoryBreakdown.forEach { sub ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(sub.subcategory.name, style = MaterialTheme.typography.bodyMedium)
                                                    Text(
                                                        "${CurrencyFormatter.formatAmount(sub.totalAmount, currencyCode)} (${String.format("%.1f", sub.percentage)}%)",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                StatsViewType.SPENDING_TREND -> {
                    if (dailyTrendData.days.isEmpty() || dailyTrendData.totalExpense == 0L) {
                        item {
                            EmptyStateView(
                                title = "No Daily Expenses",
                                message = "Add expense transactions for the selected month to view daily spending velocity, 7-day rolling trends, and run-rate pacing."
                            )
                        }
                    } else {
                        item {
                            DailySpendingTrendChart(
                                data = dailyTrendData,
                                currencyCode = currencyCode
                            )
                        }
                    }
                }

                StatsViewType.CASH_FLOW_TREND -> {
                    item {
                        MonthlyBarChart(monthlyData = monthlyGroups, currencyCode = currencyCode)
                    }

                    item {
                        Text(
                            text = "Historical Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(monthlyGroups) { month ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(month.monthLabel, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("Savings Rate: ${String.format("%.1f", month.savingsRate)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        CurrencyFormatter.formatAmount(month.balance, currencyCode),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (month.balance >= 0) IncomeGreen else ExpenseRed
                                    )
                                    Text(
                                        "In: ${CurrencyFormatter.formatAmount(month.totalIncome, currencyCode, showDecimals = false)} · Out: ${CurrencyFormatter.formatAmount(month.totalExpense, currencyCode, showDecimals = false)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                StatsViewType.MERCHANT_ANALYTICS -> {
                    item {
                        Text(
                            text = "Top Payees & Merchants",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (merchantStats.isEmpty()) {
                        item {
                            EmptyStateView(title = "No Merchant Data", message = "Add merchant names to expenses to view analytics.")
                        }
                    } else {
                        items(merchantStats) { merchant ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                merchant.merchantName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                "${merchant.transactionCount} times · Avg ${CurrencyFormatter.formatAmount(merchant.averageSpent, currencyCode)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        CurrencyFormatter.formatAmount(merchant.totalSpent, currencyCode),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }

                StatsViewType.PERIOD_COMPARISON -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "This Month vs Last Month",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Spending comparison row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Monthly Expenses", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            CurrencyFormatter.formatAmount(periodSummary.totalExpense, currencyCode),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ExpenseRed
                                        )
                                        Text(
                                            "Prev: ${CurrencyFormatter.formatAmount(periodSummary.previousPeriodExpense, currencyCode)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (periodSummary.expenseChangePercent > 0) ExpenseRed.copy(alpha = 0.15f) else IncomeGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${if (periodSummary.expenseChangePercent >= 0) "+" else ""}${String.format("%.1f", periodSummary.expenseChangePercent)}%",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (periodSummary.expenseChangePercent > 0) ExpenseRed else IncomeGreen,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                // Income comparison row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Monthly Income", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            CurrencyFormatter.formatAmount(periodSummary.totalIncome, currencyCode),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = IncomeGreen
                                        )
                                        Text(
                                            "Prev: ${CurrencyFormatter.formatAmount(periodSummary.previousPeriodIncome, currencyCode)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (periodSummary.incomeChangePercent >= 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${if (periodSummary.incomeChangePercent >= 0) "+" else ""}${String.format("%.1f", periodSummary.incomeChangePercent)}%",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (periodSummary.incomeChangePercent >= 0) IncomeGreen else ExpenseRed,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (categoryComparisons.isNotEmpty()) {
                        item {
                            MonthOverMonthCategoryBarChart(
                                comparisons = categoryComparisons,
                                currencyCode = currencyCode
                            )
                        }
                    }

                    if (dayOfWeekHabits.isNotEmpty() && dayOfWeekHabits.any { it.totalExpense > 0L }) {
                        item {
                            DayOfWeekHabitsChart(
                                habits = dayOfWeekHabits,
                                currencyCode = currencyCode
                            )
                        }
                    }
                }
            }
        }
    }

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
}
