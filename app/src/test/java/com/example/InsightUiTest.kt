package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.domain.ExpenseInsightEngine
import com.example.ui.components.*
import com.example.ui.screens.stats.expenseInsightItems
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [35])
class InsightUiTest {
    @get:Rule val compose = createComposeRule()
    private fun date(year: Int, month: Int, day: Int) = Calendar.getInstance().apply { clear(); set(year, month - 1, day, 12, 0) }
    private val account = Account(1, "Everyday wallet", AccountType.CASH)
    private val category = Category(1, "Food & drinks", TransactionType.EXPENSE, "restaurant", "#24675F")
    private fun record(id: Long, year: Int = 2026, month: Int = 10, day: Int = 1, amount: Long = 125050) = TransactionWithDetails(
        TransactionEntity(id, TransactionType.EXPENSE, date(year, month, day).timeInMillis, amount, 1, categoryId = 1,
            payee = "Neighbourhood coffee", note = "Lunch and groceries"), account, null, category, null
    )
    private fun data() = ExpenseInsightEngine.build(listOf(record(1), record(2, day = 2, amount = 45000), record(3, day = 3, amount = 999000),
        record(4, month = 9), record(5, year = 2025, month = 10)), date(2026, 10, 1), "INR", 20000, date(2026, 10, 4).timeInMillis)

    private fun home(mode: AppThemeMode) {
        compose.setContent {
            MyApplicationTheme(AppThemePalette.STUDIO, mode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("October 2026", style = MaterialTheme.typography.titleLarge)
                        PeriodTotalsBar(1000000, 325050, 674950)
                        Card { TransactionItemRow(record(1), onClick = {}) }
                        EmptyStateView("Your records live here", "Add your first expense to start seeing your spending pattern.")
                    }
                }
            }
        }
        compose.onNodeWithText("Income").assertIsDisplayed()
        compose.onNodeWithText("Expenses").assertIsDisplayed()
        compose.onNodeWithText("Total").assertIsDisplayed()
        compose.onNodeWithText("Monthly spending").assertDoesNotExist()
        compose.onNodeWithText("Lunch and groceries").assertIsDisplayed()
        compose.onNodeWithText("Everyday wallet · Neighbourhood coffee").assertIsDisplayed()
        compose.onNodeWithText("Food & drinks").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/visuals/studio-home-${mode.name.lowercase()}.png")
    }
    @Test fun studioLightSummaryAndRowsRemainReadable() = home(AppThemeMode.LIGHT)
    @Test fun studioDarkSummaryAndRowsRemainReadable() = home(AppThemeMode.DARK)

    @Test fun periodTotalsCanBeRepeatedlyCollapsedAndExpanded() {
        compose.setContent {
            MyApplicationTheme {
                var expanded by remember { mutableStateOf(true) }
                Column {
                    PeriodTotalsBar(1000000, 325050, 674950,
                        expanded = expanded, onExpandedChange = { expanded = it })
                    TransactionItemRow(record(1), onClick = {})
                }
            }
        }
        repeat(5) {
            compose.onNodeWithContentDescription("Collapse period totals").performClick()
            compose.onNodeWithText("Income").assertDoesNotExist()
            compose.onNodeWithText("Expenses").assertDoesNotExist()
            compose.onNodeWithText("Total").assertDoesNotExist()
            compose.onNodeWithText("Period totals").assertIsDisplayed()
            compose.onNodeWithText("Lunch and groceries").assertIsDisplayed()
            compose.onNodeWithContentDescription("Expand period totals").performClick()
            compose.onNodeWithText("Income").assertIsDisplayed()
            compose.onNodeWithText("Expenses").assertIsDisplayed()
            compose.onNodeWithText("Total").assertIsDisplayed()
            compose.onNodeWithText("Period totals").assertDoesNotExist()
        }
        compose.onNodeWithContentDescription("Collapse period totals").performClick()
        compose.onRoot().captureRoboImage("build/reports/visuals/compact-home-collapsed.png")
    }

    @Test fun heatmapIsInteractiveAndTransactionLinksWork() {
        var navigated = 0L
        compose.setContent {
            MyApplicationTheme(AppThemePalette.STUDIO, AppThemeMode.LIGHT) {
                LazyColumn(contentPadding = PaddingValues(16.dp)) { expenseInsightItems(data(), setOf(InsightWidget.HEATMAP), {}, { navigated = it }) }
            }
        }
        compose.onNodeWithContentDescription("Day 3, ₹9,990.00").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Day 3 · ₹9,990.00 · 1 spending records"))
        compose.onNodeWithText("Neighbourhood coffee").performClick()
        compose.runOnIdle { assertEquals(3L, navigated) }
        compose.onRoot().captureRoboImage("build/reports/visuals/studio-heatmap.png")
    }

    @Test fun allInsightsScrollToDigestAndDisabledInsightsAreAbsent() {
        compose.setContent {
            MyApplicationTheme(AppThemePalette.STUDIO, AppThemeMode.DARK) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        expenseInsightItems(data(), InsightWidget.entries.toSet() - InsightWidget.SEASONALITY, {}, {})
                    }
                }
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Weekly digest"))
        compose.onNodeWithText("Weekly digest").assertIsDisplayed()
        compose.onNodeWithText("Annual seasonality").assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/reports/visuals/studio-weekly-digest.png")
    }
}
