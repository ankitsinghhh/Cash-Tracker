package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    private val defaultDecimalFormat = ThreadLocal.withInitial {
        DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    }

    fun getCurrencySymbol(currencyCode: String): String {
        return when (currencyCode.uppercase()) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "CAD" -> "CA$"
            "AUD" -> "A$"
            "SGD" -> "S$"
            "AED" -> "AED "
            else -> "$currencyCode "
        }
    }

    /**
     * Formats an amount in minor units (e.g. paise/cents) to a formatted string.
     * Example: 10000000L with INR and isIndianFormat=true -> "₹1,00,000.00"
     */
    fun formatAmount(
        amountMinorUnits: Long,
        currencyCode: String = "INR",
        isIndianFormat: Boolean = true,
        showSymbol: Boolean = true,
        showDecimals: Boolean = true
    ): String {
        val isNegative = amountMinorUnits < 0
        val absAmount = abs(amountMinorUnits)
        val mainUnit = absAmount / 100
        val minorUnit = absAmount % 100

        val formattedMain = if (isIndianFormat && currencyCode.equals("INR", ignoreCase = true)) {
            formatIndianNumber(mainUnit)
        } else {
            defaultDecimalFormat.get()?.format(mainUnit) ?: mainUnit.toString()
        }

        val result = StringBuilder()
        if (isNegative) {
            result.append("-")
        }
        if (showSymbol) {
            result.append(getCurrencySymbol(currencyCode))
        }
        result.append(formattedMain)
        if (showDecimals) {
            result.append(".").append(String.format(Locale.US, "%02d", minorUnit))
        }
        return result.toString()
    }

    /**
     * Formats integer according to Indian Numbering System:
     * e.g., 100000 -> "1,00,000", 10000000 -> "1,00,00,000"
     */
    fun formatIndianNumber(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s

        val lastThree = s.substring(s.length - 3)
        var remaining = s.substring(0, s.length - 3)
        val parts = mutableListOf<String>()

        while (remaining.length > 2) {
            parts.add(0, remaining.substring(remaining.length - 2))
            remaining = remaining.substring(0, remaining.length - 2)
        }
        if (remaining.isNotEmpty()) {
            parts.add(0, remaining)
        }

        return parts.joinToString(",") + "," + lastThree
    }

    /**
     * Parses a user input string (e.g. "125.50" or "125") to minor units (12550L).
     */
    fun parseToMinorUnits(input: String): Long {
        val sanitized = input.replace(",", "").trim()
        if (sanitized.isEmpty()) return 0L
        return try {
            val parts = sanitized.split(".")
            val mainPart = parts[0].toLongOrNull() ?: 0L
            val minorPart = if (parts.size > 1) {
                val frac = parts[1].take(2)
                frac.padEnd(2, '0').toLongOrNull() ?: 0L
            } else {
                0L
            }
            mainPart * 100 + minorPart
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Converts minor units to a plain decimal string for calculator or editing (e.g. "125.50").
     */
    fun toDecimalString(amountMinorUnits: Long): String {
        val main = amountMinorUnits / 100
        val minor = abs(amountMinorUnits % 100)
        return if (minor == 0L) {
            "$main"
        } else {
            String.format(Locale.US, "%d.%02d", main, minor)
        }
    }
}
