package com.example.domain

import android.content.Context
import androidx.work.*
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class RecurringWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        RecurringProcessor.processDueItems(AppDatabase.getDatabase(applicationContext))
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        fun schedule(context: Context) {
            val manager = WorkManager.getInstance(context)
            manager.enqueueUniquePeriodicWork("cash-tracker-recurring", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RecurringWorker>(12, TimeUnit.HOURS).build())
            manager.enqueueUniqueWork("cash-tracker-recurring-now", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<RecurringWorker>().build())
        }
    }
}
