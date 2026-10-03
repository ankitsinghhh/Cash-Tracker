package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CurrencyFormatter
import java.text.DecimalFormat

private fun triggerKeypadHaptic(view: View, haptic: HapticFeedback, context: Context) {
    // 1. View haptics with flags to ensure physical responsiveness
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } else {
            view.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        }
    } catch (e: Exception) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (e2: Exception) {}
    }

    try {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    } catch (e: Exception) {}

    // 2. Strong, punchy physical tactile pulse for high-feedback mechanical keypress feel
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(42L, 255))
                } else {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(42L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(42L)
            }
        }
    } catch (e: Exception) {
        // ignore
    }
}

@Composable
fun CalculatorKeypad(
    currentExpression: String,
    onExpressionChanged: (String) -> Unit,
    onConfirmed: (Long) -> Unit,
    onClose: (() -> Unit)? = null,
    currencyCode: String = "INR",
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    var expression by remember(currentExpression) { mutableStateOf(currentExpression.ifBlank { "0" }) }

    fun appendChar(c: String) {
        if (expression == "0" && c !in listOf("+", "-", "×", "÷", ".")) {
            expression = c
        } else {
            // Prevent duplicate operator
            val lastChar = expression.lastOrNull()
            val isCurrentOp = c in listOf("+", "-", "×", "÷")
            val isLastOp = lastChar in listOf('+', '-', '×', '÷')

            if (isCurrentOp && isLastOp) {
                expression = expression.dropLast(1) + c
            } else if (c == "." && expression.takeLastWhile { it !in listOf('+', '-', '×', '÷') }.contains('.')) {
                // Prevent multiple decimals in same number token
            } else {
                expression += c
            }
        }
        onExpressionChanged(expression)
    }

    fun backspace() {
        expression = if (expression.length <= 1) "0" else expression.dropLast(1)
        onExpressionChanged(expression)
    }

    fun clear() {
        expression = "0"
        onExpressionChanged(expression)
    }

    fun evaluate(): Double {
        return try {
            val expr = expression.replace("×", "*").replace("÷", "/")
            evaluateSimpleMath(expr)
        } catch (e: Exception) {
            0.0
        }
    }

    fun calculateResult(): Long {
        val resultVal = evaluate()
        val minorUnits = (resultVal * 100).toLong()
        return if (minorUnits < 0) 0L else minorUnits
    }

    fun onEquals() {
        val res = evaluate()
        val df = DecimalFormat("#.##")
        expression = df.format(res)
        onExpressionChanged(expression)
    }

    val evaluatedValue = evaluate()
    val previewMinorUnits = (evaluatedValue * 100).toLong()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Header Bar: Amount label & Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (expression.contains(Regex("[+\\-×÷]"))) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "= ${CurrencyFormatter.formatAmount(previewMinorUnits, currencyCode)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Keypad",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4x4 Keypad Grid matching Money Manager layout:
        // Row 1: 1, 2, 3, DEL
        // Row 2: 4, 5, 6, -
        // Row 3: 7, 8, 9, +
        // Row 4: C, 0, ., Done
        val keys = listOf(
            listOf("1", "2", "3", "DEL"),
            listOf("4", "5", "6", "-"),
            listOf("7", "8", "9", "+"),
            listOf("C", "0", ".", "DONE")
        )

        for (row in keys) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (key in row) {
                    val isAction = key in listOf("C", "DEL", "-", "+", "=")
                    val isDone = key == "DONE"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isDone -> MaterialTheme.colorScheme.primary
                                    isAction -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable {
                                triggerKeypadHaptic(view, hapticFeedback, context)
                                when (key) {
                                    "C" -> clear()
                                    "DEL" -> backspace()
                                    "-" -> appendChar("-")
                                    "+" -> appendChar("+")
                                    "=" -> onEquals()
                                    "DONE" -> onConfirmed(calculateResult())
                                    else -> appendChar(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "DEL" -> {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            "DONE" -> {
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            else -> {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = if (isAction) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isAction -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Evaluates standard arithmetic expressions like "120+45*2-30/5" without external libraries.
 */
private fun evaluateSimpleMath(str: String): Double {
    val tokens = mutableListOf<String>()
    var numBuilder = StringBuilder()

    for (c in str) {
        if (c in listOf('+', '-', '*', '/')) {
            if (numBuilder.isNotEmpty()) {
                tokens.add(numBuilder.toString())
                numBuilder.clear()
            }
            tokens.add(c.toString())
        } else if (c.isDigit() || c == '.') {
            numBuilder.append(c)
        }
    }
    if (numBuilder.isNotEmpty()) {
        tokens.add(numBuilder.toString())
    }

    if (tokens.isEmpty()) return 0.0

    // Step 1: Process multiplication and division
    val pass1 = mutableListOf<String>()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        if (token == "*" || token == "/") {
            val prev = pass1.removeAt(pass1.size - 1).toDoubleOrNull() ?: 0.0
            val next = tokens.getOrNull(i + 1)?.toDoubleOrNull() ?: 0.0
            val res = if (token == "*") prev * next else if (next != 0.0) prev / next else 0.0
            pass1.add(res.toString())
            i += 2
        } else {
            pass1.add(token)
            i++
        }
    }

    // Step 2: Process addition and subtraction
    if (pass1.isEmpty()) return 0.0
    var total = pass1[0].toDoubleOrNull() ?: 0.0
    var j = 1
    while (j < pass1.size) {
        val op = pass1[j]
        val next = pass1.getOrNull(j + 1)?.toDoubleOrNull() ?: 0.0
        if (op == "+") {
            total += next
        } else if (op == "-") {
            total -= next
        }
        j += 2
    }

    return total
}
