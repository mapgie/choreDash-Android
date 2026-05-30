package com.mapgie.dash.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyStaleChoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val choreRepository: ChoreRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val result = choreRepository.load()
        val staleLabels = result.active
            .filter { it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER }
            .map { it.label }
        NotificationHelper.showStaleChoresSummary(applicationContext, staleLabels)
        Result.success()
    }.getOrElse { Result.retry() }
}
