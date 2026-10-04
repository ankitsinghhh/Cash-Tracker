package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.domain.FinancialEngine
import androidx.paging.*
import androidx.room.withTransaction
import com.example.ui.TransactionFilterState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class FinanceRepository(val database: AppDatabase, private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
    private val reload = MutableStateFlow(0)
    private val _loadedQueries = MutableStateFlow<Set<String>>(emptySet())
    val loadedQueries = _loadedQueries.asStateFlow()
    private val _readErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val readErrors = _readErrors.asStateFlow()
    fun retryReads() { _readErrors.value = emptyMap(); reload.value++ }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun <T> readList(key: String, source: Flow<List<T>>): Flow<List<T>> = reload.flatMapLatest {
        source.onEach {
            _loadedQueries.update { keys -> keys + key }
            _readErrors.update { errors -> errors - key }
        }.catch { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            _readErrors.update { errors -> errors + (key to "Could not load your records. Please retry.") }
            emit(emptyList())
        }
    }.shared()

    private fun <T> Flow<T>.shared() = distinctUntilChanged().shareIn(scope, SharingStarted.WhileSubscribed(5000), replay = 1)

    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val recurringDao = database.recurringDao()
    private val installmentDao = database.installmentDao()
    private val memoDao = database.memoDao()
    private val bookmarkDao = database.bookmarkDao()
    private val settingDao = database.settingDao()

    // One observer gives chart preferences an atomic snapshot after a bulk change.
    val settings: Flow<Map<String, String>> = readList("settings", settingDao.getAllSettings())
        .map { items -> items.associate { it.key to it.value } }.flowOn(Dispatchers.Default).shared()

    val accounts: Flow<List<Account>> = readList("accounts", accountDao.getAllActiveAccounts())
    val allAccounts: Flow<List<Account>> = readList("allAccounts", accountDao.getAllAccounts())
    val categories: Flow<List<Category>> = readList("categories", categoryDao.getAllActiveCategories())
    val allCategories: Flow<List<Category>> = readList("allCategories", categoryDao.getAllCategories())
    val transactions: Flow<List<TransactionEntity>> = readList("transactions", transactionDao.getAllTransactions())
    val budgets: Flow<List<Budget>> = readList("budgets", budgetDao.getAllBudgets())
    val recurring: Flow<List<RecurringTransaction>> = readList("recurring", recurringDao.getAllRecurring())
    val activeRecurring: Flow<List<RecurringTransaction>> = readList("activeRecurring", recurringDao.getActiveRecurring())
    val installments: Flow<List<InstallmentPlan>> = readList("installments", installmentDao.getAllInstallments())
    val activeInstallments: Flow<List<InstallmentPlan>> = readList("activeInstallments", installmentDao.getActiveInstallments())
    val memos: Flow<List<DailyMemo>> = readList("memos", memoDao.getAllMemos())
    val bookmarks: Flow<List<Bookmark>> = readList("bookmarks", bookmarkDao.getAllBookmarks())

    // Combined Flow: Transactions with populated Account, ToAccount, Category, Subcategory
    val transactionsWithDetails: Flow<List<TransactionWithDetails>> = combine(
        transactions,
        allAccounts,
        allCategories
    ) { txList, accList, catList ->
        val accMap = accList.associateBy { it.id }
        val catMap = catList.associateBy { it.id }

        txList.map { tx ->
            TransactionWithDetails(
                transaction = tx,
                account = accMap[tx.accountId],
                toAccount = tx.toAccountId?.let { accMap[it] },
                category = catMap[tx.categoryId],
                subcategory = tx.subcategoryId?.let { catMap[it] }
            )
        }
    }.flowOn(Dispatchers.Default).shared()

    // Calculated Account Balances with Real-Time Ledger integrity
    val accountBalances: Flow<List<AccountWithBalance>> = combine(
        allAccounts,
        transactions
    ) { accList, txList ->
        FinancialEngine.calculateAccountBalances(accList, txList)
    }.flowOn(Dispatchers.Default).shared()

    // Calculated Net Worth Summary
    val netWorth: Flow<NetWorthSummary> = accountBalances.map { balances ->
        FinancialEngine.calculateNetWorth(balances)
    }.flowOn(Dispatchers.Default).shared()

    fun withDetails(source: Flow<List<TransactionEntity>>): Flow<List<TransactionWithDetails>> =
        combine(source, allAccounts, allCategories) { txs, accounts, categories ->
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }
            txs.map { tx -> TransactionWithDetails(tx, accountMap[tx.accountId],
                accountMap[tx.toAccountId], categoryMap[tx.categoryId], categoryMap[tx.subcategoryId]) }
        }.flowOn(Dispatchers.Default)

    val monthlyTotals = readList("monthly", transactionDao.monthlyTotals())

    fun transactionsBetween(start: Long, end: Long) = withDetails(transactionDao.getTransactionsBetween(start, end))

    // One-shot exports must read the database, not the replay cache of a UI flow.
    suspend fun currentTransactionsWithDetails(): List<TransactionWithDetails> = database.withTransaction {
        val accounts = accountDao.getAllAccounts().first().associateBy { it.id }
        val categories = categoryDao.getAllCategoriesSync().associateBy { it.id }
        transactionDao.getAllTransactionsSync().map { tx ->
            TransactionWithDetails(tx, accounts[tx.accountId], accounts[tx.toAccountId],
                categories[tx.categoryId], categories[tx.subcategoryId])
        }
    }

    private fun enrichPages(pages: Flow<PagingData<TransactionEntity>>) =
        combine(pages, allAccounts, allCategories) { data, accounts, categories ->
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }
            data.map { tx -> TransactionWithDetails(tx, accountMap[tx.accountId], accountMap[tx.toAccountId],
                categoryMap[tx.categoryId], categoryMap[tx.subcategoryId]) }
        }.flowOn(Dispatchers.Default)

    fun accountPages(id: Long) = enrichPages(Pager(PagingConfig(50, enablePlaceholders = false)) {
        transactionDao.pageForAccount(id)
    }.flow)

    fun searchPages(filter: TransactionFilterState) = enrichPages(Pager(PagingConfig(50, enablePlaceholders = false)) {
        transactionDao.searchPages(filter.searchQuery.trim(), filter.typeFilter?.name, filter.accountIdFilter,
            filter.categoryIdFilter, filter.onlyWithReceipt, filter.startDateMillis, filter.endDateMillis,
            filter.payeeFilter, filter.tagFilter)
    }.flow)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun searchTotals(filter: TransactionFilterState) = reload.flatMapLatest { transactionDao.searchTotals(filter.searchQuery.trim(),
        filter.typeFilter?.name, filter.accountIdFilter, filter.categoryIdFilter, filter.onlyWithReceipt,
        filter.startDateMillis, filter.endDateMillis, filter.payeeFilter, filter.tagFilter)
        .onEach { _readErrors.update { errors -> errors - "searchTotals" } }
        .catch { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            _readErrors.update { errors -> errors + ("searchTotals" to "Could not load search totals. Please retry.") }
            emit(SearchTotals())
        }
    }

    private suspend fun chunks(ids: List<Long>, action: suspend (List<Long>) -> Unit) = database.withTransaction {
        ids.distinct().chunked(500).forEach { action(it) }
    }

    // Database CRUD
    suspend fun insertTransaction(tx: TransactionEntity): Long = transactionDao.insert(tx)
    suspend fun updateTransaction(tx: TransactionEntity) = transactionDao.update(tx)
    suspend fun deleteTransaction(tx: TransactionEntity) = transactionDao.delete(tx)
    suspend fun deleteTransactionById(id: Long) = transactionDao.deleteById(id)
    suspend fun deleteTransactionsByIds(ids: List<Long>) = chunks(ids) { chunk -> transactionDao.deleteByIds(chunk) }
    suspend fun bulkUpdateCategory(ids: List<Long>, categoryId: Long) = chunks(ids) { chunk -> transactionDao.updateCategoryForIds(chunk, categoryId) }
    suspend fun bulkUpdateAccount(ids: List<Long>, accountId: Long) = chunks(ids) { chunk -> transactionDao.updateAccountForIds(chunk, accountId) }
    suspend fun bulkUpdateDate(ids: List<Long>, dateMillis: Long) = chunks(ids) { chunk -> transactionDao.updateDateForIds(chunk, dateMillis) }
    suspend fun bulkUpdateNote(ids: List<Long>, note: String) = chunks(ids) { chunk -> transactionDao.updateNoteForIds(chunk, note) }
    suspend fun bulkUpdatePayee(ids: List<Long>, payee: String) = chunks(ids) { chunk -> transactionDao.updatePayeeForIds(chunk, payee) }

    suspend fun insertAccount(account: Account): Long = accountDao.insert(account)
    suspend fun updateAccount(account: Account) = accountDao.update(account)
    suspend fun deleteAccount(account: Account) = accountDao.delete(account)
    suspend fun setAccountHidden(id: Long, hidden: Boolean) = accountDao.setAccountHidden(id, hidden)
    suspend fun setAccountArchived(id: Long, archived: Boolean) = accountDao.setAccountArchived(id, archived)

    suspend fun insertCategory(category: Category): Long = categoryDao.insert(category)
    suspend fun updateCategory(category: Category) = categoryDao.update(category)
    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun insertBudget(budget: Budget): Long = budgetDao.insert(budget)
    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)

    suspend fun insertRecurring(recurring: RecurringTransaction): Long = recurringDao.insert(recurring)
    suspend fun updateRecurring(recurring: RecurringTransaction) = recurringDao.update(recurring)
    suspend fun deleteRecurring(recurring: RecurringTransaction) = recurringDao.delete(recurring)

    suspend fun insertInstallment(plan: InstallmentPlan): Long = installmentDao.insert(plan)
    suspend fun updateInstallment(plan: InstallmentPlan) = installmentDao.update(plan)
    suspend fun deleteInstallment(plan: InstallmentPlan) = installmentDao.delete(plan)

    suspend fun insertMemo(memo: DailyMemo): Long = memoDao.insert(memo)
    suspend fun deleteMemo(memo: DailyMemo) = memoDao.delete(memo)

    suspend fun insertBookmark(bookmark: Bookmark): Long = bookmarkDao.insert(bookmark)
    suspend fun deleteBookmark(bookmark: Bookmark) = bookmarkDao.delete(bookmark)

    // Settings
    suspend fun getSetting(key: String): String? = settingDao.getValue(key)
    fun getSettingFlow(key: String): Flow<String?> = settingDao.getValueFlow(key)
    suspend fun setSetting(key: String, value: String) = settingDao.setSetting(AppSetting(key, value))
}
