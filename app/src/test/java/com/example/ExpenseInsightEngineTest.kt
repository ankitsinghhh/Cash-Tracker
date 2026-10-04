package com.example

import com.example.data.model.*
import com.example.domain.ExpenseInsightEngine
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExpenseInsightEngineTest {
    private val zone = TimeZone.getTimeZone("Asia/Kolkata")
    private fun date(year: Int, month: Int, day: Int, timezone: TimeZone = zone) = Calendar.getInstance(timezone).apply {
        clear(); set(year, month - 1, day, 12, 0)
    }
    private fun expense(id: Long, month: Int, day: Int, amount: Long, currency: String = "INR", excluded: Boolean = false,
                        type: TransactionType = TransactionType.EXPENSE, fee: Long = 0,
                        recurring: Long? = null, installment: Long? = null, payee: String = "Shop", year: Int = 2026,
                        category: Long = 1) = TransactionWithDetails(
        TransactionEntity(id, type, date(year, month, day).timeInMillis, amount, 1, transferFee = fee, categoryId = category,
            payee = payee, isExcludedFromStats = excluded, recurringRuleId = recurring, installmentId = installment),
        Account(1, "Cash", AccountType.CASH, currency = currency), null, Category(category, "Category $category", TransactionType.EXPENSE), null
    )
    @Test fun excludedTransfersAndMixedCurrenciesDoNotDistortInsights() {
        val data = ExpenseInsightEngine.build(listOf(
            expense(1, 10, 1, 150), expense(2, 10, 2, 1000, excluded = true), expense(3, 10, 1, 400, currency = "USD"),
            expense(4, 10, 1, 50000, type = TransactionType.TRANSFER, fee = 20),
            expense(5, 10, 1, 9999, type = TransactionType.INCOME)
        ), date(2026, 10, 1), "INR", 200, date(2026, 10, 4).timeInMillis)
        assertEquals(170L, data.total)
        assertEquals(1, data.omittedCurrencyRecords)
        assertEquals(150L, data.smallPurchases.total)
        assertEquals(1, data.smallPurchases.allCount)
        assertEquals(1, data.months.last().count)
        assertEquals(150L, data.months.last().average)
        assertEquals(20L, data.drivers.first { it.category.name == "Transfer fees" }.current)
    }
    @Test fun partialMonthUsesMatchingDaysAndExcludesFutureEntries() {
        val data = ExpenseInsightEngine.build(listOf(
            expense(1, 9, 4, 100), expense(2, 9, 5, 999), expense(3, 10, 4, 150), expense(4, 10, 5, 999)
        ), date(2026, 10, 1), "INR", 200, date(2026, 10, 4).timeInMillis)
        assertEquals(4, data.comparedDays)
        assertEquals(100L, data.previousTotal)
        assertEquals(150L, data.total)
        assertEquals(50L, data.drivers.sumOf { it.delta })
        assertEquals(0L, data.days[4].amount)
    }
    @Test fun completedUnequalLengthMonthsCompareFullMonths() {
        val data = ExpenseInsightEngine.build(listOf(expense(1, 1, 31, 100), expense(2, 2, 28, 150)),
            date(2026, 2, 1), "INR", 200, date(2026, 4, 1).timeInMillis)
        assertEquals(28, data.comparedDays)
        assertEquals(100L, data.previousTotal)
        assertEquals(150L, data.total)
    }
    @Test fun scheduledInstallmentsAreCountedOnceAndMerchantNamesAreCaseInsensitive() {
        val data = ExpenseInsightEngine.build(listOf(
            expense(1, 10, 1, 100, recurring = 1), expense(2, 10, 2, 200, recurring = 1, installment = 1, payee = " shop "),
            expense(3, 10, 3, 500), expense(4, 10, 4, 900, excluded = true)
        ), date(2026, 10, 1), "INR", 200, date(2026, 10, 4).timeInMillis)
        assertEquals(100L, data.months.last().recurring)
        assertEquals(200L, data.months.last().installments)
        assertEquals(300L, data.smallPurchases.total)
        assertEquals(3, data.merchants.single().count)
        assertEquals(800L, data.merchants.single().total)
        assertEquals(3, data.digest.daysWithSpending)
    }
    @Test fun digestCrossesMonthBoundaryAndExplainsRecordedDrivers() {
        val data = ExpenseInsightEngine.build(listOf(
            expense(1, 9, 20, 10), expense(2, 9, 27, 20), expense(3, 9, 30, 30), expense(4, 10, 4, 100, category = 2)
        ), date(2026, 10, 1), "INR", 200, date(2026, 10, 4).timeInMillis)
        assertEquals(130L, data.digest.expense)
        assertEquals(20L, data.digest.previousExpense)
        assertEquals("Category 2", data.digest.topCategory!!.category.name)
        assertEquals(4L, data.digest.largest!!.id)
    }
    @Test fun spikesNeedEnoughActiveDaysAndKeepLargestContributors() {
        val records = listOf(expense(1, 10, 1, 10), expense(2, 10, 2, 10)) + (3L..12L).map { expense(it, 10, 3, it * 100) }
        val data = ExpenseInsightEngine.build(records, date(2026, 10, 1), "INR", 200, date(2026, 10, 4).timeInMillis)
        assertEquals(3, data.spikes.single().day)
        assertEquals(5, data.spikes.single().contributors.size)
        assertEquals(12L, data.spikes.single().contributors.first().id)
        val tooFew = ExpenseInsightEngine.build(records.filter { it.transaction.id != 2L }, date(2026, 10, 1), "INR", 200, date(2026, 10, 4).timeInMillis)
        assertTrue(tooFew.spikes.isEmpty())
    }
    @Test fun futureMonthHasNoFabricatedRecapOrSpending() {
        val data = ExpenseInsightEngine.build(listOf(expense(1, 11, 1, 100)), date(2026, 11, 1), "INR", 200, date(2026, 10, 4).timeInMillis)
        assertEquals(0, data.comparedDays)
        assertEquals(0L, data.total)
        assertEquals(0L, data.digest.expense)
    }
    @Test fun calendarDaysSurviveDaylightSavingTransition() {
        val newYork = TimeZone.getTimeZone("America/New_York")
        val record = expense(1, 3, 8, 100).let { it.copy(transaction = it.transaction.copy(dateMillis = date(2026, 3, 8, newYork).timeInMillis)) }
        val data = ExpenseInsightEngine.build(listOf(record), date(2026, 3, 1, newYork), "INR", 200, date(2026, 3, 10, newYork).timeInMillis)
        assertEquals(31, data.days.size)
        assertEquals(31, data.days.map { it.dateMillis }.distinct().size)
        assertEquals(100L, data.days[7].amount)
        assertEquals(10, data.comparedDays)
    }
    @Test fun largeLedgerProducesBoundedChartAndDrilldownData() {
        val sample = expense(1, 10, 1, 100)
        val records = List(50000) { index -> sample.copy(transaction = sample.transaction.copy(id = index.toLong() + 1)) }
        val data = ExpenseInsightEngine.build(records, date(2026, 10, 1), "INR", 200, date(2026, 10, 4).timeInMillis)
        assertEquals(5000000L, data.total)
        assertEquals(50000, data.smallPurchases.count)
        assertEquals(50000, data.months.last().count)
        assertEquals(31, data.days.size)
        assertEquals(5, data.days.first().contributors.size)
        assertEquals(1, data.merchants.size)
    }
}
