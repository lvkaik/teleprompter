package com.yourname.teleprompter.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.yourname.teleprompter.MainActivity
import com.yourname.teleprompter.R
import com.yourname.teleprompter.TeleprompterApp
import com.yourname.teleprompter.data.prefs.SecurePrefs
import com.yourname.teleprompter.engine.AutoScroller
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject
import kotlin.math.max

/**
 * 悬浮窗提词 Service
 * - 通过 WindowManager 显示一个 TYPE_APPLICATION_OVERLAY 窗口
 * - 标题栏可拖动，四角把手可缩放
 * - 内部承载 ScrollView + TextView，支持匀速滚动
 */
@AndroidEntryPoint
class FloatingWindowService : Service() {

    @Inject lateinit var prefs: SecurePrefs

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var scrollView: ScrollView
    private lateinit var textView: TextView
    private lateinit var progress: ProgressBar

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var engineJob: Job? = null

    private var scroller: AutoScroller? = null
    private var speedPxPerSec: Float = 30f

    // 拖动 / 缩放临时变量
    private var startRawX = 0f
    private var startRawY = 0f
    private var startX = 0
    private var startY = 0
    private var startW = 0
    private var startH = 0

    private var screenW = 0
    private var screenH = 0
    private val minW by lazy { (200 * resources.displayMetrics.density).toInt() }
    private val minH by lazy { (150 * resources.displayMetrics.density).toInt() }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        computeScreenSize()

        startForegroundCompat()

        floatingView = LayoutInflater.from(this)
            .inflate(R.layout.layout_floating_teleprompter, null)

        scrollView = floatingView.findViewById(R.id.floating_scroll)
        textView = floatingView.findViewById(R.id.floating_text)
        progress = floatingView.findViewById(R.id.floating_progress)

        val savedPos = prefs.getFloatingPosition()
        val savedX = savedPos[0]
        val savedY = savedPos[1]
        val savedW = savedPos[2]
        val savedH = savedPos[3]

        layoutParams = WindowManager.LayoutParams().apply {
            width = if (savedW > 0) savedW else (screenW * 0.9).toInt()
            height = if (savedH > 0) savedH else (screenH * 0.25).toInt()
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = if (savedX > 0) savedX else 0
            y = if (savedY > 0) savedY else screenH / 4
        }

        try {
            windowManager.addView(floatingView, layoutParams)
        } catch (e: WindowManager.BadTokenException) {
            // 没有 SYSTEM_ALERT_WINDOW 权限，停止
            stopSelf()
            return
        }

        setupDrag()
        setupResizeHandles()
        setupButtons()

