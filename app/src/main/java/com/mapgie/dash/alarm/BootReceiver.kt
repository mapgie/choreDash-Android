package com.mapgie.dash.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mapgie.dash.notification.NotificationHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        NotificationHelper.createChannels(context)
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<BootWorker>().build())
    }
}