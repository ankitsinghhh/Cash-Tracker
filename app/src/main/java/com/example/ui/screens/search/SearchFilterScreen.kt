package com.example.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.domain.CurrencyFormatter
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val searchDateParser = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
private val searchDateFormatter = ThreadLocal.withInitial { SimpleDateFormat("EEE, dd MMM yyyy", Locale.US) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactionsWithDetails.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf(filterState.searchQuery) }
    var selectedType by remember { mutableStateOf(filterState.typeFilter) }
    var selectedAccountId by remember { mutableStateOf(filterState.accountIdFilter) }
    var selectedCategoryId by remember { mutableStateOf(filterState.categoryIdFilter) }
    var onlyReceipt by remember { mutableStateOf(filterState.onlyWithReceipt) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Auto focus search field on launch if query is empty
    LaunchedEffect(Unit) {
        if (searchQuery.isBlank()) {
            focusRequester.requestFocus()
        }
    }

    val isFilterActive = selectedType != null || selectedAccountId != null || selectedCategoryId != null || onlyReceipt || searchQuery.isNotBlank()

    // Live filtered search results
    val searchResults = remember(
        allTransactions, searchQuery, selectedType, selectedAccountId, selectedCategoryId, onlyReceipt
    ) {
        allTransactions.filter { item ->
            val tx = item.transaction
            val matchesType = selectedType == null || tx.type == selectedType
            val matchesAccount = selectedAccountId == null || tx.accountId == selectedAccountId || tx.toAccountId == selectedAccountId
            val matchesCategory = selectedCategoryId == null || tx.categoryId == selectedCategoryId
            val matchesReceipt = !onlyReceipt || tx.receiptUri != null
            val matchesQuery = searchQuery.isBlank() ||
                    tx.payee.contains(searchQuery, ignoreCase = true) ||
                    tx.note.contains(searchQuery, ignoreCase = true) ||
                    tx.tags.contains(searchQuery, ignoreCase = true) ||
                    (item.category?.name?.contains(searchQuery, ignoreCase = true) == true) ||
                    (item.account?.name?.contains(searchQuery, ignoreCase = true) == true)

            matchesType && matchesAccount && matchesCategory && matchesReceipt && matchesQuery
        }.sortedByDescending { it.transaction.dateMillis }
    }

    // Group filtered results by day for clean Google-style sectioning
    val groupedByDate = remember(searchResults) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        searchResults.groupBy { dateFormat.format(Date(it.transaction.dateMillis)) }
    }

    val totalExpense = remember(searchResults) {
        searchResults.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
    }
    val totalIncome = remember(searchResults) {
        searchResults.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
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
                // Google Stock Search Bar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Elevated Search Pill
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search payees, notes, tags...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        viewModel.setFilter(
                                            filterState.copy(
                                                searchQuery = it,
                                                typeFilter = selectedType,
                                                accountIdFilter = selectedAccountId,
                                                categoryIdFilter = selectedCategoryId,
                                                onlyWithReceipt = onlyReceipt
                                            )
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        viewModel.setFilter(filterState.copy(searchQuery = ""))
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter Chips Horizontal Carousel
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Filter Chip
                    item {
                        FilterChip(
                            selected = selectedType != null,
                            onClick = {
                                selectedType = when (selectedType) {
                                    null -> TransactionType.EXPENSE
                                    TransactionType.EXPENSE -> TransactionType.INCOME
                                    TransactionType.INCOME -> TransactionType.TRANSFER
                                    TransactionType.TRANSFER -> null
                                }
                            },
                            label = {
                                Text(
                                    when (selectedType) {
                                        TransactionType.EXPENSE -> "Expense"
                                        TransactionType.INCOME -> "Income"
                                        TransactionType.TRANSFER -> "Transfer"
                                        null -> "Type: All"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (selectedType) {
                                        TransactionType.EXPENSE -> Icons.Default.ArrowDownward
                                        TransactionType.INCOME -> Icons.Default.ArrowUpward
                                        TransactionType.TRANSFER -> Icons.Default.SyncAlt
                                        null -> Icons.Default.FilterList
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = CircleShape
                        )
                    }

                    // Account Filter Chip
                    item {
                        var showAccountMenu by remember { mutableStateOf(false) }
                        val activeAccount = accounts.find { it.id == selectedAccountId }

                        Box {
                            FilterChip(
                                selected = selectedAccountId != null,
                                onClick = { showAccountMenu = true },
                                label = { Text(activeAccount?.name ?: "Account: All") },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                shape = CircleShape
                            )

                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Accounts", fontWeight = if (selectedAccountId == null) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedAccountId = null
                                        showAccountMenu = false
                                    }
                                )
                                HorizontalDivider()
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.name, fontWeight = if (selectedAccountId == acc.id) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedAccountId = acc.id
                                            showAccountMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Category Filter Chip
                    item {
                        var showCategoryMenu by remember { mutableStateOf(false) }
                        val activeCategory = categories.find { it.id == selectedCategoryId }

                        Box {
                            FilterChip(
                                selected = selectedCategoryId != null,
                                onClick = { showCategoryMenu = true },
                                label = { Text(activeCategory?.name ?: "Category: All") },
                                leadingIcon = {
                                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                shape = CircleShape
                            )

                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Categories", fontWeight = if (selectedCategoryId == null) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedCategoryId = null
                                        showCategoryMenu = false
                                    }
                                )
                                HorizontalDivider()
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name, fontWeight = if (selectedCategoryId == cat.id) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = {
                                            CategoryIconBadge(iconName = cat.iconName, colorHex = cat.colorHex, size = 24.dp)
                                        },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Has Receipt Chip
                    item {
                        FilterChip(
                            selected = onlyReceipt,
                            onClick = { onlyReceipt = !onlyReceipt },
                            label = { Text("Receipt Photo") },
                            leadingIcon = {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = CircleShape
                        )
                    }

                    // Reset Filters Button
                    if (isFilterActive) {
                        item {
                            SuggestionChip(
                                onClick = {
                                    searchQuery = ""
                                    selectedType = null
                                    selectedAccountId = null
                                    selectedCategoryId = null
                                    onlyReceipt = false
                                    viewModel.clearFilter()
                                },
                                label = { Text("Reset All", color = MaterialTheme.colorScheme.error) },
                                icon = {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                },
                                shape = CircleShape
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
            // Results Summary Pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchResults.isEmpty()) "No results" else "${searchResults.size} transaction${if (searchResults.size > 1) "s" else ""} found",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (searchResults.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (totalIncome > 0) {
                                Text(
                                    text = "+${CurrencyFormatter.formatAmount(totalIncome, currencyCode, showDecimals = false)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = IncomeGreen
                                )
                            }
                            if (totalExpense > 0) {
                                Text(
                                    text = "-${CurrencyFormatter.formatAmount(totalExpense, currencyCode, showDecimals = false)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            if (searchResults.isEmpty()) {
                item {
                    EmptyStateView(
                        title = if (isFilterActive) "No Matching Transactions" else "Search Your Records",
                        message = if (isFilterActive) "Try clearing search keywords or loosening filter chips."
                        else "Type a payee, category, note or tag above to instantly find records.",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    )
                }
            } else {
                groupedByDate.entries.forEachIndexed { groupIndex, entry ->
                    val dateStr = entry.key
                    val itemsForDay = entry.value
                    val dayHeader = try {
                        val parsed = searchDateParser.get()?.parse(dateStr)
                        if (parsed != null) searchDateFormatter.get()?.format(parsed) ?: dateStr else dateStr
                    } catch (e: Exception) {
                        dateStr
                    }

                    item(key = "header_$dateStr") {
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
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dayHeader,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val daySum = itemsForDay.sumOf {
                                    when (it.transaction.type) {
                                        TransactionType.INCOME -> it.transaction.amount
                                        TransactionType.EXPENSE -> -it.transaction.amount
                                        TransactionType.TRANSFER -> 0L
                                    }
                                }
                                Text(
                                    text = CurrencyFormatter.formatAmount(daySum, currencyCode, showDecimals = false),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (daySum >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }

                    items(itemsForDay, key = { it.transaction.id }) { item ->
                        TransactionItemRow(
                            item = item,
                            currencyCode = currencyCode,
                            onClick = { onNavigateToEditTransaction(item.transaction.id) }
                        )
                    }
                }
            }
        }
    }
}
