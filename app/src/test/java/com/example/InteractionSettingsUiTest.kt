package com.example

import android.app.Application
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [35])
class InteractionSettingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun hapticSwitchDisablesSliderAndPreviewAndSliderCommitsItsValue() {
        var choice by mutableStateOf(HapticPreferences())
        var pulses = 0
        val feedback = ControlledHapticFeedback({ choice }, { true }, { pulses++ })
        compose.setContent {
            MyApplicationTheme(AppThemePalette.STUDIO, AppThemeMode.LIGHT) {
                CompositionLocalProvider(LocalHapticFeedback provides feedback) {
                    Surface { Column(Modifier.padding(16.dp)) {
                        HapticFeedbackSettings(choice, { choice = choice.copy(enabled = it) }, { choice = choice.copy(strength = it) })
                    } }
                }
            }
        }
        compose.onNodeWithContentDescription("Haptic strength").performSemanticsAction(SemanticsActions.SetProgress) { it(75f) }
        compose.runOnIdle { assertEquals(75, choice.strength) }
        compose.onNodeWithText("Test vibration").performClick()
        compose.runOnIdle { assertEquals(1, pulses) }
        compose.onNodeWithContentDescription("Haptic feedback").performClick()
        compose.onNodeWithContentDescription("Haptic strength").assertIsNotEnabled()
        compose.onNodeWithText("Test vibration").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(75, choice.strength) }
        compose.onNodeWithContentDescription("Haptic feedback").performClick()
        compose.onNodeWithText("75%").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/visuals/haptic-settings.png")
    }

    @Test fun keypadUsesOneControlledPulsePerKeyAndOffPreservesInput() {
        var choice by mutableStateOf(HapticPreferences())
        var expression by mutableStateOf("0")
        var pulses = 0
        val feedback = ControlledHapticFeedback({ choice }, { true }, { pulses++ })
        compose.setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalHapticFeedback provides feedback) {
                    CalculatorKeypad(expression, { expression = it }, {})
                }
            }
        }
        compose.onNodeWithText("7").performClick()
        compose.runOnIdle { assertEquals("7", expression); assertEquals(1, pulses); choice = choice.copy(enabled = false) }
        compose.onNodeWithText("8").performClick()
        compose.runOnIdle { assertEquals("78", expression); assertEquals(1, pulses) }
    }

    @Test fun androidProviderRespectsAppAndSystemSwitches() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vibrator = app.getSystemService(VibratorManager::class.java).defaultVibrator
        val motor = shadowOf(vibrator)
        motor.setHasVibrator(true)
        motor.setHasAmplitudeControl(true)
        @Suppress("DEPRECATION")
        Settings.System.putInt(app.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        var choice by mutableStateOf(HapticPreferences())
        var expression by mutableStateOf("0")
        compose.setContent { MyApplicationTheme { AppHapticProvider(choice) { CalculatorKeypad(expression, { expression = it }, {}) } } }
        compose.onNodeWithText("7").performClick()
        compose.runOnIdle { assertEquals(17L, motor.milliseconds); choice = HapticPreferences(enabled = false, strength = 100) }
        compose.onNodeWithText("8").performClick()
        compose.runOnIdle { assertEquals(17L, motor.milliseconds); choice = HapticPreferences(strength = 100) }
        @Suppress("DEPRECATION")
        Settings.System.putInt(app.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
        compose.onNodeWithText("9").performClick()
        compose.runOnIdle { assertEquals(17L, motor.milliseconds); assertEquals("789", expression) }
    }

    @Test fun backupControlsRequireFolderAndPreventRepeatedSavesWhileBusy() {
        var folder by mutableStateOf<CsvBackupFolder?>(null)
        var saving by mutableStateOf(false)
        var saves = 0
        compose.setContent {
            MyApplicationTheme(AppThemePalette.STUDIO, AppThemeMode.DARK) {
                Surface { CsvFolderBackupControls(folder, saving, { folder = CsvBackupFolder("content://backups/tree/root", "Ledger backups") },
                    { saves++; saving = true }) }
            }
        }
        compose.onNodeWithText("Back up CSV").assertIsNotEnabled()
        compose.onNodeWithText("Choose folder").performClick()
        compose.onNodeWithText("Folder: Ledger backups").assertIsDisplayed()
        compose.onNodeWithText("Back up CSV").performClick()
        compose.onNodeWithText("Saving…").assertIsNotEnabled()
        compose.onNodeWithText("Change folder").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, saves); saving = false }
        compose.onRoot().captureRoboImage("build/reports/visuals/csv-backup-settings.png")
    }

    @Test fun statsHasOnlyTheCardExpandButtonAndItStillOpensAndCloses() = runBlocking<Unit> {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        val store = ViewModelStore()
        var showStats by mutableStateOf(true)
        try {
            db.accountDao().insert(Account(1, "Wallet", AccountType.CASH))
            db.categoryDao().insert(Category(1, "Food", TransactionType.EXPENSE))
            db.transactionDao().insert(TransactionEntity(type = TransactionType.EXPENSE,
                dateMillis = System.currentTimeMillis(), amount = 10000, accountId = 1, categoryId = 1))
            val model = MainViewModel(app, db)
            store.put("stats", model)
            compose.setContent { if (showStats) MyApplicationTheme { FullscreenChartHost { StatsScreen(model, {}) } } }
            compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("View chart fullscreen").fetchSemanticsNodes().size == 1 }
            compose.onAllNodesWithContentDescription("View chart fullscreen").assertCountEquals(1)
            compose.onRoot().captureRoboImage("build/reports/visuals/stats-card-expand-only.png")
            compose.onNodeWithContentDescription("View chart fullscreen").performClick()
            compose.onNodeWithContentDescription("Close fullscreen chart").assertIsDisplayed().performClick()
            compose.onAllNodesWithContentDescription("View chart fullscreen").assertCountEquals(1)
        } finally {
            compose.runOnIdle { showStats = false }
            compose.waitForIdle()
            store.clear(); db.close()
        }
    }
}
