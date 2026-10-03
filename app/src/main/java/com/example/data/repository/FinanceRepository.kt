package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.domain.FinancialEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class FinanceRepository(val database: AppDatabase) {

    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val recurringDao = database.recurringDao()
    private val installmentDao = database.installmentDao()
    private val memoDao = database.memoDao()
    private val bookmarkDao = database.bookmarkDao()
    private val settingDao = database.settingDao()

    val accounts: Flow<List<Account>> = accountDao.getAllActiveAccounts()
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val categories: Flow<List<Category>> = categoryDao.getAllActiveCategories()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val budgets: Flow<List<Budget>> = budgetDao.getAllBudgets()
    val recurring: Flow<List<RecurringTransaction>> = recurringDao.getAllRecurring()
    val activeRecurring: Flow<List<RecurringTransaction>> = recurringDao.getActiveRecurring()
    val installments: Flow<List<InstallmentPlan>> = installmentDao.getAllInstallments()
    val activeInstallments: Flow<List<InstallmentPlan>> = installmentDao.getActiveInstallments()
    val memos: Flow<List<DailyMemo>> = memoDao.getAllMemos()
    val bookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    // Combined Flow: Transactions with populated Account, ToAccount, Category, Subcategory
    val transactionsWithDetails: Flow<List<TransactionWithDetails>> = combine(
        transactionDao.getAllTransactions(),
        accountDao.getAllAccounts(),
        categoryDao.getAllCategories()
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
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    // Calculated Account Balances with Real-Time Ledger integrity
    val accountBalances: Flow<List<AccountWithBalance>> = combine(
        accountDao.getAllAccounts(),
        transactionDao.getAllTransactions()
    ) { accList, txList ->
        FinancialEngine.calculateAccountBalances(accList, txList)
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    // Calculated Net Worth Summary
    val netWorth: Flow<NetWorthSummary> = accountBalances.map { balances ->
        FinancialEngine.calculateNetWorth(balances)
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    // Database CRUD
    suspend fun insertTransaction(tx: TransactionEntity): Long = transactionDao.insert(tx)
    suspend fun updateTransaction(tx: TransactionEntity) = transactionDao.update(tx)
    suspend fun deleteTransaction(tx: TransactionEntity) = transactionDao.delete(tx)
    suspend fun deleteTransactionById(id: Long) = transactionDao.deleteById(id)
    suspend fun deleteTransactionsByIds(ids: List<Long>) = transactionDao.deleteByIds(ids)
    suspend fun bulkUpdateCategory(ids: List<Long>, categoryId: Long) = transactionDao.updateCategoryForIds(ids, categoryId)
    suspend fun bulkUpdateAccount(ids: List<Long>, accountId: Long) = transactionDao.updateAccountForIds(ids, accountId)
    suspend fun bulkUpdateDate(ids: List<Long>, dateMillis: Long) = transactionDao.updateDateForIds(ids, dateMillis)
    suspend fun bulkUpdateNote(ids: List<Long>, note: String) = transactionDao.updateNoteForIds(ids, note)
    suspend fun bulkUpdatePayee(ids: List<Long>, payee: String) = transactionDao.updatePayeeForIds(ids, payee)

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
