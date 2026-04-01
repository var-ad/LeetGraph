package com.example.leetgraph

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class LeetGraphSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val syncResult = LeetGraphSyncRunner.refreshNow(applicationContext)
        return when {
            syncResult.success -> Result.success()
            syncResult.shouldRetry -> Result.retry()
            else -> Result.success()
        }
    }
}
