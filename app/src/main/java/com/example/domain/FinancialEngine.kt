package com.example.domain

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object FinancialEngine {

    // Analytics run concurrently on background dispatchers. SimpleDateFormat is mutable,
    // so each worker needs its own formatter.
    private val dateFormats = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    private val monthFormats = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM", Locale.US) }
    private val monthDisplayFormats = ThreadLocal.withInitial { SimpleDateFormat("MMMM yyyy", Locale.US) }
    private val dateFormat: SimpleDateFormat get() = dateFormats.get()!!
    private val monthFormat: SimpleDateFormat get() = monthFormats.get()!!
    private val monthDisplayFormat: SimpleDateFormat get() = monthDisplayFormats.get()!!

    fun calculateAccountBalances(
        accounts: List<Account>,
        transactions: List<TransactionEntity>
    ): List<AccountWithBalance> {
        val ledger = HashMap<Long, LongArray>()
        for (tx in transactions) {
            val source = ledger.getOrPut(tx.accountId) { LongArray(5) }
            when (tx.type) {
                TransactionType.INCOME -> source[0] += tx.amount
                TransactionType.EXPENSE -> source[1] += tx.amount
                TransactionType.TRANSFER -> {
                    source[3] += tx.amount
                    source[4] += tx.transferFee
                    tx.toAccountId?.let { ledger.getOrPut(it) { LongArray(5) }[2] += tx.amount }
                }
            }
        }
        return accounts.map { account ->
            val totals = ledger[account.id] ?: LongArray(5)
            val income = totals[0]
            val expense = totals[1]
            val transferIn = totals[2]
            val transferOut = totals[3]
            val fees = totals[4]

            val isLiability = isLiabilityAccount(account.type)
            val calculatedBalance = if (isLiability) {
                // For credit cards & loans, balance represents debt/outstanding
                account.initialBalance + expense - transferIn + transferOut
            } else {
                // For cash, bank, investment, savings:
                account.initialBalance + income - expense + transferIn - transferOut - fees
            }

            val availableCredit = if (account.type == AccountType.CREDIT_CARD) {
                max(0L, account.creditLimit - calculatedBalance)
            } else 0L

            val utilization = if (account.type == AccountType.CREDIT_CARD && account.creditLimit > 0) {
                (calculatedBalance.toFloat() / account.creditLimit.toFloat()) * 100f
            } else 0f

            AccountWithBalance(
                account = account,
                calculatedBalance = calculatedBalance,
                totalIncome = income,
                totalExpense = expense,
                totalTransferIn = transferIn,
                totalTransferOut = transferOut,
                creditLimit = account.creditLimit,
                availableCredit = availableCredit,
                utilizationPercent = utilization
            )
        }
    }

    fun isLiabilityAccount(type: AccountType): Boolean {
        return type == AccountType.CREDIT_CARD ||
                type == AccountType.LOAN ||
                type == AccountType.OTHER_LIABILITY
    }

    fun calculateNetWorth(accountBalances: List<AccountWithBalance>): NetWorthSummary {
        var totalAssets = 0L
        var totalLiabilities = 0L
        var cashAndBank = 0L
        var investments = 0L
        var otherAssets = 0L
        var creditCardDebt = 0L
        var loanDebt = 0L

        for (acc in accountBalances) {
            if (!acc.account.includeInNetWorth || acc.account.isArchived) continue

            when (acc.account.type) {
                AccountType.CASH, AccountType.BANK, AccountType.SAVINGS, AccountType.DEBIT_CARD -> {
                    totalAssets += acc.calculatedBalance
                    cashAndBank += acc.calculatedBalance
                }
                AccountType.INVESTMENT, AccountType.FIXED_DEPOSIT, AccountType.REAL_ESTATE -> {
                    totalAssets += acc.calculatedBalance
                    investments += acc.calculatedBalance
                }
                AccountType.OTHER_ASSET, AccountType.INSURANCE -> {
                    totalAssets += acc.calculatedBalance
                    otherAssets += acc.calculatedBalance
                }
                AccountType.CREDIT_CARD -> {
                    totalLiabilities += acc.calculatedBalance
                    creditCardDebt += acc.calculatedBalance
                }
                AccountType.LOAN -> {
                    totalLiabilities += acc.calculatedBalance
                    loanDebt += acc.calculatedBalance
                }
                AccountType.OTHER_LIABILITY -> {
                    totalLiabilities += acc.calculatedBalance
                }
            }
        }

        return NetWorthSummary(
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = totalAssets - totalLiabilities,
            cashAndBank = cashAndBank,
            investments = investments,
            otherAssets = otherAssets,
            creditCardDebt = creditCardDebt,
            loanDebt = loanDebt
        )
    }

    fun calculatePeriodSummary(
        transactions: List<TransactionWithDetails>,
        previousTransactions: List<TransactionWithDetails> = emptyList()
    ): PeriodSummary {
        var income = 0L
        var expense = 0L
        var count = 0

        for (item in transactions) {
            val tx = item.transaction
            if (tx.isExcludedFromStats) continue
            when (tx.type) {
                TransactionType.INCOME -> {
                    income += tx.amount
                    count++
                }
                TransactionType.EXPENSE -> {
                    expense += tx.amount
                    count++
                }
                TransactionType.TRANSFER -> {
                    // Internal transfers do NOT count towards period income/expense,
                    // but transfer fees count as expense
                    if (tx.transferFee > 0) {
                        expense += tx.transferFee
                    }
                }
            }
        }

        var prevIncome = 0L
        var prevExpense = 0L
        for (item in previousTransactions) {
            val tx = item.transaction
            if (tx.isExcludedFromStats) continue
            when (tx.type) {
                TransactionType.INCOME -> prevIncome += tx.amount
                TransactionType.EXPENSE -> prevExpense += tx.amount
                TransactionType.TRANSFER -> if (tx.transferFee > 0) prevExpense += tx.transferFee
            }
        }

        val netSavings = income - expense
        val savingsRate = if (income > 0) {
            (netSavings.toFloat() / income.toFloat()) * 100f
        } else 0f

        val expenseChange = if (prevExpense > 0) {
            ((expense - prevExpense).toFloat() / prevExpense.toFloat()) * 100f
        } else 0f

        val incomeChange = if (prevIncome > 0) {
            ((income - prevIncome).toFloat() / prevIncome.toFloat()) * 100f
        } else 0f

        return PeriodSummary(
            totalIncome = income,
            totalExpense = expense,
            netSavings = netSavings,
            savingsRate = savingsRate,
            transactionCount = count,
            previousPeriodIncome = prevIncome,
            previousPeriodExpense = prevExpense,
            expenseChangePercent = expenseChange,
            incomeChangePercent = incomeChange
        )
    }

    fun groupDaily(
        transactions: List<TransactionWithDetails>,
        memos: List<DailyMemo> = emptyList()
    ): List<DailyAggregation> {
        val memoMap = memos.associateBy { it.dateString }
        val groups = transactions.groupBy {
            dateFormat.format(Date(it.transaction.dateMillis))
        }

        return groups.map { (dateStr, txs) ->
            val firstMillis = txs.firstOrNull()?.transaction?.dateMillis ?: System.currentTimeMillis()
            var inc = 0L
            var exp = 0L

            for (t in txs) {
                if (t.transaction.isExcludedFromStats) continue
                when (t.transaction.type) {
                    TransactionType.INCOME -> inc += t.transaction.amount
                    TransactionType.EXPENSE -> exp += t.transaction.amount
                    TransactionType.TRANSFER -> if (t.transaction.transferFee > 0) exp += t.transaction.transferFee
                }
            }

            DailyAggregation(
                dateString = dateStr,
                dateMillis = firstMillis,
                totalIncome = inc,
                totalExpense = exp,
                balance = inc - exp,
                transactions = txs.sortedByDescending { it.transaction.dateMillis },
                memo = memoMap[dateStr]
            )
        }.sortedByDescending { it.dateMillis }
    }

    fun groupWeekly(transactions: List<TransactionWithDetails>): List<WeeklyAggregation> {
        val calendar = Calendar.getInstance()
        val groups = transactions.groupBy {
            calendar.timeInMillis = it.transaction.dateMillis
            val year = calendar.get(Calendar.YEAR)
            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            "$year-W$week"
        }

        return groups.map { (key, txs) ->
            var inc = 0L
            var exp = 0L
            for (t in txs) {
                if (t.transaction.isExcludedFromStats) continue
                when (t.transaction.type) {
                    TransactionType.INCOME -> inc += t.transaction.amount
                    TransactionType.EXPENSE -> exp += t.transaction.amount
                    TransactionType.TRANSFER -> if (t.transaction.transferFee > 0) exp += t.transaction.transferFee
                }
            }
            val minMillis = txs.minOfOrNull { it.transaction.dateMillis } ?: 0L
            val maxMillis = txs.maxOfOrNull { it.transaction.dateMillis } ?: 0L
            calendar.timeInMillis = minMillis
            val weekNum = calendar.get(Calendar.WEEK_OF_YEAR)

            val weekLabel = "Week $weekNum (${dateFormat.format(Date(minMillis))} - ${dateFormat.format(Date(maxMillis))})"

            WeeklyAggregation(
                weekNumber = weekNum,
                startDateMillis = minMillis,
                endDateMillis = maxMillis,
                label = weekLabel,
                totalIncome = inc,
                totalExpense = exp,
                balance = inc - exp,
                transactionCount = txs.size,
                transactions = txs.sortedByDescending { it.transaction.dateMillis }
            )
        }.sortedByDescending { it.startDateMillis }
    }

    fun groupMonthly(transactions: List<TransactionWithDetails>): List<MonthlyAggregation> {
        val groups = transactions.groupBy {
            monthFormat.format(Date(it.transaction.dateMillis))
        }

        val sortedMonths = groups.keys.sortedDescending()
        val result = mutableListOf<MonthlyAggregation>()

        for (i in sortedMonths.indices) {
            val monthStr = sortedMonths[i]
            val txs = groups[monthStr] ?: emptyList()
            var inc = 0L
            var exp = 0L

            for (t in txs) {
                if (t.transaction.isExcludedFromStats) continue
                when (t.transaction.type) {
                    TransactionType.INCOME -> inc += t.transaction.amount
                    TransactionType.EXPENSE -> exp += t.transaction.amount
                    TransactionType.TRANSFER -> if (t.transaction.transferFee > 0) exp += t.transaction.transferFee
                }
            }

            val balance = inc - exp
            val savingsRate = if (inc > 0) (balance.toFloat() / inc.toFloat()) * 100f else 0f

            var prevChange = 0L
            if (i + 1 < sortedMonths.size) {
                val prevMonthStr = sortedMonths[i + 1]
                val prevTxs = groups[prevMonthStr] ?: emptyList()
                var pInc = 0L
                var pExp = 0L
                for (pt in prevTxs) {
                    when (pt.transaction.type) {
                        TransactionType.INCOME -> pInc += pt.transaction.amount
                        TransactionType.EXPENSE -> pExp += pt.transaction.amount
                        TransactionType.TRANSFER -> if (pt.transaction.transferFee > 0) pExp += pt.transaction.transferFee
                    }
                }
                prevChange = balance - (pInc - pExp)
            }

            val sampleDate = txs.firstOrNull()?.transaction?.dateMillis?.let { Date(it) } ?: Date()
            val monthLabel = monthDisplayFormat.format(sampleDate)

            result.add(
                MonthlyAggregation(
                    monthString = monthStr,
                    monthLabel = monthLabel,
                    totalIncome = inc,
                    totalExpense = exp,
                    balance = balance,
                    savingsRate = savingsRate,
                    previousMonthBalanceChange = prevChange,
                    transactionCount = txs.size
                )
            )
        }

        return result
    }

    fun calculateCategorySpending(
        transactions: List<TransactionWithDetails>,
        type: TransactionType = TransactionType.EXPENSE
    ): List<CategorySpending> {
        val filtered = transactions.filter {
            it.transaction.type == type && !it.transaction.isExcludedFromStats
        }
        val totalSum = filtered.sumOf { it.transaction.amount }
        if (totalSum == 0L) return emptyList()

        val byCategory = filtered.groupBy { it.category }

        return byCategory.mapNotNull { (category, txList) ->
            if (category == null) return@mapNotNull null
            val catTotal = txList.sumOf { it.transaction.amount }
            val catPercent = (catTotal.toFloat() / totalSum.toFloat()) * 100f

            val subGroups = txList.groupBy { it.subcategory }
            val subBreakdown = subGroups.mapNotNull { (sub, subTxList) ->
                if (sub == null) return@mapNotNull null
                val subTotal = subTxList.sumOf { it.transaction.amount }
                val subPercent = (subTotal.toFloat() / catTotal.toFloat()) * 100f
                SubcategorySpending(
                    subcategory = sub,
                    totalAmount = subTotal,
                    transactionCount = subTxList.size,
                    percentage = subPercent
                )
            }.sortedByDescending { it.totalAmount }

            CategorySpending(
                category = category,
                totalAmount = catTotal,
                transactionCount = txList.size,
                percentage = catPercent,
                subcategoryBreakdown = subBreakdown
            )
        }.sortedByDescending { it.totalAmount }
    }

    fun calculateMerchantAnalytics(transactions: List<TransactionWithDetails>): List<MerchantStat> {
        val expenseTx = transactions.filter {
            it.transaction.type == TransactionType.EXPENSE &&
                    it.transaction.payee.isNotBlank() &&
                    !it.transaction.isExcludedFromStats
        }

        val byMerchant = expenseTx.groupBy { it.transaction.payee.trim() }

        return byMerchant.map { (name, list) ->
            val total = list.sumOf { it.transaction.amount }
            val count = list.size
            val avg = if (count > 0) total / count else 0L
            val mostRecent = list.maxOfOrNull { it.transaction.dateMillis } ?: 0L

            MerchantStat(
                merchantName = name,
                totalSpent = total,
                transactionCount = count,
                averageSpent = avg,
                mostRecentDateMillis = mostRecent
            )
        }.sortedByDescending { it.totalSpent }
    }

    fun calculateBudgetProgressList(
        budgets: List<Budget>,
        categories: List<Category>,
        monthTransactions: List<TransactionWithDetails>,
        daysElapsedInMonth: Int,
        totalDaysInMonth: Int
    ): List<BudgetProgress> {
        val expenseTxs = monthTransactions.filter {
            it.transaction.type == TransactionType.EXPENSE && !it.transaction.isExcludedFromStats
        }
        val categoryMap = categories.associateBy { it.id }
        val spendByCategory = expenseTxs.groupBy { it.transaction.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.transaction.amount } }

        val recommendedPace = if (totalDaysInMonth > 0) {
            (daysElapsedInMonth.toFloat() / totalDaysInMonth.toFloat()) * 100f
        } else 0f

        return budgets.map { budget ->
            val category = if (budget.categoryId == 0L) null else categoryMap[budget.categoryId]
            val spent = if (budget.categoryId == 0L) {
                // Total budget
                expenseTxs.sumOf { it.transaction.amount }
            } else {
                spendByCategory[budget.categoryId] ?: 0L
            }

            val remaining = budget.amount - spent
            val utilPercent = if (budget.amount > 0) {
                (spent.toFloat() / budget.amount.toFloat()) * 100f
            } else 0f

            BudgetProgress(
                budget = budget,
                category = category,
                budgetAmount = budget.amount,
                spentAmount = spent,
                remainingAmount = remaining,
                utilizationPercent = utilPercent,
                recommendedPacePercent = recommendedPace,
                isOverBudget = spent > budget.amount
            )
        }.sortedWith(compareBy<BudgetProgress> { it.category != null }.thenByDescending { it.spentAmount })
    }

    fun calculateDailySpendingTrend(
        transactions: List<TransactionWithDetails>,
        monthCalendar: Calendar
    ): DailySpendingTrendData {
        val maxDays = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyExpenseMap = LongArray(maxDays) { 0L }
        val cal = Calendar.getInstance()

        for (item in transactions) {
            if (item.transaction.isExcludedFromStats) continue
            if (item.transaction.type == TransactionType.EXPENSE) {
                cal.timeInMillis = item.transaction.dateMillis
                val day = cal.get(Calendar.DAY_OF_MONTH)
                if (day in 1..maxDays) {
                    dailyExpenseMap[day - 1] += item.transaction.amount
                }
            } else if (item.transaction.type == TransactionType.TRANSFER && item.transaction.transferFee > 0) {
                cal.timeInMillis = item.transaction.dateMillis
                val day = cal.get(Calendar.DAY_OF_MONTH)
                if (day in 1..maxDays) {
                    dailyExpenseMap[day - 1] += item.transaction.transferFee
                }
            }
        }

        val totalExpense = dailyExpenseMap.sum()
        val averageDailyExpense = if (maxDays > 0) totalExpense / maxDays else 0L
        val peakExpense = dailyExpenseMap.maxOrNull() ?: 0L
        val peakDay = if (peakExpense > 0) (dailyExpenseMap.indexOf(peakExpense) + 1) else 1
        val activeDaysCount = dailyExpenseMap.count { it > 0 }

        var runningCumulative = 0L
        val days = mutableListOf<DailyTrendPoint>()
        val idealPaceCumulative = mutableListOf<Long>()

        for (d in 0 until maxDays) {
            val dayNum = d + 1
            val dayExp = dailyExpenseMap[d]
            runningCumulative += dayExp

            // 7-day rolling window centered on current day: [d - 3, d + 3]
            val wStart = max(0, d - 3)
            val wEnd = min(maxDays - 1, d + 3)
            var wSum = 0L
            for (w in wStart..wEnd) {
                wSum += dailyExpenseMap[w]
            }
            val movingAvg = if (wEnd >= wStart) wSum / (wEnd - wStart + 1) else 0L
            val isSpike = dayExp > 0 && dayExp >= (averageDailyExpense * 1.75f).toLong() && dayExp >= (movingAvg * 1.5f).toLong()

            val pointCal = (monthCalendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, dayNum)
                set(Calendar.HOUR_OF_DAY, 12)
            }

            days.add(
                DailyTrendPoint(
                    dayOfMonth = dayNum,
                    dateMillis = pointCal.timeInMillis,
                    totalExpense = dayExp,
                    movingAverageExpense = movingAvg,
                    isSpike = isSpike,
                    cumulativeExpense = runningCumulative
                )
            )

            // Linear ideal pace target progression
            val idealForDay = if (maxDays > 0) (totalExpense * dayNum) / maxDays else 0L
            idealPaceCumulative.add(idealForDay)
        }

        return DailySpendingTrendData(
            days = days,
            idealPaceCumulative = idealPaceCumulative,
            averageDailyExpense = averageDailyExpense,
            peakDay = peakDay,
            peakExpense = peakExpense,
            activeDaysCount = activeDaysCount,
            totalExpense = totalExpense
        )
    }

    fun calculateDayOfWeekDistribution(
        transactions: List<TransactionWithDetails>
    ): List<DayOfWeekSpending> {
        val cal = Calendar.getInstance()
        val totals = LongArray(7) { 0L }
        val counts = IntArray(7) { 0 }

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        for (item in transactions) {
            if (item.transaction.isExcludedFromStats) continue
            val amount = when (item.transaction.type) {
                TransactionType.EXPENSE -> item.transaction.amount
                TransactionType.TRANSFER -> item.transaction.transferFee
                else -> 0L
            }
            if (amount <= 0L) continue

            cal.timeInMillis = item.transaction.dateMillis
            val calDow = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1, MONDAY=2, ... SATURDAY=7
            val index = when (calDow) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            totals[index] += amount
            counts[index] += 1
        }

        val totalAll = totals.sum()
        val maxTotal = totals.maxOrNull() ?: 0L

        return (0 until 7).map { i ->
            val total = totals[i]
            val pct = if (totalAll > 0) (total.toFloat() / totalAll.toFloat()) * 100f else 0f
            DayOfWeekSpending(
                dayOfWeek = i,
                dayName = dayNames[i],
                totalExpense = total,
                transactionCount = counts[i],
                percentage = pct,
                isPeakDay = total > 0 && total == maxTotal
            )
        }
    }

    fun calculateMonthOverMonthCategoryComparison(
        currentTxs: List<TransactionWithDetails>,
        prevTxs: List<TransactionWithDetails>
    ): List<CategoryComparisonItem> {
        val currentCategoryMap = mutableMapOf<Long, Pair<Category, Long>>()
        val prevCategoryMap = mutableMapOf<Long, Pair<Category, Long>>()

        for (item in currentTxs) {
            if (item.transaction.isExcludedFromStats || item.transaction.type != TransactionType.EXPENSE) continue
            val cat = item.category ?: continue
            val existing = currentCategoryMap[cat.id]?.second ?: 0L
            currentCategoryMap[cat.id] = Pair(cat, existing + item.transaction.amount)
        }

        for (item in prevTxs) {
            if (item.transaction.isExcludedFromStats || item.transaction.type != TransactionType.EXPENSE) continue
            val cat = item.category ?: continue
            val existing = prevCategoryMap[cat.id]?.second ?: 0L
            prevCategoryMap[cat.id] = Pair(cat, existing + item.transaction.amount)
        }

        val allCategoryIds = (currentCategoryMap.keys + prevCategoryMap.keys).toSet()

        return allCategoryIds.mapNotNull { catId ->
            val cat = currentCategoryMap[catId]?.first ?: prevCategoryMap[catId]?.first ?: return@mapNotNull null
            val curAmt = currentCategoryMap[catId]?.second ?: 0L
            val prevAmt = prevCategoryMap[catId]?.second ?: 0L

            val deltaPercent = if (prevAmt > 0) {
                ((curAmt - prevAmt).toFloat() / prevAmt.toFloat()) * 100f
            } else if (curAmt > 0) {
                100f
            } else {
                0f
            }

            CategoryComparisonItem(
                category = cat,
                currentMonthAmount = curAmt,
                previousMonthAmount = prevAmt,
                deltaPercent = deltaPercent,
                isIncrease = curAmt > prevAmt
            )
        }.sortedByDescending { max(it.currentMonthAmount, it.previousMonthAmount) }.take(8)
    }
}
