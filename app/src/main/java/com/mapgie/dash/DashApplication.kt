package com.mapgie.dash
import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mapgie.dash.alarm.DailyStaleChoreWorker
import com.mapgie.dash.notification.NotificationHelper
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
    }
    private fun scheduleDailyChoreCheck() {
        val request = PeriodicWorkRequestBuilder<DailyStaleChoreWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_chore_check", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}