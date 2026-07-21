package com.yourname.teleprompter.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yourname.teleprompter.data.prefs.SecurePrefs
import com.yourname.teleprompter.ui.theme.TeleprompterTheme

class SettingsActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = SecurePrefs(this)

        setContent {
            TeleprompterTheme {
                val ctx = LocalContext.current
                var apiKey by remember { mutableStateOf(prefs.getApiKey().orEmpty()) }
                var autoBoot by remember { mutableStateOf(prefs.isAutoStartOnBoot()) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("设置") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, "返回")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        Modifier
                            .padding(padding)
                            .padding(16.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("AI 提词（MiniMax）", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                                prefs.setApiKey(it)
                            },
                            label = { Text("MiniMax API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Key 通过 AES-256 加密保存在本地。",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(Modifier.height(24.dp))
                        Divider()
                        Spacer(Modifier.height(24.dp))

                        Text("开机与自启动", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Switch(
                                checked = autoBoot,
                                onCheckedChange = {
                                    autoBoot = it
                                    prefs.setAutoStartOnBoot(it)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("开机自动启动提词器")
                        }

                        Spacer(Modifier.height(24.dp))
                        Text("使用说明", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            """
                            1. 在主界面创建文稿并编辑内容
                            2. 点击文稿右侧的播放按钮启动悬浮窗
                            3. 长按悬浮窗标题栏可拖动
                            4. 拖动悬浮窗四角可调整大小
                            5. 悬浮窗内有播放/暂停按钮可控制滚动
                            6. 启用 AI 提词后，应用会自动根据你的口播滚动文稿
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}