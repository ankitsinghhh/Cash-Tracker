package com.example

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithDetails
import com.example.domain.FinancialEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FinancialEngineConcurrencyTest {
    @Test
    fun groupingKeepsDatesAndTotalsCorrectAcrossConcurrentWorkers() {
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            val results = (0 until 8).map { worker ->
                executor.submit {
                    val date = Calendar.getInstance().apply {
                        clear()
                        set(2024 + worker, worker, 15, 12, 0, 0)
                    }
                    val expectedDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.time)
                    val expectedMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(date.time)
                    val transactions = (1L..100L).map { id ->
                        TransactionWithDetails(
                            transaction = TransactionEntity(
                                id = id, type = TransactionType.EXPENSE,
                                dateMillis = date.timeInMillis, amount = 100L,
                                accountId = 1L, categoryId = 1L
                            ),
                            account = null, toAccount = null, category = null, subcategory = null
                        )
                    }
                    start.await()
                    repeat(150) {
                        val daily = FinancialEngine.groupDaily(transactions).single()
                        assertEquals(expectedDay, daily.dateString)
                        assertEquals(10000L, daily.totalExpense)
                        val weekly = FinancialEngine.groupWeekly(transactions).single()
                        assertEquals(100, weekly.transactionCount)
                        assertEquals(10000L, weekly.totalExpense)
                        val monthly = FinancialEngine.groupMonthly(transactions).single()
                        assertEquals(expectedMonth, monthly.monthString)
                        assertEquals(10000L, monthly.totalExpense)
                    }
                }
            }
            start.countDown()
            results.forEach { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }
}
