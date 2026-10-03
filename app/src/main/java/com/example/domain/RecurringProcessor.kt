package com.example.domain

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import java.util.Calendar

object RecurringProcessor {
    private const val MAX_OCCURRENCES_PER_RUN = 128

    suspend fun processDueItems(database: AppDatabase, currentMillis: Long = System.currentTimeMillis()) {
        for (snapshot in database.recurringDao().getActiveRecurring().first()) {
            for (iteration in 0 until MAX_OCCURRENCES_PER_RUN) {
                currentCoroutineContext().ensureActive()
                val processed = database.withTransaction {
                    val item = database.recurringDao().getById(snapshot.id) ?: return@withTransaction false
                    if (!item.isActive || item.nextDueDateMillis > currentMillis) return@withTransaction false
                    if (item.endDateMillis != null && item.nextDueDateMillis > item.endDateMillis) {
                        database.recurringDao().update(item.copy(isActive = false))
                        return@withTransaction false
                    }
                    val next = computeNextDueDate(item.nextDueDateMillis, item.frequency)
                    require(next > item.nextDueDateMillis) { "Recurring date must advance" }
                    database.transactionDao().insertOccurrence(TransactionEntity(
                        type = item.type, dateMillis = item.nextDueDateMillis, amount = item.amount,
                        accountId = item.accountId, toAccountId = item.toAccountId,
                        categoryId = item.categoryId, subcategoryId = item.subcategoryId,
                        payee = item.payee, note = item.note.ifBlank { "Recurring: ${item.name}" },
                        recurringRuleId = item.id, occurrenceKey = "recurring:${item.id}:${item.nextDueDateMillis}"
                    ))
                    database.recurringDao().update(item.copy(
                        nextDueDateMillis = next, lastProcessedDateMillis = item.nextDueDateMillis,
                        isActive = item.endDateMillis == null || next <= item.endDateMillis
                    ))
                    true
                }
                if (!processed) break
            }
        }
        for (snapshot in database.installmentDao().getActiveInstallments().first()) {
            for (iteration in 0 until MAX_OCCURRENCES_PER_RUN) {
                currentCoroutineContext().ensureActive()
                val processed = database.withTransaction {
                    val plan = database.installmentDao().getById(snapshot.id) ?: return@withTransaction false
                    if (plan.isCompleted || plan.nextDueDateMillis > currentMillis ||
                        plan.paidInstallments >= plan.totalInstallments) return@withTransaction false
                    val number = plan.paidInstallments + 1
                    database.transactionDao().insertOccurrence(TransactionEntity(
                        type = TransactionType.EXPENSE, dateMillis = plan.nextDueDateMillis,
                        amount = plan.monthlyAmount, accountId = plan.accountId,
                        categoryId = plan.categoryId, payee = plan.payee,
                        note = "Installment $number/${plan.totalInstallments}: ${plan.name}",
                        installmentId = plan.id, occurrenceKey = "installment:${plan.id}:$number"
                    ))
                    database.installmentDao().update(plan.copy(
                        paidInstallments = number, isCompleted = number >= plan.totalInstallments,
                        nextDueDateMillis = computeNextDueDate(plan.nextDueDateMillis, RecurringFrequency.MONTHLY)
                    ))
                    true
                }
                if (!processed) break
            }
        }
    }

    fun computeNextDueDate(currentDateMillis: Long, frequency: RecurringFrequency): Long =
        Calendar.getInstance().apply {
            timeInMillis = currentDateMillis
            when (frequency) {
                RecurringFrequency.DAILY -> add(Calendar.DAY_OF_YEAR, 1)
                RecurringFrequency.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
                RecurringFrequency.BIWEEKLY -> add(Calendar.WEEK_OF_YEAR, 2)
                RecurringFrequency.MONTHLY -> add(Calendar.MONTH, 1)
                RecurringFrequency.QUARTERLY -> add(Calendar.MONTH, 3)
                RecurringFrequency.YEARLY -> add(Calendar.YEAR, 1)
            }
        }.timeInMillis
}
