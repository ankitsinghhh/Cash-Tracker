package com.example

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.ui.MainViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionSaveTest {
    @Test fun failedSaveResetsBusyStateAndCanBeRetriedWithoutPartialBookmark() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val application = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).build()
        val model = MainViewModel(application, db)
        val store = ViewModelStore().apply { put("test", model) }
        try {
            db.accountDao().insert(Account(1, "Cash", AccountType.CASH))
            db.categoryDao().insert(Category(1, "Food", TransactionType.EXPENSE))
            val error = CompletableDeferred<String>()
            model.saveTransaction(type = TransactionType.EXPENSE, dateMillis = 1000, amount = 1250,
                accountId = 999, categoryId = 1, saveAsBookmark = true, onError = { error.complete(it) })
            assertEquals("Choose an account", withTimeout(10000) { error.await() })
            assertFalse(model.transactionSaving.value)
            assertEquals(0, db.transactionDao().count())
            assertTrue(db.bookmarkDao().getAllBookmarks().first().isEmpty())
            val saved = CompletableDeferred<Unit>()
            model.saveTransaction(type = TransactionType.EXPENSE, dateMillis = 1000, amount = 1250,
                accountId = 1, categoryId = 1, note = "Retained draft", saveAsBookmark = true,
                onComplete = { saved.complete(Unit) })
            withTimeout(10000) { saved.await(); model.transactionSaving.first { !it } }
            assertEquals("Retained draft", db.transactionDao().getAllTransactionsSync().single().note)
            assertEquals(1, db.bookmarkDao().getAllBookmarks().first().size)
        } finally { store.clear(); db.close(); Dispatchers.resetMain() }
    }
}
