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
        // 注意：CrashReporter.install 必须放在 super.onCreate() 之前，
        // 否则 Hilt 在 super 中构造 component 时如果崩，handler 还没装上。
        CrashReporter.install(this)
        super.onCreate()
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