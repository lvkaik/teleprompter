package com.yourname.teleprompter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.yourname.teleprompter.data.prefs.SecurePrefs
import com.yourname.teleprompter.service.FloatingWindowService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "com.oplus.action.BOOT_COMPLETED") return

        val prefs = SecurePrefs(context)
        if (!prefs.isAutoStartOnBoot()) return

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Teleprompter::BootWakeLock"
        ).apply { acquire(60 * 1000L) }

        ContextCompat.startForegroundService(
            context,
            Intent(context, FloatingWindowService::class.java).apply {
                action = FloatingWindowService.ACTION_START
            }
        )
    }
}