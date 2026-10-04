package com.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.data.model.*
import com.example.domain.ExpenseInsightEngine
import com.example.ui.screens.stats.expenseInsightItems
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
class FullscreenChartTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Composable private fun SelectedChart(onEnter: () -> Unit = {}, onDispose: () -> Unit = {}) {
        var selected by rememberSaveable { mutableIntStateOf(1) }
        DisposableEffect(Unit) { onEnter(); onDispose { onDispose() } }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Selected day $selected")
            Button(onClick = { selected++ }) { Text("Next day") }
            Box(Modifier.fillMaxWidth().height(chartPlotHeight(160.dp)).testTag("plot"))
        }
    }

    @Test fun repeatedOpenCloseKeepsOneChartAndTheOriginalListPosition() {
        val list = LazyListState()
        var entered = 0
        var disposed = 0
        compose.setContent {
            MyApplicationTheme {
                FullscreenChartHost {
                    LazyColumn(state = list, modifier = Modifier.testTag("stats_list")) {
                        items(6) { Text("Earlier item $it", Modifier.height(100.dp)) }
                        item("chart") { FullscreenableChart("sample", "Sample chart") { SelectedChart({ entered++ }, { disposed++ }) } }
                        items(20) { Text("Later item $it", Modifier.height(100.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("stats_list").performScrollToIndex(6)
        compose.onNodeWithText("Next day").performClick()
        val initialIndex = list.firstVisibleItemIndex
        val initialOffset = list.firstVisibleItemScrollOffset
        val initialEnterCount = entered
        val initialDisposeCount = disposed
        repeat(3) { iteration ->
            compose.onNodeWithTag("expand_sample").performClick()
            compose.onNodeWithTag("fullscreen_chart").assertIsDisplayed()
            compose.onAllNodesWithText("Selected day ${iteration + 2}").assertCountEquals(1)
            compose.onNodeWithText("Next day").performClick()
            compose.onNodeWithTag("close_fullscreen_chart").performClick()
            compose.onNodeWithTag("fullscreen_chart").assertDoesNotExist()
            compose.onNodeWithText("Selected day ${iteration + 3}").assertIsDisplayed()
            compose.runOnIdle {
                assertEquals(initialIndex, list.firstVisibleItemIndex)
                assertEquals(initialOffset, list.firstVisibleItemScrollOffset)
                assertEquals(initialEnterCount, entered)
                assertEquals(initialDisposeCount, disposed)
            }
        }
    }

    @Test fun backClosesFullscreenWithoutLosingSelection() {
        compose.setContent { MyApplicationTheme { FullscreenChartHost { FullscreenableChart("back", "Chart") { SelectedChart() } } } }
        compose.onNodeWithTag("expand_back").performClick()
        compose.onNodeWithText("Next day").performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("fullscreen_chart").assertDoesNotExist()
        compose.onNodeWithText("Selected day 2").assertIsDisplayed()
    }

    @Test fun fullscreenAndSelectedPointSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MyApplicationTheme { FullscreenChartHost { FullscreenableChart("restore", "Chart") { SelectedChart() } } } }
        compose.onNodeWithText("Next day").performClick()
        compose.onNodeWithTag("expand_restore").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("fullscreen_chart").assertIsDisplayed()
        compose.onNodeWithText("Selected day 2").assertIsDisplayed()
        compose.onNodeWithTag("close_fullscreen_chart").performClick()
        compose.onNodeWithText("Selected day 2").assertIsDisplayed()
    }

    @Test fun removingAnOpenChartClosesItsOverlayInsteadOfLeavingStaleContent() {
        val show = mutableStateOf(true)
        compose.setContent { MyApplicationTheme { FullscreenChartHost {
            if (show.value) FullscreenableChart("removed", "Chart") { SelectedChart() }
        } } }
        compose.onNodeWithTag("expand_removed").performClick()
        compose.runOnIdle { show.value = false }
        compose.onNodeWithTag("fullscreen_chart").assertDoesNotExist()
        compose.onNodeWithText("Selected day 1").assertDoesNotExist()
    }

    @Test fun wholeSectionExpansionPreservesNestedChartAndListState() {
        val list = LazyListState()
        compose.setContent { MyApplicationTheme { FullscreenChartHost {
            Column {
                ChartFullscreenButton("section")
                FullscreenableChart("section", "Insights", Modifier.fillMaxSize(), scrollInFullscreen = false, showButton = false) {
                    LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("nested_list")) {
                        items(4) { Text("Earlier item $it", Modifier.height(150.dp)) }
                        item("nested") { FullscreenableChart("nested", "Nested chart") { SelectedChart() } }
                        items(20) { Text("Later item $it", Modifier.height(100.dp)) }
                    }
                }
            }
        } } }
        compose.onNodeWithTag("nested_list").performScrollToIndex(4)
        compose.onNodeWithText("Next day").performClick()
        val originalOffset = list.firstVisibleItemScrollOffset
        compose.onNodeWithTag("expand_section").performClick()
        compose.onNodeWithText("Selected day 2").assertIsDisplayed()
        compose.onNodeWithTag("expand_nested").assertDoesNotExist()
        compose.onNodeWithTag("close_fullscreen_chart").performClick()
        compose.runOnIdle {
            assertEquals(4, list.firstVisibleItemIndex)
            assertEquals(originalOffset, list.firstVisibleItemScrollOffset)
        }
        compose.onNodeWithTag("expand_nested").performClick()
        compose.onNodeWithText("Selected day 2").assertIsDisplayed()
        compose.onNodeWithTag("close_fullscreen_chart").performClick()
    }

    @Test fun realHeatmapKeepsSelectedDayAndTransactionDrilldownInFullscreen() {
        fun date(day: Int) = Calendar.getInstance().apply { clear(); set(2026, 9, day, 12, 0) }
        val account = Account(1, "Wallet", AccountType.CASH)
        val category = Category(1, "Groceries", TransactionType.EXPENSE, "restaurant", "#24675F")
        val record = TransactionWithDetails(TransactionEntity(1, TransactionType.EXPENSE, date(3).timeInMillis,
            123450, 1, categoryId = 1, payee = "Local groceries"), account, null, category, null)
        val data = ExpenseInsightEngine.build(listOf(record), date(1), "INR", 20000, date(4).timeInMillis)
        var navigated = 0L
        compose.setContent { MyApplicationTheme(AppThemePalette.STUDIO, AppThemeMode.LIGHT) { FullscreenChartHost {
            LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.testTag("heatmap_list")) {
                expenseInsightItems(data, setOf(InsightWidget.HEATMAP), {}, { navigated = it })
            }
        } } }
        compose.onNodeWithTag("heatmap_list").performScrollToNode(hasText("Spending heatmap"))
        compose.onNodeWithContentDescription("Day 3, ₹1,234.50").performClick()
        compose.onNodeWithContentDescription("View chart fullscreen").performClick()
        compose.onNodeWithText("Day 3 · ₹1,234.50 · 1 spending records").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/visuals/fullscreen-heatmap.png")
        compose.onNodeWithText("Local groceries").performClick()
        compose.runOnIdle { assertEquals(1L, navigated) }
        compose.onNodeWithTag("close_fullscreen_chart").performClick()
        compose.onNodeWithTag("heatmap_list").performScrollToNode(hasText("Day 3 · ₹1,234.50 · 1 spending records"))
        compose.onNodeWithText("Day 3 · ₹1,234.50 · 1 spending records").assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w891dp-h411dp-land", sdk = [35])
    fun realDonutRemainsInteractiveInLandscapeFullscreen() {
        val categories = listOf(
            CategorySpending(Category(1, "Groceries", TransactionType.EXPENSE, "restaurant", "#24675F"), 750000, 5, 75f),
            CategorySpending(Category(2, "Transport", TransactionType.EXPENSE, "directions_car", "#82644D"), 250000, 3, 25f)
        )
        compose.setContent { MyApplicationTheme(AppThemePalette.STUDIO, AppThemeMode.DARK) { FullscreenChartHost {
            FullscreenableChart("donut", "Expense categories · October 2026") { DonutPieChart(categories, 1000000) }
        } } }
        compose.onNodeWithTag("expand_donut").performClick()
        compose.onNodeWithText("Transport").performScrollTo().assertIsDisplayed().performClick()
        compose.onAllNodesWithText("Transport").assertCountEquals(2)
        compose.onRoot().captureRoboImage("build/reports/visuals/fullscreen-donut-landscape.png")
        compose.onNodeWithTag("close_fullscreen_chart").assertIsDisplayed().performClick()
        compose.onAllNodesWithText("Transport").assertCountEquals(2)
    }

    @Test @Config(qualifiers = "w891dp-h411dp-land", sdk = [35])
    fun landscapeHasAnAccessibleCloseControlAndAnEnlargedPlot() {
        compose.setContent { MyApplicationTheme(AppThemePalette.STUDIO, AppThemeMode.DARK) {
            FullscreenChartHost { FullscreenableChart("landscape", "Spending pattern") { SelectedChart() } }
        } }
        val compactBounds = compose.onNodeWithTag("plot").getUnclippedBoundsInRoot()
        val compactHeight = compactBounds.bottom - compactBounds.top
        compose.onNodeWithTag("expand_landscape").performClick()
        compose.onNodeWithTag("close_fullscreen_chart").assertIsDisplayed()
        val fullscreenBounds = compose.onNodeWithTag("plot").getUnclippedBoundsInRoot()
        assertTrue(fullscreenBounds.bottom - fullscreenBounds.top > compactHeight)
        compose.onRoot().captureRoboImage("build/reports/visuals/fullscreen-landscape.png")
        compose.onNodeWithTag("close_fullscreen_chart").performClick()
        compose.onNodeWithText("Selected day 1").assertIsDisplayed()
    }
}
