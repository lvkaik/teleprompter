package com.yourname.teleprompter.data.crash

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局未捕获异常拦截器。
 *
 * 行为：
 * - 捕获所有未捕获的异常
 * - 把堆栈、线程、版本号写入 `cacheDir/crash.log`
 * - **仍然调用系统 default handler**，所以闪退行为不变（不会出现"卡死"假象）
 * - 下次启动时由 [showLastCrashToast] 在前台弹一条 Toast，并把内容打到 logcat
 *
 * 设计目的：把崩溃信息从 ColorOS 的"三方应用异常"提示里抢出来，变成可读文本。
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val CRASH_FILE = "crash.log"

    @Volatile private var initialized = false

    /** 由 Application.onCreate 调用。幂等。 */
    fun install(context: Context) {
        if (initialized) return
        initialized = true

        val appCtx = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashToFile(appCtx, thread, throwable)
            } catch (t: Throwable) {
                Log.e(TAG, "写入崩溃日志失败", t)
            }
            // 仍然交给系统 default handler，保持原本的"闪退"行为
            previous?.uncaughtException(thread, throwable)
        }
        Log.i(TAG, "CrashReporter 已安装")
    }

    /**
     * 弹一条 Toast 告知用户上次崩溃，并把内容打到 logcat（tag=CrashReporter）。
     * 由 [com.yourname.teleprompter.TeleprompterApp] 在主线程上调用。
     * 没有崩溃文件时静默返回。
     */
    fun showLastCrashToast(context: Context) {
        val appCtx = context.applicationContext
        val file = File(appCtx.cacheDir, CRASH_FILE)
        if (!file.exists() || file.length() == 0L) return

        val text = try {
            file.readText()
        } catch (t: Throwable) {
            Log.w(TAG, "读取 crash.log 失败", t)
            return
        }

        // 打到 logcat（用户/开发者可以直接 logcat -s CrashReporter 抓）
        Log.e(TAG, "=== 上次崩溃内容 ===\n$text\n=== end ===")

        // 截前 200 字塞 Toast；太长了 Toast 会被截断
        val firstLine = text.lineSequence().firstOrNull()?.take(120) ?: "上次崩溃"
        try {
            Toast.makeText(appCtx, "上次崩溃: $firstLine", Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            // Toast 在某些后台进程里可能失败，吞掉
            Log.w(TAG, "Toast 显示失败", t)
        }
    }

    private fun writeCrashToFile(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("==== CRASH @ ").append(df.format(Date())).append(" ====\n")
        sb.append("Thread: ").append(thread.name).append(" (id=").append(thread.id).append(")\n")
        sb.append("App: com.yourname.teleprompter\n")
        sb.append("Android SDK: ").append(android.os.Build.VERSION.SDK_INT).append("\n")
        sb.append("Device: ").append(android.os.Build.MANUFACTURER).append(' ')
            .append(android.os.Build.MODEL).append("\n")
        sb.append("\n").append(sw.toString())

        val file = File(context.cacheDir, CRASH_FILE)
        try {
            file.writeText(sb.toString())
            Log.e(TAG, "崩溃已写入 ${file.absolutePath}")
        } catch (t: Throwable) {
            Log.e(TAG, "写入 ${file.absolutePath} 失败", t)
        }
    }
}