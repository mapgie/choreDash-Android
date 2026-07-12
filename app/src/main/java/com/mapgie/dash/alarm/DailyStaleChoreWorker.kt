package com.mapgie.dash.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.notification.NotificationHelper
import com.mapgie.dash.notification.ReminderKind
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyStaleChoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val choreRepository: ChoreRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val result = choreRepository.load()
        val staleLabels = result.active
            .filter { it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER }
            .map { it.label }
        // Same global delivery-mode setting used for task/reminder alarms, so overdue-chore
        // alerts get the same Alarm/Notification/Silent + DND-bypass behaviour.
        val deliveryMode = runCatching { settingsRepository.settings.first().deliveryMode }
            .getOrDefault("NOTIFICATION")
        val channelId = NotificationHelper.channelId(ReminderKind.CHORE_ALERT, deliveryMode)
        NotificationHelper.showStaleChoresSummary(applicationContext, staleLabels, channelId)
        Result.success()
    }.getOrElse { Result.retry() }
}