package com.example.data.model

data class TransactionWithDetails(
    val transaction: TransactionEntity,
    val account: Account?,
    val toAccount: Account?,
    val category: Category?,
    val subcategory: Category?
)

data class AccountWithBalance(
    val account: Account,
    val calculatedBalance: Long, // in minor units
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val totalTransferIn: Long = 0L,
    val totalTransferOut: Long = 0L,
    // Credit card stats
    val creditLimit: Long = 0L,
    val availableCredit: Long = 0L,
    val utilizationPercent: Float = 0f
)

data class CategorySpending(
    val category: Category,
    val totalAmount: Long, // in minor units
    val transactionCount: Int,
    val percentage: Float,
    val subcategoryBreakdown: List<SubcategorySpending> = emptyList()
)

data class SubcategorySpending(
    val subcategory: Category,
    val totalAmount: Long,
    val transactionCount: Int,
    val percentage: Float
)

data class DailyAggregation(
    val dateString: String, // "YYYY-MM-DD"
    val dateMillis: Long,
    val totalIncome: Long,
    val totalExpense: Long,
    val balance: Long,
    val transactions: List<TransactionWithDetails>,
    val memo: DailyMemo? = null
)

data class WeeklyAggregation(
    val weekNumber: Int,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val label: String,
    val totalIncome: Long,
    val totalExpense: Long,
    val balance: Long,
    val transactionCount: Int,
    val transactions: List<TransactionWithDetails>
)

data class MonthlyAggregation(
    val monthString: String, // "YYYY-MM"
    val monthLabel: String,
    val totalIncome: Long,
    val totalExpense: Long,
    val balance: Long,
    val savingsRate: Float,
    val previousMonthBalanceChange: Long = 0L,
    val transactionCount: Int
)

data class BudgetProgress(
    val budget: Budget,
    val category: Category?, // null for Total Budget
    val budgetAmount: Long,
    val spentAmount: Long,
    val remainingAmount: Long,
    val utilizationPercent: Float,
    val recommendedPacePercent: Float,
    val isOverBudget: Boolean
)

data class MerchantStat(
    val merchantName: String,
    val totalSpent: Long,
    val transactionCount: Int,
    val averageSpent: Long,
    val mostRecentDateMillis: Long
)

data class NetWorthSummary(
    val totalAssets: Long,
    val totalLiabilities: Long,
    val netWorth: Long,
    val cashAndBank: Long,
    val investments: Long,
    val otherAssets: Long,
    val creditCardDebt: Long,
    val loanDebt: Long
)

data class PeriodSummary(
    val totalIncome: Long,
    val totalExpense: Long,
    val netSavings: Long,
    val savingsRate: Float,
    val transactionCount: Int,
    val previousPeriodIncome: Long = 0L,
    val previousPeriodExpense: Long = 0L,
    val expenseChangePercent: Float = 0f,
    val incomeChangePercent: Float = 0f
)

data class DailyTrendPoint(
    val dayOfMonth: Int,
    val dateMillis: Long,
    val totalExpense: Long,
    val movingAverageExpense: Long,
    val isSpike: Boolean,
    val cumulativeExpense: Long
)

data class DailySpendingTrendData(
    val days: List<DailyTrendPoint>,
    val idealPaceCumulative: List<Long>,
    val averageDailyExpense: Long,
    val peakDay: Int,
    val peakExpense: Long,
    val activeDaysCount: Int,
    val totalExpense: Long
)

data class CategoryComparisonItem(
    val category: Category,
    val currentMonthAmount: Long,
    val previousMonthAmount: Long,
    val deltaPercent: Float,
    val isIncrease: Boolean
)

data class DayOfWeekSpending(
    val dayOfWeek: Int,
    val dayName: String, // "Mon", "Tue", etc.
    val totalExpense: Long,
    val transactionCount: Int,
    val percentage: Float,
    val isPeakDay: Boolean
)

