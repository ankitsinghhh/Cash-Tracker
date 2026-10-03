package com.example.benchmarks

import androidx.benchmark.macro.*
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "com.aistudio.moneymanager.rxkplq"

// Run only on a dedicated test install with onboarding complete, PIN disabled,
// and a month containing the documented test dataset. Never reset a user's data.
private fun MacrobenchmarkScope.exerciseTransactions() {
    check(device.wait(Until.hasObject(By.res("add_transaction_fab")), 10000)) {
        "Complete onboarding, disable PIN and select a populated month before benchmarking"
    }
    repeat(4) { device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
        device.displayWidth / 2, device.displayHeight / 3, 20) }
    checkNotNull(device.findObject(By.text("Weekly"))).click()
    checkNotNull(device.wait(Until.findObject(By.textStartsWith("Week ")), 5000)) { "Select a month containing transactions" }.click()
    repeat(3) { device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
        device.displayWidth / 2, device.displayHeight / 3, 20) }
    repeat(4) { device.swipe(device.displayWidth / 2, device.displayHeight / 3,
        device.displayWidth / 2, device.displayHeight * 3 / 4, 20) }
    checkNotNull(device.findObject(By.textStartsWith("Week "))).click()
    checkNotNull(device.findObject(By.res("nav_tab_stats"))).click()
    checkNotNull(device.wait(Until.findObject(By.text("Daily Trend")), 5000)).click()
    checkNotNull(device.findObject(By.res("nav_tab_home"))).click()
    checkNotNull(device.wait(Until.findObject(By.res("search_button")), 5000)).click()
    checkNotNull(device.wait(Until.findObject(By.res("search_input")), 5000)).text = "Food"
    device.waitForIdle()
    device.pressBack()
    if (!device.wait(Until.hasObject(By.res("add_transaction_fab")), 1000)) {
        device.pressBack()
        check(device.wait(Until.hasObject(By.res("add_transaction_fab")), 5000))
    }
    checkNotNull(device.findObject(By.text("Daily"))).click()
}

@RunWith(AndroidJUnit4::class)
class CashTrackerBenchmarks {
    @get:Rule val benchmark = MacrobenchmarkRule()

    @Test fun coldLaunch() = benchmark.measureRepeated(PACKAGE,
        metrics = listOf(StartupTimingMetric()), iterations = 5,
        startupMode = StartupMode.COLD, compilationMode = CompilationMode.Partial()) {
        pressHome(); startActivityAndWait()
    }

    @Test fun scrollExpandAndNavigate() = benchmark.measureRepeated(PACKAGE,
        metrics = listOf(FrameTimingMetric()), iterations = 5,
        compilationMode = CompilationMode.Partial(), setupBlock = { startActivityAndWait() }) {
        exerciseTransactions()
    }
}

@RunWith(AndroidJUnit4::class)
class CashTrackerBaselineProfile {
    @get:Rule val profile = BaselineProfileRule()
    @Test fun generate() = profile.collect(PACKAGE) {
        pressHome(); startActivityAndWait(); exerciseTransactions()
    }
}