        // 文稿内容由 onStartCommand 注入；此处只创建 AutoScroller
        scroller = AutoScroller(scrollView) { speedPxPerSec }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_PAUSE -> scroller?.stop()
            ACTION_RESUME -> scroller?.start()
            else -> {
                // 首次启动 / 普通刷新：注入文稿、字号、速度
                val content = intent?.getStringExtra(EXTRA_SCRIPT_CONTENT)
                if (!content.isNullOrEmpty()) {
                    textView.text = content
                }
                if (intent != null && intent.hasExtra(EXTRA_FONT_SP)) {
                    textView.textSize = intent.getFloatExtra(EXTRA_FONT_SP, textView.textSize)
                }
                if (intent != null && intent.hasExtra(EXTRA_SPEED)) {
                    speedPxPerSec = intent.getFloatExtra(EXTRA_SPEED, speedPxPerSec)
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat() {
        try {
            val channelId = TeleprompterApp.CHANNEL_FLOATING
            val mgr = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.getNotificationChannel(channelId) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(channelId, getString(R.string.noti_channel_floating),
                        NotificationManager.IMPORTANCE_LOW)
                )
            }
            val openIntent = PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            val stopIntent = PendingIntent.getService(
                this, 1, Intent(this, FloatingWindowService::class.java).apply { action = ACTION_STOP },
                PendingIntent.FLAG_IMMUTABLE
            )
            val notif: Notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_prompter)
                .setContentTitle(getString(R.string.noti_title_running))
                .setContentText(getString(R.string.noti_text_running))
                .setContentIntent(openIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_stop, getString(R.string.action_stop), stopIntent)
                .build()

            // 仅当 RECORD_AUDIO 已授权时才声明 microphone FGS type；
            // 否则只用 specialUse。避免 Android 14+ 上因权限缺失导致 SecurityException。
            val micGranted = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val type = if (micGranted) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                startForeground(NOTI_ID, notif, type)
            } else {
                startForeground(NOTI_ID, notif)
            }
        } catch (e: Throwable) {
            // 任何 SecurityException / RemoteException 都不能让 onCreate 整个崩。
            // 退化为只用 specialUse 再试一次（仅 Android 14+）。
            android.util.Log.e("FloatingWindowService", "startForeground 失败，尝试降级", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTI_ID, buildFallbackNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                }
            } catch (e2: Throwable) {
                android.util.Log.e("FloatingWindowService", "降级也失败，停止服务", e2)
                stopSelf()
            }
        }
    }

    private fun buildFallbackNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, TeleprompterApp.CHANNEL_FLOATING)
            .setSmallIcon(R.drawable.ic_prompter)
            .setContentTitle(getString(R.string.noti_title_running))
            .setContentText(getString(R.string.noti_text_running))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag() {
        val titleBar = floatingView.findViewById<View>(R.id.floating_title_bar)
        titleBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX; startRawY = event.rawY
                    startX = layoutParams.x; startY = layoutParams.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startRawX).toInt()
                    val dy = (event.rawY - startRawY).toInt()
                    layoutParams.x = clamp(startX + dx, 0, screenW - layoutParams.width)
                    layoutParams.y = clamp(startY + dy, 0, screenH - layoutParams.height)
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupResizeHandles() {
        attachHandle(R.id.handle_top_left, ResizeCorner.TL)
        attachHandle(R.id.handle_top_right, ResizeCorner.TR)
        attachHandle(R.id.handle_bottom_left, ResizeCorner.BL)
        attachHandle(R.id.handle_bottom_right, ResizeCorner.BR)
    }

    private enum class ResizeCorner { TL, TR, BL, BR }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachHandle(id: Int, corner: ResizeCorner) {
        val handle = floatingView.findViewById<View>(id)
        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX; startRawY = event.rawY
                    startX = layoutParams.x; startY = layoutParams.y
                    startW = layoutParams.width; startH = layoutParams.height
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startRawX).toInt()
                    val dy = (event.rawY - startRawY).toInt()
                    applyResize(corner, dx, dy)
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    true
                }
                else -> false
            }
        }
    }

    private fun applyResize(corner: ResizeCorner, dx: Int, dy: Int) {
        var newX = layoutParams.x
        var newY = layoutParams.y
        var newW = layoutParams.width
        var newH = layoutParams.height

        when (corner) {
            ResizeCorner.TL -> {
                newX = clamp(startX + dx, 0, startX + startW - minW)
                newY = clamp(startY + dy, 0, startY + startH - minH)
                newW = startX + startW - newX
                newH = startY + startH - newY
            }
            ResizeCorner.TR -> {
                newY = clamp(startY + dy, 0, startY + startH - minH)
                newW = clamp(startW + dx, minW, screenW - startX)
                newH = startY + startH - newY
            }
            ResizeCorner.BL -> {
                newX = clamp(startX + dx, 0, startX + startW - minW)
                newW = startX + startW - newX
                newH = clamp(startH + dy, minH, screenH - startY)
            }
            ResizeCorner.BR -> {
                newW = clamp(startW + dx, minW, screenW - startX)
                newH = clamp(startH + dy, minH, screenH - startY)
            }
        }
        layoutParams.x = newX
        layoutParams.y = newY
        layoutParams.width = max(minW, newW)
        layoutParams.height = max(minH, newH)
    }

    private fun setupButtons() {
        val btnPlay = floatingView.findViewById<ImageButton>(R.id.btn_play_pause)
        var playing = false
        btnPlay.setOnClickListener {
            if (playing) { scroller?.stop(); playing = false }
            else { scroller?.start(); playing = true }
        }
        val btnClose = floatingView.findViewById<ImageButton>(R.id.btn_close)
        btnClose.setOnClickListener { stopSelf() }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val sv = scrollView
            val child = sv.getChildAt(0) ?: return@addOnScrollChangedListener
            val max = maxOf(1, child.height - sv.height)
            progress.progress = (sv.scrollY * 100 / max).coerceIn(0, 100)
        }
    }

    private fun computeScreenSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            screenW = bounds.width()
            screenH = bounds.height()
            val insets = metrics.windowInsets
                .getInsets(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            screenW -= insets.left + insets.right
            screenH -= insets.top + insets.bottom
        } else {
            @Suppress("DEPRECATION")
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            screenW = dm.widthPixels
            screenH = dm.heightPixels
        }
    }

    private fun clamp(v: Int, lo: Int, hi: Int): Int = v.coerceIn(lo, hi)

    override fun onDestroy() {
        prefs.setFloatingPosition(
            layoutParams.x, layoutParams.y,
            layoutParams.width, layoutParams.height
        )
        scroller?.stop()
        engineJob?.cancel()
        scope.cancel()
        if (::floatingView.isInitialized) {
            try { windowManager.removeView(floatingView) } catch (_: Throwable) {}
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTI_ID = 1001
        const val ACTION_START = "com.yourname.teleprompter.action.START_FLOATING"
        const val ACTION_STOP = "com.yourname.teleprompter.action.STOP_FLOATING"
        const val ACTION_PAUSE = "com.yourname.teleprompter.action.PAUSE"
        const val ACTION_RESUME = "com.yourname.teleprompter.action.RESUME"
        const val EXTRA_SCRIPT_CONTENT = "extra_script_content"
        const val EXTRA_FONT_SP = "extra_font_sp"
        const val EXTRA_SPEED = "extra_speed"

        fun start(ctx: Context, content: String, fontSp: Float, speed: Float) {
            val intent = Intent(ctx, FloatingWindowService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SCRIPT_CONTENT, content)
                putExtra(EXTRA_FONT_SP, fontSp)
                putExtra(EXTRA_SPEED, speed)
            }
            androidx.core.content.ContextCompat.startForegroundService(ctx, intent)
        }
    }
}