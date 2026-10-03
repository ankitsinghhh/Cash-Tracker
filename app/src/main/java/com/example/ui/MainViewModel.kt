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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = FinanceRepository(database)

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
        .map { it == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
    val accounts = repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAccounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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

    // Month Filtered Transactions
    val currentMonthTransactions = combine(
        allTransactionsWithDetails,
        _currentMonth,
        _filterState
    ) { allTx, monthCal, filter ->
        val startCal = monthCal.clone() as Calendar
        startCal.set(Calendar.DAY_OF_MONTH, 1)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = monthCal.clone() as Calendar
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        val startMillis = filter.startDateMillis ?: startCal.timeInMillis
        val endMillis = filter.endDateMillis ?: endCal.timeInMillis

        allTx.filter { item ->
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
                    (item.account?.name?.contains(filter.searchQuery, ignoreCase = true) == true)
            val matchesReceipt = !filter.onlyWithReceipt || tx.receiptUri != null

            withinDate && matchesType && matchesAccount && matchesCategory && matchesSearch && matchesReceipt
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Period Summary
    val monthPeriodSummary = combine(
        allTransactionsWithDetails,
        _currentMonth
    ) { allTx, monthCal ->
        val curStart = (monthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val curEnd = (monthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val prevMonthCal = (monthCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevStart = (prevMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val prevEnd = (prevMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val currentTxs = allTx.filter { it.transaction.dateMillis in curStart..curEnd }
        val prevTxs = allTx.filter { it.transaction.dateMillis in prevStart..prevEnd }

        FinancialEngine.calculatePeriodSummary(currentTxs, prevTxs)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PeriodSummary(0, 0, 0, 0f, 0)
    )

    // Daily Groups
    val dailyGroups = combine(currentMonthTransactions, memos) { txs, memoList ->
        FinancialEngine.groupDaily(txs, memoList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly Groups
    val weeklyGroups = currentMonthTransactions.map { txs ->
        FinancialEngine.groupWeekly(txs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Historical Groups
    val monthlyHistoricalGroups = allTransactionsWithDetails.map { txs ->
        FinancialEngine.groupMonthly(txs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Spending for current month
    val categorySpendings = currentMonthTransactions.map { txs ->
        FinancialEngine.calculateCategorySpending(txs, TransactionType.EXPENSE)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryIncomes = currentMonthTransactions.map { txs ->
        FinancialEngine.calculateCategorySpending(txs, TransactionType.INCOME)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Merchant Analytics
    val merchantStats = allTransactionsWithDetails.map { txs ->
        FinancialEngine.calculateMerchantAnalytics(txs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Spending Trend & Velocity (Time-Series)
    val dailySpendingTrend = combine(
        currentMonthTransactions,
        _currentMonth
    ) { txs, monthCal ->
        FinancialEngine.calculateDailySpendingTrend(txs, monthCal)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailySpendingTrendData(emptyList(), emptyList(), 0L, 1, 0L, 0, 0L)
    )

    // Day of Week Spending Habit Distribution
    val dayOfWeekDistribution = currentMonthTransactions.map { txs ->
        FinancialEngine.calculateDayOfWeekDistribution(txs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month over Month Category Comparison
    val monthOverMonthCategoryComparison = combine(
        allTransactionsWithDetails,
        _currentMonth
    ) { allTx, monthCal ->
        val curStart = (monthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val curEnd = (monthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val prevMonthCal = (monthCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevStart = (prevMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val prevEnd = (prevMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val currentTxs = allTx.filter { it.transaction.dateMillis in curStart..curEnd }
        val prevTxs = allTx.filter { it.transaction.dateMillis in prevStart..prevEnd }

        FinancialEngine.calculateMonthOverMonthCategoryComparison(currentTxs, prevTxs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                    (item.account?.name?.contains(filter.searchQuery, ignoreCase = true) == true)
            val matchesReceipt = !filter.onlyWithReceipt || tx.receiptUri != null

            withinDate && matchesType && matchesAccount && matchesCategory && matchesSearch && matchesReceipt
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
        viewModelScope.launch(Dispatchers.IO) {
            val ids = _selectedTransactionIds.value.toList()
            repository.deleteTransactionsByIds(ids)
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun bulkChangeCategory(categoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = _selectedTransactionIds.value.toList()
            repository.bulkUpdateCategory(ids, categoryId)
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun bulkChangeAccount(accountId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = _selectedTransactionIds.value.toList()
            repository.bulkUpdateAccount(ids, accountId)
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun bulkChangeDate(dateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = _selectedTransactionIds.value.toList()
            repository.bulkUpdateDate(ids, dateMillis)
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun bulkChangeNote(note: String, append: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = _selectedTransactionIds.value.toList()
            if (append) {
                val all = repository.database.transactionDao().getAllTransactionsSync()
                all.filter { ids.contains(it.id) }.forEach { tx ->
                    val newNote = if (tx.note.isBlank()) note else "${tx.note} | $note"
                    repository.updateTransaction(tx.copy(note = newNote))
                }
            } else {
                repository.bulkUpdateNote(ids, note)
            }
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun bulkChangePayee(payee: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
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
                isExcludedFromStats = isExcludedFromStats
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
        }
    }

    fun deleteTransaction(item: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(item)
        }
    }

    // Account CRUD
    fun saveAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            if (account.id == 0L) {
                repository.insertAccount(account)
            } else {
                repository.updateAccount(account)
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAccount(account)
        }
    }

    fun toggleHideAccount(id: Long, hidden: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAccountHidden(id, hidden)
        }
    }

    // Category CRUD
    fun saveCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            if (category.id == 0L) {
                repository.insertCategory(category)
            } else {
                repository.updateCategory(category)
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    // Budget CRUD
    fun saveBudget(categoryId: Long, monthString: String, amount: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBudget(Budget(categoryId = categoryId, monthString = monthString, amount = amount))
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBudget(budget)
        }
    }

    // Recurring & Installments
    fun saveRecurring(recurring: RecurringTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            if (recurring.id == 0L) repository.insertRecurring(recurring) else repository.updateRecurring(recurring)
        }
    }

    fun deleteRecurring(recurring: RecurringTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecurring(recurring)
        }
    }

    fun saveInstallment(plan: InstallmentPlan) {
        viewModelScope.launch(Dispatchers.IO) {
            if (plan.id == 0L) repository.insertInstallment(plan) else repository.updateInstallment(plan)
        }
    }

    fun deleteInstallment(plan: InstallmentPlan) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInstallment(plan)
        }
    }

    // Memos
    fun saveMemo(dateString: String, text: String, colorHex: String = "#10B981") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMemo(DailyMemo(dateString = dateString, memoText = text, colorHex = colorHex))
        }
    }

    fun deleteMemo(memo: DailyMemo) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemo(memo)
        }
    }

    // Bookmarks
    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmark(bookmark)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("user_name", name)
        }
    }

    fun setFinancialGoal(goal: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("financial_goal", goal)
        }
    }

    fun setMonthlyBudgetGoal(amount: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("monthly_budget_goal", amount.toString())
        }
    }

    fun setShowAccountsTab(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
                val existingAccs = repository.database.accountDao().getAccountById(1L)
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
                repository.database.accountDao().deleteAll()
                repository.database.accountDao().insertAll(newAccounts)
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
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun restartOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("onboarding_completed", "false")
        }
    }

    fun skipOnboarding(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("onboarding_completed", "true")
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Import / Restore actions
    fun importUniversalCsv(csvContent: String, onResult: (com.example.domain.ImportResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            val success = com.example.domain.BackupManager.restoreJsonBackup(database, jsonContent)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    // Settings
    fun setPrimaryCurrency(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("primary_currency", code)
            repository.setSetting("currency_code", code)
        }
    }

    fun setIndianNumberFormat(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("indian_number_format", enabled.toString())
        }
    }

    fun setThemePalette(palette: AppThemePalette) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("theme_palette", palette.id)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("theme_mode", mode.name)
        }
    }

    fun setAppPin(pin: String?) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            DemoDataGenerator.seedComprehensiveDemoData(database)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun resetAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            DemoDataGenerator.clearAllData(database)
            com.example.data.local.populateInitialDefaults(database)
            repository.setSetting("onboarding_completed", "true")
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun resetTransactionsOnly(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            database.transactionDao().deleteAll()
            database.memoDao().deleteAll()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // PC Manager Controller Functions
    fun startPcServer(context: android.content.Context) {
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

    fun stopPcServer() {
        PCManagerServer.stop()
        _isPcServerRunning.value = false
        _pcServerUrl.value = ""
    }

    fun refreshPcIp(context: android.content.Context) {
        val ip = PCManagerServer.refreshIp(context)
        _pcServerIp.value = ip
        if (_isPcServerRunning.value) {
            _pcServerUrl.value = "http://$ip:${_pcServerPort.value}"
        }
    }

    fun setPcPasscodeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("pc_passcode_enabled", enabled.toString())
            PCManagerServer.updatePasscodeConfig(enabled, pcPasscode.value)
        }
    }

    fun setPcPasscode(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("pc_passcode", code)
            PCManagerServer.updatePasscodeConfig(isPcPasscodeEnabled.value, code)
        }
    }

    init {
        // Run background recurring/installment check on launch
        viewModelScope.launch(Dispatchers.IO) {
            RecurringProcessor.processDueItems(database)
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
