package com.yourname.teleprompter.ui.scripts

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.teleprompter.data.local.entity.ScriptEntity
import com.yourname.teleprompter.data.prefs.SecurePrefs
import com.yourname.teleprompter.service.FloatingWindowService
import com.yourname.teleprompter.ui.editor.EditorActivity
import com.yourname.teleprompter.ui.keepalive.KeepAliveGuideActivity
import com.yourname.teleprompter.ui.settings.SettingsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptListScreen(
    vm: ScriptListViewModel = hiltViewModel()
) {
    val scripts by vm.scripts.collectAsState()
    val ctx = LocalContext.current
    val prefs = remember { SecurePrefs(ctx) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("悬浮提词器") },
                actions = {
                    IconButton(onClick = {
                        ctx.startActivity(Intent(ctx, KeepAliveGuideActivity::class.java))
                    }) { Icon(Icons.Default.Settings, contentDescription = "保活设置") }
                    IconButton(onClick = {
                        ctx.startActivity(Intent(ctx, SettingsActivity::class.java))
                    }) { Icon(Icons.Default.Settings, contentDescription = "设置") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val id = vm.createNew()
                ctx.startActivity(Intent(ctx, EditorActivity::class.java)
                    .putExtra(EditorActivity.EXTRA_ID, id))
            }) { Icon(Icons.Default.Add, contentDescription = "新建") }
        }
    ) { padding ->
        if (scripts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有文稿", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("点击右下角 + 创建第一条提词稿", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(scripts, key = { it.id }) { script ->
                    ScriptRow(
                        script = script,
                        onClick = {
                            ctx.startActivity(Intent(ctx, EditorActivity::class.java)
                                .putExtra(EditorActivity.EXTRA_ID, script.id))
                        },
                        onStart = { startFloating(ctx, script, prefs) },
                        onDelete = { vm.delete(script.id) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun ScriptRow(
    script: ScriptEntity,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(script.title.ifBlank { "未命名" }, fontWeight = FontWeight.SemiBold)
            Text("${script.content.length} 字 · ${df.format(Date(script.updatedAt))}",
                style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onStart) { Icon(Icons.Default.PlayArrow, "启动悬浮窗") }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除") }
    }
}

private fun startFloating(ctx: android.content.Context, script: ScriptEntity, prefs: SecurePrefs) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        !Settings.canDrawOverlays(ctx)) {
        ctx.startActivity(Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${ctx.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    prefs.setLastScriptId(script.id)
    FloatingWindowService.start(
        ctx,
        script.content,
        script.fontSizeSp,
        script.speedPxPerSec
    )
}