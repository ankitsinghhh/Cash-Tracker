package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import com.example.data.model.HapticPreferences

internal data class HapticPulse(val durationMillis: Long, val amplitude: Int)

internal class ControlledHapticFeedback(
    private val preferences: () -> HapticPreferences,
    private val systemEnabled: () -> Boolean,
    private val play: (HapticPulse) -> Unit
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val choice = preferences()
        val level = choice.strength.coerceIn(0, 100)
        if (!choice.enabled || level == 0 || !systemEnabled()) return
        val duration = if (hapticFeedbackType == HapticFeedbackType.LongPress) 14 + level * 30 / 100 else 8 + level * 18 / 100
        play(HapticPulse(duration.toLong(), (level * 255 / 100).coerceAtLeast(1)))
    }
}

/** One shared gate covers explicit keypad/PIN feedback and Compose's built-in gestures. */
@Composable
fun AppHapticProvider(preferences: HapticPreferences?, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val currentChoice by rememberUpdatedState(preferences ?: HapticPreferences(enabled = false))
    val feedback = remember(context, view) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else context.getSystemService(Vibrator::class.java)
        ControlledHapticFeedback(
            preferences = { currentChoice },
            systemEnabled = {
                @Suppress("DEPRECATION")
                view.isHapticFeedbackEnabled &&
                    Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
            },
            play = { pulse ->
                // A device without vibration hardware, or a revoked permission, must not interrupt input.
                try {
                    if (vibrator?.hasVibrator() == true) playPulse(vibrator, pulse)
                } catch (_: SecurityException) { }
            }
        )
    }
    CompositionLocalProvider(LocalHapticFeedback provides feedback, content = content)
}

@Suppress("DEPRECATION")
private fun playPulse(vibrator: Vibrator, pulse: HapticPulse) {
    if (Build.VERSION.SDK_INT >= 26) {
        val amplitude = if (vibrator.hasAmplitudeControl()) pulse.amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        val effect = VibrationEffect.createOneShot(pulse.durationMillis, amplitude)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build())
        } else {
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        }
    } else {
        vibrator.vibrate(pulse.durationMillis, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
    }
}
