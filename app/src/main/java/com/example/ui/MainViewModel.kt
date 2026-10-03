package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import com.example.domain.*
import com.example.server.PCManagerServer
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import androidx.paging.cachedIn
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class HomeSubTab {
    DAILY,
    CALENDAR,
    WEEKLY,
    MONTHLY,
    TOTAL_SUMMARY
}

enum class StatsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ALL_TIME
}

data class TransactionFilterState(
    val searchQuery: String = "",
    val typeFilter: TransactionType? = null,
    val accountIdFilter: Long? = null,
    val categoryIdFilter: Long? = null,
    val payeeFilter: String = "",
    val tagFilter: String = "",
    val onlyWithReceipt: Boolean = false,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null
)

class MainViewModel @JvmOverloads constructor(application: Application, private val databaseOverride: AppDatabase? = null) : AndroidViewModel(application) {

    private val database = databaseOverride ?: AppDatabase.getDatabase(application, viewModelScope)
    val repository = FinanceRepository(database, viewModelScope)

    val loadedQueries = repository.loadedQueries
    val readErrors = repository.readErrors
    fun retryReads() { repository.retryReads(); retryHome() }

    private val _transactionSaving = MutableStateFlow(false)
    val transactionSaving = _transactionSaving.asStateFlow()

    private val noticeChannel = Channel<UiNotice>(Channel.BUFFERED)
    val notices = noticeChannel.receiveAsFlow()
    private val _accountsLoaded = MutableStateFlow(false)
    val accountsLoaded = _accountsLoaded.asStateFlow()
    private val _homeError = MutableStateFlow<String?>(null)
    val homeError = _homeError.asStateFlow()
    private val refresh = MutableStateFlow(0)
    fun retryHome() { refresh.value++ }

