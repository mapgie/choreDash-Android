package com.mapgie.dash
import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mapgie.dash.alarm.DailyStaleChoreWorker
import com.mapgie.dash.notification.NotificationHelper
import com.mapgie.dash.widget.WidgetRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class DashApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        scheduleDailyChoreCheck()
        scheduleWidgetRefresh()
    }
    private fun scheduleDailyChoreCheck() {
        val request = PeriodicWorkRequestBuilder<DailyStaleChoreWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_chore_check", ExistingPeriodicWorkPolicy.KEEP, request)
    }
    private fun scheduleWidgetRefresh() {
        // Widgets read from Supabase, so there's no point running (and retrying) with no connectivity.
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "widget_refresh", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}