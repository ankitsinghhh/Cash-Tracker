package com.example

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.InsightWidget
import com.example.ui.MainViewModel
import com.example.ui.theme.AppThemePalette
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InsightPreferencesTest {
    @Test fun bothSummaryStatesAndQuietStudioSurviveAppRestart() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        val temporaryDirectory = Files.createTempDirectory("cash-summary-")
        val name = temporaryDirectory.resolve("settings.db").toString()
        var db = Room.databaseBuilder(app, AppDatabase::class.java, name).build()
        var store = ViewModelStore()
        try {
            var model = MainViewModel(app, db)
            store.put("test", model)
            assertTrue(withTimeout(10000) { model.homeSummaryExpanded.first { it != null } }!!)
            model.setThemePalette(AppThemePalette.STUDIO)
            withTimeout(10000) { model.themePalette.first { it == AppThemePalette.STUDIO } }
            for (expanded in listOf(false, true, false)) {
                model.setHomeSummaryExpanded(expanded).join()
                withTimeout(10000) { model.homeSummaryExpanded.first { it == expanded } }
                store.clear(); db.close()
                db = Room.databaseBuilder(app, AppDatabase::class.java, name).build()
                store = ViewModelStore()
                model = MainViewModel(app, db)
                store.put("test", model)
                assertEquals(expanded, withTimeout(10000) { model.homeSummaryExpanded.first { it != null } })
                assertEquals(AppThemePalette.STUDIO,
                    withTimeout(10000) { model.themePalette.first { it == AppThemePalette.STUDIO } })
            }
        } finally {
            store.clear(); db.close(); app.deleteDatabase(name)
            temporaryDirectory.toFile().delete(); Dispatchers.resetMain()
        }
    }

    @Test fun chartChoicesHeatmapThresholdAndExistingThemeSurviveReopeningDatabase() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        // Native SQLite on Windows needs a short path, independent of Robolectric's long test name.
        val temporaryDirectory = Files.createTempDirectory("cash-prefs-")
        val name = temporaryDirectory.resolve("settings.db").toString()
        var db = Room.databaseBuilder(app, AppDatabase::class.java, name).build()
        var store = ViewModelStore()
        try {
            var model = MainViewModel(app, db)
            store.put("test", model)
            model.setChartVisible(InsightWidget.HEATMAP, false).join()
            model.setChartVisible(InsightWidget.CATEGORY_DONUT, false).join()
            model.setCalendarHeatmap(true).join()
            model.setSmallPurchaseThreshold(12345).join()
            model.setThemePalette(AppThemePalette.SAPPHIRE)
            withTimeout(10000) { model.themePalette.first { it == AppThemePalette.SAPPHIRE } }
            store.clear(); db.close()
            db = Room.databaseBuilder(app, AppDatabase::class.java, name).build()
            store = ViewModelStore()
            model = MainViewModel(app, db)
            store.put("test", model)
            val charts = withTimeout(10000) { model.visibleCharts.first { InsightWidget.HEATMAP !in it && InsightWidget.CATEGORY_DONUT !in it } }
            assertTrue(InsightWidget.WEEKLY_DIGEST in charts)
            assertTrue(withTimeout(10000) { model.calendarHeatmap.first { it } })
            assertEquals(12345L, withTimeout(10000) { model.smallPurchaseThreshold.first { it == 12345L } })
            assertEquals(AppThemePalette.SAPPHIRE, withTimeout(10000) { model.themePalette.first { it == AppThemePalette.SAPPHIRE } })
            model.setAllChartsVisible(false).join()
            withTimeout(10000) { model.visibleCharts.first { it.isEmpty() } }
            model.setAllChartsVisible(true).join()
            assertEquals(InsightWidget.entries.toSet(), withTimeout(10000) { model.visibleCharts.first { it.size == InsightWidget.entries.size } })
            // Hiding/showing Stats charts does not alter the independent Home overlay preference.
            assertTrue(withTimeout(10000) { model.calendarHeatmap.first { it } })
        } finally { store.clear(); db.close(); app.deleteDatabase(name); temporaryDirectory.toFile().delete(); Dispatchers.resetMain() }
    }
}
