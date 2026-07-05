package com.mapgie.dash.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mapgie.dash.notification.NotificationHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        NotificationHelper.createChannels(context)
        // Unique work: BOOT_COMPLETED and MY_PACKAGE_REPLACED can both arrive in a
        // short window; one reschedule pass covers both. No network constraint —
        // local (DataStore) reminders must be rescheduled even offline; the worker
        // retries with backoff when Supabase is unreachable.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "boot_reschedule",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<BootWorker>().build()
        )
    }
}