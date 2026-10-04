package com.example.data.model

/** Stable IDs are persisted independently, so adding a chart preserves existing choices. */
enum class InsightWidget(val title: String, val description: String, val isNew: Boolean = false) {
    CATEGORY_DONUT("Category breakdown", "Expense and income category donut"),
    CATEGORY_RANKING("Category ranking", "Ranked category bars and details"),
    DAILY_TREND("Daily trend", "Daily expenses and seven-day moving average"),
    CUMULATIVE_PACE("Cumulative spending", "Spending accumulated through the month"),
    CASH_FLOW("Cash flow", "Historical income, expenses and savings rate"),
    CATEGORY_COMPARISON("Month comparison", "Income, expenses and category shifts"),
    WEEKDAY("Weekday habits", "Spending by day of the week"),
    MERCHANT_RANKING("Merchant ranking", "Top merchants across your history"),
    HEATMAP("Spending heatmap", "Tap a day to see its spending", true),
    CATEGORY_TRENDS("Category trends", "Six months of stacked category spending", true),
    SPENDING_CHANGE("Spending change explanation", "Category drivers of the change from last month", true),
    RECURRING_TREND("Recurring cost trend", "Recorded recurring expenses and installments", true),
    SMALL_PURCHASES("Small purchase impact", "See how purchases below your chosen amount add up", true),
    FREQUENCY("Frequency vs purchase size", "Monthly purchase counts and average amounts", true),
    MERCHANT_HABITS("Merchant habits", "Frequency, average purchase and total this month", true),
    SPIKES("Spending spikes", "The purchases behind unusually expensive days", true),
    SEASONALITY("Annual seasonality", "Compare the same months across calendar years", true),
    SAVINGS_WHAT_IF("Savings what-if", "Explore a category reduction you choose", true),
    WEEKLY_DIGEST("Weekly digest", "A seven-day recap with spending drivers", true)
}

data class InsightCategory(val id: Long, val name: String)
data class InsightMonth(
    val key: String, val label: String, val year: Int, val month: Int,
    val expense: Long, val count: Int, val purchaseTotal: Long, val recurring: Long, val installments: Long,
    val categoryAmounts: Map<Long, Long>, val hasRecords: Boolean
) { val average: Long get() = if (count == 0) 0 else purchaseTotal / count }
data class ExpenseContribution(val id: Long, val label: String, val amount: Long)
data class ExpenseDay(val day: Int, val dateMillis: Long, val amount: Long, val count: Int, val contributors: List<ExpenseContribution>)
data class SpendingDriver(val category: InsightCategory, val current: Long, val previous: Long) { val delta: Long get() = current - previous }
data class HabitMerchant(val name: String, val total: Long, val count: Int) { val average: Long get() = total / count }
data class SmallPurchaseImpact(val threshold: Long, val total: Long, val count: Int, val allCount: Int)
data class WeeklyDigestData(val startMillis: Long, val endMillis: Long, val expense: Long, val previousExpense: Long,
    val count: Int, val topCategory: SpendingDriver?, val largest: ExpenseContribution?, val daysWithSpending: Int)
data class KnownUpcomingBill(val name: String, val amount: Long, val dueMillis: Long)
data class ExpenseInsights(
    val currency: String, val monthLabel: String, val monthStartMillis: Long, val daysInMonth: Int,
    val sundayOffset: Int, val comparedDays: Int, val days: List<ExpenseDay>,
    val months: List<InsightMonth>, val categories: List<InsightCategory>, val drivers: List<SpendingDriver>,
    val merchants: List<HabitMerchant>, val spikes: List<ExpenseDay>, val smallPurchases: SmallPurchaseImpact,
    val digest: WeeklyDigestData, val omittedCurrencyRecords: Int,
    val upcomingBills: List<KnownUpcomingBill> = emptyList(), val monthBudgetAmount: Long = 0L
) {
    val total: Long get() = days.sumOf { it.amount }
    val previousTotal: Long get() = drivers.sumOf { it.previous }
}
