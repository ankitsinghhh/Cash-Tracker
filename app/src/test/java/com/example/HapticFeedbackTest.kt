package com.example

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.data.model.HapticPreferences
import com.example.ui.components.ControlledHapticFeedback
import com.example.ui.components.HapticPulse
import org.junit.Assert.*
import org.junit.Test

class HapticFeedbackTest {
    @Test fun offAndZeroStrengthNeverPlayAPulse() {
        var choice = HapticPreferences(enabled = false, strength = 100)
        val pulses = mutableListOf<HapticPulse>()
        val feedback = ControlledHapticFeedback({ choice }, { error("Disabled feedback must not read system settings") }, pulses::add)
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        choice = HapticPreferences(strength = 0)
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        assertTrue(pulses.isEmpty())
    }

    @Test fun systemOffSuppressesAppFeedback() {
        val pulses = mutableListOf<HapticPulse>()
        ControlledHapticFeedback({ HapticPreferences() }, { false }, pulses::add)
            .performHapticFeedback(HapticFeedbackType.LongPress)
        assertTrue(pulses.isEmpty())
    }

    @Test fun oneEventPlaysOnePulseAndStrengthIncreasesItsAmplitudeAndDuration() {
        var choice = HapticPreferences(strength = 20)
        val pulses = mutableListOf<HapticPulse>()
        val feedback = ControlledHapticFeedback({ choice }, { true }, pulses::add)
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        choice = HapticPreferences(strength = 80)
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        assertEquals(2, pulses.size)
        assertTrue(pulses[1].amplitude > pulses[0].amplitude)
        assertTrue(pulses[1].durationMillis > pulses[0].durationMillis)
        assertTrue(pulses.all { it.amplitude in 1..255 && it.durationMillis in 1..44 })
    }
}
