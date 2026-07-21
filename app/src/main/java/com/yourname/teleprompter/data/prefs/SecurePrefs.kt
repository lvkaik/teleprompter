package com.yourname.teleprompter.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // 设备不支持加密时 fallback 到普通 prefs
        context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
    }

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

    companion object {
        private const val KEY_API = "minimax_api_key"
        private const val KEY_AUTO_START = "auto_start_on_boot"
        private const val KEY_FLOAT_X = "float_x"
        private const val KEY_FLOAT_Y = "float_y"
        private const val KEY_FLOAT_W = "float_w"
        private const val KEY_FLOAT_H = "float_h"
        private const val KEY_LAST_SCRIPT = "last_script_id"
    }
}