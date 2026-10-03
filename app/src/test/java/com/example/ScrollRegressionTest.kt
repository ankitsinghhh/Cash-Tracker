package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithDetails
import com.example.data.model.WeeklyAggregation
import com.example.domain.FinancialEngine
import com.example.ui.components.horizontalSwipeListener
import com.example.ui.screens.home.DailyTabContent
import com.example.ui.screens.home.WeeklyTabContent
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [36])
class ScrollRegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun transactions(count: Int) = List(count) { index ->
        TransactionWithDetails(
            transaction = TransactionEntity(
                id = index.toLong() + 1, type = TransactionType.EXPENSE,
                dateMillis = 1705320000000L, amount = 100L,
                accountId = 1L, categoryId = 1L, note = "Transaction $index"
            ),
            account = null, toAccount = null, category = null, subcategory = null
        )
    }

    @Test
    fun largeExpandedWeekOnlyComposesVisibleRowsAndKeepsExpansionWhileScrolling() {
        val transactions = transactions(500)
        val week = WeeklyAggregation(
            weekNumber = 3, startDateMillis = 1705320000000L,
            endDateMillis = 1705320000000L, label = "Week 3",
            totalIncome = 0L, totalExpense = 50000L, balance = -50000L,
            transactionCount = transactions.size, transactions = transactions
        )
        compose.setContent {
            MyApplicationTheme { WeeklyTabContent(listOf(week), "INR", {}) }
        }
        compose.onNodeWithText("Week 3").performClick()
        compose.onNodeWithText("Transaction 0").assertIsDisplayed()
        compose.onNodeWithText("Transaction 499").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Transaction 499"))
        compose.onNodeWithText("Transaction 499").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        compose.onNodeWithText("Transaction 0").assertIsDisplayed()
    }

    @Test
    fun verticalDragScrollsTransactionsWithoutNavigatingMonths() {
        val groups = FinancialEngine.groupDaily(transactions(100))
        var monthChanges = 0
        compose.setContent {
            val changes = remember { mutableIntStateOf(0) }
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().testTag("month_content").horizontalSwipeListener(
                    onSwipeLeft = { changes.intValue++; monthChanges = changes.intValue },
                    onSwipeRight = { changes.intValue++; monthChanges = changes.intValue }
                )) {
                    DailyTabContent(groups, "INR", emptySet(), {}, {}, onEmptyAddClick = {})
                }
            }
        }
        compose.onNodeWithText("Transaction 0").assertIsDisplayed()
        compose.onNodeWithTag("month_content").performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithText("Transaction 0").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, monthChanges) }
        compose.onNodeWithTag("month_content").performTouchInput { swipeLeft() }
        compose.runOnIdle { assertEquals(1, monthChanges) }
    }
}
