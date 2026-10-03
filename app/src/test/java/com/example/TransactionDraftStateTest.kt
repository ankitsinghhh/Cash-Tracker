package com.example

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.screens.transaction.AddEditTransactionScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [35])
class TransactionDraftStateTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draftSurvivesRestorationAndInvalidSave() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).build()
        runBlocking {
            db.accountDao().insert(Account(1, "Cash", AccountType.CASH))
            db.categoryDao().insert(Category(1, "Food", TransactionType.EXPENSE))
        }
        val model = MainViewModel(application, db)
        val store = ViewModelStore().apply { put("draft", model) }
        try {
            val restoration = StateRestorationTester(compose)
            restoration.setContent { MyApplicationTheme { AddEditTransactionScreen(model, onNavigateBack = {}) } }
            compose.waitUntil(10000) { compose.onAllNodesWithTag("save_transaction_button").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Enter note...").performScrollTo().performTextReplacement("Draft retained after rotation")
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithText("Draft retained after rotation").assertExists()
            compose.onNodeWithTag("save_transaction_button").performClick()
            compose.onNodeWithText("Enter an amount greater than zero").assertExists()
            compose.onNodeWithText("Draft retained after rotation").assertExists()
            runBlocking { assertEquals(0, db.transactionDao().count()) }
        } finally { store.clear(); db.close() }
    }
}
