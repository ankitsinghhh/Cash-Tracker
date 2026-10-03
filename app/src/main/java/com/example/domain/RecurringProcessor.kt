package com.example.domain

import com.example.data.local.*
import com.example.data.model.*
import java.util.*

object RecurringProcessor {

    suspend fun processDueItems(
        database: AppDatabase,
        currentMillis: Long = System.currentTimeMillis()
    ) {
        processRecurringTransactions(database, currentMillis)
        processInstallmentPlans(database, currentMillis)
    }

    private suspend fun processRecurringTransactions(
        database: AppDatabase,
        currentMillis: Long
    ) {
        val recurringDao = database.recurringDao()
        val transactionDao = database.transactionDao()

        // Get snapshot of active items
        val activeList = mutableListOf<RecurringTransaction>()
        recurringDao.getAllRecurring().collect { list ->
            activeList.clear()
            activeList.addAll(list.filter { it.isActive })
            return@collect
        }

        for (item in activeList) {
            if (item.nextDueDateMillis <= currentMillis) {
                // Generate transaction
                val tx = TransactionEntity(
                    type = item.type,
                    dateMillis = item.nextDueDateMillis,
                    amount = item.amount,
                    accountId = item.accountId,
                    toAccountId = item.toAccountId,
                    categoryId = item.categoryId,
                    subcategoryId = item.subcategoryId,
                    payee = item.payee,
                    note = item.note.ifBlank { "Recurring: ${item.name}" },
                    recurringRuleId = item.id
                )
                transactionDao.insert(tx)

                // Calculate next due date
                val nextDate = computeNextDueDate(item.nextDueDateMillis, item.frequency)
                val shouldDeactivate = item.endDateMillis != null && nextDate > item.endDateMillis

                val updated = item.copy(
                    nextDueDateMillis = nextDate,
                    lastProcessedDateMillis = item.nextDueDateMillis,
                    isActive = !shouldDeactivate
                )
                recurringDao.update(updated)
            }
        }
    }

    private suspend fun processInstallmentPlans(
        database: AppDatabase,
        currentMillis: Long
    ) {
        val installmentDao = database.installmentDao()
        val transactionDao = database.transactionDao()

        val activePlans = mutableListOf<InstallmentPlan>()
        installmentDao.getAllInstallments().collect { list ->
            activePlans.clear()
            activePlans.addAll(list.filter { !it.isCompleted })
            return@collect
        }

        for (plan in activePlans) {
            if (plan.nextDueDateMillis <= currentMillis && plan.paidInstallments < plan.totalInstallments) {
                val currentInstallmentNumber = plan.paidInstallments + 1
                val tx = TransactionEntity(
                    type = TransactionType.EXPENSE,
                    dateMillis = plan.nextDueDateMillis,
                    amount = plan.monthlyAmount,
                    accountId = plan.accountId,
                    categoryId = plan.categoryId,
                    payee = plan.payee,
                    note = "Installment $currentInstallmentNumber/${plan.totalInstallments}: ${plan.name}",
                    installmentId = plan.id
                )
                transactionDao.insert(tx)

                val newPaidCount = currentInstallmentNumber
                val isDone = newPaidCount >= plan.totalInstallments
                val nextDue = computeNextDueDate(plan.nextDueDateMillis, RecurringFrequency.MONTHLY)

                val updated = plan.copy(
                    paidInstallments = newPaidCount,
                    isCompleted = isDone,
                    nextDueDateMillis = nextDue
                )
                installmentDao.update(updated)
            }
        }
    }

    fun computeNextDueDate(currentDateMillis: Long, frequency: RecurringFrequency): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = currentDateMillis
        when (frequency) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.BIWEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurringFrequency.QUARTERLY -> cal.add(Calendar.MONTH, 3)
            RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }
}
