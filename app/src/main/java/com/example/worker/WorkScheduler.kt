package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val PERIODIC_SYNC_WORK_NAME = "PersonalVideoLibrarySyncWork"

    fun schedulePeriodicSync(
        context: Context,
        intervalHours: Int = 12,
        wifiOnly: Boolean = true
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val safeIntervalHours = intervalHours.coerceAtLeast(1)

        val workRequest = PeriodicWorkRequestBuilder<SourceSyncWorker>(
            safeIntervalHours.toLong(),
            TimeUnit.HOURS,
            15,
            TimeUnit.MINUTES // flex interval
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
    }
}
