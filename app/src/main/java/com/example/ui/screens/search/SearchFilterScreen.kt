package com.example.ui.screens.search

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.domain.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit, modifier: Modifier = Modifier) {
    val filter by viewModel.filterState.collectAsStateWithLifecycle()
    val appliedFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val totals by viewModel.searchTotals.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currency by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val results = viewModel.searchPages.collectAsLazyPagingItems()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val state = rememberLazyListState()
    LaunchedEffect(appliedFilter) { state.scrollToItem(0) }

    fun chooseDate(start: Boolean) {
        val current = Calendar.getInstance().apply {
            timeInMillis = (if (start) filter.startDateMillis else filter.endDateMillis) ?: System.currentTimeMillis()
        }
        DatePickerDialog(context, { _, year, month, day ->
            val millis = Calendar.getInstance().apply {
                clear(); set(year, month, day)
                if (!start) { add(Calendar.DAY_OF_MONTH, 1); add(Calendar.MILLISECOND, -1) }
            }.timeInMillis
            viewModel.setFilter(if (start) filter.copy(startDateMillis = millis,
                endDateMillis = filter.endDateMillis?.coerceAtLeast(millis))
                else filter.copy(endDateMillis = millis, startDateMillis = filter.startDateMillis?.coerceAtMost(millis)))
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
    }

    Scaffold(modifier = modifier.fillMaxSize(), topBar = {
        Column {
            TopAppBar(title = { Text("Search transactions") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            })
            OutlinedTextField(value = filter.searchQuery, onValueChange = { viewModel.setFilter(filter.copy(searchQuery = it)) },
                label = { Text("Payee, note, tag, category or account") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("search_input"))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TransactionType.entries, key = { it.name }) { type ->
                    FilterChip(selected = filter.typeFilter == type, onClick = {
                        viewModel.setFilter(filter.copy(typeFilter = if (filter.typeFilter == type) null else type))
                    }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
                item { FilterChip(filter.onlyWithReceipt, { viewModel.setFilter(filter.copy(onlyWithReceipt = !filter.onlyWithReceipt)) }, label = { Text("Receipt") }) }
                item { SuggestionChip({ chooseDate(true) }, label = { Text(filter.startDateMillis?.let { "From ${dateFormat.format(Date(it))}" } ?: "From date") }) }
                item { SuggestionChip({ chooseDate(false) }, label = { Text(filter.endDateMillis?.let { "To ${dateFormat.format(Date(it))}" } ?: "To date") }) }
                item { SuggestionChip({ viewModel.clearFilter() }, label = { Text("Clear filters") }) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterMenu("Account", accounts.map { it.id to it.name }, filter.accountIdFilter) {
                    viewModel.setFilter(filter.copy(accountIdFilter = it))
                }
                FilterMenu("Category", categories.map { it.id to it.name }, filter.categoryIdFilter) {
                    viewModel.setFilter(filter.copy(categoryIdFilter = it))
                }
            }
        }
    }) { padding ->
        LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "summary", contentType = "summary") {
                Column(Modifier.padding(16.dp)) {
                    Text("${totals.count} transactions", style = MaterialTheme.typography.titleMedium)
                    Text("Income ${CurrencyFormatter.formatAmount(totals.income, currency)} · Expenses ${CurrencyFormatter.formatAmount(totals.expense, currency)}")
                }
            }
            when {
                results.loadState.refresh is LoadState.Loading || filter != appliedFilter -> item { LoadingContent("Searching…") }
                results.loadState.refresh is LoadState.Error -> item { ErrorContent("Could not load results", results::retry) }
                results.itemCount == 0 -> item { EmptyStateView("No matching transactions", "Try clearing a filter or changing the search.") }
            }
            items(results.itemCount, key = results.itemKey { it.transaction.id }, contentType = { "transaction" }) { index ->
                val item = results[index] ?: return@items
                val day = dayFormat.format(Date(item.transaction.dateMillis))
                val previous = if (index > 0) results.peek(index - 1) else null
                if (previous == null || day != dayFormat.format(Date(previous.transaction.dateMillis))) {
                    Text(day, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall)
                }
                TransactionItemRow(item, currency, onClick = { onNavigateToEditTransaction(item.transaction.id) })
            }
            if (results.loadState.append is LoadState.Loading) item { LoadingContent("Loading more…") }
            if (results.loadState.append is LoadState.Error) item { ErrorContent("Could not load more results", results::retry) }
        }
    }
}

@Composable
private fun FilterMenu(label: String, choices: List<Pair<Long, String>>, selected: Long?, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(selected != null, { expanded = true }, label = { Text(choices.find { it.first == selected }?.second ?: "$label: all") })
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text("All ${label.lowercase()}s") }, onClick = { expanded = false; onSelect(null) })
            choices.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { expanded = false; onSelect(id) }) }
        }
    }
}
