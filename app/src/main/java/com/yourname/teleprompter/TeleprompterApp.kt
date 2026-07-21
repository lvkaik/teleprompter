package com.yourname.teleprompter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.yourname.teleprompter.data.crash.CrashReporter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TeleprompterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 必须在所有业务初始化之前：捕获后续任意线程的崩溃
        CrashReporter.install(this)
        createNotificationChannels()
        // 在主线程上弹 Toast 提示上次崩溃（不影响冷启动）
        Handler(Looper.getMainLooper()).post {
            CrashReporter.showLastCrashToast(this)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_FLOATING,
                    getString(R.string.noti_channel_floating),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.noti_channel_floating_desc)
                    setShowBadge(false)
                }
            )
        }
    }

    companion object {
        const val CHANNEL_FLOATING = "prompter_floating"
    }
}