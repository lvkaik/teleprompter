package com.yourname.teleprompter.ui.keepalive

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yourname.teleprompter.ui.theme.TeleprompterTheme

/**
 * 后台保活三件套引导（realme UI 7.0 适配）
 * ① 自启动 ② 电池优化 ③ 应用冻结
 */
class KeepAliveGuideActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeleprompterTheme {
                KeepAliveScreen()
            }
        }
    }
}

@Composable
private fun KeepAliveScreen() {
    val ctx = LocalContext.current
    var step1Done by remember { mutableStateOf(false) }
    var step2Done by remember { mutableStateOf(false) }
    var step3Done by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("后台保活设置") }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "为了在 realme UI 7.0 上让提词器稳定运行，请完成以下设置：",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))

            StepRow(
                title = "1. 允许自启动",
                desc = "设置 → 应用管理 → 提词器 → 自启动 → 允许",
                done = step1Done,
                onClick = {
                    openRealmeSafecenter(ctx)
                    step1Done = true
                }
            )
            StepRow(
                title = "2. 关闭电池优化",
                desc = "设置 → 电池 → 关闭电池优化 → 提词器",
                done = step2Done,
                onClick = {
                    ctx.startActivity(Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${ctx.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    step2Done = true
                }
            )
            StepRow(
                title = "3. 关闭应用冻结（realme UI 7.0 新增）",
                desc = "设置 → 电池 → 关闭应用冻结 → 提词器",
                done = step3Done,
                onClick = {
                    openRealmePowerMgr(ctx)
                    step3Done = true
                }
            )
            StepRow(
                title = "4. 锁定最近任务卡片",
                desc = "请在最近任务页长按本应用卡片，点击锁定",
                done = false,
                onClick = {
                    Toast.makeText(ctx, "请在最近任务页长按本应用卡片，点击锁定", Toast.LENGTH_LONG).show()
                    openRecents(ctx)
                }
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { (ctx as? android.app.Activity)?.finish() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("完成") }
        }
    }
}

@Composable
private fun StepRow(title: String, desc: String, done: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun openRealmeSafecenter(ctx: android.content.Context) {
    try {
        ctx.startActivity(Intent().apply {
            component = ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppAllListActivity"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (e: Exception) {
        fallbackAppDetails(ctx)
    }
}

private fun openRealmePowerMgr(ctx: android.content.Context) {
    try {
        ctx.startActivity(Intent().apply {
            component = ComponentName(
                "com.coloros.powermanager",
                "com.coloros.powermanager.fuelgaue.PowerConsumptionActivity"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (e: Exception) {
        try {
            ctx.startActivity(Intent().apply {
                component = ComponentName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerConsumptionActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e2: Exception) {
            fallbackAppDetails(ctx)
        }
    }
}

private fun openRecents(ctx: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            ctx.startActivity(Intent("com.android.systemui.action.RECENTS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            // ignore
        }
    }
}

private fun fallbackAppDetails(ctx: android.content.Context) {
    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${ctx.packageName}")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}