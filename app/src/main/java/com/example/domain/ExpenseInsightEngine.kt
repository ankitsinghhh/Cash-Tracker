package com.example.domain

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Executed on Dispatchers.Default by MainViewModel. All money remains in minor units. */
object ExpenseInsightEngine {
    private data class Entry(val details: TransactionWithDetails, val amount: Long, val date: Long, val month: String,
        val category: InsightCategory, val merchant: String) {
        fun contribution() = ExpenseContribution(details.transaction.id,
            details.transaction.payee.ifBlank { details.transaction.note.ifBlank { category.name } }, amount)
    }

    fun build(records: List<TransactionWithDetails>, selectedMonth: Calendar, currency: String,
              smallThreshold: Long, nowMillis: Long = System.currentTimeMillis()): ExpenseInsights {
        val zone = selectedMonth.timeZone
        fun calendar(time: Long) = Calendar.getInstance(zone).apply { timeInMillis = time }
        fun midnight(time: Long) = calendar(time).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val month = midnight(selectedMonth.timeInMillis).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val next = (month.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val todayEnd = midnight(nowMillis).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        val cutoff = minOf(next.timeInMillis, todayEnd)
        val keyFormat = SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = zone }
        val labelFormat = SimpleDateFormat("MMM yy", Locale.getDefault()).apply { timeZone = zone }
        val selectedKey = keyFormat.format(month.time)
        val previous = (month.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val comparedDays = if (cutoff <= month.timeInMillis) 0 else if (cutoff == next.timeInMillis) month.getActualMaximum(Calendar.DAY_OF_MONTH) else calendar(cutoff - 1).get(Calendar.DAY_OF_MONTH)
        // For a completed month compare full months. For a partial month compare equal elapsed days.
        val previousCutoff = if (cutoff == next.timeInMillis) month.timeInMillis else (previous.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, minOf(comparedDays, getActualMaximum(Calendar.DAY_OF_MONTH)))
        }.timeInMillis
        var omitted = 0
        val entries = ArrayList<Entry>()
        val recordMonths = HashSet<String>()
        for (item in records) {
            val tx = item.transaction
            if (tx.isExcludedFromStats || tx.dateMillis >= cutoff) continue
            if (item.account?.currency != currency) { omitted++; continue }
            val key = keyFormat.format(tx.dateMillis)
            recordMonths.add(key)
            val amount = when (tx.type) {
                TransactionType.EXPENSE -> tx.amount
                TransactionType.TRANSFER -> tx.transferFee
                else -> 0L
            }
            if (amount <= 0L) continue
            val category = if (tx.type == TransactionType.TRANSFER) InsightCategory(Long.MIN_VALUE, "Transfer fees")
                else InsightCategory(item.category?.id ?: 0L, item.category?.name ?: "Uncategorised")
            entries.add(Entry(item, amount, midnight(tx.dateMillis).timeInMillis, key, category,
                tx.payee.trim()))
        }
        val selected = entries.filter { it.month == selectedKey }
        val previousEntries = entries.filter { it.date >= previous.timeInMillis && it.date < previousCutoff }
        fun totals(list: List<Entry>) = list.groupingBy { it.category.id }.fold(0L) { acc, entry -> acc + entry.amount }
        val currentTotals = totals(selected)
        val previousTotals = totals(previousEntries)
        val categories = entries.map { it.category }.distinctBy { it.id }
        val categoryMap = categories.associateBy { it.id }
        val drivers = (currentTotals.keys + previousTotals.keys).map { id ->
            SpendingDriver(categoryMap.getValue(id), currentTotals[id] ?: 0L, previousTotals[id] ?: 0L)
        }.sortedByDescending { kotlin.math.abs(it.delta) }
        val byMonth = entries.groupBy { it.month }
        val historyStart = (month.clone() as Calendar).apply { add(Calendar.YEAR, -1); set(Calendar.MONTH, Calendar.JANUARY) }
        val months = buildList {
            val cursor = historyStart.clone() as Calendar
            while (cursor.timeInMillis < next.timeInMillis) {
                val key = keyFormat.format(cursor.time)
                val values = byMonth[key].orEmpty()
                add(InsightMonth(key, labelFormat.format(cursor.time), cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH),
                    values.sumOf { it.amount }, values.count { it.details.transaction.type == TransactionType.EXPENSE },
                    values.filter { it.details.transaction.type == TransactionType.EXPENSE }.sumOf { it.amount },
                    values.filter { it.details.transaction.recurringRuleId != null && it.details.transaction.installmentId == null }.sumOf { it.amount },
                    values.filter { it.details.transaction.installmentId != null }.sumOf { it.amount }, totals(values), key in recordMonths))
                cursor.add(Calendar.MONTH, 1)
            }
        }
        val byDay = selected.groupBy { it.date }
        val days = buildList {
            val cursor = month.clone() as Calendar
            while (cursor.timeInMillis < next.timeInMillis) {
                val values = byDay[cursor.timeInMillis].orEmpty()
                add(ExpenseDay(cursor.get(Calendar.DAY_OF_MONTH), cursor.timeInMillis, values.sumOf { it.amount }, values.size,
                    values.sortedByDescending { it.amount }.take(5).map { it.contribution() }))
                cursor.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        // Compare a day with the mean of active spending days; at least three are needed.
        val activeDays = days.filter { it.amount > 0 }
        val mean = if (activeDays.isEmpty()) 0.0 else activeDays.sumOf { it.amount }.toDouble() / activeDays.size
        val spikes = if (activeDays.size < 3) emptyList() else activeDays.filter { it.amount > mean * 1.75 }.sortedByDescending { it.amount }.take(5)
        val merchants = selected.filter { it.merchant.isNotBlank() && it.details.transaction.type == TransactionType.EXPENSE }
            .groupBy { it.merchant.lowercase(Locale.ROOT) }.values.map { values ->
                HabitMerchant(values.first().merchant, values.sumOf { it.amount }, values.size)
            }.sortedByDescending { it.total }
        val purchases = selected.filter { it.details.transaction.type == TransactionType.EXPENSE }
        val small = purchases.filter { it.amount <= smallThreshold }
        val weekEnd = midnight(maxOf(month.timeInMillis, cutoff - 1)).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val weekStart = (weekEnd.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -7) }
        val previousWeekStart = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -7) }
        val week = if (comparedDays == 0) emptyList() else entries.filter { it.date >= weekStart.timeInMillis && it.date < weekEnd.timeInMillis }
        val previousWeek = if (comparedDays == 0) emptyList() else entries.filter { it.date >= previousWeekStart.timeInMillis && it.date < weekStart.timeInMillis }
        val weekTotals = totals(week)
        val previousWeekTotals = totals(previousWeek)
        val topCategoryId = weekTotals.maxByOrNull { it.value }?.key
        val digest = WeeklyDigestData(weekStart.timeInMillis, weekEnd.timeInMillis - 1,
            week.sumOf { it.amount }, previousWeek.sumOf { it.amount }, week.size,
            topCategoryId?.let { SpendingDriver(categoryMap.getValue(it), weekTotals.getValue(it), previousWeekTotals[it] ?: 0L) },
            week.maxByOrNull { it.amount }?.contribution(), week.map { it.date }.distinct().size)
        return ExpenseInsights(currency, SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply { timeZone = zone }.format(month.time),
            month.timeInMillis, month.getActualMaximum(Calendar.DAY_OF_MONTH), month.get(Calendar.DAY_OF_WEEK) - 1, comparedDays,
            days, months, categories, drivers, merchants, spikes, SmallPurchaseImpact(smallThreshold, small.sumOf { it.amount }, small.size, purchases.size), digest, omitted)
    }
}
