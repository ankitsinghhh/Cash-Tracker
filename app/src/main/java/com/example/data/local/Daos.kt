package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun getAllActiveAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountByIdFlow(id: Long): Flow<Account?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<Account>): List<Long>

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("UPDATE accounts SET isHidden = :hidden WHERE id = :id")
    suspend fun setAccountHidden(id: Long, hidden: Boolean)

    @Query("UPDATE accounts SET isArchived = :archived WHERE id = :id")
    suspend fun setAccountArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isHidden = 0 ORDER BY sortOrder ASC, name ASC")
    fun getAllActiveCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllCategoriesSync(): List<Category>

    @Query("SELECT * FROM categories WHERE type = :type AND isHidden = 0 ORDER BY sortOrder ASC, name ASC")
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>): List<Long>

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE dateMillis BETWEEN :startMillis AND :endMillis ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id IN (:ids)")
    suspend fun updateCategoryForIds(ids: List<Long>, categoryId: Long)

    @Query("UPDATE transactions SET accountId = :accountId WHERE id IN (:ids)")
    suspend fun updateAccountForIds(ids: List<Long>, accountId: Long)

    @Query("UPDATE transactions SET dateMillis = :dateMillis WHERE id IN (:ids)")
    suspend fun updateDateForIds(ids: List<Long>, dateMillis: Long)

    @Query("UPDATE transactions SET note = :note WHERE id IN (:ids)")
    suspend fun updateNoteForIds(ids: List<Long>, note: String)

    @Query("UPDATE transactions SET payee = :payee WHERE id IN (:ids)")
    suspend fun updatePayeeForIds(ids: List<Long>, payee: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthString = :monthString OR monthString = 'DEFAULT'")
    fun getBudgetsForMonth(monthString: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<Budget>): List<Long>

    @Delete
    suspend fun delete(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextDueDateMillis ASC")
    fun getActiveRecurring(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDateMillis ASC")
    fun getAllRecurring(): Flow<List<RecurringTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringTransaction): Long

    @Update
    suspend fun update(recurring: RecurringTransaction)

    @Delete
    suspend fun delete(recurring: RecurringTransaction)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installment_plans WHERE isCompleted = 0 ORDER BY nextDueDateMillis ASC")
    fun getActiveInstallments(): Flow<List<InstallmentPlan>>

    @Query("SELECT * FROM installment_plans ORDER BY nextDueDateMillis ASC")
    fun getAllInstallments(): Flow<List<InstallmentPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: InstallmentPlan): Long

    @Update
    suspend fun update(plan: InstallmentPlan)

    @Delete
    suspend fun delete(plan: InstallmentPlan)

    @Query("DELETE FROM installment_plans")
    suspend fun deleteAll()
}

@Dao
interface MemoDao {
    @Query("SELECT * FROM daily_memos WHERE dateString = :dateString")
    fun getMemoForDate(dateString: String): Flow<DailyMemo?>

    @Query("SELECT * FROM daily_memos WHERE dateString LIKE :monthPrefix || '%'")
    fun getMemosForMonth(monthPrefix: String): Flow<List<DailyMemo>>

    @Query("SELECT * FROM daily_memos")
    fun getAllMemos(): Flow<List<DailyMemo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memo: DailyMemo): Long

    @Delete
    suspend fun delete(memo: DailyMemo)

    @Query("DELETE FROM daily_memos")
    suspend fun deleteAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY id DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark): Long

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}

@Dao
interface SettingDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    fun getValueFlow(key: String): Flow<String?>

    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSetting)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}
