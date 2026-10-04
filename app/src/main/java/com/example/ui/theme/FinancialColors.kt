package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class FinancialColorSet(val income: Color, val expense: Color, val transfer: Color,
    val accent: Color = GeoPrimary, val warning: Color = WarningAmber, val refined: Boolean = false)
val LocalFinancialColors = staticCompositionLocalOf { FinancialColorSet(IncomeGreen, ExpenseRed, TransferTeal) }
object FinancialColors {
    val income: Color @Composable get() = LocalFinancialColors.current.income
    val expense: Color @Composable get() = LocalFinancialColors.current.expense
    val transfer: Color @Composable get() = LocalFinancialColors.current.transfer
    val accent: Color @Composable get() = LocalFinancialColors.current.accent
    val warning: Color @Composable get() = LocalFinancialColors.current.warning
    val refined: Boolean @Composable get() = LocalFinancialColors.current.refined
}
