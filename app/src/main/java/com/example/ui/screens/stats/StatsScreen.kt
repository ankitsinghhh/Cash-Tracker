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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
    MERCHANT_ANALYTICS,
    INSIGHTS
}

private fun chartsForView(view: StatsViewType): Set<InsightWidget> = when (view) {
    StatsViewType.CATEGORY_BREAKDOWN -> setOf(InsightWidget.CATEGORY_DONUT, InsightWidget.CATEGORY_RANKING)
    StatsViewType.SPENDING_TREND -> setOf(InsightWidget.DAILY_TREND, InsightWidget.CUMULATIVE_PACE)
    StatsViewType.PERIOD_COMPARISON -> setOf(InsightWidget.CATEGORY_COMPARISON, InsightWidget.WEEKDAY)
    StatsViewType.CASH_FLOW_TREND -> setOf(InsightWidget.CASH_FLOW)
    StatsViewType.MERCHANT_ANALYTICS -> setOf(InsightWidget.MERCHANT_RANKING)
    StatsViewType.INSIGHTS -> InsightWidget.entries.filter { it.isNew }.toSet()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onNavigateToTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var requestedViewType by rememberSaveable { mutableStateOf(StatsViewType.CATEGORY_BREAKDOWN) }
    val visibleCharts by viewModel.visibleCharts.collectAsStateWithLifecycle()
    val availableViews = remember(visibleCharts) { StatsViewType.entries.filter { chartsForView(it).any(visibleCharts::contains) } }
    val selectedViewType = requestedViewType.takeIf { it in availableViews } ?: availableViews.firstOrNull() ?: StatsViewType.INSIGHTS
    var showChartsDialog by rememberSaveable { mutableStateOf(false) }
    val insights by (if (selectedViewType == StatsViewType.INSIGHTS && availableViews.isNotEmpty()) viewModel.expenseInsights else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(null)
    val insightsError by viewModel.insightsError.collectAsStateWithLifecycle()
    val readErrors by viewModel.readErrors.collectAsStateWithLifecycle()
    val insightReadError = insightsError ?: listOf("allAccounts", "allCategories", "settings").firstNotNullOfOrNull { readErrors[it] }
    val savedViews = rememberSaveableStateHolder()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val periodSummary by viewModel.monthPeriodSummary.collectAsStateWithLifecycle()
    val categorySpendings by (if (selectedViewType == StatsViewType.CATEGORY_BREAKDOWN) viewModel.categorySpendings else kotlinx.coroutines.flow.flowOf(emptyList<CategorySpending>())).collectAsStateWithLifecycle(emptyList<CategorySpending>())
    val categoryIncomes by (if (selectedViewType == StatsViewType.CATEGORY_BREAKDOWN) viewModel.categoryIncomes else kotlinx.coroutines.flow.flowOf(emptyList<CategorySpending>())).collectAsStateWithLifecycle(emptyList<CategorySpending>())
    val merchantStats by (if (selectedViewType == StatsViewType.MERCHANT_ANALYTICS) viewModel.merchantStats else kotlinx.coroutines.flow.flowOf(emptyList<MerchantStat>())).collectAsStateWithLifecycle(emptyList<MerchantStat>())
    val monthlyGroups by (if (selectedViewType == StatsViewType.CASH_FLOW_TREND) viewModel.monthlyHistoricalGroups else kotlinx.coroutines.flow.flowOf(emptyList<MonthlyAggregation>())).collectAsStateWithLifecycle(emptyList<MonthlyAggregation>())
    val dailyTrendData by (if (selectedViewType == StatsViewType.SPENDING_TREND) viewModel.dailySpendingTrend else kotlinx.coroutines.flow.flowOf(DailySpendingTrendData(emptyList(), emptyList(), 0L, 1, 0L, 0, 0L))).collectAsStateWithLifecycle(DailySpendingTrendData(emptyList(), emptyList(), 0L, 1, 0L, 0, 0L))
    val dayOfWeekHabits by (if (selectedViewType == StatsViewType.PERIOD_COMPARISON) viewModel.dayOfWeekDistribution else kotlinx.coroutines.flow.flowOf(emptyList<DayOfWeekSpending>())).collectAsStateWithLifecycle(emptyList<DayOfWeekSpending>())
    val categoryComparisons by (if (selectedViewType == StatsViewType.PERIOD_COMPARISON) viewModel.monthOverMonthCategoryComparison else kotlinx.coroutines.flow.flowOf(emptyList<CategoryComparisonItem>())).collectAsStateWithLifecycle(emptyList<CategoryComparisonItem>())
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val monthData by viewModel.monthPageData.collectAsStateWithLifecycle()
    val monthError by viewModel.homeError.collectAsStateWithLifecycle()
    if (monthError != null) { ErrorContent(monthError!!) { viewModel.retryHome() }; return }
    if (monthData == null) { LoadingContent(); return }

    var selectedTransactionType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var selectedDrilldownCategory by remember { mutableStateOf<CategorySpending?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val activeCategoryItems = if (selectedTransactionType == TransactionType.EXPENSE) categorySpendings else categoryIncomes
    val categoryMaximum = remember(activeCategoryItems) { activeCategoryItems.maxOfOrNull { it.totalAmount } ?: 1L }
    val comparisonMaximum = remember(categoryComparisons) { categoryComparisons.maxOfOrNull { maxOf(it.currentMonthAmount, it.previousMonthAmount) } ?: 1L }
    val totalAmount = if (selectedTransactionType == TransactionType.EXPENSE) periodSummary.totalExpense else periodSummary.totalIncome
    val periodLabel = remember(currentMonth) { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(currentMonth.time) }
    val sectionId = "stats_${currentMonth.get(java.util.Calendar.YEAR)}_${currentMonth.get(java.util.Calendar.MONTH)}_${currencyCode}_${selectedViewType.name}"
    val sectionTitle = when (selectedViewType) {
        StatsViewType.CATEGORY_BREAKDOWN -> "Categories"
        StatsViewType.SPENDING_TREND -> "Daily trend"
        StatsViewType.PERIOD_COMPARISON -> "Comparison"
        StatsViewType.CASH_FLOW_TREND -> "Cash flow"
        StatsViewType.MERCHANT_ANALYTICS -> "Merchants"
        StatsViewType.INSIGHTS -> "Insights"
    }

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
                }

                // Stats Section Tabs
                if (availableViews.isNotEmpty()) ScrollableTabRow(
                    selectedTabIndex = availableViews.indexOf(selectedViewType),
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
                ) {
                    availableViews.forEach { viewType ->
                        Tab(
                            selected = selectedViewType == viewType,
                            onClick = {
                                requestedViewType = viewType
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
                                        StatsViewType.INSIGHTS -> "Insights"
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
        FullscreenableChart(sectionId, "$sectionTitle · $periodLabel", modifier = Modifier.fillMaxSize(), scrollInFullscreen = false, showButton = false) {
        savedViews.SaveableStateProvider("${currentMonth.get(java.util.Calendar.YEAR)}-${currentMonth.get(java.util.Calendar.MONTH)}-${selectedViewType.name}") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (LocalChartFullscreen.current) PaddingValues(0.dp) else innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (availableViews.isEmpty()) {
                item { EmptyStateView("Your charts are hidden", "Choose the charts you want to see.",
                    actionButton = { Button(onClick = { showChartsDialog = true }) { Text("Choose charts") } }) }
            } else
            when (selectedViewType) {
                StatsViewType.INSIGHTS -> {
                    when {
                        insightReadError != null -> item { ErrorContent(insightReadError) { viewModel.retryReads() } }
                        insights == null -> item { LoadingContent("Preparing your insights…") }
                        else -> expenseInsightItems(insights!!, visibleCharts, viewModel::setSmallPurchaseThreshold, onNavigateToTransaction)
                    }
                }
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
                                        .background(if (isSelected) (if (type == TransactionType.EXPENSE) FinancialColors.expense else FinancialColors.income) else Color.Transparent)
                                        .clickable {
                                            selectedTransactionType = type
                                            selectedDrilldownCategory = null
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
                        if (InsightWidget.CATEGORY_DONUT in visibleCharts) item {
                            FullscreenableChart("donut_${sectionId}_${selectedTransactionType.name}", "${selectedTransactionType.name.lowercase().replaceFirstChar { it.uppercase() }} categories · $periodLabel") {
                            DonutPieChart(
                                items = activeCategoryItems,
                                totalAmount = totalAmount,
                                currencyCode = currencyCode,
                                onCategorySelected = { selectedDrilldownCategory = it }
                            )
                            }
                        }

                        // Ranked Category Horizontal Bars with drilldown
                        if (InsightWidget.CATEGORY_RANKING in visibleCharts) {
                        item { Text("Category Ranking", style = MaterialTheme.typography.titleMedium) }
                        items(activeCategoryItems, key = { "rank_${it.category.id}" }, contentType = { "category" }) { category ->
                            RankedCategoryBarChart(
                                items = listOf(category.copy(subcategoryBreakdown = emptyList())),
                                currencyCode = currencyCode,
                                maximumAmount = categoryMaximum,
                                showHeader = false,
                                onCategorySelected = { selectedDrilldownCategory = category }
                            )
                        }
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
                                                    Text(cat.category.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                                    Text("${cat.transactionCount} transactions · ${String.format("%.1f", cat.percentage)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text(
                                                CurrencyFormatter.formatAmount(cat.totalAmount, currencyCode),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }


                                    }
                                }
                            }
                            items(cat.subcategoryBreakdown, key = { "sub_" + it.subcategory.id }, contentType = { "subcategory" }) { sub ->
                                ListItem(headlineContent = { Text(sub.subcategory.name) }, supportingContent = {
                                    Text(CurrencyFormatter.formatAmount(sub.totalAmount, currencyCode))
                                })
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
                            FullscreenableChart("trend_$sectionId", "Daily trend · $periodLabel") {
                            DailySpendingTrendChart(
                                data = dailyTrendData,
                                currencyCode = currencyCode,
                                showDaily = InsightWidget.DAILY_TREND in visibleCharts,
                                showCumulative = InsightWidget.CUMULATIVE_PACE in visibleCharts
                            )
                            }
                        }
                    }
                }

                StatsViewType.CASH_FLOW_TREND -> {
                    item {
                        FullscreenableChart("cash_flow_$sectionId", "Cash flow · $currencyCode") {
                        MonthlyBarChart(monthlyData = monthlyGroups, currencyCode = currencyCode)
                        }
                    }

                    item {
                        Text(
                            text = "Historical Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
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
                                    Text(month.monthLabel, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Savings Rate: ${String.format("%.1f", month.savingsRate)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        CurrencyFormatter.formatAmount(month.balance, currencyCode),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (month.balance >= 0) FinancialColors.income else FinancialColors.expense
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
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
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
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
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = FinancialColors.expense
                                    )
                                }
                            }
                        }
                    }
                }

                StatsViewType.PERIOD_COMPARISON -> {
                    if (InsightWidget.CATEGORY_COMPARISON in visibleCharts) item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "This Month vs Last Month",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
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
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = FinancialColors.expense
                                        )
                                        Text(
                                            "Prev: ${CurrencyFormatter.formatAmount(periodSummary.previousPeriodExpense, currencyCode)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (periodSummary.expenseChangePercent > 0) FinancialColors.expense.copy(alpha = 0.15f) else FinancialColors.income.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${if (periodSummary.expenseChangePercent >= 0) "+" else ""}${String.format("%.1f", periodSummary.expenseChangePercent)}%",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (periodSummary.expenseChangePercent > 0) FinancialColors.expense else FinancialColors.income,
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
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = FinancialColors.income
                                        )
                                        Text(
                                            "Prev: ${CurrencyFormatter.formatAmount(periodSummary.previousPeriodIncome, currencyCode)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (periodSummary.incomeChangePercent >= 0) FinancialColors.income.copy(alpha = 0.15f) else FinancialColors.expense.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${if (periodSummary.incomeChangePercent >= 0) "+" else ""}${String.format("%.1f", periodSummary.incomeChangePercent)}%",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (periodSummary.incomeChangePercent >= 0) FinancialColors.income else FinancialColors.expense,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (InsightWidget.CATEGORY_COMPARISON in visibleCharts && categoryComparisons.isNotEmpty()) {
                        item { Text("Category Shift (vs Last Month)", style = MaterialTheme.typography.titleMedium) }
                        items(categoryComparisons, key = { "comparison_${it.category.id}" }, contentType = { "comparison" }) { comparison ->
                            MonthOverMonthCategoryBarChart(
                                comparisons = listOf(comparison),
                                currencyCode = currencyCode,
                                maximumAmount = comparisonMaximum,
                                showHeader = false
                            )
                        }
                    }

                    if (InsightWidget.WEEKDAY in visibleCharts && dayOfWeekHabits.isNotEmpty() && dayOfWeekHabits.any { it.totalExpense > 0L }) {
                        item {
                            FullscreenableChart("weekday_$sectionId", "Weekday habits · $periodLabel") {
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
        }
        }
    }

    if (showChartsDialog) ChartPreferencesDialog(visibleCharts, viewModel::setChartVisible, viewModel::setAllChartsVisible) { showChartsDialog = false }
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
