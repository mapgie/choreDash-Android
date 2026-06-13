package com.mapgie.dash.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodically refreshes widget content so "next up" reflects the current date/time. */
@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        WidgetUpdater.updateAll(applicationContext)
        Result.success()
    }.getOrElse { Result.retry() }
}