    private fun launchMutation(onError: ((String) -> Unit)? = null, block: suspend () -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                val message = if (error is IllegalArgumentException) error.message ?: "Check your input" else "Could not save the change. Please retry."
                noticeChannel.send(UiNotice(message))
                withContext(Dispatchers.Main) { onError?.invoke(message) }
            }
        }

    fun accountPages(id: Long) = repository.accountPages(id).cachedIn(viewModelScope)
    fun transaction(id: Long) = repository.withDetails(database.transactionDao().getTransactionByIdFlow(id))

    // Current Selected Month
    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()

    // Sub-tab in Home
    private val _homeSubTab = MutableStateFlow(HomeSubTab.DAILY)
    val homeSubTab: StateFlow<HomeSubTab> = _homeSubTab.asStateFlow()

    // Filter State
    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    // Multi-select bulk edit
    private val _selectedTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<Long>> = _selectedTransactionIds.asStateFlow()

    // Global Settings & Profile
    val isOnboardingCompleted = repository.getSettingFlow("onboarding_completed")
        .map { it == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val userName = repository.getSettingFlow("user_name")
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val financialGoal = repository.getSettingFlow("financial_goal")
        .map { it ?: "TRACK_EXPENSES" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "TRACK_EXPENSES")

    val monthlyBudgetGoal = repository.getSettingFlow("monthly_budget_goal")
        .map { it?.toLongOrNull() ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val showAccountsTab = repository.getSettingFlow("show_accounts_tab")
        .map { it?.toBoolean() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val primaryCurrency = repository.getSettingFlow("primary_currency")
        .map { it ?: "INR" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    val isIndianFormat = repository.getSettingFlow("indian_number_format")
        .map { it?.toBoolean() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isPinEnabled = repository.getSettingFlow("app_pin")
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val storedPin = repository.getSettingFlow("app_pin")
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themePalette = repository.getSettingFlow("theme_palette")
        .map { setting ->
            AppThemePalette.values().firstOrNull { it.id == setting } ?: AppThemePalette.INDIGO
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemePalette.INDIGO)

    val themeMode = repository.getSettingFlow("theme_mode")
        .map { setting ->
            AppThemeMode.values().firstOrNull { it.name == setting } ?: AppThemeMode.SYSTEM
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // All active accounts & categories
    val accounts = repository.accounts.onEach { _accountsLoaded.value = true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAccounts = repository.allAccounts.onEach { _accountsLoaded.value = true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTransactionsWithDetails = repository.transactionsWithDetails.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accountBalances = repository.accountBalances.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val netWorth = repository.netWorth.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NetWorthSummary(0, 0, 0, 0, 0, 0, 0, 0)
    )

    val budgets = repository.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeRecurring = repository.activeRecurring.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allRecurring = repository.recurring.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeInstallments = repository.activeInstallments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allInstallments = repository.installments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val memos = repository.memos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bookmarks = repository.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PC Manager (Web Access) State
    private val _isPcServerRunning = MutableStateFlow(PCManagerServer.isRunning)
    val isPcServerRunning: StateFlow<Boolean> = _isPcServerRunning.asStateFlow()

    private val _pcServerUrl = MutableStateFlow(if (PCManagerServer.isRunning) PCManagerServer.serverUrl else "")
    val pcServerUrl: StateFlow<String> = _pcServerUrl.asStateFlow()

    private val _pcServerIp = MutableStateFlow(PCManagerServer.ipAddress)
    val pcServerIp: StateFlow<String> = _pcServerIp.asStateFlow()

    private val _pcServerPort = MutableStateFlow(PCManagerServer.port)
    val pcServerPort: StateFlow<Int> = _pcServerPort.asStateFlow()

    val isPcPasscodeEnabled = repository.getSettingFlow("pc_passcode_enabled")
        .map { it == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val pcPasscode = repository.getSettingFlow("pc_passcode")
        .map { it ?: "1234" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1234")

    // Prepare the home page once per data/month/filter change, outside composition.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val monthPageData: StateFlow<MonthPageData?> = combine(_currentMonth, memos, _filterState, refresh) { month, memoList, filter, _ ->
        Triple(month, memoList, filter)
    }.flatMapLatest { (month, memoList, filter) ->
        val start = (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis - 1
        val previousStart = (start.clone() as Calendar).apply { add(Calendar.MONTH, -1) }.timeInMillis
        repository.transactionsBetween(minOf(previousStart, filter.startDateMillis ?: start.timeInMillis), maxOf(end, filter.endDateMillis ?: end))
            .map<List<TransactionWithDetails>, MonthPageData?> { txs -> getMonthPageData(month.get(Calendar.YEAR) * 12 + month.get(Calendar.MONTH), txs, memoList, filter) }
            .onStart { _homeError.value = null }
            .catch { error -> _homeError.value = "Could not load transactions. Please retry."; emit(null) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMonthTransactions = monthPageData.filterNotNull().map { it.transactions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchFilter = _filterState.debounce { if (it.searchQuery.isBlank()) 0L else 250L }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionFilterState())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchPages = searchFilter.flatMapLatest { repository.searchPages(it) }.cachedIn(viewModelScope)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchTotals = searchFilter.flatMapLatest { repository.searchTotals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchTotals())

    val monthPeriodSummary = monthPageData.filterNotNull().map { it.periodSummary }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary(0, 0, 0, 0f, 0))
    val dailyGroups = monthPageData.filterNotNull().map { it.dailyGroups }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val weeklyGroups = monthPageData.filterNotNull().map { it.weeklyGroups }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val monthlyHistoricalGroups = repository.monthlyTotals.map { months ->
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val display = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        months.mapIndexed { index, month ->
            val balance = month.income - month.expense
            val previous = months.getOrNull(index + 1)
            MonthlyAggregation(month.monthString, display.format(parser.parse(month.monthString)!!),
                month.income, month.expense, balance,
                if (month.income > 0) balance.toFloat() / month.income * 100 else 0f,
                balance - (previous?.let { it.income - it.expense } ?: balance), month.count)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Spending for current month
    val categorySpendings = currentMonthTransactions.map { txs ->
        FinancialEngine.calculateCategorySpending(txs, TransactionType.EXPENSE)
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryIncomes = currentMonthTransactions.map { txs ->
        FinancialEngine.calculateCategorySpending(txs, TransactionType.INCOME)
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Merchant Analytics
    val merchantStats = allTransactionsWithDetails.map { txs ->
        FinancialEngine.calculateMerchantAnalytics(txs)
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Spending Trend & Velocity (Time-Series)
    val dailySpendingTrend = combine(
        currentMonthTransactions,
        _currentMonth
    ) { txs, monthCal ->
        FinancialEngine.calculateDailySpendingTrend(txs, monthCal)
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailySpendingTrendData(emptyList(), emptyList(), 0L, 1, 0L, 0, 0L)
    )

    // Day of Week Spending Habit Distribution
    val dayOfWeekDistribution = currentMonthTransactions.map { txs ->
        FinancialEngine.calculateDayOfWeekDistribution(txs)
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val comparisonTransactions = _currentMonth.flatMapLatest { month ->
        val first = (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.MONTH, -1)
        }
        val end = (first.clone() as Calendar).apply { add(Calendar.MONTH, 2) }.timeInMillis - 1
        repository.transactionsBetween(first.timeInMillis, end)
    }

    // Month over Month Category Comparison
    val monthOverMonthCategoryComparison = combine(
        comparisonTransactions,
        _currentMonth
    ) { allTx, monthCal ->
        val curStart = (monthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val curEnd = (monthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val prevMonthCal = (monthCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevStart = (prevMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val prevEnd = (prevMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val currentTxs = allTx.filter { it.transaction.dateMillis in curStart..curEnd }
        val prevTxs = allTx.filter { it.transaction.dateMillis in prevStart..prevEnd }

        FinancialEngine.calculateMonthOverMonthCategoryComparison(currentTxs, prevTxs)
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget Progress for current month
    val currentMonthBudgetsProgress = combine(
        budgets,
        categories,
        currentMonthTransactions,
        _currentMonth
    ) { bList, catList, txs, monthCal ->
        val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(monthCal.time)
        val filteredBudgets = bList.filter { it.monthString == monthStr || it.monthString == "DEFAULT" }

        val todayCal = Calendar.getInstance()
        val isCurrentMonth = todayCal.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH)
        val daysElapsed = if (isCurrentMonth) todayCal.get(Calendar.DAY_OF_MONTH) else monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalDays = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        FinancialEngine.calculateBudgetProgressList(filteredBudgets, catList, txs, daysElapsed, totalDays)
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation and Calendar Actions
    fun previousMonth() {
        val newCal = _currentMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, -1)
        _currentMonth.value = newCal
    }

    fun nextMonth() {
        val newCal = _currentMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, 1)
        _currentMonth.value = newCal
    }

    fun getMonthPageData(
        monthKey: Int,
        allTx: List<TransactionWithDetails> = allTransactionsWithDetails.value,
        memoList: List<DailyMemo> = memos.value,
        filter: TransactionFilterState = _filterState.value
    ): MonthPageData {
        val year = monthKey / 12
        val monthIndex = monthKey % 12

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startMillis = filter.startDateMillis ?: startCal.timeInMillis
        val endMillis = filter.endDateMillis ?: endCal.timeInMillis

        val monthTxs = allTx.filter { item ->
            val tx = item.transaction
            val withinDate = tx.dateMillis in startMillis..endMillis
            val matchesType = filter.typeFilter == null || tx.type == filter.typeFilter
            val matchesAccount = filter.accountIdFilter == null || tx.accountId == filter.accountIdFilter || tx.toAccountId == filter.accountIdFilter
            val matchesCategory = filter.categoryIdFilter == null || tx.categoryId == filter.categoryIdFilter || tx.subcategoryId == filter.categoryIdFilter
            val matchesSearch = filter.searchQuery.isBlank() ||
                    tx.payee.contains(filter.searchQuery, ignoreCase = true) ||
                    tx.note.contains(filter.searchQuery, ignoreCase = true) ||
                    tx.tags.contains(filter.searchQuery, ignoreCase = true) ||
                    (item.category?.name?.contains(filter.searchQuery, ignoreCase = true) == true) ||
                    (item.subcategory?.name?.contains(filter.searchQuery, ignoreCase = true) == true) ||
                    (item.account?.name?.contains(filter.searchQuery, ignoreCase = true) == true)
            val matchesReceipt = !filter.onlyWithReceipt || tx.receiptUri != null
            val matchesPayee = filter.payeeFilter.isBlank() || tx.payee.contains(filter.payeeFilter, ignoreCase = true)
            val matchesTag = filter.tagFilter.isBlank() || tx.tags.contains(filter.tagFilter, ignoreCase = true)

            withinDate && matchesType && matchesAccount && matchesCategory && matchesSearch && matchesReceipt && matchesPayee && matchesTag
        }

        val dGroups = FinancialEngine.groupDaily(monthTxs, memoList)
        val wGroups = FinancialEngine.groupWeekly(monthTxs)

        val prevMonthCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevStart = (prevMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val prevEnd = (prevMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val prevTxs = allTx.filter { it.transaction.dateMillis in prevStart..prevEnd }
        val summary = FinancialEngine.calculatePeriodSummary(monthTxs, prevTxs)
        val catSpendings = FinancialEngine.calculateCategorySpending(monthTxs, TransactionType.EXPENSE)

        return MonthPageData(
            monthCal = startCal,
            transactions = monthTxs,
            dailyGroups = dGroups,
            weeklyGroups = wGroups,
            periodSummary = summary,
            categorySpendings = catSpendings
        )
    }

    fun setMonth(year: Int, monthIndex: Int) {
        val newCal = Calendar.getInstance()
        newCal.set(Calendar.YEAR, year)
        newCal.set(Calendar.MONTH, monthIndex)
        newCal.set(Calendar.DAY_OF_MONTH, 1)
        _currentMonth.value = newCal
    }

    fun setHomeSubTab(tab: HomeSubTab) {
        _homeSubTab.value = tab
    }

    fun setFilter(filter: TransactionFilterState) {
        _filterState.value = filter
    }

    fun clearFilter() {
        _filterState.value = TransactionFilterState()
    }

    // Bulk selection
    fun toggleTransactionSelection(id: Long) {
        val current = _selectedTransactionIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedTransactionIds.value = current
    }

    fun clearSelection() {
        _selectedTransactionIds.value = emptySet()
    }

    fun selectAllVisible() {
        _selectedTransactionIds.value = currentMonthTransactions.value.map { it.transaction.id }.toSet()
    }

    fun selectAllTransactions(ids: Collection<Long>) {
        _selectedTransactionIds.value = ids.toSet()
    }

    fun deleteSelectedTransactions() {
        val ids = _selectedTransactionIds.value.toList()
        deleteWithUndo(ids)
    }

    private fun deleteWithUndo(ids: List<Long>) = launchMutation {
        val deleted = database.withTransaction {
            val rows = ids.chunked(500).flatMap { database.transactionDao().getByIds(it) }
            repository.deleteTransactionsByIds(ids)
            rows
        }
        _selectedTransactionIds.value = emptySet()
        if (deleted.isNotEmpty()) noticeChannel.send(UiNotice("${deleted.size} transaction(s) deleted", "Undo") {
            launchMutation { database.withTransaction { deleted.chunked(500).forEach { database.transactionDao().restoreDeleted(it) } } }
        })
    }

    fun bulkChangeCategory(categoryId: Long) {
        launchMutation {
            val ids = _selectedTransactionIds.value.toList()
            repository.bulkUpdateCategory(ids, categoryId)
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun bulkChangeAccount(accountId: Long) {
        launchMutation {
            val ids = _selectedTransactionIds.value.toList()
            repository.bulkUpdateAccount(ids, accountId)
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun bulkChangeDate(dateMillis: Long) {
        launchMutation {
            val ids = _selectedTransactionIds.value.toList()
            repository.bulkUpdateDate(ids, dateMillis)
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun bulkChangeNote(note: String, append: Boolean = false) {
        launchMutation {
            val ids = _selectedTransactionIds.value.toList()
            if (append) {
                database.withTransaction {
                ids.chunked(500).flatMap { database.transactionDao().getByIds(it) }.forEach { tx ->
                    val newNote = if (tx.note.isBlank()) note else "${tx.note} | $note"
                    repository.updateTransaction(tx.copy(note = newNote))
                }
                }
            } else {
                repository.bulkUpdateNote(ids, note)
            }
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun bulkChangePayee(payee: String) {
        launchMutation {
            val ids = _selectedTransactionIds.value.toList()
            repository.bulkUpdatePayee(ids, payee)
            _selectedTransactionIds.value = emptySet()
        }
    }

    // Transaction CRUD
    fun saveTransaction(
        id: Long = 0,
        type: TransactionType,
        dateMillis: Long,
        amount: Long,
        accountId: Long,
        toAccountId: Long? = null,
        transferFee: Long = 0L,
        categoryId: Long,
        subcategoryId: Long? = null,
        payee: String = "",
        note: String = "",
        tags: String = "",
        receiptUri: String? = null,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        isExcludedFromStats: Boolean = false,
        saveAsBookmark: Boolean = false,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (!_transactionSaving.compareAndSet(false, true)) return
        launchMutation(onError) {
            try {
            database.withTransaction {
            require(amount > 0 && transferFee >= 0) { "Enter a positive amount" }
            require(database.accountDao().getAccountById(accountId) != null) { "Choose an account" }
            require(type != TransactionType.TRANSFER || (toAccountId != null && toAccountId != accountId && database.accountDao().getAccountById(toAccountId) != null)) { "Choose a different destination account" }
            val category = database.categoryDao().getCategoryById(categoryId)
            require(type == TransactionType.TRANSFER || (category?.type == type && category.parentId == null)) { "Choose a category for this transaction type" }
            require(subcategoryId == null || database.categoryDao().getCategoryById(subcategoryId)?.parentId == categoryId) { "Choose a valid subcategory" }
            val prior = if (id != 0L) database.transactionDao().getTransactionById(id) else null
            require(id == 0L || prior != null) { "This transaction was deleted" }
            val tx = TransactionEntity(
                id = id,
                type = type,
                dateMillis = dateMillis,
                amount = amount,
                accountId = accountId,
                toAccountId = toAccountId,
                transferFee = transferFee,
                categoryId = categoryId,
                subcategoryId = subcategoryId,
                payee = payee,
                note = note,
                tags = tags,
                receiptUri = receiptUri,
                paymentMethod = paymentMethod,
                isExcludedFromStats = isExcludedFromStats,
                recurringRuleId = prior?.recurringRuleId, installmentId = prior?.installmentId,
                occurrenceKey = prior?.occurrenceKey
            )
            if (id == 0L) {
                repository.insertTransaction(tx)
            } else {
                repository.updateTransaction(tx)
            }

            if (saveAsBookmark) {
                repository.insertBookmark(
                    Bookmark(
                        title = payee.ifBlank { "Favorite Transaction" },
                        type = type,
                        amount = amount,
                        accountId = accountId,
                        toAccountId = toAccountId,
                        categoryId = categoryId,
                        subcategoryId = subcategoryId,
                        payee = payee,
                        note = note,
                        paymentMethod = paymentMethod
                    )
                )
            }

            }

            // Sync currentMonth if transaction was saved for a different month so it is visible immediately
            val txCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val curCal = _currentMonth.value
            if (txCal.get(Calendar.YEAR) != curCal.get(Calendar.YEAR) ||
                txCal.get(Calendar.MONTH) != curCal.get(Calendar.MONTH)) {
                _currentMonth.value = (txCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }

            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
            } finally { _transactionSaving.value = false }
        }
    }

    fun deleteTransaction(item: TransactionEntity) {
        deleteWithUndo(listOf(item.id))
    }

    // Account CRUD
    fun saveAccount(account: Account) {
        launchMutation {
            if (account.id == 0L) {
                repository.insertAccount(account)
            } else {
                repository.updateAccount(account)
            }
        }
    }

    fun deleteAccount(account: Account) {
        launchMutation {
            repository.deleteAccount(account)
        }
    }

    fun toggleHideAccount(id: Long, hidden: Boolean) {
        launchMutation {
            repository.setAccountHidden(id, hidden)
        }
    }

    // Category CRUD
    fun saveCategory(category: Category) {
        launchMutation {
            if (category.id == 0L) {
                repository.insertCategory(category)
            } else {
                repository.updateCategory(category)
            }
        }
    }

    fun deleteCategory(category: Category) {
        launchMutation {
            repository.deleteCategory(category)
        }
    }

    // Budget CRUD
    fun saveBudget(categoryId: Long, monthString: String, amount: Long) {
        launchMutation {
            repository.insertBudget(Budget(categoryId = categoryId, monthString = monthString, amount = amount))
        }
    }

    fun deleteBudget(budget: Budget) {
        launchMutation {
            repository.deleteBudget(budget)
        }
    }

    // Recurring & Installments
    fun saveRecurring(recurring: RecurringTransaction) {
        launchMutation {
            if (recurring.id == 0L) repository.insertRecurring(recurring) else repository.updateRecurring(recurring)
        }
    }

    fun deleteRecurring(recurring: RecurringTransaction) {
        launchMutation {
            repository.deleteRecurring(recurring)
        }
    }

    fun saveInstallment(plan: InstallmentPlan) {
        launchMutation {
            if (plan.id == 0L) repository.insertInstallment(plan) else repository.updateInstallment(plan)
        }
    }

    fun deleteInstallment(plan: InstallmentPlan) {
        launchMutation {
            repository.deleteInstallment(plan)
        }
    }

    // Memos
    fun saveMemo(dateString: String, text: String, colorHex: String = "#10B981") {
        launchMutation {
            repository.insertMemo(DailyMemo(dateString = dateString, memoText = text, colorHex = colorHex))
        }
    }

    fun deleteMemo(memo: DailyMemo) {
        launchMutation {
            repository.deleteMemo(memo)
        }
    }

    // Bookmarks
    fun deleteBookmark(bookmark: Bookmark) {
        launchMutation {
            repository.deleteBookmark(bookmark)
        }
    }

    fun setUserName(name: String) {
        launchMutation {
            repository.setSetting("user_name", name)
        }
    }

    fun setFinancialGoal(goal: String) {
        launchMutation {
            repository.setSetting("financial_goal", goal)
        }
    }

    fun setMonthlyBudgetGoal(amount: Long) {
        launchMutation {
            repository.setSetting("monthly_budget_goal", amount.toString())
        }
    }

    fun setShowAccountsTab(enabled: Boolean) {
        launchMutation {
            repository.setSetting("show_accounts_tab", enabled.toString())
        }
    }

    fun completeOnboarding(
        name: String,
        currency: String,
        isIndianFormat: Boolean,
        accounts: List<com.example.ui.screens.onboarding.AccountSetupItem>,
        monthlyBudget: Long?,
        autoDistributeCategories: Boolean,
        pin: String?,
        goal: String,
        onComplete: () -> Unit = {}
    ) {
        launchMutation {
            database.withTransaction {
            repository.setSetting("user_name", name)
            repository.setSetting("primary_currency", currency)
            repository.setSetting("currency_code", currency)
            repository.setSetting("indian_number_format", isIndianFormat.toString())
            repository.setSetting("financial_goal", goal)
            repository.setSetting("app_pin", pin ?: "")
            if (monthlyBudget != null && monthlyBudget > 0L) {
                repository.setSetting("monthly_budget_goal", monthlyBudget.toString())
            }

            // Insert / Replace customized accounts
            if (accounts.isNotEmpty()) {
                // If this is initial setup with default data, replace default accounts
                val newAccounts = accounts.mapIndexed { index, item ->
                    val balanceMinor = CurrencyFormatter.parseToMinorUnits(item.initialBalanceText)
                    val limitMinor = if (item.creditLimitText.isNotBlank()) CurrencyFormatter.parseToMinorUnits(item.creditLimitText) else null
                    Account(
                        name = item.name,
                        type = item.type,
                        groupName = when (item.type) {
                            AccountType.BANK -> "Banks"
                            AccountType.CASH -> "Cash & Wallets"
                            AccountType.CREDIT_CARD -> "Cards"
                            AccountType.INVESTMENT -> "Investments"
                            AccountType.SAVINGS -> "Savings"
                            else -> "Other"
                        },
                        institution = item.institution,
                        initialBalance = balanceMinor,
                        creditLimit = limitMinor ?: 0L,
                        currency = currency,
                        sortOrder = index + 1
                    )
                }

                // If user customized during onboarding, clear and insert clean list
                val hasReferences = database.transactionDao().count() > 0 ||
                    database.recurringDao().getAllRecurring().first().isNotEmpty() ||
                    database.installmentDao().getAllInstallments().first().isNotEmpty() ||
                    database.bookmarkDao().getAllBookmarks().first().isNotEmpty()
                if (hasReferences) {
                    val existing = database.accountDao().getAllAccounts().first()
                    newAccounts.forEach { account ->
                        val matching = existing.firstOrNull { it.name == account.name && it.type == account.type }
                        if (matching == null) database.accountDao().insert(account)
                        else database.accountDao().update(account.copy(id = matching.id))
                    }
                } else {
                    database.accountDao().deleteAll()
                    database.accountDao().insertAll(newAccounts)
                }
            }

            // Configure category budgets if monthly budget requested
            if (monthlyBudget != null && monthlyBudget > 0L && autoDistributeCategories) {
                val cal = Calendar.getInstance()
                val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
                val allCats = repository.database.categoryDao().getAllCategoriesSync()
                val expenseCats = allCats.filter { it.type == TransactionType.EXPENSE && it.parentId == null }

                // Category allocation percentages
                val allocations = mapOf(
                    "Food & Dining" to 0.30f,
                    "Groceries" to 0.20f,
                    "Bills & Utilities" to 0.20f,
                    "Shopping" to 0.15f,
                    "Entertainment" to 0.15f
                )

                allocations.forEach { (catName, ratio) ->
                    val matchedCat = expenseCats.firstOrNull { it.name.contains(catName, ignoreCase = true) }
                    if (matchedCat != null) {
                        val catBudgetAmount = (monthlyBudget * ratio).toLong()
                        repository.insertBudget(
                            Budget(
                                categoryId = matchedCat.id,
                                monthString = monthStr,
                                amount = catBudgetAmount
                            )
                        )
                    }
                }
            }

            repository.setSetting("onboarding_completed", "true")
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun restartOnboarding() {
        launchMutation {
            repository.setSetting("onboarding_completed", "false")
        }
    }

    fun skipOnboarding(onComplete: () -> Unit = {}) {
        launchMutation {
            repository.setSetting("onboarding_completed", "true")
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Import / Restore actions
    fun importUniversalCsv(csvContent: String, onResult: (com.example.domain.ImportResult) -> Unit) {
        launchMutation(onError = { message -> onResult(ImportResult(false, 0, message = message)) }) {
            val curr = primaryCurrency.value
            val result = com.example.domain.BackupManager.importTransactionsUniversal(
                database = database,
                csvContent = csvContent,
                defaultCurrency = curr
            )
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun restoreDatabaseJson(jsonContent: String, onResult: (Boolean) -> Unit) {
        launchMutation(onError = { onResult(false) }) {
            val success = com.example.domain.BackupManager.restoreJsonBackup(database, jsonContent)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    // Settings
    fun setPrimaryCurrency(code: String) {
        launchMutation {
            database.withTransaction {
            repository.setSetting("primary_currency", code)
            repository.setSetting("currency_code", code)
            }
        }
    }

    fun setIndianNumberFormat(enabled: Boolean) {
        launchMutation {
            repository.setSetting("indian_number_format", enabled.toString())
        }
    }

    fun setThemePalette(palette: AppThemePalette) {
        launchMutation {
            repository.setSetting("theme_palette", palette.id)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        launchMutation {
            repository.setSetting("theme_mode", mode.name)
        }
    }

    fun setAppPin(pin: String?) {
        launchMutation {
            repository.setSetting("app_pin", pin ?: "")
        }
        _isAppUnlocked.value = true
    }

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        if (isPinEnabled.value && !storedPin.value.isNullOrBlank()) {
            _isAppUnlocked.value = false
        }
    }

    // Data Actions
    fun seedDemoData(onComplete: () -> Unit = {}) {
        launchMutation {
            DemoDataGenerator.seedComprehensiveDemoData(database)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun resetAllData(onComplete: () -> Unit = {}) {
        launchMutation {
            database.withTransaction {
            DemoDataGenerator.clearAllData(database)
            com.example.data.local.populateInitialDefaults(database)
            repository.setSetting("onboarding_completed", "true")
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun resetTransactionsOnly(onComplete: () -> Unit = {}) {
        launchMutation {
            database.withTransaction {
            database.transactionDao().deleteAll()
            database.memoDao().deleteAll()
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // PC Manager Controller Functions
    fun startPcServer(context: android.content.Context) {
        launchMutation {
        val passcodeOn = isPcPasscodeEnabled.value
        val code = pcPasscode.value
        val started = PCManagerServer.start(
            context = context,
            repo = repository,
            requestedPort = 8888,
            passcodeOn = passcodeOn,
            code = code,
            onStatusChange = { running, ip, port ->
                _isPcServerRunning.value = running
                _pcServerIp.value = ip
                _pcServerPort.value = port
                _pcServerUrl.value = if (running) "http://$ip:$port" else ""
            }
        )
        if (started) {
            _isPcServerRunning.value = true
            _pcServerIp.value = PCManagerServer.ipAddress
            _pcServerPort.value = PCManagerServer.port
            _pcServerUrl.value = PCManagerServer.serverUrl
        }
        }
    }

    fun stopPcServer() {
        launchMutation {
        PCManagerServer.stop()
        _isPcServerRunning.value = false
        _pcServerUrl.value = ""
        }
    }

    fun refreshPcIp(context: android.content.Context) {
        launchMutation {
        val ip = PCManagerServer.refreshIp(context)
        _pcServerIp.value = ip
        if (_isPcServerRunning.value) {
            _pcServerUrl.value = "http://$ip:${_pcServerPort.value}"
        }
        }
    }

    fun setPcPasscodeEnabled(enabled: Boolean) {
        launchMutation {
            repository.setSetting("pc_passcode_enabled", enabled.toString())
            PCManagerServer.updatePasscodeConfig(enabled, pcPasscode.value)
        }
    }

    fun setPcPasscode(code: String) {
        launchMutation {
            repository.setSetting("pc_passcode", code)
            PCManagerServer.updatePasscodeConfig(isPcPasscodeEnabled.value, code)
        }
    }

    init {
        // Run background recurring/installment check on launch
        if (databaseOverride == null) launchMutation {
            RecurringWorker.schedule(application)
        }
    }
}

data class MonthPageData(
    val monthCal: Calendar,
    val transactions: List<TransactionWithDetails>,
    val dailyGroups: List<DailyAggregation>,
    val weeklyGroups: List<WeeklyAggregation>,
    val periodSummary: PeriodSummary,
    val categorySpendings: List<CategorySpending>
)
