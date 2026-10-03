package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.domain.BackupManager
import com.example.domain.FinancialEngine
import com.example.domain.RecurringProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataReliabilityTest {
    private lateinit var database: AppDatabase
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
    }
    @After fun close() = database.close()

    private suspend fun defaults() {
        database.accountDao().insert(Account(id = 1, name = "Cash", type = AccountType.CASH))
        database.categoryDao().insert(Category(id = 1, name = "Food", type = TransactionType.EXPENSE))
    }

    @Test fun concurrentRecurringRunsCatchUpExactlyOnce() = runBlocking {
        defaults()
        val due = 1705320000000L
        database.recurringDao().insert(RecurringTransaction(id = 1, name = "Daily", type = TransactionType.EXPENSE,
            amount = 100, accountId = 1, categoryId = 1, frequency = RecurringFrequency.DAILY,
            startDateMillis = due, nextDueDateMillis = due))
        withTimeout(15000) {
            coroutineScope { List(4) { async(Dispatchers.Default) { RecurringProcessor.processDueItems(database, due + 2 * 86400000L) } }.awaitAll() }
        }
        val records = database.transactionDao().getAllTransactionsSync()
        assertEquals(3, records.size)
        assertEquals(3, records.map { it.occurrenceKey }.distinct().size)
        assertEquals(300L, records.sumOf { it.amount })
    }

    @Test fun installmentsCompleteWithoutDuplicateOccurrences() = runBlocking {
        defaults()
        val due = 1705320000000L
        database.installmentDao().insert(InstallmentPlan(id = 1, name = "Phone", totalAmount = 300,
            totalInstallments = 3, monthlyAmount = 100, accountId = 1, categoryId = 1,
            startDateMillis = due, nextDueDateMillis = due))
        repeat(2) { withTimeout(10000) { RecurringProcessor.processDueItems(database, due + 100 * 86400000L) } }
        assertEquals(3, database.transactionDao().count())
        assertTrue(database.installmentDao().getById(1)!!.isCompleted)
    }

    @Test fun invalidBackupAndCsvLeaveExistingDataIntact() = runBlocking {
        defaults()
        database.transactionDao().insert(TransactionEntity(id = 1, type = TransactionType.EXPENSE,
            dateMillis = 1705320000000L, amount = 100, accountId = 1, categoryId = 1))
        val backup = BackupManager.createJsonBackup(database)
        assertFalse(BackupManager.restoreJsonBackup(database, backup.replace("\"accountId\": 1", "\"accountId\": 999")))
        assertEquals(1, database.transactionDao().count())
        val csv = "Date,Account,Category,Amount,Type\n2024-01-15,New bank,Other,100,Expense\ninvalid,Bad bank,Other,200,Expense"
        assertFalse(BackupManager.importTransactionsUniversal(database, csv).success)
        assertEquals(listOf("Cash"), database.accountDao().getAllAccounts().first().map { it.name })
        assertEquals(1, database.transactionDao().count())
        assertTrue(BackupManager.restoreJsonBackup(database, backup))
        assertEquals(1, database.transactionDao().count())
    }

    @Test fun searchTotalsIncludeSubcategoriesDatesAndTransferFees() = runBlocking {
        defaults()
        database.categoryDao().insert(Category(id = 2, name = "Coffee", type = TransactionType.EXPENSE, parentId = 1))
        database.transactionDao().insertAll(listOf(
            TransactionEntity(type = TransactionType.EXPENSE, dateMillis = 1000, amount = 250, accountId = 1, categoryId = 1, subcategoryId = 2),
            TransactionEntity(type = TransactionType.EXPENSE, dateMillis = 2000, amount = 500, accountId = 1, categoryId = 1, isExcludedFromStats = true),
            TransactionEntity(type = TransactionType.TRANSFER, dateMillis = 3000, amount = 1000, accountId = 1, categoryId = 0, transferFee = 20)
        ))
        val coffee = database.transactionDao().searchTotals("Coffee", null, null, 2, false, 0, 1500, "", "").first()
        assertEquals(1, coffee.count)
        assertEquals(250L, coffee.expense)
        val all = database.transactionDao().searchTotals("", null, null, null, false, null, null, "", "").first()
        assertEquals(3, all.count)
        assertEquals(270L, all.expense)
    }

    @Test fun importRollsBackAlreadyWrittenBatchesAndRejectsInvalidCalendarDates() = runBlocking {
        defaults()
        val valid = "Date,Account,Category,Amount,Type\n" + "2024-01-15,New bank,Other,100,Expense\n".repeat(501)
        assertFalse(BackupManager.importTransactionsUniversal(database, valid + "2024-02-31,Bad bank,Other,200,Expense").success)
        assertEquals(0, database.transactionDao().count())
        assertEquals(listOf("Cash"), database.accountDao().getAllAccounts().first().map { it.name })
        val imported = BackupManager.importTransactionsUniversal(database, valid)
        assertTrue(imported.success)
        assertEquals(501, imported.totalImported)
        assertEquals(501, database.transactionDao().count())
        assertTrue(database.transactionDao().getAllTransactionsSync().all { it.note.isEmpty() && it.subcategoryId == null })
    }

    @Test fun accountBalancesPreserveTransfersFeesAndLiabilitySemantics() {
        val accounts = listOf(Account(1, "Cash", AccountType.CASH, initialBalance = 10000),
            Account(2, "Card", AccountType.CREDIT_CARD, creditLimit = 50000))
        val transactions = listOf(
            TransactionEntity(type = TransactionType.INCOME, dateMillis = 1, amount = 500, accountId = 1, categoryId = 1),
            TransactionEntity(type = TransactionType.EXPENSE, dateMillis = 1, amount = 300, accountId = 2, categoryId = 1),
            TransactionEntity(type = TransactionType.TRANSFER, dateMillis = 1, amount = 100, accountId = 1, toAccountId = 2, transferFee = 10, categoryId = 0)
        )
        val balances = FinancialEngine.calculateAccountBalances(accounts, transactions)
        assertEquals(10390L, balances[0].calculatedBalance)
        assertEquals(200L, balances[1].calculatedBalance)
        assertEquals(49800L, balances[1].availableCredit)
    }

    @Test fun fiftyThousandRecordLedgerRemainsCorrect() {
        val accounts = (1L..100L).map { Account(it, "Account $it", AccountType.CASH) }
        val transactions = List(50000) { index -> TransactionEntity(type = TransactionType.EXPENSE,
            dateMillis = index.toLong(), amount = 100, accountId = index % 100L + 1, categoryId = 1) }
        assertEquals(-5000000L, FinancialEngine.calculateAccountBalances(accounts, transactions).sumOf { it.calculatedBalance })
    }
}
