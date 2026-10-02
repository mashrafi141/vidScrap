package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.database.DatabaseProvider
import com.example.data.preferences.AppPreferences
import com.example.data.repository.LibraryRepository

class SourceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = DatabaseProvider.getDatabase(applicationContext)
            val prefs = AppPreferences(applicationContext)
            val repo = LibraryRepository(
                sourceDao = db.sourceDao(),
                videoDao = db.videoDao(),
                preferences = prefs
            )

            val result = repo.syncAllEnabledSources()
            if (result.failedSources > 0 && result.successfulSources == 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
