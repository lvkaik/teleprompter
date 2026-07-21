package com.yourname.teleprompter.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yourname.teleprompter.MainActivity
import com.yourname.teleprompter.R
import com.yourname.teleprompter.TeleprompterApp
import com.yourname.teleprompter.data.audio.AudioRecorder
import com.yourname.teleprompter.data.prefs.SecurePrefs
import com.yourname.teleprompter.engine.AsrEngine
import com.yourname.teleprompter.engine.MatchEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 提词引擎 Service：
 * 麦克风 → PCM → MiniMax ASR → 文稿匹配 → 广播给悬浮窗
 */
@AndroidEntryPoint
class PrompterEngineService : Service() {

    @Inject lateinit var prefs: SecurePrefs
    @Inject lateinit var audioRecorder: AudioRecorder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engineJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_AI -> {
                val script = intent.getStringExtra(EXTRA_SCRIPT) ?: ""
                startEngine(script)
            }
            ACTION_STOP -> stopEngine()
        }
        return START_STICKY
    }

    private fun startEngine(script: String) {
        stopEngine()
        val apiKey = prefs.getApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "未配置 MiniMax API Key，降级为匀速模式")
            // 此处可发送广播让悬浮窗切到匀速
            return
        }
        val matchEngine = MatchEngine(script)
        val asr = AsrEngine(apiKey)

        engineJob = scope.launch {
            audioRecorder.stream()
                .catch { Log.e(TAG, "音频流异常", it) }
                .let { pcm -> asr.stream(pcm) }
                .catch { Log.e(TAG, "ASR 流异常", it) }
                .collect { msg ->
                    val text = msg.text ?: return@collect
                    if (msg.isFinal == true && text.isNotBlank()) {
                        val newPos = matchEngine.advance(text)
                        // 通过广播通知悬浮窗更新位置
                        val bc = Intent(ACTION_MATCH_UPDATE).apply {
                            setPackage(packageName)
                            putExtra(EXTRA_POS, newPos)
                            putExtra(EXTRA_TEXT, text)
                        }
                        sendBroadcast(bc)
                    }
                }
        }
    }

    private fun stopEngine() {
        engineJob?.cancel()
        engineJob = null
    }

    private fun startForegroundCompat() {
        try {
            val channelId = TeleprompterApp.CHANNEL_FLOATING
            val openIntent = PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notif: Notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_prompter)
                .setContentTitle("AI 提词引擎运行中")
                .setContentText("正在监听语音")
                .setContentIntent(openIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            val micGranted = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (micGranted) {
                    startForeground(NOTI_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } else {
                    Log.w(TAG, "缺少 RECORD_AUDIO 权限，AI 提词引擎无法启动麦克风 FGS，停止服务")
                    stopSelf()
                }
            } else {
                startForeground(NOTI_ID, notif)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "startForegroundCompat 失败，停止服务", e)
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopEngine()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PrompterEngine"
        private const val NOTI_ID = 1002
        const val ACTION_START_AI = "com.yourname.teleprompter.action.START_AI"
        const val ACTION_STOP = "com.yourname.teleprompter.action.STOP_ENGINE"
        const val ACTION_MATCH_UPDATE = "com.yourname.teleprompter.action.MATCH_UPDATE"
        const val EXTRA_SCRIPT = "extra_script"
        const val EXTRA_POS = "extra_pos"
        const val EXTRA_TEXT = "extra_text"
    }
}