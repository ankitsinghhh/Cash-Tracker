package com.example.ui.screens.home

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.domain.CurrencyFormatter
import com.example.domain.FinancialEngine
import com.example.ui.HomeSubTab
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToMemos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedTabs = rememberSaveableStateHolder()
    val loadError by viewModel.homeError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val financialGoal by viewModel.financialGoal.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val homeSubTab by viewModel.homeSubTab.collectAsStateWithLifecycle()
    val preparedPageData by viewModel.monthPageData.collectAsStateWithLifecycle()
    val currentTransactions = preparedPageData?.transactions.orEmpty()
    val monthlyGroups by (if (homeSubTab == HomeSubTab.MONTHLY) viewModel.monthlyHistoricalGroups else kotlinx.coroutines.flow.flowOf(emptyList<MonthlyAggregation>())).collectAsStateWithLifecycle(emptyList<MonthlyAggregation>())
    val accountBalances by (if (homeSubTab == HomeSubTab.TOTAL_SUMMARY) viewModel.accountBalances else kotlinx.coroutines.flow.flowOf(emptyList<AccountWithBalance>())).collectAsStateWithLifecycle(emptyList<AccountWithBalance>())
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedTransactionIds.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()

    var showMonthPickerModal by remember { mutableStateOf(false) }
    var showBulkCategoryDialog by remember { mutableStateOf(false) }
    var showBulkAccountDialog by remember { mutableStateOf(false) }
    var showBulkNoteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSelectionOverflowMenu by remember { mutableStateOf(false) }
    var bulkNoteText by remember { mutableStateOf("") }
    var bulkNoteAppend by remember { mutableStateOf(false) }

    val isFilterActive = filterState.searchQuery.isNotBlank() ||
            filterState.typeFilter != null ||
            filterState.accountIdFilter != null ||
            filterState.categoryIdFilter != null ||
            filterState.onlyWithReceipt || filterState.startDateMillis != null || filterState.endDateMillis != null ||
            filterState.payeeFilter.isNotBlank() || filterState.tagFilter.isNotBlank()

    val selectedTransactions = remember(selectedIds, currentTransactions) {
        currentTransactions.filter { selectedIds.contains(it.transaction.id) }
    }
    val selectedNetSum = remember(selectedTransactions) {
        var sum = 0L
        for (item in selectedTransactions) {
            when (item.transaction.type) {
                TransactionType.INCOME -> sum += item.transaction.amount
                TransactionType.EXPENSE -> sum -= item.transaction.amount
                TransactionType.TRANSFER -> {}
            }
        }
        sum
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (selectedIds.isNotEmpty()) {
                            Color(0xFF242C3D)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .statusBarsPadding()
            ) {
                if (selectedIds.isNotEmpty()) {
                    // Selection Top Bar Overlay (Edit, Delete, More Options)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Exit Selection",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = Color.White
                                )
                            }

                            Box {
                                IconButton(onClick = { showSelectionOverflowMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSelectionOverflowMenu,
                                    onDismissRequest = { showSelectionOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (selectedIds.size == currentTransactions.size) "Deselect All" else "Select All")
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.SelectAll, contentDescription = null)
                                        },
                                        onClick = {
                                            showSelectionOverflowMenu = false
                                            if (selectedIds.size == currentTransactions.size) {
                                                viewModel.clearSelection()
                                            } else {
                                                viewModel.selectAllTransactions(currentTransactions.map { it.transaction.id })
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Change Category") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Category, contentDescription = null)
                                        },
                                        onClick = {
                                            showSelectionOverflowMenu = false
                                            showBulkCategoryDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Change Account") },
                                        leadingIcon = {
                                            Icon(Icons.Default.AccountBalance, contentDescription = null)
                                        },
                                        onClick = {
                                            showSelectionOverflowMenu = false
                                            showBulkAccountDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Change Date") },
                                        leadingIcon = {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                        },
                                        onClick = {
                                            showSelectionOverflowMenu = false
                                            val cal = Calendar.getInstance()
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val selectedCal = Calendar.getInstance().apply {
                                                        set(year, month, dayOfMonth, 12, 0, 0)
                                                    }
                                                    viewModel.bulkChangeDate(selectedCal.timeInMillis)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Edit Note") },
                                        leadingIcon = {
                                            Icon(Icons.Default.EditNote, contentDescription = null)
                                        },
                                        onClick = {
                                            showSelectionOverflowMenu = false
                                            bulkNoteText = ""
                                            bulkNoteAppend = false
                                            showBulkNoteDialog = true
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = ExpenseRed) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed)
                                        },
                                        onClick = {
                                            showSelectionOverflowMenu = false
                                            showDeleteConfirmDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Selection Count & Selected Net Amount Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIds.size}(s) selected",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        val currencySymbol = CurrencyFormatter.getCurrencySymbol(currencyCode)
                        val formattedSelectedAmount = if (selectedNetSum < 0L) {
                            val absAmount = kotlin.math.abs(selectedNetSum)
                            val absFormatted = CurrencyFormatter.formatAmount(absAmount, currencyCode, showSymbol = false)
                            "$currencySymbol -$absFormatted"
                        } else if (selectedNetSum > 0L) {
                            val formatted = CurrencyFormatter.formatAmount(selectedNetSum, currencyCode, showSymbol = false)
                            "$currencySymbol +$formatted"
                        } else {
                            "$currencySymbol 0.00"
                        }

                        Text(
                            text = formattedSelectedAmount,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp
                            ),
                            color = Color.White
                        )
                    }
                } else {
                    // Normal Top App Bar (Month Selector + Search & Memos)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Navigation Pill
                        MonthSelectorHeader(
                            currentMonthDate = currentMonth,
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            onMonthClick = { showMonthPickerModal = true }
                        )

                        // Right Actions (Search with active filter badge + Daily Memos)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateToSearch,
                                modifier = Modifier.testTag("search_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (isFilterActive) {
                                            Badge { Text("!") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search and Filters",
                                        tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onNavigateToMemos,
                                modifier = Modifier.testTag("memo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StickyNote2,
                                    contentDescription = "Daily Memos",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Sub Tabs (Daily, Calendar, Weekly, Monthly, Summary)
                    ScrollableTabRow(
                        selectedTabIndex = homeSubTab.ordinal,
                        edgePadding = 12.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            if (homeSubTab.ordinal < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[homeSubTab.ordinal]),
                                    height = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        divider = {
                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    ) {
                        HomeSubTab.values().forEach { tab ->
                            val isSelected = homeSubTab == tab
                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.setHomeSubTab(tab) },
                                text = {
                                    Text(
                                        text = when (tab) {
                                            HomeSubTab.DAILY -> "Daily"
                                            HomeSubTab.CALENDAR -> "Calendar"
                                            HomeSubTab.WEEKLY -> "Weekly"
                                            HomeSubTab.MONTHLY -> "Monthly"
                                            HomeSubTab.TOTAL_SUMMARY -> "Summary"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }

                // Period Summary Strip (Income, Expense, Balance)
                // Kept directly below the sub tabs (or below the selection header in selection mode)
                PeriodTotalsBar(
                    totalIncome = preparedPageData?.periodSummary?.totalIncome ?: 0L,
                    totalExpense = preparedPageData?.periodSummary?.totalExpense ?: 0L,
                    balance = preparedPageData?.periodSummary?.netSavings ?: 0L,
                    currencyCode = currencyCode,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedIds.isEmpty(),
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(180)),
                exit = scaleOut(animationSpec = tween(140)) + fadeOut(animationSpec = tween(140))
            ) {
                FloatingActionButton(
                    onClick = onNavigateToAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(26.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Content area with immersive swipe slide animation between months
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                        .horizontalSwipeListener(
                            thresholdDp = 44.dp,
                            cooldownMillis = 280L,
                            onSwipeLeft = { viewModel.nextMonth() },
                            onSwipeRight = { viewModel.previousMonth() }
                        )
                ) {
                    AnimatedContent(
                        targetState = preparedPageData,
                        contentKey = { it?.monthCal?.let { month -> month.get(Calendar.YEAR) * 12 + month.get(Calendar.MONTH) } },
                        transitionSpec = {
                            val initialMonth = initialState?.monthCal?.let { it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) } ?: 0
                            val targetMonth = targetState?.monthCal?.let { it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) } ?: 0
                            val direction = if (targetMonth > initialMonth) 1 else -1
                            (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> direction * width } +
                                fadeIn(animationSpec = tween(220)))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> -direction * width } +
                                        fadeOut(animationSpec = tween(180))
                                ).using(
                                    SizeTransform(clip = false)
                                )
                        },
                        label = "MonthSlideAnimation",
                        modifier = Modifier.fillMaxSize()
                    ) { pageData ->
                        if (pageData == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (loadError != null) ErrorContent(loadError!!, viewModel::retryHome) else LoadingContent()
                            }
                            return@AnimatedContent
                        }

                        Crossfade(
                            targetState = homeSubTab,
                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                            label = "HomeSubTabsTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { targetSubTab ->
                            savedTabs.SaveableStateProvider("${pageData.monthCal.get(Calendar.YEAR)}-${pageData.monthCal.get(Calendar.MONTH)}-${targetSubTab.name}") {
                            when (targetSubTab) {
                                HomeSubTab.DAILY -> {
                                    DailyTabContent(
                                        dailyGroups = pageData.dailyGroups,
                                        currencyCode = currencyCode,
                                        selectedIds = selectedIds,
                                        onTransactionClick = { tx ->
                                            if (selectedIds.isNotEmpty()) {
                                                viewModel.toggleTransactionSelection(tx.transaction.id)
                                            } else {
                                                onNavigateToEditTransaction(tx.transaction.id)
                                            }
                                        },
                                        onTransactionLongClick = { tx ->
                                            viewModel.toggleTransactionSelection(tx.transaction.id)
                                        },
                                        onToggleSelectDay = { dayTxIds ->
                                            val allDaySelected = dayTxIds.all { selectedIds.contains(it) }
                                            val newSelection = if (allDaySelected) {
                                                selectedIds - dayTxIds.toSet()
                                            } else {
                                                selectedIds + dayTxIds.toSet()
                                            }
                                            viewModel.selectAllTransactions(newSelection)
                                        },
                                        onEmptyAddClick = onNavigateToAddTransaction
                                    )
                                }
                                HomeSubTab.CALENDAR -> {
                                    CalendarTabContent(
                                        currentMonthCal = pageData.monthCal,
                                        currentMonthTransactions = pageData.transactions,
                                        currencyCode = currencyCode,
                                        onTransactionClick = { onNavigateToEditTransaction(it.transaction.id) },
                                        onAddTransaction = onNavigateToAddTransaction,
                                        onAddMemo = { dateStr ->
                                            onNavigateToMemos()
                                        }
                                    )
                                }
                                HomeSubTab.WEEKLY -> {
                                    WeeklyTabContent(
                                        weeklyGroups = pageData.weeklyGroups,
                                        currencyCode = currencyCode,
                                        onTransactionClick = { onNavigateToEditTransaction(it.transaction.id) }
                                    )
                                }
                                HomeSubTab.MONTHLY -> {
                                    MonthlyTabContent(
                                        monthlyGroups = monthlyGroups,
                                        currencyCode = currencyCode,
                                        onMonthSelected = { monthStr ->
                                            try {
                                                val parts = monthStr.split("-")
                                                viewModel.setMonth(parts[0].toInt(), parts[1].toInt() - 1)
                                                viewModel.setHomeSubTab(HomeSubTab.DAILY)
                                            } catch (e: Exception) {}
                                        }
                                    )
                                }
                                HomeSubTab.TOTAL_SUMMARY -> {
                                    SummaryTabContent(
                                        periodSummary = pageData.periodSummary,
                                        categorySpendings = pageData.categorySpendings,
                                        accountBalances = accountBalances,
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

    // Month Picker Dialog
    if (showMonthPickerModal) {
        MonthPickerDialog(
            currentCalendar = currentMonth,
            onMonthPicked = { year, month ->
                viewModel.setMonth(year, month)
                showMonthPickerModal = false
            },
            onDismiss = { showMonthPickerModal = false }
        )
    }

    // Bulk Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete ${selectedIds.size} Transactions?") },
            text = { Text("This will permanently delete all ${selectedIds.size} selected transactions and recalculate account balances.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedTransactions()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Edit Note Dialog
    if (showBulkNoteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkNoteDialog = false },
            title = { Text("Edit Note for ${selectedIds.size} Items") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter note text to apply to all selected records:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = bulkNoteText,
                        onValueChange = { bulkNoteText = it },
                        label = { Text("Note / Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bulkNoteAppend = !bulkNoteAppend }
                    ) {
                        Checkbox(
                            checked = bulkNoteAppend,
                            onCheckedChange = { bulkNoteAppend = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Append to existing note (instead of replacing)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bulkChangeNote(bulkNoteText, append = bulkNoteAppend)
                        showBulkNoteDialog = false
                    }
                ) {
                    Text("Apply Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Change Category Dialog
    if (showBulkCategoryDialog) {
        val categories by viewModel.categories.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showBulkCategoryDialog = false },
            title = { Text("Change Category (${selectedIds.size} items)") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.bulkChangeCategory(cat.id)
                                    showBulkCategoryDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(iconName = cat.iconName, colorHex = cat.colorHex, size = 34.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(cat.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    if (cat.type == TransactionType.EXPENSE) "Expense" else "Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cat.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBulkCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Bulk Change Account Dialog
    if (showBulkAccountDialog) {
        val accounts by viewModel.accounts.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showBulkAccountDialog = false },
            title = { Text("Change Asset / Account (${selectedIds.size} items)") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(accounts) { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.bulkChangeAccount(acc.id)
                                    showBulkAccountDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (acc.type) {
                                    AccountType.BANK -> Icons.Default.AccountBalance
                                    AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                    AccountType.CASH -> Icons.Default.Payments
                                    AccountType.INVESTMENT -> Icons.Default.TrendingUp
                                    else -> Icons.Default.AccountBalanceWallet
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(acc.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    "${acc.groupName} • ${acc.type.name.replace("_", " ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBulkAccountDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private val dayNumberFormatter = ThreadLocal.withInitial { SimpleDateFormat("dd", Locale.US) }
private val dayOfWeekFormatter = ThreadLocal.withInitial { SimpleDateFormat("EEE", Locale.US) }
private val monthYearFormatter = ThreadLocal.withInitial { SimpleDateFormat("MM.yyyy", Locale.US) }

@Composable
fun DailyTabContent(
    dailyGroups: List<DailyAggregation>,
    currencyCode: String,
    selectedIds: Set<Long>,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    onTransactionLongClick: (TransactionWithDetails) -> Unit,
    onToggleSelectDay: (List<Long>) -> Unit = {},
    onEmptyAddClick: () -> Unit
) {
    if (dailyGroups.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateView(
                title = "No Transactions Found",
                message = "You have not recorded any transactions for this month yet.",
                actionButton = {
                    Button(onClick = onEmptyAddClick) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add First Transaction")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        dailyGroups.forEachIndexed { groupIndex, group ->
            item(key = group.dateString, contentType = "day_header") {
                val groupTxIds = if (selectedIds.isNotEmpty()) group.transactions.map { it.transaction.id } else emptyList()
                val isAllDaySelected = selectedIds.isNotEmpty() && groupTxIds.isNotEmpty() && groupTxIds.all { selectedIds.contains(it) }

                // Subtle gap between each day list (Money Manager signature styling)
                if (groupIndex > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f))
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.TopCenter),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        )
                    }
                }

                // Money Manager style Subtle Day Section Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cal = remember(group.dateMillis) {
                                Calendar.getInstance().apply { timeInMillis = group.dateMillis }
                            }
                            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            val chipColor = when (dayOfWeek) {
                                Calendar.SUNDAY -> ExpenseRed.copy(alpha = 0.85f)
                                Calendar.SATURDAY -> TransferTeal.copy(alpha = 0.85f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                            }
                            val chipTextColor = when (dayOfWeek) {
                                Calendar.SUNDAY, Calendar.SATURDAY -> Color.White
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = if (selectedIds.isNotEmpty()) {
                                    Modifier.clickable { onToggleSelectDay(groupTxIds) }
                                } else Modifier
                            ) {
                                // Day Number Bold
                                Text(
                                    text = dayNumberFormatter.get()?.format(Date(group.dateMillis)) ?: "",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Day of Week Chip
                                Surface(
                                    color = chipColor,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = dayOfWeekFormatter.get()?.format(Date(group.dateMillis)) ?: "",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = chipTextColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Month.Year
                                Text(
                                    text = monthYearFormatter.get()?.format(Date(group.dateMillis)) ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            // Right side: Income and Expense totals side-by-side
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (group.totalIncome > 0) {
                                    Text(
                                        text = CurrencyFormatter.formatAmount(group.totalIncome, currencyCode),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        ),
                                        color = IncomeGreen
                                    )
                                }
                                if (group.totalExpense > 0) {
                                    Text(
                                        text = CurrencyFormatter.formatAmount(group.totalExpense, currencyCode),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        ),
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }

                        if (group.memo != null) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.padding(start = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StickyNote2,
                                    contentDescription = "Memo",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = group.memo.memoText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(group.transactions, key = { _, it -> it.transaction.id }, contentType = { _, _ -> "transaction" }) { index, item ->
                TransactionItemRow(
                    item = item,
                    currencyCode = currencyCode,
                    isSelected = selectedIds.contains(item.transaction.id),
                    isInSelectionMode = selectedIds.isNotEmpty(),
                    onClick = { onTransactionClick(item) },
                    onLongClick = { onTransactionLongClick(item) }
                )
                if (index < group.transactions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )
                }
            }
        }

        if (dailyGroups.isNotEmpty()) {
            item {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                )
            }
        }
    }
}

@Composable
fun CalendarTabContent(
    currentMonthCal: Calendar,
    currentMonthTransactions: List<TransactionWithDetails>,
    currencyCode: String,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    onAddTransaction: () -> Unit,
    onAddMemo: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayDateStr = remember { dateFormat.format(Calendar.getInstance().time) }
    
    // Default selection to today if in current month, or 1st of month
    var selectedDateString by rememberSaveable(currentMonthCal.get(Calendar.YEAR), currentMonthCal.get(Calendar.MONTH)) {
        val todayCal = Calendar.getInstance()
        val isCurrentCalThisMonth = todayCal.get(Calendar.YEAR) == currentMonthCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.MONTH) == currentMonthCal.get(Calendar.MONTH)
        if (isCurrentCalThisMonth) {
            mutableStateOf(todayDateStr)
        } else {
            val firstDayCal = currentMonthCal.clone() as Calendar
            firstDayCal.set(Calendar.DAY_OF_MONTH, 1)
            mutableStateOf(dateFormat.format(firstDayCal.time))
        }
    }

    val transactionsByDate = remember(currentMonthTransactions) {
        currentMonthTransactions.groupBy { dateFormat.format(Date(it.transaction.dateMillis)) }
    }

    // Build calendar grid cells
    val cal = currentMonthCal.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH) // 0-indexed
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday ... 7 = Saturday
    val daysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val leadingDaysCount = firstDayOfWeek - 1 // Days from Sunday to first day of month

    val prevCal = cal.clone() as Calendar
    prevCal.add(Calendar.MONTH, -1)
    val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val prevYear = prevCal.get(Calendar.YEAR)
    val prevMonth = prevCal.get(Calendar.MONTH)

    val nextCal = cal.clone() as Calendar
    nextCal.add(Calendar.MONTH, 1)
    val nextYear = nextCal.get(Calendar.YEAR)
    val nextMonth = nextCal.get(Calendar.MONTH)

    val totalSlots = if (leadingDaysCount + daysInCurrentMonth > 35) 42 else 35
    val numRows = totalSlots / 7

    val calendarCells = remember(currentMonthCal, currentMonthTransactions, selectedDateString) {
        val list = mutableListOf<CalendarDayCellData>()
        for (i in 0 until totalSlots) {
            val dayOfWeek = i % 7 // 0 = Sun, 1 = Mon ... 6 = Sat
            if (i < leadingDaysCount) {
                // Previous month trailing days
                val dayNum = daysInPrevMonth - leadingDaysCount + 1 + i
                val dayCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, prevYear)
                    set(Calendar.MONTH, prevMonth)
                    set(Calendar.DAY_OF_MONTH, dayNum)
                }
                val dateStr = dateFormat.format(dayCal.time)
                val dayTxs = transactionsByDate[dateStr] ?: emptyList()
                val inc = dayTxs.filter { it.transaction.type == TransactionType.INCOME && !it.transaction.isExcludedFromStats }.sumOf { it.transaction.amount }
                val exp = dayTxs.filter { it.transaction.type == TransactionType.EXPENSE && !it.transaction.isExcludedFromStats }.sumOf { it.transaction.amount }

                list.add(
                    CalendarDayCellData(
                        dateString = dateStr,
                        displayDayText = if (dayNum == 1) "${prevMonth + 1}.1" else "$dayNum",
                        dayOfWeek = dayOfWeek,
                        isCurrentMonth = false,
                        isToday = dateStr == todayDateStr,
                        isSelected = dateStr == selectedDateString,
                        incomeAmount = inc,
                        expenseAmount = exp
                    )
                )
            } else if (i < leadingDaysCount + daysInCurrentMonth) {
                // Current month days
                val dayNum = i - leadingDaysCount + 1
                val dayCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, dayNum)
                }
                val dateStr = dateFormat.format(dayCal.time)
                val dayTxs = transactionsByDate[dateStr] ?: emptyList()
                val inc = dayTxs.filter { it.transaction.type == TransactionType.INCOME && !it.transaction.isExcludedFromStats }.sumOf { it.transaction.amount }
                val exp = dayTxs.filter { it.transaction.type == TransactionType.EXPENSE && !it.transaction.isExcludedFromStats }.sumOf { it.transaction.amount }

                list.add(
                    CalendarDayCellData(
                        dateString = dateStr,
                        displayDayText = if (dayNum == 1) "${currentMonth + 1}.1" else "$dayNum",
                        dayOfWeek = dayOfWeek,
                        isCurrentMonth = true,
                        isToday = dateStr == todayDateStr,
                        isSelected = dateStr == selectedDateString,
                        incomeAmount = inc,
                        expenseAmount = exp
                    )
                )
            } else {
                // Next month leading days
                val dayNum = i - (leadingDaysCount + daysInCurrentMonth) + 1
                val dayCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, nextYear)
                    set(Calendar.MONTH, nextMonth)
                    set(Calendar.DAY_OF_MONTH, dayNum)
                }
                val dateStr = dateFormat.format(dayCal.time)
                val dayTxs = transactionsByDate[dateStr] ?: emptyList()
                val inc = dayTxs.filter { it.transaction.type == TransactionType.INCOME && !it.transaction.isExcludedFromStats }.sumOf { it.transaction.amount }
                val exp = dayTxs.filter { it.transaction.type == TransactionType.EXPENSE && !it.transaction.isExcludedFromStats }.sumOf { it.transaction.amount }

                list.add(
                    CalendarDayCellData(
                        dateString = dateStr,
                        displayDayText = if (dayNum == 1) "${nextMonth + 1}.1" else "$dayNum",
                        dayOfWeek = dayOfWeek,
                        isCurrentMonth = false,
                        isToday = dateStr == todayDateStr,
                        isSelected = dateStr == selectedDateString,
                        incomeAmount = inc,
                        expenseAmount = exp
                    )
                )
            }
        }
        list
    }

    val selectedDayTransactions = transactionsByDate[selectedDateString] ?: emptyList()
    val selectedDayIncome = selectedDayTransactions.filter { it.transaction.type == TransactionType.INCOME && !it.transaction.isExcludedFromStats }.sumOf { it.transaction.amount }
    val selectedDayExpense = selectedDayTransactions.filter { it.transaction.type == TransactionType.EXPENSE && !it.transaction.isExcludedFromStats }.sumOf { it.transaction.amount }

    // Parse selected date for display
    val selectedDateFormatted = remember(selectedDateString) {
        try {
            val d = dateFormat.parse(selectedDateString)
            if (d != null) {
                SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US).format(d)
            } else selectedDateString
        } catch (e: Exception) {
            selectedDateString
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Weekdays Header (Sun in Red, Sat in Blue, Weekdays in Neutral)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEachIndexed { index, day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (index) {
                                    0 -> ExpenseRed
                                    6 -> TransferTeal
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Calendar Grid with border lines
                    for (row in 0 until numRows) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val cellData = calendarCells.getOrNull(cellIndex)

                                if (cellData != null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(68.dp)
                                            .background(
                                                if (cellData.isSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                selectedDateString = cellData.dateString
                                            }
                                            .padding(horizontal = 2.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Day Label / Pill
                                            if (cellData.isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = cellData.displayDayText,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            } else if (cellData.isToday) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                ) {
                                                    Text(
                                                        text = cellData.displayDayText,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = cellData.displayDayText,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = if (cellData.isCurrentMonth) FontWeight.Medium else FontWeight.Normal
                                                    ),
                                                    color = when {
                                                        !cellData.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                                                        cellData.dayOfWeek == 0 -> ExpenseRed
                                                        cellData.dayOfWeek == 6 -> TransferTeal
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }

                                            // Stacked Income / Expense amounts
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                if (cellData.incomeAmount > 0) {
                                                    Text(
                                                        text = CurrencyFormatter.formatAmount(cellData.incomeAmount, currencyCode, showSymbol = false, showDecimals = false),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = IncomeGreen,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (cellData.expenseAmount > 0) {
                                                    Text(
                                                        text = CurrencyFormatter.formatAmount(cellData.expenseAmount, currencyCode, showSymbol = false, showDecimals = false),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = ExpenseRed,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                if (col < 6) {
                                    VerticalDivider(
                                        modifier = Modifier.height(68.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Transactions Header Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedDateFormatted,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (selectedDayIncome > 0) {
                                    Text(
                                        text = "+${CurrencyFormatter.formatAmount(selectedDayIncome, currencyCode, showDecimals = false)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = IncomeGreen
                                    )
                                }
                                if (selectedDayExpense > 0) {
                                    Text(
                                        text = "-${CurrencyFormatter.formatAmount(selectedDayExpense, currencyCode, showDecimals = false)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ExpenseRed
                                    )
                                }
                                if (selectedDayIncome == 0L && selectedDayExpense == 0L) {
                                    Text(
                                        text = "No records",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { onAddMemo(selectedDateString) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.StickyNote2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Memo", style = MaterialTheme.typography.labelSmall)
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            FilledTonalButton(
                                onClick = onAddTransaction,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (selectedDayTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions on this date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(selectedDayTransactions, key = { it.transaction.id }, contentType = { "transaction" }) { item ->
                TransactionItemRow(
                    item = item,
                    currencyCode = currencyCode,
                    onClick = { onTransactionClick(item) }
                )
            }
        }
    }
}

data class CalendarDayCellData(
    val dateString: String,
    val displayDayText: String,
    val dayOfWeek: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val incomeAmount: Long,
    val expenseAmount: Long
)

@Composable
fun WeeklyTabContent(
    weeklyGroups: List<WeeklyAggregation>,
    currencyCode: String,
    onTransactionClick: (TransactionWithDetails) -> Unit
) {
    if (weeklyGroups.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateView(
                title = "No Weekly Data",
                message = "No recorded transactions for this month.",
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }

    var expandedWeeks by rememberSaveable { mutableStateOf(emptyList<Int>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weeklyGroups.forEach { week ->
            val isExpanded = week.weekNumber in expandedWeeks

            item(key = "week_${week.weekNumber}", contentType = "week_header") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedWeeks = if (isExpanded) expandedWeeks - week.weekNumber
                                        else expandedWeeks + week.weekNumber
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = week.label,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${week.transactionCount} transactions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Net: ${CurrencyFormatter.formatAmount(week.balance, currencyCode)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (week.balance >= 0) IncomeGreen else ExpenseRed
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("+${CurrencyFormatter.formatAmount(week.totalIncome, currencyCode, showDecimals = false)}", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                                        Text("-${CurrencyFormatter.formatAmount(week.totalExpense, currencyCode, showDecimals = false)}", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }

            // Each expanded row is a lazy item, so a large week never composes
            // its entire transaction history during a scroll or expansion.
            if (isExpanded) {
                items(week.transactions, key = { it.transaction.id }, contentType = { "transaction" }) { tx ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        TransactionItemRow(
                            item = tx,
                            currencyCode = currencyCode,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyTabContent(
    monthlyGroups: List<MonthlyAggregation>,
    currencyCode: String,
    onMonthSelected: (String) -> Unit
) {
    if (monthlyGroups.isEmpty()) {
        EmptyStateView(title = "No Monthly History", message = "Add transactions to see month-over-month history.")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(monthlyGroups, key = { it.monthString }) { month ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMonthSelected(month.monthString) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = month.monthLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${month.transactionCount} transactions · Savings rate: ${String.format("%.1f", month.savingsRate)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.formatAmount(month.balance, currencyCode),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (month.balance >= 0) IncomeGreen else ExpenseRed
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("+${CurrencyFormatter.formatAmount(month.totalIncome, currencyCode, showDecimals = false)}", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                            Text("-${CurrencyFormatter.formatAmount(month.totalExpense, currencyCode, showDecimals = false)}", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryTabContent(
    periodSummary: PeriodSummary,
    categorySpendings: List<CategorySpending>,
    accountBalances: List<AccountWithBalance>,
    currencyCode: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Savings & Cash Flow Highlight Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Net Monthly Savings",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = CurrencyFormatter.formatAmount(periodSummary.netSavings, currencyCode),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (periodSummary.netSavings >= 0) MaterialTheme.colorScheme.onPrimaryContainer else ExpenseRed
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Savings Rate: ${String.format("%.1f", periodSummary.savingsRate)}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${periodSummary.transactionCount} transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Top Spending Categories
        item {
            Text(
                text = "Expense by Category",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (categorySpendings.isEmpty()) {
                Text("No expense recorded for this month.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                DonutPieChart(
                    items = categorySpendings,
                    totalAmount = periodSummary.totalExpense,
                    currencyCode = currencyCode
                )
            }
        }

        // Account Balances Breakdown
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Account Balances",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            accountBalances.forEach { acc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (acc.account.type) {
                                AccountType.CASH -> Icons.Default.AccountBalanceWallet
                                AccountType.BANK -> Icons.Default.AccountBalance
                                AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                AccountType.INVESTMENT -> Icons.Default.TrendingUp
                                else -> Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = acc.account.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = CurrencyFormatter.formatAmount(acc.calculatedBalance, currencyCode),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (FinancialEngine.isLiabilityAccount(acc.account.type)) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
