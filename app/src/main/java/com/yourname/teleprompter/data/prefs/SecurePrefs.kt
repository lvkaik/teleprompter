package com.yourname.teleprompter.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 偏好存储。
 *
 * 设计要点：
 * - **lazy 初始化**：EncryptedSharedPreferences 首次创建涉及 KeyStore + 文件 IO，
 *   在主线程上同步构造有 ANR 与崩溃风险。延后到首次访问时再初始化。
 * - **损坏自愈**：ColorOS/realme UI 在某些场景下会写入半截的 prefs 文件，
 *   导致下次启动反序列化时抛 GeneralSecurityException / IOException。
 *   catch 到异常后删除损坏文件并重试一次，仍失败再回落到普通 prefs。
 */
@Singleton
class SecurePrefs @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs: SharedPreferences by lazy { createPrefs() }

    fun setApiKey(key: String) = prefs.edit().putString(KEY_API, key).apply()
    fun getApiKey(): String? = prefs.getString(KEY_API, null)

    fun setAutoStartOnBoot(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    fun isAutoStartOnBoot(): Boolean = prefs.getBoolean(KEY_AUTO_START, false)

    fun setFloatingPosition(x: Int, y: Int, w: Int, h: Int) =
        prefs.edit()
            .putInt(KEY_FLOAT_X, x)
            .putInt(KEY_FLOAT_Y, y)
            .putInt(KEY_FLOAT_W, w)
            .putInt(KEY_FLOAT_H, h)
            .apply()
    fun getFloatingPosition(): IntArray =
        intArrayOf(
            prefs.getInt(KEY_FLOAT_X, 0),
            prefs.getInt(KEY_FLOAT_Y, 0),
            prefs.getInt(KEY_FLOAT_W, 0),
            prefs.getInt(KEY_FLOAT_H, 0)
        )

    fun setLastScriptId(id: String) = prefs.edit().putString(KEY_LAST_SCRIPT, id).apply()
    fun getLastScriptId(): String? = prefs.getString(KEY_LAST_SCRIPT, null)

    fun setSpeedPxPerSec(speed: Float) =
        prefs.edit().putFloat(KEY_SPEED_PX_PER_SEC, speed).apply()
    fun getSpeedPxPerSec(): Float = prefs.getFloat(KEY_SPEED_PX_PER_SEC, 0f)

    /**
     * 尝试创建 EncryptedSharedPreferences。失败则删除损坏文件再试一次；
     * 再失败则降级为普通 prefs（明文，但仍可用）。
     */
    private fun createPrefs(): SharedPreferences {
        return try {
            buildEncryptedPrefs()
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences 首次创建失败，尝试清理后重试", e)
            // 删除可能损坏的两个 prefs 文件
            deletePrefsArtifacts(FILE_SECURE)
            deletePrefsArtifacts(FILE_SECURE_KEY)
            try {
                buildEncryptedPrefs()
            } catch (e2: Exception) {
                Log.w(TAG, "重试仍失败，降级为普通 SharedPreferences", e2)
                context.getSharedPreferences(FILE_FALLBACK, Context.MODE_PRIVATE)
            }
        }
    }

    private fun buildEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_SECURE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun deletePrefsArtifacts(fileName: String) {
        try {
            val dir = File(context.applicationInfo.dataDir, "shared_prefs")
            val f = File(dir, "$fileName.xml")
            if (f.exists()) {
                val deleted = f.delete()
                Log.i(TAG, "删除损坏的 prefs 文件 $fileName.xml: $deleted")
            }
            // EncryptedSharedPreferences 还会生成 .xml.bak / .preferences_pb 等
            listOf(".bak", ".preferences_pb").forEach { ext ->
                val bak = File(dir, "$fileName$ext")
                if (bak.exists()) bak.delete()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "删除 prefs 文件 $fileName 失败", t)
        }
    }

    companion object {
        private const val TAG = "SecurePrefs"

        private const val FILE_SECURE = "secure_prefs"
        private const val FILE_SECURE_KEY = "secure_prefs_key"
        private const val FILE_FALLBACK = "fallback_prefs"

        private const val KEY_API = "minimax_api_key"
        private const val KEY_AUTO_START = "auto_start_on_boot"
        private const val KEY_FLOAT_X = "float_x"
        private const val KEY_FLOAT_Y = "float_y"
        private const val KEY_FLOAT_W = "float_w"
        private const val KEY_FLOAT_H = "float_h"
        private const val KEY_LAST_SCRIPT = "last_script_id"
        private const val KEY_SPEED_PX_PER_SEC = "speed_px_per_sec"
    }
}